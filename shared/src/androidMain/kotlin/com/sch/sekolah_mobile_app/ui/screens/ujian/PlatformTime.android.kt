package com.sch.sekolah_mobile_app.ui.screens.ujian

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

actual fun getCurrentWibDateTime(): Pair<String, String> {
    return try {
        val now = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
        val todayStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val nowTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))
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
        val instant = java.time.Instant.parse(clean)
        java.time.Instant.now().isAfter(instant)
    } catch (_: Exception) {
        try {
            val ldt = java.time.LocalDateTime.parse(clean.substringBefore("Z").substringBefore("+"))
            java.time.LocalDateTime.now(ZoneId.of("Asia/Jakarta")).isAfter(ldt)
        } catch (_: Exception) {
            false
        }
    }
}

