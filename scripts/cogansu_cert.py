#!/usr/bin/env python3
"""CoganSU manager certificate: the size and SHA-256 the kernel expects (kernel/cogansu.mk).

The kernel trusts the manager APK whose APK Signature Scheme v2 block holds a first certificate of
exactly this size and hash (kernel/manager/apk_sign.c). The certificate is public data; the key that
signs with it is the secret.

    cogansu_cert.py apk FILE.apk                 print size and hash of a signed APK
    cogansu_cert.py keystore FILE ALIAS          same, from a keystore (password in $KEYSTORE_PASSWORD)
    cogansu_cert.py check SIZE HASH              compare with kernel/cogansu.mk (exit 1 if different)
    cogansu_cert.py write SIZE HASH              store them in kernel/cogansu.mk

Sertifikat manager CoganSU: ukuran dan SHA-256 yang diharapkan kernel (kernel/cogansu.mk).
"""
import hashlib
import os
import re
import struct
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

MK = Path(__file__).resolve().parent.parent / "kernel" / "cogansu.mk"
APK_SIG_BLOCK_MAGIC = b"APK Sig Block 42"
APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871A


def fail(message):
    print(f"cogansu_cert: {message}", file=sys.stderr)
    sys.exit(2)


def _u32(data, offset):
    return struct.unpack_from("<I", data, offset)[0]


def _lp(data, offset):
    """Length-prefixed (u32) slice: returns (payload, next offset)."""
    size = _u32(data, offset)
    start = offset + 4
    end = start + size
    if end > len(data):
        fail("APK signature block is truncated")
    return data[start:end], end


def apk_certificate(path):
    """DER of the first certificate of the first v2 signer, as the kernel reads it."""
    data = Path(path).read_bytes()
    if not zipfile.is_zipfile(path):
        fail(f"{path} is not an APK")
    # End of central directory: the last "PK\x05\x06" (no comment handling needed beyond rfind).
    eocd = data.rfind(b"PK\x05\x06")
    if eocd < 0:
        fail("no end of central directory")
    cd_offset = _u32(data, eocd + 16)
    if data[cd_offset - 16:cd_offset] != APK_SIG_BLOCK_MAGIC:
        fail("APK is not signed with APK Signature Scheme v2/v3 (no signing block)")
    block_size = struct.unpack_from("<Q", data, cd_offset - 24)[0]
    block_start = cd_offset - block_size - 8
    pairs = data[block_start + 8:cd_offset - 24]
    offset = 0
    while offset + 12 <= len(pairs):
        pair_len = struct.unpack_from("<Q", pairs, offset)[0]
        pair_id = _u32(pairs, offset + 8)
        value = pairs[offset + 12:offset + 8 + pair_len]
        offset += 8 + pair_len
        if pair_id != APK_SIGNATURE_SCHEME_V2_BLOCK_ID:
            continue
        signers, _ = _lp(value, 0)
        signer, _ = _lp(signers, 0)
        signed_data, _ = _lp(signer, 0)
        _digests, pos = _lp(signed_data, 0)
        certificates, _ = _lp(signed_data, pos)
        certificate, _ = _lp(certificates, 0)
        return certificate
    fail("APK has no APK Signature Scheme v2 block (the kernel needs v2)")


def keystore_certificate(path, alias):
    password = os.environ.get("KEYSTORE_PASSWORD")
    if not password:
        fail("set KEYSTORE_PASSWORD")
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "cert.der"
        subprocess.run(
            ["keytool", "-exportcert", "-alias", alias, "-keystore", path, "-storepass:env",
             "KEYSTORE_PASSWORD", "-file", str(out)],
            check=True, stdout=subprocess.DEVNULL)
        return out.read_bytes()


def describe(certificate):
    return f"0x{len(certificate):04x}", hashlib.sha256(certificate).hexdigest()


def read_mk():
    text = MK.read_text()
    size = re.search(r"^COGANSU_EXPECTED_SIZE\s*:=\s*(\S*)\s*$", text, re.M)
    digest = re.search(r"^COGANSU_EXPECTED_HASH\s*:=\s*(\S*)\s*$", text, re.M)
    if not size or not digest:
        fail(f"{MK} lacks COGANSU_EXPECTED_SIZE/HASH lines")
    return size.group(1), digest.group(1)


def normalize(size, digest):
    if not re.fullmatch(r"0x[0-9a-fA-F]+", size):
        fail(f"size must look like 0x033b, not {size!r}")
    if not re.fullmatch(r"[0-9a-fA-F]{64}", digest):
        fail(f"hash must be 64 hex digits, not {digest!r}")
    return f"0x{int(size, 16):04x}", digest.lower()


def main(argv):
    if len(argv) == 3 and argv[1] == "apk":
        size, digest = describe(apk_certificate(argv[2]))
    elif len(argv) == 4 and argv[1] == "keystore":
        size, digest = describe(keystore_certificate(argv[2], argv[3]))
    elif len(argv) == 4 and argv[1] == "check":
        want = normalize(argv[2], argv[3])
        have = read_mk()
        if not have[0] or not have[1]:
            print("empty")
            return 1
        print("match" if normalize(*have) == want else "mismatch")
        return 0 if normalize(*have) == want else 1
    elif len(argv) == 4 and argv[1] == "write":
        size, digest = normalize(argv[2], argv[3])
        text = MK.read_text()
        text = re.sub(r"^COGANSU_EXPECTED_SIZE\s*:=.*$", f"COGANSU_EXPECTED_SIZE := {size}", text, flags=re.M)
        text = re.sub(r"^COGANSU_EXPECTED_HASH\s*:=.*$", f"COGANSU_EXPECTED_HASH := {digest}", text, flags=re.M)
        MK.write_text(text)
        print(f"{MK}: {size} {digest}")
        return 0
    else:
        print(__doc__, file=sys.stderr)
        return 2
    print(f"size={size}")
    print(f"hash={digest}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
