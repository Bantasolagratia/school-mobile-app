package com.sch.sekolah_mobile_app.data.security

actual object PlatformAppInstaller {
    actual fun canInstallPackages(): Boolean = false
    actual fun openInstallPermissionSettings() {}
    actual fun installApk(filePath: String): Boolean = false
    actual fun getDownloadDirectory(): String = ""
}
