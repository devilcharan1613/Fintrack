package com.charan.tracmio.ui.nativeapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charan.tracmio.R
import com.charan.tracmio.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthlyTrendPoint(
    val label: String,
    val fullMonthLabel: String,
    val income: Double,
    val expense: Double
) {
    val net: Double get() = income - expense
}

fun computeSixMonthTrend(transactions: List<TransactionEntity>): List<MonthlyTrendPoint> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val shortFmt = SimpleDateFormat("MMM", Locale.US)
    val fullFmt = SimpleDateFormat("MMMM yyyy", Locale.US)

    return (5 downTo 0).map { monthsBack ->
        val startCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -monthsBack) }
        val endCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val startMs = startCal.timeInMillis
        val endMs = endCal.timeInMillis

        var inc = 0.0
        var exp = 0.0
        transactions.forEach { tx ->
            if (tx.timestamp in startMs until endMs) {
                when (tx.type.uppercase()) {
                    "INCOME", "BORROW" -> inc += tx.amount
                    "EXPENSE", "LEND" -> exp += tx.amount
                }
            }
        }
        MonthlyTrendPoint(
            label = shortFmt.format(startCal.time),
            fullMonthLabel = fullFmt.format(startCal.time),
            income = inc,
            expense = exp
        )
    }
}

