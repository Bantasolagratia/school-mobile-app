package com.sch.sekolah_mobile_app.ui.screens.jadwal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Platform-independent QR Code renderer.
 * Android implementation uses standard ZXing QRCodeWriter (ISO/IEC 18004 compliant).
 */
@Composable
expect fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    codeColor: Color = Color.Black
)
