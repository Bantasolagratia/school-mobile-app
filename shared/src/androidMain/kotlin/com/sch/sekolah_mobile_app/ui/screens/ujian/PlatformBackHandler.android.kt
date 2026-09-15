package com.sch.sekolah_mobile_app.ui.screens.ujian

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun ExamBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

