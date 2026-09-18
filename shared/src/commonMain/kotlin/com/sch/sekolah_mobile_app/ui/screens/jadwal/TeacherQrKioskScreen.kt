package com.sch.sekolah_mobile_app.ui.screens.jadwal

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.AbsensiResultMobile
import com.sch.sekolah_mobile_app.data.model.Jadwal
import com.sch.sekolah_mobile_app.data.model.QrPayloadMobile
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.JadwalRepository
import com.sch.sekolah_mobile_app.ui.screens.ujian.ExamKioskEffect
import com.sch.sekolah_mobile_app.ui.screens.ujian.releaseExamKiosk
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherQrKioskScreen(
    jadwal: Jadwal,
    profile: UserProfileResponse?,
    jadwalRepository: JadwalRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scheduleId = jadwal.effectiveId
    val teacherNip = profile?.nip?.takeIf { it != "-" } ?: profile?.email?.substringBefore('@') ?: ""

    // QR State
    var qrPayload by remember { mutableStateOf<QrPayloadMobile?>(null) }
    var secondsRemaining by remember { mutableStateOf(20) }
    var isLoadingQr by remember { mutableStateOf(false) }
    var qrErrorMessage by remember { mutableStateOf<String?>(null) }

    // Kiosk & PIN State
    var isKioskLocked by remember { mutableStateOf(false) }
    var activePin by remember { mutableStateOf("") }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showUnlockPinDialog by remember { mutableStateOf(false) }

    // Temporary PIN entry dialog state
    var inputPin by remember { mutableStateOf("") }
    var inputPinConfirm by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Live Attendees state
    var attendees by remember { mutableStateOf<List<AbsensiResultMobile>>(emptyList()) }
    var isAttendeeSheetVisible by remember { mutableStateOf(false) }

    // System Kiosk Effect Hook
    ExamKioskEffect(
        enabled = isKioskLocked,
        onViolationDetected = { _ -> }
    )

    // Function to generate new rolling QR token
    suspend fun refreshQrToken() {
        if (scheduleId.isBlank()) return
        try {
            isLoadingQr = true
            qrErrorMessage = null
            val res = jadwalRepository.generateAttendanceQr(scheduleId)
            qrPayload = res
            secondsRemaining = res.refreshIntervalSeconds
        } catch (e: Exception) {
            qrErrorMessage = e.message ?: "Gagal memuat QR presensi"
        } finally {
            isLoadingQr = false
        }
    }

    // Initial load
    LaunchedEffect(scheduleId) {
        refreshQrToken()
    }

    // Rolling QR Timer (re-generates every interval)
    LaunchedEffect(scheduleId) {
        while (isActive) {
            delay(1000)
            if (secondsRemaining > 1) {
                secondsRemaining--
            } else {
                refreshQrToken()
            }
        }
    }

    // Remote Emergency Unlock Polling (active when Kiosk is locked)
    LaunchedEffect(isKioskLocked, teacherNip) {
        if (!isKioskLocked || teacherNip.isBlank()) return@LaunchedEffect
        while (isActive && isKioskLocked) {
            delay(5000)
            try {
                val poll = jadwalRepository.pollEmergencyUnlock(teacherNip)
                if (poll.unlocked) {
                    releaseExamKiosk()
                    isKioskLocked = false
                    activePin = ""
                }
            } catch (_: Exception) {}
        }
    }

    // Attendance polling
    LaunchedEffect(scheduleId) {
        while (isActive) {
            if (scheduleId.isNotBlank()) {
                try {
                    val list = jadwalRepository.getAttendanceList(scheduleId)
                    attendees = list
                } catch (_: Exception) {}
            }
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isKioskLocked) "Mode Kiosk Guru (Layar Terkunci)" else "QR Presensi KBM",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isKioskLocked) ErrorRed else DarkNavy
                        )
                        Text(
                            text = jadwal.displayTitle,
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }
                },
                navigationIcon = {
                    if (!isKioskLocked) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = ErrorRed,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isAttendeeSheetVisible = true }) {
                        BadgedBox(
                            badge = {
                                if (attendees.isNotEmpty()) {
                                    Badge(containerColor = PrimaryTeal) {
                                        Text("${attendees.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.People, contentDescription = "Peserta Hadir")
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Schedule Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                            color = PrimaryTealContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "KELAS ${jadwal.kelas ?: "SEMUA"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnPrimaryTealContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "Ruang: ${jadwal.ruangan ?: "KBM"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateGray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = jadwal.displayTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = SlateGray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${jadwal.waktuMulai ?: "07:30"} - ${jadwal.waktuSelesai ?: "09:00"} WIB",
                            fontSize = 13.sp,
                            color = DarkNavy
                        )
                    }

                    if (!jadwal.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Catatan: ${jadwal.notes}",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rolling QR Container Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isKioskLocked) Color.White else CardSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (secondsRemaining > 5) PrimaryTeal else AccentAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dynamic Rolling QR • Berganti dalam ${secondsRemaining}d",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Code Canvas or Loading State
                    val currentToken = qrPayload?.token
                    if (!currentToken.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            QrCodeView(
                                content = currentToken,
                                modifier = Modifier.fillMaxSize(),
                                backgroundColor = Color.White,
                                codeColor = Color.Black
                            )
                        }
                    } else if (isLoadingQr) {
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .background(SurfaceVariantColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryTeal)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .background(SurfaceVariantColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = qrErrorMessage ?: "QR Belum Tersedia",
                                color = ErrorRed,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Arahkan kamera HP Siswa ke kode QR di atas untuk presensi kehadiran KBM.",
                        fontSize = 11.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Kiosk Mode Controls
            if (!isKioskLocked) {
                Button(
                    onClick = {
                        inputPin = ""
                        inputPinConfirm = ""
                        pinError = null
                        showSetPinDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.ScreenLockPortrait, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tunjukkan & Kunci Layar (Kiosk Mode)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Mode Kiosk mengunci HP pada layar ini dengan PIN 4-digit agar murid tidak dapat mengakses data pribadi atau aplikasi lain saat Anda meletakkan HP.",
                    fontSize = 11.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            } else {
                // When Kiosk is ACTIVE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ErrorRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Layar Terkunci (Kiosk Mode Aktif)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Tombol Home, Back, dan Panel Notifikasi dimatikan oleh sistem. Masukkan PIN untuk membuka kunci, atau tekan 'Batal Kunci Kiosk' pada Web Dashboard bila Anda lupa PIN.",
                            fontSize = 12.sp,
                            color = Color(0xFF771D1D),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                inputPin = ""
                                pinError = null
                                showUnlockPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buka Kunci Layar (Masukkan PIN)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Dialog: Set PIN to Enter Kiosk Mode
    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = {
                Text(text = "Pasang PIN Kiosk 4-Digit", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Tentukan 4 angka PIN untuk mengunci dan membuka kembali layar HP Anda.",
                        fontSize = 13.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) inputPin = it },
                        label = { Text("PIN 4-Digit") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputPinConfirm,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) inputPinConfirm = it },
                        label = { Text("Konfirmasi PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pinError ?: "",
                            color = ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPin.length != 4) {
                            pinError = "PIN harus tepat 4 digit angka"
                        } else if (inputPin != inputPinConfirm) {
                            pinError = "Konfirmasi PIN tidak cocok"
                        } else {
                            activePin = inputPin
                            isKioskLocked = true
                            showSetPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Kunci Layar Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Unlock Kiosk Mode with PIN
    if (showUnlockPinDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockPinDialog = false },
            title = {
                Text(text = "Buka Kunci Layar Kiosk", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Masukkan 4 angka PIN yang telah Anda tentukan saat mengunci layar.",
                        fontSize = 13.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) inputPin = it },
                        label = { Text("PIN 4-Digit") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pinError ?: "",
                            color = ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPin == activePin) {
                            releaseExamKiosk()
                            isKioskLocked = false
                            activePin = ""
                            showUnlockPinDialog = false
                        } else {
                            pinError = "PIN salah. Silakan coba lagi atau gunakan Remote Emergency Unlock dari Web Dashboard."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Buka Kunci")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockPinDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Attendance Sheet Dialog
    if (isAttendeeSheetVisible) {
        AlertDialog(
            onDismissRequest = { isAttendeeSheetVisible = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Daftar Hadir (${attendees.size})", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { isAttendeeSheetVisible = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }
            },
            text = {
                if (attendees.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada siswa yang melakukan scan presensi.",
                            color = SlateGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attendees.forEachIndexed { index, att ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceVariantColor,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${att.namaPeserta ?: att.nomorInduk ?: "Siswa"}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkNavy
                                        )
                                        Text(
                                            text = "NIS: ${att.nomorInduk ?: "-"} • ${att.waktuAbsen ?: "-"}",
                                            fontSize = 11.sp,
                                            color = SlateGray
                                        )
                                    }

                                    Surface(
                                        color = if (att.status == "HADIR") PrimaryTealContainer else Color(0xFFFDE8E8),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = att.status ?: "HADIR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (att.status == "HADIR") OnPrimaryTealContainer else ErrorRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isAttendeeSheetVisible = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}
