package com.sch.sekolah_mobile_app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryGradeItem(
    val kategoriKode: String = "",
    val kategoriNama: String = "",
    val bobot: Double = 0.0,
    val averageScore: Double = 0.0,
    val weightedScore: Double = 0.0,
    val totalExams: Int = 0,
    val totalRegistered: Int = 0
)

@Serializable
data class SubjectRaportItem(
    val kodeMapel: String = "",
    val namaMapel: String = "",
    val namaGuru: String = "",
    val kkm: Double = 75.0,
    val categoryBreakdown: List<CategoryGradeItem> = emptyList(),
    val finalScore: Double? = null,
    val predikat: String? = null,
    val lulus: Boolean? = true,
    val status: String = "PUBLISHED"
)

@Serializable
data class AttendanceRecap(
    val hadir: Int = 0,
    val sakit: Int = 0,
    val izin: Int = 0,
    val alpa: Int = 0,
    val totalHariKBM: Int = 0,
    val persentaseKehadiran: Double = 100.0
)

@Serializable
data class StudentRaportResponse(
    val nis: String = "",
    val namaMurid: String = "",
    val kelas: String = "",
    val semesterId: String = "",
    val semesterName: String = "",
    val academicYear: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "IN_PROGRESS",
    val isFinalGradePublished: Boolean = true,
    val gradeStatusMessage: String? = null,
    val attendance: AttendanceRecap = AttendanceRecap(),
    val subjects: List<SubjectRaportItem> = emptyList(),
    val gpa: Double? = null,
    val generatedAt: String? = null
)
