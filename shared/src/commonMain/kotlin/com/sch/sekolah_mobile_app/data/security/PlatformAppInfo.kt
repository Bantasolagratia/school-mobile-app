package com.sch.sekolah_mobile_app.data.security

expect object PlatformAppInfo {
    fun getVersionCode(): Long
    fun getVersionName(): String
    fun getPlatformName(): String
}
