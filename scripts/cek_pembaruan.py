#!/usr/bin/env python3
"""Cek pembaruan CoganSU: only checks, never builds, publishes or writes anything but its report.

Compares CoganSU main with what is newest upstream and next to it:
  - KernelSU (tiann/KernelSU): newest release and main, commits not merged into CoganSU yet (by area);
  - SUSFS (simonpunk, gki-android14-6.1): newest commit, and whether its KernelSU patch still applies to
    CoganSU main and to KernelSU main (whether merging upstream now would break SUSFS);
  - CoganSU: kernel/cogansu.mk, newest release and commits since, last Build CoganSU run;
  - Cogan Kernel: Cogan Bridge ABI as the app reads it, CoganSU integration, newest release.

    cek_pembaruan.py --repo cogan17/CoganSU [--upstream-ref upstream/main] [--out updates.md]
"""
import argparse
import base64
import re
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cogansu_cert  # noqa: E402
from cogansu_common import (KERNEL_REPO, RELEASE_TAG, SUSFS_BRANCH, SUSFS_KSU_PATCH, SUSFS_PROJECT,  # noqa: E402
                            AREA_WORDS, UPSTREAM_REPO, area_counts, commits, gh, git, gitlab, wib)

ROOT = Path(__file__).resolve().parent.parent
BRIDGE_ABI = "1"
BRIDGE_FILES = ("abi", "release", "banner", "device", "soc")  # what manager/.../cogan/Cogan.kt reads


def ok(flag, lang):
    return ("✅ " if flag else "⚠️ ") + {"id": ("ya" if flag else "tidak"), "en": ("yes" if flag else "no")}[lang]


def patch_applies(patch, tree):
    result = subprocess.run(["git", "apply", "--check", str(patch)], cwd=tree, capture_output=True, text=True)
    return result.returncode == 0


def upstream_state(upstream_ref, api):
    head = git("rev-parse", upstream_ref)
    base = git("merge-base", "HEAD", upstream_ref)
    pending = [item for item in commits(f"HEAD..{upstream_ref}") if not item[1].startswith("Merge ")]
    release = api(f"repos/{UPSTREAM_REPO}/releases/latest") or {}
    return {
        "head": head, "head_date": git("log", "-1", "--format=%cI", head),
        "base": base, "base_date": git("log", "-1", "--format=%cI", base),
        "pending": pending,
        "release": release.get("tag_name", ""), "release_date": release.get("published_at", ""),
    }


def susfs_state(upstream_ref):
    project = SUSFS_PROJECT
    branch = gitlab(f"projects/{project}/repository/branches/{SUSFS_BRANCH}") or {}
    commit = branch.get("commit") or {}
    path = urllib.parse.quote(SUSFS_KSU_PATCH, safe="")
    state = {"commit": commit.get("id", ""), "date": commit.get("committed_date", ""),
             "title": commit.get("title", ""), "cogansu": None, "upstream": None}
    if not state["commit"]:
        return state
    try:
        url = (f"https://gitlab.com/api/v4/projects/{project}/repository/files/{path}/raw"
               f"?ref={state['commit']}")
        with urllib.request.urlopen(url, timeout=60) as reply:
            patch_text = reply.read()
    except OSError:
        return state
    with tempfile.TemporaryDirectory() as tmp:
        patch = Path(tmp) / "susfs.patch"
        patch.write_bytes(patch_text)
        state["cogansu"] = patch_applies(patch, ROOT)
        tree = Path(tmp) / "upstream"
        if git("worktree", "add", "-q", "--detach", str(tree), upstream_ref, check=False) is not None:
            try:
                state["upstream"] = patch_applies(patch, tree)
            finally:
                git("worktree", "remove", "--force", str(tree), check=False)
    return state


def cogansu_state(repo, api):
    size, digest = cogansu_cert.read_mk()
    releases = api(f"repos/{repo}/releases?per_page=100")
    newest = next((release for release in (releases if isinstance(releases, list) else [])
                   if not release.get("draft") and RELEASE_TAG.match(release.get("tag_name", ""))), None)
    since = None
    if newest:
        tagged = git("rev-parse", "-q", "--verify", f"{newest['tag_name']}^{{commit}}", check=False)
        if tagged:
            since = [item for item in commits(f"{tagged}..HEAD") if not item[1].startswith("Merge ")]
    runs = api(f"repos/{repo}/actions/workflows/build-cogansu.yml/runs?branch=main&per_page=1") or {}
    run = (runs.get("workflow_runs") or [None])[0]
    return {
        "cert": bool(size and digest), "head": git("rev-parse", "HEAD"),
        "code": 30000 + int(git("rev-list", "--count", "HEAD")),
        "release": newest, "since": since, "run": run,
    }


