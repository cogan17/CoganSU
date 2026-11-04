# Integrasi dengan Cogan Kernel / Cogan Kernel integration

🇮🇩 Catatan teknis untuk menambahkan CoganSU sebagai root manager di [cogan17/Cogan-Kernel](https://github.com/cogan17/Cogan-Kernel). Perubahan di repo itu dikerjakan lewat PR tersendiri di sana.

🇬🇧 Technical notes for adding CoganSU as a root manager to [cogan17/Cogan-Kernel](https://github.com/cogan17/Cogan-Kernel). Changes in that repository go through its own PR.

## Ringkas / Summary

| | |
|---|---|
| Sumber / Source | `https://github.com/cogan17/CoganSU`, commit `main` yang sudah punya run **Build CoganSU** sukses |
| Pasang / Setup | `kernel/setup.sh` CoganSU di commit itu (`bash setup.sh <commit>`); folder tetap `KernelSU/` |
| Identitas manager | otomatis dari `kernel/cogansu.mk` (paket `com.cogan.su` + ukuran/hash sertifikat); tidak perlu variabel make tambahan |
| SUSFS | patch simonpunk `kernel_patches/KernelSU/10_enable_susfs_for_ksu.patch` dipasang ke `KernelSU/` persis seperti root `KSU` (KernelSU resmi) |
| Manager | APK `CoganSU_<versi>_<kode>-release.apk` dari rilis/artifact **commit yang sama** |
| Gagal bila / Fails when | `kernel/cogansu.mk` masih kosong (Kbuild berhenti: *manager certificate … is not set*) |

## Langkah di build.sh (usulan) / Proposed build.sh step

Mengikuti pola root `KSU` yang sudah ada:

```sh
elif [ "$ROOT" = "CSU" ]; then
  # CoganSU: KernelSU with Cogan's own manager identity (kernel/cogansu.mk), same commit as the manager.
  curl -fsSL "https://raw.githubusercontent.com/cogan17/CoganSU/$ROOT_COMMIT/kernel/setup.sh" -o "$WORK/root-setup.sh"
  bash "$WORK/root-setup.sh" "$ROOT_COMMIT"
  [ "$(git -C KernelSU rev-parse HEAD)" = "$ROOT_COMMIT" ] || fail "CoganSU checkout is not $ROOT_COMMIT"
  KSU_VERSION=""
fi
```

dan pada bagian SUSFS, `CSU` diperlakukan sama dengan `KSU`:

```sh
if [ "$ROOT" = "KSU" ] || [ "$ROOT" = "CSU" ]; then
  git -C KernelSU apply --whitespace=nowarn "$WORK/susfs/kernel_patches/KernelSU/10_enable_susfs_for_ksu.patch" || wait_for ...
fi
```

## Sudah dicek / Verified

- Patch SUSFS untuk KernelSU (cabang `gki-android14-6.1`, 4 Nov 2026) terpasang bersih (`git apply --check`) di atas perubahan CoganSU: perubahan `kernel/Kbuild` CoganSU berada di luar bagian yang diubah patch itu.
- Kbuild: tanpa nilai sertifikat build berhenti; dengan nilai di `kernel/cogansu.mk` atau `KSU_EXPECTED_SIZE/HASH` di baris perintah, nilainya dipakai.
- `scripts/cogansu_cert.py apk` membaca APK KernelSU resmi v3.3.1 sebagai `0x033b c371061b…d2d6`, sama dengan nilai bawaan Kbuild upstream: cara hitungnya sama dengan kernel.

## Versi / Version

Versi kernel dan manager sama-sama `30000 + jumlah commit` repo CoganSU, jadi keduanya harus dari commit yang sama (aturan Cogan Kernel: kernel dan manager selalu sama).

Kernel and manager versions are both `30000 + commit count` of the CoganSU repository, so both must come from the same commit.
