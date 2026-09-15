package com.sch.sekolah_mobile_app.data.storage

import platform.Foundation.NSUserDefaults
import platform.UIKit.UIPasteboard

actual class PlatformStorage(private val userDefaults: NSUserDefaults) {
    actual fun getString(key: String, defaultValue: String?): String? {
        return userDefaults.stringForKey(key) ?: defaultValue
    }

    actual fun setString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }

    actual fun clear() {
        val dictionary = userDefaults.dictionaryRepresentation()
        for (key in dictionary.keys) {
            val keyString = key.toString()
            userDefaults.removeObjectForKey(keyString)
        }
    }
}

actual fun getPlatformStorage(): PlatformStorage {
    return PlatformStorage(NSUserDefaults.standardUserDefaults)
}

actual fun getDefaultServerHost(): String = "localhost"

actual fun copyToClipboard(label: String, text: String) {
    UIPasteboard.generalPasteboard.string = text
}