def bridge_state(api):
    source = api(f"repos/{KERNEL_REPO}/contents/kernel/patches/cogan_bridge.c")
    build = api(f"repos/{KERNEL_REPO}/contents/kernel/build.sh")
    release = api(f"repos/{KERNEL_REPO}/releases/latest") or {}
    state = {"abi": None, "features": None, "files": None, "integrated": None,
             "release": release.get("tag_name", ""), "release_date": release.get("published_at", "")}
    if isinstance(source, dict) and source.get("content"):
        text = base64.b64decode(source["content"]).decode("utf-8", "replace")
        abi = re.search(r"abi_show\([^)]*\)\s*\{[^}]*sysfs_emit\(buf,\s*\"(\d+)\\n\"", text)
        features = re.search(r"features_show\([^)]*\)\s*\{[^}]*sysfs_emit\(buf,\s*\"([^\"]*)\"", text)
        state["abi"] = abi.group(1) if abi else ""
        state["features"] = re.findall(r"(\w+)=%d", features.group(1)) if features else []
        state["files"] = re.findall(r"__ATTR\((\w+),", text)
    if isinstance(build, dict) and build.get("content"):
        state["integrated"] = "cogan17/CoganSU" in base64.b64decode(build["content"]).decode("utf-8", "replace")
    return state


T = {
    "id": {
        "title": "## 🔎 Cek pembaruan CoganSU",
        "intro": "Hanya mengecek: tidak membangun, tidak menerbitkan, tidak menulis apa pun.",
        "head": "| Komponen | CoganSU sekarang | Terbaru | Status |",
        "ksu": "KernelSU (hulu)", "susfs": "SUSFS (simonpunk)", "csu": "CoganSU", "ck": "Cogan Kernel",
        "release": "rilis", "main": "main", "base": "dasar",
        "pending": "{n} commit belum digabung ({areas})", "uptodate": "✅ sudah terbaru",
        "susfs_ok": "✅ patch SUSFS cocok dengan CoganSU", "susfs_bad": "❌ patch SUSFS tidak cocok dengan CoganSU: tunggu simonpunk",
        "susfs_up_ok": "menggabung KernelSU main sekarang: patch tetap cocok",
        "susfs_up_bad": "⚠️ menggabung KernelSU main sekarang: patch tidak cocok (tunggu simonpunk sebelum menggabung)",
        "unknown": "tidak terbaca",
        "cert": "kernel/cogansu.mk", "cert_ok": "✅ terisi", "cert_bad": "⚠️ masih kosong (docs/kunci.md)",
        "last_release": "rilis terakhir", "no_release": "belum ada rilis",
        "since": "{n} commit sejak rilis terakhir", "since0": "✅ tidak ada commit baru sejak rilis terakhir",
        "run": "Build CoganSU terakhir", "no_run": "belum pernah dijalankan", "run_old": "⚠️ bukan commit main terbaru",
        "bridge": "Cogan Bridge", "bridge_ok": "✅ ABI {abi}, fitur {features}: cocok dengan aplikasi",
        "bridge_bad": "❌ ABI/fitur berubah ({abi}; {features}): perbarui cogan/Cogan.kt",
        "integrated": "CoganSU di build.sh", "titles": "Judul asli commit KernelSU yang belum digabung",
    },
    "en": {
        "title": "## 🔎 CoganSU update check",
        "intro": "Checks only: builds, publishes and writes nothing.",
        "head": "| Component | CoganSU now | Newest | Status |",
        "ksu": "KernelSU (upstream)", "susfs": "SUSFS (simonpunk)", "csu": "CoganSU", "ck": "Cogan Kernel",
        "release": "release", "main": "main", "base": "base",
        "pending": "{n} commits not merged ({areas})", "uptodate": "✅ up to date",
        "susfs_ok": "✅ SUSFS patch fits CoganSU", "susfs_bad": "❌ SUSFS patch does not fit CoganSU: wait for simonpunk",
        "susfs_up_ok": "merging KernelSU main now: the patch still fits",
        "susfs_up_bad": "⚠️ merging KernelSU main now: the patch would not fit (wait for simonpunk before merging)",
        "unknown": "unreadable",
        "cert": "kernel/cogansu.mk", "cert_ok": "✅ filled in", "cert_bad": "⚠️ still empty (docs/kunci.en.md)",
        "last_release": "last release", "no_release": "no release yet",
        "since": "{n} commits since the last release", "since0": "✅ no new commits since the last release",
        "run": "last Build CoganSU", "no_run": "never run", "run_old": "⚠️ not the newest main commit",
        "bridge": "Cogan Bridge", "bridge_ok": "✅ ABI {abi}, features {features}: matches the app",
        "bridge_bad": "❌ ABI/features changed ({abi}; {features}): update cogan/Cogan.kt",
        "integrated": "CoganSU in build.sh", "titles": "KernelSU commits not merged yet (original titles from the source)",
    },
}


