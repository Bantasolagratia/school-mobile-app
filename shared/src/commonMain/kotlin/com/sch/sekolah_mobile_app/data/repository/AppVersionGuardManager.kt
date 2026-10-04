package com.sch.sekolah_mobile_app.data.repository

import com.sch.sekolah_mobile_app.data.model.DecryptedAppVersionPayload
import com.sch.sekolah_mobile_app.data.model.EncryptedVersionGuardResponse
import com.sch.sekolah_mobile_app.data.model.VersionCheckResult
import com.sch.sekolah_mobile_app.data.remote.ApiConfig
import com.sch.sekolah_mobile_app.data.security.PlatformAppInfo
import com.sch.sekolah_mobile_app.data.security.PlatformCrypto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

class AppVersionGuardManager private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 8_000
        }
    }

    // Public Key RSA-2048 & Pre-shared AES Key
    private val RSA_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlSTjQn9RkClMD1o702Sg2B9yoEuP8v5jR89RQtoaDa1m1NvXo3Xoe3TXgqMJVQjLJz6Ln5++lro/VmZqHE/6XxXzbSkUTdX2/nFie6Me9LHKb9KKIQ6lFmgnOqvMNRWhyDZaB4cUQCl1rN+UQMb/uLTXkt9BR5ILd3xO9NzgVPzYFbDNUjYK0WCgGJHVdJKPWxcUjm8XqzINS2x59NtQHMUwXrrR+sMLvqEXGTCDpwKa/GvV21xp2vwXLJkzUQ72LU+4fZeJw1e67xEqiH31UE64ZP/bwJG16zN8Wz1G+gHKZyEwbioHRDcXYWi+QiYTzlBcKcGUolx2Gufw18/A/wIDAQAB"
    private val AES_KEY = "wSiVQQgbj3kHZlZKnCKoupbDeIarM13dgGBHMpgwybQ="

    private val _versionCheckResult = MutableStateFlow<VersionCheckResult>(VersionCheckResult.Idle)
    val versionCheckResult: StateFlow<VersionCheckResult> = _versionCheckResult.asStateFlow()

    companion object {
        val instance: AppVersionGuardManager by lazy { AppVersionGuardManager() }
    }

    /**
     * Memeriksa versi aplikasi ke server dengan payload terenkripsi dan tanda tangan digital.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun checkAppVersion() {
        scope.launch {
            _versionCheckResult.value = VersionCheckResult.Checking

            val platform = PlatformAppInfo.getPlatformName()
            val currentCode = PlatformAppInfo.getVersionCode()
            val clientNonce = generateRandomNonce()

            val baseUrl = ApiConfig.getBaseUrl()
            val url = "$baseUrl/api/v1/app/version-guard"

            try {
                val httpResponse = httpClient.get(url) {
                    parameter("platform", platform)
                    parameter("nonce", clientNonce)
                    parameter("currentVersionCode", currentCode)
                }

                if (!httpResponse.status.isSuccess()) {
                    _versionCheckResult.value = VersionCheckResult.NetworkError("Server mengembalikan kode status ${httpResponse.status.value}")
                    return@launch
                }

                val bodyText = httpResponse.bodyAsText()
                val encResponse = json.decodeFromString<EncryptedVersionGuardResponse>(bodyText)

                if (encResponse.payload.isBlank() || encResponse.signature.isBlank()) {
                    _versionCheckResult.value = VersionCheckResult.TamperingDetected("Format respons tidak valid atau payload kosong.")
                    return@launch
                }

                // 1. Verifikasi Digital Signature RSA-SHA256 (Mencegah Tampering & Spoofing)
                val ivBytes = Base64.decode(encResponse.iv.trim())
                val cipherBytes = Base64.decode(encResponse.payload.trim())
                val signedData = ivBytes + cipherBytes

                val isSignatureValid = PlatformCrypto.verifyRsaSignature(RSA_PUBLIC_KEY, signedData, encResponse.signature)
                if (!isSignatureValid) {
                    _versionCheckResult.value = VersionCheckResult.TamperingDetected("Tanda tangan digital server tidak valid! Terindikasi manipulasi data respons.")
                    return@launch
                }

                // 2. Dekripsi Payload menggunakan AES-256-GCM
                val decryptedJson = PlatformCrypto.decryptAesGcm(AES_KEY, encResponse.iv, encResponse.payload)
                if (decryptedJson.isBlank()) {
                    _versionCheckResult.value = VersionCheckResult.TamperingDetected("Gagal mendekripsi respons versi aplikasi.")
                    return@launch
                }

                val payload = json.decodeFromString<DecryptedAppVersionPayload>(decryptedJson)

                // 3. Validasi Nonce (Anti-Replay Attack)
                if (payload.nonce != clientNonce) {
                    _versionCheckResult.value = VersionCheckResult.TamperingDetected("Nonce respons tidak cocok dengan request! Terdeteksi serangan pemutaran ulang paket (replay attack).")
                    return@launch
                }

                // 4. Evaluasi Versi & Force Update
                val isMandatory = payload.forceUpdate || (currentCode < payload.minVersionCode)
                if (isMandatory) {
                    _versionCheckResult.value = VersionCheckResult.ForceUpdateRequired(payload)
                } else if (currentCode < payload.latestVersionCode) {
                    _versionCheckResult.value = VersionCheckResult.UpdateOptional(payload)
                } else {
                    _versionCheckResult.value = VersionCheckResult.UpToDate
                }

            } catch (e: Exception) {
                _versionCheckResult.value = VersionCheckResult.NetworkError(e.message ?: "Gagal terhubung ke server untuk pengecekan versi.")
            }
        }
    }

    private fun generateRandomNonce(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..24)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}
