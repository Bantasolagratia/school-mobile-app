package com.sch.sekolah_mobile_app.ui.screens.jadwal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Standard, ISO/IEC 18004 compliant QR Code renderer using Google ZXing QRCodeWriter.
 * Generates accurate Version 1-40 QR codes with proper alignment patterns,
 * version information bits (Version 7+), and optimal masking penalties.
 */
@Composable
actual fun QrCodeView(
    content: String,
    modifier: Modifier,
    backgroundColor: Color,
    codeColor: Color
) {
    val bitMatrix = remember(content) {
        if (content.isBlank()) null
        else {
            try {
                val hints = mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
                )
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (bitMatrix != null) {
                val width = bitMatrix.width
                val height = bitMatrix.height
                val cellW = size.width / width
                val cellH = size.height / height

                for (y in 0 until height) {
                    for (x in 0 until width) {
                        if (bitMatrix.get(x, y)) {
                            drawRect(
                                color = codeColor,
                                topLeft = Offset(x * cellW, y * cellH),
                                size = Size(cellW + 0.5f, cellH + 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
