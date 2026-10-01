package com.sch.sekolah_mobile_app.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
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
import com.sch.sekolah_mobile_app.data.model.*
import com.sch.sekolah_mobile_app.data.repository.JadwalRepository
import com.sch.sekolah_mobile_app.ui.screens.ujian.formatScheduleTime
import com.sch.sekolah_mobile_app.ui.screens.ujian.getCurrentWibDateTime
import com.sch.sekolah_mobile_app.ui.screens.ujian.isSchedulePast
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

    val todayPair = remember { getCurrentWibDateTime() }
    val todayDateStr = todayPair.first
    val todayParts = remember(todayDateStr) { todayDateStr.split("-") }
    val realTodayYear = remember(todayParts) { todayParts.getOrNull(0)?.toIntOrNull() ?: 2026 }
    val realTodayMonth = remember(todayParts) { todayParts.getOrNull(1)?.toIntOrNull() ?: 9 }
    val realTodayDay = remember(todayParts) { todayParts.getOrNull(2)?.toIntOrNull() ?: 27 }

    // Calendar navigation state: year, month (1-12), selected day
    var currentYear by remember { mutableStateOf(realTodayYear) }
    var currentMonth by remember { mutableStateOf(realTodayMonth) }
    var selectedDay by remember { mutableStateOf(realTodayDay) }

    // Schedule and Event data
    var allSchedules by remember { mutableStateOf<List<Jadwal>>(emptyList()) }
    var calendarDoc by remember { mutableStateOf<CalendarDocumentMobile?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilterCategory by remember { mutableStateOf("ALL") }

    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    fun fetchData() {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null
                val monthStr = currentMonth.toString().padStart(2, '0')
                val datePrefix = "$currentYear-$monthStr"
                try {
                    val list = jadwalRepository.getSchedules(tanggal = datePrefix)
                    allSchedules = list
                } catch (e: Exception) {
                    println("Failed to fetch schedules: ${e.message}")
                }
                try {
                    val doc = jadwalRepository.getCalendarDocument()
                    calendarDoc = doc
                } catch (e: Exception) {
                    println("Failed to fetch calendar doc: ${e.message}")
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Gagal memuat jadwal"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentYear, currentMonth) {
        fetchData()
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
                j.waktu?.startsWith(selectedDateString) == true ||
                j.waktu?.contains(selectedDateString) == true
            val matchCategory = when (selectedFilterCategory) {
                "MURID" -> !j.isCategoryGuru
                "GURU" -> j.isCategoryGuru
                else -> true
            }
            matchDate && matchCategory
        }
    }

    val allEvents = remember(calendarDoc) {
        calendarDoc?.payload?.schoolEvents ?: emptyList()
    }
    val allNationalHolidays = remember(calendarDoc) {
        calendarDoc?.payload?.nationalHolidays ?: emptyList()
    }

    // Filter events for selected day (respecting target audience)
    val dailyEvents = remember(allEvents, selectedDateString, isGuru) {
        allEvents.filter { evt ->
            val inRange = isDateInRange(selectedDateString, evt.date, evt.endDate)
            val audienceMatch = evt.targetAudience.isNullOrEmpty() ||
                (isGuru && evt.targetAudience.any { it.equals("guru", ignoreCase = true) }) ||
                (!isGuru && evt.targetAudience.any { it.equals("murid", ignoreCase = true) })
            inRange && audienceMatch
        }
    }

    val dailyHolidays = remember(allNationalHolidays, selectedDateString) {
        allNationalHolidays.filter { it.date == selectedDateString }
    }

    val totalAgendaCount = remember(dailyHolidays, dailyEvents, dailySchedules) {
        dailyHolidays.size + dailyEvents.size + dailySchedules.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kalender",
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
                    IconButton(onClick = { fetchData() }) {
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
                                        val isToday = (dayNum == realTodayDay && currentMonth == realTodayMonth && currentYear == realTodayYear)
                                        val dayDateStr = "$currentYear-${currentMonth.toString().padStart(2, '0')}-${dayNum.toString().padStart(2, '0')}"

                                        val isSunday = (c == 6)
                                        val hasHoliday = allNationalHolidays.any { it.date == dayDateStr }
                                        val hasSchoolHoliday = allEvents.any { evt ->
                                            evt.isHoliday == true && isDateInRange(dayDateStr, evt.date, evt.endDate)
                                        }
                                        val isRedDay = isSunday || hasHoliday || hasSchoolHoliday

                                        val hasExam = allEvents.any { evt ->
                                            val type = evt.type?.uppercase() ?: ""
                                            val audienceMatch = evt.targetAudience.isNullOrEmpty() ||
                                                (isGuru && evt.targetAudience.any { it.equals("guru", ignoreCase = true) }) ||
                                                (!isGuru && evt.targetAudience.any { it.equals("murid", ignoreCase = true) })
                                            val isExamType = type.contains("UJIAN") || type.contains("EXAM") ||
                                                evt.title?.contains("UJIAN", ignoreCase = true) == true ||
                                                evt.title?.contains("UTS", ignoreCase = true) == true ||
                                                evt.title?.contains("UAS", ignoreCase = true) == true
                                            isExamType && audienceMatch && isDateInRange(dayDateStr, evt.date, evt.endDate)
                                        }

                                        val hasGeneralEvent = allEvents.any { evt ->
                                            val type = evt.type?.uppercase() ?: ""
                                            val isExamType = type.contains("UJIAN") || type.contains("EXAM") ||
                                                evt.title?.contains("UJIAN", ignoreCase = true) == true ||
                                                evt.title?.contains("UTS", ignoreCase = true) == true ||
                                                evt.title?.contains("UAS", ignoreCase = true) == true
                                            val isHol = evt.isHoliday == true
                                            val audienceMatch = evt.targetAudience.isNullOrEmpty() ||
                                                (isGuru && evt.targetAudience.any { it.equals("guru", ignoreCase = true) }) ||
                                                (!isGuru && evt.targetAudience.any { it.equals("murid", ignoreCase = true) })
                                            !isExamType && !isHol && audienceMatch && isDateInRange(dayDateStr, evt.date, evt.endDate)
                                        }

                                        val hasSchedule = allSchedules.any { j ->
                                            j.waktuMulai?.startsWith(dayDateStr) == true || j.waktu?.startsWith(dayDateStr) == true
                                        }

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
                                                        isRedDay -> ErrorRed
                                                        else -> DarkNavy
                                                    }
                                                )

                                                // Colored dot indicators for Holiday, Exam, Event, Schedule
                                                val hasAnyDot = hasHoliday || hasSchoolHoliday || hasExam || hasGeneralEvent || hasSchedule
                                                if (hasAnyDot) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    ) {
                                                        if (hasHoliday || hasSchoolHoliday) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.White else ErrorRed)
                                                            )
                                                        }
                                                        if (hasExam) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.White else Color(0xFFF97316))
                                                            )
                                                        }
                                                        if (hasGeneralEvent) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.White else Color(0xFF3B82F6))
                                                            )
                                                        }
                                                        if (hasSchedule) {
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calendar Legend
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = ErrorRed, label = "Libur")
                        LegendItem(color = Color(0xFFF97316), label = "Ujian")
                        LegendItem(color = Color(0xFF3B82F6), label = "Event")
                        LegendItem(color = PrimaryTeal, label = "KBM")
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
                    text = "$totalAgendaCount Agenda",
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }

            // Schedules and Events List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            } else if (totalAgendaCount == 0) {
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
                            text = "Tidak Ada Agenda Kegiatan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tidak ada sesi kelas, ujian, agenda sekolah, atau hari libur untuk tanggal yang dipilih.",
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
                    // 1. Hari Libur Nasional
                    items(dailyHolidays) { hol ->
                        NationalHolidayCardItem(holiday = hol)
                    }

                    // 2. Event Sekolah & Ujian Master
                    items(dailyEvents) { evt ->
                        SchoolEventCardItem(event = evt)
                    }

                    // 3. Jadwal KBM / Ujian Kelas
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
                    text = "${formatScheduleTime(jadwal.waktuMulai)} - ${formatScheduleTime(jadwal.waktuSelesai)} WIB",
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

            val isEnded = isSchedulePast(jadwal.waktuSelesai, jadwal.waktuMulai, jadwal.waktu) || "SELESAI".equals(jadwal.status, ignoreCase = true)

            // Action Buttons based on Role
            if (isGuru) {
                if (!jadwal.isCategoryGuru) {
                    if (isEnded) {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFFF1F5F9),
                                disabledContentColor = SlateGray
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = SlateGray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Jadwal Telah Berakhir", color = SlateGray)
                        }
                    } else {
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
                }
            } else {
                // Murid Action Button
                if (isEnded) {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFFF1F5F9),
                            disabledContentColor = SlateGray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = SlateGray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sesi Presensi Telah Berakhir", color = SlateGray)
                    }
                } else {
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
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = SlateGray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NationalHolidayCardItem(holiday: NationalHolidayMobile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border = BorderStroke(1.dp, Color(0xFFFECACA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFFFEE2E2),
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = ErrorRed,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "HARI LIBUR NASIONAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = holiday.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )
            }
        }
    }
}

