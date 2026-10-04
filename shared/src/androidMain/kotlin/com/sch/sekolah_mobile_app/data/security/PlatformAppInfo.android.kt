package com.sch.sekolah_mobile_app.data.security

import android.os.Build
import com.sch.sekolah_mobile_app.data.storage.AndroidPlatformContext

actual object PlatformAppInfo {

    actual fun getVersionCode(): Long {
        return try {
            val context = AndroidPlatformContext.get()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    actual fun getVersionName(): String {
        return try {
            val context = AndroidPlatformContext.get()
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    actual fun getPlatformName(): String = "ANDROID"
}
