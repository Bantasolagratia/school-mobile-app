package com.sch.sekolah_mobile_app.data.repository

import com.sch.sekolah_mobile_app.data.model.*
import com.sch.sekolah_mobile_app.data.remote.ApiClient
import com.sch.sekolah_mobile_app.data.storage.PlatformStorage
import com.sch.sekolah_mobile_app.data.storage.getPlatformStorage
import com.sch.sekolah_mobile_app.ui.screens.ujian.getCurrentEpochMillis
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString

class AuthRepository(
    val apiClient: ApiClient = ApiClient(),
    private val storage: PlatformStorage = getPlatformStorage()
) {
    companion object {
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_LAST_LOGIN_MS = "auth_last_login_timestamp"
        private const val KEY_PROFILE_JSON = "auth_user_profile_json"
        private const val MAX_SESSION_DURATION_MS = 14L * 24 * 3600 * 1000L // 14 hari batas mutlak sesi
    }

    private var cachedToken: String? = null
    private var cachedRefreshToken: String? = null
    private var cachedProfile: UserProfileResponse? = null

    private val refreshMutex = Mutex()
    private val _sessionTerminationFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sessionTerminationFlow: SharedFlow<String> = _sessionTerminationFlow.asSharedFlow()

    init {
        cachedToken = storage.getString(KEY_ACCESS_TOKEN, null)
        cachedRefreshToken = storage.getString(KEY_REFRESH_TOKEN, null)
        val profileJson = storage.getString(KEY_PROFILE_JSON, null)
        if (!profileJson.isNullOrBlank()) {
            try {
                cachedProfile = apiClient.json.decodeFromString<UserProfileResponse>(profileJson)
            } catch (_: Exception) {
                storage.remove(KEY_PROFILE_JSON)
            }
        }

        // Hubungkan callback auto silent refresh ke ApiClient
        apiClient.tokenRefresher = {
            if (performSilentRefresh()) {
                getAccessToken()
            } else {
                null
            }
        }
    }

    fun hasActiveSession(): Boolean {
        if (cachedToken.isNullOrBlank()) return false
        return isAbsoluteSessionValid()
    }

    fun isAbsoluteSessionValid(): Boolean {
        val lastLoginStr = storage.getString(KEY_LAST_LOGIN_MS, null) ?: return false
        val lastLoginMs = lastLoginStr.toLongOrNull() ?: return false
        val now = getCurrentEpochMillis()
        val elapsed = now - lastLoginMs
        // Perlindungan terhadap manipulasi jam sistem (diff < 0) atau melebihi 14 hari
        return elapsed in 0..MAX_SESSION_DURATION_MS
    }

    fun getCachedProfile(): UserProfileResponse? = cachedProfile

    fun getAccessToken(): String? = cachedToken

    fun getRefreshToken(): String? = cachedRefreshToken

    suspend fun login(email: String, pass: String): UserProfileResponse {
        val session = apiClient.login(email.trim(), pass)
        val token = session.accessToken
        cachedToken = token
        cachedRefreshToken = session.refreshToken
        storage.setString(KEY_ACCESS_TOKEN, token)
        if (!session.refreshToken.isNullOrBlank()) {
            storage.setString(KEY_REFRESH_TOKEN, session.refreshToken)
        }
        storage.setString(KEY_LAST_LOGIN_MS, getCurrentEpochMillis().toString())

        val profile = apiClient.fetchProfile(token)
        if (!profile.isRoleMurid && !profile.isRoleGuru) {
            logout()
            throw IllegalStateException("Akses ditolak: Akun ini bukan akun Murid atau Guru.")
        }

        if (profile.isRoleMurid) {
            // Validasi aturan Single Active User saat Ujian aktif:
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
        }

        cachedProfile = profile
        try {
            val jsonStr = apiClient.json.encodeToString(profile)
            storage.setString(KEY_PROFILE_JSON, jsonStr)
        } catch (_: Exception) {}

        return profile
    }

    suspend fun performSilentRefresh(): Boolean {
        return refreshMutex.withLock {
            // 1. Validasi batas mutlak 14 hari
            if (!isAbsoluteSessionValid()) {
                purgeSessionAndMemory("Sesi login Anda telah melewati batas 14 hari. Silakan masuk kembali.")
                return@withLock false
            }

            val currentRefreshToken = cachedRefreshToken ?: storage.getString(KEY_REFRESH_TOKEN, null)
            if (currentRefreshToken.isNullOrBlank()) {
                purgeSessionAndMemory("Refresh token tidak ditemukan. Silakan masuk kembali.")
                return@withLock false
            }

            // 2. Double-Checked Locking: cek apakah coroutine lain telah me-refresh token
            val storedToken = storage.getString(KEY_ACCESS_TOKEN, null)
            if (!cachedToken.isNullOrBlank() && storedToken != null && cachedToken != storedToken) {
                cachedToken = storedToken
                cachedRefreshToken = storage.getString(KEY_REFRESH_TOKEN, null)
                return@withLock true
            }

            // 3. Eksekusi Refresh Token ke Gateway/GoTrue
            try {
                val newSession = apiClient.refreshToken(currentRefreshToken)
                val newAccess = newSession.accessToken
                val newRefresh = newSession.refreshToken
                if (newAccess.isBlank()) {
                    purgeSessionAndMemory("Gagal memperbarui sesi otentikasi.")
                    return@withLock false
                }
                cachedToken = newAccess
                storage.setString(KEY_ACCESS_TOKEN, newAccess)
                if (!newRefresh.isNullOrBlank()) {
                    cachedRefreshToken = newRefresh
                    storage.setString(KEY_REFRESH_TOKEN, newRefresh)
                }
                return@withLock true
            } catch (e: Exception) {
                purgeSessionAndMemory("Sesi login telah kedaluwarsa. Silakan masuk kembali.")
                return@withLock false
            }
        }
    }

    fun purgeSessionAndMemory(reason: String = "Session expired") {
        cachedToken = null
        cachedRefreshToken = null
        cachedProfile = null
        storage.remove(KEY_ACCESS_TOKEN)
        storage.remove(KEY_REFRESH_TOKEN)
        storage.remove(KEY_PROFILE_JSON)
        storage.remove(KEY_LAST_LOGIN_MS)
        RemoteConfigManager.instance.resetToSafeDefaults()
        _sessionTerminationFlow.tryEmit(reason)
    }

    fun logout() {
        purgeSessionAndMemory("Logout manual oleh pengguna")
    }
}

