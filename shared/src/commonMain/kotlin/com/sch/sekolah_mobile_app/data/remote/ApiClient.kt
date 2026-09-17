package com.sch.sekolah_mobile_app.data.remote

import com.sch.sekolah_mobile_app.data.model.Guru
import com.sch.sekolah_mobile_app.data.model.LoginRequest
import com.sch.sekolah_mobile_app.data.model.SessionResponse
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.model.ExamScheduleItem
import com.sch.sekolah_mobile_app.data.model.EmergencyExitVerifyRequest
import com.sch.sekolah_mobile_app.data.model.EmergencyExitVerifyResponse
import com.sch.sekolah_mobile_app.data.model.SessionHeartbeatRequest
import com.sch.sekolah_mobile_app.data.model.SessionHeartbeatResponse
import com.sch.sekolah_mobile_app.data.model.StudentMataPelajaranItem
import com.sch.sekolah_mobile_app.data.model.MateriItem
import com.sch.sekolah_mobile_app.data.model.UjianDetailMobile
import com.sch.sekolah_mobile_app.data.model.StudentRaportResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiClient {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }

    suspend fun login(email: String, password: String): SessionResponse {
        val url = ApiConfig.getTokenUrl()
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = email.trim(), password = password))
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Email atau password tidak valid."
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["error_description"]?.jsonPrimitive?.content
                    ?: jsonTree["msg"]?.jsonPrimitive?.content
                    ?: jsonTree["message"]?.jsonPrimitive?.content
                    ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun fetchProfile(token: String): UserProfileResponse {
        val url = ApiConfig.getProfileUrl()
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Gagal mengambil profil (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun getDaftarGuru(token: String): List<Guru> {
        val url = ApiConfig.getGuruUrl()
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Gagal mengambil data guru (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun getUpcomingExams(token: String, kelas: String? = null): List<ExamScheduleItem> {
        val url = ApiConfig.getStudentUpcomingExamsUrl(kelas)
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Gagal mengambil daftar ujian (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun sendSessionHeartbeat(
        token: String,
        scheduleId: String? = null,
        status: String? = null,
        keterangan: String? = null
    ): SessionHeartbeatResponse {
        val url = ApiConfig.getSessionHeartbeatUrl()
        val response = httpClient.post(url) {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SessionHeartbeatRequest(scheduleId = scheduleId, status = status, keterangan = keterangan))
        }

        val responseText = response.bodyAsText()
        if (response.status.value == 409) {
            var msg = "Akun ini sedang aktif dalam sesi ujian di perangkat lain. Silakan hubungi Guru Pengawas untuk melakukan Reset Sesi / Kick jika Anda ingin berganti perangkat."
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                msg = jsonTree["message"]?.jsonPrimitive?.content ?: msg
            } catch (_: Exception) {}
            return SessionHeartbeatResponse(active = false, action = "BLOCKED", message = msg)
        }

        if (response.status.value == 401) {
            var msg = "Sesi Anda telah di-reset oleh Pengawas atau dialihkan."
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                msg = jsonTree["message"]?.jsonPrimitive?.content ?: msg
            } catch (_: Exception) {}
            return SessionHeartbeatResponse(active = false, action = "KICK", message = msg)
        }

        if (!response.status.isSuccess()) {
            var message = "Gagal mengirim heartbeat (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun examStart(
        token: String,
        scheduleId: String? = null,
        keterangan: String? = null
    ): SessionHeartbeatResponse {
        val url = ApiConfig.getExamStartUrl()
        val response = httpClient.post(url) {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SessionHeartbeatRequest(scheduleId = scheduleId, status = "MENGERJAKAN", keterangan = keterangan))
        }

        val responseText = response.bodyAsText()
        if (response.status.value == 409) {
            var msg = "Akun ini sedang aktif dalam sesi ujian di perangkat lain. Silakan hubungi Guru Pengawas untuk melakukan Reset Sesi / Kick jika Anda ingin berganti perangkat."
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                msg = jsonTree["message"]?.jsonPrimitive?.content ?: msg
            } catch (_: Exception) {}
            return SessionHeartbeatResponse(active = false, action = "BLOCKED", message = msg)
        }

        if (response.status.value == 401) {
            var msg = "Sesi Anda telah di-reset oleh Pengawas atau dialihkan."
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                msg = jsonTree["message"]?.jsonPrimitive?.content ?: msg
            } catch (_: Exception) {}
            return SessionHeartbeatResponse(active = false, action = "KICK", message = msg)
        }

        if (!response.status.isSuccess()) {
            var message = "Gagal memulai sesi ujian (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun examExit(
        token: String,
        scheduleId: String? = null,
        status: String? = "SELESAI",
        keterangan: String? = null
    ): Boolean {
        val url = ApiConfig.getExamExitUrl()
        val response = httpClient.post(url) {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SessionHeartbeatRequest(scheduleId = scheduleId, status = status, keterangan = keterangan))
        }
        return response.status.isSuccess()
    }

    suspend fun verifyEmergencyExitKey(
        token: String,
        scheduleId: String,
        kelas: String? = null,
        key: String
    ): EmergencyExitVerifyResponse {
        val url = ApiConfig.getVerifyExitKeyUrl()
        val response = httpClient.post(url) {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(EmergencyExitVerifyRequest(scheduleId = scheduleId, kelas = kelas, key = key.trim()))
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Gagal memverifikasi kunci keluar (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun getUjianDetail(token: String, ujianId: String): UjianDetailMobile {
        val url = ApiConfig.getUjianDetailUrl(ujianId)
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Gagal mengambil lembar ujian (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun getStudentSubjects(token: String, kelas: String? = null): List<StudentMataPelajaranItem> {
        val url = ApiConfig.getStudentMataPelajaranUrl(kelas)
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Gagal mengambil daftar mata pelajaran (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun getMateriList(token: String, kodeMapel: String): List<MateriItem> {
        val url = ApiConfig.getMateriListUrl(kodeMapel)
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Gagal mengambil daftar materi (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun getMateriDetail(token: String, materiId: String): MateriItem {
        val url = ApiConfig.getMateriDetailUrl(materiId)
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Gagal mengambil isi materi (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }

    suspend fun fetchImageBytes(pathOrUrl: String, token: String? = null): ByteArray {
        val baseUrl = ApiConfig.getBaseUrl()
        val fullUrl = when {
            pathOrUrl.startsWith("http://localhost:") || pathOrUrl.startsWith("http://127.0.0.1:") || pathOrUrl.startsWith("http://10.0.2.2:") -> {
                val afterHost = pathOrUrl.substringAfter("://", "")
                val path = afterHost.substringAfter("/", "")
                val clean = if (path.startsWith("api/")) "/$path" else "/api/$path"
                "$baseUrl$clean"
            }
            pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://") -> pathOrUrl
            else -> {
                val clean = if (pathOrUrl.startsWith("/")) pathOrUrl else "/$pathOrUrl"
                val apiPath = if (clean.startsWith("/api/")) clean else "/api$clean"
                "$baseUrl$apiPath"
            }
        }

        val response = httpClient.get(fullUrl) {
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }

        if (!response.status.isSuccess()) {
            throw Exception("Gagal memuat gambar (${response.status.value}): $fullUrl")
        }

        val bytes = response.readBytes()
        if (bytes.size >= 15) {
            val prefix = bytes.take(15).toByteArray().decodeToString()
            if (prefix.contains("<!doctype", ignoreCase = true) || prefix.contains("<html", ignoreCase = true)) {
                throw Exception("Response dari $fullUrl bukan file gambar melainkan halaman HTML fallback")
            }
        }
        return bytes
    }

    suspend fun getMyRaport(token: String): StudentRaportResponse {
        val url = ApiConfig.getMyRaportUrl()
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            var message = "Gagal memuat data rapor (${response.status.value})"
            try {
                val jsonTree = json.parseToJsonElement(responseText).jsonObject
                message = jsonTree["message"]?.jsonPrimitive?.content ?: message
            } catch (_: Exception) {}
            throw Exception(message)
        }

        return response.body()
    }
}

