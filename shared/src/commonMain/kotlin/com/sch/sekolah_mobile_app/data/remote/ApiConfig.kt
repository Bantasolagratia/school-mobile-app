package com.sch.sekolah_mobile_app.data.remote

import com.sch.sekolah_mobile_app.data.storage.getDefaultServerHost
import com.sch.sekolah_mobile_app.data.storage.getPlatformStorage

object ApiConfig {
    const val AUTH_PORT = 8000
    const val API_PORT = 8080
    const val GATEWAY_PORT = 5173

    private const val KEY_CUSTOM_HOST = "custom_server_host"

    fun getHost(): String {
        val storage = getPlatformStorage()
        val custom = storage.getString(KEY_CUSTOM_HOST, null)?.trim()?.takeIf { it.isNotEmpty() }
        if (custom == "10.0.2.2" || custom == "localhost") {
            storage.remove(KEY_CUSTOM_HOST)
            return getDefaultServerHost()
        }
        return custom ?: getDefaultServerHost()
    }

    fun setHost(newHost: String) {
        val storage = getPlatformStorage()
        val clean = newHost.trim().ifEmpty { getDefaultServerHost() }
        storage.setString(KEY_CUSTOM_HOST, clean)
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

    fun getGuruUrl(): String =
        "${getBaseUrl()}/api/management/guru"

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

    fun getVerifyExitKeyUrl(): String =
        "${getBaseUrl()}/api/ujian/schedule/verify-exit-key"

    fun getUjianDetailUrl(id: String): String =
        "${getBaseUrl()}/api/ujian/$id"

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
}
