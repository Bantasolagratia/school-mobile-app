package com.sch.sekolah_mobile_app.data.storage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences

object AndroidPlatformContext {
    private const val DEFAULT_SIT_HOST = "https://development.taile61966.ts.net"

    private var appContext: Context? = null
    private var currentActivity: java.lang.ref.WeakReference<android.app.Activity>? = null

    // Host server dinamis disuntikkan dari BuildConfig modul app saat startup via MainActivity.kt init
    var serverHost: String = DEFAULT_SIT_HOST
        private set

    fun init(context: Context, serverHost: String? = null) {
        appContext = context.applicationContext
        if (!serverHost.isNullOrBlank()) {
            this.serverHost = serverHost.trim()
        }
        if (context is android.app.Activity) {
            currentActivity = java.lang.ref.WeakReference(context)
        }
    }

    fun setActivity(activity: android.app.Activity) {
        currentActivity = java.lang.ref.WeakReference(activity)
    }

    fun getActivity(): android.app.Activity? = currentActivity?.get()

    fun get(): Context = appContext ?: throw IllegalStateException("AndroidPlatformContext not initialized")
}

actual class PlatformStorage(private val prefs: SharedPreferences) {
    actual fun getString(key: String, defaultValue: String?): String? =
        prefs.getString(key, defaultValue)

    actual fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    actual fun clear() {
        prefs.edit().clear().apply()
    }
}

actual fun getPlatformStorage(): PlatformStorage {
    val context = AndroidPlatformContext.get()
    val prefs = context.getSharedPreferences("sekolah_mobile_cmp_prefs", Context.MODE_PRIVATE)
    return PlatformStorage(prefs)
}

actual fun getDefaultServerHost(): String = AndroidPlatformContext.serverHost

actual fun copyToClipboard(label: String, text: String) {
    val context = AndroidPlatformContext.get()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

actual fun showSystemNotification(id: String, title: String, message: String) {
    try {
        val context = AndroidPlatformContext.get()
        val channelId = "sekolah_alerts_high_v1"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(channelId)
            if (existing == null) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Notifikasi Akademik & Sekolah",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi pop-up pengumuman, absensi, jadwal, dan ujian"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = if (launchIntent != null) {
            android.app.PendingIntent.getActivity(
                context,
                id.hashCode(),
                launchIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val smallIconRes = if (context.applicationInfo.icon != 0) {
            context.applicationInfo.icon
        } else {
            android.R.drawable.ic_dialog_info
        }

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        notificationManager.notify(id.hashCode(), builder.build())
    } catch (_: Exception) {
    }
}

