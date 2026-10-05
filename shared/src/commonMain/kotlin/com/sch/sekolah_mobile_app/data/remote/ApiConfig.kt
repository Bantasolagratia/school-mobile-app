package com.sch.sekolah_mobile_app.data.remote

import com.sch.sekolah_mobile_app.data.security.PlatformAppInfo
import com.sch.sekolah_mobile_app.data.storage.getDefaultServerHost
import com.sch.sekolah_mobile_app.data.storage.getPlatformStorage

object ApiConfig {
    const val AUTH_PORT = 8000
    const val API_PORT = 8080
    const val GATEWAY_PORT = 5173

    private const val KEY_SERVER_HOST = "cached_server_host"
    private const val KEY_HOST_APP_VERSION = "cached_server_host_app_version"
    private const val LEGACY_KEY_CUSTOM_HOST = "custom_server_host"

    fun getHost(): String {
        val storage = getPlatformStorage()

        // Bersihkan legacy key custom_server_host lama
        storage.remove(LEGACY_KEY_CUSTOM_HOST)

        val currentAppVersion = try {
            "${PlatformAppInfo.getVersionName()}_${PlatformAppInfo.getVersionCode()}"
        } catch (_: Exception) {
            "1.0_1"
        }

        val cachedVersion = storage.getString(KEY_HOST_APP_VERSION, null)
        val defaultHost = getDefaultServerHost()

        // Jika baru install, update versi, atau belum ada versi tersimpan:
        // baca ulang dari file konfigurasi (getDefaultServerHost) lalu masukkan ke cache
        if (cachedVersion != currentAppVersion) {
            storage.setString(KEY_SERVER_HOST, defaultHost)
            storage.setString(KEY_HOST_APP_VERSION, currentAppVersion)
            return defaultHost
        }

        val cachedHost = storage.getString(KEY_SERVER_HOST, null)?.trim()?.takeIf { it.isNotEmpty() }
        if (cachedHost.isNullOrEmpty()) {
            storage.setString(KEY_SERVER_HOST, defaultHost)
            return defaultHost
        }

        return cachedHost
    }

    fun getBaseUrl(): String {
        val host = getHost()
        val cleanHost = host.removePrefix("http://").removePrefix("https://")
        return if (cleanHost.contains(":")) {
            "http://$cleanHost"
        } else {
            "http://$cleanHost:$GATEWAY_PORT"
        }
    }

    fun getTokenUrl(): String =
        "${getBaseUrl()}/auth/token?grant_type=password"

    fun getRefreshTokenUrl(): String =
        "${getBaseUrl()}/auth/token?grant_type=refresh_token"

    fun getProfileUrl(): String =
        "${getBaseUrl()}/api/auth-flow/profile"

    fun getVerifyUrl(): String =
        "${getBaseUrl()}/api/auth-flow/verify"

    fun getRegisterUrl(): String =
        "${getBaseUrl()}/api/auth-flow/register"

    fun getGuruUrl(): String =
        "${getBaseUrl()}/api/management/guru"

    fun getCalendarUrl(): String =
        "${getBaseUrl()}/api/calendar"

    fun getStudentUpcomingExamsUrl(kelas: String? = null): String {
        val k = kelas?.trim()?.takeIf { it.isNotEmpty() }
        return if (k != null) "${getBaseUrl()}/api/ujian/schedule/student-upcoming?kelas=$k"
        else "${getBaseUrl()}/api/ujian/schedule/student-upcoming"
    }

    @Deprecated("Use getExamStartUrl() instead", replaceWith = ReplaceWith("getExamStartUrl()"))
    fun getSessionHeartbeatUrl(): String =
        "${getBaseUrl()}/api/ujian/schedule/exam-start"

    fun getExamStartUrl(): String =
        "${getBaseUrl()}/api/ujian/schedule/exam-start"

    fun getExamExitUrl(): String =
        "${getBaseUrl()}/api/ujian/schedule/exam-exit"

