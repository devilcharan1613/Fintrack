package com.tracemeo.app.ui.nativeapp

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class LiquidGlassSettings(
    val themePreset: String = "dark",
    val glassUiStyle: String = "ui1",
    val wallpaperPreset: String = "wp1",
    val backgroundVisibility: Float = 0.45f,
    val wallpaperBlurDp: Float = 8f,
    val glowIntensity: Float = 0.75f,
    val uiScale: Float = 1.0f,
    val deviceMotionEnabled: Boolean = false,
    val animationStyle: Int = 1,
    val customPrimaryHex: String = "#00F5D4",
    val customSecondaryHex: String = "#A855F7",
    val privacyMaskEnabled: Boolean = false,
    val privacyMaskActive: Boolean = false,
    val stealthModeActive: Boolean = false,
    val customWallpaperPath: String? = null,
    val userName: String = "Charan",
    val currencySymbol: String = "₹",
    val hasCompletedOnboarding: Boolean = false,
    val roundUpEnabled: Boolean = false
) {
    companion object {
        val ALLOWED_CURRENCIES = listOf("₹", "$", "€", "£")
    }
}

data class ThemePalette(
    val id: String,
    val displayName: String,
    val isLight: Boolean,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val meshGlow1: Color,
    val meshGlow2: Color
)

