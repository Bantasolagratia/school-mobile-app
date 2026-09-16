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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.StudentMataPelajaranItem
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.MataPelajaranRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMapelScreen(
    mapelRepository: MataPelajaranRepository,
    profile: UserProfileResponse?,
    onNavigateBack: () -> Unit,
    onSelectMapel: (StudentMataPelajaranItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var subjectList by remember { mutableStateOf<List<StudentMataPelajaranItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val studentKelas = profile?.displayDetail ?: "Kelas 10-B"

    fun loadSubjects() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val list = mapelRepository.getStudentSubjects(studentKelas)
                subjectList = list
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Gagal memuat daftar mata pelajaran."
            }
        }
    }

    LaunchedEffect(studentKelas) {
        loadSubjects()
    }

    val filteredSubjects = remember(subjectList, searchQuery) {
        if (searchQuery.isBlank()) {
            subjectList
        } else {
            val q = searchQuery.trim().lowercase()
            subjectList.filter {
                (it.nama?.lowercase()?.contains(q) == true) ||
                (it.kode?.lowercase()?.contains(q) == true) ||
                (it.kategori?.lowercase()?.contains(q) == true) ||
                (it.guruPengajar?.lowercase()?.contains(q) == true)
            }
        }
    }

    val totalMateri = remember(subjectList) {
        subjectList.sumOf { it.totalMateri }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mata Pelajaran",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = "Kelas: $studentKelas",
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
                    IconButton(onClick = { loadSubjects() }) {
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
            // Summary Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(PrimaryTeal, PrimaryTealDark)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Materi Pembelajaran",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Akses modul, bahan bacaan, dan materi guru yang di-assign ke kelasmu.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalMateri",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Materi Aktif",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Cari mata pelajaran atau guru...", fontSize = 14.sp) },
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

            // Content Area
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
                                text = "Memuat daftar mata pelajaran...",
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
                                onClick = { loadSubjects() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }

                filteredSubjects.isEmpty() -> {
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
                                text = if (searchQuery.isNotEmpty()) "Mata pelajaran tidak ditemukan" else "Belum Ada Mata Pelajaran",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Coba kata kunci pencarian yang lain." else "Tidak ada mata pelajaran yang di-assign untuk kelas $studentKelas.",
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
                        items(filteredSubjects, key = { it.kode ?: it.id ?: "" }) { mapel ->
                            StudentMapelCard(
                                mapel = mapel,
                                onClick = { onSelectMapel(mapel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentMapelCard(
    mapel: StudentMataPelajaranItem,
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
                // Category & Code badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = PrimaryTealContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = mapel.kode ?: "-",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnPrimaryTealContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (!mapel.kategori.isNullOrBlank()) {
                        Surface(
                            color = SurfaceVariantColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = mapel.kategori,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateGray,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Materi Count Badge
                Surface(
                    color = if (mapel.totalMateri > 0) AccentAmberContainer else SurfaceVariantColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = if (mapel.totalMateri > 0) Color(0xFFB45309) else SlateGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${mapel.totalMateri} Materi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mapel.totalMateri > 0) Color(0xFFB45309) else SlateGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = mapel.nama ?: "Mata Pelajaran",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )

            if (!mapel.deskripsi.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mapel.deskripsi,
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

            // Footer: Teacher name & Open action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariantColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = mapel.guruPengajar ?: "Guru Pengampu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!mapel.guruNip.isNullOrBlank()) {
                            Text(
                                text = "NIP: ${mapel.guruNip}",
                                fontSize = 10.sp,
                                color = SlateLight
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lihat Materi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryTeal
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

