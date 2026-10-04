package com.sch.sekolah_mobile_app.data.security

actual object PlatformAppInfo {
    actual fun getVersionCode(): Long = 1L
    actual fun getVersionName(): String = "1.0"
    actual fun getPlatformName(): String = "IOS"
}
