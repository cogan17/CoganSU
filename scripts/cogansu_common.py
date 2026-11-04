"""Shared helpers for the CoganSU workflows (Rilis dari run, Cek pembaruan)."""
import datetime
import json
import re
import subprocess
import urllib.request

WIB = datetime.timezone(datetime.timedelta(hours=7))
UPSTREAM_REPO = "tiann/KernelSU"
UPSTREAM_URL = f"https://github.com/{UPSTREAM_REPO}.git"
KERNEL_REPO = "cogan17/Cogan-Kernel"
SUSFS_PROJECT = "simonpunk%2Fsusfs4ksu"
SUSFS_BRANCH = "gki-android14-6.1"
SUSFS_KSU_PATCH = "kernel_patches/KernelSU/10_enable_susfs_for_ksu.patch"
# Release tags, like Cogan Kernel: v<YY.MM.DD>-r<Build Revision> (UTC date of the build).
RELEASE_TAG = re.compile(r"^v\d{2}\.\d{2}\.\d{2}-r\d+$")

# Which part of KernelSU a commit touches, by path (most specific first).
AREAS = (
    ("kernel/", "kernel"),
    ("manager/", "manager"),
    ("userspace/", "ksud"),
    ("uapi/", "ksud"),
    ("js/", "webui"),
)
AREA_ORDER = ("kernel", "manager", "ksud", "webui", "lain")
AREA_WORDS = {
    "id": {"kernel": "kernel", "manager": "aplikasi", "ksud": "ksud", "webui": "WebUI", "lain": "lain-lain"},
    "en": {"kernel": "kernel", "manager": "app", "ksud": "ksud", "webui": "WebUI", "lain": "other"},
}


def read_env(path):
    values = {}
    for line in open(path, encoding="utf-8"):
        line = line.rstrip("\n")
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value
    return values


def gh(path):
    """GitHub API through the gh CLI (GH_TOKEN in the workflow); None when it cannot be read."""
    try:
        result = subprocess.run(["gh", "api", path], capture_output=True, text=True, timeout=120)
        return json.loads(result.stdout) if result.returncode == 0 and result.stdout.strip() else None
    except (OSError, subprocess.TimeoutExpired, json.JSONDecodeError):
        return None


def gitlab(path):
    try:
        with urllib.request.urlopen("https://gitlab.com/api/v4/" + path, timeout=60) as reply:
            return json.load(reply)
    except (OSError, ValueError):
        return None


def git(*args, cwd=None, check=True):
    result = subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True)
    if check and result.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)}: {result.stderr.strip()}")
    return result.stdout.strip() if result.returncode == 0 else None


MONTHS = {
    "id": ("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"),
    "en": ("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
}


def wib(stamp, lang="id"):
    """ISO-8601 time (UTC from GitHub/GitLab, or with an offset from git) as '4 Nov 2026 17.29 WIB'."""
    if not stamp:
        return "-"
    moment = datetime.datetime.fromisoformat(stamp.replace("Z", "+00:00")).astimezone(WIB)
    return f"{moment.day} {MONTHS[lang][moment.month - 1]} {moment.year} {moment:%H.%M} WIB"


def area(paths):
    for prefix, name in AREAS:
        if any(path.startswith(prefix) for path in paths):
            return name
    return "lain"


def commits(rev_range, cwd=None):
    """[(sha, subject, body, area)] for a rev range, oldest first."""
    out = git("log", "--reverse", "--format=%x1e%H%x1f%s%x1f%b%x1f", "--name-only", rev_range, cwd=cwd)
    result = []
    for record in (out or "").split("\x1e"):
        if not record.strip():
            continue
        sha, subject, body, files = (record.split("\x1f") + ["", "", "", ""])[:4]
        paths = [line for line in files.splitlines() if line.strip()]
        result.append((sha.strip(), subject.strip(), body.strip(), area(paths)))
    return result


def area_counts(items, lang):
    counts = {}
    for *_, name in items:
        counts[name] = counts.get(name, 0) + 1
    return ", ".join(f"{AREA_WORDS[lang][name]} {counts[name]}" for name in AREA_ORDER if name in counts)


def trailer(body, key):
    match = re.search(rf"^{re.escape(key)}:\s*(.+)$", body or "", re.M)
    return match.group(1).strip() if match else ""
