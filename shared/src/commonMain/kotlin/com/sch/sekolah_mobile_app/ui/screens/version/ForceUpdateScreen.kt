package com.sch.sekolah_mobile_app.ui.screens.version

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.DecryptedAppVersionPayload
import com.sch.sekolah_mobile_app.data.security.PlatformAppInfo
import com.sch.sekolah_mobile_app.ui.screens.ujian.ExamBackHandler
import com.sch.sekolah_mobile_app.ui.theme.*

@Composable
fun ForceUpdateScreen(
    payload: DecryptedAppVersionPayload? = null,
    tamperReason: String? = null,
    onRetry: () -> Unit
) {
    // Intersep tombol back hardware agar tidak bisa keluar atau bypass
    ExamBackHandler(enabled = true) {
        // Blokir navigasi kembali saat layar force update aktif
    }

    val uriHandler = LocalUriHandler.current
    val isTampered = !tamperReason.isNullOrBlank()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LightBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
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
                            .background(if (isTampered) ErrorRed.copy(alpha = 0.12f) else PrimaryTealContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTampered) Icons.Default.Security else Icons.Default.SystemUpdate,
                            contentDescription = if (isTampered) "Security Warning" else "Update Required",
                            modifier = Modifier.size(40.dp),
                            tint = if (isTampered) ErrorRed else PrimaryTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title
                    Text(
                        text = if (isTampered) "Peringatan Integritas Aplikasi" else (payload?.title ?: "Pembaruan Aplikasi Diperlukan"),
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
                            payload?.message ?: "Versi aplikasi Anda saat ini sudah tidak didukung oleh sistem. Harap perbarui ke versi terbaru untuk melanjutkan."
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

                        // Button Update Sekarang
                        Button(
                            onClick = {
                                if (payload.updateUrl.isNotBlank()) {
                                    try {
                                        uriHandler.openUri(payload.updateUrl)
                                    } catch (_: Exception) {}
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
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button Coba Lagi / Verifikasi Ulang
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
