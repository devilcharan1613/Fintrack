package com.charan.tracmio.ui.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class TracmioBgThemeSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val primary: Color,
    val secondary: Color,
    val bgTop: Color,
    val bgMiddle: Color,
    val bgBottom: Color
)

data class TracmioGlassUiSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val surfaceAlpha: Float,
    val borderTopAlpha: Float,
    val borderBottomAlpha: Float,
    val cornerRadiusDp: Int,
    val elevationDp: Int,
    val tintColor: Color = Color.White
)

val TRACMIO_BG_THEMES = listOf(
    TracmioBgThemeSpec("bg1", "Aurora Teal", "Obsidian cyan & emerald aurora", Color(0xFF22F59B), Color(0xFF18E0D0), Color(0xFF041720), Color(0xFF030D14), Color(0xFF02080E)),
    TracmioBgThemeSpec("bg2", "Tracmio Neon Emerald", "Signature liquid matrix green", Color(0xFF00FA9A), Color(0xFF10B981), Color(0xFF031D16), Color(0xFF03100D), Color(0xFF010806)),
    TracmioBgThemeSpec("bg3", "Obsidian Royal Gold", "Luxury warm gold on carbon", Color(0xFFFFCA4B), Color(0xFFF59E0B), Color(0xFF1E1707), Color(0xFF0E0B05), Color(0xFF070503)),
    TracmioBgThemeSpec("bg4", "Sapphire Prism", "Deep cobalt & electric sky glow", Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFF06162E), Color(0xFF050C1A), Color(0xFF02060D)),
    TracmioBgThemeSpec("bg5", "Arctic Crystal Pearl", "Icy frost & glacial turquoise", Color(0xFF67E8F9), Color(0xFF2DD4BF), Color(0xFF08222B), Color(0xFF051319), Color(0xFF030A0E)),
    TracmioBgThemeSpec("bg6", "Cyber Ruby Nebula", "Crimson neon & magenta pulse", Color(0xFFFF4D7D), Color(0xFFF43F5E), Color(0xFF260816), Color(0xFF13050C), Color(0xFF090206)),
    TracmioBgThemeSpec("bg7", "Matrix Bio-Forest", "Deep canopy lime & jade", Color(0xFF4ADE80), Color(0xFF22C55E), Color(0xFF062112), Color(0xFF04120A), Color(0xFF020905)),
    TracmioBgThemeSpec("bg8", "Amethyst Cosmic Haze", "Ultraviolet orchid & indigo", Color(0xFFC084FC), Color(0xFF8B5CF6), Color(0xFF1B0B2E), Color(0xFF0E0619), Color(0xFF07030D)),
    TracmioBgThemeSpec("bg9", "Titanium Solar Amber", "Molten copper & sunset orange", Color(0xFFFB923C), Color(0xFFF97316), Color(0xFF241005), Color(0xFF120803), Color(0xFF090402)),
    TracmioBgThemeSpec("bg10", "Deep Ocean Bioluminescence", "Abyssal marine & electric aqua", Color(0xFF00E5FF), Color(0xFF00B4D8), Color(0xFF031D29), Color(0xFF020E16), Color(0xFF01070B))
)

