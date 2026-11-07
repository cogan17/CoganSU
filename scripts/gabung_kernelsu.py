#!/usr/bin/env python3
"""Merge the newest KernelSU main into CoganSU before every Build CoganSU (owner 2026-11-07: "jika saya build
otomatis akan mengupdate dari KSU, jadi tidak perlu ada kata menunggu"), then check SUSFS's patch for KernelSU.

    gabung_kernelsu.py [--merge] [--upstream-ref upstream/main] [--susfs-ref refs/check/susfs]
                       [--out GITHUB_OUTPUT] [--summary GITHUB_STEP_SUMMARY]

Runs in a CoganSU checkout whose HEAD is the branch to build. Without --merge (a branch other than main) it
only reports. Upstream changes to files CoganSU removed on purpose (.github/: KernelSU's workflows, issue
templates, dependabot) stay removed. Any other conflict stops: it needs a person, not waiting.

Outputs: sha (the commit to build), merged (yes/no), upstream (KernelSU main), susfs
(fits / upstream-too / cogansu-only / unknown).

Gabung KernelSU main terbaru ke CoganSU sebelum setiap Build CoganSU, lalu cek patch SUSFS untuk KernelSU.
"""
import argparse
import os
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from cogansu_common import SUSFS_KSU_PATCH, commits, git  # noqa: E402

REMOVED_ON_PURPOSE = (".github/",)


def unmerged():
    """{path: XY status} of the unmerged paths."""
    out = git("status", "--porcelain=v1", "-z")
    found = {}
    for record in (out or "").split("\0"):
        if len(record) > 3 and record[:2] in ("DD", "AU", "UD", "UA", "DU", "AA", "UU"):
            found[record[3:]] = record[:2]
    return found


def merge(upstream):
    """Merge upstream into HEAD (no fast-forward). Returns the merge's upstream commit titles."""
    titles = [subject for _, subject, _, _ in commits(f"HEAD..{upstream}")]
    result = subprocess.run(["git", "merge", "--no-ff", "--no-commit", "--no-edit", upstream],
                            capture_output=True, text=True)
    if result.returncode != 0:
        conflicts = unmerged()
        kept_removed = [p for p, s in conflicts.items() if s == "DU" and p.startswith(REMOVED_ON_PURPOSE)]
        for path in kept_removed:
            git("rm", "-q", "--", path)
        left = sorted(p for p in conflicts if p not in kept_removed)
        if left or not conflicts:
            git("merge", "--abort", check=False)
            raise RuntimeError("KernelSU main bentrok dengan perubahan CoganSU / conflicts with CoganSU's changes: "
                               + (", ".join(left) or result.stderr.strip()[:300]))
    short = git("rev-parse", "--short=12", upstream)
    body = "\n".join(f"- {title}" for title in titles[-30:])
    more = f"\n- … {len(titles) - 30} lagi / more" if len(titles) > 30 else ""
    git("commit", "-q", "-m", f"Merge KernelSU main {short} ke CoganSU (otomatis, Build CoganSU)\n\n{body}{more}")
    return titles


def patch_fits(ref, patch):
    """True when patch applies to the tree of ref (scratch index, no checkout)."""
    with tempfile.TemporaryDirectory() as tmp:
        env = {**os.environ, "GIT_INDEX_FILE": str(Path(tmp, "index"))}
        subprocess.run(["git", "read-tree", ref], env=env, check=True, capture_output=True)
        return subprocess.run(["git", "apply", "--cached", "--check", "-"], input=patch, env=env, text=True,
                              capture_output=True).returncode == 0


def susfs_state(susfs_ref, upstream):
    patch = git("show", f"{susfs_ref}:{SUSFS_KSU_PATCH}", check=False) if susfs_ref else None
    if not patch:
        return "unknown"
    patch += "\n"
    if patch_fits("HEAD", patch):
        return "fits"
    return "cogansu-only" if patch_fits(upstream, patch) else "upstream-too"


TEXT = {
    "merged": ("✅ KernelSU main `{up}` digabung otomatis ({n} commit hulu).",
               "✅ KernelSU main `{up}` merged automatically ({n} upstream commits)."),
    "same": ("✅ Sudah memuat KernelSU main terbaru `{up}`.", "✅ Already carries the newest KernelSU main `{up}`."),
    "behind": ("ℹ️ Bukan branch utama: KernelSU main `{up}` tidak digabung ({n} commit hulu belum ada).",
               "ℹ️ Not the main branch: KernelSU main `{up}` not merged ({n} upstream commits missing)."),
    "fits": ("✅ Patch SUSFS `{s}` untuk KernelSU cocok.", "✅ SUSFS `{s}` patch for KernelSU fits."),
    "upstream-too": ("⏳ Patch SUSFS `{s}` belum cocok, juga dengan KernelSU main: tunggu simonpunk.",
                     "⏳ SUSFS `{s}` patch does not fit, nor KernelSU main: wait for simonpunk."),
    "cogansu-only": ("❌ Patch SUSFS `{s}` cocok dengan KernelSU main tetapi tidak dengan CoganSU: perubahan CoganSU "
                     "di `kernel/` perlu disesuaikan.",
                     "❌ SUSFS `{s}` patch fits KernelSU main but not CoganSU: CoganSU's changes in `kernel/` need "
                     "adapting."),
    "unknown": ("⚠️ Patch SUSFS tidak terbaca.", "⚠️ SUSFS patch could not be read."),
}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--merge", action="store_true")
    parser.add_argument("--upstream-ref", default="upstream/main")
    parser.add_argument("--susfs-ref", default="refs/check/susfs")
    parser.add_argument("--out", type=Path)
    parser.add_argument("--summary", type=Path)
    args = parser.parse_args(argv)
    upstream = git("rev-parse", args.upstream_ref)
    up = upstream[:12]
    if git("merge-base", "--is-ancestor", upstream, "HEAD", check=False) is not None:
        merged, state = "no", "same"
        count = 0
    elif args.merge:
        try:
            count = len(merge(upstream))
        except RuntimeError as error:
            print(f"::error::{error}")
            return 1
        merged, state = "yes", "merged"
    else:
        merged, state = "no", "behind"
        count = len(commits(f"HEAD..{upstream}"))
    susfs_sha = git("rev-parse", "--verify", "-q", args.susfs_ref, check=False) or ""
    susfs = susfs_state(args.susfs_ref if susfs_sha else None, upstream)
    head = git("rev-parse", "HEAD")
    lines = [TEXT[state][0].format(up=up, n=count), TEXT[susfs][0].format(s=susfs_sha[:12])]
    lines_en = [TEXT[state][1].format(up=up, n=count), TEXT[susfs][1].format(s=susfs_sha[:12])]
    report = ("### 🔄 KernelSU terbaru / newest KernelSU\n\n" + "\n".join(f"- 🇮🇩 {line}" for line in lines) + "\n"
              + "\n".join(f"- 🇬🇧 {line}" for line in lines_en) + "\n")
    print(report)
    if args.summary:
        with args.summary.open("a", encoding="utf-8") as out:
            out.write(report + "\n")
    if args.out:
        with args.out.open("a", encoding="utf-8") as out:
            out.write(f"sha={head}\nmerged={merged}\nupstream={upstream}\nsusfs={susfs}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
