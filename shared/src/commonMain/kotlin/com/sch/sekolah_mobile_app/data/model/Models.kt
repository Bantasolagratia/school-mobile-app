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
            ?: "Kelas 10-C"
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

@Serializable
data class Jadwal(
    val idJadwal: String? = null,
    val judulKegiatan: String? = null,
    val mataPelajaran: String? = null,
    val kelas: String? = null,
    val guru: String? = null,
    val ruangan: String? = null,
    val waktu: String? = null,
    val notes: String? = null
)

@Serializable
data class TargetKelasItemMobile(
    val kelas: String,
    val guruPengampu: String? = null,
    val namaGuru: String? = null,
    val status: String? = null,
    val pengawas: String? = null,
    val namaPengawas: String? = null,
    val ruangan: String? = null,
    val emergencyExitKey: String? = null
)

@Serializable
data class ExamScheduleItem(
    val id: String? = null,
    val ujianId: String? = null,
    val judulUjian: String? = null,
    val kodeMapel: String? = null,
    val namaMapel: String? = null,
    val guruPengaju: String? = null,
    val namaGuruPengaju: String? = null,
    val tanggal: String? = null,
    val jamMulai: String? = null,
    val jamSelesai: String? = null,
    val waktuMulai: String? = null,
    val waktuSelesai: String? = null,
    val isMandiri: Boolean = true,
    val status: String? = null,
    val adminStatus: String? = null,
    val targetKelas: List<TargetKelasItemMobile> = emptyList()
) {
    val displaySupervisor: String
        get() {
            val pengawas = targetKelas.firstOrNull()?.namaPengawas?.takeIf { it.isNotBlank() }
            return pengawas ?: namaGuruPengaju ?: "Guru Pengawas"
        }

    val displayRuangan: String
        get() {
            return targetKelas.firstOrNull()?.ruangan?.takeIf { it.isNotBlank() } ?: "Ruang Ujian"
        }
}

@Serializable
data class UjianQuestionMobile(
    val id: String? = null,
    val type: String? = null,
    val urutan: Int? = 0,
    val pertanyaan: String? = null,
    val pilihan: List<String> = emptyList(),
    val bobot: Int? = 1
)

@Serializable
data class UjianDetailMobile(
    val id: String? = null,
    val judul: String? = null,
    val kodeMapel: String? = null,
    val namaMapel: String? = null,
    val authorName: String? = null,
    val questions: List<UjianQuestionMobile> = emptyList()
)

@Serializable
data class EmergencyExitVerifyRequest(
    val scheduleId: String,
    val kelas: String? = null,
    val key: String
)

@Serializable
data class EmergencyExitVerifyResponse(
    val valid: Boolean = false,
    val message: String? = null
)

@Serializable
data class SessionHeartbeatRequest(
    val scheduleId: String? = null,
    val status: String? = null,
    val keterangan: String? = null
)

@Serializable
data class SessionHeartbeatResponse(
    val active: Boolean = true,
    val action: String? = null,
    val message: String? = null,
    val serverTime: String? = null
)


