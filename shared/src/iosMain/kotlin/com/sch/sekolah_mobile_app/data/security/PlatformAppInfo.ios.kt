package com.sch.sekolah_mobile_app.data.security

actual object PlatformAppInfo {
    actual fun getVersionCode(): Long = 5L
    actual fun getVersionName(): String = "1.2.0"
    actual fun getPlatformName(): String = "IOS"
}
