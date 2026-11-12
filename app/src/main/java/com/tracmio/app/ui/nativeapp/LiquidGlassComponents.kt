package com.tracmio.app.ui.nativeapp

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracmio.app.data.LiquidGlassSettings
import kotlin.math.cos
import kotlin.math.sin

data class ThemePalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val meshGlow1: Color,
    val meshGlow2: Color
)

object ThemePresets {
    fun getPalette(preset: String): ThemePalette {
        return when (preset) {
            "cyber_neon" -> ThemePalette(
                primary = Color(0xFF00F0FF),
                secondary = Color(0xFFFF007F),
                tertiary = Color(0xFF9D00FF),
                background = Color(0xFF0B0416),
                surface = Color(0xFF190C2F),
                surfaceBorder = Color(0x44FF007F),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFFB9A6D9),
                meshGlow1 = Color(0x66FF007F),
                meshGlow2 = Color(0x6600F0FF)
            )
            "emerald_aurora" -> ThemePalette(
                primary = Color(0xFF10B981),
                secondary = Color(0xFF059669),
                tertiary = Color(0xFF34D399),
                background = Color(0xFF021B14),
                surface = Color(0xFF063529),
                surfaceBorder = Color(0x4410B981),
                textPrimary = Color(0xFFF0FDF4),
                textSecondary = Color(0xFFA7F3D0),
                meshGlow1 = Color(0x5510B981),
                meshGlow2 = Color(0x55047857)
            )
            "deep_sapphire" -> ThemePalette(
                primary = Color(0xFF3B82F6),
                secondary = Color(0xFF6366F1),
                tertiary = Color(0xFF60A5FA),
                background = Color(0xFF080E1E),
                surface = Color(0xFF111C38),
                surfaceBorder = Color(0x443B82F6),
                textPrimary = Color(0xFFEFF6FF),
                textSecondary = Color(0xFFBFDBFE),
                meshGlow1 = Color(0x553B82F6),
                meshGlow2 = Color(0x556366F1)
            )
            "obsidian_gold" -> ThemePalette(
                primary = Color(0xFFF59E0B),
                secondary = Color(0xFFD97706),
                tertiary = Color(0xFFFCD34D),
                background = Color(0xFF0D0B07),
                surface = Color(0xFF221C11),
                surfaceBorder = Color(0x44F59E0B),
                textPrimary = Color(0xFFFFFBEB),
                textSecondary = Color(0xFFFDE68A),
                meshGlow1 = Color(0x55F59E0B),
                meshGlow2 = Color(0x44B45309)
            )
            "ruby_sunset" -> ThemePalette(
                primary = Color(0xFFF43F5E),
                secondary = Color(0xFFFB923C),
                tertiary = Color(0xFFFDA4AF),
                background = Color(0xFF1A060F),
                surface = Color(0xFF330F20),
                surfaceBorder = Color(0x44F43F5E),
                textPrimary = Color(0xFFFFF1F2),
                textSecondary = Color(0xFFFECDD3),
                meshGlow1 = Color(0x66F43F5E),
                meshGlow2 = Color(0x55FB923C)
            )
            "frosted_light" -> ThemePalette(
                primary = Color(0xFF0284C7),
                secondary = Color(0xFF0EA5E9),
                tertiary = Color(0xFF38BDF8),
                background = Color(0xFFF1F5F9),
                surface = Color(0xFFFFFFFF),
                surfaceBorder = Color(0x330284C7),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF475569),
                meshGlow1 = Color(0x3338BDF8),
                meshGlow2 = Color(0x330284C7)
            )
            else -> ThemePalette(
                primary = Color(0xFF00D6A3),
                secondary = Color(0xFF00A9C7),
                tertiary = Color(0xFFF5A623),
                background = Color(0xFF070B12),
                surface = Color(0xFF101927),
                surfaceBorder = Color(0x3300D6A3),
                textPrimary = Color(0xFFF8FAFC),
                textSecondary = Color(0xFF94A3B8),
                meshGlow1 = Color(0x5500D6A3),
                meshGlow2 = Color(0x5500A9C7)
            )
        }
    }
}

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

