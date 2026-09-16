package com.sch.sekolah_mobile_app.ui.screens.mapel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.sch.sekolah_mobile_app.data.model.MateriItem
import com.sch.sekolah_mobile_app.data.model.StudentMataPelajaranItem
import com.sch.sekolah_mobile_app.data.repository.MataPelajaranRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMateriListScreen(
    subject: StudentMataPelajaranItem,
    mapelRepository: MataPelajaranRepository,
    onNavigateBack: () -> Unit,
    onSelectMateri: (MateriItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var materiList by remember { mutableStateOf<List<MateriItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val kodeMapel = subject.kode ?: ""

    fun loadMateri() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val list = mapelRepository.getMateriList(kodeMapel)
                materiList = list
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Gagal memuat materi pembelajaran."
            }
        }
    }

    LaunchedEffect(kodeMapel) {
        loadMateri()
    }

    val filteredMateri = remember(materiList, searchQuery) {
        if (searchQuery.isBlank()) {
            materiList
        } else {
            val q = searchQuery.trim().lowercase()
            materiList.filter {
                (it.judul?.lowercase()?.contains(q) == true) ||
                (it.deskripsi?.lowercase()?.contains(q) == true) ||
                (it.authorName?.lowercase()?.contains(q) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subject.nama ?: "Materi Pelajaran",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Kode: ${subject.kode ?: "-"} • Guru: ${subject.guruPengajar ?: "-"}",
                            fontSize = 12.sp,
                            color = SlateGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    IconButton(onClick = { loadMateri() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = PrimaryTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackground)
                .padding(paddingValues)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = subject.nama ?: "Mata Pelajaran",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Text(
                                    text = "${subject.kategori ?: "Umum"} • ${materiList.size} Materi Tersedia",
                                    fontSize = 12.sp,
                                    color = SlateGray
                                )
                            }
                        }

                        Surface(
                            color = PrimaryTealContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnPrimaryTealContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!subject.deskripsi.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = subject.deskripsi,
                            fontSize = 12.sp,
                            color = SlateGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Cari topik atau materi pembelajaran...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SlateGray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SlateGray
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = BorderStrokeColor
                )
            )

            // Content Body
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryTeal)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Memuat materi pembelajaran...",
                                fontSize = 13.sp,
                                color = SlateGray
                            )
                        }
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkNavy
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { loadMateri() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }

                filteredMateri.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = SlateLight,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Materi tidak ditemukan" else "Belum Ada Materi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Coba kata kunci pencarian yang lain." else "Guru pengampu belum mempublikasikan materi untuk mata pelajaran ini.",
                                fontSize = 12.sp,
                                color = SlateGray
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredMateri, key = { it.id ?: "" }) { materi ->
                            MateriCard(
                                materi = materi,
                                onClick = { onSelectMateri(materi) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MateriCard(
    materi: MateriItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Topic tag
                Surface(
                    color = PrimaryTealContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Materi Pembelajaran",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimaryTealContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Block count
                val blockCount = materi.blocks.size
                Surface(
                    color = SurfaceVariantColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$blockCount bagian",
                        fontSize = 11.sp,
                        color = SlateGray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = materi.judul ?: "Tanpa Judul",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )

            // Description
            if (!materi.deskripsi.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = materi.deskripsi,
                    fontSize = 12.sp,
                    color = SlateGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderStrokeColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = SlateGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = materi.authorName ?: materi.author ?: "Guru Pengajar",
                        fontSize = 12.sp,
                        color = SlateGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Baca Sekarang",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryTeal
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Baca",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

