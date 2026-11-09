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
 * Instead CoganSU checks APKs for signs that they use root, when the feature is turned on (every
 * installed app) and when an app is installed or updated, and asks once: Allow (adds it to Superuser)
 * or Deny (never asked again).
 *
 * The trigger is a boot script in /data/adb/service.d that watches packages.list (busybox inotifyd)
 * and sends an explicit broadcast to [RootRequestReceiver]. Nothing new is visible to other apps.
 */
object RootRequest {
    private const val PREFS = "cogan_root_request"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_NEVER = "never"
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
    private val DEX_MARKERS = listOf("Lcom/topjohnwu/superuser/", "Leu/chainfire/libsuperuser/", "Lcom/stericson/")

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

    private const val STATE = "/data/adb/cogansu"
    private const val SEEN = "$STATE/root_request_seen"

    /**
     * The boot script. The APK check runs here, as root in the background; for each app that uses root
     * it sends the package to [RootRequestReceiver], which asks the user. A fast pass (known apps and
     * ACCESS_SUPERUSER from one dumpsys) asks within seconds; the dex check, which can take minutes
     * over every installed app, follows (owner, 10 Nov 2026: waiting minutes was too slow). Each APK
     * path is checked once ($SEEN); an update gets a new path and is checked again.
     */
    private fun script(context: Context): String {
        val target = "${context.packageName}/${RootRequestReceiver::class.java.name}"
        return """
            |#!/system/bin/sh
            |# CoganSU: Settings > Ask root for new apps (cogan/RootRequest.kt). Removed when it is turned off.
            |# Out of the manager's cgroup: Android kills and freezes every process the app started (owner's phone,
            |# 10 Nov 2026: the watcher was gone after leaving CoganSU). From service.d at boot this changes nothing.
            |echo ${'$'}${'$'} > /sys/fs/cgroup/cgroup.procs 2>/dev/null
            |B=$BUSYBOX
            |KNOWN=" ${KNOWN.joinToString(" ")} "
            |R="${DEX_MARKERS.joinToString(" ") { "-e $it" }}"
            |mkdir -p $STATE; touch $SEEN
            |# -f 32: FLAG_INCLUDE_STOPPED_PACKAGES, so the question still comes after CoganSU was swiped away.
            |ask() { am broadcast -f 32 -n $target -a $ACTION_CHECK --es $EXTRA_PACKAGE "${'$'}1" --ei $EXTRA_UID "${'$'}2" </dev/null >/dev/null 2>&1; }
            |scan() {
            |  pm list packages -3 -f -U | sed 's/^package://' > $STATE/list
            |  # Fast pass, seconds: known root apps and ACCESS_SUPERUSER, read for every app in one dumpsys.
            |  SU=" ${'$'}(dumpsys package 2>/dev/null | awk '/^  Package \[/ { p = substr(${'$'}2, 2, length(${'$'}2) - 2) } /android\.permission\.ACCESS_SUPERUSER/ && p != "" { print p }' | sort -u | tr '\n' ' ') "
            |  : > $STATE/slow
            |  while read -r l u; do
            |    a=${'$'}{l%=*}; p=${'$'}{l##*=}; uid=${'$'}{u#uid:}; uid=${'$'}{uid%%,*}
            |    grep -qxF "${'$'}p|${'$'}a" $SEEN && continue
            |    case "${'$'}KNOWN${'$'}SU" in
            |      *" ${'$'}p "*) echo "${'$'}p|${'$'}a" >> $SEEN; ask "${'$'}p" "${'$'}uid" ;;
            |      *) echo "${'$'}l ${'$'}u" >> $STATE/slow ;;
            |    esac
            |  done < $STATE/list
            |  # Slow pass: root libraries inside the dex, 4 apps at a time (fixed-string search, no regex).
            |  n=0
            |  while read -r l u; do
            |    dexcheck "${'$'}l" "${'$'}u" </dev/null &
            |    n=${'$'}((n + 1)); [ ${'$'}((n % 4)) -eq 0 ] && wait
            |  done < $STATE/slow
            |  wait
            |}
            |dexcheck() {
            |  a=${'$'}{1%=*}; p=${'$'}{1##*=}; uid=${'$'}{2#uid:}; uid=${'$'}{uid%%,*}
            |  echo "${'$'}p|${'$'}a" >> $SEEN
            |  for d in ${'$'}(${'$'}B unzip -l "${'$'}a" 2>/dev/null | grep -o 'classes[0-9]*\.dex'); do
            |    ${'$'}B unzip -p "${'$'}a" "${'$'}d" 2>/dev/null | grep -a -q -F ${'$'}R && ask "${'$'}p" "${'$'}uid" && return
            |  done
            |}
            |until [ "${'$'}(getprop sys.boot_completed)" = 1 ]; do sleep 5; done
            |# Twice: an app installed while the first (long) check ran is caught by the second, quick one.
            |scan; scan
            |${'$'}B inotifyd - /data/system:wy | while read -r ev dir name; do
            |  [ "${'$'}name" = packages.list ] && scan
            |done
            |""".trimMargin()
    }

    /**
     * Turns the feature on or off. On: every installed app is checked again (owner, 10 Nov 2026: an app
     * installed before turning it on, Root Explorer, was never asked about). Off: script and watcher go.
     */
    suspend fun setEnabled(context: Context, enabled: Boolean) = lock.withLock {
        if (enabled) {
            sh("mkdir -p /data/adb/service.d && printf '%s' ${quote(script(context))} > $SCRIPT && chmod 755 $SCRIPT")
            // Old watchers too: their inotifyd outlives the script (owner's phone, 10 Nov 2026: 3 left over).
            sh("pkill -f cogansu_root_request.sh; pkill -f 'inotifyd - /data/system:wy'; rm -f $SEEN; setsid nohup sh $SCRIPT >/dev/null 2>&1 &")
        } else {
            sh("rm -f $SCRIPT $SEEN; pkill -f cogansu_root_request.sh; pkill -f 'inotifyd - /data/system:wy'")
        }
        // "seen" was the first build's state in prefs (24 KB on the owner's phone); it now lives in $SEEN.
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled).remove("seen") }
    }

    /** The script found [packageName] using root: ask, unless it already has root or must never be asked. */
    fun ask(context: Context, packageName: String, uid: Int) {
        if (!isEnabled(context) || packageName == context.packageName || packageName in neverAsk(context)) return
        if (Natives.getAppProfile(packageName, uid).allowSu) return
        notify(context, packageName, uid)
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
