package com.sch.sekolah_mobile_app.ui.screens.ujian

import com.sch.sekolah_mobile_app.data.model.Jadwal

/**
 * Platform-independent WIB (Asia/Jakarta) date and time provider.
 * Returns Pair(date: "yyyy-MM-dd", time: "HH:mm")
 */
expect fun getCurrentWibDateTime(): Pair<String, String>

expect fun getCurrentEpochMillis(): Long

expect fun isIsoTimestampPast(isoString: String?): Boolean

/**
 * Extracts yyyy-MM-dd date in Western Indonesia Time (WIB / UTC+7).
 */
expect fun extractScheduleDate(dateTimeStr: String?): String?

/**
 * Formats time as HH:mm in Western Indonesia Time (WIB / UTC+7).
 */
expect fun formatScheduleTime(timeStr: String?): String

/**
 * Parses an ISO 8601 string or date/time string to epoch millis.
 */
expect fun parseScheduleToEpochMillis(dateTimeStr: String?): Long?

/**
 * Checks whether the schedule has already ended.
 */
fun isSchedulePast(waktuSelesai: String?, waktuMulai: String? = null, waktu: String? = null): Boolean {
    val targetEndTime = waktuSelesai?.trim()
    if (!targetEndTime.isNullOrBlank()) {
        if (targetEndTime.contains("T")) {
            return isIsoTimestampPast(targetEndTime)
        }
        val (todayStr, nowTimeStr) = getCurrentWibDateTime()
        val scheduleDate = extractScheduleDate(waktuMulai) ?: extractScheduleDate(waktu)
        if (scheduleDate != null) {
            if (scheduleDate < todayStr) return true
            if (scheduleDate > todayStr) return false
        }
        val endHhMm = formatScheduleTime(targetEndTime)
        if (endHhMm != "--:--") {
            return endHhMm < nowTimeStr
        }
    }

    val targetStartTime = (waktuMulai ?: waktu)?.trim()
    if (!targetStartTime.isNullOrBlank() && targetStartTime.contains("T")) {
        return isIsoTimestampPast(targetStartTime)
    }

    return false
}

/**
 * Checks whether a schedule spans or includes the target date (yyyy-MM-dd).
 * For example: A schedule starting on 2026-10-01 07:00 and ending on 2026-10-02 00:40
 * will return true for both 2026-10-01 and 2026-10-02.
 */
fun isScheduleOnDate(targetDate: String, waktuMulai: String?, waktuSelesai: String?, waktu: String? = null): Boolean {
    if (targetDate.isBlank()) return false
    val startDate = extractScheduleDate(waktuMulai) ?: extractScheduleDate(waktu)
    if (startDate == null) {
        return waktuMulai?.startsWith(targetDate) == true || waktu?.startsWith(targetDate) == true
    }
    val endDate = extractScheduleDate(waktuSelesai) ?: startDate
    return targetDate in startDate..endDate
}

fun isScheduleOnDate(targetDate: String, jadwal: Jadwal): Boolean {
    return isScheduleOnDate(targetDate, jadwal.waktuMulai, jadwal.waktuSelesai, jadwal.waktu)
}

/**
 * Calculates proximity distance of a schedule to current time in milliseconds.
 * Returns:
 * - 0L for currently active / ongoing sessions (within [start, end] and not ended)
 * - Positive difference (start - now) for upcoming sessions (the sooner, the smaller)
 * - 1_000_000_000L + elapsed (now - end) for past / ended sessions (most recently ended first, but behind upcoming)
 */
fun getScheduleProximity(jadwal: Jadwal, nowMillis: Long = getCurrentEpochMillis()): Long {
    val startMillis = parseScheduleToEpochMillis(jadwal.waktuMulai ?: jadwal.waktu)
    val endMillis = parseScheduleToEpochMillis(jadwal.waktuSelesai)
        ?: startMillis?.plus(90 * 60 * 1000L) // Default 90 menit jika waktuSelesai kosong

    if (startMillis == null && endMillis == null) return Long.MAX_VALUE
    val s = startMillis ?: endMillis!!
    val e = endMillis ?: (s + 90 * 60 * 1000L)

    val isEnded = isSchedulePast(jadwal.waktuSelesai, jadwal.waktuMulai, jadwal.waktu) ||
            "SELESAI".equals(jadwal.status, ignoreCase = true)

    return when {
        // Sedang berlangsung saat ini: prioritas utama (paling atas)
        !isEnded && nowMillis in s..e -> 0L

        // Akan datang: diurutkan dari yang paling dekat waktu mulainya
        !isEnded && nowMillis < s -> s - nowMillis

        // Sudah selesai / lampau: diurutkan dari yang paling baru saja selesai,
        // diberi offset 1_000_000_000L agar sesi aktif & mendatang hari ini/besok tetap di atas
        else -> {
            val elapsed = if (nowMillis > e) nowMillis - e else 0L
            1_000_000_000L + elapsed
        }
    }
}

