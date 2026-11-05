<h1 align="center">CoganSU · Xiaomi 14T Pro</h1>

<p align="center">
  <code>rothko</code> &nbsp;·&nbsp; HyperOS &nbsp;·&nbsp; Android 16 &nbsp;·&nbsp; root manager berbasis KernelSU
</p>

<p align="center">
  <b><img src="https://flagcdn.com/24x18/id.png" width="22" alt=""> Bahasa Indonesia</b> &nbsp;·&nbsp; <a href="README.en.md"><img src="https://flagcdn.com/24x18/gb.png" width="22" alt=""> English</a>
</p>

<p align="center">
  <a href="https://github.com/cogan17/CoganSU/releases"><b>⬇️ Unduh CoganSU</b></a>
  &nbsp;·&nbsp;
  <a href="docs/pasang.md"><b>📖 Panduan</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/cogan17/CoganSU/releases"><b>📝 Catatan Rilis</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/cogan17/Cogan-Kernel"><b>🐧 Cogan Kernel</b></a>
  &nbsp;·&nbsp;
  <a href="https://t.me/coganTWRP"><b>💬 Komunitas Telegram</b></a>
</p>

> [!WARNING]
> Jika terjadi bootloop atau brick, bukan tanggung jawab maintainer/developer. Pemilik device bertanggung jawab atas devicenya sendiri! Harap cadangkan datamu.

> [!IMPORTANT]
> CoganSU hanya jalan di **Cogan Kernel yang dibangun dengan CoganSU** dari commit yang sama. Di kernel dengan KernelSU resmi, KernelSU Next, BakaSU atau SukiSU, aplikasi ini tidak dikenali (dan sebaliknya).

