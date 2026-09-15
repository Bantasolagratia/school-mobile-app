package com.sch.sekolah_mobile_app.data.remote

import com.sch.sekolah_mobile_app.data.model.Guru
import com.sch.sekolah_mobile_app.data.model.LoginRequest
import com.sch.sekolah_mobile_app.data.model.SessionResponse
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
}

