package com.sch.sekolah_mobile_app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
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

    val isRoleGuru: Boolean
        get() = isTeacher || roles.contains("GURU") || identities.any { it.isTeacher || it.role == "GURU" }

    val studentIdentity: UserIdentity?
        get() = identities.firstOrNull { it.isStudent || it.role == "MURID" } ?: identities.firstOrNull()

    val teacherIdentity: UserIdentity?
        get() = identities.firstOrNull { it.isTeacher || it.role == "GURU" } ?: identities.firstOrNull()

    val displayName: String
        get() = (if (isRoleGuru) teacherIdentity?.name else studentIdentity?.name)?.trim()?.takeIf { it.isNotEmpty() }
            ?: email?.substringBefore('@')?.takeIf { it.isNotEmpty() }
            ?: if (isRoleGuru) "Guru" else "Siswa"

    val displayDetail: String
        get() = (if (isRoleGuru) teacherIdentity?.detail else studentIdentity?.detail)?.trim()?.takeIf { it.isNotEmpty() }
            ?: if (isRoleGuru) "Dewan Guru" else "Kelas 10-B"

    val nis: String
        get() = studentIdentity?.id ?: "-"

    val nip: String
        get() = teacherIdentity?.id ?: "-"
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
    val id: String? = null,
    val idJadwal: String? = null,
    val judulKegiatan: String? = null,
    val kategori: String? = "MURID",
    val mataPelajaran: String? = null,
    val kelas: String? = null,
    val guru: String? = null,
    val pengawas: String? = null,
    val ruangan: String? = null,
    val waktu: String? = null,
    val waktuMulai: String? = null,
    val waktuSelesai: String? = null,
    val toleransiKeterlambatanMenit: Int? = 15,
    val status: String? = "TERJADWAL",
    val notes: String? = null,
    val createdBy: String? = null
) {
    val effectiveId: String
        get() = id?.takeIf { it.isNotBlank() } ?: idJadwal ?: ""

    val displayTitle: String
        get() = judulKegiatan?.takeIf { it.isNotBlank() }
            ?: mataPelajaran?.takeIf { it.isNotBlank() }
            ?: "Jadwal Kegiatan"

    val isCategoryGuru: Boolean
        get() = kategori?.equals("GURU", ignoreCase = true) == true
}

@Serializable
data class QrPayloadMobile(
    val token: String,
    val idJadwal: String? = null,
    val judulKegiatan: String? = null,
    val generatedAt: Long = 0,
    val expiresAt: Long = 0,
    val refreshIntervalSeconds: Int = 20
)

@Serializable
data class ScanQrRequestMobile(
    val token: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val deviceId: String? = null
)

@Serializable
data class AbsensiResultMobile(
    val id: String? = null,
    val nomorInduk: String? = null,
    val namaPeserta: String? = null,
    val idKegiatan: String? = null,
    val status: String? = null,
    val waktuAbsen: String? = null,
    val tipePeserta: String? = null,
    val diubahOleh: String? = null,
    val waktuKoreksi: String? = null,
    val alasanKoreksi: String? = null
)

@Serializable
data class NotificationItemMobile(
    val id: String,
    val recipientType: String? = null,
    val recipientId: String? = null,
    val title: String,
    val message: String,
    val category: String? = null,
    val referenceId: String? = null,
    val isRead: Boolean = false,
    val readAt: String? = null,
    val createdAt: String? = null
)

@Serializable
data class EmergencyPollResponse(
    val unlocked: Boolean = false
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
    val targetKelas: List<TargetKelasItemMobile> = emptyList(),
    val studentStatus: String? = null
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
data class ExamImageItemMobile(
    val id: String? = null,
    val url: String? = null,
    val filename: String? = null,
    val size: Long? = 0
)

@Serializable
data class UjianQuestionMobile(
    val id: String? = null,
    val type: String? = null,
    val urutan: Int? = 0,
    val pertanyaan: String? = null,
    val pilihan: List<String> = emptyList(),
    val bobot: Double? = 1.0,
    val maxPoints: Double? = null,
    val catatan: String? = null,
    val imageUrl: String? = null,
    val imageId: String? = null,
    val imageCaption: String? = null
)

@Serializable
data class UjianDetailMobile(
    val id: String? = null,
    val judul: String? = null,
    val kodeMapel: String? = null,
    val namaMapel: String? = null,
    val authorName: String? = null,
    val questions: List<UjianQuestionMobile> = emptyList(),
    val images: List<ExamImageItemMobile> = emptyList()
)

@Serializable
data class SubmitExamRequestMobile(
    val scheduleId: String? = null,
    val durationSeconds: Long = 0,
    val answers: Map<String, String> = emptyMap()
)

@Serializable
data class SubmitExamResponseMobile(
    val resultId: String? = null,
    val scheduleId: String? = null,
    val score: Double = 0.0,
    val status: String? = null,
    val hasUngradedEssay: Boolean = false,
    val message: String? = null
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

@Serializable
data class StudentMataPelajaranItem(
    val id: String? = null,
    val kode: String? = null,
    val nama: String? = null,
    val kategori: String? = null,
    val deskripsi: String? = null,
    val guruPengajar: String? = null,
    val guruNip: String? = null,
    val totalMateri: Long = 0
)

@Serializable
data class EditorBlockProperties(
    val checked: Boolean? = null,
    val icon: String? = null,
    val language: String? = null,
    val caption: String? = null,
    val url: String? = null,
    val rows: List<List<String>>? = null
)

@Serializable
data class EditorBlockItem(
    val id: String? = null,
    val type: String? = null,
    val content: String? = null,
    val properties: EditorBlockProperties? = null
)

@Serializable
data class MateriItem(
    val id: String? = null,
    val judul: String? = null,
    val deskripsi: String? = null,
    val kodeMapel: String? = null,
    val namaMapel: String? = null,
    val coverImage: String? = null,
    val icon: String? = null,
    val status: String? = null,
    val author: String? = null,
    val authorName: String? = null,
    val blocks: List<EditorBlockItem> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)


