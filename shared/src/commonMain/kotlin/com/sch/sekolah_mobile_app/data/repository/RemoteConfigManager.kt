package com.sch.sekolah_mobile_app.data.repository

import com.sch.sekolah_mobile_app.data.model.FeatureFlagUpdateEvent
import com.sch.sekolah_mobile_app.data.model.RemoteConfigResponse
import com.sch.sekolah_mobile_app.data.remote.ApiConfig
import com.sch.sekolah_mobile_app.data.storage.PlatformStorage
import com.sch.sekolah_mobile_app.data.storage.getPlatformStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.min
import kotlin.random.Random

class RemoteConfigManager private constructor() {

    private val storage: PlatformStorage = getPlatformStorage()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    // Key Constants 2-Level
    private val STORAGE_KEY_CONFIG = "remote_config_json"
    private val STORAGE_KEY_ETAG = "remote_config_etag"
    val FLAG_RAPORT_MODULE = "VISUAL_RAPORT_MODULE"
    val FLAG_FINAL_GRADE = "VISUAL_RAPORT_FINAL_GRADE"

    // Level 1: Modul Utama Raport
    private val _isRaportModuleEnabled = MutableStateFlow(false)
    val isRaportModuleEnabled: StateFlow<Boolean> = _isRaportModuleEnabled.asStateFlow()

    // Level 2: Visibilitas Nilai Total Mapel
    private val _isFinalGradeEnabled = MutableStateFlow(false)
    val isFinalGradeEnabled: StateFlow<Boolean> = _isFinalGradeEnabled.asStateFlow()

    private var sseJob: Job? = null

    companion object {
        val instance: RemoteConfigManager by lazy { RemoteConfigManager() }
    }

    init {
        // 1. Baca cache lokal saat inisialisasi (Nir-Latensi UI Boot)
        loadFromLocalStorage()
    }

    private fun loadFromLocalStorage() {
        try {
            val cachedJson = storage.getString(STORAGE_KEY_CONFIG, null)
            if (cachedJson != null) {
                val parsed = json.decodeFromString<Map<String, Boolean>>(cachedJson)
                _isRaportModuleEnabled.value = parsed[FLAG_RAPORT_MODULE] ?: false
                _isFinalGradeEnabled.value = parsed[FLAG_FINAL_GRADE] ?: false
            } else {
                // Fail-Closed Default Mutlak (Keduanya False)
                _isRaportModuleEnabled.value = false
                _isFinalGradeEnabled.value = false
            }
        } catch (e: Exception) {
            _isRaportModuleEnabled.value = false
            _isFinalGradeEnabled.value = false
        }
    }

    /**
     * Dipanggil pada Cold Start dan App Resume dari Background (Conditional HTTP GET ETag)
     */
    suspend fun syncWithServer(userToken: String? = null) {
        val cachedEtag = storage.getString(STORAGE_KEY_ETAG, "")
        val baseUrl = ApiConfig.getBaseUrl()
        val url = "$baseUrl/api/remote-config/features"

        try {
            val response = httpClient.get(url) {
                if (!cachedEtag.isNullOrBlank()) {
                    header(HttpHeaders.IfNoneMatch, cachedEtag)
                }
                if (!userToken.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                }
                header("X-Client-Platform", "ANDROID")
                header("X-App-Version", "1.0.0")
            }

            // Tangani 304 Not Modified secara terpisah tanpa membaca body!
            if (response.status == HttpStatusCode.NotModified) {
                return
            }

            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val parsed = json.decodeFromString<RemoteConfigResponse>(bodyText)
                
                // Simpan payload & ETag baru
                val featuresMap = parsed.features
                _isRaportModuleEnabled.value = featuresMap[FLAG_RAPORT_MODULE] ?: false
                _isFinalGradeEnabled.value = featuresMap[FLAG_FINAL_GRADE] ?: false

                storage.setString(STORAGE_KEY_CONFIG, json.encodeToString(featuresMap))
                if (parsed.etag.isNotBlank()) {
                    storage.setString(STORAGE_KEY_ETAG, parsed.etag)
                }
            }
        } catch (e: Exception) {
            // Jaringan mati / timeout: Tetap gunakan nilai lokal cache
        }
    }

    /**
     * Membuka koneksi SSE saat aplikasi berada di latar depan (Foreground)
     */
    fun startRealtimeStream(userToken: String? = null) {
        stopRealtimeStream()
        sseJob = scope.launch {
            var attempt = 0
            while (isActive) {
                try {
                    val baseUrl = ApiConfig.getBaseUrl()
                    val url = "$baseUrl/api/remote-config/stream/sse"

                    httpClient.prepareGet(url) {
                        header(HttpHeaders.Accept, "text/event-stream")
                        if (!userToken.isNullOrBlank()) {
                            header(HttpHeaders.Authorization, "Bearer $userToken")
                        }
                    }.execute { httpResponse ->
                        val channel: ByteReadChannel = httpResponse.body()
                        attempt = 0

                        var currentEvent = ""
                        while (!channel.isClosedForRead && isActive) {
                            val line = channel.readUTF8Line() ?: break
                            if (line.startsWith("event:")) {
                                currentEvent = line.substringAfter("event:").trim()
                            } else if (line.startsWith("data:")) {
                                val dataJson = line.substringAfter("data:").trim()
                                handleSseMessage(currentEvent, dataJson)
                            }
                        }
                    }
                } catch (e: Exception) {
                    attempt++
                    val baseDelay = min(60_000L, 1000L * (1 shl min(attempt, 6)))
                    val jitter = Random.nextLong(0, 1000L)
                    delay(baseDelay + jitter)
                }
            }
        }
    }

    fun stopRealtimeStream() {
        sseJob?.cancel()
        sseJob = null
    }

    private fun handleSseMessage(event: String, dataJson: String) {
        if (event == "FEATURE_FLAG_UPDATE") {
            try {
                val update = json.decodeFromString<FeatureFlagUpdateEvent>(dataJson)
                when (update.flagKey) {
                    FLAG_RAPORT_MODULE -> {
                        _isRaportModuleEnabled.value = update.isEnabled
                    }
                    FLAG_FINAL_GRADE -> {
                        _isFinalGradeEnabled.value = update.isEnabled
                    }
                }
            } catch (ignored: Exception) {}
        }
    }
}

