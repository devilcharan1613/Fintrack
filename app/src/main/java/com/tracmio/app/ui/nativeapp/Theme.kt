package com.tracmio.app.ui.nativeapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TracmioDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00D6A3),
    onPrimary = Color(0xFF041F18),
    primaryContainer = Color(0xFF09392E),
    onPrimaryContainer = Color(0xFFD8FFF3),
    secondary = Color(0xFF00A9C7),
    onSecondary = Color(0xFF041E24),
    tertiary = Color(0xFFF5A623),
    background = Color(0xFF090D14),
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF101724),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF182235),
    onSurfaceVariant = Color(0xFF98A3B5),
    error = Color(0xFFEF4444)
)

@Composable
fun TracmioNativeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TracmioDarkColorScheme,
        content = content
    )
}
