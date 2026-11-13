package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.LiquidGlassSettings
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeIntelligenceCalculators
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.TransactionDateTime
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val REPORT_PALETTES = listOf(
    "Aurora" to listOf(Color(0xFF00D6A3), Color(0xFF06B6D4), Color(0xFFF43F5E), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFF3B82F6)),
    "Tracmio Green" to listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF059669), Color(0xFF06B6D4), Color(0xFF14B8A6), Color(0xFF6EE7B7)),
    "Sunset" to listOf(Color(0xFFF97316), Color(0xFFF43F5E), Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFFA855F7), Color(0xFFEF4444)),
    "Sapphire" to listOf(Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFF0EA5E9), Color(0xFF22D3EE))
)

@Composable
fun ReportsScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    var periodTab by remember { mutableStateOf("ALL") } // ALL, THIS_MONTH, LAST_MONTH, THREE_MO, CUSTOM
    var customFrom by remember { mutableStateOf("") }
    var customTo by remember { mutableStateOf("") }
    var visualStyle by remember { mutableStateOf("3D Neon Bars") } // 3D Neon Bars, Area Wave, Capsules & Polar, Executive
    var paletteIndex by remember { mutableIntStateOf(0) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var inspectedMonthIndex by remember { mutableIntStateOf(-1) }

    val chartColors = REPORT_PALETTES[paletteIndex].second

    val periodTransactions = remember(state.transactions, periodTab, customFrom, customTo) {
        val now = Calendar.getInstance()
        val curYm = NativeLedgerCalculator.monthKey(now.timeInMillis)
        val lastCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastYm = NativeLedgerCalculator.monthKey(lastCal.timeInMillis)
        val threeMoCutoff = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis

        val parsedFrom = customFrom.takeIf { it.length >= 8 }?.let { TransactionDateTime.startOfDayMillis(it) }
        val parsedTo = customTo.takeIf { it.length >= 8 }?.let { TransactionDateTime.endOfDayMillis(it) }

        state.transactions.filter { tx ->
            when (periodTab) {
                "THIS_MONTH" -> NativeLedgerCalculator.monthKey(tx.timestamp) == curYm
                "LAST_MONTH" -> NativeLedgerCalculator.monthKey(tx.timestamp) == lastYm
                "THREE_MO" -> tx.timestamp >= threeMoCutoff
                "CUSTOM" -> (parsedFrom == null || tx.timestamp >= parsedFrom) && (parsedTo == null || tx.timestamp <= parsedTo)
                else -> true
            }
        }
    }

    val periodIncome = remember(periodTransactions) {
        periodTransactions.filter { it.type.equals("INCOME", true) }.sumOf { it.amount }
    }
    val periodExpense = remember(periodTransactions) {
        periodTransactions.filter { it.type.equals("EXPENSE", true) }.sumOf { it.amount }
    }
    val periodNet = periodIncome - periodExpense
    val savingsRate = if (periodIncome > 0) ((periodNet / periodIncome) * 100.0).coerceIn(-100.0, 100.0) else 0.0

    val expenseByCat = remember(periodTransactions) {
        periodTransactions.filter { it.type.equals("EXPENSE", true) }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }
    val monthlyBuckets = remember(state.transactions) {
        NativeIntelligenceCalculators.buildMonthlyReportBuckets(state.transactions, 6)
    }
    val merchantUpi = remember(periodTransactions) {
        periodTransactions.filter { it.type.equals("EXPENSE", true) }
            .groupBy { it.upiApp.ifBlank { it.paymentMethod } }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }
    val weekendRadar = remember(periodTransactions) {
        NativeLedgerCalculator.computeWeekendRadar(periodTransactions)
    }
    val monthComp = remember(state.transactions) {
        NativeLedgerCalculator.computeMonthComparison(state.transactions)
    }
    val incomeSplit = remember(periodTransactions) {
        NativeLedgerCalculator.computeIncomeSourceSplit(periodTransactions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }
                Text("📊 Reports & Analytics", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            }
            OutlinedButton(
                onClick = {
                    val lines = buildList {
                        add("TRACMIO ANALYTICS REPORT · $periodTab" to true)
                        add("Income: ${MoneyPaise.formatCurrency(periodIncome)} | Expense: ${MoneyPaise.formatCurrency(periodExpense)}" to false)
                        add("Net Cashflow: ${MoneyPaise.formatCurrency(periodNet)} (${savingsRate.toInt()}% savings rate)" to true)
                        add("INCOME SOURCE SPLIT" to true)
                        add("• Family / Allowance: ${MoneyPaise.formatCurrency(incomeSplit.familyAllowance)} (${String.format("%.1f", incomeSplit.familyPercentage)}%)" to false)
                        add("• Own Earnings / Freelance: ${MoneyPaise.formatCurrency(incomeSplit.ownEarnings)} (${String.format("%.1f", incomeSplit.ownPercentage)}%)" to false)
                        add("CATEGORY BREAKDOWN" to true)
                        expenseByCat.forEach { (c, v) -> add("• $c: ${MoneyPaise.formatCurrency(v)}" to false) }
                        add("MERCHANT & UPI BREAKDOWN" to true)
                        merchantUpi.forEach { (m, v) -> add("• $m: ${MoneyPaise.formatCurrency(v)}" to false) }
                    }
                    hostCallbacks.printCustomReport("Tracmio_Reports_$periodTab", "Tracmio Analytics Report", lines)
                },
                modifier = Modifier.testTag("export_reports_pdf_btn")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export PDF")
            }
        }

        // Period Filter Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(
                "ALL" to "All Time",
                "THIS_MONTH" to "This Month",
                "LAST_MONTH" to "Last Month",
                "THREE_MO" to "3 Months",
                "CUSTOM" to "Custom Range"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = periodTab == key,
                    onClick = { periodTab = key; selectedCategoryFilter = null },
                    label = { Text(label) }
                )
            }
        }

        if (periodTab == "CUSTOM") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = customFrom, onValueChange = { customFrom = it }, label = { Text("From YYYY-MM-DD") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = customTo, onValueChange = { customTo = it }, label = { Text("To YYYY-MM-DD") }, modifier = Modifier.weight(1f))
            }
        }

        // Visual Styles + 4 Report Color Palettes
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("3D Neon Bars", "Area Wave", "Capsules & Polar", "Executive").forEach { style ->
                FilterChip(selected = visualStyle == style, onClick = { visualStyle = style }, label = { Text(style) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            REPORT_PALETTES.forEachIndexed { idx, (name, _) ->
                FilterChip(selected = paletteIndex == idx, onClick = { paletteIndex = idx }, label = { Text("🎨 $name") })
            }
        }

        // Net Cashflow Hero & Savings Rate
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("NET CASHFLOW", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text(
                            displayAmount(periodNet, settings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (periodNet >= 0) chartColors[0] else Color(0xFFF43F5E)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Savings Rate", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text("${savingsRate.toInt()}%", style = MaterialTheme.typography.titleLarge, color = chartColors[1], fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("In: ${displayAmount(periodIncome, settings)}", color = chartColors[0], style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("Out: ${displayAmount(periodExpense, settings)}", color = Color(0xFFF43F5E), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("${periodTransactions.size} txns", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Feature #10: Parent / Family Pocket-Money vs. Own Earnings Split Card
        GlassCard(
            settings = settings,
            modifier = Modifier.fillMaxWidth().testTag("reports_income_source_split_card")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "👨‍👩‍👧 Family / Pocket-Money vs. 💼 Own Earnings Split",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text("👨‍👩‍👧 Family / Allowance", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text(
                            displayAmount(incomeSplit.familyAllowance, settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.secondary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${String.format("%.0f", incomeSplit.familyPercentage)}% of income",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text("💼 Own Earnings / Freelance", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text(
                            displayAmount(incomeSplit.ownEarnings, settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${String.format("%.0f", incomeSplit.ownPercentage)}% of income",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { (incomeSplit.ownPercentage / 100f).coerceIn(0f, 1f) },
                    color = palette.primary,
                    trackColor = palette.secondary.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Interactive 6-Month Cashflow Chart
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("6-Month Cashflow ($visualStyle)", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Canvas(modifier = Modifier.fillMaxWidth().height(155.dp)) {
                    val maxVal = monthlyBuckets.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
                    val slotW = size.width / monthlyBuckets.size.coerceAtLeast(1)
                    if (visualStyle == "Area Wave") {
                        val incPath = Path()
                        val expPath = Path()
                        monthlyBuckets.forEachIndexed { idx, b ->
                            val x = idx * slotW + slotW / 2f
                            val iy = size.height - ((b.income / maxVal).toFloat() * size.height * 0.85f)
                            val ey = size.height - ((b.expense / maxVal).toFloat() * size.height * 0.85f)
                            if (idx == 0) { incPath.moveTo(x, iy); expPath.moveTo(x, ey) }
                            else { incPath.lineTo(x, iy); expPath.lineTo(x, ey) }
                            drawCircle(chartColors[0], radius = 6f, center = Offset(x, iy))
                            drawCircle(chartColors[2], radius = 6f, center = Offset(x, ey))
                        }
                        drawPath(incPath, color = chartColors[0], style = Stroke(width = 5f))
                        drawPath(expPath, color = chartColors[2], style = Stroke(width = 4f))
                    } else {
                        monthlyBuckets.forEachIndexed { idx, b ->
                            val x = idx * slotW + slotW * 0.16f
                            val barW = slotW * 0.3f
                            val ih = ((b.income / maxVal).toFloat() * size.height * 0.86f).coerceAtLeast(4f)
                            val eh = ((b.expense / maxVal).toFloat() * size.height * 0.86f).coerceAtLeast(4f)
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(chartColors[0], chartColors[1].copy(alpha = 0.7f))),
                                topLeft = Offset(x, size.height - ih),
                                size = Size(barW, ih),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                            drawRoundRect(
                                color = chartColors[2].copy(alpha = 0.9f),
                                topLeft = Offset(x + barW + 4f, size.height - eh),
                                size = Size(barW, eh),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    monthlyBuckets.forEachIndexed { idx, b ->
                        Text(
                            text = b.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (inspectedMonthIndex == idx) chartColors[0] else palette.textSecondary,
                            fontWeight = if (inspectedMonthIndex == idx) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                                inspectedMonthIndex = if (inspectedMonthIndex == idx) -1 else idx
                            }.padding(4.dp)
                        )
                    }
                }
                if (inspectedMonthIndex in monthlyBuckets.indices) {
                    val m = monthlyBuckets[inspectedMonthIndex]
                    Text(
                        "${m.yearMonth} · In: ${displayAmount(m.income, settings)} · Out: ${displayAmount(m.expense, settings)} · Net: ${displayAmount(m.net, settings)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = chartColors[0]
                    )
                }
            }
        }

        // Interactive Category Distribution
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Category Distribution ($visualStyle)", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                val totalExp = expenseByCat.sumOf { it.value }.coerceAtLeast(1.0)
                if (expenseByCat.isEmpty()) {
                    Text("No expenses recorded in this period.", color = palette.textSecondary)
                } else {
                    if (visualStyle == "Capsules & Polar") {
                        Canvas(modifier = Modifier.fillMaxWidth().height(175.dp)) {
                            var startAngle = -90f
                            val diameter = size.minDimension * 0.78f
                            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                            expenseByCat.take(6).forEachIndexed { idx, entry ->
                                val sweep = ((entry.value / totalExp) * 360.0).toFloat()
                                drawArc(
                                    color = chartColors[idx % chartColors.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(diameter, diameter),
                                    style = Stroke(width = 34f, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }
                    } else if (visualStyle == "Executive") {
                        Canvas(modifier = Modifier.fillMaxWidth().height(175.dp)) {
                            val entries = expenseByCat.take(5)
                            val maxV = (entries.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val r = size.minDimension * 0.4f
                            val path = Path()
                            entries.forEachIndexed { i, entry ->
                                val angle = ((2 * Math.PI * i) / entries.size) - Math.PI / 2
                                val ratio = (entry.value / maxV).toFloat()
                                val px = cx + (cos(angle) * r * ratio).toFloat()
                                val py = cy + (sin(angle) * r * ratio).toFloat()
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                drawLine(palette.surfaceBorder, Offset(cx, cy), Offset(cx + (cos(angle) * r).toFloat(), cy + (sin(angle) * r).toFloat()), 2f)
                            }
                            path.close()
                            drawPath(path, chartColors[0].copy(alpha = 0.35f))
                            drawPath(path, chartColors[0], style = Stroke(width = 3f))
                        }
                    }

                    expenseByCat.take(8).forEachIndexed { idx, (cat, amt) ->
                        val ratio = (amt / totalExp).toFloat().coerceIn(0f, 1f)
                        val isSelected = selectedCategoryFilter == cat
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) chartColors[idx % chartColors.size].copy(alpha = 0.14f) else Color.Transparent)
                                .clickable { selectedCategoryFilter = if (isSelected) null else cat }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$cat ${if (isSelected) "(filtered)" else ""}", color = palette.textPrimary, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${displayAmount(amt, settings)} (${(ratio * 100).toInt()}%)",
                                    color = chartColors[idx % chartColors.size],
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = chartColors[idx % chartColors.size],
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }

        // Filtered Transactions when a category bar is tapped
        selectedCategoryFilter?.let { cat ->
            val filtered = periodTransactions.filter { it.category.equals(cat, true) }.sortedByDescending { it.timestamp }
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$cat Entries (${filtered.size})", style = MaterialTheme.typography.titleSmall, color = chartColors[0])
                        Text("Clear", color = palette.secondary, modifier = Modifier.clickable { selectedCategoryFilter = null })
                    }
                    filtered.take(15).forEach { tx ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${TransactionDateTime.formatDateInput(tx.timestamp)} · ${tx.note.ifBlank { tx.paymentMethod }}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, modifier = Modifier.weight(1f))
                            Text(displayAmount(tx.amount, settings), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        }
                    }
                }
            }
        }

        // Side-by-Side Month Comparison
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📅 Side-by-Side Month Comparison", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(palette.surfaceElevated).padding(10.dp)) {
                        Text(monthComp.previousMonthLabel, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text("In: ${displayAmount(monthComp.previousIncome, settings)}", color = chartColors[0], style = MaterialTheme.typography.bodySmall)
                        Text("Out: ${displayAmount(monthComp.previousExpense, settings)}", color = Color(0xFFF43F5E), style = MaterialTheme.typography.bodySmall)
                    }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(palette.surfaceElevated).padding(10.dp)) {
                        Text(monthComp.currentMonthLabel, style = MaterialTheme.typography.labelSmall, color = chartColors[0])
                        Text("In: ${displayAmount(monthComp.currentIncome, settings)}", color = chartColors[0], style = MaterialTheme.typography.bodySmall)
                        Text("Out: ${displayAmount(monthComp.currentExpense, settings)}", color = Color(0xFFF43F5E), style = MaterialTheme.typography.bodySmall)
                    }
                }
                val sign = if (monthComp.expenseDeltaPct >= 0) "+" else ""
                Text("Expense change vs last month: $sign${String.format(Locale.US, "%.1f", monthComp.expenseDeltaPct)}%", style = MaterialTheme.typography.labelMedium, color = palette.secondary)
            }
        }

        // Account & Person Breakdown
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🏦 Account Balances Summary", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                state.accounts.filter { !it.isArchived }.forEach { acc ->
                    val bal = NativeLedgerCalculator.accountBalance(acc, state.transactions)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${acc.name} (${acc.accountType})", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                        Text(displayAmount(bal, settings), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = chartColors[0])
                    }
                }
                if (state.personSummaries.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("👥 People Net Positions", style = MaterialTheme.typography.titleSmall, color = palette.secondary)
                    state.personSummaries.forEach { ps ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ps.person.name, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                            Text(
                                if (ps.net >= 0) "+${displayAmount(ps.net, settings)}" else displayAmount(ps.net, settings),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (ps.net >= 0) chartColors[0] else Color(0xFFF43F5E)
                            )
                        }
                    }
                }
            }
        }

        // Weekend vs Weekday Radar & Merchant/UPI Breakdown
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassCard(settings = settings, modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🛰️ Weekend Radar", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                    Text("Weekday avg: ${displayAmount(weekendRadar.weekdayAvgPerDay, settings)}/d", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                    Text("Weekend avg: ${displayAmount(weekendRadar.weekendAvgPerDay, settings)}/d", style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                }
            }
            GlassCard(settings = settings, modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📲 Merchant & UPI", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                    if (merchantUpi.isEmpty()) Text("No UPI txns yet", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    merchantUpi.take(4).forEach { (app, amt) ->
                        Text("$app: ${displayAmount(amt, settings)}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun MoneyStoryScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    val streak = remember(state.transactions) { NativeLedgerCalculator.computeNoSpendStreakDays(state.transactions) }
    val split503020 = remember(state.transactions) { NativeIntelligenceCalculators.compute503020Breakdown(state.transactions) }
    val topCat = state.transactions.filter { it.type.equals("EXPENSE", true) }.groupBy { it.category }.maxByOrNull { it.value.sumOf { tx -> tx.amount } }
    val biggestTx = state.transactions.filter { it.type.equals("EXPENSE", true) }.maxByOrNull { it.amount }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }
                Text("📖 Your Money Story", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            }
            OutlinedButton(onClick = {
                val lines = listOf(
                    "TRACMIO MONEY STORY" to true,
                    "Total Income: ${MoneyPaise.formatCurrency(state.totalIncome)}" to false,
                    "Total Expense: ${MoneyPaise.formatCurrency(state.totalExpense)}" to false,
                    "Current No-Spend Streak: $streak days" to true,
                    "50/30/20 Needs: ${split503020.needsPct}% (${MoneyPaise.formatCurrency(split503020.needs)})" to false,
                    "50/30/20 Wants: ${split503020.wantsPct}% (${MoneyPaise.formatCurrency(split503020.wants)})" to false,
                    "50/30/20 Savings: ${split503020.savingsPct}% (${MoneyPaise.formatCurrency(split503020.savings)})" to false
                )
                hostCallbacks.printCustomReport("Tracmio_Money_Story", "Tracmio Money Story", lines)
            }) {
                Text("Export Story PDF")
            }
        }
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Chapter 1 · Cashflow", style = MaterialTheme.typography.titleMedium, color = palette.primary)
                Text("Across ${state.transactions.size} entries, you earned ${displayAmount(state.totalIncome, settings)} and spent ${displayAmount(state.totalExpense, settings)}.", color = palette.textPrimary)
                Text("Current No-Spend Streak: $streak days 🔥", color = palette.secondary, fontWeight = FontWeight.Bold)
                topCat?.let {
                    Text("Your #1 category is ${it.key} (${displayAmount(it.value.sumOf { x -> x.amount }, settings)}).", color = palette.textPrimary)
                }
                biggestTx?.let {
                    Text("Largest single expense: ${it.category} — ${displayAmount(it.amount, settings)}.", color = palette.textSecondary)
                }
            }
        }
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Chapter 2 · 50 / 30 / 20 Allocation", style = MaterialTheme.typography.titleMedium, color = palette.primary)
                Text("Needs (${split503020.needsPct}% · Target 50%): ${displayAmount(split503020.needs, settings)}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                LinearProgressIndicator(progress = { split503020.needsPct / 100f }, color = palette.primary, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)))
                Text("Wants (${split503020.wantsPct}% · Target 30%): ${displayAmount(split503020.wants, settings)}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                LinearProgressIndicator(progress = { split503020.wantsPct / 100f }, color = Color(0xFFF59E0B), modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)))
                Text("Savings (${split503020.savingsPct}% · Target 20%): ${displayAmount(split503020.savings, settings)}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                LinearProgressIndicator(progress = { split503020.savingsPct / 100f }, color = Color(0xFF10B981), modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)))
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun SpendingDnaScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    val radar = remember(state.transactions) { NativeLedgerCalculator.computeWeekendRadar(state.transactions) }
    val archetype = when {
        state.totalIncome > state.totalExpense * 1.4 && state.totalIncome > 0 -> "Super Saver 💎"
        radar.weekendAvgPerDay > radar.weekdayAvgPerDay * 1.3 -> "Weekend Explorer 🎉"
        else -> "Balanced Planner ⚖️"
    }
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayTotals = remember(state.transactions) {
        val map = IntArray(7)
        val totals = DoubleArray(7)
        val cal = Calendar.getInstance()
        state.transactions.filter { it.type.equals("EXPENSE", true) }.forEach { tx ->
            cal.timeInMillis = tx.timestamp
            val idx = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                else -> 6
            }
            totals[idx] += tx.amount
            map[idx]++
        }
        totals.mapIndexed { i, sum -> dayNames[i] to (if (map[i] > 0) sum else 0.0) }
    }
    val maxDay = dayTotals.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }
                Text("🧬 Spending DNA", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            }
            OutlinedButton(onClick = {
                val lines = buildList {
                    add("SPENDING DNA PROFILE" to true)
                    add("Archetype: $archetype" to true)
                    add("Weekend Avg: ${MoneyPaise.formatCurrency(radar.weekendAvgPerDay)}/day" to false)
                    add("Weekday Avg: ${MoneyPaise.formatCurrency(radar.weekdayAvgPerDay)}/day" to false)
                    dayTotals.forEach { (d, amt) -> add("$d Spend: ${MoneyPaise.formatCurrency(amt)}" to false) }
                }
                hostCallbacks.printCustomReport("Tracmio_Spending_DNA", "Tracmio Spending DNA", lines)
            }) { Text("Export DNA PDF") }
        }
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("YOUR ARCHETYPE", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                Text(archetype, style = MaterialTheme.typography.headlineSmall, color = palette.primary, fontWeight = FontWeight.ExtraBold)
                Text("Weekend Avg: ${displayAmount(radar.weekendAvgPerDay, settings)}/day vs Weekday Avg: ${displayAmount(radar.weekdayAvgPerDay, settings)}/day", color = palette.textPrimary)
            }
        }
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Day-of-Week Spend Rhythm (Mon–Sun)", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                dayTotals.forEach { (day, amt) ->
                    val pct = (amt / maxDay).toFloat().coerceIn(0f, 1f)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(day, modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                        LinearProgressIndicator(progress = { pct }, color = if (day in listOf("Sat", "Sun")) palette.secondary else palette.primary, modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)))
                        Text(displayAmount(amt, settings), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun GlobalMultiSearchScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onNavigateRoute: (String) -> Unit,
    onBack: () -> Unit
) {
    var q by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Text("🔍 Global Vault Search", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            placeholder = { Text("Search across transactions, accounts, goals, people, trackers...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("global_search_input")
        )
        if (q.isNotBlank()) {
            state.accounts.filter { it.name.contains(q, true) }.forEach { acc ->
                GlassCard(settings = settings, onClick = { onNavigateRoute("accounts") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Account: ${acc.name} (${acc.accountType})", modifier = Modifier.padding(12.dp), color = palette.primary)
                }
            }
            state.transactions.filter { it.category.contains(q, true) || it.note.contains(q, true) }.take(15).forEach { tx ->
                GlassCard(settings = settings, onClick = { onNavigateRoute("transactions") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Txn: ${tx.category} · ${displayAmount(tx.amount, settings)} · ${tx.note}", modifier = Modifier.padding(12.dp), color = palette.textPrimary)
                }
            }
            state.goals.filter { it.name.contains(q, true) }.forEach { g ->
                GlassCard(settings = settings, onClick = { onNavigateRoute("planning") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Goal: ${g.name}", modifier = Modifier.padding(12.dp), color = palette.secondary)
                }
            }
            state.persons.filter { it.name.contains(q, true) }.forEach { p ->
                GlassCard(settings = settings, onClick = { onNavigateRoute("people") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Person: ${p.name}", modifier = Modifier.padding(12.dp), color = palette.textPrimary)
                }
            }
            state.customTrackers.filter { it.name.contains(q, true) || it.customTypeName.contains(q, true) }.forEach { tr ->
                GlassCard(settings = settings, onClick = { onNavigateRoute("trackers") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tracker: ${tr.name} (${tr.customTypeName})", modifier = Modifier.padding(12.dp), color = palette.primary)
                }
            }
        }
    }
}
