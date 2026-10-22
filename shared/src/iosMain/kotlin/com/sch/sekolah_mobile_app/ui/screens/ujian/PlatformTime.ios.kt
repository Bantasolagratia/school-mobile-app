package com.sch.sekolah_mobile_app.ui.screens.ujian

import platform.Foundation.*
import platform.Foundation.timeIntervalSince1970

actual fun getCurrentWibDateTime(): Pair<String, String> {
    return try {
        val date = NSDate()
        val formatter = NSDateFormatter()
        formatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta") ?: NSTimeZone.defaultTimeZone
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
            wibFormatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta") ?: NSTimeZone.defaultTimeZone
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
            wibFormatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta") ?: NSTimeZone.defaultTimeZone
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
        wibFormatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta") ?: NSTimeZone.defaultTimeZone
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
            val date = isoFormatter.dateFromString(clean)
            if (date != null) {
                return (date.timeIntervalSince1970 * 1000.0).toLong()
            }
            val wibFormatter = NSDateFormatter()
            wibFormatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta") ?: NSTimeZone.defaultTimeZone
            wibFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
            val cleanNoZ = clean.substringBefore("Z").substringBefore("+")
            val fallbackDate = wibFormatter.dateFromString(cleanNoZ)
            if (fallbackDate != null) {
                return (fallbackDate.timeIntervalSince1970 * 1000.0).toLong()
            }
        }
        val formatter = NSDateFormatter()
        formatter.timeZone = NSTimeZone.timeZoneWithName("Asia/Jakarta") ?: NSTimeZone.defaultTimeZone
        if (clean.contains(" ")) {
            val parts = clean.split(" ")
            val datePart = parts[0]
            val timePart = parts[1]
            val timeTokens = timePart.split(":")
            val h = timeTokens.getOrNull(0)?.padStart(2, '0') ?: "00"
            val m = timeTokens.getOrNull(1)?.padStart(2, '0') ?: "00"
            formatter.dateFormat = "yyyy-MM-dd HH:mm"
            val normalized = "$datePart $h:$m"
            val date = formatter.dateFromString(normalized) ?: return null
            (date.timeIntervalSince1970 * 1000.0).toLong()
        } else if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') {
            formatter.dateFormat = "yyyy-MM-dd"
            val date = formatter.dateFromString(clean.take(10)) ?: return null
            (date.timeIntervalSince1970 * 1000.0).toLong()
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
