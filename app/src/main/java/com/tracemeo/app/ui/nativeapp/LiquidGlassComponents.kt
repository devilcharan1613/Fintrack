package com.tracemeo.app.ui.nativeapp

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracemeo.app.R
import com.tracemeo.app.data.LiquidGlassSettings
import com.tracemeo.app.data.MoneyPaise
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

val ANIMATION_STYLES = listOf(
    1 to Pair("Liquid Flow", "🌊"),
    2 to Pair("Glass Ripple", "💧"),
    3 to Pair("Aurora Glow", "🌌"),
    4 to Pair("Mesh Wave", "〰️"),
    5 to Pair("Fluid Morph", "🔮"),
    6 to Pair("Neon Pulse", "⚡"),
    7 to Pair("Soft Fade", "☁️"),
    8 to Pair("Parallax Depth", "📐"),
    9 to Pair("Floating Particles", "✨"),
    10 to Pair("Crystal Refraction", "💎")
)

val WALLPAPER_PRESETS = listOf(
    "wp1" to "Northern Aurora Silk",
    "wp2" to "3D Emerald Prism",
    "wp3" to "Obsidian Molten Gold",
    "wp4" to "Cyberpunk Neon Grid",
    "wp5" to "Cosmic Sapphire Nebula",
    "wp6" to "Crimson Velvet Eclipse",
    "wp7" to "Bioluminescent Abyss",
    "wp8" to "Amethyst Crystal Horizon",
    "wp9" to "Titanium Carbon Weave",
    "wp10" to "Arctic Diamond Frost"
)

@Composable
fun LiquidMeshBackground(
    settings: LiquidGlassSettings,
    tiltXProvider: () -> Float,
    tiltYProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(
        settings.themePreset,
        settings.customPrimaryHex,
        settings.customSecondaryHex
    )
    var firstFrameDrawn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        firstFrameDrawn = true
    }

    val durationMs = when (settings.animationStyle) {
        6 -> 6500
        7 -> 18000
        else -> 12000
    }
    val phaseValue: Float = if (firstFrameDrawn) {
        val infiniteTransition = rememberInfiniteTransition(label = "mesh_anim")
        val phaseState: State<Float> = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        phaseState.value
    } else {
        0f
    }

    val customBitmap = remember(settings.customWallpaperPath) {
        val path = settings.customWallpaperPath
        if (!path.isNullOrBlank() && File(path).exists()) {
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        } else null
    }

    val baseBackgroundBrush = if (palette.isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFEEF8FB),
                Color(0xFFE8F3FB),
                Color(0xFFF1EFFF)
            )
        )
    } else if (palette.id == "dark" || palette.id == "custom") {
        Brush.verticalGradient(
            colors = listOf(
                ThemePresets.AURORA_BASE_TOP,
                ThemePresets.AURORA_BASE_MID,
                ThemePresets.AURORA_BASE_BOTTOM
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                palette.background,
                palette.surface.copy(alpha = 0.65f),
                palette.background
            )
        )
    }

    Box(modifier = modifier.fillMaxSize().background(brush = baseBackgroundBrush)) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val motion = if (settings.deviceMotionEnabled) 14f else 0f
                        translationX = tiltXProvider() * motion
                        translationY = tiltYProvider() * motion
                        scaleX = 1.06f
                        scaleY = 1.06f
                        alpha = settings.backgroundVisibility.coerceIn(0.15f, 1f)
                    }
                    .blur(settings.wallpaperBlurDp.dp)
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f
                    }
            ) {
                drawWallpaperPreset(
                    presetId = settings.wallpaperPreset,
                    palette = palette,
                    visibility = settings.backgroundVisibility,
                    glowFactor = settings.glowIntensity,
                    phase = phaseValue,
                    tiltX = if (settings.deviceMotionEnabled) tiltXProvider() else 0f,
                    tiltY = if (settings.deviceMotionEnabled) tiltYProvider() else 0f
                )
            }
        }
    }
}

