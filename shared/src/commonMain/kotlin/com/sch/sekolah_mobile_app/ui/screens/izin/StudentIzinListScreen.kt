package com.sch.sekolah_mobile_app.ui.screens.izin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.SuratIzinItemMobile
import com.sch.sekolah_mobile_app.data.repository.IzinRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentIzinListScreen(
    izinRepository: IzinRepository,
    onNavigateBack: () -> Unit,
    onCreateIzin: () -> Unit,
    onSelectIzin: (SuratIzinItemMobile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var izinList by remember { mutableStateOf<List<SuratIzinItemMobile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val refreshData = {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                izinList = izinRepository.getMyIzinList()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Gagal memuat daftar surat izin."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val filteredList = remember(izinList, selectedFilter, searchQuery) {
        izinList.filter { item ->
            val matchFilter = when (selectedFilter) {
                "PENDING" -> item.status.equals("PENDING", ignoreCase = true)
                "APPROVED" -> item.status.equals("APPROVED", ignoreCase = true)
                "REJECTED" -> item.status.equals("REJECTED", ignoreCase = true)
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchSearch = q.isEmpty() ||
                    item.keterangan.lowercase().contains(q) ||
                    (item.guruPenanggungJawabNama?.lowercase()?.contains(q) == true) ||
                    item.kategori.lowercase().contains(q) ||
                    item.id.lowercase().contains(q)
            matchFilter && matchSearch
        }
    }

    val pendingCount = izinList.count { it.status.equals("PENDING", ignoreCase = true) }
    val approvedCount = izinList.count { it.status.equals("APPROVED", ignoreCase = true) }
    val rejectedCount = izinList.count { it.status.equals("REJECTED", ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Surat Izin Siswa",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = "Pengajuan & Riwayat Izin",
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
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = PrimaryTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateIzin,
                containerColor = PrimaryTeal,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "Buat Izin") },
                text = { Text("Buat Surat Izin", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackground)
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Cari izin (alasan, guru, kategori)...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = SlateGray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Hapus",
                                tint = SlateGray
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = BorderStrokeColor
                )
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Semua (${izinList.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("Menunggu ($pendingCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentAmberContainer,
                            selectedLabelColor = Color(0xFFB45309)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "APPROVED",
                        onClick = { selectedFilter = "APPROVED" },
                        label = { Text("Disetujui ($approvedCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDCFCE7),
                            selectedLabelColor = Color(0xFF15803D)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "REJECTED",
                        onClick = { selectedFilter = "REJECTED" },
                        label = { Text("Ditolak ($rejectedCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEE2E2),
                            selectedLabelColor = Color(0xFFB91C1C)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body Content
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryTeal)
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage ?: "Terjadi kesalahan",
                                color = DarkNavy,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { refreshData() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }

                filteredList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Tidak ada surat izin yang sesuai pencarian" else "Belum ada riwayat surat izin",
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Klik tombol '+ Buat Surat Izin' untuk mengajukan izin sakit, keluar sekolah, atau lainnya.",
                                color = SlateGray,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            IzinCard(
                                item = item,
                                onClick = { onSelectIzin(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IzinCard(
    item: SuratIzinItemMobile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(kategori = item.kategori)
                StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reason / Keterangan text
            Text(
                text = item.keterangan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkNavy,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date Range
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = SlateGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val dateText = if (item.tanggalMulai == item.tanggalSelesai) {
                    item.tanggalMulai
                } else {
                    "${item.tanggalMulai} s/d ${item.tanggalSelesai}"
                }
                Text(
                    text = dateText,
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Teacher Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = SlateGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Guru PJ: ${item.guruPenanggungJawabNama ?: "Guru Penanggung Jawab"}",
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }

            // Attachment indicator if available
            if (!item.originalFilename.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariantColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.originalFilename,
                            fontSize = 11.sp,
                            color = DarkNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(kategori: String) {
    val (label, bg, fg) = when (kategori.uppercase()) {
        "SAKIT" -> Triple("Sakit", Color(0xFFFEF3C7), Color(0xFFB45309))
        "KELUAR_SEKOLAH" -> Triple("Keluar Sekolah", Color(0xFFCCFBF1), Color(0xFF0F766E))
        else -> Triple("Lainnya", Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, bg, fg) = when (status.uppercase()) {
        "APPROVED" -> Triple("Disetujui", Color(0xFFDCFCE7), Color(0xFF15803D))
        "REJECTED" -> Triple("Ditolak", Color(0xFFFEE2E2), Color(0xFFB91C1C))
        else -> Triple("Menunggu", Color(0xFFFEF9C3), Color(0xFFA16207))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
