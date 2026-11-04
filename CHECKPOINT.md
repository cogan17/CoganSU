# Checkpoint — 4 Nov 2026, sekitar 22.55 WIB

## Sedang berjalan

Tidak ada pekerjaan atau run yang sedang berjalan. Owner pindah ke sesi TWRP dan OFOX.

## Sudah selesai sejak checkpoint 18.40 WIB

- **#13** (`8add986`) `feat(manager)`: baris Cogan Kernel pindah ke kartu info di bawah Versi manajer (bukan tautan); isinya versi asli dari Cogan Bridge, Versi kernel tetap `uname` (versi samaran SUSFS). Splash tanpa logo dan tanpa jeda 500 ms. Build CoganSU CI [run #8](https://github.com/cogan17/CoganSU/actions/runs/41857680134) hijau; **diuji owner di HP ±22.41 WIB** (`v3.3.1-51-gae06aba8` + Cogan Kernel r38): semua sesuai, logo sudah tidak ada.
- **#14** (`970116c`) `docs`: README ID/EN sesuai Beranda terbaru dan tabel Status uji.
- Issue #3 diperbarui (langkah dan catatan).
- Auto cut charging Cogan Smart (repo Cogan-Kernel): **berfungsi**. `events.tsv`: cas diputus di 95% (19.30 WIB), lanjut di 90% (20.41 WIB). Angka 100% di layar hanya masalah tampilan (dikonfirmasi owner); modul tidak diubah. Batas suhu Kernel Default di HP tersimpan 39 °C (bawaan 41 °C), CPU besar sering ditahan 60%.
- KernelSU hulu `main`: tidak ada commit baru yang belum digabung (±22.50 WIB).

## Tertunda

- **Owner yang menjalankan workflow** (owner, 4 Nov: "nanti biar saya saja yg run workflow"). Asisten tidak menjalankan workflow tanpa diminta.
- Rilis Release/Latest CoganSU (supaya terbaca cek pembaruan aplikasi), bila owner meminta.
- *Opsional, owner:* secret `KEY_ALIAS` `CoganSU` → `cogansu`.
- **Pembaruan HyperOS `3.0.302.0.WNNIDXM`** (patch keamanan Oktober 2026) sedang diunduh di HP owner (sebelumnya `OS3.0.301.0`). OTA mengganti `boot`, jadi Cogan Kernel dan root hilang sesudah update; perlu flash ulang Cogan Kernel (cek KMI/kecocokan dulu di repo Cogan-Kernel). Keputusan owner.

## Langkah pertama berikutnya

- Baca `CLAUDE.md`, lalu tunggu instruksi owner.

---

# Checkpoint — 4 Nov 2026, sekitar 18.40 WIB

## Sedang berjalan

Tidak ada pekerjaan atau run yang sedang berjalan.

## Sudah selesai sejak checkpoint siang

- **#8** (`5ef9ac2`) `fix(kernel)`: sertifikat kunci rilis RSA 4096 berukuran 1261 byte melebihi batas 1024 byte `kernel/manager/apk_sign.c` hulu, jadi kernel menolak manager ("Tidak terpasang", dmesg `is_manager: 0` tanpa baris `expected:`). Batas jadi 2048, buffer pindah ke `kmalloc`; ukuran dan hash tetap dicek. Compile lokal driver + SUSFS `a0ac906` lolos; patch SUSFS tetap cocok.
- **#9** (`f26b1b9`) `fix(manager)`: kartu Cogan Kernel menulis "bukan Cogan Kernel" karena `ShellUtils.fastCmd` hanya mengembalikan baris terakhir; kini semua baris dibaca lewat `newJob()`.
- Rilis `v26.11.04-r2` (Pre-Release, Build #3) dan `v26.11.04-r3` (Pre-Release, Build #4; job Terbit diulang sekali karena GitHub 502).
- **Diuji di HP owner 4 Nov ±18.37 WIB**: Cogan Kernel r38 (Builder run 41819469210, root CoganSU r3) + manager r3 → **Berfungsi** (`v3.3.1-49-gf26b1b9e`), kartu Cogan Kernel `6.1.176-Cogan-Rothko-ACK-r38-5be85323d` · SUSFS · NoMount · BBG.

## Tertunda

- Jadikan `v26.11.04-r3` Release (bukan Pre-Release) supaya terbaca cek pembaruan aplikasi, bila owner meminta; Cogan Kernel r38 diterbitkan bersamaan lewat Rilis dari run di repo Cogan-Kernel.

## Langkah pertama berikutnya

- Baca `CLAUDE.md`, lalu tunggu instruksi owner.

---

# Checkpoint — 4 Nov 2026, sekitar 11.30 WIB

## Sedang berjalan

Tidak ada pekerjaan atau run yang sedang berjalan.

## Sudah selesai (semua di `main`)

- Source KernelSU `28db74f6` (riwayat dipertahankan) + identitas CoganSU: aplikasi `com.cogan.su`, hanya arm64, ikon logo Cogan, Beranda (cek `rothko`, kartu Cogan Kernel dari Cogan Bridge, tautan kernel/panduan/Telegram), cek pembaruan ke repo ini.
- Kernel: `kernel/cogansu.mk` berisi sertifikat kunci rilis `0x04ed` `b0fb06caa39728fa16f4a005712dad3c31613568ae4921dd73b16075c28aec6f` (#5). **Kunci rilis tidak boleh diganti lagi.** Patch SUSFS KernelSU (`gki-android14-6.1` `a0ac906`) terpasang bersih.
- Workflow manual: Build CoganSU (Release Type, Build Revision), Rilis dari run (timpa, teks_saja), Cek pembaruan (#1, #4).
- README seperti Cogan Kernel + `docs/pasang.md` (#2), aturan `CLAUDE.md` termasuk gaya balasan (#6).
- Secret kunci rilis terisi oleh owner (`KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- Build CoganSU pertama hijau: [run 41769640596](https://github.com/cogan17/CoganSU/actions/runs/41769640596), APK `v3.3.1-43-g6ba22980` (32723) ter-compile di CI. Belum diuji di HP.
- Issue pelacak: [#3 Menuju rilis pertama CoganSU](https://github.com/cogan17/CoganSU/issues/3).

## Tertunda

- Rilis dari run 41769640596 ditolak (benar: dibangun sebelum `kernel/cogansu.mk` terisi). Perlu Build CoganSU baru dari `main`: Pre-Release `r1` langsung, atau CI lalu Rilis dari run.
- *Opsional, owner:* secret `KEY_ALIAS` berisi `CoganSU`, sehingga nama itu disensor `***` di log. Ganti isinya menjadi `cogansu` (kunci tetap sama).
- **Integrasi Cogan Kernel** (root `CSU` di Builder, rancangan `docs/integrasi-cogan-kernel.md`): menunggu owner mengizinkan akses tulis ke `cogan17/Cogan-Kernel`. Tanpa ini aplikasi belum bisa dipakai di HP.
- Uji HP (CoganSU + Cogan Kernel dari commit yang sama), lalu rilis Release dan baris *Status uji* README.

## Langkah pertama berikutnya

Tanya owner: lanjut integrasi ke Cogan-Kernel (minta akses repo lewat `add_repo` push), atau jalankan Build CoganSU baru dulu untuk Pre-Release `r1`.
