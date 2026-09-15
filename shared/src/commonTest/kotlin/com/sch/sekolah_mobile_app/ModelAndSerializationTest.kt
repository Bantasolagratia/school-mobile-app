package com.sch.sekolah_mobile_app

import com.sch.sekolah_mobile_app.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class ModelAndSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testLoginRequestSerialization() {
        val req = LoginRequest(email = "test@sekolah.com", password = "SecretPassword123!")
        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("test@sekolah.com"))
        assertTrue(serialized.contains("SecretPassword123!"))

        val deserialized = json.decodeFromString<LoginRequest>(serialized)
        assertEquals(req.email, deserialized.email)
        assertEquals(req.password, deserialized.password)
    }

    @Test
    fun testSessionResponseDeserialization() {
        val rawJson = """
            {
                "access_token": "mock-jwt-token-12345",
                "token_type": "bearer",
                "expires_in": 3600,
                "refresh_token": "mock-refresh-token",
                "user": {
                    "id": "usr-123",
                    "email": "murid@sekolah.com"
                }
            }
        """.trimIndent()

        val session = json.decodeFromString<SessionResponse>(rawJson)
        assertEquals("mock-jwt-token-12345", session.accessToken)
        assertEquals("murid@sekolah.com", session.user?.email)
    }

    @Test
    fun testUserProfileResponseRoleMurid() {
        val profile = UserProfileResponse(
            idUser = "usr-001",
            email = "wrenley@murid.sekolah.com",
            isStudent = true,
            roles = listOf("MURID"),
            identities = listOf(
                UserIdentity(
                    id = "202610012",
                    name = "Wrenley Roth",
                    detail = "Kelas 10-C",
                    role = "MURID",
                    isStudent = true
                )
            )
        )

        assertTrue(profile.isRoleMurid)
        assertEquals("Wrenley Roth", profile.displayName)
        assertEquals("Kelas 10-C", profile.displayDetail)
        assertEquals("202610012", profile.nis)
    }

    @Test
    fun testGuruInitialsAndDisplay() {
        val guru1 = Guru(nip = "19850101", nama = "Budi Santoso", jabatan = "Guru Matematika")
        assertEquals("BS", guru1.initials)
        assertEquals("Guru Matematika", guru1.displayJabatan)

        val guru2 = Guru(nip = "19850102", nama = "Siti", jabatan = null)
        assertEquals("S", guru2.initials)
        assertEquals("Guru Pengajar", guru2.displayJabatan)
    }
}

