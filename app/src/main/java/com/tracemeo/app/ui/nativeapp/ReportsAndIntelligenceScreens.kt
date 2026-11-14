package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeIntelligenceCalculators
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.NativeTracmioUiState
import com.tracemeo.app.data.SpendingChartWindow
import java.util.*

@Composable
fun ReportsAndIntelligenceScreens(
    subRoute: String,
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    onSelectSubRoute: (String) -> Unit
) {
    val curr = settings.currencySymbol
    val tabs = listOf(
        "reports" to "📈 Reports & Charts",
        "financial_xray" to "🩻 Financial X-Ray",
        "money_story" to "📖 Money Story",
        "spending_dna" to "🧬 Spending DNA",
        "calendar" to "📅 Calendar & SIM"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { (routeKey, label) ->
                    val sel = subRoute == routeKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) palette.primary else palette.surface)
                            .border(1.dp, if (sel) palette.primary else palette.surfaceBorder, RoundedCornerShape(16.dp))
                            .clickable { onSelectSubRoute(routeKey) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        when (subRoute) {
            "financial_xray" -> {
                item {
                    val xray = remember(state.accounts, state.transactions, state.budgets, state.goals, state.loans, state.subscriptions) {
                        NativeIntelligenceCalculators.computeFinancialXRay(
                            accounts = state.accounts,
                            transactions = state.transactions,
                            budgets = state.budgets,
                            goals = state.goals,
                            loans = state.loans,
                            loanPayments = state.loanPayments,
                            subscriptions = state.subscriptions,
                            academicEvents = state.academicEvents
                        )
                    }
                    LiquidGlassComponents(settings = settings, palette = palette) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("🩻 10-Point Financial X-Ray", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.primary)
                                Text("Diagnostic Health Grade: ${xray.grade}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            }
                            Text("${xray.overallScore}/100", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = palette.primary)
                        }
                        xray.metrics.forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(palette.surface).padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.name, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                    Text(m.summary, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                                Text("${m.score}% · ${m.status}", fontWeight = FontWeight.ExtraBold, color = palette.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            "spending_dna", "money_story" -> {
                item {
                    val split = remember(state.transactions) {
                        NativeIntelligenceCalculators.compute503020Breakdown(state.transactions)
                    }
                    LiquidGlassComponents(settings = settings, palette = palette) {
                        Text(
                            if (subRoute == "spending_dna") "🧬 50/30/20 Spending DNA Breakdown" else "📖 Monthly Money Story Narrative",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.primary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(palette.surface).padding(12.dp)) {
                                Column {
                                    Text("50% Needs", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    Text("$curr${"%,.0f".format(split.needs)}", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                                }
                            }
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(palette.surface).padding(12.dp)) {
                                Column {
                                    Text("30% Wants", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    Text("$curr${"%,.0f".format(split.wants)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFFB7185))
                                }
                            }
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(palette.surface).padding(12.dp)) {
                                Column {
                                    Text("20% Savings", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    Text("$curr${"%,.0f".format(split.savings)}", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                                }
                            }
                        }
                    }
                }
            }
            "calendar" -> {
                item {
                    var simName by remember { mutableStateOf("Jio / Airtel Prepaid") }
                    var simAmt by remember { mutableStateOf("299") }
                    var simDays by remember { mutableStateOf("28") }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LiquidGlassComponents(settings = settings, palette = palette) {
                            Text("🔥 Calendar No-Spend Streak & Milestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.primary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("🏅 3d Saver", "🔥 7d Streak", "💎 14d Pro", "🏆 ₹1k+ Saved").forEach { badge ->
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(palette.surface).border(1.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(badge, style = MaterialTheme.typography.labelSmall, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        LiquidGlassComponents(settings = settings, palette = palette) {
                            Text("📶 SIM / WiFi Recharge Countdown (28 / 56 / 84d)", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = simName, onValueChange = { simName = it }, label = { Text("Operator / WiFi") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = simAmt, onValueChange = { simAmt = it }, label = { Text("Plan ($curr)") }, singleLine = true, modifier = Modifier.width(95.dp))
                                OutlinedTextField(value = simDays, onValueChange = { simDays = it }, label = { Text("Days") }, singleLine = true, modifier = Modifier.width(80.dp))
                            }
                            Button(
                                onClick = {
                                    val amt = simAmt.toDoubleOrNull() ?: 299.0
                                    val days = simDays.toIntOrNull() ?: 28
                                    viewModel.addSubscription(simName.trim(), amt, "${days}d", isSim = true, validityDays = days)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("+ Track SIM / WiFi Pack", color = palette.background, fontWeight = FontWeight.ExtraBold)
                            }
                            state.subscriptions.forEach { sub ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(palette.surface).padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(sub.name, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                        Text("$curr${"%,.0f".format(sub.amount)} · ${sub.billingCycle}", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                    }
                                    Text("✕", color = Color(0xFFFB7185), modifier = Modifier.clickable { viewModel.deleteSubscription(sub) })
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Reports default view
                item {
                    var chartStyle by remember { mutableStateOf("📊 3D Neon Bars") }
                    val distribution = remember(state.transactions, state.categories) {
                        NativeLedgerCalculator.computeSpendingDistribution(state.transactions, state.categories, SpendingChartWindow.MONTHLY)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LiquidGlassComponents(settings = settings, palette = palette) {
                            Text("📈 4-Style Visual Chart Switcher", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("📊 3D Neon Bars", "🌊 Area Wave", "💊 Capsules & Polar", "⚡ Executive").forEach { st ->
                                    val sel = chartStyle == st
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (sel) palette.primary else palette.surface)
                                            .clickable { chartStyle = st }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(st, style = MaterialTheme.typography.labelSmall, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (distribution.isEmpty()) {
                                Text("Add expenses to see category distribution charts.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            } else {
                                val maxVal = (distribution.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                                distribution.entries.forEach { (cat, amt) ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(cat, style = MaterialTheme.typography.labelMedium, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                            Text("$curr${"%,.0f".format(amt)}", style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.ExtraBold)
                                        }
                                        LinearProgressIndicator(
                                            progress = { (amt / maxVal).toFloat().coerceIn(0.05f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = palette.primary,
                                            trackColor = palette.surfaceElevated
                                        )
                                    }
                                }
                            }
                        }

                        // Merchant & UPI App Breakdown + Weekend vs Weekday Radar + Side-by-Side Month Comparison
                        LiquidGlassComponents(settings = settings, palette = palette) {
                            Text("💳 Merchant & UPI App Breakdown", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                            val upiMap = state.transactions.filter { it.type.equals("EXPENSE", true) }
                                .groupBy { it.upiApp.ifBlank { it.paymentMethod } }
                            upiMap.forEach { (method, list) ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$method (${list.size} txns)", color = palette.textPrimary)
                                    Text("$curr${"%,.0f".format(list.sumOf { it.amount })}", color = palette.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        LiquidGlassComponents(settings = settings, palette = palette) {
                            Text("📅 Weekend vs. Weekday Spend Radar & Month Comparison", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                            var weekendSpend = 0.0
                            var weekdaySpend = 0.0
                            state.transactions.filter { it.type.equals("EXPENSE", true) }.forEach { tx ->
                                val dow = Calendar.getInstance().apply { timeInMillis = tx.timestamp }.get(Calendar.DAY_OF_WEEK)
                                if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) weekendSpend += tx.amount else weekdaySpend += tx.amount
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Weekday Spend: $curr${"%,.0f".format(weekdaySpend)}", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                Text("Weekend Spend: $curr${"%,.0f".format(weekendSpend)}", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