val TRACMIO_GLASS_UI_STYLES = listOf(
    TracmioGlassUiSpec("ui1", "Classic Liquid Glass", "Balanced specular frost", 0.10f, 0.34f, 0.08f, 22, 8),
    TracmioGlassUiSpec("ui2", "Frosted Diamond Crystal", "Crisp high-refraction facets", 0.15f, 0.50f, 0.16f, 18, 12),
    TracmioGlassUiSpec("ui3", "Neon Cyber Edge", "Sharp glowing accent rim", 0.08f, 0.65f, 0.22f, 16, 10, Color(0xFF22F59B)),
    TracmioGlassUiSpec("ui4", "3D Floating Prism", "Deep elevated volumetric cards", 0.13f, 0.42f, 0.12f, 26, 16),
    TracmioGlassUiSpec("ui5", "Velvet Matte Smoke", "Stealth low-glare obsidian", 0.18f, 0.18f, 0.04f, 20, 4, Color(0xFF0F172A)),
    TracmioGlassUiSpec("ui6", "Liquid Mercury Chrome", "Metallic silver-white sheen", 0.14f, 0.58f, 0.20f, 24, 14, Color(0xFFE2E8F0)),
    TracmioGlassUiSpec("ui7", "Emerald Bio-Glass", "Infused emerald crystal tint", 0.12f, 0.48f, 0.14f, 22, 10, Color(0xFF10B981)),
    TracmioGlassUiSpec("ui8", "Royal Gold Luxe", "Champagne gold specular rim", 0.11f, 0.52f, 0.16f, 22, 12, Color(0xFFFBBF24)),
    TracmioGlassUiSpec("ui9", "Tactical HUD Grid", "Precision architectural corners", 0.09f, 0.44f, 0.15f, 12, 6, Color(0xFF38BDF8)),
    TracmioGlassUiSpec("ui10", "Ultra-Round Bubble Glass", "Soft organic capsule surfaces", 0.14f, 0.38f, 0.10f, 32, 12)
)

data class ActiveTracmioVisuals(
    val bgSpec: TracmioBgThemeSpec = TRACMIO_BG_THEMES.first(),
    val glassSpec: TracmioGlassUiSpec = TRACMIO_GLASS_UI_STYLES.first(),
    val primaryAccent: Color = Color(0xFF22F59B),
    val secondaryAccent: Color = Color(0xFF18E0D0),
    val incomeColor: Color = Color(0xFF2CE59B),
    val expenseColor: Color = Color(0xFFFF5C7C),
    val warningColor: Color = Color(0xFFFFB84D)
)

val LocalTracmioVisuals = staticCompositionLocalOf { ActiveTracmioVisuals() }

fun parseHexOrDefault(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor("#$clean"))
    }.getOrDefault(fallback)
}

@Composable
fun TracmioNativeTheme(
    preferences: TracmioUserPreferences = TracmioUserPreferences(),
    content: @Composable () -> Unit
) {
    val bgSpec = TRACMIO_BG_THEMES.find { it.id == preferences.bgThemeId } ?: TRACMIO_BG_THEMES.first()
    val glassSpec = TRACMIO_GLASS_UI_STYLES.find { it.id == preferences.glassUiId } ?: TRACMIO_GLASS_UI_STYLES.first()

    val primary = if (preferences.customPrimaryHex.isNotBlank()) {
        parseHexOrDefault(preferences.customPrimaryHex, bgSpec.primary)
    } else {
        bgSpec.primary
    }
    val secondary = if (preferences.customSecondaryHex.isNotBlank()) {
        parseHexOrDefault(preferences.customSecondaryHex, bgSpec.secondary)
    } else {
        bgSpec.secondary
    }

    val colorScheme = darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF031A12),
        primaryContainer = primary.copy(alpha = 0.20f),
        onPrimaryContainer = Color(0xFFD8FFF0),
        secondary = secondary,
        onSecondary = Color(0xFF031C22),
        tertiary = Color(0xFFFFB84D),
        background = bgSpec.bgMiddle,
        onBackground = Color(0xFFF3F8FC),
        surface = bgSpec.bgTop.copy(alpha = 0.92f),
        onSurface = Color(0xFFF3F8FC),
        surfaceVariant = bgSpec.bgTop.copy(alpha = 0.65f),
        onSurfaceVariant = Color(0xFF9EB4C7),
        error = Color(0xFFFF5C7C)
    )

    val visuals = ActiveTracmioVisuals(
        bgSpec = bgSpec,
        glassSpec = glassSpec,
        primaryAccent = primary,
        secondaryAccent = secondary
    )

    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = (baseDensity.fontScale * preferences.uiScale).coerceIn(0.75f, 1.45f)
    )

    CompositionLocalProvider(
        LocalTracmioVisuals provides visuals,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentBorderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    val glass = visuals.glassSpec
    val shape = RoundedCornerShape(glass.cornerRadiusDp.dp)
    val rimColor = accentBorderColor ?: if (glass.id == "ui3") visuals.primaryAccent else glass.tintColor

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            rimColor.copy(alpha = glass.borderTopAlpha),
            rimColor.copy(alpha = glass.borderBottomAlpha),
            visuals.primaryAccent.copy(alpha = glass.borderBottomAlpha * 0.85f)
        )
    )
    val surfaceBrush = Brush.verticalGradient(
        colors = listOf(
            glass.tintColor.copy(alpha = glass.surfaceAlpha + 0.04f),
            visuals.bgSpec.bgTop.copy(alpha = 0.72f)
        )
    )

    val baseMod = modifier
        .shadow(
            elevation = glass.elevationDp.dp,
            shape = shape,
            ambientColor = visuals.primaryAccent.copy(alpha = 0.18f),
            spotColor = visuals.primaryAccent.copy(alpha = 0.25f)
        )
        .clip(shape)
        .background(brush = surfaceBrush, shape = shape)
        .border(BorderStroke(1.dp, borderBrush), shape = shape)
        .let { mod ->
            if (onClick != null) mod.clickable(onClick = onClick) else mod
        }

    Column(
        modifier = baseMod.padding(contentPadding),
        content = content
    )
}

