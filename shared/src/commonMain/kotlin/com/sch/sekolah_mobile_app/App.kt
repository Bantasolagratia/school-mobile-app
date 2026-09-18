package com.sch.sekolah_mobile_app

import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sch.sekolah_mobile_app.data.model.ExamScheduleItem
import com.sch.sekolah_mobile_app.data.model.Jadwal
import com.sch.sekolah_mobile_app.data.model.MateriItem
import com.sch.sekolah_mobile_app.data.model.StudentMataPelajaranItem
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.AuthRepository
import com.sch.sekolah_mobile_app.data.repository.GuruRepository
import com.sch.sekolah_mobile_app.data.repository.JadwalRepository
import com.sch.sekolah_mobile_app.data.repository.MataPelajaranRepository
import com.sch.sekolah_mobile_app.data.repository.NotificationRepository
import com.sch.sekolah_mobile_app.data.repository.RaportRepository
import com.sch.sekolah_mobile_app.data.repository.RemoteConfigManager
import com.sch.sekolah_mobile_app.data.repository.UjianRepository
import com.sch.sekolah_mobile_app.ui.screens.calendar.CalendarScreen
import com.sch.sekolah_mobile_app.ui.screens.guru.GuruModuleScreen
import com.sch.sekolah_mobile_app.ui.screens.home.HomeScreen
import com.sch.sekolah_mobile_app.ui.screens.jadwal.StudentQrScannerScreen
import com.sch.sekolah_mobile_app.ui.screens.jadwal.TeacherQrKioskScreen
import com.sch.sekolah_mobile_app.ui.screens.login.LoginScreen
import com.sch.sekolah_mobile_app.ui.screens.mapel.MateriDetailScreen
import com.sch.sekolah_mobile_app.ui.screens.mapel.StudentMapelScreen
import com.sch.sekolah_mobile_app.ui.screens.mapel.StudentMateriListScreen
import com.sch.sekolah_mobile_app.ui.screens.notifications.NotificationsScreen
import com.sch.sekolah_mobile_app.ui.screens.profile.ProfileScreen
import com.sch.sekolah_mobile_app.ui.screens.raport.RaportScreen
import com.sch.sekolah_mobile_app.ui.screens.ujian.ExamTakingScreen
import com.sch.sekolah_mobile_app.ui.screens.ujian.UjianMuridScreen
import com.sch.sekolah_mobile_app.ui.theme.LightBackground
import com.sch.sekolah_mobile_app.ui.theme.PrimaryTeal
import com.sch.sekolah_mobile_app.ui.theme.SekolahMobileTheme

enum class ScreenState {
    LOGIN,
    MAIN,
    EXAM_TAKING
}

enum class SubScreen {
    NONE,
    UJIAN_LIST,
    MAPEL_STUDENT,
    MATERI_LIST,
    MATERI_DETAIL,
    RAPORT,
    CALENDAR,
    TEACHER_QR_KIOSK,
    STUDENT_QR_SCANNER,
    NOTIFICATIONS
}

enum class NavigationTab(val label: String, val icon: ImageVector) {
    HOME("Beranda", Icons.Default.Home),
    GURU("Guru", Icons.Default.Groups),
    PROFILE("Profil", Icons.Default.Person)
}

