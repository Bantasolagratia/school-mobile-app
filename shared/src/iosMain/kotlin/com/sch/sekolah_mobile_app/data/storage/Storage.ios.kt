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

actual fun getDefaultServerHost(): String = "https://development.taile61966.ts.net"

actual fun copyToClipboard(label: String, text: String) {
    UIPasteboard.generalPasteboard.string = text
}

actual fun showSystemNotification(id: String, title: String, message: String) {
    try {
        val center = platform.UserNotifications.UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            platform.UserNotifications.UNAuthorizationOptionAlert or
                platform.UserNotifications.UNAuthorizationOptionSound or
                platform.UserNotifications.UNAuthorizationOptionBadge
        ) { granted, _ ->
            if (granted) {
                val content = platform.UserNotifications.UNMutableNotificationContent().apply {
                    setTitle(title)
                    setBody(message)
                    setSound(platform.UserNotifications.UNNotificationSound.defaultSound())
                }
                val request = platform.UserNotifications.UNNotificationRequest.requestWithIdentifier(
                    identifier = id,
                    content = content,
                    trigger = null
                )
                center.addNotificationRequest(request, withCompletionHandler = null)
            }
        }
    } catch (_: Exception) {
    }
}

