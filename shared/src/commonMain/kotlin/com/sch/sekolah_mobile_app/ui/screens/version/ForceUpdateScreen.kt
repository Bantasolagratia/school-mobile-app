package com.sch.sekolah_mobile_app.ui.screens.version

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.DecryptedAppVersionPayload
import com.sch.sekolah_mobile_app.data.security.ApkUrlCryptoHelper
import com.sch.sekolah_mobile_app.data.security.PlatformApkDownloader
import com.sch.sekolah_mobile_app.data.security.PlatformAppInfo
import com.sch.sekolah_mobile_app.data.security.PlatformAppInstaller
import com.sch.sekolah_mobile_app.ui.screens.ujian.ExamBackHandler
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

sealed class ApkDownloadState {
    object Idle : ApkDownloadState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : ApkDownloadState()
    data class Success(val apkPath: String) : ApkDownloadState()
    data class Error(val message: String) : ApkDownloadState()
    data class NeedsPermission(val apkPath: String) : ApkDownloadState()
}

@Composable
fun ForceUpdateScreen(
    payload: DecryptedAppVersionPayload? = null,
    tamperReason: String? = null,
    isMandatory: Boolean = true,
    onDismiss: ((dontShowAgain: Boolean) -> Unit)? = null,
    onRetry: () -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    // Intersep tombol back hardware:
    // Jika mandatory, back diblokir total.
    // Jika optional (!isMandatory), back akan menutup dialog sesuai status dontShowAgain.
    ExamBackHandler(enabled = true) {
        if (!isMandatory) {
            onDismiss?.invoke(dontShowAgain)
        }
    }

    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val isTampered = !tamperReason.isNullOrBlank()

    var downloadState by remember { mutableStateOf<ApkDownloadState>(ApkDownloadState.Idle) }

    fun formatBytesToMB(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = ((bytes.toDouble() / (1024.0 * 1024.0)) * 10.0).toLong() / 10.0
        return "$mb MB"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isMandatory) LightBackground else Color.Black.copy(alpha = 0.65f)
            )
            .then(
                if (!isMandatory) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismiss?.invoke(dontShowAgain)
                    }
                } else {
                    Modifier
                }
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Mencegah klik di dalam card menutup modal */ },
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isMandatory) 8.dp else 12.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Tombol Tutup 'X' di pojok kanan atas (hanya untuk update opsional)
                if (!isMandatory) {
                    IconButton(
                        onClick = { onDismiss?.invoke(dontShowAgain) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = SlateGray
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isTampered -> ErrorRed.copy(alpha = 0.12f)
                                    downloadState is ApkDownloadState.Success -> SuccessGreenContainer
                                    else -> PrimaryTealContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isTampered -> Icons.Default.Security
                                downloadState is ApkDownloadState.Success -> Icons.Default.CheckCircle
                                downloadState is ApkDownloadState.Downloading -> Icons.Default.Download
                                else -> Icons.Default.SystemUpdate
                            },
                            contentDescription = if (isTampered) "Security Warning" else if (isMandatory) "Update Required" else "Update Available",
                            modifier = Modifier.size(40.dp),
                            tint = when {
                                isTampered -> ErrorRed
                                downloadState is ApkDownloadState.Success -> SuccessGreen
                                else -> PrimaryTeal
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title
                    Text(
                        text = if (isTampered) {
                            "Peringatan Integritas Aplikasi"
                        } else {
                            payload?.title ?: if (isMandatory) "Pembaruan Aplikasi Diperlukan" else "Pembaruan Aplikasi Tersedia"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isTampered) ErrorRed else DarkNavy
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Message
                    Text(
                        text = if (isTampered) {
                            tamperReason ?: "Terdeteksi manipulasi data atau respons server tidak dapat diverifikasi keasliannya. Demi keamanan data Anda, aplikasi diblokir sementara."
                        } else {
                            payload?.message ?: if (isMandatory) {
                                "Versi aplikasi Anda saat ini sudah tidak didukung oleh sistem. Harap perbarui ke versi terbaru untuk melanjutkan."
                            } else {
                                "Versi terbaru aplikasi telah tersedia. Silakan lakukan pembaruan untuk menikmati fitur dan peningkatan performa terbaru."
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SlateGray,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!isTampered && payload != null) {
                        // Version Badge Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariantColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Versi Anda",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateLight
                                    )
                                    Text(
                                        text = "${PlatformAppInfo.getVersionName()} (v${PlatformAppInfo.getVersionCode()})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = DarkNavy
                                    )
                                }

                                Text(
                                    text = "➔",
                                    color = SlateLight,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Versi Terbaru",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateLight
                                    )
                                    Text(
                                        text = "${payload.latestVersionName} (v${payload.latestVersionCode})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = PrimaryTeal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val rawUrl = payload.updateUrl
                        val decryptedUrl = remember(rawUrl) { ApkUrlCryptoHelper.decryptApkUrl(rawUrl) }
                        val isFolder = remember(decryptedUrl) { ApkUrlCryptoHelper.isGoogleDriveFolder(decryptedUrl) }
                        val directDownloadUrl = remember(decryptedUrl) { ApkUrlCryptoHelper.resolveDirectDownloadUrl(decryptedUrl) }

                        // Function to start in-app download
                        fun startInAppDownload() {
                            val downloadDir = PlatformAppInstaller.getDownloadDirectory()
                            if (downloadDir.isBlank()) {
                                if (decryptedUrl.isNotBlank()) {
                                    try { uriHandler.openUri(decryptedUrl) } catch (_: Exception) {}
                                }
                                return
                            }

                            val apkDestination = "$downloadDir/update_v${payload.latestVersionCode}.apk"
                            downloadState = ApkDownloadState.Downloading(0f, 0L, -1L)

                            coroutineScope.launch {
                                val success = PlatformApkDownloader.download(
                                    url = directDownloadUrl,
                                    destinationPath = apkDestination,
                                    onProgress = { progress, downloaded, total ->
                                        downloadState = ApkDownloadState.Downloading(progress, downloaded, total)
                                    }
                                )

                                if (success) {
                                    if (!PlatformAppInstaller.canInstallPackages()) {
                                        downloadState = ApkDownloadState.NeedsPermission(apkDestination)
                                    } else {
                                        downloadState = ApkDownloadState.Success(apkDestination)
                                        PlatformAppInstaller.installApk(apkDestination)
                                    }
                                } else {
                                    downloadState = ApkDownloadState.Error("Gagal mengunduh pembaruan. Silakan periksa koneksi internet atau gunakan opsi browser.")
                                }
                            }
                        }

                        // UI berdasarkan state unduhan
                        when (val state = downloadState) {
                            is ApkDownloadState.Idle -> {
                                Button(
                                    onClick = {
                                        if (isFolder) {
                                            if (decryptedUrl.isNotBlank()) {
                                                try { uriHandler.openUri(decryptedUrl) } catch (_: Exception) {}
                                            }
                                        } else {
                                            startInAppDownload()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Perbarui Sekarang",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    )
                                }

                                if (!isMandatory) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Checkbox: Jangan tampilkan ini lagi
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { dontShowAgain = !dontShowAgain }
                                            .padding(vertical = 4.dp, horizontal = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = dontShowAgain,
                                            onCheckedChange = { dontShowAgain = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = PrimaryTeal,
                                                checkmarkColor = CardSurface
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Jangan tampilkan ini lagi",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = SlateGray,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedButton(
                                        onClick = { onDismiss?.invoke(dontShowAgain) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Nanti Saja",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = SlateGray,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }

                            is ApkDownloadState.Downloading -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (state.progress >= 0f) {
                                        LinearProgressIndicator(
                                            progress = { state.progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp)),
                                            color = PrimaryTeal,
                                            trackColor = SurfaceVariantColor
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val percent = (state.progress * 100).toInt()
                                        val downloadedStr = formatBytesToMB(state.downloadedBytes)
                                        val totalStr = if (state.totalBytes > 0) formatBytesToMB(state.totalBytes) else "..."
                                        Text(
                                            text = "Mengunduh pembaruan: $percent% ($downloadedStr / $totalStr)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = SlateGray
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp)),
                                            color = PrimaryTeal,
                                            trackColor = SurfaceVariantColor
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val downloadedStr = formatBytesToMB(state.downloadedBytes)
                                        Text(
                                            text = "Mengunduh: $downloadedStr...",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = SlateGray
                                        )
                                    }
                                }
                            }

                            is ApkDownloadState.NeedsPermission -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Izin Diperlukan",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DarkNavy
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Aktifkan izin 'Pasang aplikasi tidak dikenal' untuk aplikasi ini agar dapat menyelesaikan pembaruan.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateGray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            PlatformAppInstaller.openInstallPermissionSettings()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Buka Pengaturan Izin",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            PlatformAppInstaller.installApk(state.apkPath)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(
                                            text = "Saya Sudah Mengizinkan, Pasang",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }
                            }

                            is ApkDownloadState.Success -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Button(
                                        onClick = {
                                            PlatformAppInstaller.installApk(state.apkPath)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pasang Pembaruan",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        )
                                    }
                                }
                            }

                            is ApkDownloadState.Error -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { startInAppDownload() },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Coba Lagi", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                        if (decryptedUrl.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    try { uriHandler.openUri(decryptedUrl) } catch (_: Exception) {}
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Buka Browser", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium))
                                            }
                                        }
                                    }
                                    if (!isMandatory) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        TextButton(
                                            onClick = { onDismiss?.invoke(dontShowAgain) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Tutup",
                                                style = MaterialTheme.typography.labelMedium.copy(color = SlateGray)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isMandatory) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Button Coba Lagi / Verifikasi Ulang (khusus mandatory)
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = SlateGray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Periksa Kembali",
                                color = SlateGray,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}