@Composable
fun TracmioAnimated3DLogo(
    size: Dp = 96.dp,
    showSonarRings: Boolean = true,
    modifier: Modifier = Modifier
) {
    val visuals = LocalTracmioVisuals.current
    val infinite = rememberInfiniteTransition(label = "tracmio_logo_anim")
    val floatOffsetY by infinite.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_float"
    )
    val pulseProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_pulse"
    )

    Box(
        modifier = modifier.size(if (showSonarRings) size + 36.dp else size),
        contentAlignment = Alignment.Center
    ) {
        if (showSonarRings) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = size.toPx() * 0.48f
                val r1 = baseRadius + (size.toPx() * 0.25f * pulseProgress)
                val a1 = (1f - pulseProgress) * 0.45f
                drawCircle(
                    color = visuals.primaryAccent.copy(alpha = a1),
                    radius = r1,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                val secondPhase = (pulseProgress + 0.5f) % 1f
                val r2 = baseRadius + (size.toPx() * 0.25f * secondPhase)
                val a2 = (1f - secondPhase) * 0.35f
                drawCircle(
                    color = visuals.secondaryAccent.copy(alpha = a2),
                    radius = r2,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        val squircle = RoundedCornerShape((size.value * 0.28f).dp)
        Surface(
            modifier = Modifier
                .size(size)
                .offset(y = floatOffsetY.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = squircle,
                    ambientColor = visuals.primaryAccent.copy(alpha = 0.5f),
                    spotColor = visuals.primaryAccent.copy(alpha = 0.65f)
                ),
            shape = squircle,
            color = Color(0xFF04141D),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        visuals.primaryAccent.copy(alpha = 0.80f),
                        visuals.secondaryAccent.copy(alpha = 0.35f)
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                visuals.primaryAccent.copy(alpha = 0.28f),
                                Color(0xFF030E15)
                            )
                        )
                    )
                    .padding((size.value * 0.08f).coerceAtLeast(4f).dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tracmio_launcher_foreground),
                    contentDescription = "Tracmio 3D Liquid Glass Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun TracmioBrandWordmark(
    fontSizeSp: Int = 24,
    showSubtitle: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    val visuals = LocalTracmioVisuals.current
    Column(horizontalAlignment = horizontalAlignment) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = Color(0xFFF4F8FC),
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append("Trac")
                }
                withStyle(
                    SpanStyle(
                        color = visuals.primaryAccent,
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append("mio")
                }
            },
            fontSize = fontSizeSp.sp,
            letterSpacing = (-0.5).sp
        )
        if (showSubtitle) {
            Text(
                text = "FINANCE & EXPENSES TRACKER",
                fontSize = (fontSizeSp * 0.38f).coerceAtLeast(9f).sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TracmioAppOpenSplash(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(360))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            visuals.primaryAccent.copy(alpha = 0.22f),
                            Color(0xFF04141E),
                            Color(0xFF02090F)
                        )
                    )
                )
                .clickable { onDismiss() }
                .testTag("tracmio_app_open_splash"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                TracmioAnimated3DLogo(size = 118.dp, showSonarRings = true)
                TracmioBrandWordmark(
                    fontSizeSp = 34,
                    showSubtitle = true,
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(160.dp)
                        .height(5.dp)
                        .clip(CircleShape),
                    color = visuals.primaryAccent,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                Text(
                    text = "100% Offline · Liquid Glass Engine · v1.00",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TracmioWelcomeOverlay(
    initialName: String,
    initialCurrency: String,
    onCompleteWelcome: (name: String, currency: String) -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    var name by remember(initialName) { mutableStateOf(initialName) }
    var selectedCurrency by remember(initialCurrency) {
        mutableStateOf(initialCurrency.ifBlank { "INR" })
    }
    val currencies = listOf(
        "INR" to "INR (₹)",
        "USD" to "USD ($)",
        "EUR" to "EUR (€)",
        "GBP" to "GBP (£)",
        "JPY" to "JPY (¥)",
        "AED" to "AED"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF041824),
                        Color(0xFF030D14),
                        Color(0xFF02070C)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 32.dp)
            .testTag("tracmio_welcome_overlay"),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            accentBorderColor = visuals.primaryAccent,
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TracmioAnimated3DLogo(size = 96.dp, showSonarRings = true)

                TracmioBrandWordmark(
                    fontSizeSp = 30,
                    showSubtitle = true,
                    horizontalAlignment = Alignment.CenterHorizontally
                )

                Text(
                    text = "Welcome to your private, 100% offline-first personal finance & expense studio. Everything stays strictly on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = visuals.primaryAccent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, visuals.primaryAccent.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = visuals.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "100% Offline",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = visuals.secondaryAccent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, visuals.secondaryAccent.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = visuals.secondaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Liquid Glass 3D",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name (Optional)") },
                    placeholder = { Text("e.g., Charan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_name_input")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select Default Currency",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencies.forEach { (code, label) ->
                            val selected = selectedCurrency == code
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCurrency = code },
                                label = { Text(label) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = visuals.primaryAccent.copy(alpha = 0.24f),
                                    selectedLabelColor = visuals.primaryAccent
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = { onCompleteWelcome(name.ifBlank { "Finance Pro" }, selectedCurrency) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("welcome_start_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = visuals.primaryAccent,
                        contentColor = Color(0xFF031B12)
                    )
                ) {
                    Text(
                        text = "Launch Tracmio Dashboard",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun InteractiveSixMonthTrendCard(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val visuals = LocalTracmioVisuals.current
    val points = remember(transactions) { computeSixMonthTrend(transactions) }
    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    val selectedPoint = points.getOrNull(selectedIndex) ?: points.lastOrNull()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("six_month_trend_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Tracmio3DBadge(
                    icon = Icons.Default.Analytics,
                    tint = visuals.primaryAccent,
                    size = 38.dp,
                    iconSize = 20.dp
                )
                Column {
                    Text(
                        text = "6-Month Cash Flow Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any month bar or node to inspect exact values",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = "Interactive Tooltip",
                tint = visuals.primaryAccent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedPoint != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trend_chart_tooltip"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF041722).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, visuals.primaryAccent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedPoint.fullMonthLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = visuals.primaryAccent
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "Net: %s%.2f",
                                currencySymbol,
                                selectedPoint.net
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "In: +%s%.0f", currencySymbol, selectedPoint.income),
                            style = MaterialTheme.typography.labelMedium,
                            color = visuals.incomeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = String.format(Locale.US, "Out: -%s%.0f", currencySymbol, selectedPoint.expense),
                            style = MaterialTheme.typography.labelMedium,
                            color = visuals.expenseColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val maxVal = remember(points) {
            points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(100.0) ?: 100.0
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        if (points.isNotEmpty() && size.width > 0) {
                            val slotWidth = size.width.toFloat() / points.size
                            val idx = (tapOffset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                            selectedIndex = idx
                        }
                    }
                }
                .testTag("six_month_trend_canvas")
        ) {
            val slotW = size.width / points.size.coerceAtLeast(1)
            val chartBottom = size.height - 22.dp.toPx()
            val chartTop = 12.dp.toPx()
            val usableH = (chartBottom - chartTop).coerceAtLeast(10f)

            for (grid in 0..3) {
                val gy = chartTop + (usableH * grid / 3f)
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val netPath = Path()
            points.forEachIndexed { idx, pt ->
                val centerX = slotW * idx + slotW / 2f
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawRoundRect(
                        color = visuals.primaryAccent.copy(alpha = 0.12f),
                        topLeft = Offset(slotW * idx + 4.dp.toPx(), 0f),
                        size = Size(slotW - 8.dp.toPx(), chartBottom + 4.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                }

                val incRatio = (pt.income / maxVal).toFloat().coerceIn(0.03f, 1f)
                val expRatio = (pt.expense / maxVal).toFloat().coerceIn(0.03f, 1f)
                val barW = (slotW * 0.24f).coerceAtMost(16.dp.toPx())

                val incH = usableH * incRatio
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(visuals.incomeColor, visuals.incomeColor.copy(alpha = 0.35f))
                    ),
                    topLeft = Offset(centerX - barW - 2.dp.toPx(), chartBottom - incH),
                    size = Size(barW, incH),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )

                val expH = usableH * expRatio
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(visuals.expenseColor, visuals.expenseColor.copy(alpha = 0.35f))
                    ),
                    topLeft = Offset(centerX + 2.dp.toPx(), chartBottom - expH),
                    size = Size(barW, expH),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )

                val netNodeY = chartBottom - (usableH * ((pt.income.coerceAtLeast(pt.expense)) / maxVal).toFloat().coerceIn(0.08f, 0.95f))
                if (idx == 0) netPath.moveTo(centerX, netNodeY) else netPath.lineTo(centerX, netNodeY)

                drawCircle(
                    color = if (isSelected) Color.White else visuals.primaryAccent,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = Offset(centerX, netNodeY)
                )
            }

            drawPath(
                path = netPath,
                color = visuals.secondaryAccent.copy(alpha = 0.75f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEachIndexed { idx, pt ->
                val active = idx == selectedIndex
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (active) visuals.primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedIndex = idx }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

data class ReportPaletteSpec(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color
)

val REPORT_PALETTES = listOf(
    ReportPaletteSpec("aurora", "Aurora", Color(0xFF22F59B), Color(0xFF18E0D0)),
    ReportPaletteSpec("tracmio", "Tracmio Green", Color(0xFF10B981), Color(0xFF34D399)),
    ReportPaletteSpec("sunset", "Sunset", Color(0xFFFF6B6B), Color(0xFFFFB84D)),
    ReportPaletteSpec("sapphire", "Sapphire", Color(0xFF38BDF8), Color(0xFF818CF8))
)

@Composable
fun ReportsInteractiveChartsCard(
    transactions: List<TransactionEntity>,
    currencySymbol: String
) {
    var chartStyle by remember { mutableIntStateOf(0) }
    var paletteId by remember { mutableStateOf("aurora") }
    val palette = REPORT_PALETTES.find { it.id == paletteId } ?: REPORT_PALETTES.first()
    val trendPoints = remember(transactions) { computeSixMonthTrend(transactions) }
    var selectedIdx by remember(trendPoints) { mutableIntStateOf(trendPoints.lastIndex.coerceAtLeast(0)) }

    val categoryTotals = remember(transactions) {
        transactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .groupBy { it.category.ifBlank { "General" } }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
    }
    val totalExpenseSum = categoryTotals.sumOf { it.value }.coerceAtLeast(1.0)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Interactive Visual Analytics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Switch between 4 chart styles & 4 color palettes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        val styles = listOf(
            "3D Neon Bars",
            "Area Wave",
            "Capsules & Polar",
            "Executive"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            styles.forEachIndexed { index, title ->
                FilterChip(
                    selected = chartStyle == index,
                    onClick = { chartStyle = index },
                    label = { Text(title) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.primary.copy(alpha = 0.22f),
                        selectedLabelColor = palette.primary
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            REPORT_PALETTES.forEach { p ->
                FilterChip(
                    selected = paletteId == p.id,
                    onClick = { paletteId = p.id },
                    label = { Text(p.label) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(p.primary)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (chartStyle) {
            0, 1 -> {
                val pt = trendPoints.getOrNull(selectedIdx)
                if (pt != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF041520),
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = pt.fullMonthLabel,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "In: %s%.0f  •  Out: %s%.0f",
                                    currencySymbol,
                                    pt.income,
                                    currencySymbol,
                                    pt.expense
                                ),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val maxVal = trendPoints.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(100.0) ?: 100.0
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .pointerInput(trendPoints) {
                            detectTapGestures { offset ->
                                val slot = size.width / trendPoints.size.coerceAtLeast(1)
                                selectedIdx = (offset.x / slot).toInt().coerceIn(0, trendPoints.lastIndex)
                            }
                        }
                ) {
                    val slotW = size.width / trendPoints.size.coerceAtLeast(1)
                    val bottomY = size.height - 16.dp.toPx()
                    val usableH = (bottomY - 14.dp.toPx()).coerceAtLeast(10f)

                    if (chartStyle == 0) {
                        trendPoints.forEachIndexed { idx, item ->
                            val cx = slotW * idx + slotW / 2f
                            val barW = (slotW * 0.28f).coerceAtMost(18.dp.toPx())
                            val incH = usableH * (item.income / maxVal).toFloat().coerceIn(0.04f, 1f)
                            val expH = usableH * (item.expense / maxVal).toFloat().coerceIn(0.04f, 1f)

                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(palette.primary, palette.primary.copy(alpha = 0.3f))),
                                topLeft = Offset(cx - barW - 2.dp.toPx(), bottomY - incH),
                                size = Size(barW, incH),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(palette.secondary, palette.secondary.copy(alpha = 0.3f))),
                                topLeft = Offset(cx + 2.dp.toPx(), bottomY - expH),
                                size = Size(barW, expH),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    } else {
                        val wavePath = Path()
                        val fillPath = Path()
                        trendPoints.forEachIndexed { idx, item ->
                            val cx = slotW * idx + slotW / 2f
                            val ratio = (maxOf(item.income, item.expense) / maxVal).toFloat().coerceIn(0.08f, 0.95f)
                            val y = bottomY - usableH * ratio
                            if (idx == 0) {
                                wavePath.moveTo(cx, y)
                                fillPath.moveTo(cx, bottomY)
                                fillPath.lineTo(cx, y)
                            } else {
                                wavePath.lineTo(cx, y)
                                fillPath.lineTo(cx, y)
                            }
                            if (idx == trendPoints.lastIndex) {
                                fillPath.lineTo(cx, bottomY)
                                fillPath.close()
                            }
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(palette.primary.copy(alpha = 0.42f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = wavePath,
                            color = palette.primary,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            2 -> {
                if (categoryTotals.isEmpty()) {
                    Text(
                        text = "Add expenses to see Capsule & Polar category burn breakdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        categoryTotals.take(6).forEach { (cat, amt) ->
                            val pct = (amt / totalExpenseSum).toFloat().coerceIn(0f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        String.format(Locale.US, "%s%.2f (%.0f%%)", currencySymbol, amt, pct * 100f),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = palette.primary,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                val totalIn = transactions.filter { it.type.equals("INCOME", true) }.sumOf { it.amount }
                val totalOut = transactions.filter { it.type.equals("EXPENSE", true) }.sumOf { it.amount }
                val savingsRate = if (totalIn > 0) ((totalIn - totalOut) / totalIn * 100.0) else 0.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = palette.primary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = palette.primary)
                            Text("Savings Rate", style = MaterialTheme.typography.labelSmall)
                            Text(
                                String.format(Locale.US, "%.1f%%", savingsRate),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.primary
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = palette.secondary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = palette.secondary)
                            Text("Net Cash Flow", style = MaterialTheme.typography.labelSmall)
                            Text(
                                String.format(Locale.US, "%s%.2f", currencySymbol, totalIn - totalOut),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