object ThemePresets {
    val ALL_THEMES: List<ThemePalette> = listOf(
        ThemePalette(
            id = "dark",
            displayName = "Aurora Teal",
            isLight = false,
            primary = Color(0xFF00F5D4),
            secondary = Color(0xFFA855F7),
            tertiary = Color(0xFFFBBF24),
            background = Color(0xFF031923),
            backgroundMid = Color(0xFF052230),
            backgroundBottom = Color(0xFF04131A),
            surface = Color(0xC7092B3C),
            surfaceElevated = Color(0xE00F3A4E),
            surfaceBorder = Color(0x3D7DF5FF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFC0D4DF),
            meshGlow1 = Color(0xFF00D6A3),
            meshGlow2 = Color(0xFF695FDC)
        ),
        ThemePalette(
            id = "tracmio",
            displayName = "Tracmio Neon",
            isLight = false,
            primary = Color(0xFF18F2D0),
            secondary = Color(0xFF10B981),
            tertiary = Color(0xFF38BDF8),
            background = Color(0xFF021A16),
            backgroundMid = Color(0xFF052922),
            backgroundBottom = Color(0xFF031411),
            surface = Color(0xC708332B),
            surfaceElevated = Color(0xE00E463B),
            surfaceBorder = Color(0x4718F2D0),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFBBE7DF),
            meshGlow1 = Color(0xFF10B981),
            meshGlow2 = Color(0xFF00D6A3)
        ),
        ThemePalette(
            id = "obsidian",
            displayName = "Obsidian Gold",
            isLight = false,
            primary = Color(0xFFF5A623),
            secondary = Color(0xFFFBBF24),
            tertiary = Color(0xFF38BDF8),
            background = Color(0xFF100E09),
            backgroundMid = Color(0xFF1A160D),
            backgroundBottom = Color(0xFF0B0A07),
            surface = Color(0xCC211B10),
            surfaceElevated = Color(0xE62E2616),
            surfaceBorder = Color(0x4DF5A623),
            textPrimary = Color(0xFFFFFBF0),
            textSecondary = Color(0xFFD6CBB5),
            meshGlow1 = Color(0xFFF5A623),
            meshGlow2 = Color(0xFF92400E)
        ),
        ThemePalette(
            id = "sapphire",
            displayName = "Sapphire Prism",
            isLight = false,
            primary = Color(0xFF38BDF8),
            secondary = Color(0xFF818CF8),
            tertiary = Color(0xFF00F5D4),
            background = Color(0xFF051126),
            backgroundMid = Color(0xFF0B1C3D),
            backgroundBottom = Color(0xFF040D1E),
            surface = Color(0xC70E234A),
            surfaceElevated = Color(0xE0163161),
            surfaceBorder = Color(0x4738BDF8),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFC4D6F2),
            meshGlow1 = Color(0xFF38BDF8),
            meshGlow2 = Color(0xFF6366F1)
        ),
        ThemePalette(
            id = "light",
            displayName = "Arctic Pearl",
            isLight = true,
            primary = Color(0xFF00897B),
            secondary = Color(0xFF0284C7),
            tertiary = Color(0xFFD97706),
            background = Color(0xFFEAF4F7),
            backgroundMid = Color(0xFFF2F9FB),
            backgroundBottom = Color(0xFFE0EFF4),
            surface = Color(0xDEFFFFFF),
            surfaceElevated = Color(0xF5F7FCFE),
            surfaceBorder = Color(0x5500897B),
            textPrimary = Color(0xFF0F2936),
            textSecondary = Color(0xFF436475),
            meshGlow1 = Color(0xFF2DD4BF),
            meshGlow2 = Color(0xFF38BDF8)
        ),
        ThemePalette(
            id = "ruby",
            displayName = "Cyber Ruby",
            isLight = false,
            primary = Color(0xFFFB7185),
            secondary = Color(0xFFA855F7),
            tertiary = Color(0xFF00F5D4),
            background = Color(0xFF1B0710),
            backgroundMid = Color(0xFF2B0B19),
            backgroundBottom = Color(0xFF14050B),
            surface = Color(0xC7360F20),
            surfaceElevated = Color(0xE04A142C),
            surfaceBorder = Color(0x47FB7185),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFEAC2CD),
            meshGlow1 = Color(0xFFFB7185),
            meshGlow2 = Color(0xFF9333EA)
        ),
        ThemePalette(
            id = "emerald",
            displayName = "Matrix Emerald",
            isLight = false,
            primary = Color(0xFF34D399),
            secondary = Color(0xFF22D3EE),
            tertiary = Color(0xFFFBBF24),
            background = Color(0xFF031910),
            backgroundMid = Color(0xFF06291A),
            backgroundBottom = Color(0xFF02120B),
            surface = Color(0xC7093322),
            surfaceElevated = Color(0xE00E462F),
            surfaceBorder = Color(0x4734D399),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFB7E4CE),
            meshGlow1 = Color(0xFF34D399),
            meshGlow2 = Color(0xFF059669)
        ),
        ThemePalette(
            id = "amethyst",
            displayName = "Amethyst Nebula",
            isLight = false,
            primary = Color(0xFFC084FC),
            secondary = Color(0xFF38BDF8),
            tertiary = Color(0xFFFB7185),
            background = Color(0xFF120822),
            backgroundMid = Color(0xFF1D0E36),
            backgroundBottom = Color(0xFF0D051A),
            surface = Color(0xC728144A),
            surfaceElevated = Color(0xE0361B63),
            surfaceBorder = Color(0x47C084FC),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFD9C7EE),
            meshGlow1 = Color(0xFFA855F7),
            meshGlow2 = Color(0xFF06B6D4)
        ),
        ThemePalette(
            id = "titanium",
            displayName = "Titanium Amber",
            isLight = false,
            primary = Color(0xFFFBBF24),
            secondary = Color(0xFF94A3B8),
            tertiary = Color(0xFF38BDF8),
            background = Color(0xFF111418),
            backgroundMid = Color(0xFF1A1F26),
            backgroundBottom = Color(0xFF0B0E11),
            surface = Color(0xCC222831),
            surfaceElevated = Color(0xE62E3642),
            surfaceBorder = Color(0x47FBBF24),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFCBD5E1),
            meshGlow1 = Color(0xFFFBBF24),
            meshGlow2 = Color(0xFF475569)
        ),
        ThemePalette(
            id = "ocean",
            displayName = "Deep Ocean Aqua",
            isLight = false,
            primary = Color(0xFF22D3EE),
            secondary = Color(0xFF0EA5E9),
            tertiary = Color(0xFF00F5D4),
            background = Color(0xFF021420),
            backgroundMid = Color(0xFF042134),
            backgroundBottom = Color(0xFF010E17),
            surface = Color(0xC7072B42),
            surfaceElevated = Color(0xE00B3B59),
            surfaceBorder = Color(0x4722D3EE),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFBAE0F5),
            meshGlow1 = Color(0xFF22D3EE),
            meshGlow2 = Color(0xFF0284C7)
        )
    )

    val WALLPAPER_PRESETS: List<Pair<String, String>> = listOf(
        "wp1" to "Aurora Cyan Mesh",
        "wp2" to "Cosmic Violet Wave",
        "wp3" to "Emerald Horizon",
        "wp4" to "Solar Gold Halo",
        "wp5" to "Midnight Sapphire",
        "wp6" to "Coral Rose Mist",
        "wp7" to "Arctic Frost",
        "wp8" to "Deep Trench",
        "wp9" to "Neon Matrix",
        "wp10" to "Prism Borealis"
    )

    val GLASS_UI_STYLES: List<Pair<String, String>> = listOf(
        "ui1" to "Liquid Cyan (Default)",
        "ui2" to "Frosted Crystal",
        "ui3" to "Neon Edge",
        "ui4" to "Soft Satin",
        "ui5" to "Deep Obsidian",
        "ui6" to "Aqua Prism",
        "ui7" to "Minimal Clear",
        "ui8" to "Double Specular",
        "ui9" to "Capsule Pill",
        "ui10" to "High Contrast Pro"
    )

    fun getPalette(preset: String, primaryHex: String = "#00F5D4", secondaryHex: String = "#A855F7"): ThemePalette {
        return ALL_THEMES.firstOrNull { it.id == preset } ?: ALL_THEMES.first()
    }
}

