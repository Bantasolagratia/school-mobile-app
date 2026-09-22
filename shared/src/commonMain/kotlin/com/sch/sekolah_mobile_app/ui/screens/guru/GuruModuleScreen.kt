package com.sch.sekolah_mobile_app.ui.screens.guru

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.Guru
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.GuruRepository
import com.sch.sekolah_mobile_app.data.storage.copyToClipboard
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruModuleScreen(
    guruRepository: GuruRepository,
    profile: UserProfileResponse? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var searchKeyword by remember { mutableStateOf("") }
    var guruList by remember { mutableStateOf<List<Guru>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedGuru by remember { mutableStateOf<Guru?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun canViewGuruNip(targetGuruNip: String): Boolean {
        if (profile == null) return false
        if (profile.isAdmin) return true
        if (profile.isRoleGuru && profile.nip == targetGuruNip) return true
        return false
    }

    fun loadData() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val data = guruRepository.getDaftarGuru()
                guruList = data
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Gagal memuat data guru"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val filteredList = remember(searchKeyword, guruList) {
        if (searchKeyword.isBlank()) {
            guruList
        } else {
            val q = searchKeyword.trim().lowercase()
            guruList.filter {
                it.nama.lowercase().contains(q) ||
                (it.jabatan?.lowercase()?.contains(q) == true)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = CardSurface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Direktori Guru",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                            Text(
                                text = "Daftar pengajar & staf sekolah",
                                fontSize = 12.sp,
                                color = SlateGray
                            )
                        }
                        IconButton(onClick = { loadData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = PrimaryTeal)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari berdasarkan nama atau mata pelajaran...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = SlateGray)
                        },
                        trailingIcon = {
                            if (searchKeyword.isNotEmpty()) {
                                IconButton(onClick = { searchKeyword = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = SlateGray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = BorderStrokeColor
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackground)
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memuat direktori guru...", color = SlateGray, fontSize = 14.sp)
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "Terjadi kesalahan",
                            color = DarkNavy,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { loadData() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }

                filteredList.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = SlateLight, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchKeyword.isNotBlank()) "Tidak ada guru yang sesuai pencarian \"$searchKeyword\"." else "Belum ada data guru.",
                            color = SlateGray,
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Menampilkan ${filteredList.size} guru",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateGray,
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }

                        items(filteredList, key = { it.nip }) { guru ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGuru = guru },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Initials Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryTealContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = guru.initials,
                                            color = OnPrimaryTealContainer,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = guru.nama,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Jabatan badge
                                            Surface(
                                                color = SurfaceVariantColor,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = guru.displayJabatan,
                                                    fontSize = 11.sp,
                                                    color = PrimaryTeal,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // NIP badge (hanya tampil untuk admin atau pemilik identitas)
                                            if (canViewGuruNip(guru.nip)) {
                                                Text(
                                                    text = "NIP ${guru.nip}",
                                                    fontSize = 11.sp,
                                                    color = SlateGray
                                                )
                                            }
                                        }
                                    }

                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Detail",
                                        tint = SlateLight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    selectedGuru?.let { guru ->
        ModalBottomSheet(
            onDismissRequest = { selectedGuru = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PrimaryTealContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = guru.initials,
                            color = OnPrimaryTealContainer,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = guru.nama,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = guru.displayJabatan,
                            fontSize = 13.sp,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Medium
                        )
                        if (canViewGuruNip(guru.nip)) {
                            Text(
                                text = "NIP: ${guru.nip}",
                                fontSize = 12.sp,
                                color = SlateGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = BorderStrokeColor)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Kontak Pengajar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkNavy
                )

                Spacer(modifier = Modifier.height(12.dp))

                // WhatsApp Card
                val waNumber = guru.wa?.trim()?.takeIf { it.isNotEmpty() } ?: guru.telp?.trim()?.takeIf { it.isNotEmpty() }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WhatsApp", fontSize = 11.sp, color = SlateGray)
                            Text(
                                text = waNumber ?: "Tidak tersedia",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkNavy
                            )
                        }
                        if (!waNumber.isNullOrEmpty()) {
                            IconButton(onClick = {
                                copyToClipboard("WhatsApp", waNumber)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Nomor WhatsApp $waNumber berhasil disalin")
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Salin", tint = PrimaryTeal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Telepon Card
                val telpNumber = guru.telp?.trim()?.takeIf { it.isNotEmpty() }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Telepon",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nomor Telepon", fontSize = 11.sp, color = SlateGray)
                            Text(
                                text = telpNumber ?: "Tidak tersedia",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkNavy
                            )
                        }
                        if (!telpNumber.isNullOrEmpty()) {
                            IconButton(onClick = {
                                copyToClipboard("Telepon", telpNumber)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Nomor telepon $telpNumber berhasil disalin")
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Salin", tint = PrimaryTeal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { selectedGuru = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Tutup")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

