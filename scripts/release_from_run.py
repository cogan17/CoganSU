#!/usr/bin/env python3
"""Check one green "Build CoganSU" run and prepare its release: notes (ID/EN) and Telegram text (ID/EN).

    release_from_run.py INFO_DIR FILES_DIR --repo R --run-url U --sha S [--tested] [--note ..] [--note-en ..]
                        [--telegram DIR] [--text-only] [--upstream-ref upstream/main] > notes.md

Writes INFO_DIR/release.env (RELEASE, RELEASE_NAME). Exit 1 with ::error:: when anything does not match.
"""
import argparse
import hashlib
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cogansu_cert  # noqa: E402
from cogansu_common import RELEASE_TAG, area_counts, commits, gh, git, read_env, trailer  # noqa: E402

PACKAGE = "com.cogan.su"
REQUIRED = ("RELEASE", "RELEASE_NAME", "VERSION", "CODE", "COMMIT", "PACKAGE", "APK", "APK_SHA256",
            "CERT_SIZE", "CERT_HASH", "CERT_STATE", "UPSTREAM_COMMIT")

WORDS = {
    "id": {
        "about": "Root manager khusus Xiaomi 14T Pro (rothko), berbasis KernelSU.",
        "pair": "Pasang bersama Cogan Kernel yang dibangun dari commit CoganSU yang sama; "
                "kernel lain (termasuk KernelSU resmi) tidak mengenali aplikasi ini.",
        "version": "Versi", "base": "Dasar KernelSU", "package": "Paket", "cert": "Sertifikat",
        "tested_row": "Diuji di HP", "yes": "✅ sudah, oleh owner", "no": "belum",
        "since": "Perubahan sejak {prev}", "first": "Rilis pertama CoganSU",
        "upstream": "KernelSU (hulu): {n} perubahan ({areas})",
        "own_count": "{n} perubahan CoganSU", "none": "tidak ada perubahan",
        "note": "Catatan", "files": "File", "checksums": "SHA256 ada di `checksums.txt`.",
        "build": "Build", "new": "🆕 Baru:", "tg_note": "📝 Catatan:",
        "tested": "🧪 Diuji di HP owner", "untested": "🧪 Belum diuji di HP",
        "warn": "⚠️ Hanya untuk Cogan Kernel dari commit CoganSU yang sama.",
        "download": "⬇️ Unduh", "first_tg": "Rilis pertama CoganSU: root manager sendiri untuk Cogan Kernel",
    },
    "en": {
        "about": "Root manager for Xiaomi 14T Pro (rothko) only, based on KernelSU.",
        "pair": "Install it with a Cogan Kernel built from the same CoganSU commit; other kernels "
                "(official KernelSU included) do not recognise this app.",
        "version": "Version", "base": "KernelSU base", "package": "Package", "cert": "Certificate",
        "tested_row": "Tested on the phone", "yes": "✅ yes, by the owner", "no": "not yet",
        "since": "Changes since {prev}", "first": "First CoganSU release",
        "upstream": "KernelSU (upstream): {n} changes ({areas})",
        "own_count": "{n} CoganSU changes", "none": "no changes",
        "note": "Note", "files": "Files", "checksums": "SHA256 in `checksums.txt`.",
        "build": "Build", "new": "🆕 New:", "tg_note": "📝 Note:",
        "tested": "🧪 Tested on the owner's phone", "untested": "🧪 Not tested on the phone yet",
        "warn": "⚠️ Only for a Cogan Kernel built from the same CoganSU commit.",
        "download": "⬇️ Download", "first_tg": "First CoganSU release: our own root manager for Cogan Kernel",
    },
}


def fail(message):
    raise ValueError(message)


