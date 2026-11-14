package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiquidGlassComponents(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        settings = settings,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = palette.surface.copy(alpha = 0.82f),
        borderColor = palette.primary.copy(alpha = 0.38f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xC70F3A4E),
                            Color(0xB8092B3C)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun LiquidCapsuleSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayText: String,
    palette: ThemePalette,
    onValueChange: (Float) -> Unit,
    onStepMinus: () -> Unit,
    onStepPlus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.clickable(onClick = onStepMinus)
                ) {
                    Text("-1", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = palette.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(displayText, style = MaterialTheme.typography.labelLarge, color = palette.primary, fontWeight = FontWeight.ExtraBold)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.clickable(onClick = onStepPlus)
                ) {
                    Text("+1", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = palette.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = palette.primary,
                activeTrackColor = palette.primary,
                inactiveTrackColor = palette.surfaceElevated
            )
        )
    }
}

@Composable
fun AuroraGlassSectionHeader(
    emoji: String,
    title: String,
    subtitle: String,
    palette: ThemePalette,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            GlassBadge3D(emoji = emoji, accent = palette.primary, sizeDp = 40.dp)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            }
        }
        trailing?.invoke()
    }
}
