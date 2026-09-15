package com.sch.sekolah_mobile_app.data.storage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences

object AndroidPlatformContext {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

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

actual fun getDefaultServerHost(): String = "192.168.18.94"

actual fun copyToClipboard(label: String, text: String) {
    val context = AndroidPlatformContext.get()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

