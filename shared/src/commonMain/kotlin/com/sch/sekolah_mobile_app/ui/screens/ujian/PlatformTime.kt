package com.sch.sekolah_mobile_app.ui.screens.ujian

/**
 * Platform-independent WIB (Asia/Jakarta) date and time provider.
 * Returns Pair(date: "yyyy-MM-dd", time: "HH:mm")
 */
expect fun getCurrentWibDateTime(): Pair<String, String>

expect fun getCurrentEpochMillis(): Long

/**
 * Strips raw ISO/UTC markers (like .000Z, 00Z, T) and formats time as HH:mm cleanly.
 */
fun formatScheduleTime(timeStr: String?): String {
    if (timeStr.isNullOrBlank()) return "--:--"
    val clean = timeStr.trim()
    val timePart = if (clean.contains("T")) {
        clean.substringAfter("T").replace("Z", "").substringBefore("+").substringBefore("-")
    } else if (clean.contains(" ")) {
        clean.substringAfter(" ")
    } else {
        clean.replace("Z", "")
    }

    val parts = timePart.split(":")
    return if (parts.size >= 2) {
        val hour = parts[0].padStart(2, '0')
        val min = parts[1].padStart(2, '0')
        "$hour:$min"
    } else {
        timePart
    }
}

/**
 * Extracts yyyy-MM-dd date part from ISO string or date-time string.
 */
fun extractScheduleDate(dateTimeStr: String?): String? {
    if (dateTimeStr.isNullOrBlank()) return null
    val clean = dateTimeStr.trim()
    if (clean.contains("T")) {
        return clean.substringBefore("T")
    }
    if (clean.contains(" ")) {
        val first = clean.substringBefore(" ")
        if (first.contains("-")) return first
    }
    if (clean.contains("-") && clean.length >= 8) {
        return clean.take(10)
    }
    return null
}

