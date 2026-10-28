package com.sch.sekolah_mobile_app.data.security

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.sch.sekolah_mobile_app.data.storage.AndroidPlatformContext
import java.io.File

actual object PlatformAppInstaller {

    actual fun canInstallPackages(): Boolean {
        return try {
            val context = AndroidPlatformContext.get()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    actual fun openInstallPermissionSettings() {
        try {
            val context = AndroidPlatformContext.get()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    actual fun installApk(filePath: String): Boolean {
        return try {
            val context = AndroidPlatformContext.get()
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                return false
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    actual fun getDownloadDirectory(): String {
        return try {
            val context = AndroidPlatformContext.get()
            val dir = File(context.cacheDir, "updates")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir.absolutePath
        } catch (e: Exception) {
            ""
        }
    }
}
