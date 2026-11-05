<h1 align="center">CoganSU · Xiaomi 14T Pro</h1>

<p align="center">
  <code>rothko</code> &nbsp;·&nbsp; HyperOS &nbsp;·&nbsp; Android 16 &nbsp;·&nbsp; root manager based on KernelSU
</p>

<p align="center">
  <a href="README.md"><img src="https://flagcdn.com/24x18/id.png" width="22" alt=""> Bahasa Indonesia</a> &nbsp;·&nbsp; <b><img src="https://flagcdn.com/24x18/gb.png" width="22" alt=""> English</b>
</p>

<p align="center">
  <a href="https://github.com/cogan17/CoganSU/releases"><b>⬇️ Download CoganSU</b></a>
  &nbsp;·&nbsp;
  <a href="docs/pasang.en.md"><b>📖 Guide</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/cogan17/CoganSU/releases"><b>📝 Release Notes</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/cogan17/Cogan-Kernel"><b>🐧 Cogan Kernel</b></a>
  &nbsp;·&nbsp;
  <a href="https://t.me/coganTWRP"><b>💬 Telegram Community</b></a>
</p>

> [!WARNING]
> The maintainer/developer is not responsible for bootloops or bricked devices. Device owners are responsible for their own devices! Please back up your data.

> [!IMPORTANT]
> CoganSU only works on a **Cogan Kernel built with CoganSU** from the same commit. On a kernel with official KernelSU, KernelSU Next, BakaSU or SukiSU this app is not recognised (and vice versa).

> [!TIP]
> TWRP is available for the Xiaomi 14T Pro. You can download [TWRP](https://github.com/cogan17/TWRP-14T-PRO).

---

### 📖 User Guide

|   |   |
|---|---|
| 📥 **[Install CoganSU](docs/pasang.en.md)** | Flash the CoganSU variant of Cogan Kernel, then install the app |
| 🐧 **[Cogan Kernel](https://github.com/cogan17/Cogan-Kernel)** | The kernel CoganSU pairs with, for the Xiaomi 14T Pro |
| ↩️ **[Back to Stock](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/kembali-ke-stok.en.md)** | Undo the kernel / fix a bootloop |
| 🩺 **[Problems & Logs](docs/pasang.en.md#problems)** | If something goes wrong |

---

<details>
<summary>📱 <b>What the app has</b></summary>

- ✅ The **KernelSU** (tiann) root manager with an identity of its own: name **CoganSU**, package `com.cogan.su`, signed with the CoganSU release key
- ✅ Made for the **Xiaomi 14T Pro (`rothko`)**: other phones get a red warning on Home (checked from the device codename, not from the kernel name, which can be disguised)
- ✅ A **Cogan Kernel** row on Home, right under Manager version: the real kernel version and the kernel build time, read from **Cogan Bridge** (`/sys/kernel/cogan`). **Kernel version** below it stays `uname`, so it shows the spoofed version when SUSFS spoofs it
- ✅ Opens straight into Home, without a splash logo
- ✅ No "GKI mode is for testing only" notice: Cogan Kernel builds root into the kernel on purpose
- ✅ Download Cogan Kernel, installation guide and Telegram community links on Home; the update check reads CoganSU releases
- ✅ Superuser, App Profile, modules, metamodules and WebUI work as in KernelSU; modules and the allowlist in `/data/adb` are kept when moving from KernelSU
- ✅ arm64 only, Cogan logo icon

</details>

<details>
<summary>🔐 <b>Why it only works on the CoganSU variant of Cogan Kernel</b></summary>

The kernel trusts exactly one manager app: the one whose **package name and signing certificate** match those built into the kernel (`kernel/cogansu.mk`). For official KernelSU that is `me.weishu.kernelsu` with weishu's key; for CoganSU it is `com.cogan.su` with the CoganSU release key. Therefore:

- The CoganSU APK and the CoganSU variant of Cogan Kernel must come from the **same CoganSU commit** (both versions are `30000 + commit count`).
- Another app copying CoganSU's name cannot become the manager, since it lacks the release key.
- A kernel build stops while the certificate is not filled in, so the kernel never silently trusts another manager.

</details>

<details>
<summary>⚙️ <b>Workflows</b></summary>

All run from a button in the *Actions* tab only, with no schedule.

| Workflow | What it does |
|---|---|
| **Build CoganSU** | Builds `ksud` and the APK signed with the release key, then checks APK certificate = key = `kernel/cogansu.mk`. Form like Cogan Kernel's Builder: **Release Type** `CI` (artifacts only) / `Pre-Release` / `Release` (published right away) and **Build Revision** `rN` (release `vYY.MM.DD-rN`; empty = run number). |
| **Rilis dari run** | Publishes a green Build CoganSU run of `main` later, after checking SHA256 and the certificate. Release notes and **ready-to-copy Telegram text** (Indonesian and English) are on the Summary page. Tick **diuji di HP** only when the owner has tested it; **timpa** to replace a release with the same tag; **teks_saja** for notes and Telegram text without publishing. |
| **Cek pembaruan** | Checks only, builds nothing: newest KernelSU (commits not merged yet), whether the SUSFS patch still fits (now and after merging the newest KernelSU), the last CoganSU release and build, Cogan Bridge and the integration in Cogan Kernel. |

The release key is created once by the owner and kept as repository secrets: **[docs/kunci.en.md](docs/kunci.en.md)**. Technical notes for Cogan Kernel: **[docs/integrasi-cogan-kernel.md](docs/integrasi-cogan-kernel.md)**.

</details>

<details>
<summary>🧩 <b>Repository layout</b></summary>

| Folder | Contents |
|---|---|
| `kernel/` | the root driver (KernelSU) that Cogan Kernel builds in; manager identity in [`kernel/cogansu.mk`](kernel/cogansu.mk) |
| `manager/` | the CoganSU app (Kotlin/Compose); Cogan-specific code in `manager/app/src/main/java/me/weishu/kernelsu/cogan/` |
| `userspace/ksud/` | the `ksud` daemon (Rust) shipped inside the APK |
| `scripts/` | certificate (`cogansu_cert.py`), release from run, update check |
| `.github/workflows/` | Build CoganSU, Rilis dari run, Cek pembaruan |

KernelSU's commit history is kept as is, with CoganSU's changes on top, so KernelSU updates can be merged at any time.

</details>

<details>
<summary>🧪 <b>Test status</b></summary>

| Build | Result |
|---|---|
| [`v26.11.04-r3`](https://github.com/cogan17/CoganSU/releases/tag/v26.11.04-r3) (Release) + Cogan Kernel r38 | ✅ Tested by the owner on the phone, 4 Nov 2026 ~18:37 WIB: **Working**, Cogan Kernel card · SUSFS · NoMount · BBG |
| `main` `v3.3.1-51-gae06aba8` (CI build, not released) + Cogan Kernel r38 | ✅ Tested by the owner on the phone, 4 Nov 2026 ~22:41 WIB: Cogan Kernel under Manager version with the real version, Kernel version shows the SUSFS-spoofed version, no splash logo |

</details>

---

<p align="center">
  Maintainer <b>Cogan17</b> &nbsp;·&nbsp; based on <b><a href="https://github.com/tiann/KernelSU">KernelSU</a></b> by weishu and its contributors &nbsp;·&nbsp; <b>SUSFS</b> &nbsp;·&nbsp; <b>Cogan Kernel</b>
  <br>Licence: kernel GPL-2.0-only (<a href="kernel/LICENSE">kernel/LICENSE</a>), the rest GPL-3.0-or-later (<a href="LICENSE">LICENSE</a>)
</p>
