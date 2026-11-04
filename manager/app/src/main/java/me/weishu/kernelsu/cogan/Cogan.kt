package me.weishu.kernelsu.cogan

import android.os.Build
import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.ui.util.getRootShell

/**
 * CoganSU: everything that ties this manager to one phone (Xiaomi 14T Pro, `rothko`) and to
 * Cogan Kernel lives here, so upstream KernelSU code stays easy to merge.
 */
object Cogan {
    const val DEVICE_CODENAME = "rothko"
    const val DEVICE_NAME = "Xiaomi 14T Pro"

    const val MANAGER_REPO_URL = "https://github.com/cogan17/CoganSU"
    const val MANAGER_LATEST_RELEASE_API = "https://api.github.com/repos/cogan17/CoganSU/releases/latest"
    const val KERNEL_REPO_URL = "https://github.com/cogan17/Cogan-Kernel"
    const val KERNEL_RELEASES_URL = "https://github.com/cogan17/Cogan-Kernel/releases"
    const val KERNEL_GUIDE_URL = "https://github.com/cogan17/Cogan-Kernel/blob/main/docs/pasang.md"
    const val TELEGRAM_URL = "https://t.me/coganTWRP"
    const val UPSTREAM_URL = "https://github.com/tiann/KernelSU"

    /** Cogan Bridge, the root-only sysfs ABI that Cogan Kernel exports (kernel/patches/cogan_bridge.c). */
    private const val BRIDGE_DIR = "/sys/kernel/cogan"
    private val BRIDGE_FILES = listOf("abi", "release", "device", "soc", "features")

    /** True on the phone CoganSU is made for. Never decided from the kernel name, which can be disguised. */
    fun isSupportedDevice(): Boolean {
        val device = Build.DEVICE.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        return device.equals(DEVICE_CODENAME, ignoreCase = true) ||
                product.equals(DEVICE_CODENAME, ignoreCase = true) ||
                product.startsWith("${DEVICE_CODENAME}_", ignoreCase = true)
    }

    /**
     * Reads Cogan Bridge through the root shell. The files are 0400 root, so without root (or on
     * another kernel) this returns [CoganKernelInfo.Unknown]. Call it off the main thread.
     */
    fun readKernelInfo(): CoganKernelInfo {
        val values = runCatching {
            val shell = getRootShell()
            if (!shell.isRoot) return CoganKernelInfo.Unknown
            val script = BRIDGE_FILES.joinToString("; ") { name ->
                "printf '%s=' $name; head -n 1 $BRIDGE_DIR/$name 2>/dev/null; echo"
            }
            // Every line: ShellUtils.fastCmd keeps only the last one, which left the card "not Cogan Kernel".
            shell.newJob().add(script).to(ArrayList<String>()).exec().out
        }.getOrNull()
            ?.asSequence()
            ?.mapNotNull { line ->
                val index = line.indexOf('=')
                if (index <= 0) null else line.substring(0, index) to line.substring(index + 1).trim()
            }
            ?.toMap()
            ?: return CoganKernelInfo.Unknown

        val release = values["release"].orEmpty()
        if (values["abi"] != "1" || release.isEmpty()) return CoganKernelInfo.NotCogan
        val features = values["features"].orEmpty()
            .split(' ')
            .mapNotNull { pair ->
                val index = pair.indexOf('=')
                if (index <= 0) null else pair.substring(0, index) to (pair.substring(index + 1) == "1")
            }
            .toMap()
        return CoganKernelInfo(
            state = CoganKernelState.Detected,
            release = release,
            device = values["device"].orEmpty(),
            soc = values["soc"].orEmpty(),
            susfs = features["susfs"] == true,
            noMount = features["nomount"] == true,
            bbg = features["bbg"] == true,
        )
    }
}

enum class CoganKernelState {
    /** Not read yet, or no root to read it with. */
    Unknown,

    /** Root works but the kernel has no Cogan Bridge: not Cogan Kernel. */
    NotCogan,

    /** Cogan Bridge answered. */
    Detected,
}

@Immutable
data class CoganKernelInfo(
    val state: CoganKernelState = CoganKernelState.Unknown,
    val release: String = "",
    val device: String = "",
    val soc: String = "",
    val susfs: Boolean = false,
    val noMount: Boolean = false,
    val bbg: Boolean = false,
) {
    /** The kernel was built for a phone other than the one it booted on. */
    val deviceMismatch: Boolean
        get() = state == CoganKernelState.Detected && device.isNotEmpty() &&
                !device.equals(Cogan.DEVICE_CODENAME, ignoreCase = true)

    /** "SUSFS · NoMount · BBG", or "No SUSFS · …" for what is off, like Cogan Kernel's own feature line. */
    val featureLine: String
        get() = listOf(
            if (susfs) "SUSFS" else "No SUSFS",
            if (noMount) "NoMount" else "No NoMount",
            if (bbg) "BBG" else "No BBG",
        ).joinToString(" · ")

    companion object {
        val Unknown = CoganKernelInfo()
        val NotCogan = CoganKernelInfo(state = CoganKernelState.NotCogan)
    }
}