def validate(info, files, sha):
    env_path = info / "build.env"
    if not env_path.is_file():
        fail("build.env tidak ada / missing build.env")
    env = read_env(env_path)
    missing = [key for key in REQUIRED if not env.get(key)]
    if missing:
        fail(f"metadata tidak lengkap / incomplete metadata: {', '.join(missing)}")
    if env["COMMIT"] != sha:
        fail(f"commit build {env['COMMIT'][:12]} bukan commit run / not the run's commit {sha[:12]}")
    if env["PACKAGE"] != PACKAGE:
        fail(f"paket {env['PACKAGE']} bukan / not {PACKAGE}")
    if env["CERT_STATE"] != "match":
        fail("kernel/cogansu.mk belum cocok dengan kunci rilis di run itu / did not match the release key in that run")
    if not RELEASE_TAG.match(env["RELEASE"]):
        fail(f"tag rilis / release tag {env['RELEASE']!r} bukan / is not vYY.MM.DD-rN")
    if not env["CODE"].isdigit():
        fail(f"kode versi / version code: {env['CODE']!r}")
    apks = sorted(path.name for path in files.iterdir() if path.is_file())
    if apks != [env["APK"]]:
        fail(f"file harus tepat {env['APK']} / files must be exactly that APK: {', '.join(apks) or '-'}")
    apk = files / env["APK"]
    if hashlib.sha256(apk.read_bytes()).hexdigest() != env["APK_SHA256"]:
        fail(f"{apk.name}: SHA256 berbeda dari build / differs from the build")
    size, digest = cogansu_cert.describe(cogansu_cert.apk_certificate(apk))
    if (size, digest) != (env["CERT_SIZE"], env["CERT_HASH"]):
        fail(f"{apk.name}: sertifikat APK berbeda dari build / APK certificate differs from the build")
    kernel = cogansu_cert.read_mk()
    if not all(kernel) or cogansu_cert.normalize(*kernel) != (size, digest):
        fail("sertifikat APK berbeda dari kernel/cogansu.mk di main / differs from kernel/cogansu.mk on main")
    return env


def previous_release(repo, tag, api):
    """The newest published CoganSU release other than tag (the API lists newest first)."""
    releases = api(f"repos/{repo}/releases?per_page=100")
    for release in releases if isinstance(releases, list) else []:
        name = release.get("tag_name", "")
        if not release.get("draft") and RELEASE_TAG.match(name) and name != tag:
            return release
    return None


def changes(prev_tag, sha, upstream_ref):
    """(own commits, upstream commits) between the previous release and sha; None when unreadable."""
    prev = git("rev-parse", "-q", "--verify", f"{prev_tag}^{{commit}}", check=False)
    if not prev:
        return None
    own, upstream = [], []
    for item in commits(f"{prev}..{sha}"):
        # Merges, CI, docs and chores do not change the app or the kernel: not news for end users.
        if item[1].startswith("Merge ") or re.match(r"^(ci|docs|chore|test|style)(\([^)]*\))?!?:", item[1]):
            continue
        is_upstream = git("merge-base", "--is-ancestor", item[0], upstream_ref, check=False) is not None
        (upstream if is_upstream else own).append(item)
    return own, upstream


def plain(subject):
    """'feat(manager): aplikasi CoganSU' -> 'Aplikasi CoganSU': no commit jargon for end users."""
    text = re.sub(r"^\w+(\([^)]*\))?!?:\s*", "", subject).strip()
    return text[:1].upper() + text[1:]


def own_lines(own, lang):
    """Own commit subjects are Indonesian (Release-ID trailer overrides); English comes only from a
    Release-EN trailer, never a copy of the Indonesian text."""
    w = WORDS[lang]
    if lang == "id":
        return [trailer(body, "Release-ID") or plain(subject) for _, subject, body, _ in own]
    english = [trailer(body, "Release-EN") for _, _, body, _ in own]
    lines = [text for text in english if text]
    rest = len(english) - len(lines)
    if rest:
        lines.append(w["own_count"].format(n=rest) if not lines else
                     ("1 other CoganSU change" if rest == 1 else f"{rest} other CoganSU changes"))
    return lines


def base_version(version):
    return version.split("-", 1)[0]


def notes(env, lang, prev, diff, run_url, tested, note):
    w = WORDS[lang]
    flag = "🇮🇩" if lang == "id" else "🇬🇧"
    lines = [f"## {flag} {env['RELEASE_NAME']}", "", w["about"], "", f"> {w['pair']}", "",
             "| | |", "|---|---|",
             f"| {w['version']} | `{env['VERSION']}` ({env['CODE']}) |",
             f"| {w['base']} | [`{env['UPSTREAM_COMMIT'][:12]}`](https://github.com/tiann/KernelSU/commit/{env['UPSTREAM_COMMIT']}) |",
             f"| {w['package']} | `{env['PACKAGE']}` |",
             f"| {w['cert']} | `{env['CERT_SIZE']}` `{env['CERT_HASH']}` |",
             f"| {w['tested_row']} | {w['yes'] if tested else w['no']} |",
             f"| {w['build']} | [run]({run_url}) · `{env['COMMIT'][:12]}` |", ""]
    if prev is None:
        lines += [f"**{w['first']}**", ""]
    elif diff is not None:
        own, upstream = diff
        lines += [f"**{w['since'].format(prev=prev['tag_name'])}**", ""]
        items = [f"- {text}" for text in own_lines(own, lang)]
        if upstream:
            items.append("- " + w["upstream"].format(n=len(upstream), areas=area_counts(upstream, lang)))
        lines += (items or [f"- {w['none']}"]) + [""]
    if note.strip():
        lines += [f"**{w['note']}**", "", *note.replace("\\n", "\n").strip().splitlines(), ""]
    lines += [f"{w['files']}: `{env['APK']}` · {w['checksums']}", ""]
    return "\n".join(lines)


