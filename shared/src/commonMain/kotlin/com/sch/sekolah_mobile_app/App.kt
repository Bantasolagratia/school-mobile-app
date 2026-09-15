package com.sch.sekolah_mobile_app

import androidx.compose.foundation.layout.*
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
import com.sch.sekolah_mobile_app.data.model.UserProfileResponse
import com.sch.sekolah_mobile_app.data.repository.AuthRepository
import com.sch.sekolah_mobile_app.data.repository.GuruRepository
import com.sch.sekolah_mobile_app.data.repository.UjianRepository
import com.sch.sekolah_mobile_app.ui.screens.guru.GuruModuleScreen
import com.sch.sekolah_mobile_app.ui.screens.home.HomeScreen
import com.sch.sekolah_mobile_app.ui.screens.login.LoginScreen
import com.sch.sekolah_mobile_app.ui.screens.profile.ProfileScreen
import com.sch.sekolah_mobile_app.ui.screens.ujian.ExamTakingScreen
import com.sch.sekolah_mobile_app.ui.screens.ujian.UjianMuridScreen
import com.sch.sekolah_mobile_app.ui.theme.LightBackground
import com.sch.sekolah_mobile_app.ui.theme.PrimaryTeal
import com.sch.sekolah_mobile_app.ui.theme.SekolahMobileTheme

enum class ScreenState {
    LOGIN,
    MAIN
    MAIN,
    EXAM_TAKING
}

enum class SubScreen {
    NONE,
    UJIAN_LIST
}

enum class NavigationTab(val label: String, val icon: ImageVector) {
    HOME("Beranda", Icons.Default.Home),
    GURU("Guru", Icons.Default.Groups),
    PROFILE("Profil", Icons.Default.Person)
}

@Composable
fun App() {
    val authRepository = remember { AuthRepository() }
    val guruRepository = remember { GuruRepository(authRepository = authRepository) }
    val ujianRepository = remember { UjianRepository(authRepository = authRepository) }

    var screenState by remember {
        mutableStateOf(if (authRepository.hasActiveSession()) ScreenState.MAIN else ScreenState.LOGIN)
    }
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var activeExamItem by remember { mutableStateOf<ExamScheduleItem?>(null) }
    var currentProfile by remember { mutableStateOf(authRepository.getCachedProfile()) }

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
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isTabletOrWide = maxWidth >= 600.dp
                    if (currentSubScreen == SubScreen.UJIAN_LIST) {
                        UjianMuridScreen(
                            ujianRepository = ujianRepository,
                            profile = currentProfile,
                            onNavigateBack = { currentSubScreen = SubScreen.NONE },
                            onStartExam = { exam ->
                                activeExamItem = exam
                                screenState = ScreenState.EXAM_TAKING
                            }
                        )
                    } else {
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
                            if (isTabletOrWide) {
                                // Tablet & iPad Responsive Layout: NavigationRail on the left
                                Row(modifier = Modifier.fillMaxSize()) {
                                    NavigationRail(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 4.dp
                                        contentColor = PrimaryTeal
                                    ) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        NavigationTab.entries.forEach { tab ->
                                            NavigationBarItem(
                                            NavigationRailItem(
                                                selected = currentTab == tab,
                                                onClick = { currentTab = tab },
                                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                                label = { Text(tab.label) },
                                                colors = NavigationBarItemDefaults.colors(
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
                                            onLogout = {
                                                currentProfile = null
                                                screenState = ScreenState.LOGIN
                                            }
                                        )
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
                                        onLogout = {
                                            currentProfile = null
                                            screenState = ScreenState.LOGIN
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
                                    )
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

@Composable
private fun MainContent(
    tab: NavigationTab,
    authRepository: AuthRepository,
    guruRepository: GuruRepository,
    profile: UserProfileResponse?,
    onNavigateToGuru: () -> Unit,
    onNavigateToUjian: () -> Unit,
    onLogout: () -> Unit
) {
    when (tab) {
        NavigationTab.HOME -> {
            HomeScreen(
                profile = profile,
                onNavigateToGuru = onNavigateToGuru
                onNavigateToGuru = onNavigateToGuru,
                onNavigateToUjian = onNavigateToUjian
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
