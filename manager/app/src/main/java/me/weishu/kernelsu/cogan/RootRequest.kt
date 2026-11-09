package me.weishu.kernelsu.cogan

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.getRootShell
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

/**
 * CoganSU: "ask for root" like Magisk, without touching the kernel. KernelSU + SUSFS hide `su` from
 * apps that are not allowed (they never reach KernelSU's code), so an app's attempt cannot be seen.
 * Instead, when an app is installed or updated, CoganSU checks its APK for signs that it uses root
 * and asks once: Allow (adds it to Superuser) or Deny (never asked again).
 *
 * The trigger is a boot script in /data/adb/service.d that watches packages.list (busybox inotifyd)
 * and sends an explicit broadcast to [RootRequestReceiver]. Nothing new is visible to other apps.
 */
object RootRequest {
    private const val PREFS = "cogan_root_request"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_NEVER = "never"
    private const val KEY_SEEN = "seen"
    private const val CHANNEL = "cogan_root_request"
    private const val BUSYBOX = "/data/adb/ksu/bin/busybox"
    private const val SCRIPT = "/data/adb/service.d/cogansu_root_request.sh"

    const val ACTION_CHECK = "com.cogan.su.ROOT_REQUEST_CHECK"
    const val ACTION_ALLOW = "com.cogan.su.ROOT_REQUEST_ALLOW"
    const val ACTION_NEVER = "com.cogan.su.ROOT_REQUEST_NEVER"
    const val EXTRA_PACKAGE = "package"
    const val EXTRA_UID = "uid"

    /**
     * Root apps whose APK carries no sign of it (owner's phone, 9 Nov 2026: 171 apps scanned).
     * The signs below caught Swift Backup, Kernel Flasher, Morphe, App Manager, Wa Enhancer (libsu),
     * MT, ZArchiver, Root Explorer and a renamed GameGuardian (ACCESS_SUPERUSER).
     */
    private val KNOWN = setOf(
        "com.termux",
        "moe.shizuku.privileged.api",
        "org.frknkrc44.hma_oss",
        "org.matrix.vector.manager",
        "xzr.hkf",
        "com.rajmani7584.payloaddumper",
    )

    // Root libraries in the dex. "/system/bin/su" and RootBeer are NOT signs: banks, games and
    // WhatsApp carry them to detect root (owner's scan, 9 Nov 2026).
    private const val DEX_MARKERS = "Lcom/topjohnwu/superuser/|Leu/chainfire/libsuperuser/|Lcom/stericson/"

    private val lock = Mutex()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun sh(command: String): List<String> =
        getRootShell().newJob().add(command).to(ArrayList<String>(), null).exec().out

    private fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"

    fun isEnabled(context: Context) = prefs(context).getBoolean(KEY_ENABLED, false)

    fun neverAsk(context: Context): Set<String> = prefs(context).getStringSet(KEY_NEVER, emptySet())!!.toSet()

    fun setNeverAsk(context: Context, packageName: String, never: Boolean) {
        val set = neverAsk(context).toMutableSet()
        if (never) set += packageName else set -= packageName
        prefs(context).edit { putStringSet(KEY_NEVER, set) }
    }

    /** package -> uid of every installed package, read as root (the manager cannot see every app). */
    private fun uids(): Map<String, Int> = sh("pm list packages -U").mapNotNull { line ->
        val name = line.removePrefix("package:").substringBefore(' ')
        val uid = line.substringAfter(" uid:", "").substringBefore(',').toIntOrNull() ?: return@mapNotNull null
        name to uid
    }.toMap()

    /** Third-party packages as "package path uid" (pm list packages -3 -f -U). */
    private fun installed(): List<Triple<String, String, Int>> =
        sh("pm list packages -3 -f -U").mapNotNull { line ->
            // package:/data/app/~~x==/pkg-y==/base.apk=pkg uid:10123
            val body = line.removePrefix("package:")
            val uidAt = body.lastIndexOf(" uid:")
            val eq = body.lastIndexOf('=', if (uidAt < 0) body.length else uidAt)
            if (uidAt < 0 || eq < 0) return@mapNotNull null
            val uid = body.substring(uidAt + 5).trim().substringBefore(',').toIntOrNull() ?: return@mapNotNull null
            Triple(body.substring(eq + 1, uidAt), body.substring(0, eq), uid)
        }

    /** Turns the feature on or off: the boot script, its watcher, and the list of apps already seen. */
    suspend fun setEnabled(context: Context, enabled: Boolean) = lock.withLock {
        if (enabled) {
            // Only apps installed or updated from now on are asked about.
            prefs(context).edit { putStringSet(KEY_SEEN, installed().map { "${it.first}|${it.second}" }.toSet()) }
            val target = "${context.packageName}/${RootRequestReceiver::class.java.name}"
            val script = """
                |#!/system/bin/sh
                |# CoganSU: tells CoganSU when an app is installed or updated (Settings > Ask root for new apps).
                |until [ "${'$'}(getprop sys.boot_completed)" = 1 ]; do sleep 5; done
                |$BUSYBOX inotifyd - /data/system:wy | while read -r ev dir name; do
                |  [ "${'$'}name" = packages.list ] && am broadcast -n $target -a $ACTION_CHECK >/dev/null 2>&1
                |done
                |""".trimMargin()
            sh("mkdir -p /data/adb/service.d && printf '%s' ${quote(script)} > $SCRIPT && chmod 755 $SCRIPT")
            sh("pkill -f cogansu_root_request.sh; setsid nohup sh $SCRIPT >/dev/null 2>&1 &")
        } else {
            sh("rm -f $SCRIPT; pkill -f cogansu_root_request.sh; pkill -f 'inotifyd - /data/system:wy'")
        }
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }

