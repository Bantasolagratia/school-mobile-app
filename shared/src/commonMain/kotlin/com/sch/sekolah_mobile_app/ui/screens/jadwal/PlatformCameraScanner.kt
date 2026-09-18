package com.sch.sekolah_mobile_app.ui.screens.jadwal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Multiplatform Hardware Camera Scanner.
 * Strictly enforces live optical camera feed.
 * DILARANG mengakses atau menyediakan intent galeri gambar/file picker.
 */
@Composable
expect fun PlatformCameraScanner(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
)
