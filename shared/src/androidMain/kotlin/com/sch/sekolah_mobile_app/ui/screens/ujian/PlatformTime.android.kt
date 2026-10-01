package com.sch.sekolah_mobile_app.ui.screens.ujian

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val ZONE_WIB = ZoneId.of("Asia/Jakarta")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

actual fun getCurrentWibDateTime(): Pair<String, String> {
    return try {
        val now = ZonedDateTime.now(ZONE_WIB)
        val todayStr = now.format(DATE_FORMATTER)
        val nowTimeStr = now.format(TIME_FORMATTER)
        Pair(todayStr, nowTimeStr)
    } catch (e: Exception) {
        Pair("", "")
    }
}

actual fun getCurrentEpochMillis(): Long = System.currentTimeMillis()

actual fun isIsoTimestampPast(isoString: String?): Boolean {
    if (isoString.isNullOrBlank()) return false
    val clean = isoString.trim()
    return try {
        val instant = Instant.parse(clean)
        Instant.now().isAfter(instant)
    } catch (_: Exception) {
        try {
            val ldt = LocalDateTime.parse(clean.substringBefore("Z").substringBefore("+"))
            LocalDateTime.now(ZONE_WIB).isAfter(ldt)
        } catch (_: Exception) {
            false
        }
    }
}

actual fun extractScheduleDate(dateTimeStr: String?): String? {
    if (dateTimeStr.isNullOrBlank()) return null
    val clean = dateTimeStr.trim()
    return try {
        if (clean.contains("T")) {
            val instant = Instant.parse(clean)
            instant.atZone(ZONE_WIB).format(DATE_FORMATTER)
        } else if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') {
            clean.take(10)
        } else {
            null
        }
    } catch (_: Exception) {
        if (clean.contains("T")) clean.substringBefore("T")
        else if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') clean.take(10)
        else null
    }
}

actual fun formatScheduleTime(timeStr: String?): String {
    if (timeStr.isNullOrBlank()) return "--:--"
    val clean = timeStr.trim()
    return try {
        if (clean.contains("T")) {
            val instant = Instant.parse(clean)
            instant.atZone(ZONE_WIB).format(TIME_FORMATTER)
        } else {
            val plain = clean.replace("Z", "").trim()
            val timePart = if (plain.contains(" ")) plain.substringAfter(" ") else plain
            val parts = timePart.split(":")
            if (parts.size >= 2) {
                "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
            } else {
                plain.take(5)
            }
        }
    } catch (_: Exception) {
        clean.take(5)
    }
}
