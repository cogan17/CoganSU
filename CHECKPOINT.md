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
