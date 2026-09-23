package com.sch.sekolah_mobile_app.ui.screens.ujian

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.ExamScheduleItem
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.UjianRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UjianMuridScreen(
    ujianRepository: UjianRepository,
    profile: UserProfileResponse?,
    onNavigateBack: () -> Unit,
    onStartExam: (ExamScheduleItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var upcomingExams by remember { mutableStateOf<List<ExamScheduleItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentServerTimeIso by remember { mutableStateOf<String?>(null) }

    val studentKelas = profile?.displayDetail ?: "Kelas 10-B"

    fun loadData() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                // Ambil heartbeat dan server time
                try {
                    val hb = ujianRepository.sendHeartbeat()
                    currentServerTimeIso = hb.serverTime
                } catch (_: Exception) {}

                val exams = ujianRepository.getUpcomingExams(studentKelas)
                upcomingExams = exams
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Gagal memuat jadwal ujian."
            }
        }
    }

    LaunchedEffect(studentKelas) {
        loadData()
    }

    // Refresh waktu server berkala
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            try {
                val hb = ujianRepository.sendHeartbeat()
                currentServerTimeIso = hb.serverTime
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Jadwal & Modul Ujian",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = "Peserta: $studentKelas (${profile?.displayName ?: "Murid"})",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = DarkNavy
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
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
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryTeal)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Menghubungkan ke Server Ujian...",
                                fontSize = 13.sp,
                                color = SlateGray
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Gagal Memuat Jadwal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 12.sp,
                                    color = SlateGray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { loadData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                }

                upcomingExams.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tidak Ada Ujian Mendatang",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Saat ini belum ada jadwal ujian aktif yang ditugaskan untuk kelas $studentKelas.",
                                fontSize = 13.sp,
                                color = SlateGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Surface(
                                color = PrimaryTealContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Ujian dilindungi Sistem Keamanan Ketat. Layar akan terkunci penuh saat pengerjaan dimulai.",
                                        fontSize = 11.sp,
                                        color = DarkNavy,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        items(upcomingExams) { exam ->
                            ExamCardItem(
                                exam = exam,
                                onStartExam = {
                                    if (exam.studentStatus != "SELESAI") {
                                        onStartExam(exam)
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

@Composable
private fun ExamCardItem(
    exam: ExamScheduleItem,
    onStartExam: () -> Unit
) {
    // Validasi waktu server vs waktu jadwal
    val (canStart, statusBadge, statusColor) = evaluateExamTiming(exam)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Mapel Badge + Status
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
                        text = exam.kodeMapel ?: "UJIAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimaryTealContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Judul Ujian
            Text(
                text = exam.judulUjian ?: exam.namaMapel ?: "Ujian Sekolah",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )

            Text(
                text = exam.namaMapel ?: "Mata Pelajaran",
                fontSize = 13.sp,
                color = SlateGray
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = LightBackground)
            Spacer(modifier = Modifier.height(12.dp))

            // Detail Waktu & Pengawas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = SlateGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = exam.tanggal ?: "-",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkNavy
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SlateGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${exam.jamMulai ?: "--:--"} - ${exam.jamSelesai ?: "--:--"} WIB",
                            fontSize = 12.sp,
                            color = DarkNavy
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = SlateGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = exam.displaySupervisor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkNavy
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = SlateGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = exam.displayRuangan,
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isSubmitted = exam.studentStatus == "SELESAI"
            val isDianulir = exam.studentStatus == "DIANULIR"

            // Tombol Mulai Ujian (Fasilitas 6.3)
            Button(
                onClick = onStartExam,
                enabled = canStart && !isSubmitted,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDianulir) Color(0xFFD97706) else PrimaryTeal,
                    disabledContainerColor = if (isSubmitted) Color(0xFFD1FAE5) else SurfaceVariantColor
                )
            ) {
                if (isSubmitted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ujian Sudah Dikumpulkan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                } else if (isDianulir && canStart) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ujian Ulang Sekarang",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                } else if (canStart) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mulai Ujian Sekarang",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = SlateGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (statusBadge.contains("Selesai")) "Ujian Telah Berakhir" else "Terkunci (Belum Jam Mulai)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateGray
                    )
                }
            }
        }
    }
}

/**
 * Fasilitas 6.3: Validasi Waktu Mulai
 * Tombol Mulai Ujian hanya aktif jika waktu server >= jamMulai dan <= jamSelesai,
 * serta siswa belum menyelesaikan/mengumpulkan ujian.
 */
private fun evaluateExamTiming(exam: ExamScheduleItem): Triple<Boolean, String, Color> {
    if (exam.studentStatus == "SELESAI") {
        return Triple(false, "Sudah Dikerjakan", Color(0xFF059669))
    }

    val isDianulir = exam.studentStatus == "DIANULIR"
    val tanggal = exam.tanggal ?: ""
    val jamMulai = exam.jamMulai ?: ""
    val jamSelesai = exam.jamSelesai ?: ""

    // Format jam HH:mm
    return try {
        val (todayStr, nowTimeStr) = getCurrentWibDateTime()

        if (todayStr.isNotEmpty() && tanggal.isNotBlank() && tanggal < todayStr) {
            Triple(false, if (isDianulir) "Dianulir (Selesai)" else "Selesai", Color(0xFF6B7280))
        } else if (todayStr.isNotEmpty() && tanggal.isNotBlank() && tanggal > todayStr) {
            Triple(false, if (isDianulir) "Ujian Ulang Mendatang" else "Mendatang", if (isDianulir) Color(0xFFD97706) else PrimaryTeal)
        } else {
            // Hari ini
            if (nowTimeStr.isNotEmpty() && jamMulai.isNotBlank() && nowTimeStr < jamMulai) {
                Triple(false, "Mulai $jamMulai WIB", Color(0xFFD97706))
            } else if (nowTimeStr.isNotEmpty() && jamSelesai.isNotBlank() && nowTimeStr > jamSelesai) {
                Triple(false, if (isDianulir) "Dianulir (Selesai)" else "Selesai", Color(0xFF6B7280))
            } else {
                Triple(true, if (isDianulir) "Dianulir (Ujian Ulang)" else "Siap Dikerjakan", if (isDianulir) Color(0xFFD97706) else Color(0xFF059669))
            }
        }
    } catch (_: Exception) {
        // Fallback jika error
        Triple(true, if (isDianulir) "Dianulir (Ujian Ulang)" else "Aktif", if (isDianulir) Color(0xFFD97706) else Color(0xFF059669))
    }
}

