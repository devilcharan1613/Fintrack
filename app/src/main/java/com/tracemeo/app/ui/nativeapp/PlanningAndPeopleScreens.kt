package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.KhataEntryItem
import com.tracemeo.app.data.LiquidGlassSettings
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.PersonLedgerSummary
import com.tracemeo.app.data.TransactionDateTime
import com.tracemeo.app.data.local.BudgetEntity
import com.tracemeo.app.data.local.GoalEntity
import com.tracemeo.app.data.local.LoanEntity
import com.tracemeo.app.data.local.PersonEntity
import com.tracemeo.app.data.local.ValueItemEntity
import com.tracemeo.app.data.local.WishlistItemEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PlanningScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0=Budgets, 1=Goals & Savings Tools, 2=Debt Planner
    var showAddBudget by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var showAddGoal by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var topUpGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var showAddLoan by remember { mutableStateOf(false) }
    var editingLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var repayingLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var showAddWishlist by remember { mutableStateOf(false) }
    var buyingWishlist by remember { mutableStateOf<WishlistItemEntity?>(null) }
    var showAddCashback by remember { mutableStateOf(false) }
    var showAddValueItem by remember { mutableStateOf(false) }
    var editingValueItem by remember { mutableStateOf<ValueItemEntity?>(null) }
    var extraMonthlyPayoff by remember { mutableStateOf("2000") }

    val curMonth = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val runwayMonths = remember(state.accounts, state.transactions) {
        NativeLedgerCalculator.computeEmergencyRunwayMonths(state.accounts, state.transactions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Text("🎯 Planning & Debt", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Budgets & Runway") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Goals · RoundUp · Wishlist") })
            FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("Loans · Snowball vs Avalanche") })
        }

        if (tab == 0) {
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🛟 Emergency Fund Runway", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                    Text(
                        "${String.format(Locale.US, "%.1f", runwayMonths)} months of average living expenses covered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Monthly Category Budgets", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Button(onClick = { showAddBudget = true }, modifier = Modifier.testTag("add_budget_btn")) { Text("+ Budget") }
            }
            if (state.budgets.isEmpty()) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No monthly budgets yet. Set a spending cap for Food, Shopping, or Bills!",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showAddBudget = true }) { Text("+ Add Budget") }
                    }
                }
            }
            state.budgets.forEach { b ->
                val spent = state.transactions.filter {
                    it.type.equals("EXPENSE", true) &&
                        it.category.equals(b.category, true) &&
                        NativeLedgerCalculator.monthKey(it.timestamp) == b.month
                }.sumOf { it.amount }
                val pct = if (b.limitAmount > 0) (spent / b.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${b.category} (${b.month})", color = palette.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${displayAmount(spent, settings)} / ${displayAmount(b.limitAmount, settings)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (pct >= 0.9f) Color(0xFFEF4444) else palette.primary
                                )
                                IconButton(onClick = { editingBudget = b }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit budget", tint = palette.secondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteBudget(b) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete budget", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { pct },
                            color = if (pct >= 0.9f) Color(0xFFEF4444) else palette.primary,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        } else if (tab == 1) {
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🪙 Round-Up Spare Change Jar", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                            Text("Automatically round up every expense to the nearest step and credit a Goal.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        Switch(checked = settings.roundUpEnabled, onCheckedChange = { en ->
                            val firstGoal = settings.roundUpGoalId ?: state.goals.firstOrNull()?.id
                            viewModel.updateSettings { it.copy(roundUpEnabled = en, roundUpGoalId = firstGoal) }
                        })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 50, 100).forEach { step ->
                            FilterChip(
                                selected = settings.roundUpStep == step,
                                onClick = { viewModel.updateSettings { it.copy(roundUpStep = step) } },
                                label = { Text("${settings.currencySymbol}$step") }
                            )
                        }
                    }
                    if (state.goals.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            state.goals.forEach { g ->
                                FilterChip(
                                    selected = settings.roundUpGoalId == g.id,
                                    onClick = { viewModel.updateSettings { it.copy(roundUpGoalId = g.id) } },
                                    label = { Text(g.name) }
                                )
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Savings Goals", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Button(onClick = { showAddGoal = true }, modifier = Modifier.testTag("add_goal_btn")) { Text("+ Goal") }
            }
            if (state.goals.isEmpty()) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No savings goals yet. Start a piggy bank for a phone, trip, or emergency!",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showAddGoal = true }) { Text("+ Add Goal") }
                    }
                }
            }
            state.goals.forEach { goal ->
                val saved = MoneyPaise.toRupees(NativeLedgerCalculator.goalSavedPaise(goal, state.goalContributions))
                val pct = if (goal.targetAmount > 0) (saved / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(goal.name, color = palette.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(onClick = {
                                    viewModel.addGoalContribution(goal.id, 500.0, state.accounts.firstOrNull { !it.isArchived }?.id, "Quick top-up")
                                }) { Text("+${settings.currencySymbol}500") }
                                OutlinedButton(onClick = { topUpGoal = goal }) { Text("+ Top Up") }
                                IconButton(onClick = { editingGoal = goal }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit goal", tint = palette.secondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteGoal(goal) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete goal", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Text("${displayAmount(saved, settings)} / ${displayAmount(goal.targetAmount, settings)} (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall, color = palette.primary)
                        LinearProgressIndicator(progress = { pct }, color = palette.primary, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)))
                    }
                }
            }

            val skippedSaved = state.wishlistItems.filter { it.status.equals("SKIPPED", true) }.sumOf { it.price }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("❄️ 24h Cooling Wishlist", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    if (skippedSaved > 0) {
                        Text("Saved by skipping impulse buys: ${displayAmount(skippedSaved, settings)}", style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(onClick = { showAddWishlist = true }, modifier = Modifier.testTag("add_wishlist_btn")) { Text("+ Add Wishlist Item") }
            }
            if (state.wishlistItems.isEmpty()) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tempted by an impulse buy? Add it here to cool down for 24 hours first.",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showAddWishlist = true }) { Text("+ Add Item") }
                    }
                }
            }
            state.wishlistItems.forEach { w ->
                val hrsLeft = ((w.unlockedAt - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(0L)
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${w.name} · ${displayAmount(w.price, settings)}", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (w.status != "COOLING") "Status: ${w.status}" else if (hrsLeft > 0) "${hrsLeft}h impulse cool-down left" else "Cool-down complete — decide mindfully",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteWishlistItem(w) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete wishlist item", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                        if (w.status == "COOLING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.updateWishlistStatus(w, "SKIPPED") },
                                    modifier = Modifier.weight(1f).testTag("skip_wishlist_${w.id}")
                                ) {
                                    Text("Skip & Save ✓", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = { buyingWishlist = w },
                                    modifier = Modifier.weight(1f).testTag("buy_wishlist_${w.id}")
                                ) {
                                    Text("Bought It → Log Expense", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🎁 Cashback & 💎 Price-Per-Use", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { showAddCashback = true }, modifier = Modifier.testTag("add_cashback_btn")) { Text("+ Cashback") }
                    OutlinedButton(onClick = { editingValueItem = null; showAddValueItem = true }, modifier = Modifier.testTag("add_value_item_btn")) { Text("+ Item") }
                }
            }
            val totalCb = state.cashbackEntries.sumOf { it.amount }
            if (state.cashbackEntries.isEmpty()) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No cashback logged yet. Record UPI scratch cards & credit rewards!",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showAddCashback = true }) { Text("+ Log Cashback") }
                    }
                }
            } else {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Total Rewards Earned: ${displayAmount(totalCb, settings)}", color = palette.primary, fontWeight = FontWeight.Bold)
                        state.cashbackEntries.forEach { cb ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${cb.source}: ${displayAmount(cb.amount, settings)}" + if (cb.note.isNotBlank()) " · ${cb.note}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.deleteCashbackEntry(cb) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete cashback", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            if (state.valueItems.isEmpty()) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Track Price-Per-Use on shoes, headphones, gym memberships & gadgets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { editingValueItem = null; showAddValueItem = true }) { Text("+ Add Item") }
                    }
                }
            }
            state.valueItems.forEach { v ->
                val ppu = v.cost / v.uses.coerceAtLeast(1)
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(v.name, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                            Text("Cost ${displayAmount(v.cost, settings)} · ${v.uses} uses = ${displayAmount(ppu, settings)}/use", style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { viewModel.incrementValueItemUse(v) }) { Text("+1 Use") }
                            IconButton(onClick = { editingValueItem = v; showAddValueItem = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit item", tint = palette.secondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.deleteValueItem(v) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        } else {
            val extra = extraMonthlyPayoff.toDoubleOrNull() ?: 0.0
            val snowball = remember(state.loans, state.loanPayments, extra) {
                NativeLedgerCalculator.simulateDebtPayoff(state.loans, state.loanPayments, extra, isAvalanche = false)
            }
            val avalanche = remember(state.loans, state.loanPayments, extra) {
                NativeLedgerCalculator.simulateDebtPayoff(state.loans, state.loanPayments, extra, isAvalanche = true)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Loans & EMIs", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Button(onClick = { showAddLoan = true }, modifier = Modifier.testTag("add_loan_btn")) { Text("+ Loan") }
            }

            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("❄️ Snowball vs. 🔥 Avalanche Payoff Planner", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                    OutlinedTextField(
                        value = extraMonthlyPayoff,
                        onValueChange = { extraMonthlyPayoff = it },
                        label = { Text("Extra Monthly Payment (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(palette.surfaceElevated).padding(10.dp)) {
                            Text("❄️ Snowball", fontWeight = FontWeight.Bold, color = palette.secondary)
                            Text("${snowball.totalMonths} months", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text("Interest: ${displayAmount(snowball.totalInterestPaid, settings)}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(palette.surfaceElevated).padding(10.dp)) {
                            Text("🔥 Avalanche", fontWeight = FontWeight.Bold, color = palette.primary)
                            Text("${avalanche.totalMonths} months", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text("Interest: ${displayAmount(avalanche.totalInterestPaid, settings)}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                    val savedInterest = (snowball.totalInterestPaid - avalanche.totalInterestPaid).coerceAtLeast(0.0)
                    if (savedInterest > 1.0) {
                        Text("Avalanche saves ${displayAmount(savedInterest, settings)} in interest!", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                    }
                }
            }

            if (state.loans.isEmpty()) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No active loans or EMIs. Add a loan to compare Snowball vs. Avalanche payoff!",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showAddLoan = true }) { Text("+ Add Loan") }
                    }
                }
            }

            state.loans.forEach { loan ->
                val paid = MoneyPaise.toRupees(NativeLedgerCalculator.loanPaidPaise(loan, state.loanPayments))
                val rem = MoneyPaise.toRupees(NativeLedgerCalculator.loanRemainingPaise(loan, state.loanPayments))
                val pct = if (loan.principal > 0) (paid / loan.principal).toFloat().coerceIn(0f, 1f) else 0f
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${loan.name} (${loan.interestRate}% APR)", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                Text("Remaining: ${displayAmount(rem, settings)} · Paid: ${displayAmount(paid, settings)}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(onClick = { repayingLoan = loan }, modifier = Modifier.testTag("repay_loan_${loan.id}")) {
                                    Text("Record Repayment", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { editingLoan = loan }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit loan", tint = palette.secondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteLoan(loan) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete loan", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        LinearProgressIndicator(progress = { pct }, color = palette.primary, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
    }

    // Budget Add/Edit
    if (showAddBudget || editingBudget != null) {
        var cat by remember(editingBudget) { mutableStateOf(editingBudget?.category ?: "Food") }
        var lim by remember(editingBudget) { mutableStateOf(editingBudget?.limitAmount?.let { MoneyPaise.formatInput(MoneyPaise.toPaise(it)) } ?: "") }
        var month by remember(editingBudget) { mutableStateOf(editingBudget?.month ?: curMonth) }
        AlertDialog(
            onDismissRequest = { showAddBudget = false; editingBudget = null },
            containerColor = palette.surface,
            title = { Text(if (editingBudget == null) "New Monthly Budget" else "Edit Monthly Budget", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") })
                    OutlinedTextField(value = lim, onValueChange = { lim = it }, label = { Text("Monthly Limit (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Month (YYYY-MM)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val l = lim.toDoubleOrNull() ?: return@Button
                    if (cat.isNotBlank() && l > 0) {
                        val ed = editingBudget
                        if (ed == null) viewModel.addBudget(cat.trim(), l, month.ifBlank { curMonth })
                        else viewModel.updateBudget(ed, cat.trim(), l, month.ifBlank { curMonth })
                        showAddBudget = false
                        editingBudget = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddBudget = false; editingBudget = null }) { Text("Cancel") } }
        )
    }

    // Goal Add/Edit
    if (showAddGoal || editingGoal != null) {
        var name by remember(editingGoal) { mutableStateOf(editingGoal?.name ?: "") }
        var target by remember(editingGoal) { mutableStateOf(editingGoal?.targetAmount?.let { MoneyPaise.formatInput(MoneyPaise.toPaise(it)) } ?: "") }
        AlertDialog(
            onDismissRequest = { showAddGoal = false; editingGoal = null },
            containerColor = palette.surface,
            title = { Text(if (editingGoal == null) "New Savings Goal" else "Edit Savings Goal", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Goal Name") })
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val t = target.toDoubleOrNull() ?: return@Button
                    if (name.isNotBlank() && t > 0) {
                        val ed = editingGoal
                        if (ed == null) viewModel.addGoal(name.trim(), t)
                        else viewModel.updateGoal(ed, name.trim(), t)
                        showAddGoal = false
                        editingGoal = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddGoal = false; editingGoal = null }) { Text("Cancel") } }
        )
    }

    // Custom Goal Contribution
    topUpGoal?.let { goal ->
        val activeAccs = remember(state.accounts) { state.accounts.filter { !it.isArchived } }
        var selectedAcc by remember(activeAccs) { mutableStateOf(activeAccs.firstOrNull()) }
        var expandAcc by remember { mutableStateOf(false) }
        var amtText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { topUpGoal = null },
            containerColor = palette.surface,
            title = { Text("Top Up · ${goal.name}", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amtText,
                        onValueChange = { amtText = it },
                        label = { Text("Contribution (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Box {
                        OutlinedButton(onClick = { expandAcc = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("From Wallet: ${selectedAcc?.name ?: "None"}")
                        }
                        DropdownMenu(expanded = expandAcc, onDismissRequest = { expandAcc = false }) {
                            activeAccs.forEach { acc ->
                                DropdownMenuItem(text = { Text(acc.name) }, onClick = { selectedAcc = acc; expandAcc = false })
                            }
                        }
                    }
                    OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Note (optional)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val v = amtText.toDoubleOrNull() ?: return@Button
                    if (v > 0) {
                        viewModel.addGoalContribution(goal.id, v, selectedAcc?.id, noteText)
                        topUpGoal = null
                    }
                }) { Text("Add Contribution") }
            },
            dismissButton = { TextButton(onClick = { topUpGoal = null }) { Text("Cancel") } }
        )
    }

    // Buy Wishlist Item
    buyingWishlist?.let { w ->
        val activeAccs = remember(state.accounts) { state.accounts.filter { !it.isArchived } }
        var selectedAcc by remember(activeAccs) {
            mutableStateOf(
                activeAccs.firstOrNull { !it.accountType.contains("Cash", ignoreCase = true) }
                    ?: activeAccs.firstOrNull()
            )
        }
        var expandAcc by remember { mutableStateOf(false) }
        var cat by remember { mutableStateOf("Shopping") }
        AlertDialog(
            onDismissRequest = { buyingWishlist = null },
            containerColor = palette.surface,
            title = { Text("Bought It → Log Expense", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${w.name} — ${displayAmount(w.price, settings)}", fontWeight = FontWeight.Bold, color = palette.primary)
                    Box {
                        OutlinedButton(onClick = { expandAcc = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Pay From: ${selectedAcc?.name ?: "Select Wallet"}")
                        }
                        DropdownMenu(expanded = expandAcc, onDismissRequest = { expandAcc = false }) {
                            activeAccs.forEach { acc ->
                                DropdownMenuItem(text = { Text("${acc.name} (${acc.accountType})") }, onClick = { selectedAcc = acc; expandAcc = false })
                            }
                        }
                    }
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Expense Category") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val accId = selectedAcc?.id ?: return@Button
                    viewModel.buyWishlistItem(w, accId, cat.trim().ifBlank { "Shopping" })
                    buyingWishlist = null
                }) { Text("Confirm & Log") }
            },
            dismissButton = { TextButton(onClick = { buyingWishlist = null }) { Text("Cancel") } }
        )
    }

    // Record Loan Repayment Dialog
    repayingLoan?.let { loan ->
        val activeAccs = remember(state.accounts) { state.accounts.filter { !it.isArchived } }
        var selectedAcc by remember(activeAccs) {
            mutableStateOf(
                activeAccs.firstOrNull { !it.accountType.contains("Cash", ignoreCase = true) }
                    ?: activeAccs.firstOrNull()
            )
        }
        var expandAcc by remember { mutableStateOf(false) }
        var repayAmt by remember(loan) {
            mutableStateOf(MoneyPaise.formatInput(MoneyPaise.toPaise(loan.monthlyPayment.coerceAtLeast(500.0))))
        }
        var repayNote by remember { mutableStateOf("EMI Repayment") }
        AlertDialog(
            onDismissRequest = { repayingLoan = null },
            containerColor = palette.surface,
            title = { Text("Record Repayment · ${loan.name}", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = repayAmt,
                        onValueChange = { repayAmt = it },
                        label = { Text("Repayment Amount (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Box {
                        OutlinedButton(onClick = { expandAcc = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Pay From Wallet: ${selectedAcc?.name ?: "None"}")
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
                    OutlinedTextField(
                        value = repayNote,
                        onValueChange = { repayNote = it },
                        label = { Text("Note (e.g. Monthly EMI or Extra Principal)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = repayAmt.toDoubleOrNull() ?: return@Button
                    if (amt > 0) {
                        viewModel.addLoanPayment(loan.id, amt, selectedAcc?.id, repayNote)
                        repayingLoan = null
                    }
                }) { Text("Save Repayment") }
            },
            dismissButton = { TextButton(onClick = { repayingLoan = null }) { Text("Cancel") } }
        )
    }

    // Loan Add/Edit
    if (showAddLoan || editingLoan != null) {
        var name by remember(editingLoan) { mutableStateOf(editingLoan?.name ?: "") }
        var principal by remember(editingLoan) { mutableStateOf(editingLoan?.principal?.let { MoneyPaise.formatInput(MoneyPaise.toPaise(it)) } ?: "") }
        var emi by remember(editingLoan) { mutableStateOf(editingLoan?.monthlyPayment?.let { MoneyPaise.formatInput(MoneyPaise.toPaise(it)) } ?: "") }
        var apr by remember(editingLoan) { mutableStateOf(editingLoan?.interestRate?.toString() ?: "10.5") }
        AlertDialog(
            onDismissRequest = { showAddLoan = false; editingLoan = null },
            containerColor = palette.surface,
            title = { Text(if (editingLoan == null) "Add Loan / Debt" else "Edit Loan / Debt", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Loan Name") })
                    OutlinedTextField(value = principal, onValueChange = { principal = it }, label = { Text("Principal (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = emi, onValueChange = { emi = it }, label = { Text("Monthly EMI (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = apr, onValueChange = { apr = it }, label = { Text("APR %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = principal.toDoubleOrNull() ?: return@Button
                    val m = emi.toDoubleOrNull() ?: 1000.0
                    val r = apr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && p > 0) {
                        val ed = editingLoan
                        if (ed == null) viewModel.addLoan(name.trim(), p, m, r)
                        else viewModel.updateLoan(ed, name.trim(), p, m, r)
                        showAddLoan = false
                        editingLoan = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddLoan = false; editingLoan = null }) { Text("Cancel") } }
        )
    }

    if (showAddWishlist) {
        var n by remember { mutableStateOf("") }
        var p by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddWishlist = false },
            containerColor = palette.surface,
            title = { Text("Add to 24h Cooling Wishlist", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Item Name") })
                    OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Price (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pr = p.toDoubleOrNull() ?: return@Button
                    if (n.isNotBlank() && pr > 0) { viewModel.addWishlistItem(n.trim(), pr); showAddWishlist = false }
                }) { Text("Start 24h Timer") }
            },
            dismissButton = { TextButton(onClick = { showAddWishlist = false }) { Text("Cancel") } }
        )
    }

    if (showAddCashback) {
        val activeAccs = remember(state.accounts) { state.accounts.filter { !it.isArchived } }
        var selectedAcc by remember(activeAccs) {
            mutableStateOf(
                activeAccs.firstOrNull { !it.accountType.contains("Cash", ignoreCase = true) }
                    ?: activeAccs.firstOrNull()
            )
        }
        var creditToWallet by remember { mutableStateOf(true) }
        var expandAcc by remember { mutableStateOf(false) }
        var s by remember { mutableStateOf("GPay / Card") }
        var a by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCashback = false },
            containerColor = palette.surface,
            title = { Text("🎁 Log Cashback / Reward", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = s, onValueChange = { s = it }, label = { Text("Source App / Card") })
                    OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("Amount (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Credit directly to wallet balance", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                        Switch(checked = creditToWallet, onCheckedChange = { creditToWallet = it })
                    }
                    if (creditToWallet && activeAccs.isNotEmpty()) {
                        Box {
                            OutlinedButton(onClick = { expandAcc = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Wallet: ${selectedAcc?.name ?: "Select Account"}")
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
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = a.toDoubleOrNull() ?: return@Button
                    if (s.isNotBlank() && amt > 0) {
                        viewModel.addCashbackEntry(s.trim(), amt, note, if (creditToWallet) selectedAcc?.id else null)
                        showAddCashback = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddCashback = false }) { Text("Cancel") } }
        )
    }

    if (showAddValueItem || editingValueItem != null) {
        val ed = editingValueItem
        var n by remember(ed) { mutableStateOf(ed?.name ?: "") }
        var c by remember(ed) { mutableStateOf(ed?.cost?.let { MoneyPaise.formatInput(MoneyPaise.toPaise(it)) } ?: "") }
        var u by remember(ed) { mutableStateOf((ed?.uses ?: 1).toString()) }
        AlertDialog(
            onDismissRequest = { showAddValueItem = false; editingValueItem = null },
            containerColor = palette.surface,
            title = { Text(if (ed == null) "Add Price-Per-Use Item" else "Edit Price-Per-Use Item", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Item (e.g. Headphones)") })
                    OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text("Purchase Cost (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = u, onValueChange = { u = it }, label = { Text("Total Uses So Far") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cost = c.toDoubleOrNull() ?: return@Button
                    val uses = (u.toIntOrNull() ?: 1).coerceAtLeast(1)
                    if (n.isNotBlank() && cost > 0) {
                        if (ed == null) viewModel.addValueItem(n.trim(), cost, uses)
                        else viewModel.updateValueItem(ed, n.trim(), cost, uses)
                        showAddValueItem = false
                        editingValueItem = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddValueItem = false; editingValueItem = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun PeopleScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    initialTab: String = "CONTACTS",
    onBack: () -> Unit
) {
    var peopleTab by remember(initialTab) { mutableStateOf(initialTab) } // CONTACTS or KHATA
    var showAddPerson by remember { mutableStateOf(false) }
    var showAddKhata by remember { mutableStateOf(false) }
    var editingKhataEntry by remember { mutableStateOf<KhataEntryItem?>(null) }
    var personToDelete by remember { mutableStateOf<PersonEntity?>(null) }
    var showSplitModal by remember { mutableStateOf(false) }
    var activeSummary by remember { mutableStateOf<PersonLedgerSummary?>(null) }
    var pName by remember { mutableStateOf("") }
    var pPhone by remember { mutableStateOf("") }
    var pNotes by remember { mutableStateOf("") }
    var pUpiId by remember { mutableStateOf("") }

    var khataShop by remember { mutableStateOf("") }
    var khataItem by remember { mutableStateOf("") }
    var khataAmount by remember { mutableStateOf("") }
    var khataCategory by remember { mutableStateOf("Khata & Kirana") }

    val pendingKhata = remember(state.khataEntries) {
        state.khataEntries.filter { !it.isSettled }
    }
    val khataGroupedByShop = remember(pendingKhata) {
        pendingKhata.groupBy { it.shopOrPersonName.trim() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }
                Text("👥 People, Split & Khata", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { showSplitModal = true }, modifier = Modifier.testTag("open_smart_split_btn")) {
                    Text("⚖️ Split Bill")
                }
                if (peopleTab == "KHATA") {
                    Button(onClick = { showAddKhata = true }, modifier = Modifier.testTag("add_khata_btn")) {
                        Text("+ Khata")
                    }
                } else {
                    Button(onClick = { showAddPerson = true }, modifier = Modifier.testTag("add_person_btn")) {
                        Text("+ Person")
                    }
                }
            }
        }

        // Sub-Tab Switcher: Contacts & Smart Split vs. Friend / Shopkeeper Khata (Pay Later)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = peopleTab == "CONTACTS",
                onClick = { peopleTab = "CONTACTS" },
                label = { Text("👥 People & UPI (${state.personSummaries.size})") },
                modifier = Modifier.testTag("people_tab_contacts")
            )
            FilterChip(
                selected = peopleTab == "KHATA",
                onClick = { peopleTab = "KHATA" },
                label = {
                    val pendingSum = pendingKhata.sumOf { it.amount }
                    Text("📓 Khata Pay Later (${pendingKhata.size} · ${displayAmount(pendingSum, settings)})")
                },
                modifier = Modifier.testTag("people_tab_khata")
            )
        }

        if (peopleTab == "KHATA") {
            // Feature #6: Friend / Shopkeeper "Khata" (Pay Later) Tab
            GlassCard(
                settings = settings,
                modifier = Modifier.fillMaxWidth().testTag("khata_summary_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "📓 Friend / Shopkeeper Khata (Pay Later)",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Log canteen, kirana, milk/water can, or chai-stall tabs and settle with 1 tap at week's end.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Button(onClick = { showAddKhata = true }, modifier = Modifier.testTag("khata_quick_add_btn")) {
                            Text("+ Log Credit")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf(
                            Triple("Chai Stall", "Tea & Samosa", 30.0),
                            Triple("College Canteen", "Lunch / Maggi", 60.0),
                            Triple("Kirana Store", "Milk & Bread", 85.0),
                            Triple("Laundry / Iron", "Clothes Press", 50.0)
                        ).forEach { (shop, item, amt) ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.addKhataEntry(shop, item, amt, "Khata & Kirana") },
                                label = { Text("+ $shop ₹${amt.toInt()}") }
                            )
                        }
                    }
                }
            }

            if (khataGroupedByShop.isEmpty()) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No pending Khata credits! Tap '+ Add Khata Entry' or a quick chip above whenever you buy on tab.",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { editingKhataEntry = null; showAddKhata = true }) { Text("+ Add Khata Entry") }
                    }
                }
            } else {
                khataGroupedByShop.forEach { (shopName, entries) ->
                    val shopTotal = entries.sumOf { it.amount }
                    GlassCard(
                        settings = settings,
                        modifier = Modifier.fillMaxWidth().testTag("khata_group_$shopName")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "🏪 $shopName (${entries.size} items)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Pending Khata: ${displayAmount(shopTotal, settings)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Button(
                                    onClick = { viewModel.clearKhataForShopOrPerson(shopName) },
                                    modifier = Modifier.testTag("clear_khata_btn_$shopName")
                                ) {
                                    Text("Clear Khata ✓")
                                }
                            }
                            entries.forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(palette.surfaceElevated.copy(alpha = 0.6f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.itemNote, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                        Text(TransactionDateTime.formatDateInput(entry.createdAt), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(displayAmount(entry.amount, settings), style = MaterialTheme.typography.labelLarge, color = palette.primary, fontWeight = FontWeight.Bold)
                                        TextButton(onClick = { viewModel.settleSingleKhataEntry(entry) }) {
                                            Text("Mark Paid", style = MaterialTheme.typography.labelSmall)
                                        }
                                        IconButton(
                                            onClick = {
                                                editingKhataEntry = entry
                                                khataShop = entry.shopOrPersonName
                                                khataItem = entry.itemNote
                                                khataAmount = MoneyPaise.formatInput(MoneyPaise.toPaise(entry.amount))
                                                khataCategory = "Khata & Kirana"
                                                showAddKhata = true
                                            },
                                            modifier = Modifier.size(28.dp).testTag("edit_khata_${entry.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Khata", tint = palette.secondary, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteKhataEntry(entry.id) },
                                            modifier = Modifier.size(28.dp).testTag("delete_khata_${entry.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Khata", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (state.personSummaries.isEmpty()) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No contacts yet. Add a friend to track borrowed/lent balances, save UPI IDs, and split bills!",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showAddPerson = true }) { Text("+ Add Person") }
                    }
                }
            }

            state.personSummaries.forEach { sum ->
                val savedUpi = state.personUpiCards[sum.person.id]
                GlassCard(
                    settings = settings,
                    onClick = { activeSummary = sum },
                    modifier = Modifier.fillMaxWidth().testTag("person_card_${sum.person.id}")
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sum.person.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    "Lent: ${displayAmount(sum.lent, settings)} · Borrowed: ${displayAmount(sum.borrowed, settings)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                                if (sum.person.phone.isNotBlank()) {
                                    Text(sum.person.phone, style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (sum.net >= 0) "+${displayAmount(sum.net, settings)}" else displayAmount(sum.net, settings),
                                    color = if (sum.net >= 0) palette.primary else Color(0xFFF43F5E),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { personToDelete = sum.person }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove contact", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                        if (savedUpi != null && savedUpi.upiId.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.surfaceElevated.copy(alpha = 0.65f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "📱 UPI: ${savedUpi.upiId}" + if (savedUpi.qrImageUri.isNotBlank()) " · 📷 QR Saved" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = { hostCallbacks.copyToClipboard(savedUpi.upiId) },
                                    modifier = Modifier.testTag("copy_person_upi_${sum.person.id}")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy UPI ID", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeSummary?.let { initialSum ->
        val currentSum = state.personSummaries.firstOrNull { it.person.id == initialSum.person.id } ?: initialSum
        PersonDetailDialog(
            summary = currentSum,
            state = state,
            settings = settings,
            palette = palette,
            viewModel = viewModel,
            hostCallbacks = hostCallbacks,
            onDismiss = { activeSummary = null }
        )
    }

    if (showAddKhata || editingKhataEntry != null) {
        val ed = editingKhataEntry
        AlertDialog(
            onDismissRequest = { showAddKhata = false; editingKhataEntry = null },
            containerColor = palette.surface,
            title = {
                Text(
                    if (ed == null) "📓 Add Pending Khata (Pay Later)" else "✏️ Edit Khata Entry",
                    color = palette.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = khataShop,
                        onValueChange = { khataShop = it },
                        label = { Text("Shopkeeper / Friend (e.g. Raju Kirana / Canteen)") }
                    )
                    OutlinedTextField(
                        value = khataItem,
                        onValueChange = { khataItem = it },
                        label = { Text("Item / Note (e.g. Milk + Maggi)") }
                    )
                    OutlinedTextField(
                        value = khataAmount,
                        onValueChange = { khataAmount = it },
                        label = { Text("Amount (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = khataCategory,
                        onValueChange = { khataCategory = it },
                        label = { Text("Category") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = khataAmount.toDoubleOrNull() ?: return@Button
                    if (khataShop.isNotBlank() && amt > 0) {
                        if (ed == null) {
                            viewModel.addKhataEntry(khataShop.trim(), khataItem.ifBlank { "Khata Item" }, amt, khataCategory.ifBlank { "Khata & Kirana" })
                        } else {
                            viewModel.updateKhataEntry(ed.id, khataShop.trim(), khataItem.ifBlank { "Khata Item" }, amt)
                        }
                        khataItem = ""; khataAmount = ""; editingKhataEntry = null; showAddKhata = false
                    }
                }) { Text("Save Khata") }
            },
            dismissButton = { TextButton(onClick = { showAddKhata = false; editingKhataEntry = null }) { Text("Cancel") } }
        )
    }

    personToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            containerColor = palette.surface,
            title = { Text("Remove ${target.name}?", color = palette.textPrimary) },
            text = {
                Text(
                    "Remove ${target.name} from your People directory? Existing ledger transactions remain safe.",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    onClick = {
                        viewModel.deletePerson(target)
                        personToDelete = null
                    }
                ) { Text("Remove Contact") }
            },
            dismissButton = { TextButton(onClick = { personToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showAddPerson) {
        AlertDialog(
            onDismissRequest = { showAddPerson = false },
            containerColor = palette.surface,
            title = { Text("Add Person / Friend", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Name") })
                    OutlinedTextField(value = pPhone, onValueChange = { pPhone = it }, label = { Text("Phone (optional)") })
                    OutlinedTextField(value = pUpiId, onValueChange = { pUpiId = it }, label = { Text("Saved UPI ID (e.g. name@okaxis, optional)") })
                    OutlinedTextField(value = pNotes, onValueChange = { pNotes = it }, label = { Text("Notes") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pName.isNotBlank()) {
                        viewModel.addPerson(pName.trim(), pPhone.trim(), pNotes.trim(), pUpiId.trim())
                        pName = ""; pPhone = ""; pNotes = ""; pUpiId = ""; showAddPerson = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddPerson = false }) { Text("Cancel") } }
        )
    }

    if (showSplitModal) {
        SmartSplitBillDialog(
            state = state,
            settings = settings,
            palette = palette,
            onDismiss = { showSplitModal = false },
            onCopySummary = { summary -> hostCallbacks.copyToClipboard(summary) },
            onLogSplits = { selectedIds, shares, title ->
                val accId = state.accounts.firstOrNull { !it.isArchived }?.id ?: return@SmartSplitBillDialog
                selectedIds.forEach { pid ->
                    val share = shares[pid] ?: 0.0
                    if (share > 0.0) {
                        viewModel.logSplitShareForPerson(pid, accId, share, title, youOweThem = false)
                    }
                }
                showSplitModal = false
            }
        )
    }
}

@Composable
private fun PersonDetailDialog(
    summary: PersonLedgerSummary,
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onDismiss: () -> Unit
) {
    val existingUpiCard = state.personUpiCards[summary.person.id]
    var editingMode by remember { mutableStateOf(false) }
    var name by remember(summary.person) { mutableStateOf(summary.person.name) }
    var phone by remember(summary.person) { mutableStateOf(summary.person.phone) }
    var notes by remember(summary.person) { mutableStateOf(summary.person.notes) }
    var upiIdInput by remember(existingUpiCard) { mutableStateOf(existingUpiCard?.upiId ?: "") }
    var qrUriInput by remember(existingUpiCard) { mutableStateOf(existingUpiCard?.qrImageUri ?: "") }
    val activeAccs = remember(state.accounts) { state.accounts.filter { !it.isArchived } }
    var settleAcc by remember(activeAccs) { mutableStateOf(activeAccs.firstOrNull()) }
    var expandAcc by remember { mutableStateOf(false) }

    val history = remember(state.transactions, summary.person.id) {
        state.transactions.filter { it.personId == summary.person.id }.sortedByDescending { it.timestamp }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("👤 ${summary.person.name}", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = { editingMode = !editingMode }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit contact", tint = palette.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editingMode) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = upiIdInput, onValueChange = { upiIdInput = it }, label = { Text("UPI ID (e.g. friend@okicici)") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                viewModel.updatePerson(summary.person, name, phone, notes)
                                viewModel.savePersonUpiCard(summary.person.id, upiIdInput, qrUriInput)
                                editingMode = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Contact & UPI") }
                } else {
                    if (summary.person.phone.isNotBlank() || summary.person.notes.isNotBlank()) {
                        Text(
                            "${summary.person.phone} ${summary.person.notes}".trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }

                    // Feature #7: Saved UPI ID / QR Card per Person
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceElevated)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📱 Saved UPI ID & QR Card", style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = upiIdInput,
                            onValueChange = { upiIdInput = it },
                            label = { Text("UPI ID (e.g. ravi@okaxis)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("person_upi_id_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.savePersonUpiCard(summary.person.id, upiIdInput, qrUriInput) },
                                modifier = Modifier.weight(1f).testTag("save_person_upi_btn")
                            ) {
                                Text("Save UPI", style = MaterialTheme.typography.labelSmall)
                            }
                            if (upiIdInput.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { hostCallbacks.copyToClipboard(upiIdInput.trim()) },
                                    modifier = Modifier.weight(1f).testTag("detail_copy_upi_btn")
                                ) {
                                    Text("Copy UPI ID", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    hostCallbacks.pickTrackerAttachment { _, _, localPath, _ ->
                                        qrUriInput = localPath
                                        viewModel.savePersonUpiCard(summary.person.id, upiIdInput, localPath)
                                    }
                                }
                            ) {
                                Text(if (qrUriInput.isNotBlank()) "📷 QR✓" else "+ QR", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(palette.surfaceElevated).padding(10.dp)) {
                            Text("Lent (Receivable)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(displayAmount(summary.lent, settings), fontWeight = FontWeight.Bold, color = palette.primary)
                        }
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(palette.surfaceElevated).padding(10.dp)) {
                            Text("Borrowed (Payable)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(displayAmount(summary.borrowed, settings), fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.primary.copy(alpha = 0.14f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (summary.net >= 0) "Net Owed to You" else "Net You Owe",
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                        Text(
                            displayAmount(abs(summary.net), settings),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.net >= 0) palette.primary else Color(0xFFF43F5E)
                        )
                    }

                    if (abs(summary.net) >= 0.01 && activeAccs.isNotEmpty()) {
                        Box {
                            OutlinedButton(onClick = { expandAcc = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Settle via Wallet: ${settleAcc?.name ?: "Select"}")
                            }
                            DropdownMenu(expanded = expandAcc, onDismissRequest = { expandAcc = false }) {
                                activeAccs.forEach { acc ->
                                    DropdownMenuItem(text = { Text(acc.name) }, onClick = { settleAcc = acc; expandAcc = false })
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val accId = settleAcc?.id ?: return@Button
                                    viewModel.settlePersonBalance(summary.person.id, accId, summary.net, "Settled with ${summary.person.name}")
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f).testTag("settle_person_btn")
                            ) { Text("1-Tap Settle Up") }
                            OutlinedButton(
                                onClick = {
                                    val dir = if (summary.net >= 0) "owes you" else "you owe"
                                    val upiLine = if (upiIdInput.isNotBlank()) "\nUPI ID: $upiIdInput" else ""
                                    val msg = "*Tracmio Ledger · ${summary.person.name}*\nLent: ${displayAmount(summary.lent, settings)}\nBorrowed: ${displayAmount(summary.borrowed, settings)}\nBalance ($dir): ${displayAmount(abs(summary.net), settings)}$upiLine"
                                    hostCallbacks.copyToClipboard(msg)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("WhatsApp Copy") }
                        }
                    }

                    Text("Ledger History (${history.size})", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                    if (history.isEmpty()) {
                        Text("No borrow/lend entries with ${summary.person.name} yet.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    } else {
                        history.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.surfaceElevated.copy(alpha = 0.6f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${tx.type} · ${tx.category}", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                                    Text("${TransactionDateTime.formatDateInput(tx.timestamp)} ${tx.note}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                                Text(displayAmount(tx.amount, settings), fontWeight = FontWeight.Bold, color = palette.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun SmartSplitBillDialog(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onCopySummary: (String) -> Unit,
    onLogSplits: (List<Long>, Map<Long, Double>, String) -> Unit
) {
    var title by remember { mutableStateOf("Dinner / Trip Split") }
    var totalStr by remember { mutableStateOf("") }
    var splitMode by remember { mutableStateOf("EQUAL") } // EQUAL, PERCENT, EXACT
    var includeMe by remember { mutableStateOf(true) }
    val selectedIds = remember { mutableStateMapOf<Long, Boolean>() }
    val customValues = remember { mutableStateMapOf<Long, String>() }

    val total = totalStr.toDoubleOrNull() ?: 0.0
    val chosen = state.persons.filter { selectedIds[it.id] == true }
    val count = (chosen.size + if (includeMe) 1 else 0).coerceAtLeast(1)
    val shares = remember(total, chosen, splitMode, includeMe, customValues.toMap()) {
        chosen.associate { p ->
            val v = when (splitMode) {
                "EQUAL" -> total / count
                "PERCENT" -> total * ((customValues[p.id]?.toDoubleOrNull() ?: 0.0) / 100.0)
                else -> customValues[p.id]?.toDoubleOrNull() ?: 0.0
            }
            p.id to v
        }
    }
    val othersSum = shares.values.sum()
    val myShare = if (!includeMe) 0.0 else if (splitMode == "EQUAL") total / count else (total - othersSum).coerceAtLeast(0.0)

    val summaryText = remember(title, total, chosen, shares, includeMe, myShare, state.personUpiCards) {
        buildString {
            appendLine("*$title* — Total: ${MoneyPaise.formatCurrency(total)}")
            if (includeMe) {
                appendLine("• Me (Your Share): ${MoneyPaise.formatCurrency(myShare)}")
            }
            chosen.forEach { p ->
                val upiTag = state.personUpiCards[p.id]?.upiId?.takeIf { it.isNotBlank() }?.let { " (UPI: $it)" } ?: ""
                appendLine("• ${p.name}: ${MoneyPaise.formatCurrency(shares[p.id] ?: 0.0)}$upiTag")
            }
            append("Tracked with Tracmio")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("⚖️ Smart Split & WhatsApp Summary", color = palette.textPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Bill Title") })
                OutlinedTextField(value = totalStr, onValueChange = { totalStr = it }, label = { Text("Total Bill (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("EQUAL", "PERCENT", "EXACT").forEach { m ->
                        FilterChip(selected = splitMode == m, onClick = { splitMode = m }, label = { Text(m) })
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeMe, onCheckedChange = { includeMe = it })
                        Text("Include Me in Split", color = palette.textPrimary)
                    }
                    if (includeMe && total > 0) {
                        Text(
                            "Your Share: ${MoneyPaise.formatCurrency(myShare)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                state.persons.forEach { p ->
                    val checked = selectedIds[p.id] == true
                    val personUpi = state.personUpiCards[p.id]?.upiId?.takeIf { it.isNotBlank() }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = { selectedIds[p.id] = it })
                                Column {
                                    Text(p.name, color = palette.textPrimary)
                                    if (personUpi != null) {
                                        Text("UPI: $personUpi", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                                    }
                                }
                            }
                            if (checked && splitMode != "EQUAL") {
                                OutlinedTextField(
                                    value = customValues[p.id] ?: "",
                                    onValueChange = { customValues[p.id] = it },
                                    label = { Text(if (splitMode == "PERCENT") "%" else settings.currencySymbol) },
                                    modifier = Modifier.width(90.dp)
                                )
                            } else if (checked) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(MoneyPaise.formatCurrency(shares[p.id] ?: 0.0), color = palette.primary, fontWeight = FontWeight.Bold)
                                    if (personUpi != null) {
                                        TextButton(onClick = { onCopySummary(personUpi) }) {
                                            Text("Copy UPI", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onCopySummary(summaryText) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("WhatsApp Copy")
                }
                Button(onClick = { onLogSplits(chosen.map { it.id }, shares, title) }, enabled = chosen.isNotEmpty() && total > 0) {
                    Text("Log Split")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
