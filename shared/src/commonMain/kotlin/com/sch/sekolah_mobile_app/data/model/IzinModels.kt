package com.sch.sekolah_mobile_app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SuratIzinItemMobile(
    val id: String,
    val muridNis: Long? = null,
    val muridNama: String? = null,
    val kelas: String? = null,
    val kategori: String, // SAKIT, KELUAR_SEKOLAH, LAINNYA
    val tanggalMulai: String,
    val tanggalSelesai: String,
    val keterangan: String,
    val guruPenanggungJawabNip: String,
    val guruPenanggungJawabNama: String? = null,
    val originalFilename: String? = null,
    val canonicalFilename: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val attachmentUrl: String? = null,
    val status: String, // PENDING, APPROVED, REJECTED
    val catatanGuru: String? = null,
    val respondedBy: String? = null,
    val respondedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class SelectedFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
    val sizeBytes: Long
)