def telegram_post(env, lang, prev, diff, link, tested, note):
    w = WORDS[lang]
    lines = ["🛡️ CoganSU · Xiaomi 14T Pro (rothko)",
             f"📦 {env['RELEASE']} · CoganSU {env['CODE']} · KernelSU {base_version(env['VERSION'])}", ""]
    if prev is None:
        items = [w["first_tg"]]
    elif diff is None:
        items = []
    else:
        own, upstream = diff
        items = own_lines(own, lang)
        if upstream:
            items.append(w["upstream"].format(n=len(upstream), areas=area_counts(upstream, lang)))
    if items:
        lines += [w["new"], *[f"• {item}" for item in items], ""]
    if note.strip():
        lines += [w["tg_note"], *note.replace("\\n", "\n").strip().splitlines(), ""]
    lines += [w["warn"], w["tested"] if tested else w["untested"]]
    if link:
        lines.append(f"{w['download']}: {link}")
    return "\n".join(lines) + "\n"


def summary(posts):
    out = []
    for lang, title in (("id", "🇮🇩 Teks Telegram (Indonesia)"), ("en", "🇬🇧 Telegram text (English)")):
        out += [f"<details{' open' if lang == 'id' else ''}><summary>{title}</summary>", "", "```text",
                posts[lang].rstrip("\n"), "```", "", "</details>", ""]
    return "\n".join(out)


def prepare(info, files, repo, run_url, sha, tested=False, note="", note_en="", telegram=None,
            text_only=False, upstream_ref="upstream/main", api=gh):
    env = validate(info, files, sha)
    tag = env["RELEASE"]
    prev = previous_release(repo, tag, api)
    diff = changes(prev["tag_name"], sha, upstream_ref) if prev else None
    link = f"https://github.com/{repo}/releases/tag/{tag}"
    if text_only:
        current = api(f"repos/{repo}/releases/tags/{tag}")
        if not isinstance(current, dict) or current.get("draft", True):
            link = run_url
    if telegram is not None:
        telegram.mkdir(parents=True, exist_ok=True)
        posts = {lang: telegram_post(env, lang, prev, diff, link, tested, note if lang == "id" else note_en)
                 for lang in ("id", "en")}
        for lang, text in posts.items():
            (telegram / f"{lang}.txt").write_text(text, encoding="utf-8")
        (telegram / "summary.md").write_text(f"**{env['RELEASE_NAME']} · {env['VERSION']} ({env['CODE']})**\n\n" + summary(posts),
                                             encoding="utf-8")
    text = notes(env, "id", prev, diff, run_url, tested, note) + "\n" + \
        notes(env, "en", prev, diff, run_url, tested, note_en)
    metadata = {"RELEASE": tag, "RELEASE_NAME": env["RELEASE_NAME"]}
    return metadata, text


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("info", type=Path)
    parser.add_argument("files", type=Path)
    parser.add_argument("--repo", required=True)
    parser.add_argument("--run-url", required=True)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--tested", action="store_true")
    parser.add_argument("--note", default="")
    parser.add_argument("--note-en", default="")
    parser.add_argument("--telegram", type=Path, default=Path("telegram"))
    parser.add_argument("--text-only", action="store_true")
    parser.add_argument("--upstream-ref", default="upstream/main")
    args = parser.parse_args(argv)
    try:
        metadata, text = prepare(args.info, args.files, args.repo, args.run_url, args.sha, args.tested,
                                 args.note, args.note_en, args.telegram, args.text_only, args.upstream_ref)
    except (ValueError, OSError, RuntimeError) as error:
        parser.exit(1, f"::error::{error}\n")
    (args.info / "release.env").write_text("".join(f"{k}={v}\n" for k, v in metadata.items()), encoding="utf-8")
    sys.stdout.write(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