    fun getSubmitExamUrl(): String =
        "${getBaseUrl()}/api/ujian/result/submit"

    fun getVerifyExitKeyUrl(): String =
        "${getBaseUrl()}/api/ujian/schedule/verify-exit-key"

    fun getUjianDetailUrl(id: String): String =
        "${getBaseUrl()}/api/ujian/$id"

    fun getStudentExamHistoryUrl(kodeMapel: String): String =
        "${getBaseUrl()}/api/exam-history/student/$kodeMapel"

    fun getExamResultDetailUrl(resultId: String): String =
        "${getBaseUrl()}/api/ujian/result/detail/$resultId"

    fun getStudentMataPelajaranUrl(kelas: String? = null): String {
        val k = kelas?.trim()?.takeIf { it.isNotEmpty() }
        return if (k != null) "${getBaseUrl()}/api/student/mata-pelajaran/my-subjects?kelas=$k"
        else "${getBaseUrl()}/api/student/mata-pelajaran/my-subjects"
    }

    fun getMateriListUrl(kodeMapel: String): String =
        "${getBaseUrl()}/api/materi?kodeMapel=$kodeMapel&myOnly=false"

    fun getMateriDetailUrl(id: String): String =
        "${getBaseUrl()}/api/materi/$id"

    fun getMyRaportUrl(): String =
        "${getBaseUrl()}/api/raport/my-raport"

    fun getJadwalListUrl(tanggal: String? = null, kategori: String? = null): String {
        val params = mutableListOf<String>()
        if (!tanggal.isNullOrBlank()) params.add("tanggal=$tanggal")
        if (!kategori.isNullOrBlank()) params.add("kategori=$kategori")
        val q = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        return "${getBaseUrl()}/api/jadwal$q"
    }

    fun getJadwalDetailUrl(id: String): String =
        "${getBaseUrl()}/api/jadwal/$id"

    fun getGenerateQrUrl(id: String): String =
        "${getBaseUrl()}/api/jadwal/$id/qr/generate"

    fun getEmergencyPollUrl(nip: String): String =
        "${getBaseUrl()}/api/jadwal/kiosk/emergency-poll?nip=$nip"

    fun getScanQrUrl(): String =
        "${getBaseUrl()}/api/absensi/scan-qr"

    fun getAttendanceByScheduleUrl(idJadwal: String): String =
        "${getBaseUrl()}/api/absensi/jadwal/$idJadwal"

    fun getNotificationsUrl(unreadOnly: Boolean = false): String =
        "${getBaseUrl()}/api/notifications?unreadOnly=$unreadOnly"

    fun getUnreadNotificationsCountUrl(): String =
        "${getBaseUrl()}/api/notifications/unread-count"

    fun getMarkNotificationReadUrl(id: String): String =
        "${getBaseUrl()}/api/notifications/$id/read"

    fun getMarkAllNotificationsReadUrl(): String =
        "${getBaseUrl()}/api/notifications/read-all"

    fun getMyIzinListUrl(): String =
        "${getBaseUrl()}/api/izin/my-requests"

    fun getSubmitIzinUrl(): String =
        "${getBaseUrl()}/api/izin/request"

    fun getIzinDetailUrl(id: String): String =
        "${getBaseUrl()}/api/izin/$id"

    fun getIzinAttachmentUrl(id: String): String =
        "${getBaseUrl()}/api/izin/$id/attachment"

    fun getDefaultWaliKelasUrl(): String =
        "${getBaseUrl()}/api/izin/default-wali"

    fun getExamCardUrl(nis: String? = null): String {
        val n = nis?.trim()?.takeIf { it.isNotEmpty() }
        return if (n != null) "${getBaseUrl()}/api/ujian/room-allocation/my-card?nis=$n"
        else "${getBaseUrl()}/api/ujian/room-allocation/my-card"
    }
}