private fun DrawScope.drawWallpaperPreset(
    presetId: String,
    palette: ThemePalette,
    visibility: Float,
    glowFactor: Float,
    phase: Float,
    tiltX: Float,
    tiltY: Float
) {
    val w = size.width
    val h = size.height
    val vis = visibility.coerceIn(0.15f, 1f)
    val glow = glowFactor.coerceIn(0.1f, 1f) * vis

    if (palette.isLight) {
        val dx = (cos(phase) * 35f + tiltX * 45f)
        val dy = (sin(phase) * 40f + tiltY * 45f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00BFAE).copy(alpha = 0.18f * glow), Color.Transparent),
                center = Offset(w * 0.82f + dx, h * 0.14f + dy),
                radius = w * 0.75f
            ),
            center = Offset(w * 0.82f + dx, h * 0.14f + dy),
            radius = w * 0.75f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF6EA5FF).copy(alpha = 0.16f * glow), Color.Transparent),
                center = Offset(w * 0.16f - dx * 0.6f, h * 0.82f - dy * 0.6f),
                radius = w * 0.80f
            ),
            center = Offset(w * 0.16f - dx * 0.6f, h * 0.82f - dy * 0.6f),
            radius = w * 0.80f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA855F7).copy(alpha = 0.10f * glow), Color.Transparent),
                center = Offset(w * 0.88f, h * 0.85f),
                radius = w * 0.65f
            ),
            center = Offset(w * 0.88f, h * 0.85f),
            radius = w * 0.65f
        )
        return
    }

    val (c1, c2, c3) = when (presetId) {
        "wp2" -> Triple(Color(0xFF34D399), Color(0xFF047857), Color(0xFF6EE7B7))
        "wp3" -> Triple(Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFFFEF08A))
        "wp4" -> Triple(Color(0xFFF43F5E), Color(0xFF00F5D4), Color(0xFF8B5CF6))
        "wp5" -> Triple(Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFF06B6D4))
        "wp6" -> Triple(Color(0xFFF43F5E), Color(0xFF9F1239), Color(0xFFFB7185))
        "wp7" -> Triple(Color(0xFF2DD4BF), Color(0xFF0284C7), Color(0xFF14B8A6))
        "wp8" -> Triple(Color(0xFFC084FC), Color(0xFF9333EA), Color(0xFFF472B6))
        "wp9" -> Triple(Color(0xFF64748B), Color(0xFF38BDF8), Color(0xFF334155))
        "wp10" -> Triple(Color(0xFFBAE6FD), Color(0xFF38BDF8), Color(0xFF0284C7))
        else -> Triple(
            palette.meshGlow1.copy(alpha = 1f),
            palette.meshGlow2.copy(alpha = 1f),
            Color(0xFF009FDC)
        )
    }

    val dx = (cos(phase) * 55f + tiltX * 70f)
    val dy = (sin(phase) * 65f + tiltY * 70f)

    // Top-right / upper luminous cyan-teal aurora glow (#00F5D4 / #18F2D0)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(c1.copy(alpha = 0.34f * glow), c1.copy(alpha = 0.14f * glow), Color.Transparent),
            center = Offset(w * 0.80f + dx * 0.7f, h * 0.12f + dy * 0.7f),
            radius = w * 0.85f
        ),
        center = Offset(w * 0.80f + dx * 0.7f, h * 0.12f + dy * 0.7f),
        radius = w * 0.85f
    )
    // Bottom-left deep turquoise aurora glow (#009FDC)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(c3.copy(alpha = 0.28f * glow), c3.copy(alpha = 0.10f * glow), Color.Transparent),
            center = Offset(w * 0.15f + dx, h * 0.84f + dy),
            radius = w * 0.82f
        ),
        center = Offset(w * 0.15f + dx, h * 0.84f + dy),
        radius = w * 0.82f
    )
    // Bottom-right / mid-right amethyst violet aurora glow (#A855F7 / #8B5CF6)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(c2.copy(alpha = 0.26f * glow), c2.copy(alpha = 0.09f * glow), Color.Transparent),
            center = Offset(w * 0.88f - dx * 0.6f, h * 0.78f - dy * 0.6f),
            radius = w * 0.85f
        ),
        center = Offset(w * 0.88f - dx * 0.6f, h * 0.78f - dy * 0.6f),
        radius = w * 0.85f
    )

    if (presetId == "wp4") {
        val gridAlpha = 0.22f * vis
        for (i in 0..8) {
            val y = h * 0.55f + (i * h * 0.055f)
            drawLine(c2.copy(alpha = gridAlpha), Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    settings: LiquidGlassSettings = LiquidGlassSettings(),
    shape: RoundedCornerShape? = null,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = ThemePresets.getPalette(
        settings.themePreset,
        settings.customPrimaryHex,
        settings.customSecondaryHex
    )
    val profile = ThemePresets.getGlassProfile(settings.glassUiStyle)
    val resolvedShape = shape ?: RoundedCornerShape(profile.cornerDp.dp)
    val topAlpha = (profile.surfaceAlpha + 0.04f).coerceIn(0.48f, 0.95f)
    val bottomAlpha = (profile.surfaceAlpha + 0.06f).coerceIn(0.52f, 0.96f)
    val effectiveBorderAlpha = profile.borderAlpha.coerceIn(0.24f, 0.88f)

    val cardFillBrush = if (backgroundColor != null) {
        Brush.verticalGradient(
            colors = listOf(
                backgroundColor.copy(alpha = topAlpha),
                backgroundColor.copy(alpha = bottomAlpha)
            )
        )
    } else if (palette.isLight) {
        // Exact HTML Prototype Light Mode Card: linear-gradient(145deg, rgba(255,255,255,0.94), rgba(226,245,249,0.84) 58%, rgba(236,231,255,0.72))
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.94f),
                Color(0xFFE2F5F9).copy(alpha = 0.86f),
                Color(0xFFECE7FF).copy(alpha = 0.76f)
            )
        )
    } else if (palette.id == "dark" || palette.id == "custom") {
        // Exact HTML Prototype Dark Mode Card: linear-gradient(145deg, rgba(16, 52, 68, 0.78), rgba(8, 27, 42, 0.82))
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF103444).copy(alpha = topAlpha),
                Color(0xFF0A2434).copy(alpha = (topAlpha + bottomAlpha) * 0.5f),
                Color(0xFF081B2A).copy(alpha = bottomAlpha)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                palette.surfaceElevated.copy(alpha = topAlpha),
                palette.surface.copy(alpha = bottomAlpha)
            )
        )
    }

    val borderBrush = if (borderColor != null) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (palette.isLight) 0.9f else 0.42f),
                borderColor.copy(alpha = effectiveBorderAlpha),
                borderColor.copy(alpha = (effectiveBorderAlpha * 0.75f).coerceAtLeast(0.20f))
            )
        )
    } else if (palette.isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFF4696AA).copy(alpha = 0.34f),
                Color(0xFF6EA5FF).copy(alpha = 0.26f)
            )
        )
    } else if (palette.id == "dark" || palette.id == "custom") {
        // Prismatic cyan-to-amethyst glass border matching HTML .card::before
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFA5F3FC).copy(alpha = (effectiveBorderAlpha + 0.18f).coerceAtMost(0.75f)),
                Color(0xFF00F5D4).copy(alpha = effectiveBorderAlpha),
                Color(0xFF38BDF8).copy(alpha = (effectiveBorderAlpha * 0.82f).coerceAtLeast(0.22f)),
                Color(0xFFA855F7).copy(alpha = (effectiveBorderAlpha * 0.76f).coerceAtLeast(0.20f))
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                ThemePresets.AURORA_SPECULAR_TOP,
                palette.surfaceBorder.copy(alpha = effectiveBorderAlpha),
                palette.surfaceBorder.copy(alpha = (effectiveBorderAlpha * 0.72f).coerceAtLeast(0.18f))
            )
        )
    }

    Box(
        modifier = modifier
            .clip(resolvedShape)
            .background(brush = cardFillBrush)
            .border(
                width = borderWidth,
                brush = borderBrush,
                shape = resolvedShape
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        content()
    }
}

