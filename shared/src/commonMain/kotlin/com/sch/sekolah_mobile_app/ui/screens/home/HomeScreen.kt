package com.sch.sekolah_mobile_app.ui.screens.home

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
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.RemoteConfigManager
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
    onNavigateToNotifications: () -> Unit = {},
    unreadNotifCount: Long = 0
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
                        fontSize = if (isWideScreen) 26.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                    Text(
                        text = if (isGuru) "Selamat datang di Portal Dewan Guru" else "Selamat datang di Portal Siswa",
                        fontSize = 13.sp,
                        color = SlateGray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge(containerColor = ErrorRed) {
                                        Text("$unreadNotifCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Pusat Notifikasi",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryTealContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGuru) Icons.Default.PersonOutline else Icons.Default.School,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Student Identity Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
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
                            Surface(
                                color = AccentAmber,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isGuru) "GURU AKTIF" else "MURID AKTIF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Menu Akademik & Layanan",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Featured Card: Direktori Guru (Modul Guru)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToGuru() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
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

            // Additional feature cards (responsive 2-column if tablet/wide screen)
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
                        title = if (isGuru) "Jadwal Mengajar" else "Jadwal Pelajaran",
                        subtitle = if (isGuru) "Agenda kelas & QR KBM" else "Lihat agenda kelas mingguan",
                        onClick = onNavigateToJadwal
                    )
                    QuickInfoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        title = "Tugas & Ujian",
                        subtitle = "Status pengerjaan & jadwal ujian",
                        onClick = onNavigateToUjian
                    )
                    if (!isGuru) {
                        QuickInfoCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.History,
                            title = "Riwayat Ujian",
                            subtitle = "Histori & review nilai",
                            onClick = onNavigateToExamHistory
                        )
                    }
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
                        title = if (isGuru) "Jadwal Mengajar" else "Jadwal Pelajaran",
                        subtitle = if (isGuru) "Agenda kelas & QR KBM" else "Lihat agenda kelas mingguan",
                        onClick = onNavigateToJadwal
                    )
                    QuickInfoCard(
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        title = "Tugas & Ujian",
                        subtitle = "Status pengerjaan & jadwal ujian",
                        onClick = onNavigateToUjian
                    )
                    if (!isGuru) {
                        QuickInfoCard(
                            icon = Icons.Default.History,
                            title = "Riwayat Ujian & Nilai",
                            subtitle = "Lihat histori ujian & review pembahasan",
                            onClick = onNavigateToExamHistory
                        )
                    }
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

            Spacer(modifier = Modifier.height(20.dp))

            // Announcement Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor)
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
                            text = "Pengumuman Akademik",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Gunakan menu Direktori Guru untuk menghubungi wali kelas atau guru mata pelajaran terkait konsultasi belajar.",
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkNavy)
                Text(text = subtitle, fontSize = 11.sp, color = SlateGray)
            }
            if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Buka",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
