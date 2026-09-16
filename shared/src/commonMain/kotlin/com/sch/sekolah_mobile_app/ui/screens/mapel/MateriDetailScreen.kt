package com.sch.sekolah_mobile_app.ui.screens.mapel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.EditorBlockItem
import com.sch.sekolah_mobile_app.data.model.MateriItem
import com.sch.sekolah_mobile_app.data.repository.MataPelajaranRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriDetailScreen(
    materiId: String,
    initialMateri: MateriItem? = null,
    mapelRepository: MataPelajaranRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var materi by remember { mutableStateOf(initialMateri) }
    var isLoading by remember { mutableStateOf(initialMateri == null || initialMateri.blocks.isEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun fetchDetail() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val detail = mapelRepository.getMateriDetail(materiId)
                materi = detail
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                if (materi == null) {
                    errorMessage = e.message ?: "Gagal memuat isi materi."
                }
            }
        }
    }

    LaunchedEffect(materiId) {
        fetchDetail()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = materi?.judul ?: "Materi Pelajaran",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            maxLines = 1
                        )
                        Text(
                            text = "Mode Baca Siswa • ${materi?.namaMapel ?: "Mata Pelajaran"}",
                            fontSize = 12.sp,
                            color = SlateGray,
                            maxLines = 1
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
                    IconButton(onClick = { fetchDetail() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = PrimaryTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = CardSurface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Selesai membaca topik ini?",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }

                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Kembali ke Daftar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            isLoading && materi == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightBackground)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Membuka lembar materi pembelajaran...",
                            fontSize = 13.sp,
                            color = SlateGray
                        )
                    }
                }
            }

            errorMessage != null && materi == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightBackground)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "Gagal memuat materi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { fetchDetail() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
            }

            else -> {
                val currentMateri = materi ?: return@Scaffold
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightBackground)
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Header Article Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Badge subject
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = PrimaryTealContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = currentMateri.kodeMapel ?: "MAPEL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnPrimaryTealContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    color = SurfaceVariantColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = PrimaryTeal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Bahan Bacaan",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = DarkNavy
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Judul Materi
                            Text(
                                text = currentMateri.judul ?: "Tanpa Judul",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy,
                                lineHeight = 28.sp
                            )

                            // Deskripsi
                            if (!currentMateri.deskripsi.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentMateri.deskripsi,
                                    fontSize = 14.sp,
                                    color = SlateGray,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = BorderStrokeColor, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Author info & reading stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryTealContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = PrimaryTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = currentMateri.authorName ?: currentMateri.author ?: "Guru",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkNavy
                                        )
                                        Text(
                                            text = "Guru Pengampu",
                                            fontSize = 10.sp,
                                            color = SlateLight
                                        )
                                    }
                                }

                                Text(
                                    text = "${currentMateri.blocks.size} bagian",
                                    fontSize = 11.sp,
                                    color = SlateGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Material Content Reader Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (currentMateri.blocks.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Konten materi masih kosong atau belum diisi oleh guru.",
                                        fontSize = 13.sp,
                                        color = SlateGray
                                    )
                                }
                            } else {
                                currentMateri.blocks.forEachIndexed { index, block ->
                                    MateriBlockItemView(
                                        block = block,
                                        blockIndex = index + 1,
                                        mapelRepository = mapelRepository
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MateriBlockItemView(
    block: EditorBlockItem,
    blockIndex: Int,
    mapelRepository: MataPelajaranRepository
) {
    val type = block.type?.lowercase() ?: "paragraph"
    val content = block.content ?: ""

    when (type) {
        "heading_1" -> {
            Text(
                text = content,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy,
                lineHeight = 26.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        "heading_2" -> {
            Text(
                text = content,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        "heading_3" -> {
            Text(
                text = content,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkNavy,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        "bullet_list" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    modifier = Modifier.padding(end = 8.dp, start = 4.dp)
                )
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = DarkNavy,
                    lineHeight = 21.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        "numbered_list" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "$blockIndex.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    modifier = Modifier.padding(end = 8.dp, start = 2.dp)
                )
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = DarkNavy,
                    lineHeight = 21.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        "todo" -> {
            val isChecked = block.properties?.checked == true
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryTeal,
                        uncheckedColor = SlateGray
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = if (isChecked) SlateGray else DarkNavy,
                    lineHeight = 20.sp
                )
            }
        }

        "callout" -> {
            val iconEmoji = block.properties?.icon ?: "💡"
            Surface(
                color = PrimaryTealContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = iconEmoji,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        text = content,
                        fontSize = 13.sp,
                        color = OnPrimaryTealContainer,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        "quote" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PrimaryTeal)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = SlateGray,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        "code" -> {
            val language = block.properties?.language ?: "code"
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = language.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = content,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        "divider" -> {
            HorizontalDivider(
                color = BorderStrokeColor,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        "table" -> {
            val rows = block.properties?.rows ?: emptyList()
            if (rows.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        rows.forEachIndexed { rowIndex, rowList ->
                            val isHeader = rowIndex == 0
                            Row(
                                modifier = Modifier
                                    .background(if (isHeader) SurfaceVariantColor else CardSurface)
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                rowList.forEach { cell ->
                                    Text(
                                        text = cell,
                                        fontSize = 12.sp,
                                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                        color = DarkNavy,
                                        modifier = Modifier
                                            .widthIn(min = 90.dp)
                                            .padding(end = 12.dp)
                                    )
                                }
                            }
                            if (rowIndex < rows.size - 1) {
                                HorizontalDivider(color = BorderStrokeColor, thickness = 0.6.dp)
                            }
                        }
                    }
                }
            }
        }

        "image" -> {
            val url = block.properties?.url
            val caption = block.properties?.caption
            MateriImageBlock(
                url = url,
                caption = caption,
                fallbackText = content,
                mapelRepository = mapelRepository
            )
        }

        else -> {
            // Default: paragraph
            Text(
                text = content,
                fontSize = 14.sp,
                color = DarkNavy,
                lineHeight = 22.sp
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun MateriImageBlock(
    url: String?,
    caption: String?,
    fallbackText: String,
    mapelRepository: MataPelajaranRepository
) {
    var imageBitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(!url.isNullOrBlank()) }
    var isError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            isLoading = true
            isError = false
            try {
                val bytes = mapelRepository.fetchImageBytes(url)
                imageBitmap = bytes.decodeToImageBitmap()
                isLoading = false
            } catch (_: Exception) {
                isLoading = false
                isError = true
            }
        }
    }

    Surface(
        color = SurfaceVariantColor,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bmp = imageBitmap
            when {
                bmp != null -> {
                    Image(
                        bitmap = bmp,
                        contentDescription = caption ?: fallbackText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PrimaryTeal,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Memuat gambar...",
                                fontSize = 11.sp,
                                color = SlateGray
                            )
                        }
                    }
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            val cap = caption?.ifBlank { null } ?: fallbackText.ifBlank { null }
            if (cap != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cap,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = SlateGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

