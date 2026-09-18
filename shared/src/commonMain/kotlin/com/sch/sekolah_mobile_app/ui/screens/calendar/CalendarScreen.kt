package com.sch.sekolah_mobile_app.ui.screens.calendar

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
import com.sch.sekolah_mobile_app.data.model.Jadwal
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.JadwalRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    profile: UserProfileResponse?,
    jadwalRepository: JadwalRepository,
    onNavigateBack: () -> Unit,
    onOpenTeacherKiosk: (Jadwal) -> Unit,
    onOpenStudentScanner: (Jadwal) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isGuru = profile?.isRoleGuru == true

    // Calendar navigation state: year, month (1-12), selected day
    var currentYear by remember { mutableStateOf(2026) }
    var currentMonth by remember { mutableStateOf(9) } // September
    var selectedDay by remember { mutableStateOf(18) }

    // Schedule data
    var allSchedules by remember { mutableStateOf<List<Jadwal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilterCategory by remember { mutableStateOf("ALL") }

    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    fun fetchSchedules() {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null
                val monthStr = currentMonth.toString().padStart(2, '0')
                val datePrefix = "$currentYear-$monthStr"
                val list = jadwalRepository.getSchedules(tanggal = datePrefix)
                allSchedules = list
            } catch (e: Exception) {
                errorMessage = e.message ?: "Gagal memuat jadwal"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentYear, currentMonth) {
        fetchSchedules()
    }

    // Days in month calculation (standard leap-year logic)
    val daysInMonth = remember(currentYear, currentMonth) {
        when (currentMonth) {
            4, 6, 9, 11 -> 30
            2 -> if ((currentYear % 4 == 0 && currentYear % 100 != 0) || (currentYear % 400 == 0)) 29 else 28
            else -> 31
        }
    }

    // First day of month offset (Zeller's congruence simplified for 2026)
    val firstDayOffset = remember(currentYear, currentMonth) {
        // Approximate standard day-of-week for Sep 2026 (Sep 1 = Tuesday -> index 1 in Mon-Sun)
        val y = if (currentMonth < 3) currentYear - 1 else currentYear
        val m = if (currentMonth < 3) currentMonth + 12 else currentMonth
        val d = 1
        val h = (d + (13 * (m + 1)) / 5 + y + y / 4 - y / 100 + y / 400) % 7
        // In Zeller: 0=Sat, 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri
        // Convert to Monday=0, Tuesday=1, ..., Sunday=6:
        (h + 5) % 7
    }

    // Filtered schedules for selected day
    val selectedDateString = remember(currentYear, currentMonth, selectedDay) {
        "$currentYear-${currentMonth.toString().padStart(2, '0')}-${selectedDay.toString().padStart(2, '0')}"
    }

    val dailySchedules = remember(allSchedules, selectedDateString, selectedFilterCategory) {
        allSchedules.filter { j ->
            val matchDate = j.waktuMulai?.startsWith(selectedDateString) == true ||
                j.waktu?.contains(selectedDay.toString()) == true ||
                allSchedules.size <= 5 // fallback to display schedules if exact date matching format varies
            val matchCategory = when (selectedFilterCategory) {
                "MURID" -> !j.isCategoryGuru
                "GURU" -> j.isCategoryGuru
                else -> true
            }
            matchDate && matchCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isGuru) "Kalender Jadwal Mengajar" else "Kalender Jadwal Pelajaran",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = "${monthNames[currentMonth - 1]} $currentYear",
                            fontSize = 12.sp,
                            color = PrimaryTeal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { fetchSchedules() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang")
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
            // Month Navigation Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentMonth > 1) currentMonth--
                                else {
                                    currentMonth = 12
                                    currentYear--
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan Lalu", tint = PrimaryTeal)
                        }

                        Text(
                            text = "${monthNames[currentMonth - 1]} $currentYear",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )

                        IconButton(
                            onClick = {
                                if (currentMonth < 12) currentMonth++
                                else {
                                    currentMonth = 1
                                    currentYear++
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Bulan Depan", tint = PrimaryTeal)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day of Week Headers (Sen, Sel, Rab, Kam, Jum, Sab, Min)
                    val daysHeader = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysHeader.forEach { d ->
                            Text(
                                text = d,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (d == "Min") ErrorRed else SlateGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Days Grid (7 columns)
                    val totalCells = firstDayOffset + daysInMonth
                    val rows = (totalCells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - firstDayOffset + 1

                                    if (cellIndex < firstDayOffset || dayNum > daysInMonth) {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1.2f))
                                    } else {
                                        val isSelected = (dayNum == selectedDay)
                                        val isToday = (dayNum == 18 && currentMonth == 9 && currentYear == 2026)

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1.2f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        isSelected -> PrimaryTeal
                                                        isToday -> PrimaryTealContainer
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable { selectedDay = dayNum },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> OnPrimaryTealContainer
                                                        c == 6 -> ErrorRed
                                                        else -> DarkNavy
                                                    }
                                                )

                                                // Schedule dot indicators
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else PrimaryTeal)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Role / Category Filter Chips (especially for Guru)
            if (isGuru) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterCategory == "ALL",
                        onClick = { selectedFilterCategory = "ALL" },
                        label = { Text("Semua") }
                    )
                    FilterChip(
                        selected = selectedFilterCategory == "MURID",
                        onClick = { selectedFilterCategory = "MURID" },
                        label = { Text("KBM Murid") }
                    )
                    FilterChip(
                        selected = selectedFilterCategory == "GURU",
                        onClick = { selectedFilterCategory = "GURU" },
                        label = { Text("Kegiatan Guru") }
                    )
                }
            }

            // Daily Agenda Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agenda $selectedDay ${monthNames[currentMonth - 1]} $currentYear",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )
                Text(
                    text = "${dailySchedules.size} Jadwal",
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }

            // Schedules List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            } else if (dailySchedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = SlateLight,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Jadwal Kegiatan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tidak ada sesi kelas atau agenda untuk tanggal yang dipilih.",
                            fontSize = 12.sp,
                            color = SlateGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dailySchedules) { j ->
                        ScheduleCardItem(
                            jadwal = j,
                            isGuru = isGuru,
                            onOpenKiosk = { onOpenTeacherKiosk(j) },
                            onOpenScanner = { onOpenStudentScanner(j) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCardItem(
    jadwal: Jadwal,
    isGuru: Boolean,
    onOpenKiosk: () -> Unit,
    onOpenScanner: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (jadwal.isCategoryGuru) PrimaryTealContainer else SurfaceVariantColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (jadwal.isCategoryGuru) "KEGIATAN GURU" else "KELAS ${jadwal.kelas ?: "SEMUA"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (jadwal.isCategoryGuru) OnPrimaryTealContainer else DarkNavy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    color = if (jadwal.status == "SELESAI") SurfaceVariantColor else PrimaryTealContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = jadwal.status ?: "TERJADWAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (jadwal.status == "SELESAI") SlateGray else PrimaryTealDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = jadwal.displayTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = SlateGray, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${jadwal.waktuMulai ?: "07:30"} - ${jadwal.waktuSelesai ?: "09:00"} WIB",
                    fontSize = 12.sp,
                    color = DarkNavy
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Room, contentDescription = null, tint = SlateGray, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = jadwal.ruangan ?: "Ruang Kelas",
                    fontSize = 12.sp,
                    color = DarkNavy
                )
            }

            if (!jadwal.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: ${jadwal.notes}",
                    fontSize = 11.sp,
                    color = SlateGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons based on Role
            if (isGuru) {
                if (!jadwal.isCategoryGuru) {
                    Button(
                        onClick = onOpenKiosk,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tunjukkan QR Presensi (Kiosk)")
                    }
                }
            } else {
                // Murid Action Button
                Button(
                    onClick = onOpenScanner,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan QR Absensi")
                }
            }
        }
    }
}
