package com.sch.sekolah_mobile_app.data.repository

import com.sch.sekolah_mobile_app.data.model.Guru
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.remote.ApiClient
import com.sch.sekolah_mobile_app.data.storage.PlatformStorage
import com.sch.sekolah_mobile_app.data.storage.getPlatformStorage
import kotlinx.serialization.encodeToString

class AuthRepository(
    private val apiClient: ApiClient = ApiClient(),
    private val storage: PlatformStorage = getPlatformStorage()
) {
    companion object {
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_PROFILE_JSON = "auth_user_profile_json"
    }

    private var cachedToken: String? = null
    private var cachedProfile: UserProfileResponse? = null

    init {
        cachedToken = storage.getString(KEY_ACCESS_TOKEN, null)
        val profileJson = storage.getString(KEY_PROFILE_JSON, null)
        if (!profileJson.isNullOrBlank()) {
            try {
                cachedProfile = apiClient.json.decodeFromString<UserProfileResponse>(profileJson)
            } catch (_: Exception) {
                storage.remove(KEY_PROFILE_JSON)
            }
        }
    }

    fun hasActiveSession(): Boolean {
        return !cachedToken.isNullOrBlank()
    }

    fun getCachedProfile(): UserProfileResponse? = cachedProfile

    fun getAccessToken(): String? = cachedToken

    suspend fun login(email: String, pass: String): UserProfileResponse {
        val session = apiClient.login(email.trim(), pass)
        val token = session.accessToken
        cachedToken = token
        storage.setString(KEY_ACCESS_TOKEN, token)

        val profile = apiClient.fetchProfile(token)
        if (!profile.isRoleMurid) {
            logout()
            throw IllegalStateException("Akses ditolak: Akun ini bukan akun Murid/Siswa.")
        }

        // Validasi aturan Single Active User saat Ujian aktif:
        // Jika akun sedang terkunci di sesi ujian HP lain, login baru DITOLAK
        // Murid hanya bisa login setelah Guru Pengawas melakukan Reset Sesi / Kick.
        try {
            val heartbeat = apiClient.sendSessionHeartbeat(token)
            if (!heartbeat.active && heartbeat.action == "BLOCKED") {
                logout()
                throw IllegalStateException(heartbeat.message ?: "Akun ini sedang aktif dalam sesi ujian di perangkat lain. Silakan hubungi Guru Pengawas untuk melakukan Reset Sesi / Kick jika Anda ingin berganti perangkat.")
            }
        } catch (e: Exception) {
            if (e.message?.contains("aktif dalam sesi ujian") == true) {
                logout()
                throw e
            }
        }

        cachedProfile = profile
        try {
            val jsonStr = apiClient.json.encodeToString(profile)
            storage.setString(KEY_PROFILE_JSON, jsonStr)
        } catch (_: Exception) {}

        return profile
    }

    fun logout() {
        cachedToken = null
        cachedProfile = null
        storage.remove(KEY_ACCESS_TOKEN)
        storage.remove(KEY_PROFILE_JSON)
    }
}

class GuruRepository(
    private val apiClient: ApiClient = ApiClient(),
    private val authRepository: AuthRepository
) {
    suspend fun getDaftarGuru(): List<Guru> {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getDaftarGuru(token)
    }
}

class UjianRepository(
    private val apiClient: ApiClient = ApiClient(),
    private val authRepository: AuthRepository
) {
    suspend fun getUpcomingExams(kelas: String? = null): List<com.sch.sekolah_mobile_app.data.model.ExamScheduleItem> {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getUpcomingExams(token, kelas)
    }

    suspend fun sendHeartbeat(
        scheduleId: String? = null,
        status: String? = null,
        keterangan: String? = null
    ): com.sch.sekolah_mobile_app.data.model.SessionHeartbeatResponse {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.sendSessionHeartbeat(token, scheduleId, status, keterangan)
    }

    suspend fun examStart(
        scheduleId: String? = null,
        keterangan: String? = null
    ): com.sch.sekolah_mobile_app.data.model.SessionHeartbeatResponse {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.examStart(token, scheduleId, keterangan)
    }

    suspend fun examExit(
        scheduleId: String? = null,
        status: String? = "SELESAI",
        keterangan: String? = null
    ): Boolean {
        val token = authRepository.getAccessToken() ?: return false
        return try {
            apiClient.examExit(token, scheduleId, status, keterangan)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun verifyEmergencyExitKey(
        scheduleId: String,
        kelas: String?,
        key: String
    ): com.sch.sekolah_mobile_app.data.model.EmergencyExitVerifyResponse {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.verifyEmergencyExitKey(token, scheduleId, kelas, key)
    }

    suspend fun getUjianDetail(ujianId: String): com.sch.sekolah_mobile_app.data.model.UjianDetailMobile {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getUjianDetail(token, ujianId)
    }
}


