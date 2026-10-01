package com.sch.sekolah_mobile_app.ui.screens.ujian

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithName

actual fun getCurrentWibDateTime(): Pair<String, String> {
    return try {
        val date = NSDate()
        val formatter = NSDateFormatter()
        formatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta")
        formatter.dateFormat = "yyyy-MM-dd"
        val dateStr = formatter.stringFromDate(date)
        formatter.dateFormat = "HH:mm"
        val timeStr = formatter.stringFromDate(date)
        Pair(dateStr, timeStr)
    } catch (e: Exception) {
        Pair("", "")
    }
}

actual fun getCurrentEpochMillis(): Long = (platform.Foundation.NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun isIsoTimestampPast(isoString: String?): Boolean {
    if (isoString.isNullOrBlank()) return false
    return try {
        val formatter = platform.Foundation.NSISO8601DateFormatter()
        val date = formatter.dateFromString(isoString.trim()) ?: return false
        date.timeIntervalSinceNow < 0
    } catch (_: Exception) {
        false
    }
}

