# Aturan untuk asisten AI di repo ini

CoganSU adalah root manager versi sendiri milik owner (cogan17), turunan [tiann/KernelSU](https://github.com/tiann/KernelSU), khusus Xiaomi 14T Pro (`rothko`) dan dipasangkan dengan [cogan17/Cogan-Kernel](https://github.com/cogan17/Cogan-Kernel). Aturan umum owner sama dengan di Cogan-Kernel (lihat `CLAUDE.md` di sana); ringkasnya:

- **Komunikasi ke owner wajib bahasa Indonesia.** Deliverable pengguna akhir (README, catatan rilis, summary workflow) dua bahasa: Indonesia lalu English.
- **Tanpa atribusi AI**: author/committer `Cogan <79615344+cogan17@users.noreply.github.com>`, pesan commit tanpa `Co-Authored-By`/`Claude-Session`, PR tanpa footer Claude.
- Waktu untuk owner ditulis dalam **WIB**.
- **Hemat run Actions**: workflow hanya manual; cek lokal dulu, gagal = baca log dan perbaiki sekali dengan benar.
- **Gate gagal, bukan lolos diam-diam.** Kbuild berhenti bila sertifikat manager belum diisi; workflow berhenti bila kunci berbeda dengan `kernel/cogansu.mk`.

## Khusus repo ini

- **Riwayat KernelSU dipertahankan**; perubahan CoganSU di atasnya supaya `git merge upstream/main` tetap mudah. Ubah kode upstream sesedikit mungkin; kode khusus Cogan di `manager/app/src/main/java/me/weishu/kernelsu/cogan/`, `values*/strings_cogan.xml`, `kernel/cogansu.mk`, `scripts/cogansu_cert.py`. Namespace Kotlin tetap `me.weishu.kernelsu` (ksud memanggil `…/me.weishu.kernelsu.ui.MainActivity`); yang berubah hanya `applicationId` `com.cogan.su`.
- **Jangan menyentuh bagian `kernel/Kbuild` dan file kernel yang diubah patch SUSFS** (`kernel_patches/KernelSU/10_enable_susfs_for_ksu.patch` simonpunk). Setelah mengubah `kernel/`, cek `git apply --check` patch itu.
- **Kunci rilis tidak pernah di-commit dan tidak pernah diganti** (secret `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; panduan `docs/kunci.md`). Ukuran/hash sertifikatnya publik dan disimpan di `kernel/cogansu.mk`.
- Kernel dan manager selalu dari commit CoganSU yang sama (versi = 30000 + jumlah commit).
- Tidak ada SDK Android di container sesi Claude (dl.google.com diblokir): APK hanya bisa dibuktikan lewat workflow **Build CoganSU**.
