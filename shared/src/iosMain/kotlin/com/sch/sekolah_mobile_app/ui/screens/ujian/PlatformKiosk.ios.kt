package com.sch.sekolah_mobile_app.ui.screens.ujian

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationUserDidTakeScreenshotNotification
import platform.UIKit.UIScreen
import platform.UIKit.UIScreenCapturedDidChangeNotification

@Composable
actual fun ExamKioskEffect(
    enabled: Boolean,
    onViolationDetected: (type: String) -> Unit
) {
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        try {
            UIApplication.sharedApplication.idleTimerDisabled = true
        } catch (_: Exception) {}

        val center = NSNotificationCenter.defaultCenter
        val mainQueue = NSOperationQueue.mainQueue

        // 1. Deteksi jika Screen Recording / Mirroring sudah aktif sejak awal masuk ujian
        try {
            if (UIScreen.mainScreen.isCaptured()) {
                onViolationDetected("SCREEN_RECORDING_DETECTED")
            }
        } catch (_: Exception) {}

        // 2. Observer Screenshot (Power + Volume Up di iOS)
        val screenshotObserver = center.addObserverForName(
            name = UIApplicationUserDidTakeScreenshotNotification,
            `object` = null,
            queue = mainQueue
        ) { _ ->
            onViolationDetected("SCREENSHOT_TAKEN")
        }

        // 3. Observer Screen Recording / AirPlay Mirroring
        val captureObserver = center.addObserverForName(
            name = UIScreenCapturedDidChangeNotification,
            `object` = null,
            queue = mainQueue
        ) { _ ->
            try {
                if (UIScreen.mainScreen.isCaptured()) {
                    onViolationDetected("SCREEN_RECORDING_DETECTED")
                }
            } catch (_: Exception) {}
        }

        // 4. Observer App Switching / Minimize ke Background
        val backgroundObserver = center.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = mainQueue
        ) { _ ->
            onViolationDetected("APP_SWITCHED")
        }

        onDispose {
            try {
                center.removeObserver(screenshotObserver)
                center.removeObserver(captureObserver)
                center.removeObserver(backgroundObserver)
                UIApplication.sharedApplication.idleTimerDisabled = false
            } catch (_: Exception) {}
        }
    }
}

actual fun releaseExamKiosk() {
    try {
        UIApplication.sharedApplication.idleTimerDisabled = false
    } catch (_: Exception) {}
}
