package com.sch.sekolah_mobile_app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sch.sekolah_mobile_app.data.model.NotificationItemMobile
import com.sch.sekolah_mobile_app.data.repository.NotificationRepository
import com.sch.sekolah_mobile_app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notificationRepository: NotificationRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Belum Dibaca, 1: Semua (7 Hari)
    var notifications by remember { mutableStateOf<List<NotificationItemMobile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedNotification by remember { mutableStateOf<NotificationItemMobile?>(null) }

    fun loadNotifications() {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null
                val list = notificationRepository.getNotifications(unreadOnly = (selectedTab == 0))
                notifications = list
            } catch (e: Exception) {
                errorMessage = e.message ?: "Gagal memuat notifikasi"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTab) {
        loadNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pusat Notifikasi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Text(
                            text = "Retensi Riwayat 7 Hari Terakhir",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    notificationRepository.markAllAsRead()
                                    loadNotifications()
                                } catch (_: Exception) {}
                            }
                        }
                    ) {
                        Text("Baca Semua", fontSize = 12.sp, color = PrimaryTeal, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightBackground)
        ) {
            // Tab selector (Belum Dibaca vs Semua)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardSurface,
                contentColor = PrimaryTeal
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Belum Dibaca", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Semua (7 Hari)", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            } else if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = SlateLight,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 0) "Tidak Ada Notifikasi Baru" else "Kotak Notifikasi Kosong",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Notifikasi aktivitas absensi, jadwal baru, atau pengumuman sekolah akan ditampilkan di sini selama 7 hari.",
                            fontSize = 12.sp,
                            color = SlateGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications) { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                selectedNotification = item
                                if (!item.isRead) {
                                    coroutineScope.launch {
                                        try {
                                            notificationRepository.markAsRead(item.id)
                                            notifications = notifications.map {
                                                if (it.id == item.id) it.copy(isRead = true) else it
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedNotification?.let { notif ->
        AlertDialog(
            onDismissRequest = { selectedNotification = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryTealContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(notif.category),
                        contentDescription = null,
                        tint = PrimaryTeal
                    )
                }
            },
            title = {
                Text(
                    text = notif.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        text = notif.message,
                        fontSize = 13.sp,
                        color = DarkNavy,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!notif.createdAt.isNullOrBlank()) {
                        Text(
                            text = "Waktu: ${notif.createdAt}",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedNotification = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItemMobile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) CardSurface else Color(0xFFFBFBFB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!item.isRead) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (!item.isRead) PrimaryTealContainer else SurfaceVariantColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(item.category),
                    contentDescription = null,
                    tint = if (!item.isRead) PrimaryTeal else SlateGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = DarkNavy,
                        modifier = Modifier.weight(1f)
                    )

                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = if (!item.isRead) DarkNavy.copy(alpha = 0.85f) else SlateGray,
                    lineHeight = 16.sp
                )

                if (!item.createdAt.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.createdAt,
                        fontSize = 10.sp,
                        color = SlateLight
                    )
                }
            }
        }
    }
}

private fun getCategoryIcon(category: String?): ImageVector {
    return when (category?.uppercase()) {
        "ABSENSI" -> Icons.Default.FactCheck
        "JADWAL" -> Icons.Default.EventNote
        "UJIAN" -> Icons.Default.Assignment
        else -> Icons.Default.Notifications
    }
}
