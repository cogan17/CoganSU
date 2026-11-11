package me.weishu.kernelsu.cogan

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.dialog.ConfirmResult
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog

/** CoganSU: the Home rows "Security check" and "Back up boot" ([CoganTools]); dialogs follow the UI style. */
class CoganToolActions(val securityCheck: () -> Unit, val backupBoot: () -> Unit)

@Composable
fun rememberCoganToolActions(coganKernel: CoganKernelInfo): CoganToolActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loading = rememberLoadingDialog()
    val dialog = rememberConfirmDialog()
    fun s(id: Int, vararg args: Any) = context.getString(id, *args)

    return CoganToolActions(
        securityCheck = {
            scope.launch {
                val report = loading.withLoading {
                    withContext(Dispatchers.IO) { runCatching { CoganTools.securityReport(context, coganKernel) } }
                }.getOrElse { s(R.string.cogan_tool_failed, it.message.orEmpty()) }
                val copy = dialog.awaitConfirm(
                    title = s(R.string.cogan_check_title),
                    content = report,
                    markdown = true,
                    confirm = s(R.string.cogan_check_copy),
                    dismiss = s(android.R.string.cancel),
                )
                if (copy == ConfirmResult.Confirmed) {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("CoganSU", report))
                    Toast.makeText(context, s(R.string.cogan_check_copied), Toast.LENGTH_SHORT).show()
                }
            }
        },
        backupBoot = {
            scope.launch {
                val go = dialog.awaitConfirm(
                    title = s(R.string.cogan_backup_title),
                    content = s(R.string.cogan_backup_confirm, CoganTools.BACKUP_DIR),
                    confirm = s(R.string.cogan_backup_start),
                    dismiss = s(android.R.string.cancel),
                )
                if (go != ConfirmResult.Confirmed) return@launch
                val result = loading.withLoading { withContext(Dispatchers.IO) { runCatching { CoganTools.backupBoot() } } }
                dialog.awaitConfirm(
                    title = s(R.string.cogan_backup_title),
                    content = result.fold(
                        { s(R.string.cogan_backup_done, it) },
                        { s(R.string.cogan_tool_failed, it.message.orEmpty()) },
                    ),
                    confirm = s(android.R.string.ok),
                )
            }
        },
    )
}