    private fun usesRoot(packageName: String, apk: String): Boolean {
        if (packageName in KNOWN) return true
        val a = quote(apk)
        val dex = sh(
            "for d in $($BUSYBOX unzip -l $a | grep -o 'classes[0-9]*\\.dex'); do " +
                "$BUSYBOX unzip -p $a ${'$'}d | grep -a -q -E '$DEX_MARKERS' && echo yes && break; done"
        )
        if (dex.contains("yes")) return true
        return sh("dumpsys package ${quote(packageName)} | grep -q ACCESS_SUPERUSER && echo yes").contains("yes")
    }

    /** Called by the watcher: asks once about every new or updated app that uses root. */
    suspend fun check(context: Context) = lock.withLock {
        if (!isEnabled(context)) return@withLock
        val prefs = prefs(context)
        val seen = prefs.getStringSet(KEY_SEEN, emptySet())!!
        val never = neverAsk(context)
        val apps = installed()
        for ((packageName, apk, uid) in apps) {
            if ("$packageName|$apk" in seen || packageName == context.packageName || packageName in never) continue
            if (Natives.getAppProfile(packageName, uid).allowSu) continue
            if (usesRoot(packageName, apk)) notify(context, packageName, uid)
        }
        // Store what is installed now: removed apps drop out, an update changes the path.
        prefs.edit { putStringSet(KEY_SEEN, apps.map { "${it.first}|${it.second}" }.toSet()) }
    }

    @SuppressLint("MissingPermission") // checked by areNotificationsEnabled()
    private fun notify(context: Context, packageName: String, uid: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.cogan_root_request_channel), NotificationManager.IMPORTANCE_HIGH)
        )
        // shortcut: apps hidden from the manager by package visibility show their package name.
        val label = runCatching {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
        fun action(action: String) = PendingIntent.getBroadcast(
            context, uid * 2 + if (action == ACTION_ALLOW) 0 else 1,
            Intent(context, RootRequestReceiver::class.java).setAction(action)
                .putExtra(EXTRA_PACKAGE, packageName).putExtra(EXTRA_UID, uid),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_logo_vector)
            .setContentTitle(context.getString(R.string.cogan_root_request_title, label))
            .setContentText(context.getString(R.string.cogan_root_request_text))
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.cogan_root_request_allow), action(ACTION_ALLOW))
            .addAction(0, context.getString(R.string.cogan_root_request_deny), action(ACTION_NEVER))
            .build()
        manager.notify(packageName, uid, notification)
    }

    fun answer(context: Context, packageName: String, uid: Int, allow: Boolean) {
        context.getSystemService(NotificationManager::class.java).cancel(packageName, uid)
        if (allow) {
            Natives.setAppProfile(Natives.getAppProfile(packageName, uid).copy(currentUid = uid, allowSu = true))
        } else {
            setNeverAsk(context, packageName, true)
        }
    }

    /** Root decisions to a JSON file: apps allowed root, and apps never to be asked about. */
    fun backup(context: Context, out: OutputStream) {
        val allowed = uids().filter { (name, uid) -> Natives.getAppProfile(name, uid).allowSu }.keys.sorted()
        val json = JSONObject()
            .put("cogansu_root_requests", 1)
            .put("allow", JSONArray(allowed))
            .put("never", JSONArray(neverAsk(context).sorted()))
        out.write(json.toString(2).toByteArray())
    }

    /** Restores a [backup]: allows root to listed apps that are installed, merges the never-ask list. */
    fun restore(context: Context, input: InputStream): Int {
        val json = JSONObject(input.readBytes().decodeToString())
        require(json.optInt("cogansu_root_requests") == 1) { "not a CoganSU root backup" }
        val allow = json.getJSONArray("allow")
        val uids = uids()
        var restored = 0
        for (i in 0 until allow.length()) {
            val packageName = allow.getString(i)
            val uid = uids[packageName] ?: continue
            val profile = Natives.getAppProfile(packageName, uid)
            if (profile.allowSu || Natives.setAppProfile(profile.copy(currentUid = uid, allowSu = true))) restored++
        }
        val never = json.optJSONArray("never") ?: JSONArray()
        val set = neverAsk(context).toMutableSet()
        for (i in 0 until never.length()) set += never.getString(i)
        prefs(context).edit { putStringSet(KEY_NEVER, set) }
        return restored
    }
}