@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val guruRepository = remember { GuruRepository(authRepository = authRepository) }
    val ujianRepository = remember { UjianRepository(authRepository = authRepository) }
    val mapelRepository = remember { MataPelajaranRepository(authRepository = authRepository) }
    val raportRepository = remember { RaportRepository(authRepository = authRepository) }
    val jadwalRepository = remember { JadwalRepository(authRepository = authRepository) }
    val notificationRepository = remember { NotificationRepository(authRepository = authRepository) }

    val isRaportModuleEnabled by RemoteConfigManager.instance.isRaportModuleEnabled.collectAsState()

    var screenState by remember {
        mutableStateOf(if (authRepository.hasActiveSession()) ScreenState.MAIN else ScreenState.LOGIN)
    }
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var activeExamItem by remember { mutableStateOf<ExamScheduleItem?>(null) }
    var currentProfile by remember { mutableStateOf(authRepository.getCachedProfile()) }
    var selectedMapel by remember { mutableStateOf<StudentMataPelajaranItem?>(null) }
    var selectedMateri by remember { mutableStateOf<MateriItem?>(null) }
    var selectedMateriId by remember { mutableStateOf<String?>(null) }
    var selectedJadwal by remember { mutableStateOf<Jadwal?>(null) }
    var unreadNotifCount by remember { mutableStateOf(0L) }

    // Sync remote config, SSE, dan hitung notifikasi saat user terautentikasi
    LaunchedEffect(authRepository.getAccessToken()) {
        val token = authRepository.getAccessToken()
        AppLifecycleObserver.onAppForeground(token)
        if (!token.isNullOrBlank()) {
            try {
                unreadNotifCount = notificationRepository.getUnreadCount()
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AppLifecycleObserver.onAppBackground()
        }
    }

    // Route Guard: Evict dari layar raport jika Level 1 dimatikan
    LaunchedEffect(isRaportModuleEnabled) {
        if (!isRaportModuleEnabled && currentSubScreen == SubScreen.RAPORT) {
            currentSubScreen = SubScreen.NONE
        }
    }

    // Zero Data Leak: Tangani penghentian sesi otomatis (14 hari kedaluwarsa atau silent refresh gagal)
    LaunchedEffect(Unit) {
        authRepository.sessionTerminationFlow.collect { _ ->
            AppLifecycleObserver.onAppBackground()
            currentProfile = null
            activeExamItem = null
            selectedMapel = null
            selectedMateri = null
            selectedMateriId = null
            selectedJadwal = null
            unreadNotifCount = 0L
            currentSubScreen = SubScreen.NONE
            currentTab = NavigationTab.HOME
            screenState = ScreenState.LOGIN
        }
    }

    SekolahMobileTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LightBackground
        ) {
            when (screenState) {
                ScreenState.LOGIN -> {
                    LoginScreen(
                        authRepository = authRepository,
                        onLoginSuccess = {
                            currentProfile = authRepository.getCachedProfile()
                            currentTab = NavigationTab.HOME
                            currentSubScreen = SubScreen.NONE
                            screenState = ScreenState.MAIN
                        }
                    )
                }

                ScreenState.EXAM_TAKING -> {
                    val exam = activeExamItem
                    if (exam != null) {
                        ExamTakingScreen(
                            exam = exam,
                            ujianRepository = ujianRepository,
                            profile = currentProfile,
                            onExamSubmitted = {
                                activeExamItem = null
                                currentSubScreen = SubScreen.NONE
                                screenState = ScreenState.MAIN
                            },
                            onEmergencyExit = {
                                activeExamItem = null
                                currentSubScreen = SubScreen.NONE
                                screenState = ScreenState.MAIN
                            },
                            onKickedBySupervisor = {
                                authRepository.logout()
                                currentProfile = null
                                activeExamItem = null
                                currentSubScreen = SubScreen.NONE
                                screenState = ScreenState.LOGIN
                            }
                        )
                    } else {
                        screenState = ScreenState.MAIN
                    }
                }

                ScreenState.MAIN -> {
                    when (currentSubScreen) {
                        SubScreen.UJIAN_LIST -> {
                            UjianMuridScreen(
                                ujianRepository = ujianRepository,
                                profile = currentProfile,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onStartExam = { exam ->
                                    activeExamItem = exam
                                    screenState = ScreenState.EXAM_TAKING
                                }
                            )
                        }

                        SubScreen.MAPEL_STUDENT -> {
                            StudentMapelScreen(
                                mapelRepository = mapelRepository,
                                profile = currentProfile,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onSelectMapel = { mapel ->
                                    selectedMapel = mapel
                                    currentSubScreen = SubScreen.MATERI_LIST
                                }
                            )
                        }

                        SubScreen.MATERI_LIST -> {
                            val mapel = selectedMapel
                            if (mapel != null) {
                                StudentMateriListScreen(
                                    subject = mapel,
                                    mapelRepository = mapelRepository,
                                    onNavigateBack = { currentSubScreen = SubScreen.MAPEL_STUDENT },
                                    onSelectMateri = { mat ->
                                        selectedMateri = mat
                                        selectedMateriId = mat.id
                                        currentSubScreen = SubScreen.MATERI_DETAIL
                                    }
                                )
                            } else {
                                currentSubScreen = SubScreen.MAPEL_STUDENT
                            }
                        }

                        SubScreen.MATERI_DETAIL -> {
                            val mId = selectedMateriId
                            if (mId != null) {
                                MateriDetailScreen(
                                    materiId = mId,
                                    initialMateri = selectedMateri,
                                    mapelRepository = mapelRepository,
                                    onNavigateBack = { currentSubScreen = SubScreen.MATERI_LIST }
                                )
                            } else {
                                currentSubScreen = SubScreen.MATERI_LIST
                            }
                        }

                        SubScreen.RAPORT -> {
                            RaportScreen(
                                raportRepository = raportRepository,
                                profile = currentProfile,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE }
                            )
                        }

                        SubScreen.CALENDAR -> {
                            CalendarScreen(
                                profile = currentProfile,
                                jadwalRepository = jadwalRepository,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onOpenTeacherKiosk = { j ->
                                    selectedJadwal = j
                                    currentSubScreen = SubScreen.TEACHER_QR_KIOSK
                                },
                                onOpenStudentScanner = { j ->
                                    selectedJadwal = j
                                    currentSubScreen = SubScreen.STUDENT_QR_SCANNER
                                }
                            )
                        }

                        SubScreen.TEACHER_QR_KIOSK -> {
                            val j = selectedJadwal
                            if (j != null) {
                                TeacherQrKioskScreen(
                                    jadwal = j,
                                    profile = currentProfile,
                                    jadwalRepository = jadwalRepository,
                                    onNavigateBack = { currentSubScreen = SubScreen.CALENDAR }
                                )
                            } else {
                                currentSubScreen = SubScreen.CALENDAR
                            }
                        }

                        SubScreen.STUDENT_QR_SCANNER -> {
                            StudentQrScannerScreen(
                                jadwal = selectedJadwal,
                                jadwalRepository = jadwalRepository,
                                onNavigateBack = { currentSubScreen = SubScreen.CALENDAR }
                            )
                        }

                        SubScreen.NOTIFICATIONS -> {
                            NotificationsScreen(
                                notificationRepository = notificationRepository,
                                onNavigateBack = {
                                    currentSubScreen = SubScreen.NONE
                                    coroutineScope.launch {
                                        try {
                                            unreadNotifCount = notificationRepository.getUnreadCount()
                                        } catch (_: Exception) {}
                                    }
                                }
                            )
                        }

                        SubScreen.NONE -> {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val isTabletOrWide = maxWidth >= 600.dp

                                if (isTabletOrWide) {
                                    // Tablet & iPad Responsive Layout: NavigationRail on the left
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        NavigationRail(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = PrimaryTeal
                                        ) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            NavigationTab.entries.forEach { tab ->
                                                NavigationRailItem(
                                                    selected = currentTab == tab,
                                                    onClick = { currentTab = tab },
                                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                                    label = { Text(tab.label) },
                                                    colors = NavigationRailItemDefaults.colors(
                                                        selectedIconColor = PrimaryTeal,
                                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                                    )
                                                )
                                            }
                                        }

                                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            MainContent(
                                                tab = currentTab,
                                                authRepository = authRepository,
                                                guruRepository = guruRepository,
                                                profile = currentProfile,
                                                onNavigateToGuru = { currentTab = NavigationTab.GURU },
                                                onNavigateToUjian = { currentSubScreen = SubScreen.UJIAN_LIST },
                                                onNavigateToMapel = { currentSubScreen = SubScreen.MAPEL_STUDENT },
                                                onNavigateToRaport = {
                                                    if (isRaportModuleEnabled) {
                                                        currentSubScreen = SubScreen.RAPORT
                                                    }
                                                },
                                                onNavigateToJadwal = { currentSubScreen = SubScreen.CALENDAR },
                                                onNavigateToNotifications = { currentSubScreen = SubScreen.NOTIFICATIONS },
                                                unreadNotifCount = unreadNotifCount,
                                                onLogout = {
                                                    currentProfile = null
                                                    screenState = ScreenState.LOGIN
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    // Phone Layout: Bottom Navigation Bar
                                    Scaffold(
                                        bottomBar = {
                                            NavigationBar(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                tonalElevation = 4.dp
                                            ) {
                                                NavigationTab.entries.forEach { tab ->
                                                    NavigationBarItem(
                                                        selected = currentTab == tab,
                                                        onClick = { currentTab = tab },
                                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                                        label = { Text(tab.label) },
                                                        colors = NavigationBarItemDefaults.colors(
                                                            selectedIconColor = PrimaryTeal,
                                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    ) { innerPadding ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(innerPadding)
                                        ) {
                                            MainContent(
                                                tab = currentTab,
                                                authRepository = authRepository,
                                                guruRepository = guruRepository,
                                                profile = currentProfile,
                                                onNavigateToGuru = { currentTab = NavigationTab.GURU },
                                                onNavigateToUjian = { currentSubScreen = SubScreen.UJIAN_LIST },
                                                onNavigateToMapel = { currentSubScreen = SubScreen.MAPEL_STUDENT },
                                                onNavigateToRaport = {
                                                    if (isRaportModuleEnabled) {
                                                        currentSubScreen = SubScreen.RAPORT
                                                    }
                                                },
                                                onNavigateToJadwal = { currentSubScreen = SubScreen.CALENDAR },
                                                onNavigateToNotifications = { currentSubScreen = SubScreen.NOTIFICATIONS },
                                                unreadNotifCount = unreadNotifCount,
                                                onLogout = {
                                                    currentProfile = null
                                                    screenState = ScreenState.LOGIN
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    tab: NavigationTab,
    authRepository: AuthRepository,
    guruRepository: GuruRepository,
    profile: UserProfileResponse?,
    onNavigateToGuru: () -> Unit,
    onNavigateToUjian: () -> Unit,
    onNavigateToMapel: () -> Unit,
    onNavigateToRaport: () -> Unit,
    onNavigateToJadwal: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    unreadNotifCount: Long,
    onLogout: () -> Unit
) {
    when (tab) {
        NavigationTab.HOME -> {
            HomeScreen(
                profile = profile,
                onNavigateToGuru = onNavigateToGuru,
                onNavigateToUjian = onNavigateToUjian,
                onNavigateToMapel = onNavigateToMapel,
                onNavigateToRaport = onNavigateToRaport,
                onNavigateToJadwal = onNavigateToJadwal,
                onNavigateToNotifications = onNavigateToNotifications,
                unreadNotifCount = unreadNotifCount
            )
        }
        NavigationTab.GURU -> {
            GuruModuleScreen(
                guruRepository = guruRepository,
                onNavigateBack = null
            )
        }
        NavigationTab.PROFILE -> {
            ProfileScreen(
                authRepository = authRepository,
                profile = profile,
                onLogout = onLogout
            )
        }
    }
}
