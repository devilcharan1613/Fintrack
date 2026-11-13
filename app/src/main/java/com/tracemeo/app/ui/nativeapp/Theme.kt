package com.tracemeo.app.ui.nativeapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tracemeo.app.R
import com.tracemeo.app.data.LiquidGlassSettings

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val TracmioTypography = Typography(
    headlineLarge = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, lineHeight = 14.sp)
)

data class ThemePalette(
    val id: String,
    val displayName: String,
    val isLight: Boolean,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val meshGlow1: Color,
    val meshGlow2: Color
)

data class GlassUiProfile(
    val id: String,
    val name: String,
    val surfaceAlpha: Float,
    val borderAlpha: Float,
    val cornerDp: Int,
    val blurIntensity: Float
)

object ThemePresets {
    val ALL_THEMES = listOf(
        "dark" to "Aurora Teal",
        "tracmio" to "Tracmio Neon",
        "obsidian" to "Obsidian Gold",
        "sapphire" to "Sapphire Prism",
        "light" to "Arctic Pearl",
        "ruby" to "Cyber Ruby",
        "emerald" to "Matrix Emerald",
        "amethyst" to "Amethyst Nebula",
        "titanium" to "Titanium Amber",
        "ocean" to "Deep Ocean",
        "custom" to "Custom Neon"
    )

    val GLASS_UI_PROFILES = listOf(
        GlassUiProfile("ui1", "Crystal Aurora", 0.72f, 0.36f, 22, 1.0f),
        GlassUiProfile("ui2", "Prism Frost", 0.80f, 0.45f, 26, 1.2f),
        GlassUiProfile("ui3", "Liquid Pill", 0.68f, 0.30f, 28, 0.9f),
        GlassUiProfile("ui4", "Obsidian Velvet", 0.88f, 0.25f, 18, 0.8f),
        GlassUiProfile("ui5", "Ultra Clear", 0.54f, 0.48f, 24, 1.1f),
        GlassUiProfile("ui6", "Neon Edge", 0.76f, 0.75f, 20, 1.0f),
        GlassUiProfile("ui7", "Soft Satin", 0.82f, 0.22f, 24, 1.0f),
        GlassUiProfile("ui8", "Sharp Tech", 0.78f, 0.40f, 12, 0.85f),
        GlassUiProfile("ui9", "Aero Capsule", 0.70f, 0.38f, 30, 1.15f),
        GlassUiProfile("ui10", "Executive Matte", 0.92f, 0.20f, 16, 0.6f)
    )

    fun getGlassProfile(id: String): GlassUiProfile =
        GLASS_UI_PROFILES.firstOrNull { it.id == id } ?: GLASS_UI_PROFILES.first()

    fun parseHexColor(hex: String, fallback: Color): Color {
        return runCatching {
            val cleaned = hex.trim().removePrefix("#")
            val parsed = cleaned.toLong(16)
            when (cleaned.length) {
                6 -> Color((0xFF000000L or parsed).toInt())
                8 -> Color(parsed.toInt())
                else -> fallback
            }
        }.getOrDefault(fallback)
    }

