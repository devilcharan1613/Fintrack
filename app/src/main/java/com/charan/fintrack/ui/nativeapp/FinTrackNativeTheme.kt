package com.charan.fintrack.ui.nativeapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Midnight = Color(0xFF041923)
private val DeepNavy = Color(0xFF062838)
private val Teal = Color(0xFF00D8C5)
private val Cyan = Color(0xFF45DDF4)
private val Violet = Color(0xFF9D6BFF)
private val Ice = Color(0xFFEAF7FC)

@Composable
fun FinTrackNativeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dark = darkColorScheme(
        primary = Teal,
        onPrimary = Midnight,
        secondary = Cyan,
        onSecondary = Midnight,
        tertiary = Violet,
        background = Midnight,
        onBackground = Ice,
        surface = DeepNavy,
        onSurface = Ice,
        surfaceVariant = Color(0xFF123B4B),
        onSurfaceVariant = Color(0xFFB4D0DC),
        error = Color(0xFFFF6B77),
        onError = Color(0xFF26060A)
    )
    val light = lightColorScheme(
        primary = Color(0xFF007E79),
        onPrimary = Color.White,
        secondary = Color(0xFF087EA4),
        tertiary = Color(0xFF6B4DB4),
        background = Color(0xFFF1F8FA),
        onBackground = Color(0xFF102E39),
        surface = Color.White,
        onSurface = Color(0xFF102E39),
        surfaceVariant = Color(0xFFE0EFF2),
        onSurfaceVariant = Color(0xFF45636E)
    )
    MaterialTheme(colorScheme = if (darkTheme) dark else light, content = content)
}
