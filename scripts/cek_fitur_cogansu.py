#!/usr/bin/env python3
"""Cek penjaga fitur CoganSU: build berhenti bila fitur CoganSU hilang dari tree, mis. setelah gabung
KernelSU main atau konflik yang salah diselesaikan. Ini gate, bukan kunci: KernelSU terbaru tetap digabung;
yang dicek hanya bahwa perubahan CoganSU masih ada (owner, 10 Nov 2026).

Guard for CoganSU's own features: the build stops when one of them is gone from the tree. Not a lock or
pin: the newest KernelSU is still merged, only CoganSU's changes are checked. Each feature PR adds its
check below; a check names the file and the exact text that must still be there.

Usage: cek_fitur_cogansu.py [--root DIR] [--summary FILE]   (exit 1 when a feature is missing)
"""
import argparse
import sys
from pathlib import Path

APP = "manager/app/src/main"
KT = f"{APP}/java/me/weishu/kernelsu"

# (feature, file, text that must be in that file)
CHECKS = [
    ("Identitas com.cogan.su / package", "manager/app/build.gradle.kts", 'else "com.cogan.su"'),
    ("Nama CoganSU / app name", "manager/app/build.gradle.kts", 'else "CoganSU"'),
    ("Kernel memercayai manager CoganSU / kernel trusts CoganSU", "kernel/Kbuild", "include $(MDIR)cogansu.mk"),
    ("Build kernel berhenti tanpa sertifikat / no certificate stops the kernel build", "kernel/Kbuild",
     "$(error CoganSU: the manager certificate hash is not set"),
    ("Sertifikat 1261 byte / 1261-byte certificate", "kernel/manager/apk_sign.c", "#define CERT_MAX_LENGTH 2048"),
    ("Identitas sertifikat / certificate identity", "kernel/cogansu.mk", "COGANSU_EXPECTED_HASH"),
    ("setup.sh mengkloning CoganSU / setup.sh clones CoganSU", "kernel/setup.sh", "https://github.com/cogan17/CoganSU"),
    ("Cek rothko dan Cogan Bridge / rothko check and Cogan Bridge", f"{KT}/cogan/Cogan.kt", 'BRIDGE_DIR = "/sys/kernel/cogan"'),
    ("Baris Cogan Kernel + waktu build (Material) / Cogan Kernel row", f"{KT}/ui/screen/home/HomeMaterial.kt",
     "coganKernelSummary(coganKernel)"),
    ("Baris Cogan Kernel + waktu build (Miuix) / Cogan Kernel row", f"{KT}/ui/screen/home/HomeMiuix.kt",
     "coganKernelSummary(coganKernel)"),
    ("Tautan Cogan Kernel, panduan, Telegram (Material) / links", f"{KT}/ui/screen/home/HomeMaterial.kt",
     "Cogan.TELEGRAM_URL"),
    ("Tautan Cogan Kernel, panduan, Telegram (Miuix) / links", f"{KT}/ui/screen/home/HomeMiuix.kt", "Cogan.TELEGRAM_URL"),
    ("Peringatan HP selain rothko / non-rothko warning", f"{KT}/ui/screen/home/HomeUiState.kt", "Cogan.isSupportedDevice()"),
    ("Tanpa peringatan mode GKI / no GKI-mode notice", f"{KT}/ui/screen/home/HomeUiState.kt",
     "val showGkiWarning: Boolean\n        get() = false"),
    ("Buka aplikasi tanpa jeda splash / no splash delay", f"{KT}/ui/MainActivity.kt",
     "splashScreen.setKeepOnScreenCondition { !contentReady }"),
    ("Tanpa logo pembuka / no splash logo", f"{APP}/res/values/themes.xml",
     '<item name="windowSplashScreenAnimatedIcon">@android:color/transparent</item>'),
    ("Tanpa logo pembuka (gelap) / no splash logo (dark)", f"{APP}/res/values-night/themes.xml",
     '<item name="windowSplashScreenAnimatedIcon">@android:color/transparent</item>'),
    ("Cek pembaruan dari rilis CoganSU / update check", f"{KT}/ui/util/Downloader.kt", "Cogan.MANAGER_LATEST_RELEASE_API"),
    ("Halaman Tentang CoganSU / About page", f"{KT}/ui/screen/about/AboutScreen.kt", "Cogan.MANAGER_REPO_URL"),
    ("Instal modul lewat intent selalu dikonfirmasi / module install always confirmed",
     f"{KT}/ui/navigation3/IntentDispatcher.kt", "// CoganSU: always ask."),
    ("Token intent tidak ikut backup / intent token not backed up", f"{APP}/res/xml/backup_rules.xml",
     'path="cogan_intent_token.xml"'),
    ("Token intent tidak ikut backup/pindah HP / not backed up or transferred", f"{APP}/res/xml/data_extraction_rules.xml",
     '<device-transfer>\n        <exclude domain="sharedpref" path="cogan_intent_token.xml" />'),
    ("Token intent di file sendiri / token in its own file",
     f"{KT}/data/repository/SettingsRepositoryImpl.kt", 'INTENT_TOKEN_PREFS = "cogan_intent_token"'),
    ("Teks CoganSU (EN) / strings", f"{APP}/res/values/strings_cogan.xml", 'name="cogan_kernel_title"'),
    ("Teks CoganSU (ID) / strings", f"{APP}/res/values-in/strings_cogan.xml", 'name="cogan_kernel_title"'),
    ("Ikon CoganSU / launcher icon", f"{APP}/res/mipmap-xxxhdpi/ic_launcher_cogan.png", None),
    ("WebUI hanya halaman modul / WebUI loads module pages only", f"{KT}/ui/webui/WebViewHelper.kt", "if (isModulePage(url)) return false"),
    ("WebUI tanpa frame web luar / WebUI blocks web frames", f"{KT}/ui/webui/SuFilePathHandler.java", "frame-src \x27self\x27 blob: data:; form-action \x27self\x27"),
]


def check(root):
    """The checks that fail, as (feature, file, why)."""
    missing = []
    for feature, rel, text in CHECKS:
        path = root / rel
        if not path.is_file():
            missing.append((feature, rel, "file hilang / file missing"))
        elif text is not None and text not in path.read_text(encoding="utf-8", errors="replace"):
            missing.append((feature, rel, "teks hilang / text missing"))
    return missing


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--summary", type=Path)
    args = parser.parse_args(argv)
    missing = check(args.root)
    if missing:
        lines = ["### ❌ Fitur CoganSU hilang / CoganSU features missing", "",
                 "- 🇮🇩 Build berhenti. Kembalikan fitur ini (biasanya konflik gabung KernelSU yang salah diselesaikan).",
                 "- 🇬🇧 Build stopped. Restore these features (usually a KernelSU merge conflict resolved the wrong way).", ""]
        lines += [f"- {feature}: `{rel}` ({why})" for feature, rel, why in missing]
        for feature, rel, why in missing:
            print(f"::error file={rel}::{feature}: {why}")
    else:
        lines = [f"### ✅ Fitur CoganSU lengkap / CoganSU features present ({len(CHECKS)} cek / checks)"]
    report = "\n".join(lines) + "\n"
    print(report)
    if args.summary:
        with args.summary.open("a", encoding="utf-8") as out:
            out.write(report + "\n")
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
