package com.sch.sekolah_mobile_app.ui.screens.izin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.Guru
import com.sch.sekolah_mobile_app.data.model.SelectedFile
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.GuruRepository
import com.sch.sekolah_mobile_app.data.repository.IzinRepository
import com.sch.sekolah_mobile_app.ui.screens.ujian.getCurrentWibDateTime
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.random.Random

fun generateUniqueIzinId(): String {
    val randomBytes = Random.nextBytes(16)
    randomBytes[6] = ((randomBytes[6].toInt() and 0x0f) or 0x40).toByte()
    randomBytes[8] = ((randomBytes[8].toInt() and 0x3f) or 0x80).toByte()
    val hex = randomBytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateIzinScreen(
    izinRepository: IzinRepository,
    guruRepository: GuruRepository,
    profile: UserProfileResponse?,
    onNavigateBack: () -> Unit,
    onSuccessSubmitted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val generatedId = remember { generateUniqueIzinId() }
    val defaultToday = remember {
        try {
            getCurrentWibDateTime().first
        } catch (_: Exception) {
            "2026-09-25"
        }
    }

    var kategori by remember { mutableStateOf("SAKIT") }
    var tanggalMulai by remember { mutableStateOf(defaultToday) }
    var tanggalSelesai by remember { mutableStateOf(defaultToday) }
    var keterangan by remember { mutableStateOf("") }

    var guruList by remember { mutableStateOf<List<Guru>>(emptyList()) }
    var defaultWaliGuru by remember { mutableStateOf<Guru?>(null) }
    var selectedGuru by remember { mutableStateOf<Guru?>(null) }
    var isGuruDropdownOpen by remember { mutableStateOf(false) }
    var isTeacherSearchOpen by remember { mutableStateOf(false) }
    var teacherSearchQuery by remember { mutableStateOf("") }

    var selectedFile by remember { mutableStateOf<SelectedFile?>(null) }
    var filePickerError by remember { mutableStateOf<String?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Load list guru & default wali kelas
    LaunchedEffect(Unit) {
        val list = try {
            guruRepository.getDaftarGuru()
        } catch (_: Exception) {
            emptyList()
        }
        guruList = list

        val wali = try {
            izinRepository.getDefaultWaliKelas()
        } catch (_: Exception) {
            null
        }

        if (wali != null) {
            defaultWaliGuru = wali
            val matchedInList = list.firstOrNull { it.nip == wali.nip }
            selectedGuru = matchedInList ?: wali
        } else if (list.isNotEmpty() && selectedGuru == null) {
            selectedGuru = list.first()
        }
    }

    val openFilePicker = rememberPlatformFilePicker(
        onFileSelected = { file ->
            selectedFile = file
            filePickerError = null
        },
        onError = { err ->
            filePickerError = err
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Buat Surat Izin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = "Isi formulir pengajuan izin",
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
            // Card ID Izin Tergenerate
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryTealContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ID Izin Otomatis:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryTealDark
                        )
                        Text(
                            text = generatedId,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTealDark
                        )
                    }
                }
            }

            // Kategori Izin
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kategori Izin *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val categories = listOf(
                        "SAKIT" to "Sakit",
                        "KELUAR_SEKOLAH" to "Keluar Sekolah",
                        "LAINNYA" to "Lainnya"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (catKey, catLabel) ->
                            val isSelected = kategori == catKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) PrimaryTeal else BorderStrokeColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(if (isSelected) PrimaryTealContainer else Color.Transparent)
                                    .clickable { kategori = catKey }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = catLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PrimaryTealDark else SlateGray
                                )
                            }
                        }
                    }
                }
            }

            // Tanggal Izin
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Periode Tanggal Izin *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = tanggalMulai,
                            onValueChange = { tanggalMulai = it },
                            label = { Text("Tgl Mulai (YYYY-MM-DD)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryTeal,
                                unfocusedBorderColor = BorderStrokeColor
                            )
                        )

                        OutlinedTextField(
                            value = tanggalSelesai,
                            onValueChange = { tanggalSelesai = it },
                            label = { Text("Tgl Selesai (YYYY-MM-DD)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryTeal,
                                unfocusedBorderColor = BorderStrokeColor
                            )
                        )
                    }
                }
            }

            // Guru Penanggung Jawab
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Guru Penanggung Jawab *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pilih guru yang akan memverifikasi dan menyetujui surat izin",
                        fontSize = 11.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTeacherSearchOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeColor),
                        color = SurfaceVariantColor
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedGuru?.nama ?: "Pilih Guru Penanggung Jawab",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedGuru != null) DarkNavy else SlateGray
                                )
                                if (selectedGuru != null) {
                                    val isWali = (selectedGuru?.nip != null && selectedGuru?.nip == defaultWaliGuru?.nip)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (isWali) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = PrimaryTealContainer,
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Text(
                                                    text = "Wali Kelas",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryTealDark,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = selectedGuru?.displayJabatan ?: "Guru Pengajar",
                                            fontSize = 11.sp,
                                            color = SlateGray
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = SlateGray
                            )
                        }
                    }
                }
            }

            // Keterangan / Isi Surat
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Keterangan / Isi Surat Izin *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tuliskan alasan lengkap, gejala penyakit, atau keperluan izin sekolah.",
                        fontSize = 11.sp,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { keterangan = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        placeholder = {
                            Text(
                                "Contoh: Mengalami demam dan flu sejak semalam, mohon izin beristirahat di rumah sesuai anjuran dokter.",
                                fontSize = 12.sp,
                                color = SlateLight
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = BorderStrokeColor
                        )
                    )
                }
            }

            // Upload Dokumen (Gambar / PDF)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lampiran Dokumen Bukti (Opsional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unggah surat dokter, surat orang tua, atau berkas pendukung (PDF/Gambar, maks 5 MB) bila ada.",
                        fontSize = 11.sp,
                        color = SlateGray,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedFile == null) {
                        OutlinedButton(
                            onClick = { openFilePicker() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pilih Dokumen (Gambar / PDF)", fontSize = 13.sp)
                        }
                    } else {
                        // File Selected Preview Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryTealContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTealLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (selectedFile?.mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedFile?.name ?: "Dokumen",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryTealDark
                                    )
                                    val sizeKb = (selectedFile?.sizeBytes ?: 0) / 1024
                                    Text(
                                        text = "${sizeKb} KB • ${selectedFile?.mimeType}",
                                        fontSize = 10.sp,
                                        color = SlateGray
                                    )
                                }
                                IconButton(onClick = { selectedFile = null }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus File",
                                        tint = ErrorRed
                                    )
                                }
                            }
                        }
                    }

                    if (filePickerError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = filePickerError ?: "",
                            fontSize = 11.sp,
                            color = ErrorRed
                        )
                    }
                }
            }

            // Error Display if submit failed
            if (submitError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = submitError ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    // Validasi
                    val trimmedKeterangan = keterangan.trim()
                    if (trimmedKeterangan.isEmpty()) {
                        submitError = "Keterangan / isi surat izin tidak boleh kosong."
                        return@Button
                    }
                    if (selectedGuru == null) {
                        submitError = "Silakan pilih Guru Penanggung Jawab."
                        return@Button
                    }
                    val file = selectedFile

                    submitError = null
                    isSubmitting = true
                    coroutineScope.launch {
                        try {
                            izinRepository.submitIzin(
                                id = generatedId,
                                kategori = kategori,
                                tanggalMulai = tanggalMulai.trim(),
                                tanggalSelesai = tanggalSelesai.trim(),
                                keterangan = trimmedKeterangan,
                                guruNip = selectedGuru!!.nip,
                                guruNama = selectedGuru!!.nama,
                                file = file
                            )
                            showSuccessDialog = true
                        } catch (e: Exception) {
                            submitError = e.message ?: "Gagal mengirim surat izin."
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mengirim Pengajuan...", fontSize = 14.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kirim Surat Izin", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Dialog Pilih Guru
    if (isTeacherSearchOpen) {
        val filteredGuru = remember(guruList, teacherSearchQuery, defaultWaliGuru) {
            val q = teacherSearchQuery.trim().lowercase()
            val baseList = if (q.isEmpty()) guruList else guruList.filter {
                it.nama.lowercase().contains(q) ||
                        (it.jabatan?.lowercase()?.contains(q) == true) ||
                        (defaultWaliGuru?.nip == it.nip && "wali kelas".contains(q))
            }
            // Prioritaskan Wali Kelas di posisi paling atas
            if (defaultWaliGuru != null) {
                baseList.sortedByDescending { it.nip == defaultWaliGuru?.nip }
            } else {
                baseList
            }
        }

        AlertDialog(
            onDismissRequest = { isTeacherSearchOpen = false },
            title = {
                Text("Pilih Guru Penanggung Jawab", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = teacherSearchQuery,
                        onValueChange = { teacherSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari nama guru...", fontSize = 12.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateGray) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredGuru, key = { it.nip }) { guru ->
                            val isWali = (guru.nip == defaultWaliGuru?.nip)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedGuru = guru
                                        isTeacherSearchOpen = false
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedGuru?.nip == guru.nip) PrimaryTealContainer else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = guru.nama,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkNavy
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (isWali) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = PrimaryTealContainer,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "Wali Kelas",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryTealDark,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = guru.displayJabatan,
                                                fontSize = 11.sp,
                                                color = SlateGray
                                            )
                                        }
                                    }
                                    if (selectedGuru?.nip == guru.nip) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PrimaryTeal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isTeacherSearchOpen = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Modal Dialog Sukses
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onSuccessSubmitted()
            },
            title = {
                Text("Pengajuan Terkirim! 🎉", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Surat izin berhasil diajukan dengan status PENDING. Mohon menunggu persetujuan dari Guru Penanggung Jawab (${selectedGuru?.nama ?: "-"}). Anda akan menerima notifikasi setelah ada respon.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onSuccessSubmitted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Kembali ke Riwayat")
                }
            }
        )
    }
}
