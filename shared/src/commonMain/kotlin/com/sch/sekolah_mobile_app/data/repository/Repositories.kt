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

