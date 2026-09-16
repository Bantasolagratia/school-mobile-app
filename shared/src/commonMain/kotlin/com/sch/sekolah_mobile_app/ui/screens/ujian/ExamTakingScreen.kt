package com.sch.sekolah_mobile_app.ui.screens.ujian

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.ExamScheduleItem
import com.sch.sekolah_mobile_app.data.model.UjianDetailMobile
import com.sch.sekolah_mobile_app.data.model.UjianQuestionMobile
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.UjianRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExamTakingScreen(
    exam: ExamScheduleItem,
    ujianRepository: UjianRepository,
    profile: UserProfileResponse?,
    onExamSubmitted: () -> Unit,
    onEmergencyExit: () -> Unit,
    onKickedBySupervisor: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val studentKelas = profile?.displayDetail ?: "Kelas 10-B"

    // Intercept hardware/gesture back button (Fasilitas 7.2)
    var showBackBlockedDialog by remember { mutableStateOf(false) }
    ExamBackHandler(enabled = true) {
        showBackBlockedDialog = true
    }

    // Fasilitas 7.1 & 7.2: Kiosk Lock & App Switching Violation Detection
    var violationCount by remember { mutableStateOf(0) }
    var showViolationDialog by remember { mutableStateOf(false) }
    var isSecurityLockedByViolation by remember { mutableStateOf(false) }
    var violationKeyInput by remember { mutableStateOf("") }
    var isVerifyingViolationKey by remember { mutableStateOf(false) }
    var violationKeyError by remember { mutableStateOf<String?>(null) }

    // Exam Questions & State
    var questions by remember { mutableStateOf<List<UjianQuestionMobile>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var studentAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isLoadingQuestions by remember { mutableStateOf(true) }

    // Heartbeat & Security Lock State
    var isKickedBySupervisor by remember { mutableStateOf(false) }
    var kickMessage by remember { mutableStateOf("") }
    var isAlreadyFinished by remember { mutableStateOf(false) }
    var alreadyFinishedMessage by remember { mutableStateOf("") }

    // Emergency Exit Key Dialog State (Fasilitas 7.3)
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var emergencyKeyInput by remember { mutableStateOf("") }
    var isVerifyingKey by remember { mutableStateOf(false) }
    var emergencyKeyError by remember { mutableStateOf<String?>(null) }

    ExamKioskEffect(enabled = !isKickedBySupervisor && !isAlreadyFinished) { _ ->
        violationCount++
        showViolationDialog = true
        if (violationCount >= 2) {
            isSecurityLockedByViolation = true
        }
        coroutineScope.launch {
            try {
                ujianRepository.sendHeartbeat(
                    scheduleId = exam.id,
                    status = if (violationCount >= 2) "TERKUNCI" else "MENGERJAKAN",
                    keterangan = "Terdeteksi mencoba berpindah aplikasi / minimize (Pelanggaran ke-$violationCount)"
                )
            } catch (_: Exception) {}
        }
    }

    // Finish Exam Dialog
    var showFinishConfirmation by remember { mutableStateOf(false) }

    // Timer Countdown (Default 90 menit atau disesuaikan)
    var remainingSeconds by remember { mutableStateOf(90 * 60) }

    // Initial load: Fetch questions and lock session on server
    LaunchedEffect(exam.id) {
        // 1. Lock session on server (single hit exam-start, replaces periodic heartbeat)
        try {
            val hb = ujianRepository.examStart(
                scheduleId = exam.id,
                keterangan = "Ujian sedang berlangsung di perangkat mobile"
            )
            if (!hb.active) {
                if (hb.action == "ALREADY_FINISHED") {
                    isAlreadyFinished = true
                    alreadyFinishedMessage = hb.message ?: "Anda telah menyelesaikan dan mengumpulkan ujian ini."
                } else if (hb.action == "KICK") {
                    isKickedBySupervisor = true
                    kickMessage = hb.message ?: "Sesi Anda telah di-reset oleh Guru Pengawas."
                }
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("menyelesaikan", ignoreCase = true) || msg.contains("ALREADY_FINISHED", ignoreCase = true)) {
                isAlreadyFinished = true
                alreadyFinishedMessage = msg
            }
        }

        // 2. Fetch exam questions
        try {
            if (exam.ujianId != null) {
                val detail = ujianRepository.getUjianDetail(exam.ujianId)
                if (detail.questions.isNotEmpty()) {
                    questions = detail.questions
                } else {
                    questions = generateFallbackQuestions(exam)
                }
            } else {
                questions = generateFallbackQuestions(exam)
            }
        } catch (_: Exception) {
            questions = generateFallbackQuestions(exam)
        } finally {
            isLoadingQuestions = false
        }
    }

    // Periodic heartbeat removed — replaced by single-hit exam-start/exam-exit endpoints

    // Timer Countdown
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    val formattedTime = remember(remainingSeconds) {
        val hours = remainingSeconds / 3600
        val mins = (remainingSeconds % 3600) / 60
        val secs = remainingSeconds % 60
        "${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }

    // Main Full-Screen Layout (Kiosk Mode)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Kiosk Header (High Security Banner)
            Surface(
                color = DarkNavy,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = PrimaryTeal,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = exam.kodeMapel ?: "UJIAN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = exam.judulUjian ?: "Pengerjaan Ujian",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pengawas: ${exam.displaySupervisor} | ${exam.displayRuangan}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // Timer Badge
                        Surface(
                            color = if (remainingSeconds < 300) Color(0xFFEF4444) else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formattedTime,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Security Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mode Aman Aktif",
                                fontSize = 11.sp,
                                color = PrimaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Tombol Kunci Keluar Darurat (Fasilitas 7.3)
                        OutlinedButton(
                            onClick = {
                                emergencyKeyInput = ""
                                emergencyKeyError = null
                                showEmergencyDialog = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF87171)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444))
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kunci Keluar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Questions Number Palette
            if (questions.isNotEmpty()) {
                Surface(
                    color = CardSurface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(questions) { index, _ ->
                            val isAnswered = studentAnswers.containsKey(index)
                            val isCurrent = index == currentIndex

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isCurrent -> PrimaryTeal
                                            isAnswered -> PrimaryTealContainer
                                            else -> SurfaceVariantColor
                                        }
                                    )
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = if (isCurrent) DarkNavy else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { currentIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isCurrent -> Color.White
                                        isAnswered -> OnPrimaryTealContainer
                                        else -> SlateGray
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Question Content Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when {
                    isLoadingQuestions -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryTeal)
                        }
                    }

                    questions.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada soal yang tersedia.", color = SlateGray)
                        }
                    }

                    else -> {
                        val q = questions[currentIndex]
                        val currentAnswer = studentAnswers[currentIndex] ?: ""

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Question header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Soal Nomor ${currentIndex + 1} dari ${questions.size}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Surface(
                                    color = SurfaceVariantColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    val typeLabel = when {
                                        "ESSAI".equals(q.type, ignoreCase = true) -> "SOAL ISIAN"
                                        "URAIAN".equals(q.type, ignoreCase = true) -> "SOAL URAIAN"
                                        else -> (q.type ?: "PILIHAN GANDA").replace("_", " ")
                                    }
                                    Text(
                                        text = typeLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SlateGray,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Pertanyaan (bagian #blank# atau #blank direplace menjadi garis bawah _)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                val rawPertanyaan = q.pertanyaan ?: "Pertanyaan tidak memiliki teks."
                                val formattedPertanyaan = rawPertanyaan.replace(Regex("#blank#?", RegexOption.IGNORE_CASE), "_____")
                                Text(
                                    text = formattedPertanyaan,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = DarkNavy,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Pilihan Jawaban
                            if ("PILIHAN_GANDA".equals(q.type, ignoreCase = true) || q.pilihan.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val options = if (q.pilihan.isNotEmpty()) q.pilihan else listOf("A", "B", "C", "D")
                                    val labels = listOf("A", "B", "C", "D", "E")

                                    options.forEachIndexed { optIndex, optText ->
                                        val label = labels.getOrElse(optIndex) { "${optIndex + 1}" }
                                        val isSelected = currentAnswer == label || currentAnswer == optText

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    studentAnswers = studentAnswers + (currentIndex to label)
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) PrimaryTealContainer else CardSurface
                                            ),
                                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(PrimaryTeal),
                                                width = 2.dp
                                            ) else CardDefaults.outlinedCardBorder().copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(SurfaceVariantColor)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) PrimaryTeal else SurfaceVariantColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else DarkNavy
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = optText,
                                                    fontSize = 14.sp,
                                                    color = DarkNavy
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ESSAI (ISIAN) / URAIAN
                                val isIsian = "ESSAI".equals(q.type, ignoreCase = true)
                                Text(
                                    text = if (isIsian) "Jawaban Bagian Isian (_):" else "Tuliskan Jawaban Anda:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkNavy
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = currentAnswer,
                                    onValueChange = { newAnswer ->
                                        studentAnswers = studentAnswers + (currentIndex to newAnswer)
                                    },
                                    placeholder = {
                                        Text(if (isIsian) "Ketik kata isian untuk mengisi bagian (_) di sini..." else "Ketik jawaban lengkap di sini...")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(if (isIsian) 100.dp else 140.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            // Bottom Navigation Bar (Fluid & Responsive)
            Surface(
                color = CardSurface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Status Progress Ringkas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Soal ${currentIndex + 1} dari ${questions.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateGray
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryTealContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${studentAnswers.size}/${questions.size} Terjawab",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnPrimaryTealContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Aksi Navigasi Fluid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tombol Sebelumnya
                        if (currentIndex > 0) {
                            OutlinedButton(
                                onClick = { currentIndex-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = DarkNavy
                                ),
                                border = BorderStroke(1.dp, BorderStrokeColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sebelumnya",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Tombol Selanjutnya / Kumpulkan (Fluid flex-grow)
                        val isLastQuestion = currentIndex >= questions.size - 1
                        Button(
                            onClick = {
                                if (!isLastQuestion) {
                                    currentIndex++
                                } else {
                                    showFinishConfirmation = true
                                }
                            },
                            modifier = Modifier
                                .weight(if (currentIndex > 0) 1.25f else 1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isLastQuestion) PrimaryTeal else Color(0xFF059669)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (!isLastQuestion) {
                                Text(
                                    text = "Selanjutnya",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kumpulkan",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // DIALOGS & SECURITY LOCKS
        // -------------------------------------------------------------

        // 1. Alert Block Back Button (Fasilitas 7.2)
        if (showBackBlockedDialog) {
            AlertDialog(
                onDismissRequest = { showBackBlockedDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "Navigasi Terkunci",
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                },
                text = {
                    Text(
                        "Ujian berada dalam mode Layar Penuh Terkunci. Anda dilarang keluar, menekan tombol kembali, atau berpindah aplikasi selama ujian berlangsung.\n\nJika terjadi kendala darurat, gunakan tombol 'Kunci Keluar' dan hubungi Guru Pengawas.",
                        fontSize = 13.sp,
                        color = SlateGray,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showBackBlockedDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("Lanjutkan Ujian")
                    }
                }
            )
        }

        // 2. Kunci Keluar Darurat (Key Keluar) Dialog (Fasilitas 7.3)
        if (showEmergencyDialog) {
            AlertDialog(
                onDismissRequest = { if (!isVerifyingKey) showEmergencyDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        "Kunci Keluar Darurat",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = DarkNavy
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Hanya Guru Pengawas yang berwenang memasukkan kode token keluar darurat ini.",
                            fontSize = 12.sp,
                            color = SlateGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = emergencyKeyInput,
                            onValueChange = {
                                if (it.length <= 6) {
                                    emergencyKeyInput = it
                                    emergencyKeyError = null
                                }
                            },
                            label = { Text("6 Digit Key Keluar") },
                            placeholder = { Text("849 203") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                letterSpacing = 4.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            isError = emergencyKeyError != null
                        )

                        if (emergencyKeyError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = emergencyKeyError ?: "",
                                color = ErrorRed,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (emergencyKeyInput.trim().length < 6) {
                                emergencyKeyError = "Masukkan 6 digit kode token."
                                return@Button
                            }

                            isVerifyingKey = true
                            emergencyKeyError = null

                            coroutineScope.launch {
                                try {
                                    val resp = ujianRepository.verifyEmergencyExitKey(
                                        scheduleId = exam.id ?: "",
                                        kelas = studentKelas,
                                        key = emergencyKeyInput.trim()
                                    )

                                    isVerifyingKey = false
                                    if (resp.valid) {
                                        showEmergencyDialog = false
                                        releaseExamKiosk()
                                        onEmergencyExit()
                                    } else {
                                        emergencyKeyError = resp.message ?: "Key Keluar tidak sesuai atau tidak valid."
                                    }
                                } catch (e: Exception) {
                                    isVerifyingKey = false
                                    emergencyKeyError = e.message ?: "Gagal memverifikasi Key Keluar."
                                }
                            }
                        },
                        enabled = !isVerifyingKey && emergencyKeyInput.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        if (isVerifyingKey) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Buka Kunci Layar")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEmergencyDialog = false },
                        enabled = !isVerifyingKey
                    ) {
                        Text("Batal")
                    }
                }
            )
        }

        // 3. Notifikasi Sesi Di-reset / Kick oleh Guru Pengawas (Fasilitas 5 & 6.2)
        if (isKickedBySupervisor) {
            AlertDialog(
                onDismissRequest = {},
                icon = {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "Sesi Ujian Di-reset",
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                },
                text = {
                    Text(
                        text = if (kickMessage.isNotBlank()) kickMessage else "Guru Pengawas telah melakukan Reset Sesi / Kick untuk akun Anda. Anda sekarang dapat login kembali di perangkat baru.",
                        fontSize = 13.sp,
                        color = SlateGray,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            releaseExamKiosk()
                            onKickedBySupervisor()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("Keluar ke Login")
                    }
                }
            )
        }

        // 3b. Notifikasi Ujian Sudah Dikumpulkan / Selesai
        if (isAlreadyFinished) {
            AlertDialog(
                onDismissRequest = {},
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        "Ujian Sudah Selesai",
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                },
                text = {
                    Text(
                        text = if (alreadyFinishedMessage.isNotBlank()) alreadyFinishedMessage else "Anda telah mengumpulkan jawaban dan menyelesaikan sesi ujian ini. Ujian tidak dapat dikerjakan kembali.",
                        fontSize = 13.sp,
                        color = SlateGray,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            releaseExamKiosk()
                            onExamSubmitted()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("Kembali ke Beranda Ujian")
                    }
                }
            )
        }

        // 4. Konfirmasi Selesai Ujian
        if (showFinishConfirmation) {
            AlertDialog(
                onDismissRequest = { showFinishConfirmation = false },
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("Kumpulkan Jawaban Ujian?", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        text = "Anda telah menjawab ${studentAnswers.size} dari ${questions.size} soal. Pastikan semua soal telah diperiksa sebelum mengumpulkan ujian.",
                        fontSize = 13.sp,
                        color = SlateGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showFinishConfirmation = false
                            coroutineScope.launch {
                                try {
                                    ujianRepository.examExit(
                                        scheduleId = exam.id,
                                        status = "SELESAI",
                                        keterangan = "Ujian selesai dikumpulkan oleh murid"
                                    )
                                } catch (_: Exception) {}
                                releaseExamKiosk()
                                onExamSubmitted()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("Ya, Kumpulkan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishConfirmation = false }) {
                        Text("Periksa Lagi")
                    }
                }
            )
        }

        // 5. Peringatan Pelanggaran Berpindah Aplikasi (Fasilitas 7.2)
        if (showViolationDialog && !isSecurityLockedByViolation) {
            AlertDialog(
                onDismissRequest = { showViolationDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        "⚠️ Peringatan Keamanan Ujian!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Sistem mendeteksi percobaan keluar / meminimalkan layar ujian (Pelanggaran #$violationCount).",
                            fontWeight = FontWeight.SemiBold,
                            color = DarkNavy,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selama ujian berlangsung, Anda dilarang keras membuka aplikasi lain, jendela mengambang, maupun panel notifikasi. Percobaan ini telah dilaporkan ke dashboard Guru Pengawas.",
                            fontSize = 12.sp,
                            color = SlateGray,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PENTING: Jika terdeteksi berpindah aplikasi sekali lagi, modul ujian akan DIBEKUKAN / TERKUNCI TOTAL!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showViolationDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("Saya Mengerti & Lanjutkan Ujian")
                    }
                }
            )
        }

        // 6. Ujian Terkunci Total Karena Pelanggaran Berulang (Fasilitas 7.2 & 7.3)
        if (isSecurityLockedByViolation) {
            AlertDialog(
                onDismissRequest = {}, // Non-dismissible
                icon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(44.dp)
                    )
                },
                title = {
                    Text(
                        "🚨 UJIAN DIBEKUKAN!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Aplikasi mendeteksi perpindahan aplikasi berulang kali ($violationCount kali). Akses pengerjaan soal telah dikunci demi integritas ujian.",
                            fontSize = 13.sp,
                            color = DarkNavy,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Silakan panggil Guru Pengawas di ruangan untuk memasukkan Kunci Keluar Darurat (Key Pengawas 6-digit) agar modul ujian dapat dibuka kembali.",
                            fontSize = 12.sp,
                            color = SlateGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = violationKeyInput,
                            onValueChange = {
                                if (it.length <= 6) violationKeyInput = it
                                violationKeyError = null
                            },
                            label = { Text("Key Pengawas (6-digit)") },
                            placeholder = { Text("Contoh: 495713") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = violationKeyError != null,
                            supportingText = violationKeyError?.let { { Text(it, color = Color(0xFFDC2626)) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isVerifyingViolationKey = true
                                violationKeyError = null
                                try {
                                    val resp = ujianRepository.verifyEmergencyExitKey(
                                        scheduleId = exam.id ?: "",
                                        kelas = studentKelas,
                                        key = violationKeyInput.trim()
                                    )
                                    isVerifyingViolationKey = false
                                    if (resp.valid) {
                                        // Reset violation dan pulihkan akses ujian
                                        isSecurityLockedByViolation = false
                                        violationCount = 0
                                        showViolationDialog = false
                                        violationKeyInput = ""
                                        try {
                                            ujianRepository.sendHeartbeat(
                                                scheduleId = exam.id,
                                                status = "MENGERJAKAN",
                                                keterangan = "Kunci ujian dibuka kembali oleh Pengawas"
                                            )
                                        } catch (_: Exception) {}
                                    } else {
                                        violationKeyError = resp.message ?: "Key Pengawas salah atau tidak valid."
                                    }
                                } catch (e: Exception) {
                                    isVerifyingViolationKey = false
                                    violationKeyError = e.message ?: "Gagal memverifikasi Key Pengawas."
                                }
                            }
                        },
                        enabled = !isVerifyingViolationKey && violationKeyInput.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        if (isVerifyingViolationKey) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Buka Kunci Ujian")
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isVerifyingViolationKey = true
                                violationKeyError = null
                                try {
                                    val resp = ujianRepository.verifyEmergencyExitKey(
                                        scheduleId = exam.id ?: "",
                                        kelas = studentKelas,
                                        key = violationKeyInput.trim()
                                    )
                                    isVerifyingViolationKey = false
                                    if (resp.valid) {
                                        releaseExamKiosk()
                                        onEmergencyExit()
                                    } else {
                                        violationKeyError = resp.message ?: "Key Pengawas salah untuk keluar darurat."
                                    }
                                } catch (e: Exception) {
                                    isVerifyingViolationKey = false
                                    violationKeyError = e.message ?: "Gagal memverifikasi Key Pengawas."
                                }
                            }
                        },
                        enabled = !isVerifyingViolationKey && violationKeyInput.trim().isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Text("Keluar Darurat")
                    }
                }
            )
        }
    }
}

/**
 * Fallback questions jika bank soal belum dihubungkan atau offline
 */
private fun generateFallbackQuestions(exam: ExamScheduleItem): List<UjianQuestionMobile> {
    return listOf(
        UjianQuestionMobile(
            id = "q1",
            type = "PILIHAN_GANDA",
            urutan = 1,
            pertanyaan = "Komponen utama dalam arsitektur komputer yang berfungsi sebagai otak pemrosesan instruksi adalah...",
            pilihan = listOf("CPU (Central Processing Unit)", "RAM (Random Access Memory)", "Hard Disk Drive", "Power Supply Unit")
        ),
        UjianQuestionMobile(
            id = "q2",
            type = "PILIHAN_GANDA",
            urutan = 2,
            pertanyaan = "Struktur data manakah yang menerapkan prinsip FIFO (First In First Out)?",
            pilihan = listOf("Stack", "Queue", "Binary Tree", "Graph")
        ),
        UjianQuestionMobile(
            id = "q3",
            type = "PILIHAN_GANDA",
            urutan = 3,
            pertanyaan = "Protokol jaringan standar yang digunakan untuk mengamankan komunikasi web melalui enkripsi TLS/SSL adalah...",
            pilihan = listOf("HTTP", "FTP", "HTTPS", "SMTP")
        ),
        UjianQuestionMobile(
            id = "q4",
            type = "ESSAI",
            urutan = 4,
            pertanyaan = "Jelaskan perbedaan mendasar antara database relasional (SQL) dan database non-relasional (NoSQL) beserta contoh penggunaannya!"
        ),
        UjianQuestionMobile(
            id = "q5",
            type = "URAIAN",
            urutan = 5,
            pertanyaan = "Tuliskan langkah-langkah dalam menyusun algoritma pencarian biner (Binary Search) dan sebutkan kompleksitas waktunya!"
        )
    )
}

