package com.sch.sekolah_mobile_app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class UserDto(
    val id: String? = null,
    val email: String? = null
)

@Serializable
data class SessionResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: UserDto? = null
)

@Serializable
data class UserIdentity(
    val id: String,
    val name: String? = null,
    val detail: String? = null,
    val role: String? = null,
    val isStudent: Boolean = false,
    val isTeacher: Boolean = false,
    val isAdmin: Boolean = false,
    val isGuardian: Boolean = false
)

@Serializable
data class UserProfileResponse(
    val idUser: String? = null,
    val email: String? = null,
    val isAdmin: Boolean = false,
    val isTeacher: Boolean = false,
    val isStudent: Boolean = false,
    val isGuardian: Boolean = false,
    val roles: List<String> = emptyList(),
    val identities: List<UserIdentity> = emptyList()
) {
    val isRoleMurid: Boolean
        get() = isStudent || roles.contains("MURID")

    val studentIdentity: UserIdentity?
        get() = identities.firstOrNull { it.isStudent || it.role == "MURID" } ?: identities.firstOrNull()

    val displayName: String
        get() = studentIdentity?.name?.trim()?.takeIf { it.isNotEmpty() }
            ?: email?.substringBefore('@')?.takeIf { it.isNotEmpty() }
            ?: "Siswa"

    val displayDetail: String
        get() = studentIdentity?.detail?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Kelas 10-B"

    val nis: String
        get() = studentIdentity?.id ?: "-"
}

@Serializable
data class Guru(
    val nip: String,
    val nama: String,
    val jabatan: String? = null,
    val telp: String? = null,
    val wa: String? = null
) {
    val initials: String
        get() {
            val parts = nama.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> "?"
                parts.size >= 2 -> "${parts[0][0]}${parts[1][0]}".uppercase()
                else -> "${parts[0][0]}".uppercase()
            }
        }

    val displayJabatan: String
        get() = jabatan?.trim()?.takeIf { it.isNotEmpty() } ?: "Guru Pengajar"
}

