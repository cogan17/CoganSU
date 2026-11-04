# Kunci rilis CoganSU

<b>Bahasa Indonesia</b> · [English](kunci.en.md)

Kernel hanya memercayai aplikasi manager yang ditandatangani **kunci rilis CoganSU**. Kunci ini dibuat **sekali**, disimpan sebagai secret repo, dan **tidak pernah diganti**.

> [!CAUTION]
> - Jangan pernah membagikan file kunci (`.jks`) atau sandinya, dan jangan meng-commit-nya ke repo. Siapa pun yang memegang kunci ini bisa membuat aplikasi yang dianggap kernel sebagai manager root.
> - Simpan cadangan file kunci dan sandinya di tempat aman (mis. Google Drive pribadi + catatan sandi). Bila hilang, kernel harus dibangun ulang dengan kunci baru dan manager lama tidak dikenali lagi.

## 1. Buat kunci

Butuh `keytool` (ikut Java). Pilih salah satu:

- **PC** dengan Java 17+ terpasang.
- **Termux** di HP: `pkg install openjdk-21`.

Jalankan (ganti `SANDI_KUAT` dengan sandi panjang berisi huruf dan angka saja, tanpa spasi atau `\`):

```sh
keytool -genkeypair -v \
  -keystore cogansu.jks -storetype PKCS12 \
  -alias cogansu -keyalg RSA -keysize 4096 -validity 36500 \
  -storepass SANDI_KUAT -keypass SANDI_KUAT \
  -dname "CN=CoganSU, O=Cogan"
base64 -w 0 cogansu.jks > cogansu.jks.txt
```

`cogansu.jks.txt` berisi satu baris panjang: itulah isi secret `KEYSTORE`.

## 2. Simpan sebagai secret repo

GitHub → repo **CoganSU** → *Settings* → *Secrets and variables* → *Actions* → *New repository secret*:

| Nama secret | Isi |
|---|---|
| `KEYSTORE` | seluruh isi `cogansu.jks.txt` |
| `KEYSTORE_PASSWORD` | `SANDI_KUAT` |
| `KEY_ALIAS` | `cogansu` |
| `KEY_PASSWORD` | `SANDI_KUAT` (sama; PKCS12 memakai satu sandi) |

## 3. Daftarkan sertifikat ke kernel

1. Jalankan workflow **Build CoganSU**.
2. Ringkasan run menampilkan **Sertifikat**: ukuran (mis. `0x0577`) dan hash (64 huruf/angka). Ini data publik, bukan rahasia.
3. Isi `kernel/cogansu.mk`:
   ```sh
   python3 scripts/cogansu_cert.py write 0x0577 <hash>
   ```
   lalu commit. (Bisa juga dari APK yang sudah ditandatangani: `python3 scripts/cogansu_cert.py apk CoganSU.apk`.)
4. Build lagi: ringkasan sekarang menulis ✅ *cocok dengan kernel/cogansu.mk*, dan run itu boleh dirilis lewat **Rilis dari run**.

Sejak itu, setiap build memeriksa bahwa kunci di secret masih sama dengan `kernel/cogansu.mk`; bila berbeda, build berhenti.
