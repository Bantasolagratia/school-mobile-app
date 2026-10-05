package com.sch.sekolah_mobile_app.ui.screens.ujian

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSISO8601DateFormatter
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
        val formatter = NSISO8601DateFormatter()
        val date = formatter.dateFromString(isoString.trim()) ?: return false
        date.timeIntervalSinceNow < 0
    } catch (_: Exception) {
        false
    }
}

actual fun extractScheduleDate(dateTimeStr: String?): String? {
    if (dateTimeStr.isNullOrBlank()) return null
    val clean = dateTimeStr.trim()
    return try {
        if (clean.contains("T")) {
            val isoFormatter = NSISO8601DateFormatter()
            val date = isoFormatter.dateFromString(clean) ?: return clean.substringBefore("T")
            val wibFormatter = NSDateFormatter()
            wibFormatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta")
            wibFormatter.dateFormat = "yyyy-MM-dd"
            wibFormatter.stringFromDate(date)
        } else if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') {
            clean.take(10)
        } else {
            null
        }
    } catch (_: Exception) {
        clean.take(10)
    }
}

actual fun formatScheduleTime(timeStr: String?): String {
    if (timeStr.isNullOrBlank()) return "--:--"
    val clean = timeStr.trim()
    return try {
        if (clean.contains("T")) {
            val isoFormatter = NSISO8601DateFormatter()
            val date = isoFormatter.dateFromString(clean) ?: return clean.substringAfter("T").take(5)
            val wibFormatter = NSDateFormatter()
            wibFormatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta")
            wibFormatter.dateFormat = "HH:mm"
            wibFormatter.stringFromDate(date)
        } else {
            clean.take(5)
        }
    } catch (_: Exception) {
        clean.take(5)
    }
}

actual fun formatEpochMillisToTime(epochMillis: Long): String {
    return try {
        val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
        val wibFormatter = NSDateFormatter()
        wibFormatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta")
        wibFormatter.dateFormat = "HH:mm"
        wibFormatter.stringFromDate(date)
    } catch (_: Exception) {
        "--:--"
    }
}

actual fun parseScheduleToEpochMillis(dateTimeStr: String?): Long? {
    if (dateTimeStr.isNullOrBlank()) return null
    val clean = dateTimeStr.trim()
    return try {
        if (clean.contains("T")) {
            val isoFormatter = NSISO8601DateFormatter()
            val date = isoFormatter.dateFromString(clean) ?: return null
            (date.timeIntervalSince1970 * 1000.0).toLong()
        } else {
            val formatter = NSDateFormatter()
            formatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta")
            if (clean.length >= 16 && clean.contains(" ")) {
                formatter.dateFormat = "yyyy-MM-dd HH:mm"
            } else if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') {
                formatter.dateFormat = "yyyy-MM-dd"
            } else {
                return null
            }
            val date = formatter.dateFromString(clean) ?: return null
            (date.timeIntervalSince1970 * 1000.0).toLong()
        }
    } catch (_: Exception) {
        null
    }
}