@Composable
fun Tracmio3DBadge(
    icon: ImageVector,
    tint: Color = LocalTracmioVisuals.current.primaryAccent,
    size: Dp = 42.dp,
    iconSize: Dp = 21.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .size(size)
            .shadow(6.dp, shape, spotColor = tint.copy(alpha = 0.45f)),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.42f),
                    tint.copy(alpha = 0.36f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            tint.copy(alpha = 0.32f),
                            Color(0xFF061520).copy(alpha = 0.92f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

fun vectorIconForCategoryOrType(key: String): ImageVector {
    val norm = key.trim().lowercase()
    return when {
        norm.contains("food") || norm.contains("dining") || norm.contains("snack") -> Icons.Default.Restaurant
        norm.contains("transport") || norm.contains("fuel") || norm.contains("car") || norm.contains("travel") -> Icons.Default.DirectionsCar
        norm.contains("shop") || norm.contains("cloth") -> Icons.Default.ShoppingBag
        norm.contains("bill") || norm.contains("util") || norm.contains("electric") -> Icons.Default.Bolt
        norm.contains("health") || norm.contains("med") -> Icons.Default.LocalHospital
        norm.contains("entertain") || norm.contains("movie") -> Icons.Default.Movie
        norm.contains("salary") || norm.contains("income") -> Icons.Default.AttachMoney
        norm.contains("business") || norm.contains("work") -> Icons.Default.Work
        norm.contains("transfer") -> Icons.AutoMirrored.Filled.CompareArrows
        norm.contains("borrow") || norm.contains("lend") || norm.contains("loan") -> Icons.Default.Handshake
        norm.contains("goal") || norm.contains("saving") -> Icons.Default.Savings
        norm.contains("upi") || norm.contains("qr") -> Icons.Default.QrCode2
        norm.contains("card") || norm.contains("credit") -> Icons.Default.CreditCard
        norm.contains("bank") -> Icons.Default.AccountBalance
        norm.contains("cash") || norm.contains("wallet") -> Icons.Default.AccountBalanceWallet
        norm.contains("report") || norm.contains("chart") -> Icons.Default.Analytics
        norm.contains("budget") -> Icons.Default.PieChart
        norm.contains("theme") || norm.contains("appear") -> Icons.Default.Palette
        norm.contains("person") || norm.contains("people") -> Icons.Default.People
        norm.contains("categor") -> Icons.Default.Category
        norm.contains("setting") -> Icons.Default.Settings
        norm.contains("remind") || norm.contains("notif") -> Icons.Default.NotificationsActive
        norm.contains("sec") || norm.contains("lock") -> Icons.Default.Security
        norm.contains("home") -> Icons.Default.Home
        norm.contains("flag") -> Icons.Default.Flag
        norm.contains("spark") -> Icons.Default.AutoAwesome
        norm.contains("up") -> Icons.AutoMirrored.Filled.TrendingUp
        norm.contains("down") -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.Default.ReceiptLong
    }
}
