package com.sch.sekolah_mobile_app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.ExamCardMobile
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.RemoteConfigManager
import com.sch.sekolah_mobile_app.data.repository.UjianRepository
import com.sch.sekolah_mobile_app.data.storage.getPlatformStorage
import com.sch.sekolah_mobile_app.ui.theme.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun HomeScreen(
    profile: UserProfileResponse?,
    onNavigateToGuru: () -> Unit,
    onNavigateToUjian: () -> Unit = {},
    onNavigateToExamHistory: () -> Unit = {},
    onNavigateToMapel: () -> Unit = {},
    onNavigateToRaport: () -> Unit = {},
    onNavigateToJadwal: () -> Unit = {},
    onNavigateToTeacherJadwal: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToIzin: () -> Unit = {},
    unreadNotifCount: Long = 0,
    ujianRepository: UjianRepository? = null
) {
    val platformStorage = remember { getPlatformStorage() }
    var isSensitiveInfoMasked by remember {
        mutableStateOf(platformStorage.getString("pref_mask_sensitive_info", "false") == "true")
    }
    val isRaportModuleEnabled by RemoteConfigManager.instance.isRaportModuleEnabled.collectAsState()
    val isGuru = profile?.isRoleGuru == true
    val displayName = profile?.displayName ?: if (isGuru) "Guru" else "Siswa"
    val displayDetail = profile?.displayDetail ?: if (isGuru) "Dewan Guru" else "Kelas 10-B"
    val nis = profile?.nis ?: "202610012"
    val nip = profile?.nip ?: "-"

    // State Kartu Ujian (Flip Card)
    var isCardFlipped by remember { mutableStateOf(false) }
    var examCard by remember { mutableStateOf<ExamCardMobile?>(null) }
    var isLoadingExamCard by remember { mutableStateOf(false) }

    LaunchedEffect(profile?.nis) {
        if (!isGuru && ujianRepository != null) {
            try {
                isLoadingExamCard = true
                examCard = ujianRepository.getMyExamCard(profile?.nis)
            } catch (_: Exception) {}
            finally {
                isLoadingExamCard = false
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Welcome Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGuru) "Halo, $displayName 👨‍🏫" else "Halo, $displayName 👋",
                        fontSize = if (isWideScreen) 25.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isGuru) "Portal Akademik & Administrasi Guru" else "Portal Pembelajaran & Siswa",
                        fontSize = 12.5.sp,
                        color = SlateGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge(
                                        containerColor = ErrorRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadNotifCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Pusat Notifikasi",
                                tint = DarkNavy,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryTealContainer,
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.25f)),
                        shadowElevation = 1.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGuru) Icons.Default.PersonOutline else Icons.Default.School,
                                contentDescription = null,
                                tint = PrimaryTealDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Student Identity Card (Flip Card)
            // HANYA siswa resmi yang memiliki alokasi valid dan BUKAN blacklist yang memiliki kartu peserta ujian & tombol flip
            val canFlipToExamCard = !isGuru && examCard?.isExamDay == true && examCard?.eligible == true && examCard?.isBlacklisted != true

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                if (!isCardFlipped || !canFlipToExamCard) {
                    // Sisi Depan: Kartu Pelajar Digital / Kartu Identitas Guru
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(PrimaryTeal, PrimaryTealDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isGuru) "KARTU IDENTITAS GURU" else "KARTU PELAJAR DIGITAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Tombol Flip Kartu Peserta Ujian HANYA muncul jika peserta resmi (eligible) dan tidak dicekal
                                    if (canFlipToExamCard) {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.22f),
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable { isCardFlipped = true }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = "Lihat Kartu Ujian",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        color = if (!isGuru && examCard?.isBlacklisted == true) Color(0xFFEF4444) else AccentAmber,
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = when {
                                                isGuru -> "GURU AKTIF"
                                                examCard?.isBlacklisted == true -> "DICEKAL DARI UJIAN"
                                                else -> "MURID AKTIF"
                                            },
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isGuru && examCard?.isBlacklisted == true) Color.White else DarkNavy,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = displayName,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.2).sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isGuru) "NIP" else "NIS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isSensitiveInfoMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isSensitiveInfoMasked) "Tampilkan" else "Sembunyikan",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clickable {
                                                    isSensitiveInfoMasked = !isSensitiveInfoMasked
                                                    platformStorage.setString("pref_mask_sensitive_info", isSensitiveInfoMasked.toString())
                                                }
                                        )
                                    }
                                    Text(
                                        text = if (isSensitiveInfoMasked) "****" else (if (isGuru) nip else nis),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Column {
                                    Text(if (isGuru) "JABATAN" else "KELAS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(displayDetail, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                                Column {
                                    Text("SEMESTER", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text("Ganjil 2026/2027", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    // Sisi Belakang: Kartu Peserta Ujian Resmi (Flip Card)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF115E59)))
                            )
                            .padding(20.dp)
                    ) {
                        Column {
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
                                        modifier = Modifier.size(16.dp)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.22f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { isCardFlipped = false }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "Balik ke Kartu Pelajar",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = AccentAmber,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "PESERTA RESMI",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = examCard?.nama ?: displayName,
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
                                    Text("NIS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = examCard?.nis?.toString() ?: nis,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Column {
                                    Text("KELAS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = examCard?.kelas ?: displayDetail,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Column {
                                    Text("RUANGAN", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = examCard?.ruangan ?: "-",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("POSISI", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    Surface(
                                        color = AccentAmber,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = examCard?.posisi ?: "-",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DarkNavy,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("UJIAN", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = examCard?.judulUjian ?: examCard?.kategori ?: "Ujian Semester",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("SEMESTER", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = examCard?.semester ?: "Ganjil 2026/2027",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Alert Pencekalan Ujian (Blacklist) untuk Siswa
            if (!isGuru && examCard?.isBlacklisted == true) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Akses Ujian Dicekal (Blacklist)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = examCard?.alasan?.let { "Alasan pencekalan: $it." }
                                    ?: "Siswa terdaftar dalam daftar pencekalan ujian sekolah.",
                                fontSize = 11.sp,
                                color = Color(0xFFB91C1C),
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "Kartu peserta ujian tidak diterbitkan untuk siswa yang dicekal.",
                                fontSize = 10.sp,
                                color = Color(0xFF7F1D1D),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Menu Akademik & Layanan",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isGuru) {
                // Modul Guru: Menu paling atas adalah Kalender
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToJadwal() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(PrimaryTealContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Kalender",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Kalender",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = PrimaryTealContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Akademik",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnPrimaryTealContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Agenda kegiatan sekolah, kalender akademik & jadwal penting",
                                fontSize = 12.sp,
                                color = SlateGray
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Buka",
                            tint = PrimaryTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Menu Baru: Jadwal (Mengajar, Pengawas & QR Kiosk)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTeacherJadwal() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Jadwal",
                                tint = Color(0xFF6D28D9),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Jadwal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFFEDE9FE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "KBM & Pengawas",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6D28D9),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Jadwal mengajar, tugas pengawas ujian & presensi QR Kiosk",
                                fontSize = 12.sp,
                                color = SlateGray
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Buka",
                            tint = Color(0xFF6D28D9)
                        )
                    }
                }
            } else {
                // Siswa: Main Featured Card Direktori Guru
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToGuru() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(PrimaryTealContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Direktori Guru",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Direktori Guru",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = PrimaryTealContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Utama",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnPrimaryTealContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Akses kontak WhatsApp, telepon, dan daftar pengajar sekolah",
                                fontSize = 12.sp,
                                color = SlateGray
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Buka",
                            tint = PrimaryTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Additional feature cards untuk Siswa (responsive 2-column if tablet/wide screen)
                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        QuickInfoCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = "Mata Pelajaran",
                            subtitle = "Bahan ajar & materi bacaan",
                            onClick = onNavigateToMapel
                        )
                        QuickInfoCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CalendarToday,
                            title = "Kalender",
                            subtitle = "Agenda kelas, ujian & kegiatan",
                            onClick = onNavigateToJadwal
                        )
                        QuickInfoCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            title = "Tugas & Ujian",
                            subtitle = "Status pengerjaan & jadwal ujian",
                            onClick = onNavigateToUjian
                        )
                        QuickInfoCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.History,
                            title = "Riwayat Ujian",
                            subtitle = "Histori & review nilai",
                            onClick = onNavigateToExamHistory
                        )
                        QuickInfoCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Description,
                            title = "Surat Izin",
                            subtitle = "Pengajuan & riwayat izin",
                            onClick = onNavigateToIzin
                        )
                        if (isRaportModuleEnabled) {
                            QuickInfoCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Assessment,
                                title = "Rapor Semester",
                                subtitle = "Capaian nilai & presensi",
                                onClick = onNavigateToRaport
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickInfoCard(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = "Mata Pelajaran",
                            subtitle = "Bahan ajar & materi bacaan",
                            onClick = onNavigateToMapel
                        )
                        QuickInfoCard(
                            icon = Icons.Default.CalendarToday,
                            title = "Kalender",
                            subtitle = "Agenda kelas, ujian & kegiatan",
                            onClick = onNavigateToJadwal
                        )
                        QuickInfoCard(
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            title = "Tugas & Ujian",
                            subtitle = "Status pengerjaan & jadwal ujian",
                            onClick = onNavigateToUjian
                        )
                        QuickInfoCard(
                            icon = Icons.Default.History,
                            title = "Riwayat Ujian & Nilai",
                            subtitle = "Lihat histori ujian & review pembahasan",
                            onClick = onNavigateToExamHistory
                        )
                        QuickInfoCard(
                            icon = Icons.Default.Description,
                            title = "Surat Izin",
                            subtitle = "Pengajuan & riwayat izin siswa",
                            onClick = onNavigateToIzin
                        )
                        AnimatedVisibility(
                            visible = isRaportModuleEnabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            QuickInfoCard(
                                icon = Icons.Default.Assessment,
                                title = "Rapor Semester",
                                subtitle = "Capaian nilai & presensi",
                                onClick = onNavigateToRaport
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Announcement Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor),
                border = BorderStroke(1.dp, BorderStrokeColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isGuru) "Pusat Jadwal & Presensi Guru" else "Pengumuman Akademik",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isGuru)
                                "Gunakan menu Jadwal untuk melihat penugasan mengajar atau pengawas ujian, dan aktifkan QR presensi Kiosk saat sesi dimulai."
                            else
                                "Gunakan menu Direktori Guru untuk menghubungi wali kelas atau guru mata pelajaran terkait konsultasi belajar.",
                            fontSize = 12.sp,
                            color = SlateGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickInfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Card(
        modifier = clickableModifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderStrokeColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = PrimaryTealContainer,
                border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.18f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = PrimaryTealDark, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkNavy)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 11.5.sp, color = SlateGray, lineHeight = 15.sp)
            }
            if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Buka",
                    tint = SlateLight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
