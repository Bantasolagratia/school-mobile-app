package com.sch.sekolah_mobile_app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = PrimaryTealContainer,
    onPrimaryContainer = OnPrimaryTealContainer,
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = DarkNavy,
    surface = CardSurface,
    onSurface = DarkNavy,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = SlateGray,
    outline = BorderStrokeColor,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun SekolahMobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

