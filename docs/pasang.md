# 📥 Pasang CoganSU

[← Kembali ke README](../README.md) · <img src="https://flagcdn.com/24x18/gb.png" width="22" alt=""> [English](pasang.en.md)

CoganSU adalah aplikasi root manager. Root-nya sendiri ada di dalam **Cogan Kernel varian CoganSU**, jadi keduanya dipasang bersama dan harus dari **commit CoganSU yang sama** (lihat catatan rilis).

---

## Sebelum mulai

- Bootloader sudah **unlock**, HP **Xiaomi 14T Pro (`rothko`)**.
- **Cadangkan `boot`** dulu dan simpan juga `boot.img` stok, seperti di [panduan Cogan Kernel](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/pasang.md#1-cadangkan-boot-dulu).
- Siapkan dua file dari rilis yang sama: ZIP **Cogan Kernel varian CoganSU** dan APK **`CoganSU_…-release.apk`**.

## 1. Flash Cogan Kernel varian CoganSU

Ikuti [Pasang Kernel](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/pasang.md#2-flash) (Kernel Flasher atau TWRP), lalu reboot.

## 2. Pasang aplikasi CoganSU

1. Pasang `CoganSU_…-release.apk`.
2. Aplikasi root manager lama (KernelSU, KernelSU Next, BakaSU, SukiSU) boleh dihapus: kernel varian CoganSU tidak mengenalinya lagi. **Modul dan daftar izin root di `/data/adb` tetap ada**, tidak perlu dipasang ulang.

## 3. Setelah menyala

Buka CoganSU, lalu cek Beranda:

- Kartu hijau **Berfungsi** dengan mode **GKI**.
- Kartu **Cogan Kernel**: versi kernel dan baris fitur, mis. `SUSFS · NoMount · BBG`.
- Tidak ada kartu merah *"CoganSU hanya untuk Xiaomi 14T Pro"*.

## Kembali ke root manager lain

Flash Cogan Kernel varian lain (mis. KernelSU Next) atau `boot` cadangan, lalu pasang aplikasi manager yang sesuai dengan ZIP itu. Modul di `/data/adb` tetap ada.

## Masalah

| Gejala | Penyebab dan solusi |
|---|---|
| Beranda: **Tidak terpasang** / **Tidak didukung** | Kernel bukan varian CoganSU, atau APK dan kernel dari commit/rilis berbeda. Flash ZIP Cogan Kernel dari rilis yang sama dengan APK. |
| Kartu Cogan Kernel: *"Menunggu root untuk membaca Cogan Bridge"* | Root belum aktif untuk aplikasi; buka ulang CoganSU setelah kartu **Berfungsi** hijau. |
| Kartu Cogan Kernel: *"Kernel ini bukan Cogan Kernel"* | Kernel yang menyala bukan Cogan Kernel (mis. `boot` stok). |
| Kartu merah *"Cogan Kernel ini dibuat untuk …"* | ZIP kernel untuk HP lain. Kembalikan `boot` cadangan. |
| Bootloop | [Kembali ke Stok](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/kembali-ke-stok.md) lewat TWRP atau `boot` stok. |

Laporan ke [Komunitas Telegram](https://t.me/coganTWRP) dengan menyertakan versi CoganSU (Beranda → *Versi manajer*), versi kernel, dan log dari [Masalah & Log Cogan Kernel](https://github.com/cogan17/Cogan-Kernel/blob/main/docs/masalah-dan-log.md).
