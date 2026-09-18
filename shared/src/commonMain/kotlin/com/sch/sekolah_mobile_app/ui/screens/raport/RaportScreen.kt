package com.sch.sekolah_mobile_app.ui.screens.raport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.CategoryGradeItem
import com.sch.sekolah_mobile_app.data.model.StudentRaportResponse
import com.sch.sekolah_mobile_app.data.model.SubjectRaportItem
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.RaportRepository
import com.sch.sekolah_mobile_app.data.repository.RemoteConfigManager
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaportScreen(
    raportRepository: RaportRepository,
    profile: UserProfileResponse?,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isRaportModuleEnabled by RemoteConfigManager.instance.isRaportModuleEnabled.collectAsState()
    val isFinalGradeEnabled by RemoteConfigManager.instance.isFinalGradeEnabled.collectAsState()

    var raportData by remember { mutableStateOf<StudentRaportResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var expandedSubjects by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showEvictionDialog by remember { mutableStateOf(false) }

    // Active Session Eviction (Level 1): Jika modul dimatikan saat siswa sedang membaca rapor
    LaunchedEffect(isRaportModuleEnabled) {
        if (!isRaportModuleEnabled) {
            raportData = null
            showEvictionDialog = true
        }
    }

    fun loadRaport() {
        if (!isRaportModuleEnabled) {
            showEvictionDialog = true
            return
        }
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val res = raportRepository.getMyRaport()
                raportData = res
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Gagal memuat lembar raport siswa."
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRaport()
    }

    if (showEvictionDialog) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706))
                    Spacer(Modifier.width(8.dp))
                    Text("Akses Rapor Ditutup", fontWeight = FontWeight.Bold, color = DarkNavy)
                }
            },
            text = {
                Text(
                    "Pihak sekolah telah menutup sementara akses lembar rapor untuk verifikasi akademik. Anda akan dialihkan kembali ke Beranda."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEvictionDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Kembali ke Beranda")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Rapor Semesteran",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = "Laporan Capaian Hasil Belajar",
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
                    IconButton(onClick = { loadRaport() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = PrimaryTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackground)
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryTeal)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Menghitung nilai rapor siswa...",
                                fontSize = 14.sp,
                                color = SlateGray
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Gagal Memuat Rapor",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 13.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { loadRaport() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                }

                raportData != null -> {
                    val data = raportData!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Identity & Active Semester Card
                        item {
                            StudentIdentityHeader(data = data)
                        }

                        // 2. Metrics (GPA & Kehadiran)
                        item {
                            RaportMetricsSection(data = data, isFinalGradeEnabled = isFinalGradeEnabled)
                        }

                        // 3. Subjects Section Title
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Nilai Mata Pelajaran",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Surface(
                                    color = PrimaryTealContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${data.subjects.size} Mapel",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnPrimaryTealContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // 4. Subjects List
                        if (data.subjects.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Belum ada capaian ujian atau mata pelajaran yang terdata pada semester ini.",
                                            fontSize = 13.sp,
                                            color = SlateGray
                                        )
                                    }
                                }
                            }
                        } else {
                            items(data.subjects) { subject ->
                                val isExpanded = expandedSubjects.contains(subject.kodeMapel)
                                SubjectGradeCard(
                                    subject = subject,
                                    isExpanded = isExpanded,
                                    isFinalGradeEnabled = isFinalGradeEnabled,
                                    onToggleExpand = {
                                        expandedSubjects = if (isExpanded) {
                                            expandedSubjects - subject.kodeMapel
                                        } else {
                                            expandedSubjects + subject.kodeMapel
                                        }
                                    }
                                )
                            }
                        }

                        // Footer Note
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Kalkulasi resmi sistem berbasis pembobotan kategori ujian aktif dan rentang tanggal semester tunggal.",
                                fontSize = 11.sp,
                                color = SlateGray,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentIdentityHeader(data: StudentRaportResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(DarkNavy, Color(0xFF1E293B))
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAPORAN HASIL BELAJAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                    Surface(
                        color = Color(0xFF059669),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (data.status == "FINALIZED") "FINAL" else "AKTIF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = data.namaMurid.ifEmpty { "Siswa" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("NIS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f))
                        Text(data.nis, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Column {
                        Text("KELAS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f))
                        Text(data.kelas, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Column {
                        Text("SEMESTER", fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f))
                        Text(data.semesterName.ifEmpty { "Semester Aktif" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                if (data.startDate.isNotEmpty() && data.endDate.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📅 Periode: ${data.startDate} s.d. ${data.endDate}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RaportMetricsSection(data: StudentRaportResponse, isFinalGradeEnabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // GPA Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "RATA-RATA AKHIR (GPA)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isFinalGradeEnabled && data.gpa != null) {
                        Text(
                            text = ((data.gpa * 10.0).toLong() / 10.0).toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                        Text(
                            text = "Skala 0 - 100",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Menunggu Pleno",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                        Text(
                            text = "Nilai belum dirilis",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                }
            }

            // Attendance % Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                val pct = data.attendance.persentaseKehadiran
                val pctFormatted = ((pct * 10.0).toLong() / 10.0).toString()
                val color = if (pct >= 80.0) Color(0xFF059669) else Color(0xFFDC2626)

                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PRESENSI KEHADIRAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$pctFormatted%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "${data.attendance.hadir} Hadir / ${data.attendance.totalHariKBM} KBM",
                        fontSize = 11.sp,
                        color = SlateGray
                    )
                }
            }
        }

        // Attendance 4-chip Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "REKAPITULASI PRESENSI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendancePill(modifier = Modifier.weight(1f), label = "HADIR", count = data.attendance.hadir, bg = Color(0xFFECFDF5), fg = Color(0xFF065F46))
                    AttendancePill(modifier = Modifier.weight(1f), label = "SAKIT", count = data.attendance.sakit, bg = Color(0xFFFEF3C7), fg = Color(0xFF92400E))
                    AttendancePill(modifier = Modifier.weight(1f), label = "IZIN", count = data.attendance.izin, bg = Color(0xFFEFF6FF), fg = Color(0xFF1E40AF))
                    AttendancePill(modifier = Modifier.weight(1f), label = "ALPA", count = data.attendance.alpa, bg = Color(0xFFFEF2F2), fg = Color(0xFF991B1B))
                }
            }
        }
    }
}

