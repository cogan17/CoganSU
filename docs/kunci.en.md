# CoganSU release key

[Bahasa Indonesia](kunci.md) · <b>English</b>

The kernel only trusts the manager app signed with the **CoganSU release key**. The key is created **once**, stored as repository secrets, and **never changed**.

> [!CAUTION]
> - Never share the key file (`.jks`) or its password, and never commit it. Whoever holds this key can make an app the kernel treats as the root manager.
> - Keep a backup of the key file and its password somewhere safe. If it is lost, the kernel has to be rebuilt with a new key and the old manager is no longer recognised.

## 1. Create the key

You need `keytool` (comes with Java): a PC with Java 17+, or **Termux** on the phone (`pkg install openjdk-21`).

Replace `STRONG_PASSWORD` with a long password of letters and digits only (no spaces or `\`):

```sh
keytool -genkeypair -v \
  -keystore cogansu.jks -storetype PKCS12 \
  -alias cogansu -keyalg RSA -keysize 4096 -validity 36500 \
  -storepass STRONG_PASSWORD -keypass STRONG_PASSWORD \
  -dname "CN=CoganSU, O=Cogan"
base64 -w 0 cogansu.jks > cogansu.jks.txt
```

`cogansu.jks.txt` holds one long line: the value of the `KEYSTORE` secret.

## 2. Store it as repository secrets

GitHub → **CoganSU** repository → *Settings* → *Secrets and variables* → *Actions* → *New repository secret*:

| Secret | Value |
|---|---|
| `KEYSTORE` | the whole content of `cogansu.jks.txt` |
| `KEYSTORE_PASSWORD` | `STRONG_PASSWORD` |
| `KEY_ALIAS` | `cogansu` |
| `KEY_PASSWORD` | `STRONG_PASSWORD` (the same; PKCS12 uses one password) |

## 3. Register the certificate in the kernel

1. Run the **Build CoganSU** workflow (release unticked).
2. The run summary shows the **Certificate**: size (e.g. `0x0577`) and hash (64 hex digits). This is public data, not a secret.
3. Fill in `kernel/cogansu.mk`:
   ```sh
   python3 scripts/cogansu_cert.py write 0x0577 <hash>
   ```
   and commit. (From a signed APK: `python3 scripts/cogansu_cert.py apk CoganSU.apk`.)
4. Build again: the summary now says ✅ *matches kernel/cogansu.mk*, and a release may be published.

From then on every build checks that the key in the secrets still matches `kernel/cogansu.mk`, and stops if it does not.
