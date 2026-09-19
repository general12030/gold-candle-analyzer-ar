package com.goldcandle.analyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GoldLightColors = lightColorScheme(
    primary = Color(0xFFB8860B),
    secondary = Color(0xFF8B6B00),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF1B1B1B),
    onSurface = Color(0xFF1B1B1B)
)

@Composable
fun GoldCandleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GoldLightColors,
        content = content
    )
}
