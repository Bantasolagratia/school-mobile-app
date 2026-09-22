package com.sch.sekolah_mobile_app.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.AuthRepository
import com.sch.sekolah_mobile_app.data.storage.getPlatformStorage
import com.sch.sekolah_mobile_app.ui.theme.*

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    profile: UserProfileResponse?,
    onLogout: () -> Unit
) {
    val platformStorage = remember { getPlatformStorage() }
    var isSensitiveInfoMasked by remember {
        mutableStateOf(platformStorage.getString("pref_mask_sensitive_info", "false") == "true")
    }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val displayName = profile?.displayName ?: "Siswa"
    val email = profile?.email ?: "wrenley@murid.sekolah.com"
    val nis = profile?.nis ?: "202610012"
    val displayDetail = profile?.displayDetail ?: "Kelas 10-C"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryTealContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.take(2).uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OnPrimaryTealContainer
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = displayName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkNavy
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            color = AccentAmberContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "ROLE: SISWA / MURID",
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
                    text = "Data Akademik Siswa",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )
                Spacer(modifier = Modifier.height(12.dp))

                ProfileInfoRow(
                    icon = Icons.Default.Badge,
                    label = "Nomor Induk Siswa (NIS)",
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
                                contentDescription = if (isSensitiveInfoMasked) "Tampilkan NIS" else "Sembunyikan NIS",
                                tint = SlateGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderStrokeColor)
                ProfileInfoRow(icon = Icons.Default.Email, label = "Email Akun", value = email)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderStrokeColor)
                ProfileInfoRow(icon = Icons.Default.Class, label = "Kelas / Rombel", value = displayDetail)
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
            text = { Text("Apakah Anda yakin ingin keluar dari akun murid ini?") },
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