/**
 * 3D Liquid Glass Icon Badge matching HTML prototype (.acc-icon / .more-icon / .tx-icon)
 */
@Composable
fun GlassBadge3D(
    emoji: String,
    accent: Color,
    sizeDp: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.34f),
                        Color(0xFF082230).copy(alpha = 0.82f),
                        accent.copy(alpha = 0.18f)
                    )
                )
            )
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.48f),
                        accent.copy(alpha = 0.65f),
                        Color(0xFFA855F7).copy(alpha = 0.32f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (sizeDp.value * 0.48f).sp)
    }
}

/**
 * Fixed Left-Aligned Topbar across ALL screens matching HTML prototype .topbar
 */
@Composable
fun TracmioTopBar(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onSingleTapPrivacy: () -> Unit,
    onTripleTapStealth: () -> Unit,
    onOpenOnboarding: () -> Unit = {},
    onQuickAdd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }

    val topbarBgBrush = if (palette.isLight) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xEBFFFFFF),
                Color(0xD9EAF6FA)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xE0031018),
                Color(0xB8031018)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(brush = topbarBgBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenOnboarding)
                    .padding(end = 6.dp)
                    .testTag("topbar_brand_left")
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (palette.isLight) {
                                    listOf(Color(0xFFFFFFFF), Color(0xFFDFF4F8))
                                } else {
                                    listOf(Color(0xFF0A2636), Color(0xFF05131E))
                                }
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (palette.isLight) Color(0xFF009F91).copy(alpha = 0.55f) else Color(0xFF38F9D7).copy(alpha = 0.80f),
                            shape = RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.tracmio_launcher_foreground),
                        contentDescription = "Tracmio Logo — Profile & Currency",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Trac",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "mio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (palette.isLight) Color(0xFF009F91) else Color(0xFF22C55E)
                        )
                        if (settings.stealthModeActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = palette.tertiary.copy(alpha = 0.22f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Stealth Active",
                                        tint = palette.tertiary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "STEALTH",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.tertiary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = if (settings.userName.isNotBlank()) "${settings.userName} · ${settings.currencySymbol} Vault" else "Offline Finance (${settings.currencySymbol})",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Privacy Eye Button — 1x tap toggles Mask, 3x fast tap toggles Stealth Mode
                Surface(
                    shape = CircleShape,
                    color = if (settings.privacyMaskEnabled || settings.stealthModeActive) {
                        palette.primary.copy(alpha = 0.24f)
                    } else if (palette.isLight) {
                        Color.White.copy(alpha = 0.88f)
                    } else {
                        Color(0xFF0E2B3A).copy(alpha = 0.78f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (settings.stealthModeActive) palette.tertiary else palette.surfaceBorder
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapMs <= 750L) {
                                        tapCount += 1
                                    } else {
                                        tapCount = 1
                                    }
                                    lastTapMs = now
                                    if (tapCount >= 3) {
                                        tapCount = 0
                                        onTripleTapStealth()
                                    } else {
                                        onSingleTapPrivacy()
                                    }
                                }
                            )
                        }
                        .testTag("topbar_privacy_eye")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (settings.privacyMaskEnabled || settings.stealthModeActive) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = "Toggle Privacy Mask (Triple-tap for Stealth Mode)",
                            tint = if (settings.stealthModeActive) palette.tertiary else if (settings.privacyMaskEnabled) palette.primary else palette.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Glowing Neon "+ Add" Topbar Pill matching HTML .add-btn
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (palette.isLight) {
                                    listOf(Color(0xFF00BFA5), Color(0xFF009688))
                                } else {
                                    listOf(Color(0xFF00F5D4), Color(0xFF00B894))
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (palette.isLight) Color(0xFF80CBC4) else Color(0xFFA7F3D0).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable(onClick = onQuickAdd)
                        .padding(horizontal = 14.dp)
                        .testTag("topbar_neon_add_btn")
                ) {
                    Text(
                        text = "+ Add",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (palette.isLight) Color.White else Color(0xFF03181C),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.surfaceBorder.copy(alpha = 0.45f))
        )
    }
}

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    title: String,
    valueLabel: String,
    modifier: Modifier = Modifier,
    accentColor: Color = ThemePresets.AURORA_NEON_TEAL,
    textColor: Color = Color.White
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.96f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ThemePresets.AURORA_GLASS_SURFACE.copy(alpha = 0.72f))
                .border(1.2.dp, ThemePresets.AURORA_GLASS_BORDER, RoundedCornerShape(14.dp))
                .pointerInput(valueRange) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newPct = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + newPct * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
                .pointerInput(valueRange) {
                    detectTapGestures { offset ->
                        val newPct = (offset.x / size.width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + newPct * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
        ) {
            val width = maxWidth
            val progress = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val fillWidth = width * progress

            Box(
                modifier = Modifier
                    .width(fillWidth)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.52f),
                                ThemePresets.AURORA_NEON_AQUA.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 38.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = (fillWidth - 14.dp).coerceAtLeast(0.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun LiveAppearancePreview(
    settings: LiquidGlassSettings,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(
        settings.themePreset,
        settings.customPrimaryHex,
        settings.customSecondaryHex
    )
    val animStyleName = ANIMATION_STYLES.firstOrNull { it.first == settings.animationStyle }?.second?.first ?: "Liquid Flow"
    val animStyleIcon = ANIMATION_STYLES.firstOrNull { it.first == settings.animationStyle }?.second?.second ?: "🌊"

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        settings = settings
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Appearance Preview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.primary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "$animStyleIcon $animStyleName",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = 0.28f * settings.glowIntensity),
                                palette.secondary.copy(alpha = 0.16f * settings.glowIntensity)
                            )
                        )
                    )
                    .border(1.2.dp, palette.surfaceBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("TOTAL LIQUID BALANCE", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        MoneyPaise.formatCurrency(142850.0, false, settings.currencySymbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "↑ Income ${MoneyPaise.formatCurrency(65000.0, false, settings.currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "↓ Expenses ${MoneyPaise.formatCurrency(28450.0, false, settings.currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemePresets.AURORA_EXPENSE_ROSE,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