> [!TIP]
> TWRP sudah tersedia untuk Xiaomi 14T Pro. Kamu bisa mendownload [TWRP](https://github.com/cogan17/TWRP-14T-PRO).

---

### 📖 Panduan Pengguna

|   |   |
|---|---|
| 📥 **[Pasang CoganSU](docs/pasang.md)** | Flash Cogan Kernel varian CoganSU, lalu pasang aplikasinya |
| 🐧 **[Cogan Kernel](https://github.com/cogan17/Cogan-Kernel)** | Kernel pasangan CoganSU untuk Xiaomi 14T Pro |
| ↩️ **[Kembali ke Stok](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/kembali-ke-stok.md)** | Batalkan kernel / perbaiki bootloop |
| 🩺 **[Masalah & Log](docs/pasang.md#masalah)** | Kalau ada kendala |

---

<details>
<summary>📱 <b>Isi aplikasi</b></summary>

- ✅ Root manager **KernelSU** (tiann) dengan identitas sendiri: nama **CoganSU**, paket `com.cogan.su`, ditandatangani kunci rilis CoganSU
- ✅ Khusus **Xiaomi 14T Pro (`rothko`)**: HP lain diberi peringatan merah di Beranda (dicek dari kode perangkat, bukan dari nama kernel yang bisa disamarkan)
- ✅ Baris **Cogan Kernel** di Beranda, tepat di bawah Versi manajer: versi kernel asli dan waktu build kernel, dibaca dari **Cogan Bridge** (`/sys/kernel/cogan`). **Versi kernel** di bawahnya tetap `uname`, jadi menampilkan versi samaran bila SUSFS menyamarkannya
- ✅ Langsung masuk ke Beranda saat dibuka, tanpa logo pembuka
- ✅ Tanpa peringatan "mode GKI hanya untuk pengujian": Cogan Kernel memang membangun root ke dalam kernel
- ✅ Tautan Unduh Cogan Kernel, Panduan pasang dan Komunitas Telegram di Beranda; cek pembaruan membaca rilis CoganSU
- ✅ Superuser, App Profile, modul, metamodule dan WebUI sama dengan KernelSU; modul dan daftar izin di `/data/adb` tetap dipakai saat pindah dari KernelSU
- ✅ Hanya arm64, ikon logo Cogan

</details>

<details>
<summary>🔐 <b>Kenapa hanya jalan di Cogan Kernel varian CoganSU</b></summary>

Kernel hanya memercayai satu aplikasi manager: yang **nama paket dan sertifikat tanda tangannya** sama dengan yang dibangun ke dalam kernel (`kernel/cogansu.mk`). Untuk KernelSU resmi itu `me.weishu.kernelsu` dengan kunci weishu; untuk CoganSU `com.cogan.su` dengan kunci rilis CoganSU. Karena itu:

- APK CoganSU dan Cogan Kernel varian CoganSU harus dari **commit CoganSU yang sama** (versi keduanya `30000 + jumlah commit`).
- Aplikasi lain yang meniru nama CoganSU tidak bisa menjadi manager, karena tidak punya kunci rilisnya.
- Build kernel berhenti bila sertifikat belum diisi, jadi kernel tidak pernah diam-diam memercayai manager lain.

</details>

<details>
<summary>⚙️ <b>Workflow</b></summary>

Semua hanya lewat tombol di tab *Actions*, tanpa jadwal.

| Workflow | Fungsi |
|---|---|
| **Build CoganSU** | Membangun `ksud` dan APK bertanda tangan kunci rilis, lalu mengecek sertifikat APK = kunci = `kernel/cogansu.mk`. Form seperti Builder Cogan Kernel: **Release Type** `CI` (artifact saja) / `Pre-Release` / `Release` (langsung terbit) dan **Build Revision** `rN` (rilis `vYY.MM.DD-rN`; kosong = nomor run). |
| **Rilis dari run** | Menerbitkan run Build CoganSU yang hijau dari `main` belakangan, setelah cek SHA256 dan sertifikat. Catatan rilis dan **Teks Telegram siap salin** (Indonesia dan English) ada di halaman Summary. Centang **diuji di HP** hanya bila owner sudah mengujinya; **timpa** untuk mengganti rilis dengan tag yang sama; **teks_saja** untuk catatan dan teks Telegram tanpa menerbitkan. |
| **Cek pembaruan** | Hanya mengecek, tidak membangun: KernelSU terbaru (commit yang belum digabung), apakah patch SUSFS masih cocok (sekarang dan bila KernelSU terbaru digabung), rilis dan build CoganSU terakhir, Cogan Bridge dan integrasi di Cogan Kernel. |
| **Hapus log run lama** | Bersih-bersih tab *Actions*. Bawaan cek saja; ketik `HAPUS` untuk menghapus run yang lebih tua dari **simpan_hari** (bawaan 7) dan run workflow yang sudah tidak ada. Artifact run yang dihapus ikut hilang, jadi terbitkan dulu build yang mau dirilis. |

Kunci rilis dibuat sekali oleh owner dan disimpan sebagai secret repo: **[docs/kunci.md](docs/kunci.md)**. Catatan teknis untuk Cogan Kernel: **[docs/integrasi-cogan-kernel.md](docs/integrasi-cogan-kernel.md)**.

</details>

<details>
<summary>🧩 <b>Isi repo</b></summary>

| Folder | Isi |
|---|---|
| `kernel/` | driver root (KernelSU) yang dipasang Cogan Kernel; identitas manager di [`kernel/cogansu.mk`](kernel/cogansu.mk) |
| `manager/` | aplikasi CoganSU (Kotlin/Compose); bagian khusus Cogan di `manager/app/src/main/java/me/weishu/kernelsu/cogan/` |
| `userspace/ksud/` | daemon `ksud` (Rust) yang ikut di dalam APK |
| `scripts/` | sertifikat (`cogansu_cert.py`), Rilis dari run, Cek pembaruan |
| `.github/workflows/` | Build CoganSU, Rilis dari run, Cek pembaruan |

Riwayat commit KernelSU disimpan apa adanya; perubahan CoganSU ada di atasnya, sehingga pembaruan dari KernelSU bisa digabung kapan saja.

</details>

<details>
<summary>🧪 <b>Status uji</b></summary>

| Build | Hasil |
|---|---|
| [`v26.11.04-r3`](https://github.com/cogan17/CoganSU/releases/tag/v26.11.04-r3) (Release) + Cogan Kernel r38 | ✅ Diuji owner di HP 4 Nov 2026 ±18.37 WIB: **Berfungsi**, kartu Cogan Kernel · SUSFS · NoMount · BBG |
| `main` `v3.3.1-51-gae06aba8` (build CI, belum dirilis) + Cogan Kernel r38 | ✅ Diuji owner di HP 4 Nov 2026 ±22.41 WIB: Cogan Kernel di bawah Versi manajer dengan versi asli, Versi kernel menampilkan versi samaran SUSFS, tanpa logo pembuka |

</details>

---

<p align="center">
  Maintainer <b>Cogan17</b> &nbsp;·&nbsp; berbasis <b><a href="https://github.com/tiann/KernelSU">KernelSU</a></b> karya weishu dan kontributornya &nbsp;·&nbsp; <b>SUSFS</b> &nbsp;·&nbsp; <b>Cogan Kernel</b>
  <br>Lisensi: kernel GPL-2.0-only (<a href="kernel/LICENSE">kernel/LICENSE</a>), selebihnya GPL-3.0-or-later (<a href="LICENSE">LICENSE</a>)
</p>
