package me.weishu.kernelsu.cogan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.SegmentedListItem
import me.weishu.kernelsu.ui.component.material.SegmentedSwitchItem
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon

/** CoganSU: Settings rows and the App Profile row of "ask root for new apps" ([RootRequest]). */
private class RootRequestUi(
    val enabled: Boolean,
    val setEnabled: (Boolean) -> Unit,
    val backup: () -> Unit,
    val restore: () -> Unit,
)

@Composable
private fun rememberRootRequestUi(): RootRequestUi {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(RootRequest.isEnabled(context)) }
    fun toast(id: Int, vararg args: Any) = Toast.makeText(context, context.getString(id, *args), Toast.LENGTH_SHORT).show()
    fun apply(on: Boolean) = scope.launch {
        val ok = withContext(Dispatchers.IO) { runCatching { RootRequest.setEnabled(context, on) }.isSuccess }
        if (ok) enabled = on else toast(R.string.cogan_root_request_failed)
    }
    // Without notifications the questions cannot be shown; the feature still turns on, Android asks once.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { apply(true) }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)!!.use { RootRequest.backup(context, it) } }.isSuccess
            }
            toast(if (ok) R.string.cogan_root_backup_done else R.string.cogan_root_request_failed)
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)!!.use { RootRequest.restore(context, it) } }.getOrNull()
            }
            if (count != null) toast(R.string.cogan_root_restore_done, count) else toast(R.string.cogan_root_request_failed)
        }
    }
    return RootRequestUi(
        enabled = enabled,
        setEnabled = { on ->
            val ask = on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (ask) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else apply(on)
        },
        backup = { backup.launch("cogansu-izin-root.json") },
        restore = { restore.launch(arrayOf("application/json", "*/*")) },
    )
}

@Composable
fun RootRequestSwitchMaterial() {
    val ui = rememberRootRequestUi()
    SegmentedSwitchItem(
        icon = Icons.Filled.NotificationsActive,
        title = stringResource(R.string.cogan_root_request_setting),
        summary = stringResource(R.string.cogan_root_request_setting_summary),
        checked = ui.enabled,
        onCheckedChange = ui.setEnabled,
    )
}

@Composable
private fun RootRequestActionMaterial(icon: ImageVector, title: Int, summary: Int, onClick: (RootRequestUi) -> Unit) {
    val ui = rememberRootRequestUi()
    SegmentedListItem(
        onClick = { onClick(ui) },
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        leadingContent = { Icon(icon, null) },
    )
}

@Composable
fun RootRequestBackupMaterial() = RootRequestActionMaterial(
    Icons.Filled.Backup, R.string.cogan_root_backup, R.string.cogan_root_backup_summary
) { it.backup() }

@Composable
fun RootRequestRestoreMaterial() = RootRequestActionMaterial(
    Icons.Filled.Restore, R.string.cogan_root_restore, R.string.cogan_root_restore_summary
) { it.restore() }

@Composable
private fun MiuixStart(icon: ImageVector) = MiuixIcon(
    icon, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground
)

/** The same three rows for the Miuix style, inside the card that holds Debugging WebView. */
@Composable
fun RootRequestMiuix() {
    val ui = rememberRootRequestUi()
    SwitchPreference(
        title = stringResource(R.string.cogan_root_request_setting),
        summary = stringResource(R.string.cogan_root_request_setting_summary),
        startAction = { MiuixStart(Icons.Rounded.NotificationsActive) },
        checked = ui.enabled,
        onCheckedChange = ui.setEnabled,
    )
    ArrowPreference(
        title = stringResource(R.string.cogan_root_backup),
        summary = stringResource(R.string.cogan_root_backup_summary),
        startAction = { MiuixStart(Icons.Rounded.Backup) },
        onClick = ui.backup,
    )
    ArrowPreference(
        title = stringResource(R.string.cogan_root_restore),
        summary = stringResource(R.string.cogan_root_restore_summary),
        startAction = { MiuixStart(Icons.Rounded.Restore) },
        onClick = ui.restore,
    )
}

@Composable
private fun rememberNeverAsk(packageName: String): Pair<Boolean, (Boolean) -> Unit> {
    val context = LocalContext.current
    var never by remember(packageName) { mutableStateOf(packageName in RootRequest.neverAsk(context)) }
    return never to { on: Boolean -> RootRequest.setNeverAsk(context, packageName, on); never = on }
}

/** App Profile: put the app on, or take it off, the "never ask about root" list. */
@Composable
fun NeverAskMaterial(packageName: String) {
    val (never, set) = rememberNeverAsk(packageName)
    SegmentedSwitchItem(
        icon = Icons.Filled.NotificationsOff,
        title = stringResource(R.string.cogan_root_never_ask),
        summary = stringResource(R.string.cogan_root_never_ask_summary),
        checked = never,
        onCheckedChange = set,
    )
}

@Composable
fun NeverAskMiuix(packageName: String) {
    val (never, set) = rememberNeverAsk(packageName)
    SwitchPreference(
        title = stringResource(R.string.cogan_root_never_ask),
        summary = stringResource(R.string.cogan_root_never_ask_summary),
        startAction = { MiuixStart(Icons.Rounded.NotificationsOff) },
        checked = never,
        onCheckedChange = set,
    )
}