def report(lang, up, susfs, csu, bridge):
    t = T[lang]
    rows = [t["head"], "|---|---|---|---|"]
    status = t["uptodate"] if not up["pending"] else "⚠️ " + t["pending"].format(
        n=len(up["pending"]), areas=area_counts(up["pending"], lang))
    rows.append(f"| {t['ksu']} | {t['base']} `{up['base'][:12]}` · {wib(up['base_date'], lang)} | "
                f"{t['main']} `{up['head'][:12]}` · {wib(up['head_date'], lang)}<br>{t['release']} "
                f"{up['release'] or '-'} · {wib(up['release_date'], lang)} | {status} |")
    if susfs["cogansu"] is None:
        susfs_status = t["unknown"]
    else:
        susfs_status = t["susfs_ok"] if susfs["cogansu"] else t["susfs_bad"]
        if up["pending"] and susfs["upstream"] is not None:
            susfs_status += "<br>" + (t["susfs_up_ok"] if susfs["upstream"] else t["susfs_up_bad"])
    rows.append(f"| {t['susfs']} | {SUSFS_BRANCH} | `{susfs['commit'][:12] or '-'}` · {wib(susfs['date'], lang)} | "
                f"{susfs_status} |")
    rel = csu["release"]
    rel_text = f"{rel['tag_name']} · {wib(rel.get('published_at'), lang)}" if rel else t["no_release"]
    if rel and csu["since"] is not None:
        since = t["since0"] if not csu["since"] else "⚠️ " + t["since"].format(n=len(csu["since"]))
    else:
        since = "-"
    rows.append(f"| {t['csu']} | main `{csu['head'][:12]}` ({csu['code']}) | {t['last_release']}: {rel_text} | "
                f"{since}<br>{t['cert']}: {t['cert_ok'] if csu['cert'] else t['cert_bad']} |")
    run = csu["run"]
    if run:
        run_status = {"success": "✅", "failure": "❌"}.get(run.get("conclusion") or "", "⏳")
        run_text = f"{run_status} [{run.get('conclusion') or run.get('status')}]({run.get('html_url')}) · {wib(run.get('created_at'), lang)}"
        if run.get("head_sha") != csu["head"]:
            run_text += f"<br>{t['run_old']}"
    else:
        run_text = t["no_run"]
    rows.append(f"| {t['run']} | | | {run_text} |")
    if bridge["abi"] is None:
        bridge_text = t["unknown"]
    else:
        fits = bridge["abi"] == BRIDGE_ABI and all(name in (bridge["files"] or []) for name in BRIDGE_FILES)
        bridge_text = (t["bridge_ok"] if fits else t["bridge_bad"]).format(
            abi=bridge["abi"] or "?", features=", ".join(bridge["features"] or []) or "?")
    integrated = t["unknown"] if bridge["integrated"] is None else ok(bridge["integrated"], lang)
    rows.append(f"| {t['ck']} | | {t['release']} {bridge['release'] or '-'} · {wib(bridge['release_date'], lang)} | "
                f"{t['bridge']}: {bridge_text}<br>{t['integrated']}: {integrated} |")
    lines = [t["title"], "", t["intro"], "", *rows, ""]
    if up["pending"] and lang == "id":
        lines += [f"{t['titles']}: lihat bagian English di bawah.", ""]
    if up["pending"] and lang == "en":
        lines += [f"<details><summary>{t['titles']}</summary>", ""]
        lines += [f"- {AREA_WORDS['en'][name]}: {subject}" for _, subject, _, name in up["pending"][-40:]]
        lines += ["", "</details>", ""]
    return "\n".join(lines)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", required=True)
    parser.add_argument("--upstream-ref", default="upstream/main")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args(argv)
    up = upstream_state(args.upstream_ref, gh)
    susfs = susfs_state(args.upstream_ref)
    csu = cogansu_state(args.repo, gh)
    bridge = bridge_state(gh)
    text = report("id", up, susfs, csu, bridge) + "\n" + report("en", up, susfs, csu, bridge)
    if args.out:
        args.out.write_text(text, encoding="utf-8")
    sys.stdout.write(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