@Composable
fun LiquidMeshBackground(
    settings: LiquidGlassSettings,
    tiltX: Float = 0f,
    tiltY: Float = 0f,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val glowFactor = settings.glowIntensity.coerceIn(0.1f, 1f)

        // Draw solid dark background
        drawRect(color = palette.background)

        val motionFactor = if (settings.deviceMotionEnabled) 1f else 0.2f
        val offsetX1 = (cos(phase) * 60f + tiltX * 80f) * motionFactor
        val offsetY1 = (sin(phase) * 70f + tiltY * 80f) * motionFactor

        val offsetX2 = (sin(phase * 0.8f) * 80f - tiltX * 60f) * motionFactor
        val offsetY2 = (cos(phase * 0.8f) * 90f - tiltY * 60f) * motionFactor

        // Glow orb 1 (top-center/left)
        val center1 = Offset(width * 0.3f + offsetX1, height * 0.2f + offsetY1)
        val radius1 = (width * 0.75f).coerceAtLeast(200f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.meshGlow1.copy(alpha = palette.meshGlow1.alpha * glowFactor),
                    palette.meshGlow1.copy(alpha = palette.meshGlow1.alpha * 0.4f * glowFactor),
                    Color.Transparent
                ),
                center = center1,
                radius = radius1
            ),
            center = center1,
            radius = radius1
        )

        // Glow orb 2 (bottom-right)
        val center2 = Offset(width * 0.75f + offsetX2, height * 0.75f + offsetY2)
        val radius2 = (width * 0.85f).coerceAtLeast(250f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.meshGlow2.copy(alpha = palette.meshGlow2.alpha * glowFactor),
                    palette.meshGlow2.copy(alpha = palette.meshGlow2.alpha * 0.3f * glowFactor),
                    Color.Transparent
                ),
                center = center2,
                radius = radius2
            ),
            center = center2,
            radius = radius2
        )

        // Subtle center accent orb
        val center3 = Offset(width * 0.5f, height * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.primary.copy(alpha = 0.08f * glowFactor),
                    Color.Transparent
                ),
                center = center3,
                radius = width * 0.6f
            ),
            center = center3,
            radius = width * 0.6f
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val bg = backgroundColor ?: Color(0xCC111827)
    val border = borderColor ?: Color(0x3300D6A3)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bg.copy(alpha = 0.85f),
                        bg.copy(alpha = 0.65f)
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        border.copy(alpha = 0.6f),
                        border.copy(alpha = 0.15f),
                        border.copy(alpha = 0.4f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = shape
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        content()
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
    accentColor: Color = Color(0xFF00D6A3)
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
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x331E293B))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                .pointerInput(valueRange) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newPct = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + newPct * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
        ) {
            val width = maxWidth
            val progress = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val fillWidth = width * progress

            // Active track fill with liquid gradient
            Box(
                modifier = Modifier
                    .width(fillWidth)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.4f),
                                accentColor.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Glass slider thumb
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
    val palette = ThemePresets.getPalette(settings.themePreset)
    val animStyleName = ANIMATION_STYLES.firstOrNull { it.first == settings.animationStyle }?.second?.first ?: "Liquid Flow"
    val animStyleIcon = ANIMATION_STYLES.firstOrNull { it.first == settings.animationStyle }?.second?.second ?: "🌊"

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = palette.surface.copy(alpha = 0.8f),
        borderColor = palette.primary.copy(alpha = 0.4f)
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
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$animStyleIcon $animStyleName",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mock Balance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = 0.25f * settings.glowIntensity),
                                palette.secondary.copy(alpha = 0.15f * settings.glowIntensity)
                            )
                        )
                    )
                    .border(1.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "TOTAL LIQUID BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹1,42,850.00",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "↑ Income ₹65,000",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "↓ Expenses ₹28,450",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF43F5E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sample Transaction Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33000000))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "☕", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Coffee & Bakery",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Primary Cash • 11:20 AM",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                Text(
                    text = "-₹240.00",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF43F5E)
                )
            }
        }
    }
}
