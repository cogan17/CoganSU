<h1 align="center">CoganSU · Xiaomi 14T Pro</h1>

<p align="center">
  <code>rothko</code> &nbsp;·&nbsp; root manager &nbsp;·&nbsp; based on <a href="https://github.com/tiann/KernelSU">KernelSU</a>
</p>

<p align="center">
  <a href="README.md">Bahasa Indonesia</a> &nbsp;·&nbsp; <b>English</b>
</p>

<p align="center">
  <a href="https://github.com/cogan17/CoganSU/releases"><b>⬇️ Download CoganSU</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/cogan17/Cogan-Kernel"><b>🐧 Cogan Kernel</b></a>
  &nbsp;·&nbsp;
  <a href="https://t.me/coganTWRP"><b>💬 Telegram community</b></a>
</p>

> [!WARNING]
> The maintainer/developer is not responsible for bootloops or bricks. You are responsible for your own device. Back up your data.

---

CoganSU is a root manager of our own for the **Xiaomi 14T Pro (rothko)**, built from KernelSU's code and paired with **[Cogan Kernel](https://github.com/cogan17/Cogan-Kernel)**.

### How it differs from official KernelSU

| | Official KernelSU | CoganSU |
|---|---|---|
| App name / package | KernelSU · `me.weishu.kernelsu` | CoganSU · `com.cogan.su` |
| Signing key | weishu's key | CoganSU's own release key |
| Devices | any GKI | Xiaomi 14T Pro (`rothko`) only; other phones get a warning |
| Home | "GKI mode is for testing" notice, donation, learn KernelSU | **Cogan Kernel** card (kernel version and SUSFS · NoMount · BBG from Cogan Bridge), kernel download, guide, Telegram |
| Update check | KernelSU releases | CoganSU releases |
| Architectures | arm64, x86_64, riscv64 | arm64 only |

> [!IMPORTANT]
> The kernel only accepts the manager whose **package name and signing certificate** match the ones built into it. CoganSU therefore only works on a **Cogan Kernel built with CoganSU**. On a kernel with official KernelSU the CoganSU app is not recognised (and vice versa). Modules and the root allowlist in `/data/adb` are kept.

### Layout

| Folder | Contents |
|---|---|
| `kernel/` | the root driver (KernelSU) that Cogan Kernel builds in; manager identity in [`kernel/cogansu.mk`](kernel/cogansu.mk) |
| `manager/` | the CoganSU app (Kotlin/Compose); Cogan-specific code in `manager/app/src/main/java/me/weishu/kernelsu/cogan/` |
| `userspace/ksud/` | the `ksud` daemon (Rust) shipped inside the APK |
| `scripts/cogansu_cert.py` | computes the signing certificate's size and hash, fills `kernel/cogansu.mk` |
| `.github/workflows/` | manual workflows: **Build CoganSU**, **Rilis dari run** (release from run), **Cek pembaruan** (update check) |

### Building

1. Once: create the release key and store it as repository secrets, see **[docs/kunci.en.md](docs/kunci.en.md)**.
2. Run the **Build CoganSU** workflow (*Actions* tab). The APK is the `CoganSU_…-release.apk` artifact (unzipped), next to `build-info`; the run summary shows the certificate's size and hash.
3. First run: put those values in `kernel/cogansu.mk` (`python3 scripts/cogansu_cert.py write <size> <hash>`), commit, and build again.
4. Publish with the **Rilis dari run** workflow, given the `run_id` of a green Build CoganSU run of `main` (only once `kernel/cogansu.mk` matches). Choose *Release* so the app's update check sees it; *teks_saja* only makes the notes and Telegram text.
5. Build Cogan Kernel from the same CoganSU commit, see **[docs/integrasi-cogan-kernel.md](docs/integrasi-cogan-kernel.md)**.

The **Cek pembaruan** workflow only checks: newest KernelSU (commits not merged yet), whether the SUSFS patch still fits (now and after merging the newest KernelSU), the last CoganSU release, the last build, and Cogan Bridge in Cogan Kernel.

### Credits and licence

CoganSU is derived from **[KernelSU](https://github.com/tiann/KernelSU)** by weishu (tiann) and its contributors. The kernel part is GPL-2.0-only, the rest GPL-3.0-or-later (see [`LICENSE`](LICENSE) and [`kernel/LICENSE`](kernel/LICENSE)). KernelSU's commit history is kept as is, with CoganSU's changes on top, so KernelSU updates can be merged at any time.
