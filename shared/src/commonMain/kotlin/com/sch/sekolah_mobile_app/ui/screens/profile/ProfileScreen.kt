package com.sch.sekolah_mobile_app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.AuthRepository
import com.sch.sekolah_mobile_app.data.storage.getPlatformStorage
import com.sch.sekolah_mobile_app.ui.screens.izin.rememberPlatformFilePicker
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    profile: UserProfileResponse?,
    onProfileUpdated: (UserProfileResponse) -> Unit = {},
    onLogout: () -> Unit
) {
    val platformStorage = remember { getPlatformStorage() }
    val coroutineScope = rememberCoroutineScope()

    var effectiveProfile by remember(profile) { mutableStateOf(profile) }
    var isSensitiveInfoMasked by remember {
        mutableStateOf(platformStorage.getString("pref_mask_sensitive_info", "false") == "true")
    }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var photoFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isPhotoFeedbackError by remember { mutableStateOf(false) }
    var isPhotoPendingApproval by remember { mutableStateOf(false) }

    val displayName = effectiveProfile?.displayName ?: "Siswa"
    val username = effectiveProfile?.effectiveUsername?.takeIf { it.isNotEmpty() } ?: "wrenley"
    val nis = effectiveProfile?.nis ?: "202610012"
    val displayDetail = effectiveProfile?.displayDetail ?: "Kelas 10-C"
    val isMurid = effectiveProfile?.isRoleMurid == true && nis != "-"
    val fotoUrl = effectiveProfile?.fotoUrl

    // Segarkan profil dari server saat membuka layar profil
    LaunchedEffect(Unit) {
        try {
            val fresh = authRepository.refreshProfile()
            effectiveProfile = fresh
            onProfileUpdated(fresh)
        } catch (_: Exception) {}
    }

    // Muat gambar foto profil aktif jika tersedia
    LaunchedEffect(fotoUrl) {
        if (!fotoUrl.isNullOrBlank()) {
            try {
                val bytes = authRepository.apiClient.fetchImageBytes(fotoUrl, authRepository.getAccessToken())
                if (bytes.isNotEmpty()) {
                    avatarBitmap = bytes.decodeToImageBitmap()
                }
            } catch (_: Exception) {
                avatarBitmap = null
            }
        } else {
            avatarBitmap = null
        }
    }

    val launchPhotoPicker = rememberPlatformFilePicker(
        onFileSelected = { selectedFile ->
            if (!selectedFile.mimeType.startsWith("image/")) {
                isPhotoFeedbackError = true
                photoFeedbackMessage = "Format file tidak didukung. Pilih foto berformat JPG, PNG, atau WEBP."
                return@rememberPlatformFilePicker
            }
            if (selectedFile.sizeBytes > 5 * 1024 * 1024) {
                isPhotoFeedbackError = true
                photoFeedbackMessage = "Ukuran foto melebihi batas maksimal 5 MB."
                return@rememberPlatformFilePicker
            }
            coroutineScope.launch {
                isUploadingPhoto = true
                photoFeedbackMessage = null
                isPhotoFeedbackError = false
                try {
                    val (res, updatedProfile) = authRepository.uploadStudentPhoto(nis, selectedFile)
                    effectiveProfile = updatedProfile
                    onProfileUpdated(updatedProfile)
                    val pending = res.requiresApproval || res.status.equals("PENDING", ignoreCase = true)
                    isPhotoPendingApproval = pending
                    isPhotoFeedbackError = false
                    if (!pending) {
                        // Tampilkan langsung bitmap yang baru diunggah
                        try {
                            avatarBitmap = selectedFile.bytes.decodeToImageBitmap()
                        } catch (_: Exception) {}
                    }
                    photoFeedbackMessage = res.message ?: if (pending) {
                        "Pembaruan foto berhasil diajukan dan sedang menunggu persetujuan Administrator."
                    } else {
                        "Foto profil berhasil disimpan dan diterapkan."
                    }
                } catch (e: Exception) {
                    isPhotoFeedbackError = true
                    photoFeedbackMessage = e.message ?: "Gagal mengunggah foto profil."
                } finally {
                    isUploadingPhoto = false
                }
            }
        },
        onError = { errMsg ->
            isPhotoFeedbackError = true
            photoFeedbackMessage = errMsg
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Avatar + Tombol Kamera (untuk Murid)
        Box(
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(PrimaryTealContainer)
                    .border(2.dp, PrimaryTeal.copy(alpha = 0.25f), CircleShape)
                    .let { mod ->
                        if (isMurid && !isUploadingPhoto) {
                            mod.clickable { launchPhotoPicker() }
                        } else {
                            mod
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val bmp = avatarBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "Foto Profil $displayName",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        text = displayName.take(2).uppercase(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimaryTealContainer
                    )
                }

                if (isUploadingPhoto) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            if (isMurid) {
                Surface(
                    onClick = { if (!isUploadingPhoto) launchPhotoPicker() },
                    shape = CircleShape,
                    color = PrimaryTeal,
                    shadowElevation = 3.dp,
                    border = BorderStroke(2.dp, Color.White),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Unggah / Ganti Foto Profil",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (isMurid) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { if (!isUploadingPhoto) launchPhotoPicker() },
                enabled = !isUploadingPhoto,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isUploadingPhoto) "Mengunggah foto..."
                    else if (avatarBitmap != null || !fotoUrl.isNullOrBlank()) "Ganti Foto Profil"
                    else "Unggah Foto Profil",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal
                )
            }
        }

        if (!photoFeedbackMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            val bgColor = when {
                isPhotoFeedbackError -> Color(0xFFFEE2E2)
                isPhotoPendingApproval -> Color(0xFFFEF3C7)
                else -> Color(0xFFD1FAE5)
            }
            val textColor = when {
                isPhotoFeedbackError -> ErrorRed
                isPhotoPendingApproval -> Color(0xFF92400E)
                else -> Color(0xFF065F46)
            }
            Surface(
                color = bgColor,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            isPhotoFeedbackError -> Icons.Default.ErrorOutline
                            isPhotoPendingApproval -> Icons.Default.HourglassTop
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = photoFeedbackMessage ?: "",
                        fontSize = 12.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = displayName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkNavy,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            color = AccentAmberContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            val roleText = when {
                effectiveProfile?.isAdmin == true -> "ROLE: ADMINISTRATOR"
                effectiveProfile?.isRoleGuru == true -> "ROLE: GURU / TENAGA PENDIDIK"
                else -> "ROLE: SISWA / MURID"
            }
            Text(
                text = roleText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Information Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Data Akun Pengguna",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )
                Spacer(modifier = Modifier.height(12.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Badge,
                    label = if (effectiveProfile?.isRoleGuru == true) "Nomor Induk Pegawai (NIP)" else "Nomor Induk Siswa (NIS)",
                    value = if (isSensitiveInfoMasked) "****" else nis,
                    trailingAction = {
                        IconButton(
                            onClick = {
                                isSensitiveInfoMasked = !isSensitiveInfoMasked
                                platformStorage.setString("pref_mask_sensitive_info", isSensitiveInfoMasked.toString())
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isSensitiveInfoMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isSensitiveInfoMasked) "Tampilkan" else "Sembunyikan",
                                tint = SlateGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderStrokeColor)
                ProfileInfoRow(icon = Icons.Default.Person, label = "Username", value = username)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderStrokeColor)
                ProfileInfoRow(
                    icon = Icons.Default.Class,
                    label = if (effectiveProfile?.isRoleGuru == true) "Jabatan / Keterangan" else "Kelas / Rombel",
                    value = displayDetail
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderStrokeColor)
                ProfileInfoRow(icon = Icons.Default.CheckCircle, label = "Status", value = "Aktif Terdaftar")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Keluar",
                tint = ErrorRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Keluar dari Akun",
                color = ErrorRed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Keluar") },
            text = { Text("Apakah Anda yakin ingin keluar dari akun ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authRepository.logout()
                        onLogout()
                    }
                ) {
                    Text("Ya, Keluar", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryTeal,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = SlateGray)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkNavy)
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}

