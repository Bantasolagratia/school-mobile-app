package com.sch.sekolah_mobile_app.ui.screens.izin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.SuratIzinItemMobile
import com.sch.sekolah_mobile_app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IzinDetailScreen(
    item: SuratIzinItemMobile,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Detail Surat Izin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = "Informasi lengkap permohonan izin",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = DarkNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackground)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner
            val isPending = item.status.equals("PENDING", ignoreCase = true)
            val isApproved = item.status.equals("APPROVED", ignoreCase = true)
            val isRejected = item.status.equals("REJECTED", ignoreCase = true)

            val (statusBg, statusBorder, statusIcon, statusTitle, statusDesc) = when {
                isApproved -> Tuple5(
                    Color(0xFFDCFCE7),
                    Color(0xFF86EFAC),
                    Icons.Default.CheckCircle,
                    "Surat Izin Disetujui",
                    "Permohonan izin Anda telah diterima dan disetujui oleh Guru Penanggung Jawab."
                )
                isRejected -> Tuple5(
                    Color(0xFFFEE2E2),
                    Color(0xFFFCA5A5),
                    Icons.Default.Cancel,
                    "Surat Izin Ditolak",
                    "Permohonan izin Anda ditolak oleh Guru Penanggung Jawab. Periksa catatan penolakan di bawah."
                )
                else -> Tuple5(
                    Color(0xFFFEF9C3),
                    Color(0xFFFDE047),
                    Icons.Default.HourglassTop,
                    "Menunggu Persetujuan Guru",
                    "Surat izin telah terkirim dan sedang menunggu verifikasi oleh Guru Penanggung Jawab."
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, statusBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = if (isApproved) Color(0xFF15803D) else if (isRejected) Color(0xFFB91C1C) else Color(0xFFA16207),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = statusTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusDesc,
                            fontSize = 12.sp,
                            color = SlateGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Catatan Guru jika ada
            if (!item.catatanGuru.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = null,
                                tint = if (isRejected) ErrorRed else PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Catatan Guru Penanggung Jawab",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceVariantColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.catatanGuru,
                                fontSize = 13.sp,
                                color = DarkNavy,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 18.sp
                            )
                        }
                        if (!item.respondedBy.isNullOrBlank() || !item.respondedAt.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Direspon oleh: ${item.respondedBy ?: "-"} pada ${item.respondedAt ?: "-"}",
                                fontSize = 11.sp,
                                color = SlateGray
                            )
                        }
                    }
                }
            }

            // Ringkasan Data Izin
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Informasi Pengajuan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )

                    // ID Izin
                    DetailItem(
                        label = "ID Surat Izin",
                        value = item.id,
                        isMonospace = true
                    )

                    // Kategori
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Kategori Izin", fontSize = 12.sp, color = SlateGray)
                        CategoryBadge(kategori = item.kategori)
                    }

                    // Periode
                    val dateText = if (item.tanggalMulai == item.tanggalSelesai) {
                        item.tanggalMulai
                    } else {
                        "${item.tanggalMulai} s/d ${item.tanggalSelesai}"
                    }
                    DetailItem(label = "Periode Tanggal", value = dateText)

                    // Guru Penanggung Jawab
                    DetailItem(
                        label = "Guru Penanggung Jawab",
                        value = item.guruPenanggungJawabNama ?: "-"
                    )

                    // Keterangan / Isi Surat
                    Column {
                        Text(text = "Keterangan / Isi Surat", fontSize = 12.sp, color = SlateGray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceVariantColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.keterangan,
                                fontSize = 13.sp,
                                color = DarkNavy,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Timestamps
                    if (!item.createdAt.isNullOrBlank()) {
                        DetailItem(label = "Diajukan Pada", value = item.createdAt)
                    }
                }
            }

            // Lampiran Dokumen
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lampiran Dokumen Bukti",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!item.originalFilename.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceVariantColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (item.mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.originalFilename,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkNavy
                                    )
                                    val sizeKb = (item.sizeBytes ?: 0) / 1024
                                    Text(
                                        text = "${sizeKb} KB • ${item.mimeType ?: "-"}",
                                        fontSize = 11.sp,
                                        color = SlateGray
                                    )
                                    if (!item.canonicalFilename.isNullOrBlank()) {
                                        Text(
                                            text = "Tersimpan: ${item.canonicalFilename}",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = SlateLight
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Tidak ada lampiran file dokumen.",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Column {
        Text(text = label, fontSize = 12.sp, color = SlateGray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = DarkNavy
        )
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)
