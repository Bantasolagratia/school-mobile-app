package com.sch.sekolah_mobile_app.ui.screens.ujian

import androidx.compose.runtime.Composable

/**
 * Fasilitas 7.1 & 7.2: Kiosk Mode & Anti-Cheating Screen Lock
 * - Mengaktifkan mode penguncian layar (App Pinning / startLockTask)
 * - Mencegah minimize, recent apps, home button, dan notification pull-down
 * - Mendeteksi jika pengguna mencoba berpindah aplikasi atau meminimalkan layar
 */
@Composable
expect fun ExamKioskEffect(
    enabled: Boolean,
    onViolationDetected: (type: String) -> Unit = {}
)

expect fun releaseExamKiosk()

