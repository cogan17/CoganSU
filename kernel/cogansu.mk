# CoganSU manager identity, read by kernel/Kbuild.
#
# The kernel only trusts the manager app whose package name AND signing certificate match these
# values. COGANSU_EXPECTED_SIZE/HASH come from the certificate of the CoganSU release key (public
# data, not a secret): the "Build CoganSU" workflow prints them, and scripts/cogansu_cert.py
# computes them from a keystore or a signed APK. A kernel build stops while they are empty, so a
# kernel can never silently trust the official KernelSU manager (or anyone else's) instead.
#
# Identitas manager CoganSU, dibaca kernel/Kbuild. Kernel hanya memercayai aplikasi manager yang
# nama paket DAN sertifikat tanda tangannya cocok dengan nilai di bawah. Ukuran dan hash berasal
# dari sertifikat kunci rilis CoganSU (data publik, bukan rahasia). Build kernel berhenti selama
# nilainya masih kosong.

COGANSU_PACKAGE_NAME := com.cogan.su
COGANSU_EXPECTED_SIZE := 0x04ed
COGANSU_EXPECTED_HASH := b0fb06caa39728fa16f4a005712dad3c31613568ae4921dd73b16075c28aec6f