@Composable
private fun AttendancePill(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    bg: Color,
    fg: Color
) {
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = fg)
            Text(text = "$count", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}

@Composable
private fun SubjectGradeCard(
    subject: SubjectRaportItem,
    isExpanded: Boolean,
    isFinalGradeEnabled: Boolean,
    onToggleExpand: () -> Unit
) {
    val isGradeVisible = isFinalGradeEnabled && subject.finalScore != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Grade Badge Circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGradeVisible) {
                                if (subject.lulus == true) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                            } else {
                                Color(0xFFFEF3C7)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGradeVisible && subject.predikat != null) {
                        Text(
                            text = subject.predikat,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (subject.lulus == true) Color(0xFF047857) else Color(0xFFDC2626)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = subject.namaMapel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = subject.kodeMapel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateGray,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Guru: ${subject.namaGuru.ifEmpty { "-" }} • KKM: ${subject.kkm}",
                        fontSize = 11.sp,
                        color = SlateGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Final Score & Status Badge
                Column(horizontalAlignment = Alignment.End) {
                    if (isGradeVisible && subject.finalScore != null) {
                        val finalFormatted = ((subject.finalScore * 10.0).toLong() / 10.0).toString()
                        Text(
                            text = finalFormatted,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (subject.lulus == true) Color(0xFF059669) else Color(0xFFDC2626)
                        )
                        Surface(
                            color = if (subject.lulus == true) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (subject.lulus == true) "TUNTAS" else "REMIDI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (subject.lulus == true) Color(0xFF166534) else Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Menunggu Pleno",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SlateGray
                )
            }

            // Accordion Breakdown
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (isGradeVisible) {
                        Text(
                            text = "RINCIAN BOBOT KATEGORI UJIAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (subject.categoryBreakdown.isEmpty()) {
                            Text(
                                text = "Belum ada data kategori ujian.",
                                fontSize = 12.sp,
                                color = SlateGray
                            )
                        } else {
                            subject.categoryBreakdown.forEach { cat ->
                                val avgFormatted = ((cat.averageScore * 10.0).toLong() / 10.0).toString()
                                val weightContrib = ((cat.weightedScore * 10.0).toLong() / 10.0).toString()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${cat.kategoriNama} (${cat.kategoriKode})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkNavy
                                        )
                                        Text(
                                            text = "Bobot: ${cat.bobot}% • Diikuti: ${cat.totalExams} ujian",
                                            fontSize = 10.sp,
                                            color = SlateGray
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Rata-rata: $avgFormatted",
                                            fontSize = 11.sp,
                                            color = DarkNavy
                                        )
                                        Text(
                                            text = "+$weightContrib poin",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryTeal
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Rincian nilai disembunyikan sampai sidang pleno dewan guru selesai disahkan.",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }
    }
}

