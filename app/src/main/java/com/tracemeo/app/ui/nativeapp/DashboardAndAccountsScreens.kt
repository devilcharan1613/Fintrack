package com.tracemeo.app.ui.nativeapp

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.AccountAllocation
import com.tracemeo.app.data.LiquidGlassSettings
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeIntelligenceCalculators
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.PendingRefundItem
import com.tracemeo.app.data.SpendingChartWindow
import com.tracemeo.app.data.UpiSmsDraft
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private val DASHBOARD_CHART_COLORS = listOf(
    Color(0xFF00D6A3),
    Color(0xFF06B6D4),
    Color(0xFFF43F5E),
    Color(0xFFF59E0B),
    Color(0xFF8B5CF6),
    Color(0xFF3B82F6),
    Color(0xFF10B981),
    Color(0xFFEC4899)
)

@Composable
fun DashboardScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onOpenQuickTransaction: (String) -> Unit,
    onOpenSmsDraft: (UpiSmsDraft) -> Unit,
    onOpenCashCounter: () -> Unit,
    onOpenTransfer: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onShowReceipt: (TransactionEntity) -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    val safeToSpend = remember(state.accounts, state.transactions) {
        NativeLedgerCalculator.computeSafeToSpend(state.accounts, state.transactions)
    }
    val todaySummary = remember(state.accounts, state.transactions) {
        NativeLedgerCalculator.computeTodayExpenseSummary(state.accounts, state.transactions)
    }
    val monthlyPace = remember(state.accounts, state.transactions, state.budgets) {
        NativeIntelligenceCalculators.computeMonthlyPace(state.accounts, state.transactions, state.budgets)
    }
    val runwayMonths = remember(state.accounts, state.transactions) {
        NativeLedgerCalculator.computeEmergencyRunwayMonths(state.accounts, state.transactions)
    }
    val warrantyTxns = remember(state.transactions) {
        state.transactions.filter { it.warrantyDate.isNotBlank() }.take(3)
    }
    val totalBorrowed = remember(state.transactions) {
        state.transactions.filter { it.type.equals("BORROW", true) }.sumOf { it.amount }
    }
    val totalLent = remember(state.transactions) {
        state.transactions.filter { it.type.equals("LEND", true) }.sumOf { it.amount }
    }
    val totalDebt = remember(state.loans, state.loanPayments) {
        state.loans.sumOf { MoneyPaise.toRupees(NativeLedgerCalculator.loanRemainingPaise(it, state.loanPayments)) }
    }
    val netWorth = remember(state.totalBalance, totalDebt) { state.totalBalance - totalDebt }

    var chartWindow by remember { mutableStateOf(SpendingChartWindow.MONTHLY) }
    var chartMode by remember { mutableStateOf("DONUT") } // DONUT or BAR
    var selectedSliceCategory by remember { mutableStateOf<String?>(null) }
    val chartDistribution = remember(state.transactions, state.categories, chartWindow) {
        NativeLedgerCalculator.computeSpendingDistribution(
            transactions = state.transactions,
            categories = state.categories,
            window = chartWindow
        )
    }

    var dashActivityFilter by remember { mutableStateOf("All") } // All, Today, Yesterday, Cash, UPI
    val filteredRecentActivity = remember(state.transactions, dashActivityFilter) {
        val nowMs = System.currentTimeMillis()
        val todayKey = NativeLedgerCalculator.dayKey(nowMs)
        val yestCal = Calendar.getInstance().apply { timeInMillis = nowMs; add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayKey = NativeLedgerCalculator.dayKey(yestCal.timeInMillis)
        state.transactions.filter { tx ->
            when (dashActivityFilter.uppercase(Locale.US)) {
                "TODAY" -> NativeLedgerCalculator.dayKey(tx.timestamp) == todayKey
                "YESTERDAY" -> NativeLedgerCalculator.dayKey(tx.timestamp) == yesterdayKey
                "CASH" -> tx.paymentMethod.equals("Cash", ignoreCase = true)
                "UPI" -> tx.paymentMethod.equals("UPI", ignoreCase = true) || tx.upiApp.isNotBlank()
                else -> true
            }
        }.take(14)
    }

    val pendingRefundItems = remember(state.pendingRefunds) {
        state.pendingRefunds.filter { !it.isReceived }
    }
    var showAddChipDialog by remember { mutableStateOf(false) }
    var showAddAccountQuick by remember { mutableStateOf(false) }
    var showSmsPasteDialog by remember { mutableStateOf(false) }
    var showAddRefundDialog by remember { mutableStateOf(false) }
    var editingRefund by remember { mutableStateOf<PendingRefundItem?>(null) }
    var receivingRefund by remember { mutableStateOf<PendingRefundItem?>(null) }
    var refundTitle by remember { mutableStateOf("") }
    var refundSource by remember { mutableStateOf("Amazon") }
    var refundAmount by remember { mutableStateOf("") }
    var refundExpectedNote by remember { mutableStateOf("Tomorrow") }
    var smsInputText by remember { mutableStateOf("") }
    var chipIcon by remember { mutableStateOf("☕") }
    var chipLabel by remember { mutableStateOf("") }
    var chipAmount by remember { mutableStateOf("") }
    var chipCategory by remember { mutableStateOf("Food & Chai") }

    val dateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Safe-to-Spend Daily Limit Banner
        item {
            GlassCard(
                settings = settings,
                onClick = { onNavigateRoute("accounts") },
                modifier = Modifier.fillMaxWidth().testTag("safe_to_spend_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SAFE TO SPEND TODAY (${safeToSpend.remainingDaysInMonth}d left)",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primary
                        )
                        Text(
                            "${displayAmount(safeToSpend.remainingToday, settings)} remaining · Daily limit ${displayAmount(safeToSpend.dailyAllowance, settings)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    GlassBadge3D(emoji = "🛡️", accent = palette.primary, sizeDp = 36.dp)
                }
            }
        }

        // End-of-Day "Anything Spent Today?" Gentle Check-In Card
        if (todaySummary.todayTxCount == 0) {
            item {
                GlassCard(
                    settings = settings,
                    borderColor = palette.primary.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth().testTag("eod_anything_spent_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🌙 Anything Spent Today?",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "No spends logged today — all good, or add ${settings.currencySymbol}?",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onOpenQuickTransaction("EXPENSE") },
                            modifier = Modifier.testTag("eod_log_spend_btn")
                        ) {
                            Text("+ Log Spend")
                        }
                    }
                }
            }
        }

        // Hero Net Liquidity Card
        item {
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth().testTag("dashboard_hero_card")) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    palette.primary.copy(alpha = 0.22f * settings.glowIntensity),
                                    palette.secondary.copy(alpha = 0.14f * settings.glowIntensity)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("TOTAL LIQUID NET BALANCE", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text(
                            displayAmount(state.totalBalance, settings, 45200.0),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("↑ Income", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text(displayAmount(state.totalIncome, settings, 25000.0), color = palette.primary, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("↓ Expenses", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text(displayAmount(state.totalExpense, settings, 9400.0), color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val actions = listOf(
                    Triple("+ Income", "💰", { onOpenQuickTransaction("INCOME") }),
                    Triple("+ Expense", "💸", { onOpenQuickTransaction("EXPENSE") }),
                    Triple("💵 Cash", "💵", onOpenCashCounter),
                    Triple("🔁 Transfer", "🔁", onOpenTransfer)
                )
                actions.forEachIndexed { idx, (label, emoji, onClick) ->
                    GlassCard(
                        settings = settings,
                        onClick = onClick,
                        modifier = Modifier.weight(1f).testTag("quick_action_$idx")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GlassBadge3D(emoji = emoji, accent = if (idx == 0) palette.primary else palette.secondary, sizeDp = 36.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textPrimary, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Opt-In Paste SMS / UPI Quick-Log Card (100% offline clipboard check only when user taps)
        item {
            GlassCard(
                settings = settings,
                onClick = {
                    val clip = hostCallbacks.readClipboardText().orEmpty()
                    val draft = NativeIntelligenceCalculators.parseUpiSmsToDraft(clip)
                    if (draft != null) {
                        onOpenSmsDraft(draft)
                    } else {
                        smsInputText = clip
                        showSmsPasteDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("dash_sms_quicklog_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        GlassBadge3D(emoji = "📲", accent = palette.secondary, sizeDp = 34.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Paste Bank SMS / UPI Quick-Log", style = MaterialTheme.typography.labelLarge, color = palette.textPrimary)
                            Text("Auto-parse debited/credited amount, app & note offline", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste SMS", tint = palette.primary)
                }
            }
        }

        // Monthly Expense Pace Meter
        item {
            val statusColor = when (monthlyPace.statusLabel) {
                "High Pace" -> Color(0xFFEF4444)
                "Caution" -> Color(0xFFF59E0B)
                else -> palette.primary
            }
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth().testTag("monthly_pace_meter")) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("⏱️ Monthly Expense Pace", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                            Text(
                                "${displayAmount(monthlyPace.currentMonthExpense, settings)} of ${displayAmount(monthlyPace.monthlyBudgetTotal, settings)} · ${monthlyPace.daysLeft}d left",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = statusColor.copy(alpha = 0.18f)) {
                            Text(
                                monthlyPace.statusLabel,
                                color = statusColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.surfaceElevated)
                    ) {
                        val barWidth = maxWidth * monthlyPace.paceRatio
                        val markerOffset = (maxWidth * monthlyPace.expectedDayRatio).coerceAtMost(maxWidth - 2.dp)
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(10.dp)
                                .background(statusColor)
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = markerOffset)
                                .width(2.dp)
                                .height(10.dp)
                                .background(Color.White)
                        )
                    }
                    Text(
                        "Safe daily allowance remaining: ${displayAmount(monthlyPace.safePerDay, settings)}/day",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary
                    )
                }
            }
        }

        // Quick Stats 2x2 Grid (Borrowed, Lent, Net Worth, Transactions)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassCard(
                        settings = settings,
                        onClick = { onNavigateRoute("people") },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("BORROWED", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(displayAmount(totalBorrowed, settings), style = MaterialTheme.typography.titleMedium, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                        }
                    }
                    GlassCard(
                        settings = settings,
                        onClick = { onNavigateRoute("people") },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("LENT OUT", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(displayAmount(totalLent, settings), style = MaterialTheme.typography.titleMedium, color = palette.secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassCard(
                        settings = settings,
                        onClick = { onNavigateRoute("xray") },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("NET WORTH", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(displayAmount(netWorth, settings), style = MaterialTheme.typography.titleMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    GlassCard(
                        settings = settings,
                        onClick = { onNavigateRoute("transactions") },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("TRANSACTIONS", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text("${state.transactions.size} logged", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1-Tap Quick Spend Chips (with × delete & + Chip)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡ 1-Tap Quick Spend Chips", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                    TextButton(onClick = { showAddChipDialog = true }, modifier = Modifier.testTag("add_quick_chip_btn")) {
                        Text("+ Chip", color = palette.primary)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.quickTemplates, key = { it.id }) { chip ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surfaceElevated.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.surfaceBorder),
                            modifier = Modifier
                                .clickable { viewModel.triggerQuickTemplate(chip) }
                                .testTag("quick_chip_${chip.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(chip.icon)
                                Column {
                                    Text(chip.label, style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                                    Text(displayAmount(chip.amount, settings), style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteQuickTemplate(chip) },
                                    modifier = Modifier.size(28.dp).testTag("delete_quick_chip_${chip.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete chip", tint = palette.textSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Accounts Horizontal Strip + "+ Add Account" dashed card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Wallets & Accounts", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                    TextButton(onClick = { onNavigateRoute("accounts") }) { Text("Manage →", color = palette.primary) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.accounts.filter { !it.isArchived }, key = { it.id }) { acc ->
                        val bal = NativeLedgerCalculator.accountBalance(acc, state.transactions)
                        val allocCount = NativeLedgerCalculator.parseAllocations(acc.allocationsJson).size
                        GlassCard(
                            settings = settings,
                            onClick = { onNavigateRoute("accounts") },
                            modifier = Modifier.width(175.dp).height(112.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(acc.name, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(acc.accountType, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                                Column {
                                    Text(displayAmount(bal, settings), style = MaterialTheme.typography.titleMedium, color = palette.primary, fontWeight = FontWeight.Bold, maxLines = 1)
                                    if (allocCount > 0) {
                                        Text("$allocCount allocations", style = MaterialTheme.typography.labelSmall, color = palette.secondary, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .width(148.dp)
                                .height(112.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.5.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                                .background(palette.primary.copy(alpha = 0.08f))
                                .clickable { showAddAccountQuick = true }
                                .testTag("dash_add_account_card"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = "Add Account", tint = palette.primary)
                                Spacer(Modifier.height(4.dp))
                                Text("+ Add Account", style = MaterialTheme.typography.labelLarge, color = palette.primary)
                            }
                        }
                    }
                }
            }
        }

        // Summary Dashboard with Native Compose Charting (Weekly / Monthly & Donut / Bar Switcher)
        item {
            GlassCard(
                settings = settings,
                modifier = Modifier.fillMaxWidth().testTag("dashboard_category_chart_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "📊 Category Spending Summary",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${chartDistribution.window.label} · ${chartDistribution.transactionCount} spends · ${displayAmount(chartDistribution.totalSpent, settings)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = chartMode == "DONUT",
                                onClick = { chartMode = "DONUT" },
                                label = { Text("🍩 Donut", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("chart_mode_donut")
                            )
                            FilterChip(
                                selected = chartMode == "BAR",
                                onClick = { chartMode = "BAR" },
                                label = { Text("📊 Bar", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("chart_mode_bar")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpendingChartWindow.entries.forEach { win ->
                            FilterChip(
                                selected = chartWindow == win,
                                onClick = { chartWindow = win; selectedSliceCategory = null },
                                label = { Text(win.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("chart_window_${win.name.lowercase()}")
                            )
                        }
                    }

                    val chartColors = listOf(
                        Color(0xFF14B8A6),
                        Color(0xFF06B6D4),
                        Color(0xFF8B5CF6),
                        Color(0xFFF59E0B),
                        Color(0xFFEC4899),
                        Color(0xFF10B981)
                    )
                    val animProgress by animateFloatAsState(
                        targetValue = if (chartDistribution.slices.isEmpty()) 0f else 1f,
                        animationSpec = tween(durationMillis = 520),
                        label = "chartSweepAnim"
                    )

                    if (chartDistribution.slices.isEmpty()) {
                        Text(
                            "No expenses logged for ${chartDistribution.window.label.lowercase()} yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    } else if (chartMode == "DONUT") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(132.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(120.dp)) {
                                    val strokeWidth = 22.dp.toPx()
                                    val arcDiameter = size.minDimension - strokeWidth
                                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                                    var startAngle = -90f
                                    chartDistribution.slices.forEachIndexed { idx, slice ->
                                        val sweep = (slice.percentage / 100f) * 360f * animProgress
                                        val color = chartColors[idx % chartColors.size]
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = Size(arcDiameter, arcDiameter),
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val highlighted = chartDistribution.slices.firstOrNull { it.category == selectedSliceCategory }
                                    Text(
                                        text = highlighted?.category ?: "Total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = displayAmount(highlighted?.amount ?: chartDistribution.totalSpent, settings),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                chartDistribution.slices.forEachIndexed { idx, slice ->
                                    val color = chartColors[idx % chartColors.size]
                                    val isSelected = selectedSliceCategory == slice.category
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) color.copy(alpha = 0.16f) else Color.Transparent)
                                            .clickable {
                                                selectedSliceCategory = if (isSelected) null else slice.category
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "${slice.icon} ${slice.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            "${String.format("%.0f", slice.percentage)}% · ${displayAmount(slice.amount, settings)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chartDistribution.slices.forEachIndexed { idx, slice ->
                                val color = chartColors[idx % chartColors.size]
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${slice.icon} ${slice.category} (${slice.transactionCount})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "${displayAmount(slice.amount, settings)} · ${String.format("%.1f", slice.percentage)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    ) {
                                        drawRoundRect(
                                            color = color.copy(alpha = 0.16f),
                                            size = size,
                                            cornerRadius = CornerRadius(8f, 8f)
                                        )
                                        val fillW = (size.width * (slice.percentage / 100f) * animProgress).coerceIn(0f, size.width)
                                        drawRoundRect(
                                            color = color,
                                            size = Size(fillW, size.height),
                                            cornerRadius = CornerRadius(8f, 8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Emergency Runway & Warranty Vault + Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(
                    settings = settings,
                    onClick = { onNavigateRoute("planning") },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("🛟 Emergency Runway", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("${String.format(Locale.US, "%.1f", runwayMonths)} months", style = MaterialTheme.typography.titleMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                        Text("Tap for Budgets & Goals", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
                GlassCard(
                    settings = settings,
                    onClick = { onNavigateRoute("transactions") },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("🛡️ Warranty Vault", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text("${warrantyTxns.size} active items", style = MaterialTheme.typography.titleMedium, color = palette.secondary, fontWeight = FontWeight.Bold)
                        val firstW = warrantyTxns.firstOrNull()
                        if (firstW != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${firstW.category} (${firstW.warrantyDate})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearTransactionWarranty(firstW) },
                                    modifier = Modifier.size(22.dp).testTag("clear_warranty_${firstW.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear warranty", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                }
                            }
                        } else {
                            Text("Track bill deadlines", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Feature #9: "Waiting for Refund / Return" List (Pending Refunds & Returns Tracker)
        item {
            GlassCard(
                settings = settings,
                modifier = Modifier.fillMaxWidth().testTag("dashboard_pending_refunds_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "📦 Waiting for Refund / Return",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (pendingRefundItems.isEmpty()) "Track Amazon/Flipkart refunds or promised friend returns"
                                else "${pendingRefundItems.size} pending · ${displayAmount(pendingRefundItems.sumOf { it.amount }, settings)} expected back",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                editingRefund = null
                                refundTitle = ""
                                refundAmount = ""
                                refundSource = "Amazon"
                                refundExpectedNote = "Tomorrow"
                                showAddRefundDialog = true
                            },
                            modifier = Modifier.testTag("dash_add_refund_btn")
                        ) {
                            Text("+ Refund", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (pendingRefundItems.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceElevated.copy(alpha = 0.45f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Returned an order or cancelled a ticket? Add it so you don't forget!",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    editingRefund = null
                                    refundTitle = ""
                                    refundAmount = ""
                                    showAddRefundDialog = true
                                }
                            ) {
                                Text("+ Add Refund", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                            }
                        }
                    }

                    pendingRefundItems.forEach { refund ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceElevated.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${refund.title} · ${displayAmount(refund.amount, settings)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${refund.source} · Expected: ${refund.expectedDateNote.ifBlank { "Soon" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = {
                                        editingRefund = refund
                                        refundTitle = refund.title
                                        refundAmount = MoneyPaise.formatInput(MoneyPaise.toPaise(refund.amount))
                                        refundSource = refund.source
                                        refundExpectedNote = refund.expectedDateNote
                                        showAddRefundDialog = true
                                    },
                                    modifier = Modifier.size(30.dp).testTag("refund_edit_btn_${refund.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit refund", tint = palette.secondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deletePendingRefund(refund.id) },
                                    modifier = Modifier.size(30.dp).testTag("refund_delete_btn_${refund.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete refund", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                                Button(
                                    onClick = { receivingRefund = refund },
                                    modifier = Modifier.testTag("refund_received_btn_${refund.id}")
                                ) {
                                    Text("Received ✓", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("planning" to "🎯 Planning", "people" to "👥 People & Split", "reports" to "📊 Reports", "xray" to "🩻 X-Ray", "guide" to "📖 Guide").forEach { (route, label) ->
                    FilterChip(selected = false, onClick = { onNavigateRoute(route) }, label = { Text(label) })
                }
            }
        }

        // Feature #2: Quick-Filter Pills on Dashboard (All, Today, Yesterday, Cash, UPI) right above Recent Activity
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                    TextButton(onClick = { onNavigateRoute("transactions") }) { Text("See All →", color = palette.primary) }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("dashboard_quick_filter_pills"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Today", "Yesterday", "Cash", "UPI").forEach { pill ->
                        FilterChip(
                            selected = dashActivityFilter == pill,
                            onClick = { dashActivityFilter = pill },
                            label = { Text(pill, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("dash_filter_pill_${pill.lowercase()}")
                        )
                    }
                }
            }
        }

        items(filteredRecentActivity.take(12), key = { it.id }) { tx ->
            val accName = state.accounts.firstOrNull { it.id == tx.accountId }?.name ?: "Wallet"
            val isExpense = tx.type.equals("EXPENSE", true) || tx.type.equals("LEND", true) || tx.type.equals("TRANSFER_OUT", true)
            GlassCard(
                settings = settings,
                onClick = { onShowReceipt(tx) },
                modifier = Modifier.fillMaxWidth().testTag("dash_tx_card_${tx.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (tx.type.equals("EXPENSE", true)) {
                            IconButton(
                                onClick = { viewModel.deleteTransaction(tx) },
                                modifier = Modifier.size(36.dp).testTag("tx_red_delete_${tx.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete expense", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        GlassBadge3D(
                            emoji = if (isExpense) "💸" else "💰",
                            accent = if (isExpense) Color(0xFFF43F5E) else palette.primary,
                            sizeDp = 38.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tx.category + if (tx.subcategory.isNotBlank()) " · ${tx.subcategory}" else "",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(accName, tx.note.takeIf { it.isNotBlank() }, dateFmt.format(Date(tx.timestamp))).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onEditTransaction(tx) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit transaction", tint = palette.textSecondary, modifier = Modifier.size(15.dp))
                            }
                            Text(
                                text = (if (isExpense) "-" else "+") + displayAmount(tx.amount, settings),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpense) Color(0xFFF43F5E) else palette.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddRefundDialog) {
        AlertDialog(
            onDismissRequest = { showAddRefundDialog = false; editingRefund = null },
            containerColor = palette.surface,
            title = {
                Text(
                    if (editingRefund == null) "📦 Add Pending Refund / Return" else "✏️ Edit Pending Refund",
                    color = palette.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = refundTitle,
                        onValueChange = { refundTitle = it },
                        label = { Text("Item / Person (e.g. Amazon Shoes or Ravi)") }
                    )
                    OutlinedTextField(
                        value = refundAmount,
                        onValueChange = { refundAmount = it },
                        label = { Text("Amount (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = refundSource,
                        onValueChange = { refundSource = it },
                        label = { Text("Source (Amazon / Flipkart / Friend)") }
                    )
                    OutlinedTextField(
                        value = refundExpectedNote,
                        onValueChange = { refundExpectedNote = it },
                        label = { Text("When expected? (e.g. Tomorrow / 3 days)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = refundAmount.toDoubleOrNull() ?: return@Button
                    if (refundTitle.isNotBlank() && amt > 0) {
                        val existing = editingRefund
                        if (existing == null) {
                            viewModel.addPendingRefund(refundTitle, amt, refundSource, refundExpectedNote)
                        } else {
                            viewModel.updatePendingRefund(existing.id, refundTitle, amt, refundSource, refundExpectedNote)
                        }
                        refundTitle = ""; refundAmount = ""; editingRefund = null; showAddRefundDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddRefundDialog = false; editingRefund = null }) { Text("Cancel") } }
        )
    }

    receivingRefund?.let { ref ->
        val activeAccs = remember(state.accounts) { state.accounts.filter { !it.isArchived } }
        var selectedAcc by remember(activeAccs) {
            mutableStateOf(
                activeAccs.firstOrNull { !it.accountType.contains("Cash", ignoreCase = true) }
                    ?: activeAccs.firstOrNull()
            )
        }
        var expandAcc by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { receivingRefund = null },
            containerColor = palette.surface,
            title = { Text("📦 Credit Refund to Wallet", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${ref.title} · ${displayAmount(ref.amount, settings)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Choose which Bank / UPI / Cash wallet received this refund:",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Box {
                        OutlinedButton(onClick = { expandAcc = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Credit Wallet: ${selectedAcc?.name ?: "Select Account"}")
                        }
                        DropdownMenu(expanded = expandAcc, onDismissRequest = { expandAcc = false }) {
                            activeAccs.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.accountType})") },
                                    onClick = { selectedAcc = acc; expandAcc = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.markPendingRefundReceived(ref, selectedAcc?.id)
                    receivingRefund = null
                }) { Text("Confirm & Credit") }
            },
            dismissButton = { TextButton(onClick = { receivingRefund = null }) { Text("Cancel") } }
        )
    }

    if (showAddChipDialog) {
        AlertDialog(
            onDismissRequest = { showAddChipDialog = false },
            containerColor = palette.surface,
            title = { Text("Add 1-Tap Quick Spend Chip", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = chipIcon, onValueChange = { chipIcon = it }, label = { Text("Emoji Icon") })
                    OutlinedTextField(value = chipLabel, onValueChange = { chipLabel = it }, label = { Text("Label (e.g. Metro)") })
                    OutlinedTextField(
                        value = chipAmount,
                        onValueChange = { chipAmount = it },
                        label = { Text("Amount (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(value = chipCategory, onValueChange = { chipCategory = it }, label = { Text("Category") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = chipAmount.toDoubleOrNull() ?: return@Button
                    if (chipLabel.isNotBlank() && amt > 0) {
                        viewModel.addQuickTemplate(chipIcon, chipLabel.trim(), amt, chipCategory.ifBlank { "Food" })
                        chipLabel = ""; chipAmount = ""; showAddChipDialog = false
                    }
                }) { Text("Save Chip") }
            },
            dismissButton = { TextButton(onClick = { showAddChipDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddAccountQuick) {
        AddAccountDialog(
            palette = palette,
            onDismiss = { showAddAccountQuick = false },
            onSave = { name, type, bal, inst, num, ccLim, billDay, dueDay ->
                viewModel.addAccount(name, type, bal, "INR", inst, num, "", ccLim, billDay, dueDay)
                showAddAccountQuick = false
            }
        )
    }

    if (showSmsPasteDialog) {
        AlertDialog(
            onDismissRequest = { showSmsPasteDialog = false },
            containerColor = palette.surface,
            title = { Text("📲 Paste Bank / UPI SMS", color = palette.textPrimary) },
            text = {
                OutlinedTextField(
                    value = smsInputText,
                    onValueChange = { smsInputText = it },
                    label = { Text("e.g. Rs.450 debited via GPay at Swiggy") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("sms_paste_input")
                )
            },
            confirmButton = {
                Button(onClick = {
                    val draft = NativeIntelligenceCalculators.parseUpiSmsToDraft(smsInputText)
                    if (draft != null) {
                        showSmsPasteDialog = false
                        onOpenSmsDraft(draft)
                    } else {
                        viewModel.showStatus("Could not detect an amount in SMS text")
                    }
                }) { Text("Parse & Log") }
            },
            dismissButton = { TextButton(onClick = { showSmsPasteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AccountsScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    onOpenCashCounter: () -> Unit,
    onQuickTransactionForAccount: (Long, String) -> Unit = { _, _ -> },
    onTransferFromAccount: (Long) -> Unit = {}
) {
    var showAddAccount by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var allocTargetAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var reconcileTargetAccount by remember { mutableStateOf<AccountEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Accounts & Wallets", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                Text("Net Total: ${displayAmount(state.totalBalance, settings)}", style = MaterialTheme.typography.bodySmall, color = palette.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenCashCounter, modifier = Modifier.testTag("accounts_cash_count_btn")) {
                    Text("💵 Cash Count")
                }
                Button(onClick = { showAddAccount = true }, modifier = Modifier.testTag("accounts_add_btn")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Add")
                }
            }
        }

        // Feature #4: 1-Tap ATM Withdrawal (Bank → Cash) with ₹500, ₹1,000, ₹2,000 pills
        GlassCard(
            settings = settings,
            modifier = Modifier.fillMaxWidth().testTag("accounts_atm_withdrawal_card")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "🏧 1-Tap ATM Withdrawal (Bank → Cash Wallet)",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Instantly transfer cash withdrawn from your primary Bank account into your Cash Wallet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(500.0 to "₹500", 1000.0 to "₹1,000", 2000.0 to "₹2,000", 5000.0 to "₹5,000").forEach { (amt, label) ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.performAtmWithdrawal(amt) },
                            label = { Text("🏧 $label", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("atm_withdraw_${amt.toInt()}")
                        )
                    }
                }
            }
        }

        val activeAccountsList = state.accounts.filter { !it.isArchived }
        if (activeAccountsList.isEmpty()) {
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "No active wallets yet. Add your Bank, UPI, or Cash Wallet to start tracking!",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { showAddAccount = true }) { Text("+ Add Wallet") }
                }
            }
        }

        activeAccountsList.forEach { acc ->
            val currentBal = NativeLedgerCalculator.accountBalance(acc, state.transactions)
            val allocs = NativeLedgerCalculator.parseAllocations(acc.allocationsJson)
            val unallocated = NativeLedgerCalculator.unallocatedBalance(acc, state.transactions)
            val ccUtil = NativeLedgerCalculator.creditCardUtilizationPct(acc, state.transactions)

            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(acc.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text("${acc.accountType} · ${acc.institution.ifBlank { "Offline Ledger" }}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(displayAmount(currentBal, settings), style = MaterialTheme.typography.titleLarge, color = palette.primary, fontWeight = FontWeight.ExtraBold)
                            IconButton(
                                onClick = { editingAccount = acc },
                                modifier = Modifier.size(36.dp).testTag("edit_account_${acc.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit account", tint = palette.textSecondary, modifier = Modifier.size(17.dp))
                            }
                        }
                    }

                    if (acc.accountType.contains("Credit", ignoreCase = true) && acc.creditLimit > 0) {
                        val isHigh = ccUtil > 30.0
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isHigh) Color(0x33EF4444) else palette.surfaceElevated.copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isHigh) {
                                        Icon(Icons.Default.Warning, contentDescription = "High Utilization", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        "Credit Utilization: ${String.format("%.1f", ccUtil)}% ${if (isHigh) "(>30% Alert!)" else ""}",
                                        color = if (isHigh) Color(0xFFEF4444) else palette.textPrimary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Text("Bill Day ${acc.billingCycleDay} · Due ${acc.dueDay}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (ccUtil.toFloat() / 100f).coerceIn(0f, 1f) },
                                color = if (isHigh) Color(0xFFEF4444) else palette.primary,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    if (allocs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Allocations & Auto-Split % · Unallocated: ${displayAmount(unallocated, settings)}", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                            allocs.forEach { al ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• ${al.name} (${al.autoSplitPercent.toInt()}% auto)", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                    Text(displayAmount(al.allocatedAmount, settings), style = MaterialTheme.typography.bodySmall, color = palette.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Per-Account Quick Actions (+ Deposit, - Withdraw, Transfer)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(
                            selected = false,
                            onClick = { onQuickTransactionForAccount(acc.id, "INCOME") },
                            label = { Text("+ Deposit") },
                            modifier = Modifier.testTag("acc_deposit_${acc.id}")
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onQuickTransactionForAccount(acc.id, "EXPENSE") },
                            label = { Text("− Withdraw") },
                            modifier = Modifier.testTag("acc_withdraw_${acc.id}")
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onTransferFromAccount(acc.id) },
                            label = { Text("⇄ Transfer") },
                            modifier = Modifier.testTag("acc_transfer_${acc.id}")
                        )
                        if (acc.accountType.equals("Cash", true)) {
                            FilterChip(
                                selected = false,
                                onClick = onOpenCashCounter,
                                label = { Text("💵 Count") }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { allocTargetAccount = acc }, modifier = Modifier.weight(1f).testTag("allocate_btn_${acc.id}")) {
                            Text("📂 Allocate")
                        }
                        OutlinedButton(onClick = { reconcileTargetAccount = acc }, modifier = Modifier.weight(1f).testTag("reconcile_btn_${acc.id}")) {
                            Text("⚖️ Reconcile")
                        }
                    }
                }
            }
        }
    }

    if (showAddAccount) {
        AddAccountDialog(
            palette = palette,
            onDismiss = { showAddAccount = false },
            onSave = { name, type, bal, inst, num, ccLim, billDay, dueDay ->
                viewModel.addAccount(name, type, bal, "INR", inst, num, "", ccLim, billDay, dueDay)
                showAddAccount = false
            }
        )
    }

    editingAccount?.let { acc ->
        EditAccountDialog(
            account = acc,
            currencySymbol = settings.currencySymbol,
            palette = palette,
            onDismiss = { editingAccount = null },
            onSave = { updated ->
                viewModel.updateAccount(updated)
                editingAccount = null
            },
            onArchive = { target ->
                viewModel.archiveAccount(target)
                editingAccount = null
            }
        )
    }

    allocTargetAccount?.let { acc ->
        AccountAllocationsDialog(
            account = acc,
            palette = palette,
            onDismiss = { allocTargetAccount = null },
            onSave = { updatedList ->
                viewModel.updateAccountAllocations(acc, updatedList)
                allocTargetAccount = null
            }
        )
    }

    reconcileTargetAccount?.let { acc ->
        val currentBal = NativeLedgerCalculator.accountBalance(acc, state.transactions)
        ReconcileAccountDialog(
            account = acc,
            currentBalance = currentBal,
            palette = palette,
            onDismiss = { reconcileTargetAccount = null },
            onReconcile = { actualBal, note ->
                viewModel.reconcileAccountBalance(acc, actualBal, note)
                reconcileTargetAccount = null
            }
        )
    }
}

@Composable
fun AddAccountDialog(
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String, Double, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Bank") }
    var balance by remember { mutableStateOf("0") }
    var institution by remember { mutableStateOf("") }
    var accNumber by remember { mutableStateOf("") }
    var ccLimit by remember { mutableStateOf("50000") }
    var billDay by remember { mutableStateOf("1") }
    var dueDay by remember { mutableStateOf("18") }
    val types = listOf("Bank", "Cash", "Wallet", "Savings", "Credit Card")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("+ Add Account / Wallet", color = palette.textPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") })
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Opening Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(value = institution, onValueChange = { institution = it }, label = { Text("Bank / Institution (optional)") })
                OutlinedTextField(value = accNumber, onValueChange = { accNumber = it }, label = { Text("Last 4 Digits (optional)") })
                if (type == "Credit Card") {
                    OutlinedTextField(
                        value = ccLimit,
                        onValueChange = { ccLimit = it },
                        label = { Text("Credit Limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = billDay, onValueChange = { billDay = it }, label = { Text("Bill Day") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = dueDay, onValueChange = { dueDay = it }, label = { Text("Due Day") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        name.trim(),
                        type,
                        balance.toDoubleOrNull() ?: 0.0,
                        institution,
                        accNumber,
                        ccLimit.toDoubleOrNull() ?: 0.0,
                        billDay.toIntOrNull() ?: 1,
                        dueDay.toIntOrNull() ?: 15
                    )
                }
            }) { Text("Save Account") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AccountAllocationsDialog(
    account: AccountEntity,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onSave: (List<AccountAllocation>) -> Unit
) {
    val list = remember { mutableStateListOf<AccountAllocation>().apply { addAll(NativeLedgerCalculator.parseAllocations(account.allocationsJson)) } }
    var editingIdx by remember { mutableStateOf<Int?>(null) }
    var pocketName by remember { mutableStateOf("") }
    var pocketAmt by remember { mutableStateOf("") }
    var autoPct by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("📂 Allocations & Auto-Split · ${account.name}", color = palette.textPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set savings/reserve pockets & Auto-Split % on every new Income.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                list.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(palette.surfaceElevated).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${MoneyPaise.formatCurrency(item.allocatedAmount)} · Auto-Split ${item.autoSplitPercent}%", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    editingIdx = idx
                                    pocketName = item.name
                                    pocketAmt = MoneyPaise.formatInput(MoneyPaise.toPaise(item.allocatedAmount))
                                    autoPct = item.autoSplitPercent.toInt().toString()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit pocket", tint = palette.secondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    list.removeAt(idx)
                                    if (editingIdx == idx) {
                                        editingIdx = null
                                        pocketName = ""; pocketAmt = ""; autoPct = "0"
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete pocket", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                OutlinedTextField(value = pocketName, onValueChange = { pocketName = it }, label = { Text("Pocket Name (e.g. Emergency)") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pocketAmt,
                        onValueChange = { pocketAmt = it },
                        label = { Text("Allocated Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = autoPct,
                        onValueChange = { autoPct = it },
                        label = { Text("Auto-Split %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (pocketName.isNotBlank()) {
                            val idx = editingIdx
                            val updatedPocket = AccountAllocation(
                                id = if (idx != null && idx in list.indices) list[idx].id else UUID.randomUUID().toString().take(8),
                                name = pocketName.trim(),
                                allocatedAmount = pocketAmt.toDoubleOrNull() ?: 0.0,
                                autoSplitPercent = (autoPct.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
                            )
                            if (idx != null && idx in list.indices) {
                                list[idx] = updatedPocket
                            } else {
                                list.add(updatedPocket)
                            }
                            editingIdx = null
                            pocketName = ""; pocketAmt = ""; autoPct = "0"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (editingIdx != null) "✓ Update Allocation Pocket" else "+ Add Allocation Pocket") }
            }
        },
        confirmButton = { Button(onClick = { onSave(list.toList()) }) { Text("Save Pockets") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ReconcileAccountDialog(
    account: AccountEntity,
    currentBalance: Double,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onReconcile: (Double, String) -> Unit
) {
    var actual by remember { mutableStateOf(MoneyPaise.formatInput(MoneyPaise.toPaise(currentBalance))) }
    var note by remember { mutableStateOf("Balance adjustment") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("⚖️ Reconcile ${account.name}", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ledger Balance: ${MoneyPaise.formatCurrency(currentBalance)}", color = palette.textSecondary)
                OutlinedTextField(
                    value = actual,
                    onValueChange = { actual = it },
                    label = { Text("Actual Real-World Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Adjustment Note") })
            }
        },
        confirmButton = {
            Button(onClick = {
                actual.toDoubleOrNull()?.let { onReconcile(it, note) }
            }) { Text("Reconcile") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
