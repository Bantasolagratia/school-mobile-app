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
