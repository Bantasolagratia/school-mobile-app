package com.sch.sekolah_mobile_app.ui.screens.ujian

/**
 * Platform-independent WIB (Asia/Jakarta) date and time provider.
 * Returns Pair(date: "yyyy-MM-dd", time: "HH:mm")
 */
expect fun getCurrentWibDateTime(): Pair<String, String>

expect fun getCurrentEpochMillis(): Long

/**
 * Formats a raw timestamp or ISO-8601 string (e.g. "2026-10-01T13:47:00Z" or "07:30")
 * into clean, user-friendly 24h format "HH:mm".
 * Automatically converts UTC timestamps ending with 'Z' to Western Indonesia Time (WIB / UTC+7).
 */
fun formatScheduleTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "--:--"
    val trimmed = raw.trim()

    // Plain format e.g. "07:30" or "07:30:00"
    if (!trimmed.contains("T") && !trimmed.contains("Z") && !trimmed.contains("+")) {
        return trimmed.take(5)
    }

    if (trimmed.contains("T")) {
        val timeWithTz = trimmed.substringAfter("T")
        val isUtc = trimmed.endsWith("Z")
        val rawTime = timeWithTz.substringBefore("Z").substringBefore("+").substringBefore("-")
        val parts = rawTime.split(":")
        if (parts.size >= 2) {
            val hh = parts[0].toIntOrNull()
            val mm = parts[1].toIntOrNull()
            if (hh != null && mm != null) {
                val finalHh = if (isUtc) (hh + 7) % 24 else hh
                return "${finalHh.toString().padStart(2, '0')}:${mm.toString().padStart(2, '0')}"
            }
        }
        return rawTime.take(5)
    }

    return trimmed.take(5)
}

/**
 * Extracts date YYYY-MM-DD from an ISO string if present.
 */
fun extractScheduleDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    if (trimmed.contains("T")) return trimmed.substringBefore("T")
    if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') return trimmed.take(10)
    return null
}

