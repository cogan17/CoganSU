package me.weishu.kernelsu.cogan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * CoganSU: the boot watcher's "an app was installed or updated" broadcast (sent as root, explicitly to
 * this non-exported receiver), and the Allow / Deny buttons of the root request notification.
 */
class RootRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    RootRequest.ACTION_CHECK -> RootRequest.check(app)
                    RootRequest.ACTION_ALLOW, RootRequest.ACTION_NEVER -> {
                        val packageName = intent.getStringExtra(RootRequest.EXTRA_PACKAGE) ?: return@launch
                        val uid = intent.getIntExtra(RootRequest.EXTRA_UID, -1).takeIf { it >= 0 } ?: return@launch
                        RootRequest.answer(app, packageName, uid, intent.action == RootRequest.ACTION_ALLOW)
                    }
                }
            } catch (e: Exception) {
                Log.e("CoganRootRequest", "root request failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
