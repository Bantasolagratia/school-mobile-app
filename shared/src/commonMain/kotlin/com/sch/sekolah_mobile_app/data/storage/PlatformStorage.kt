package com.sch.sekolah_mobile_app.data.storage

expect class PlatformStorage {
    fun getString(key: String, defaultValue: String? = null): String?
    fun setString(key: String, value: String)
    fun remove(key: String)
    fun clear()
}

expect fun getPlatformStorage(): PlatformStorage
expect fun getDefaultServerHost(): String
expect fun copyToClipboard(label: String, text: String)