class GuruRepository(
    private val authRepository: AuthRepository,
    private val apiClient: ApiClient = authRepository.apiClient
) {
    suspend fun getDaftarGuru(): List<Guru> {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getDaftarGuru(token)
    }
}

class UjianRepository(
    private val authRepository: AuthRepository,
    private val apiClient: ApiClient = authRepository.apiClient
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

    suspend fun submitExam(
        scheduleId: String? = null,
        durationSeconds: Long = 0,
        answers: Map<String, String> = emptyMap()
    ): com.sch.sekolah_mobile_app.data.model.SubmitExamResponseMobile {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.submitExam(token, scheduleId, durationSeconds, answers)
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

    suspend fun fetchImageBytes(pathOrUrl: String): ByteArray {
        val token = authRepository.getAccessToken()
        return apiClient.fetchImageBytes(pathOrUrl, token)
    }

    suspend fun getStudentExamHistory(kodeMapel: String): com.sch.sekolah_mobile_app.data.model.StudentSubjectExamHistoryResponseMobile {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getStudentExamHistory(token, kodeMapel)
    }

    suspend fun getExamResultDetail(resultId: String): com.sch.sekolah_mobile_app.data.model.ExamResultDetailResponseMobile {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getExamResultDetail(token, resultId)
    }
}

class MataPelajaranRepository(
    private val authRepository: AuthRepository,
    private val apiClient: ApiClient = authRepository.apiClient
) {
    suspend fun getStudentSubjects(kelas: String? = null): List<StudentMataPelajaranItem> {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getStudentSubjects(token, kelas)
    }

    suspend fun getMateriList(kodeMapel: String): List<MateriItem> {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getMateriList(token, kodeMapel)
    }

    suspend fun getMateriDetail(materiId: String): MateriItem {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getMateriDetail(token, materiId)
    }

    suspend fun fetchImageBytes(pathOrUrl: String): ByteArray {
        val token = authRepository.getAccessToken()
        return apiClient.fetchImageBytes(pathOrUrl, token)
    }
}

class RaportRepository(
    private val authRepository: AuthRepository,
    private val apiClient: ApiClient = authRepository.apiClient
) {
    suspend fun getMyRaport(): StudentRaportResponse {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getMyRaport(token)
    }
}

class JadwalRepository(
    private val authRepository: AuthRepository,
    private val apiClient: ApiClient = authRepository.apiClient
) {
    suspend fun getSchedules(tanggal: String? = null, kategori: String? = null): List<Jadwal> {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getJadwalList(token, tanggal, kategori)
    }

    suspend fun getScheduleDetail(id: String): Jadwal {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getJadwalDetail(token, id)
    }

    suspend fun generateAttendanceQr(id: String): QrPayloadMobile {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.generateAttendanceQr(token, id)
    }

    suspend fun pollEmergencyUnlock(nip: String): EmergencyPollResponse {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.pollEmergencyUnlock(token, nip)
    }

    suspend fun scanQrCode(tokenStr: String, latitude: Double? = null, longitude: Double? = null): AbsensiResultMobile {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        val req = ScanQrRequestMobile(token = tokenStr, latitude = latitude, longitude = longitude)
        return apiClient.scanQrCode(token, req)
    }

    suspend fun getAttendanceList(idJadwal: String): List<AbsensiResultMobile> {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getAttendanceBySchedule(token, idJadwal)
    }
}

class NotificationRepository(
    private val authRepository: AuthRepository,
    private val apiClient: ApiClient = authRepository.apiClient
) {
    suspend fun getNotifications(unreadOnly: Boolean = false): List<NotificationItemMobile> {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.getNotifications(token, unreadOnly)
    }

    suspend fun getUnreadCount(): Long {
        val token = authRepository.getAccessToken() ?: return 0
        return apiClient.getUnreadNotificationsCount(token)
    }

    suspend fun markAsRead(id: String): NotificationItemMobile {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        return apiClient.markNotificationRead(token, id)
    }

    suspend fun markAllAsRead() {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("Sesi login tidak ditemukan. Silakan masuk kembali.")
        apiClient.markAllNotificationsRead(token)
    }
}