    fun getPalette(
        preset: String,
        customPrimaryHex: String = "#00D6A3",
        customSecondaryHex: String = "#00A9C7"
    ): ThemePalette {
        return when (preset) {
            "tracmio" -> ThemePalette(
                id = "tracmio",
                displayName = "Tracmio Neon",
                isLight = false,
                primary = Color(0xFF22C55E),
                secondary = Color(0xFF06B6D4),
                tertiary = Color(0xFF86EFAC),
                background = Color(0xFF02120A),
                surface = Color(0xFF062A1A),
                surfaceElevated = Color(0xFF0B3B25),
                surfaceBorder = Color(0x6622C55E),
                textPrimary = Color(0xFFF0FDF4),
                textSecondary = Color(0xFFA7F3D0),
                meshGlow1 = Color(0x6622C55E),
                meshGlow2 = Color(0x5506B6D4)
            )
            "obsidian" -> ThemePalette(
                id = "obsidian",
                displayName = "Obsidian Gold",
                isLight = false,
                primary = Color(0xFFF59E0B),
                secondary = Color(0xFFD97706),
                tertiary = Color(0xFFFEF08A),
                background = Color(0xFF09090B),
                surface = Color(0xFF1A1610),
                surfaceElevated = Color(0xFF272115),
                surfaceBorder = Color(0x55F59E0B),
                textPrimary = Color(0xFFFFFBEB),
                textSecondary = Color(0xFFFDE68A),
                meshGlow1 = Color(0x55F59E0B),
                meshGlow2 = Color(0x44B45309)
            )
            "sapphire" -> ThemePalette(
                id = "sapphire",
                displayName = "Sapphire Prism",
                isLight = false,
                primary = Color(0xFF3B82F6),
                secondary = Color(0xFF6366F1),
                tertiary = Color(0xFF93C5FD),
                background = Color(0xFF030714),
                surface = Color(0xFF0F1A38),
                surfaceElevated = Color(0xFF172752),
                surfaceBorder = Color(0x553B82F6),
                textPrimary = Color(0xFFEFF6FF),
                textSecondary = Color(0xFFBFDBFE),
                meshGlow1 = Color(0x663B82F6),
                meshGlow2 = Color(0x556366F1)
            )
            "light" -> ThemePalette(
                id = "light",
                displayName = "Arctic Pearl",
                isLight = true,
                primary = Color(0xFF008D83),
                secondary = Color(0xFF0284C7),
                tertiary = Color(0xFFD97706),
                background = Color(0xFFEAF4F7),
                surface = Color(0xFFF8FCFE),
                surfaceElevated = Color(0xFFFFFFFF),
                surfaceBorder = Color(0x3D235A6E),
                textPrimary = Color(0xFF102B3B),
                textSecondary = Color(0xFF456475),
                meshGlow1 = Color(0x4400D2BE),
                meshGlow2 = Color(0x446EA5FF)
            )
            "ruby" -> ThemePalette(
                id = "ruby",
                displayName = "Cyber Ruby",
                isLight = false,
                primary = Color(0xFFF43F5E),
                secondary = Color(0xFFFB923C),
                tertiary = Color(0xFFFDA4AF),
                background = Color(0xFF140206),
                surface = Color(0xFF2B0A14),
                surfaceElevated = Color(0xFF3E1020),
                surfaceBorder = Color(0x55F43F5E),
                textPrimary = Color(0xFFFFF1F2),
                textSecondary = Color(0xFFFECDD3),
                meshGlow1 = Color(0x66F43F5E),
                meshGlow2 = Color(0x55FB923C)
            )
            "emerald" -> ThemePalette(
                id = "emerald",
                displayName = "Matrix Emerald",
                isLight = false,
                primary = Color(0xFF10B981),
                secondary = Color(0xFF34D399),
                tertiary = Color(0xFF6EE7B7),
                background = Color(0xFF021610),
                surface = Color(0xFF062C20),
                surfaceElevated = Color(0xFF0A402E),
                surfaceBorder = Color(0x5510B981),
                textPrimary = Color(0xFFECFDF5),
                textSecondary = Color(0xFFA7F3D0),
                meshGlow1 = Color(0x6610B981),
                meshGlow2 = Color(0x55059669)
            )
            "amethyst" -> ThemePalette(
                id = "amethyst",
                displayName = "Amethyst Nebula",
                isLight = false,
                primary = Color(0xFFC084FC),
                secondary = Color(0xFFF472B6),
                tertiary = Color(0xFFE879F9),
                background = Color(0xFF0D041A),
                surface = Color(0xFF200C3A),
                surfaceElevated = Color(0xFF2D1252),
                surfaceBorder = Color(0x55C084FC),
                textPrimary = Color(0xFFFAF5FF),
                textSecondary = Color(0xFFE9D5FF),
                meshGlow1 = Color(0x66C084FC),
                meshGlow2 = Color(0x55F472B6)
            )
            "titanium" -> ThemePalette(
                id = "titanium",
                displayName = "Titanium Amber",
                isLight = false,
                primary = Color(0xFFFBBF24),
                secondary = Color(0xFF94A3B8),
                tertiary = Color(0xFFFEF08A),
                background = Color(0xFF0B0E14),
                surface = Color(0xFF181E29),
                surfaceElevated = Color(0xFF232B3A),
                surfaceBorder = Color(0x55FBBF24),
                textPrimary = Color(0xFFF8FAFC),
                textSecondary = Color(0xFFCBD5E1),
                meshGlow1 = Color(0x55FBBF24),
                meshGlow2 = Color(0x4494A3B8)
            )
            "ocean" -> ThemePalette(
                id = "ocean",
                displayName = "Deep Ocean",
                isLight = false,
                primary = Color(0xFF00B4D8),
                secondary = Color(0xFF90E0EF),
                tertiary = Color(0xFF48CAE4),
                background = Color(0xFF010C18),
                surface = Color(0xFF062440),
                surfaceElevated = Color(0xFF0A3257),
                surfaceBorder = Color(0x5500B4D8),
                textPrimary = Color(0xFFF0F9FF),
                textSecondary = Color(0xFFBAE6FD),
                meshGlow1 = Color(0x6600B4D8),
                meshGlow2 = Color(0x550077B6)
            )
            "custom" -> {
                val p = parseHexColor(customPrimaryHex, Color(0xFF00D6A3))
                val s = parseHexColor(customSecondaryHex, Color(0xFF00A9C7))
                ThemePalette(
                    id = "custom",
                    displayName = "Custom Neon",
                    isLight = false,
                    primary = p,
                    secondary = s,
                    tertiary = Color(0xFFF5A623),
                    background = Color(0xFF060911),
                    surface = Color(0xFF0F1726),
                    surfaceElevated = Color(0xFF172338),
                    surfaceBorder = p.copy(alpha = 0.45f),
                    textPrimary = Color(0xFFF8FAFC),
                    textSecondary = Color(0xFF94A3B8),
                    meshGlow1 = p.copy(alpha = 0.4f),
                    meshGlow2 = s.copy(alpha = 0.35f)
                )
            }
            else -> ThemePalette(
                id = "dark",
                displayName = "Aurora Teal",
                isLight = false,
                primary = Color(0xFF00D6A3),
                secondary = Color(0xFF00A9C7),
                tertiary = Color(0xFFF5A623),
                background = Color(0xFF050A12),
                surface = Color(0xFF0B1928),
                surfaceElevated = Color(0xFF11263B),
                surfaceBorder = Color(0x5200E5C3),
                textPrimary = Color(0xFFF5F7FA),
                textSecondary = Color(0xFF98A7B9),
                meshGlow1 = Color(0x5500D6A3),
                meshGlow2 = Color(0x5500A9C7)
            )
        }
    }
}

@Composable
fun TracmioNativeTheme(
    settings: LiquidGlassSettings = LiquidGlassSettings(),
    content: @Composable () -> Unit
) {
    val palette = ThemePresets.getPalette(
        settings.themePreset,
        settings.customPrimaryHex,
        settings.customSecondaryHex
    )
    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            error = Color(0xFFEF4444)
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = Color(0xFF041418),
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            error = Color(0xFFEF4444)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TracmioTypography,
        content = content
    )
}
