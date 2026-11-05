# Aturan untuk asisten AI di repo ini

**`cogan17/CoganSU` adalah repo utama root manager CoganSU** (owner, 4 Nov 2026: "Ini repo utama kita"). CoganSU adalah root manager versi sendiri milik owner, turunan [tiann/KernelSU](https://github.com/tiann/KernelSU), khusus Xiaomi 14T Pro (`rothko`) dan terintegrasi dengan [cogan17/Cogan-Kernel](https://github.com/cogan17/Cogan-Kernel). Aturan di bawah disalin dari `CLAUDE.md` Cogan-Kernel lalu disesuaikan untuk CoganSU; bila ada yang bertentangan dengan repo itu untuk urusan kernel, `CLAUDE.md` Cogan-Kernel yang berlaku.

## Bahasa (WAJIB)

- **Semua komunikasi asisten ke owner wajib bahasa Indonesia** (owner, 31 Okt 2026: "saya hanya mengerti bahasa ibu saya"). Ini mencakup balasan chat, penjelasan, ringkasan, pertanyaan, deskripsi PR, dan pesan commit. Jangan membalas owner dalam bahasa Inggris.
- **Deliverable untuk pengguna akhir tetap dua bahasa (Indonesia lalu English)**: catatan rilis, teks Telegram, summary workflow, README dan panduan di `docs/` (`*.md` Indonesia, `*.en.md` English). Bagian Indonesia menjadi acuan owner; bagian English untuk pengunduh rilis.
- **Isi tiap bahasa harus sesuai bahasanya**: bagian English tidak boleh menyalin teks Indonesia. Bila terjemahan belum ada, jangan mengarang; tulis rujukan sumbernya.
- Teks di aplikasi: English di `values/strings_cogan.xml`, Indonesia di `values-in/strings_cogan.xml`; keduanya selalu diperbarui bersama.

## Gaya balasan ke owner (owner, 4 Nov 2026)

- Kalimat pertama langsung menjawab inti.
- Tanpa kalimat pembuka, sapaan, atau kesimpulan di akhir. Jangan merangkum jawaban.
- Tanpa kata atau frasa klise AI (mis. *delve*, *tapestry*, *testament*, *seamless*, *crucial*, dan padanannya dalam bahasa Indonesia).
- Bahasa ringkas, kasual, dan konkret.

## Tanpa atribusi AI

- Author dan committer memakai identitas owner: `git config user.name Cogan` dan `git config user.email 79615344+cogan17@users.noreply.github.com`.
- Pesan commit **tanpa** `Co-Authored-By: Claude …`, `Claude-Session: …` atau atribusi AI lain.
- Deskripsi PR **tanpa** footer "Generated with Claude Code" atau tautan sesi.
- Branch kerja memakai nama biasa: `feat/…`, `fix/…`, `docs/…`, `chore/…`. Perubahan masuk ke `main` lewat PR.

## Waktu

- Waktu untuk owner selalu ditulis dalam **WIB** (Jakarta, UTC+7), misalnya "17.29 WIB". Kalau sumbernya UTC (GitHub, log), ubah dulu ke WIB.

## Isi repo (owner, 4 Nov 2026)

- **Dasar KernelSU resmi, riwayatnya dipertahankan.** CoganSU dimulai dari KernelSU `main` commit `28db74f6` (manager `v3.3.1-36-g28db74f6`, versi 32716, sama dengan yang dipakai owner di HP). Perubahan CoganSU berada di atas riwayat KernelSU supaya pembaruan hulu bisa digabung (`git merge` dari `tiann/KernelSU` `main`). Ubah kode hulu sesedikit mungkin.
- **Kode khusus Cogan dikumpulkan di tempat sendiri**: `manager/app/src/main/java/me/weishu/kernelsu/cogan/` (cek perangkat, Cogan Bridge, tautan), `values*/strings_cogan.xml`, `mipmap-*/ic_launcher_cogan.png`, `kernel/cogansu.mk`, `scripts/cogansu_cert.py`, `.github/workflows/build-cogansu.yml`, `docs/`. Perubahan pada file hulu ditandai komentar `CoganSU:`.
- **Identitas**: nama aplikasi `CoganSU`, `applicationId` `com.cogan.su` (PR build `com.cogan.su.pr`), hanya `arm64-v8a`. **Namespace Kotlin tetap `me.weishu.kernelsu`** (ksud memanggil `<paket>/me.weishu.kernelsu.ui.MainActivity`, dan namespace yang sama memudahkan merge); jangan diganti.
- **Khusus satu perangkat**: Xiaomi 14T Pro, codename `rothko` (`Build.DEVICE`/`Build.PRODUCT`, bukan dari `uname`, yang bisa disamarkan). HP lain tidak diblokir, hanya diberi peringatan merah di Beranda.
- **Integrasi dengan Cogan Kernel lewat Cogan Bridge** (`/sys/kernel/cogan/{abi,release,banner,device,soc}`, root-only, `kernel/patches/cogan_bridge.c` di Cogan-Kernel). Baris *Cogan Kernel* di Beranda (di bawah Versi manajer, bukan tautan) membaca file itu lewat shell root: versi kernel asli dan baris build dari `banner` (`#1 SMP PREEMPT …`), keduanya tidak terpengaruh spoof uname SUSFS. Baris fitur `SUSFS · NoMount · BBG` tidak ditampilkan (owner, 5 Nov 2026). **Versi kernel** tetap `uname` (versi samaran). Bila ABI Cogan Bridge berubah di Cogan-Kernel, `cogan/Cogan.kt` ikut diperbarui.
- **Beranda**: peringatan hulu "mode GKI hanya untuk lingkungan pengujian" tidak ditampilkan (Cogan Kernel memang membangun root ke dalam kernel). Kartu donasi/belajar KernelSU diganti tautan Unduh Cogan Kernel, Panduan pasang (`docs/pasang.md` Cogan-Kernel) dan Komunitas Telegram (`t.me/coganTWRP`). Halaman Tentang menautkan repo ini dan tetap menyebut kredit KernelSU (weishu dan kontributornya, GPL-3.0).
- **Cek pembaruan** membaca rilis terbaru `cogan17/CoganSU`. Nama APK `CoganSU_<git describe>_<kode>-release.apk` harus tetap cocok dengan regex `v(.+?)_(\d+)-` di `ui/util/Downloader.kt`.
- **Kernel hanya memercayai manager CoganSU.** `kernel/Kbuild` membaca `kernel/cogansu.mk`: paket `com.cogan.su` dan ukuran/hash sertifikat kunci rilis. Selama ukuran/hash kosong, build kernel **berhenti** (tidak pernah jatuh ke nilai KernelSU resmi). Nilai di baris perintah (`KSU_EXPECTED_SIZE/HASH`) tetap boleh menimpa.
- **Kunci rilis**: dibuat sekali oleh owner, disimpan sebagai secret `KEYSTORE` (base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (panduan `docs/kunci.md`). **Tidak pernah di-commit dan tidak pernah diganti**; asisten tidak membuat atau menyimpan kunci kecuali owner memintanya. Ukuran/hash sertifikatnya data publik dan disimpan di `kernel/cogansu.mk` (`scripts/cogansu_cert.py write`). `*.jks` diabaikan git.
- **Tiga workflow, semuanya hanya tombol manual, meniru Cogan Kernel** (owner, 4 Nov 2026: "buat rilis dari run dan check, tiru cogan kernel"). Tanpa jadwal atau pemicu otomatis; jangan menambahkannya.
  - **Build CoganSU** (`build-cogansu.yml`), form seperti Builder Cogan Kernel (owner, 4 Nov 2026): **Release Type** `CI` (bawaan, artifact saja) / `Pre-Release` / `Release` (job *Terbit* menerbitkan run itu juga; hanya dari branch default dan `kernel/cogansu.mk` harus cocok, dicek sebelum build) dan **Build Revision** `rN` (kosong = nomor run). Alur: ksud arm64 → APK bertanda tangan kunci rilis → cek `libksud.so` ada di APK, sertifikat APK (blok v2) = kunci = `kernel/cogansu.mk` (berbeda = berhenti; kosong = build CI tetap jalan dan summary menyebut nilai yang harus diisi) → artifact APK apa adanya (`archive: false`) dan `build-info` (`build.env`: RELEASE, RELEASE_NAME, VERSION, CODE, COMMIT, PACKAGE, APK, APK_SHA256, CERT_SIZE/HASH/STATE, UPSTREAM_COMMIT) → summary dua bahasa. **Tag rilis seperti Cogan Kernel: `vYY.MM.DD-rN`** (tanggal UTC build), nama `CoganSU vYY.MM.DD-rN`. Builder tidak pernah menimpa tag yang sudah ada.
  - **Rilis dari run** (`release-from-run.yml`, `scripts/release_from_run.py`): satu run Build CoganSU yang hijau dari `main` → cek run, `build.env` lengkap dan dari commit run, SHA256 APK, sertifikat APK = build = `kernel/cogansu.mk` di `main`, `CERT_STATE=match` → rilis dengan tag dari build (`vYY.MM.DD-rN`; Pre-Release atau Release/Latest; cek pembaruan aplikasi membaca *Latest*), `checksums.txt`, catatan ID/EN dan teks Telegram ID/EN di summary; `teks_saja` tanpa menerbitkan; centang **diuji di HP** hanya bila owner benar-benar mengujinya. Tag yang sudah ada menghentikan rilis, kecuali centang **timpa** (mati secara default, sama dengan Cogan Kernel: nomor rN bisa bentrok): rilis lama dan tag-nya diganti setelah semua cek lolos, hanya untuk tag `vYY.MM.DD-rN`.
  - **Cek pembaruan** (`cek-pembaruan.yml`, `scripts/cek_pembaruan.py`): hanya mengecek, tidak membangun/menerbitkan/menulis. KernelSU (rilis, `main`, commit belum digabung per bagian: kernel/aplikasi/ksud/WebUI/lain-lain), SUSFS (commit terbaru; patch KernelSU-nya cocok dengan CoganSU `main`, dan dengan KernelSU `main` = aman digabung atau "tunggu simonpunk"), CoganSU (`kernel/cogansu.mk`, rilis terakhir dan commit sesudahnya, Build CoganSU terakhir), Cogan Kernel (ABI dan file Cogan Bridge yang dibaca aplikasi, CoganSU sudah ada di `build.sh` atau belum, rilis terakhir). Waktu dalam WIB.
  - Workflow hulu KernelSU (LKM banyak KMI, ksud banyak arsitektur, website, dependabot, issue template) dihapus; jangan dikembalikan tanpa permintaan owner.
- **Catatan rilis dan teks Telegram untuk pengguna awam**: perubahan CoganSU diambil dari judul commit sejak rilis sebelumnya tanpa awalan `feat(…):`; commit `ci`, `docs`, `chore`, `test`, `style` dan merge dilewati; perubahan KernelSU hulu ditulis sebagai jumlah per bagian. Teks Telegram tanpa hash. Commit yang mengubah aplikasi/kernel untuk pengguna menyertakan trailer `Release-EN: <ringkasan English>` (dan `Release-ID:` bila judulnya kurang jelas untuk awam); tanpa `Release-EN`, bagian English hanya menulis jumlah perubahan, tidak menyalin teks Indonesia.
- **Versi**: kernel dan manager sama-sama `30000 + jumlah commit` repo ini; nama versi `git describe` dengan tag `v*` hulu (workflow menghapus tag lain sebelum describe). **Kernel dan manager selalu dari commit CoganSU yang sama.**
- **SUSFS**: Cogan Kernel memasang patch simonpunk `kernel_patches/KernelSU/10_enable_susfs_for_ksu.patch` ke folder `KernelSU/` (setup.sh CoganSU mengkloning repo ini ke folder bernama `KernelSU` untuk itu). **Jangan mengubah bagian `kernel/` yang disentuh patch itu**; setelah mengubah `kernel/`, cek `git apply --check` patch SUSFS terbaru (cabang `gki-android14-6.1`). Bila patch terbaru tidak cocok dengan CoganSU, build Cogan Kernel menunggu "Tunggu simonpunk menyesuaikan SUSFS"; jangan membuat salinan patch sendiri.
- **Integrasi di Cogan-Kernel** (root `CSU`, rancangan di `docs/integrasi-cogan-kernel.md`) dikerjakan lewat PR di repo Cogan-Kernel, mengikuti aturan repo itu.
- **Tidak ada sistem kunci, lock atau freeze** (aturan Cogan-Kernel): pembaruan hulu KernelSU digabung, bukan dibekukan. Bila hulu belum cocok (mis. dengan SUSFS), berhenti dan sebut siapa yang ditunggu.

## Pelajaran yang tetap berlaku

- **Diagnosa dengan bukti, bukan keyakinan.** Tulis jelas mana yang terukur lokal, mana yang sudah dibuktikan CI, dan mana yang sudah dibuktikan di HP.
- **Gate gagal, bukan lolos diam-diam.** Sertifikat kosong menghentikan build kernel; kunci yang berbeda menghentikan workflow. Jangan pernah mengubahnya jadi peringatan.
- **Jalur darurat tetap aman.** Tidak pernah wipe data, tidak pernah `flash_all_lock`. Bootloop dipulihkan lewat TWRP atau `boot` stok ([kembali ke stok](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/kembali-ke-stok.md)). Flash dan pasang aplikasi selalu keputusan owner.
- **Manager CoganSU tidak dikenali kernel lain**, termasuk kernel dengan KernelSU resmi (mis. r11), dan sebaliknya. Modul dan daftar izin di `/data/adb` tetap dipakai saat berpindah.
- **Setiap versi punya rilis sendiri dan tidak dihapus.** Artifact Actions bertahan 90 hari; yang permanen hanya file rilis.
- **Instruksi yang bisa dibaca dua arah: tanya dulu.**

## Hemat run workflow (owner, 23 Okt 2026)

- **Jangan boros Actions.** Jalankan workflow hanya bila sudah yakin hasilnya akan benar: kode sudah dicek lokal (tes, lint, uji dengan repo hulu sungguhan bila bisa).
- **Gagal = investigasi dulu, bukan coba-coba.** Baca log, temukan penyebabnya, perbaiki sekali dengan benar, baru jalankan ulang. Jangan berputar-putar dengan run berulang.
- **Investigasi secukupnya.** Jangan terlalu lama; cari bukti yang menentukan, lalu putuskan.
- **Cepat bukan berarti teledor.** Kerjakan dengan bijaksana.
- Container sesi Claude **tidak punya SDK Android** (dl.google.com diblokir), jadi APK hanya bisa dibuktikan lewat workflow Build CoganSU. Yang bisa dicek lokal: sintaks Kotlin (ktlint dari Maven Central), XML, workflow (actionlint), skrip Python, Kbuild (`make` tiruan), patch SUSFS (`git apply --check`).

## Uji di HP: hanya untuk build yang akan dirilis

- Kompatibilitas dibuktikan di CI. Claude boleh menjalankan build tanpa rilis, karena tidak membuat rilis dan tidak menyentuh HP.
- **Uji HP hanya untuk build yang akan dirilis**, satu build saja (manager CoganSU + Cogan Kernel dari commit yang sama). Label "diuji di HP" hanya untuk build yang benar-benar diuji owner.

## Checkpoint

- Bila owner menulis **checkpoint**: hentikan semua pekerjaan saat itu juga, tanpa mengecek apa pun satu per satu.
- Simpan keadaan terakhir ke [`CHECKPOINT.md`](CHECKPOINT.md): apa yang sedang berjalan, apa yang sudah selesai, apa yang tertunda, dan langkah pertama berikutnya. Commit dan push ke `main` lewat PR.
- Sesi berikutnya **mulai dari `CHECKPOINT.md`**, lalu lanjut hanya setelah owner meminta.
