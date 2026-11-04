<h1 align="center">CoganSU · Xiaomi 14T Pro</h1>

<p align="center">
  <code>rothko</code> &nbsp;·&nbsp; root manager &nbsp;·&nbsp; berbasis <a href="https://github.com/tiann/KernelSU">KernelSU</a>
</p>

<p align="center">
  <b>Bahasa Indonesia</b> &nbsp;·&nbsp; <a href="README.en.md">English</a>
</p>

<p align="center">
  <a href="https://github.com/cogan17/CoganSU/releases"><b>⬇️ Unduh CoganSU</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/cogan17/Cogan-Kernel"><b>🐧 Cogan Kernel</b></a>
  &nbsp;·&nbsp;
  <a href="https://t.me/coganTWRP"><b>💬 Komunitas Telegram</b></a>
</p>

> [!WARNING]
> Jika terjadi bootloop atau brick, bukan tanggung jawab maintainer/developer. Pemilik device bertanggung jawab atas devicenya sendiri! Harap cadangkan datamu.

---

CoganSU adalah root manager versi sendiri untuk **Xiaomi 14T Pro (rothko)**, dibangun dari kode KernelSU dan dipasangkan dengan **[Cogan Kernel](https://github.com/cogan17/Cogan-Kernel)**.

### Bedanya dengan KernelSU resmi

| | KernelSU resmi | CoganSU |
|---|---|---|
| Nama aplikasi / paket | KernelSU · `me.weishu.kernelsu` | CoganSU · `com.cogan.su` |
| Kunci tanda tangan | kunci weishu | kunci rilis CoganSU sendiri |
| Perangkat | semua GKI | khusus Xiaomi 14T Pro (`rothko`); HP lain diberi peringatan |
| Beranda | peringatan "mode GKI hanya untuk pengujian", donasi, belajar KernelSU | kartu **Cogan Kernel** (versi kernel dan fitur SUSFS · NoMount · BBG dari Cogan Bridge), tautan unduh kernel, panduan, Telegram |
| Cek pembaruan | rilis KernelSU | rilis CoganSU |
| Arsitektur | arm64, x86_64, riscv64 | arm64 saja |

> [!IMPORTANT]
> Kernel hanya mengenali manager yang **nama paket dan sertifikat tanda tangannya** cocok dengan yang dibangun ke dalam kernel. Jadi CoganSU hanya jalan di **Cogan Kernel yang dibangun dengan CoganSU**. Di kernel dengan KernelSU resmi, aplikasi CoganSU tidak dikenali (dan sebaliknya). Modul dan daftar izin root di `/data/adb` tetap dipakai.

### Struktur

| Folder | Isi |
|---|---|
| `kernel/` | driver root (KernelSU) yang dipasang Cogan Kernel; identitas manager di [`kernel/cogansu.mk`](kernel/cogansu.mk) |
| `manager/` | aplikasi CoganSU (Kotlin/Compose); bagian khusus Cogan di `manager/app/src/main/java/me/weishu/kernelsu/cogan/` |
| `userspace/ksud/` | daemon `ksud` (Rust) yang ikut di dalam APK |
| `scripts/cogansu_cert.py` | menghitung ukuran dan hash sertifikat tanda tangan, mengisi `kernel/cogansu.mk` |
| `.github/workflows/build-cogansu.yml` | workflow **Build CoganSU** (manual) |

### Membangun

1. Sekali saja: buat kunci rilis dan simpan sebagai secret repo, lihat **[docs/kunci.md](docs/kunci.md)**.
2. Jalankan workflow **Build CoganSU** (tab *Actions*). Hasilnya APK di artifact `CoganSU`; ringkasan run menampilkan ukuran dan hash sertifikat.
3. Run pertama: isi `kernel/cogansu.mk` dengan nilai itu (`python3 scripts/cogansu_cert.py write <ukuran> <hash>`), commit, lalu build lagi.
4. Centang **rilis** untuk menerbitkan rilis GitHub (hanya bila `kernel/cogansu.mk` sudah cocok).
5. Bangun Cogan Kernel dari commit CoganSU yang sama, lihat **[docs/integrasi-cogan-kernel.md](docs/integrasi-cogan-kernel.md)**.

### Kredit dan lisensi

CoganSU adalah turunan dari **[KernelSU](https://github.com/tiann/KernelSU)** karya weishu (tiann) dan para kontributornya. Bagian kernel berlisensi GPL-2.0-only, selebihnya GPL-3.0-or-later (lihat [`LICENSE`](LICENSE) dan [`kernel/LICENSE`](kernel/LICENSE)). Riwayat commit KernelSU disimpan apa adanya; perubahan CoganSU ada di atasnya, sehingga pembaruan dari KernelSU bisa digabung kapan saja.