@Composable
fun TracmioNativeTheme(
    settings: LiquidGlassSettings = LiquidGlassSettings(),
    content: @Composable () -> Unit
) {
    val palette = ThemePresets.getPalette(settings.themePreset, settings.customPrimaryHex, settings.customSecondaryHex)
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, settings.uiScale) {
        val factor = settings.uiScale.coerceIn(0.8f, 1.3f)
        Density(density = baseDensity.density * factor, fontScale = baseDensity.fontScale)
    }

    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceElevated,
            surfaceContainer = palette.surface,
            surfaceContainerHigh = palette.surfaceElevated,
            surfaceContainerHighest = palette.surfaceElevated,
            outline = palette.surfaceBorder,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceElevated,
            surfaceContainer = palette.surface,
            surfaceContainerHigh = palette.surfaceElevated,
            surfaceContainerHighest = palette.surfaceElevated,
            outline = palette.surfaceBorder,
            onPrimary = palette.background,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary
        )
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

@Composable
fun GlassCard(
    settings: LiquidGlassSettings,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    val bgBrush = if (backgroundColor != null) {
        Brush.verticalGradient(listOf(backgroundColor, backgroundColor.copy(alpha = (backgroundColor.alpha * 0.92f))))
    } else if (palette.isLight) {
        Brush.verticalGradient(listOf(palette.surfaceElevated, palette.surface))
    } else {
        Brush.verticalGradient(listOf(Color(0xC70F3A4E), Color(0xB8092B3C)))
    }

    val strokeBrush = if (borderColor != null) {
        Brush.verticalGradient(listOf(borderColor.copy(alpha = 0.75f), borderColor))
    } else {
        Brush.verticalGradient(listOf(Color(0x29FFFFFF), palette.surfaceBorder))
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(width = borderWidth, brush = strokeBrush, shape = shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

@Composable
fun GlassBadge3D(
    emoji: String,
    accent: Color,
    sizeDp: Dp
) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.34f), accent.copy(alpha = 0.12f))
                )
            )
            .border(1.2.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(text = emoji)
    }
}

@Composable
fun LiquidMeshBackground(
    settings: LiquidGlassSettings,
    tiltXProvider: () -> Float,
    tiltYProvider: () -> Float
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    val infiniteTransition = rememberInfiniteTransition(label = "aurora_mesh")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_phase"
    )

    val customBitmap = remember(settings.customWallpaperPath) {
        settings.customWallpaperPath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.background, palette.backgroundMid, palette.backgroundBottom)
                )
            )
    ) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = "Custom Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = settings.backgroundVisibility.coerceIn(0.05f, 1f) }
            )
        }

        // GPU-composited radial aurora orbs (zero per-frame .blur() modifier)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = tiltXProvider() * 14f
                    translationY = tiltYProvider() * 14f
                }
        ) {
            val glowAlpha = (0.28f * settings.glowIntensity).coerceIn(0.08f, 0.55f)
            val orb1Center = Offset(
                x = size.width * (0.22f + 0.08f * cos(phase)),
                y = size.height * (0.18f + 0.06f * sin(phase))
            )
            val orb2Center = Offset(
                x = size.width * (0.78f - 0.08f * sin(phase)),
                y = size.height * (0.48f + 0.08f * cos(phase))
            )
            val orb3Center = Offset(
                x = size.width * (0.35f + 0.06f * sin(phase * 0.7f)),
                y = size.height * (0.82f - 0.05f * cos(phase * 0.7f))
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.meshGlow1.copy(alpha = glowAlpha), Color.Transparent),
                    center = orb1Center,
                    radius = size.width * 0.72f
                ),
                center = orb1Center,
                radius = size.width * 0.72f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.meshGlow2.copy(alpha = glowAlpha * 0.9f), Color.Transparent),
                    center = orb2Center,
                    radius = size.width * 0.68f
                ),
                center = orb2Center,
                radius = size.width * 0.68f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.primary.copy(alpha = glowAlpha * 0.65f), Color.Transparent),
                    center = orb3Center,
                    radius = size.width * 0.62f
                ),
                center = orb3Center,
                radius = size.width * 0.62f
            )
        }
    }
}
