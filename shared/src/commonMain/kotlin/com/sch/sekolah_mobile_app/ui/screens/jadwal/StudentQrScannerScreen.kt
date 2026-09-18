package com.sch.sekolah_mobile_app.ui.screens.jadwal

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.AbsensiResultMobile
import com.sch.sekolah_mobile_app.data.model.Jadwal
import com.sch.sekolah_mobile_app.data.repository.JadwalRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQrScannerScreen(
    jadwal: Jadwal? = null,
    jadwalRepository: JadwalRepository,
    onNavigateBack: () -> Unit,
    onAttendanceSuccess: (AbsensiResultMobile) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<AbsensiResultMobile?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualTokenInput by remember { mutableStateOf("") }

    // Laser scanning line animation
    val infiniteTransition = rememberInfiniteTransition()
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    fun submitScan(token: String) {
        if (isProcessing) return
        val cleanToken = token.trim()
        if (cleanToken.isEmpty()) return

        isProcessing = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val result = jadwalRepository.scanQrCode(cleanToken)
                scanResult = result
                showSuccessDialog = true
                onAttendanceSuccess(result)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Gagal memproses presensi QR"
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Scan QR Presensi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (jadwal != null) {
                            Text(
                                text = jadwal.displayTitle,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    // Manual Token Input helper (Strictly NO photo gallery picker!)
                    IconButton(onClick = { showManualInputDialog = true }) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Input Kode Manual", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B0F19)),
            contentAlignment = Alignment.Center
        ) {
            // Live Hardware Camera Viewfinder (Strictly live camera, no gallery)
            PlatformCameraScanner(
                onQrDetected = { detectedCode ->
                    submitScan(detectedCode)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dark semi-transparent overlay surrounding the targeting box
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top guidance banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(vertical = 16.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Arahkan kamera ke Rolling QR yang ditampilkan di HP Guru atau layar proyektor kelas",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Center Aiming Reticle (Square)
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.dp, PrimaryTeal, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated laser line
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val currentY = size.height * scanLineY
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, PrimaryTealLight, PrimaryTeal, PrimaryTealLight, Color.Transparent)
                            ),
                            start = Offset(0f, currentY),
                            end = Offset(size.width, currentY),
                            strokeWidth = 4f
                        )
                    }

                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryTeal)
                        }
                    }
                }

                // Bottom instructions & Anti-Fraud security notice
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Anti-Fraud Security: Live Camera Only",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTealLight
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Pengunggahan foto dari galeri dinonaktifkan untuk menjamin kehadiran fisik siswa di dalam kelas.",
                        fontSize = 10.sp,
                        color = SlateLight,
                        textAlign = TextAlign.Center
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        val res = scanResult
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateBack()
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PrimaryTealContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Sukses",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Presensi Berhasil!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Kehadiran Anda telah diverifikasi dan dicatat ke sistem database sekolah.",
                        fontSize = 13.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceVariantColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Nama Siswa:", fontSize = 12.sp, color = SlateGray)
                                Text(
                                    res?.namaPeserta ?: "-",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("NIS:", fontSize = 12.sp, color = SlateGray)
                                Text(
                                    res?.nomorInduk ?: "-",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Status:", fontSize = 12.sp, color = SlateGray)
                                Text(
                                    res?.status ?: "HADIR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selesai")
                }
            }
        )
    }

    // Manual Input Dialog (For fallback testing in emulator without camera)
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = {
                Text("Input Token Presensi Manual", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Masukkan kode token presensi jika kamera perangkat Anda mengalami kendala:",
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualTokenInput,
                        onValueChange = { manualTokenInput = it },
                        label = { Text("Kode Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualInputDialog = false
                        submitScan(manualTokenInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Verifikasi Presensi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
