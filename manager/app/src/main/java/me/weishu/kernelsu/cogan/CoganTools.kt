package me.weishu.kernelsu.cogan

import android.content.Context
import android.system.Os
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.getRootShell
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * CoganSU-only tools on Home: Cogan Kernel update check, one-tap security check and boot backup.
 * They read Cogan Bridge or rothko's partitions, so no other root manager has them (owner, 11 Nov 2026).
 */
object CoganTools {
    private const val KERNEL_LATEST_RELEASE_API = "https://api.github.com/repos/cogan17/Cogan-Kernel/releases/latest"

    data class KernelUpdate(val tag: String, val url: String)

    /** Build date of the running Cogan Kernel, from Cogan Bridge's banner ("... Wed Nov 11 03:48:50 UTC 2026"). */
    internal fun buildDate(build: String): LocalDate? {
        val m = Regex("""([A-Z][a-z]{2})\s+(\d{1,2}) \d{2}:\d{2}:\d{2} \S+ (\d{4})""").find(build) ?: return null
        val (month, day, year) = m.destructured
        return runCatching {
            LocalDate.parse("$day $month $year", DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
        }.getOrNull()
    }

    /** Cogan Kernel tags are vYY.MM.DD-rN, the UTC date of the build. */
    internal fun tagDate(tag: String): LocalDate? {
        val m = Regex("""^v(\d{2})\.(\d{2})\.(\d{2})-r\d+$""").find(tag) ?: return null
        val (y, mo, d) = m.destructured
        return runCatching { LocalDate.of(2000 + y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()
    }

    /** Revision number N of a vYY.MM.DD-rN tag. */
    internal fun revision(tag: String): Int? = Regex("""-r(\d+)$""").find(tag)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * The newest Cogan Kernel release when it is newer than the running kernel: by rN when the kernel
     * reports its own tag (Cogan Bridge identity), else by day (older kernels; a second build on the
     * same day is then not offered).
     */
    fun kernelUpdate(info: CoganKernelInfo): KernelUpdate? {
        if (info.state != CoganKernelState.Detected) return null
        val runningRevision = revision(info.tag)
        val runningDate = buildDate(info.build)
        if (runningRevision == null && runningDate == null) return null
        return runCatching {
            ksuApp.okhttpClient.newCall(Request.Builder().url(KERNEL_LATEST_RELEASE_API).build()).execute().use { response ->
                if (!response.isSuccessful) return null
                val json = JSONObject(response.body.string())
                val tag = json.getString("tag_name")
                val newer = if (runningRevision != null) {
                    (revision(tag) ?: return null) > runningRevision
                } else {
                    (tagDate(tag) ?: return null).isAfter(runningDate)
                }
                if (newer) KernelUpdate(tag, json.getString("html_url")) else null
            }
        }.getOrNull()
    }

    // Stock kernel releases of HyperOS 302 and 301 (Cogan-Kernel kernel/configs/stock-identity.env, docs/pasang).
    private val STOCK_RELEASES = setOf(
        "6.1.157-android14-11-g0baf70f2581d-ab15659624",
        "6.1.138-android14-11-g6ab8c9a86a33-ab14396278",
    )

    /** One-tap security check as Markdown: each line ✅, ⚠️ or ❌ with what to do. */
    fun securityReport(context: Context, info: CoganKernelInfo): String {
        val reads = mapOf(
            "selinux" to "getenforce",
            "dmesg" to "cat /proc/sys/kernel/dmesg_restrict",
            "kptr" to "cat /proc/sys/kernel/kptr_restrict",
            "bpf" to "cat /proc/sys/kernel/unprivileged_bpf_disabled",
            "features" to "cat /sys/kernel/cogan/features",
            "adb" to "settings get global adb_enabled",
            "dev" to "settings get global development_settings_enabled",
            "vb" to "getprop ro.boot.verifiedbootstate",
        )
        val script = reads.entries.joinToString("; ") { (key, cmd) -> "echo \"$key=$($cmd 2>/dev/null)\"" }
        val v = getRootShell().newJob().add(script).to(ArrayList(), null).exec().out
            .mapNotNull { line -> line.indexOf('=').takeIf { it > 0 }?.let { line.substring(0, it) to line.substring(it + 1).trim() } }
            .toMap()
        fun s(id: Int, vararg args: Any) = context.getString(id, *args)
        val uname = Os.uname().release
        val lines = mutableListOf<String>()
        fun add(ok: Boolean?, text: String) = lines.add("${when (ok) { true -> "✅"; false -> "❌"; null -> "⚠️" }} $text")

        add(v["selinux"] == "Enforcing", s(R.string.cogan_check_selinux, v["selinux"].orEmpty()))
        add(v["dmesg"] == "1", s(R.string.cogan_check_dmesg, v["dmesg"].orEmpty()))
        add(v["kptr"] == "2", s(R.string.cogan_check_kptr, v["kptr"].orEmpty()))
        // Android's bpfloader writes 0 on every boot and needs it; SELinux guards it (Cogan-Kernel CLAUDE.md).
        add(true, s(R.string.cogan_check_bpf, v["bpf"].orEmpty()))
        val susfs = v["features"].orEmpty().contains("susfs=1")
        add(if (info.state == CoganKernelState.Detected) susfs else null, s(if (susfs) R.string.cogan_check_susfs_on else R.string.cogan_check_susfs_off))
        when {
            info.state == CoganKernelState.Detected && uname == info.release -> add(false, s(R.string.cogan_check_uname_real, uname))
            uname in STOCK_RELEASES -> add(true, s(R.string.cogan_check_uname_stock, uname))
            else -> add(null, s(R.string.cogan_check_uname_other, uname))
        }
        add(if (v["adb"] == "1") null else true, s(if (v["adb"] == "1") R.string.cogan_check_adb_on else R.string.cogan_check_adb_off))
        add(if (v["dev"] == "1") null else true, s(if (v["dev"] == "1") R.string.cogan_check_dev_on else R.string.cogan_check_dev_off))
        add(null, s(R.string.cogan_check_bootloader, v["vb"].orEmpty()))
        return lines.joinToString("\n\n")
    }

    /** Where backups go: /sdcard/Coganisme, written through /data/media/0 and handed to media_rw so apps see them. */
    const val BACKUP_DIR = "/sdcard/Coganisme"
    private const val BACKUP_DIR_DATA = "/data/media/0/Coganisme"
    private val BACKUP_PARTITIONS = listOf("boot", "init_boot", "vendor_boot")

    /**
     * Copies boot, init_boot and vendor_boot of the active slot to a new folder
     * /sdcard/Coganisme/<date>_<slot>/<name>.img (never overwriting an older backup) with SHA-256 sums.
     * Read-only on the partitions. Returns the folder, or throws with the shell's error.
     */
    fun backupBoot(): String {
        val stamp = java.time.LocalDateTime.now(ZoneOffset.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val parts = BACKUP_PARTITIONS.joinToString(" ")
        val script = """
            set -e
            slot=${'$'}(getprop ro.boot.slot_suffix)
            [ -n "${'$'}slot" ] || { echo "no slot suffix" >&2; exit 1; }
            dir=$BACKUP_DIR_DATA/${stamp}${'$'}slot
            mkdir -p "${'$'}dir"
            for p in $parts; do
              dev=/dev/block/by-name/${'$'}p${'$'}slot
              [ -e "${'$'}dev" ] || dev=/dev/block/bootdevice/by-name/${'$'}p${'$'}slot
              dd if="${'$'}dev" of="${'$'}dir/${'$'}p.img" bs=1048576
            done
            cd "${'$'}dir" && sha256sum *.img > sha256.txt
            chown -R 1023:1023 $BACKUP_DIR_DATA && chmod -R u+rwX,g+rwX $BACKUP_DIR_DATA
            echo "${stamp}${'$'}slot"
        """.trimIndent()
        val result = getRootShell().newJob().add(script).to(ArrayList(), ArrayList()).exec()
        check(result.isSuccess) { result.err.joinToString("\n").ifEmpty { "backup failed (${result.code})" } }
        return "$BACKUP_DIR/${result.out.lastOrNull().orEmpty()}"
    }
}