@Composable
private fun SchoolEventCardItem(event: SchoolEventMobile) {
    val type = event.type?.uppercase() ?: ""
    val isExam = type.contains("UJIAN") || type.contains("EXAM") ||
            event.title?.contains("UJIAN", ignoreCase = true) == true ||
            event.title?.contains("UTS", ignoreCase = true) == true ||
            event.title?.contains("UAS", ignoreCase = true) == true
    val isHoliday = event.isHoliday == true

    val bgColor = when {
        isHoliday -> Color(0xFFFEF2F2)
        isExam -> Color(0xFFFFF7ED)
        else -> Color(0xFFEFF6FF)
    }
    val borderColor = when {
        isHoliday -> Color(0xFFFECACA)
        isExam -> Color(0xFFFED7AA)
        else -> Color(0xFFBFDBFE)
    }
    val badgeColor = when {
        isHoliday -> ErrorRed
        isExam -> Color(0xFFEA580C)
        else -> Color(0xFF2563EB)
    }
    val badgeText = when {
        isHoliday -> "LIBUR SEKOLAH"
        isExam -> "UJIAN & ASESMEN"
        else -> "AGENDA SEKOLAH"
    }
    val iconTint = when {
        isHoliday -> ErrorRed
        isExam -> Color(0xFFEA580C)
        else -> Color(0xFF2563EB)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (!event.endDate.isNullOrBlank() && event.endDate != event.date) {
                    Surface(
                        color = Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${event.date} s/d ${event.endDate}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateGray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = event.title ?: "Event Sekolah",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )

            if (!event.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    fontSize = 12.sp,
                    color = DarkNavy.copy(alpha = 0.85f)
                )
            }

            if (!event.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.location,
                        fontSize = 11.sp,
                        color = SlateGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun isDateInRange(targetDate: String, startDate: String?, endDate: String?): Boolean {
    if (startDate.isNullOrBlank()) return false
    val end = if (endDate.isNullOrBlank()) startDate else endDate
    return targetDate in startDate..end
}
