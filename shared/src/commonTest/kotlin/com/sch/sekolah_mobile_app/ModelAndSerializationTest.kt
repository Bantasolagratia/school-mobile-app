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

    @Test
    fun testRegistrationModelsSerialization() {
        val verifyReq = VerifyIdentityRequest(role = "MURID", identifier = "202610001")
        val verifyJson = json.encodeToString(verifyReq)
        assertTrue(verifyJson.contains("MURID"))
        assertTrue(verifyJson.contains("202610001"))

        val identResp = IdentityResponse(
            found = true,
            role = "MURID",
            identifier = "202610001",
            name = "Budi Santoso",
            detail = "Kelas X-A"
        )
        val identJson = json.encodeToString(identResp)
        val decoded = json.decodeFromString<IdentityResponse>(identJson)
        assertTrue(decoded.found)
        assertEquals("Budi Santoso", decoded.name)

        val regReq = RegistrationRequest(
            role = "MURID",
            identifier = "202610001",
            email = "budi@murid.sekolah.com",
            password = "Password123!",
            telp = "08123456789",
            wa = "08123456789"
        )
        val regJson = json.encodeToString(regReq)
        assertTrue(regJson.contains("budi@murid.sekolah.com"))

        val guruVerifyReq = VerifyIdentityRequest(role = "GURU", identifier = "19850101-201001-1-001")
        val guruVerifyJson = json.encodeToString(guruVerifyReq)
        assertTrue(guruVerifyJson.contains("GURU"))
        assertTrue(guruVerifyJson.contains("19850101-201001-1-001"))

        val guruRegReq = RegistrationRequest(
            role = "GURU",
            identifier = "19850101-201001-1-001",
            email = "ahmad@guru.sekolah.com",
            password = "Password123!",
            telp = "08129876543",
            wa = "08129876543"
        )
        val guruRegJson = json.encodeToString(guruRegReq)
        assertTrue(guruRegJson.contains("ahmad@guru.sekolah.com"))
        assertTrue(guruRegJson.contains("GURU"))
    }
}

