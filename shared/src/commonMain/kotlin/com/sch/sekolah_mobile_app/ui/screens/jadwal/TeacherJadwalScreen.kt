package com.sch.sekolah_mobile_app.ui.screens.jadwal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.Jadwal
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.JadwalRepository
import com.sch.sekolah_mobile_app.ui.screens.ujian.extractScheduleDate
import com.sch.sekolah_mobile_app.ui.screens.ujian.formatScheduleTime
import com.sch.sekolah_mobile_app.ui.screens.ujian.getCurrentWibDateTime
import com.sch.sekolah_mobile_app.ui.screens.ujian.isScheduleOnDate
import com.sch.sekolah_mobile_app.ui.screens.ujian.isSchedulePast
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

private enum class JadwalFilterTab(val label: String) {
    ALL("Semua"),
    TODAY("Hari Ini"),
    MENGAJAR("Mengajar"),
    PENGAWAS("Pengawas")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherJadwalScreen(
    profile: UserProfileResponse?,
    jadwalRepository: JadwalRepository,
    onNavigateBack: (() -> Unit)? = null,
    onOpenKiosk: (Jadwal) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var schedules by remember { mutableStateOf<List<Jadwal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(JadwalFilterTab.ALL) }

    val todayDateStr = remember { getCurrentWibDateTime().first }

    fun fetchSchedules() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val list = jadwalRepository.getSchedules()
                schedules = list
            } catch (e: Exception) {
                errorMessage = e.message ?: "Gagal memuat jadwal guru."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchSchedules()
    }

    // Helper functions
    fun isPengawasSchedule(j: Jadwal): Boolean {
        val id = j.effectiveId.lowercase()
        val title = (j.judulKegiatan ?: "").lowercase()
        val notes = (j.notes ?: "").lowercase()
        val pengawas = (j.pengawas ?: "").lowercase()
        return id.startsWith("supervisor-") ||
                title.contains("pengawas") ||
                notes.contains("pengawas") ||
                pengawas.isNotBlank()
    }

    fun isTodaySchedule(j: Jadwal): Boolean {
        return isScheduleOnDate(todayDateStr, j)
    }

    // Counts
    val allCount = schedules.size
    val todayCount = schedules.count { isTodaySchedule(it) }
    val pengajarCount = schedules.count { !isPengawasSchedule(it) }
    val pengawasCount = schedules.count { isPengawasSchedule(it) }

    // Filtered items
    val filteredSchedules = remember(schedules, selectedTab, searchQuery) {
        schedules.filter { j ->
            val matchTab = when (selectedTab) {
                JadwalFilterTab.ALL -> true
                JadwalFilterTab.TODAY -> isTodaySchedule(j)
                JadwalFilterTab.MENGAJAR -> !isPengawasSchedule(j)
                JadwalFilterTab.PENGAWAS -> isPengawasSchedule(j)
            }

            val query = searchQuery.trim().lowercase()
            val matchQuery = if (query.isBlank()) true else {
                (j.displayTitle.lowercase().contains(query)) ||
                (j.mataPelajaran?.lowercase()?.contains(query) == true) ||
                (j.kelas?.lowercase()?.contains(query) == true) ||
                (j.ruangan?.lowercase()?.contains(query) == true) ||
                (j.notes?.lowercase()?.contains(query) == true)
            }

            matchTab && matchQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Jadwal Guru",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = "Mengajar, Pengawas & Presensi QR Kiosk",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { fetchSchedules() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Perbarui", tint = PrimaryTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightBackground)
        ) {
            // Search Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari judul, kelas, ruangan, atau mapel...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SlateGray, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = SlateGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Filter Tabs Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(JadwalFilterTab.entries) { tab ->
                    val isSelected = selectedTab == tab
                    val count = when (tab) {
                        JadwalFilterTab.ALL -> allCount
                        JadwalFilterTab.TODAY -> todayCount
                        JadwalFilterTab.MENGAJAR -> pengajarCount
                        JadwalFilterTab.PENGAWAS -> pengawasCount
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = "${tab.label} ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal,
                            selectedLabelColor = Color.White,
                            containerColor = CardSurface,
                            labelColor = DarkNavy
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PrimaryTeal else Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading && schedules.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryTeal)
                    }
                } else if (errorMessage != null && schedules.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = errorMessage ?: "", color = ErrorRed, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { fetchSchedules() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                } else if (filteredSchedules.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = SlateGray, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tidak Ada Jadwal Ditemukan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Tidak ada jadwal yang cocok dengan kata kunci \"$searchQuery\"."
                                else "Belum ada agenda jadwal untuk kategori yang dipilih.",
                                fontSize = 12.sp,
                                color = SlateGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        items(filteredSchedules, key = { it.effectiveId }) { jadwal ->
                            val isPengawas = isPengawasSchedule(jadwal)
                            TeacherJadwalCard(
                                jadwal = jadwal,
                                isPengawas = isPengawas,
                                onOpenKiosk = { onOpenKiosk(jadwal) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherJadwalCard(
    jadwal: Jadwal,
    isPengawas: Boolean,
    onOpenKiosk: () -> Unit
) {
    val isEnded = isSchedulePast(jadwal.waktuSelesai, jadwal.waktuMulai, jadwal.waktu) || "SELESAI".equals(jadwal.status, ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Role Badge + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role Badge
                if (isPengawas) {
                    Surface(
                        color = Color(0xFFEDE9FE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF6D28D9),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PENGAWAS UJIAN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF6D28D9)
                            )
                        }
                    }
                } else {
                    Surface(
                        color = PrimaryTealContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = OnPrimaryTealContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GURU PENGAJAR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnPrimaryTealContainer
                            )
                        }
                    }
                }

                val isEnded = isSchedulePast(jadwal.waktuSelesai, jadwal.waktuMulai, jadwal.waktu) || "SELESAI".equals(jadwal.status, ignoreCase = true)

                // Status Badge
                Surface(
                    color = if (isEnded) Color(0xFFF1F5F9) else if ("PUBLISHED".equals(jadwal.status, ignoreCase = true)) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isEnded) "SELESAI" else (jadwal.status ?: "TERJADWAL"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isEnded) SlateGray else if ("PUBLISHED".equals(jadwal.status, ignoreCase = true)) Color(0xFF15803D) else SlateGray,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = jadwal.displayTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Kelas & Ruangan Info Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Kelas: ${jadwal.kelas ?: "Semua"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkNavy
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Room, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ruang: ${jadwal.ruangan ?: "KBM"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkNavy
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = SlateGray, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${formatScheduleTime(jadwal.waktuMulai)} - ${formatScheduleTime(jadwal.waktuSelesai)} WIB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkNavy
                )

                val datePart = extractScheduleDate(jadwal.waktuMulai) ?: extractScheduleDate(jadwal.waktu)
                if (datePart != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SlateGray, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = datePart,
                        fontSize = 11.sp,
                        color = SlateGray
                    )
                }
            }

            if (!jadwal.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan: ${jadwal.notes}",
                    fontSize = 11.sp,
                    color = SlateGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button: Buka Presensi QR (Kiosk) or Disabled if Ended
            if (isEnded) {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(0xFFF1F5F9),
                        disabledContentColor = SlateGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = SlateGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jadwal Telah Berakhir",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateGray
                    )
                }
            } else {
                Button(
                    onClick = onOpenKiosk,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPengawas) Color(0xFF6D28D9) else PrimaryTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buka Presensi QR (Kiosk)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

