# 📥 Install CoganSU

[← Back to README](../README.en.md) · <img src="https://flagcdn.com/24x18/id.png" width="22" alt=""> [Bahasa Indonesia](pasang.md)

CoganSU is the root manager app. Root itself lives inside the **CoganSU variant of Cogan Kernel**, so both are installed together and must come from the **same CoganSU commit** (see the release notes).

---

## Before you start

- Bootloader **unlocked**, phone **Xiaomi 14T Pro (`rothko`)**.
- **Back up `boot`** first and also keep the stock `boot.img`, as in the [Cogan Kernel guide](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/pasang.en.md).
- Have two files from the same release: the **CoganSU variant of Cogan Kernel** ZIP and the **`CoganSU_…-release.apk`** APK.

## 1. Flash the CoganSU variant of Cogan Kernel

Follow [Install the Kernel](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/pasang.en.md) (Kernel Flasher or TWRP), then reboot.

## 2. Install the CoganSU app

1. Install `CoganSU_…-release.apk`.
2. The old root manager app (KernelSU, KernelSU Next, BakaSU, SukiSU) can be removed: the CoganSU kernel no longer recognises it. **Modules and the root allowlist in `/data/adb` stay**, no need to reinstall them.

## 3. After boot

Open CoganSU and check Home:

- A green **Working** card in **GKI** mode.
- A **Cogan Kernel** card: kernel version and feature line, e.g. `SUSFS · NoMount · BBG`.
- No red *"CoganSU is made only for Xiaomi 14T Pro"* card.

## Back to another root manager

Flash another variant of Cogan Kernel (e.g. KernelSU Next) or your `boot` backup, then install the manager app that matches that ZIP. Modules in `/data/adb` stay.

## Problems

| Symptom | Cause and fix |
|---|---|
| Home: **Not installed** / **Unsupported** | The kernel is not the CoganSU variant, or the APK and kernel come from different commits/releases. Flash the Cogan Kernel ZIP from the same release as the APK. |
| Cogan Kernel card: *"Waiting for root to read Cogan Bridge"* | Root is not active for the app yet; reopen CoganSU once the **Working** card is green. |
| Cogan Kernel card: *"This kernel is not Cogan Kernel"* | The running kernel is not Cogan Kernel (e.g. stock `boot`). |
| Red card *"This Cogan Kernel was built for …"* | A kernel ZIP for another phone. Restore your `boot` backup. |
| Bootloop | [Back to Stock](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/kembali-ke-stok.en.md) with TWRP or stock `boot`. |

Report in the [Telegram community](https://t.me/coganTWRP) with the CoganSU version (Home → *Manager version*), the kernel version, and logs as in [Cogan Kernel's Problems & Logs](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/masalah-dan-log.en.md).
