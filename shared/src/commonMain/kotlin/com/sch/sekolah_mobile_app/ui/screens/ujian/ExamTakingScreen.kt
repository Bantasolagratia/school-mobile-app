package com.sch.sekolah_mobile_app.ui.screens.ujian

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import com.sch.sekolah_mobile_app.data.model.ExamCardMobile
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
    var examDetail by remember { mutableStateOf<UjianDetailMobile?>(null) }
    var questions by remember { mutableStateOf<List<UjianQuestionMobile>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var studentAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var flaggedQuestions by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoadingQuestions by remember { mutableStateOf(true) }

    // Heartbeat & Security Lock State
    var isKickedBySupervisor by remember { mutableStateOf(false) }
    var kickMessage by remember { mutableStateOf("") }
    var isAlreadyFinished by remember { mutableStateOf(false) }
    var alreadyFinishedMessage by remember { mutableStateOf("") }

    // Kartu Tanda Peserta Ujian Dialog State
    var showExamCardDialog by remember { mutableStateOf(false) }
    var examCard by remember { mutableStateOf<ExamCardMobile?>(null) }

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
    var submitResultData by remember { mutableStateOf<com.sch.sekolah_mobile_app.data.model.SubmitExamResponseMobile?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSubmitSuccessDialog by remember { mutableStateOf(false) }

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
                examDetail = detail
                if (detail.questions.isNotEmpty()) {
                    questions = detail.questions
                } else {
                    questions = generateFallbackQuestions(exam)
                }
            } else {
                questions = generateFallbackQuestions(exam)
            }
        } catch (e: Exception) {
            println("[ExamTakingScreen] Failed to load exam detail: ${e.message}")
            e.printStackTrace()
            questions = generateFallbackQuestions(exam)
        } finally {
            isLoadingQuestions = false
        }

        // 3. Fetch exam participant card (for supervisor inspection anytime during kiosk)
        try {
            examCard = ujianRepository.getMyExamCard(profile?.nis)
        } catch (_: Exception) {}
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

                        // Timer Badge with Multi-Tier Warning Colors
                        val timerBgColor = when {
                            remainingSeconds < 300 -> Color(0xFFEF4444) // Red Critical (< 5m)
                            remainingSeconds < 600 -> Color(0xFFD97706) // Amber Warning (< 10m)
                            else -> Color.White.copy(alpha = 0.15f)
                        }
                        Surface(
                            color = timerBgColor,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (remainingSeconds < 600) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                            )
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

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Tombol Kartu Ujian (Dapat dipanggil kapanpun tanpa mengganggu ujian)
                            // HANYA muncul pada ujian massal (UTS / UAS) di hari H
                            if (exam.isMassExam && (examCard?.isExamDay == true || examCard?.eligible == true)) {
                                Button(
                                    onClick = { showExamCardDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Badge,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Kartu Ujian",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Bar Sisa Waktu Ujian
                    val timeProgress = (remainingSeconds.toFloat() / (90 * 60)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { timeProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = when {
                            remainingSeconds < 300 -> Color(0xFFEF4444)
                            remainingSeconds < 600 -> Color(0xFFF59E0B)
                            else -> PrimaryTeal
                        },
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
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
                            val isFlagged = flaggedQuestions.contains(index)
                            val isCurrent = index == currentIndex

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isCurrent -> PrimaryTeal
                                            isFlagged -> Color(0xFFFEF3C7)
                                            isAnswered -> PrimaryTealContainer
                                            else -> SurfaceVariantColor
                                        }
                                    )
                                    .border(
                                        width = if (isCurrent) 2.dp else if (isFlagged) 1.5.dp else 1.dp,
                                        color = when {
                                            isCurrent -> DarkNavy
                                            isFlagged -> Color(0xFFF59E0B)
                                            isAnswered -> PrimaryTeal.copy(alpha = 0.35f)
                                            else -> BorderStrokeColor
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { currentIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent || isFlagged) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isCurrent -> Color.White
                                        isFlagged -> Color(0xFFB45309)
                                        isAnswered -> OnPrimaryTealContainer
                                        else -> SlateGray
                                    }
                                )
                                if (isFlagged && !isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(3.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B))
                                    )
                                }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Tombol Ragu-ragu (Flagged)
                                    val isCurrentFlagged = flaggedQuestions.contains(currentIndex)
                                    Surface(
                                        color = if (isCurrentFlagged) Color(0xFFFEF3C7) else SurfaceVariantColor,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCurrentFlagged) Color(0xFFF59E0B) else BorderStrokeColor
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                flaggedQuestions = if (isCurrentFlagged) {
                                                    flaggedQuestions - currentIndex
                                                } else {
                                                    flaggedQuestions + currentIndex
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = if (isCurrentFlagged) Color(0xFFB45309) else SlateGray,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (isCurrentFlagged) "Ragu-ragu" else "Ragu?",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isCurrentFlagged) Color(0xFFB45309) else SlateGray
                                            )
                                        }
                                    }

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
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Pertanyaan (bagian #blank# atau #blank direplace menjadi garis bawah _)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    val targetImageUrl = q.imageUrl?.ifBlank { null }
                                        ?: q.imageId?.let { id ->
                                            examDetail?.images?.find { it.id == id }?.url ?: "/api/ujian/images/$id"
                                        }

                                    // 1. Lampiran Gambar Soal (ditampilkan di bagian atas seperti standar CBT & web editor)
                                    if (!targetImageUrl.isNullOrBlank()) {
                                        ExamQuestionImageCard(
                                            imageUrl = targetImageUrl,
                                            caption = q.imageCaption,
                                            ujianRepository = ujianRepository
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }

                                    // 2. Resolusi Teks Pertanyaan
                                    // Antisipasi jika pertanyaan kosong/blank atau hanya label nomor seperti "Soal 1"
                                    val isDummyNumbering = q.pertanyaan?.trim()?.matches(Regex("^Soal\\s*\\d+$", RegexOption.IGNORE_CASE)) == true
                                    val rawPertanyaan = when {
                                        !q.pertanyaan.isNullOrBlank() && !isDummyNumbering -> q.pertanyaan
                                        !q.imageCaption.isNullOrBlank() && (q.pertanyaan.isNullOrBlank() || isDummyNumbering) -> q.imageCaption
                                        !q.pertanyaan.isNullOrBlank() -> q.pertanyaan
                                        else -> "Perhatikan lampiran gambar di atas untuk menjawab soal ini."
                                    }
                                    val formattedPertanyaan = rawPertanyaan.replace(Regex("#blank#?", RegexOption.IGNORE_CASE), "_____")

                                    Text(
                                        text = formattedPertanyaan,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkNavy
                                    )
                                }
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

        // 3c. Pop-up Kartu Tanda Peserta Ujian (Dapat dipanggil kapanpun oleh murid/pengawas tanpa mengganggu ujian)
        if (showExamCardDialog) {
            Dialog(
                onDismissRequest = { showExamCardDialog = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(PrimaryTeal, PrimaryTealDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "KARTU PESERTA UJIAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                            Surface(
                                color = AccentAmber,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "PESERTA RESMI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = examCard?.nama ?: (profile?.displayName ?: "Siswa"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("NIS", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    text = examCard?.nis?.toString() ?: (profile?.nis ?: "202610012"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text("KELAS", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    text = examCard?.kelas ?: (profile?.displayDetail ?: "Kelas 10-B"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text("RUANGAN", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    text = examCard?.ruangan ?: (exam.displayRuangan),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("POSISI", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                                Surface(
                                    color = AccentAmber,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = examCard?.posisi ?: "A1",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkNavy,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = Color.Black.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mode Kiosk Aman Aktif • Ujian: ${exam.judulUjian ?: "Ujian Semester"}",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showExamCardDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tutup & Lanjutkan Ujian",
                                color = DarkNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Konfirmasi Selesai Ujian
        if (showFinishConfirmation) {
            val answeredCount = studentAnswers.size
            val flaggedCount = flaggedQuestions.size
            val unAnsweredCount = (questions.size - answeredCount).coerceAtLeast(0)

            AlertDialog(
                onDismissRequest = { if (!isSubmitting) showFinishConfirmation = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        "Kumpulkan Jawaban Ujian?",
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pastikan seluruh soal telah diperiksa sebelum menyelesaikan pengerjaan.",
                            fontSize = 12.sp,
                            color = SlateGray,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 3 Summary Stat Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Terjawab
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "$answeredCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                    Text(text = "Dijawab", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
                                }
                            }
                            // Ragu-ragu
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "$flaggedCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                    Text(text = "Ragu-ragu", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                                }
                            }
                            // Belum
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "$unAnsweredCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(text = "Belum", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                }
                            }
                        }

                        if (unAnsweredCount > 0 || flaggedCount > 0) {
                            Surface(
                                color = Color(0xFFFFF7ED),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFC2410C),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Masih ada soal belum dijawab atau ragu-ragu.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9A3412),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isSubmitting,
                        onClick = {
                            isSubmitting = true
                            coroutineScope.launch {
                                var submitResp: com.sch.sekolah_mobile_app.data.model.SubmitExamResponseMobile? = null
                                try {
                                    val mappedAnswers = mutableMapOf<String, String>()
                                    questions.forEachIndexed { idx, q ->
                                        val ans = studentAnswers[idx]
                                        if (ans != null) {
                                            val key = q.id ?: (idx + 1).toString()
                                            mappedAnswers[key] = ans
                                            mappedAnswers[(idx + 1).toString()] = ans
                                        }
                                    }
                                    val elapsed = (90 * 60 - remainingSeconds).toLong().coerceAtLeast(0L)
                                    submitResp = ujianRepository.submitExam(
                                        scheduleId = exam.id ?: "",
                                        durationSeconds = elapsed,
                                        answers = mappedAnswers
                                    )
                                } catch (_: Exception) {
                                    try {
                                        ujianRepository.examExit(
                                            scheduleId = exam.id,
                                            status = "SELESAI",
                                            keterangan = "Ujian selesai dikumpulkan oleh murid"
                                        )
                                    } catch (_: Exception) {}
                                }
                                showFinishConfirmation = false
                                isSubmitting = false
                                submitResultData = submitResp
                                showSubmitSuccessDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isSubmitting) "Mengumpulkan..." else "Ya, Kumpulkan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        enabled = !isSubmitting,
                        onClick = { showFinishConfirmation = false },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderStrokeColor)
                    ) {
                        Text("Periksa Lagi", color = DarkNavy, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        // 4b. Dialog Hasil Pengumpulan Ujian (Hanya tampil saat submit dan jika tidak ada uraian)
        if (showSubmitSuccessDialog) {
            val hasEssay = submitResultData?.hasUngradedEssay == true || questions.any { it.type == "URAIAN" }
            AlertDialog(
                onDismissRequest = { /* Must click button to dismiss and leave */ },
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(44.dp)
                    )
                },
                title = {
                    Text(
                        "🎉 Ujian Berhasil Dikumpulkan!",
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (hasEssay) {
                            Text(
                                text = "Jawaban ujian Anda telah berhasil disimpan di sistem. Karena ujian ini memiliki soal bertipe Uraian, nilai akhir akan dipublikasikan setelah seluruh jawaban diperiksa dan dievaluasi oleh Guru Pengampu.",
                                fontSize = 13.sp,
                                color = SlateGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        } else {
                            Text(
                                text = "Jawaban ujian Anda telah berhasil dievaluasi oleh sistem.",
                                fontSize = 13.sp,
                                color = SlateGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "NILAI ANDA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${submitResultData?.score ?: 0.0}",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSubmitSuccessDialog = false
                            releaseExamKiosk()
                            onExamSubmitted()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Kembali ke Beranda", fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ExamQuestionImageCard(
    imageUrl: String,
    caption: String?,
    ujianRepository: UjianRepository
) {
    var imageBitmap by remember(imageUrl) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }
    var hasError by remember(imageUrl) { mutableStateOf(false) }
    var showZoomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        isLoading = true
        hasError = false
        try {
            val bytes = ujianRepository.fetchImageBytes(imageUrl)
            if (bytes.isNotEmpty()) {
                imageBitmap = bytes.decodeToImageBitmap()
            } else {
                hasError = true
            }
        } catch (_: Exception) {
            hasError = true
        } finally {
            isLoading = false
        }
    }

    Surface(
        color = SurfaceVariantColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderStrokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bmp = imageBitmap
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PrimaryTeal,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Memuat lampiran gambar...",
                                fontSize = 11.sp,
                                color = SlateGray
                            )
                        }
                    }
                }
                bmp != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showZoomDialog = true }
                    ) {
                        Image(
                            bitmap = bmp,
                            contentDescription = caption ?: "Gambar Lampiran Soal",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkNavy.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Perbesar",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Perbesar",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (!caption.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = caption,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = SlateGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = SlateLight,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Lampiran gambar tidak dapat dimuat",
                                fontSize = 12.sp,
                                color = SlateGray
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialog zoom gambar
    if (showZoomDialog && imageBitmap != null) {
        Dialog(onDismissRequest = { showZoomDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lampiran Gambar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkNavy
                        )
                        IconButton(
                            onClick = { showZoomDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Tutup",
                                tint = SlateGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = caption ?: "Gambar Lampiran Soal",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 420.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    if (!caption.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = caption,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = SlateGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}


