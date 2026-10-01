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
