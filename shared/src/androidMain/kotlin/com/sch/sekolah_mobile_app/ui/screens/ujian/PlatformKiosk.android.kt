package com.sch.sekolah_mobile_app.ui.screens.ujian

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.sch.sekolah_mobile_app.data.storage.AndroidPlatformContext

@Composable
actual fun ExamKioskEffect(
    enabled: Boolean,
    onViolationDetected: (type: String) -> Unit
) {
    val context = LocalContext.current
    val activity = (context as? Activity) ?: AndroidPlatformContext.getActivity()

    DisposableEffect(enabled, activity) {
        if (!enabled || activity == null) return@DisposableEffect onDispose {}

        // 1. Lock Task Mode (App Pinning / Kiosk Mode)
        // Mencegah murid menekan Home, Recent Apps, atau membuka panel notifikasi
        try {
            activity.startLockTask()
        } catch (_: Exception) {
            // Ditangani jika perangkat belum mengaktifkan screen pinning di pengaturan
        }

        // 2. FLAG_KEEP_SCREEN_ON (layar tetap aktif) & FLAG_SECURE (anti-screenshot/recording/preview recent)
        try {
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } catch (_: Exception) {}

        // 3. Immersive Sticky Mode (Sembunyikan status bar dan navigation bar)
        val insetsController = try {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller
        } catch (_: Exception) {
            null
        }

        // 4. Lifecycle Monitoring untuk deteksi perpindahan aplikasi
        var hasStarted = false
        val lifecycleOwner = activity as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                hasStarted = true
            } else if (hasStarted && (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP)) {
                // Murid berusaha meminimalkan layar, membuka floating window, atau berpindah aplikasi
                onViolationDetected("APP_SWITCHED")
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)

        onDispose {
            try {
                activity.stopLockTask()
            } catch (_: Exception) {}

            try {
                activity.window.clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SECURE
                )
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
                lifecycleOwner?.lifecycle?.removeObserver(observer)
            } catch (_: Exception) {}
        }
    }
}

actual fun releaseExamKiosk() {
    val activity = AndroidPlatformContext.getActivity() ?: return
    try {
        activity.stopLockTask()
    } catch (_: Exception) {}

    try {
        activity.window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SECURE
        )
        val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    } catch (_: Exception) {}
}

