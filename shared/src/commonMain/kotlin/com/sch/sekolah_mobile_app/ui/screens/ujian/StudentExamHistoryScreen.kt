package com.sch.sekolah_mobile_app.ui.screens.ujian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.ExamResultDetailResponseMobile
import com.sch.sekolah_mobile_app.data.model.StudentExamHistoryItemMobile
import com.sch.sekolah_mobile_app.data.model.StudentMataPelajaranItem
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.MataPelajaranRepository
import com.sch.sekolah_mobile_app.data.repository.UjianRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentExamHistoryScreen(
    ujianRepository: UjianRepository,
    mapelRepository: MataPelajaranRepository,
    profile: UserProfileResponse?,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val studentKelas = profile?.displayDetail ?: "Kelas 10-B"

    // State 1: Subject Selection
    var subjectList by remember { mutableStateOf<List<StudentMataPelajaranItem>>(emptyList()) }
    var selectedMapel by remember { mutableStateOf<StudentMataPelajaranItem?>(null) }
    var isLoadingSubjects by remember { mutableStateOf(true) }
    var subjectSearchQuery by remember { mutableStateOf("") }

    // State 2: Exam History in Selected Subject
    var examHistoryList by remember { mutableStateOf<List<StudentExamHistoryItemMobile>>(emptyList()) }
    var isLoadingExams by remember { mutableStateOf(false) }
    var examSearchQuery by remember { mutableStateOf("") }
    var examErrorMessage by remember { mutableStateOf<String?>(null) }

    // State 3: Exam Review Modal
    var reviewResultDetail by remember { mutableStateOf<ExamResultDetailResponseMobile?>(null) }
    var isLoadingReview by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    // Load subjects on start
    fun loadSubjects() {
        isLoadingSubjects = true
        coroutineScope.launch {
            try {
                subjectList = mapelRepository.getStudentSubjects(studentKelas)
            } catch (_: Exception) {}
            isLoadingSubjects = false
        }
    }

    LaunchedEffect(studentKelas) {
        loadSubjects()
    }

    // Load exams when a subject is selected
    fun loadExamHistory(kodeMapel: String) {
        isLoadingExams = true
        examErrorMessage = null
        coroutineScope.launch {
            try {
                val resp = ujianRepository.getStudentExamHistory(kodeMapel)
                examHistoryList = resp.exams
            } catch (e: Exception) {
                examErrorMessage = e.message ?: "Gagal memuat riwayat ujian."
            } finally {
                isLoadingExams = false
            }
        }
    }

    // Load review detail
    fun loadReviewDetail(resultId: String) {
        isLoadingReview = true
        showReviewDialog = true
        coroutineScope.launch {
            try {
                val detail = ujianRepository.getExamResultDetail(resultId)
                reviewResultDetail = detail
            } catch (e: Exception) {
                reviewResultDetail = null
            } finally {
                isLoadingReview = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedMapel == null) "Riwayat Ujian & Nilai" else selectedMapel?.nama ?: "Riwayat Ujian",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedMapel == null) "Pilih Mata Pelajaran" else "Kode: ${selectedMapel?.kode ?: "-"} ($studentKelas)",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedMapel != null) {
                            selectedMapel = null
                            examHistoryList = emptyList()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = DarkNavy
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedMapel != null) {
                            selectedMapel?.kode?.let { loadExamHistory(it) }
                        } else {
                            loadSubjects()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = PrimaryTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        containerColor = LightBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedMapel == null) {
                // VIEW 1: PILIH MATA PELAJARAN
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Search Mapel
                    OutlinedTextField(
                        value = subjectSearchQuery,
                        onValueChange = { subjectSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari mata pelajaran atau guru...", fontSize = 13.sp, color = SlateGray) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = SlateGray)
                        },
                        trailingIcon = {
                            if (subjectSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { subjectSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus", tint = SlateGray)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoadingSubjects) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryTeal)
                        }
                    } else {
                        val filteredMapel = remember(subjectList, subjectSearchQuery) {
                            if (subjectSearchQuery.isBlank()) subjectList
                            else {
                                val q = subjectSearchQuery.trim().lowercase()
                                subjectList.filter {
                                    (it.nama?.lowercase()?.contains(q) == true) ||
                                    (it.kode?.lowercase()?.contains(q) == true) ||
                                    (it.guruPengajar?.lowercase()?.contains(q) == true)
                                }
                            }
                        }

                        if (filteredMapel.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = SlateGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Tidak Ada Mata Pelajaran",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkNavy
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(filteredMapel) { mapel ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedMapel = mapel
                                                mapel.kode?.let { loadExamHistory(it) }
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryTealContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = PrimaryTeal,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = Color(0xFFF1F5F9),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = mapel.kode ?: "MAPEL",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = DarkNavy,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = mapel.nama ?: "Mata Pelajaran",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkNavy
                                                )
                                                Text(
                                                    text = "Guru: ${mapel.guruPengajar ?: "-"}",
                                                    fontSize = 12.sp,
                                                    color = SlateGray
                                                )
                                            }

                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = "Buka",
                                                tint = SlateGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // VIEW 2: DAFTAR UJIAN DALAM MAPEL TERPILIH
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header Card Mapel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryTealContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedMapel?.nama ?: "",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Text(
                                    text = "Daftar seluruh ujian yang ditugaskan di kelas $studentKelas",
                                    fontSize = 11.sp,
                                    color = SlateGray
                                )
                            }
                            TextButton(
                                onClick = {
                                    selectedMapel = null
                                    examHistoryList = emptyList()
                                }
                            ) {
                                Text("Ganti Mapel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Judul Ujian
                    OutlinedTextField(
                        value = examSearchQuery,
                        onValueChange = { examSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari judul ujian...", fontSize = 13.sp, color = SlateGray) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = SlateGray)
                        },
                        trailingIcon = {
                            if (examSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { examSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus", tint = SlateGray)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoadingExams) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryTeal)
                        }
                    } else if (examErrorMessage != null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(examErrorMessage ?: "", color = ErrorRed, fontSize = 13.sp)
                        }
                    } else {
                        val filteredExams = remember(examHistoryList, examSearchQuery) {
                            if (examSearchQuery.isBlank()) examHistoryList
                            else {
                                val q = examSearchQuery.trim().lowercase()
                                examHistoryList.filter {
                                    (it.judulUjian?.lowercase()?.contains(q) == true) ||
                                    (it.kategoriKode?.lowercase()?.contains(q) == true)
                                }
                            }
                        }

                        if (filteredExams.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AssignmentLate,
                                        contentDescription = null,
                                        tint = SlateGray,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Belum Ada Riwayat Ujian",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkNavy
                                    )
                                    Text(
                                        text = "Ujian yang telah dijadwalkan akan muncul di sini.",
                                        fontSize = 12.sp,
                                        color = SlateGray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(filteredExams) { ex ->
                                    ExamHistoryCard(
                                        item = ex,
                                        onReviewClicked = {
                                            if (ex.resultId != null) {
                                                loadReviewDetail(ex.resultId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MODAL DIALOG: REVIEW PEMBAHASAN SOAL & JAWABAN
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = {
                showReviewDialog = false
                reviewResultDetail = null
            },
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Review Pembahasan",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        IconButton(onClick = {
                            showReviewDialog = false
                            reviewResultDetail = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }
                    reviewResultDetail?.let { r ->
                        Text(
                            text = "${r.judulUjian ?: "Ujian"} (${r.namaMapel ?: ""})",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }
                }
            },
            text = {
                if (isLoadingReview) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryTeal)
                    }
                } else if (reviewResultDetail == null) {
                    Text("Gagal memuat rincian review.", color = ErrorRed, fontSize = 13.sp)
                } else {
                    val detail = reviewResultDetail!!
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Summary Score Card
                        item {
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Nilai Akhir", fontSize = 11.sp, color = SlateGray)
                                        Text(
                                            text = if (detail.hasUngradedEssay) "⏳ Menunggu" else "${detail.score}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (detail.hasUngradedEssay) Color(0xFFD97706) else if (detail.score >= 75) Color(0xFF059669) else Color(0xFFDC2626)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Benar", fontSize = 11.sp, color = SlateGray)
                                        Text(
                                            text = "${detail.totalBenar}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Salah", fontSize = 11.sp, color = SlateGray)
                                        Text(
                                            text = "${detail.totalSalah}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        }

                        // Questions Review List
                        items(detail.feedbackList) { q ->
                            Surface(
                                color = CardSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Question Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Soal #${q.urutan} • ${q.type ?: "SOAL"}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkNavy,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        if (q.type == "URAIAN") {
                                            Surface(
                                                color = if (q.teacherScore != null) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (q.teacherScore != null) "Skor: ${q.teacherScore} / 10" else "Menunggu Nilai Guru",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (q.teacherScore != null) Color(0xFF059669) else Color(0xFFD97706),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                color = if (q.isCorrect) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (q.isCorrect) "✅ Benar" else "❌ Salah",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (q.isCorrect) Color(0xFF059669) else Color(0xFFDC2626),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Pertanyaan
                                    Text(
                                        text = q.pertanyaan ?: "",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkNavy
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Jawaban Murid
                                    Surface(
                                        color = if (q.isCorrect) Color(0xFFF0FDF4) else Color(0xFFFFF1F2),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (q.isCorrect) Color(0xFFBBF7D0) else Color(0xFFFECDD3)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Jawaban Anda:",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (q.isCorrect) Color(0xFF166534) else Color(0xFF991B1B)
                                            )
                                            Text(
                                                text = q.studentAnswer?.takeIf { it.isNotBlank() } ?: "(Tidak dijawab)",
                                                fontSize = 12.sp,
                                                color = DarkNavy
                                            )
                                        }
                                    }

                                    // Kunci Jawaban Benar (jika diizinkan guru)
                                    if (detail.reviewAllowed && !q.correctAnswer.isNullOrBlank() && q.type != "URAIAN") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "Kunci Jawaban Benar:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryTeal
                                                )
                                                Text(
                                                    text = q.correctAnswer ?: "",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = DarkNavy
                                                )
                                            }
                                        }
                                    }

                                    // Catatan Feedback Guru (jika ada)
                                    if (!q.teacherNotes.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFFFFFBEB),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "Catatan Koreksi Guru:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                                Text(
                                                    text = q.teacherNotes ?: "",
                                                    fontSize = 12.sp,
                                                    color = DarkNavy
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReviewDialog = false
                        reviewResultDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup", fontSize = 13.sp)
                }
            }
        )
    }
}

@Composable
private fun ExamHistoryCard(
    item: StudentExamHistoryItemMobile,
    onReviewClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Kategori & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PrimaryTealContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.kategoriKode ?: "UJIAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimaryTealContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Status Badge
                when {
                    item.status == "DIANULIR" -> {
                        Surface(
                            color = Color(0xFFFFF1F2),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔄 Dianulir",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    item.hasUngradedEssay || item.status == "WAITING_ESSAY_GRADING" -> {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⏳ Menunggu Koreksi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    item.isAttended && item.score != null -> {
                        Surface(
                            color = if (item.score >= 75) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Nilai: ${item.score}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (item.score >= 75) Color(0xFF059669) else Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    else -> {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Belum Mengerjakan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateGray,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Judul Ujian
            Text(
                text = item.judulUjian ?: "Ujian Sekolah",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Info Waktu
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = SlateGray,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${item.tanggal ?: "-"} • ${item.jamMulai ?: "-"} - ${item.jamSelesai ?: "-"} WIB",
                    fontSize = 11.sp,
                    color = SlateGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Footer / Review Action
            if (item.isAttended && item.resultId != null) {
                if (item.isReviewAllowed) {
                    OutlinedButton(
                        onClick = onReviewClicked,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryTeal
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTeal)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Review Pembahasan Soal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = SlateGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Review pembahasan belum diizinkan oleh guru.",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                }
            } else {
                Text(
                    text = "Siswa belum / tidak mengumpulkan ujian ini.",
                    fontSize = 11.sp,
                    color = SlateGray
                )
            }
        }
    }
}

