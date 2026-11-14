package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracemeo.app.data.*
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    onQuickAdd: () -> Unit,
    onOpenTransactionReceipt: (TransactionEntity) -> Unit,
    onNavigate: (String) -> Unit
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    val userName = settings.userName.ifBlank { "Charan" }
    val curr = settings.currencySymbol
    val mask = settings.privacyMaskActive
    val stealth = settings.stealthModeActive

    val pace = remember(state.accounts, state.transactions, state.budgets) {
        NativeIntelligenceCalculators.computeMonthlyPace(state.accounts, state.transactions, state.budgets)
    }
    val safeToSpend = remember(state.accounts, state.transactions) {
        NativeLedgerCalculator.computeSafeToSpend(state.accounts, state.transactions)
    }
    var showAddChipDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("dashboard_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "$greeting, $userName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        // 1. Hero Total Balance Card (Reference Screenshot 2 Design)
        item {
            GlassCard(
                settings = settings,
                shape = RoundedCornerShape(28.dp),
                borderColor = Color(0x6600F5D4),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0E4B5A).copy(alpha = 0.92f),
                                    Color(0xFF082838).copy(alpha = 0.94f),
                                    Color(0xFF281B4E).copy(alpha = 0.90f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            "TOTAL BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            MoneyPaise.formatCurrency(state.totalBalance, curr, stealth, mask),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Income Sub-card with emerald/cyan glow
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF073B3A).copy(alpha = 0.85f))
                                    .border(1.4.dp, Color(0xFF00F5D4).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                    .padding(vertical = 14.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                            .border(1.5.dp, Color(0xFFA7F3D0), CircleShape)
                                    ) {
                                        Text("↓", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    }
                                    Text(
                                        MoneyPaise.formatCurrency(state.totalIncome, curr, stealth, mask),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.textPrimary
                                    )
                                    Text("Income", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                            }

                            // Expense Sub-card with purple/coral glow
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF231942).copy(alpha = 0.85f))
                                    .border(1.4.dp, Color(0xFFA855F7).copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                                    .padding(vertical = 14.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                            .border(1.5.dp, Color(0xFFFECDD3), CircleShape)
                                    ) {
                                        Text("↑", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    }
                                    Text(
                                        MoneyPaise.formatCurrency(state.totalExpense, curr, stealth, mask),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.textPrimary
                                    )
                                    Text("Expense", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Monthly Pace Card
        item {
            LiquidGlassComponents(settings = settings, palette = palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🕒", fontSize = 16.sp)
                        Text(
                            "MONTHLY PACE",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (pace.isOnTrack) Color(0xFF064E3B) else Color(0xFF78350F),
                        modifier = Modifier.border(
                            1.dp,
                            if (pace.isOnTrack) Color(0xFF10B981) else Color(0xFFF59E0B),
                            RoundedCornerShape(16.dp)
                        )
                    ) {
                        Text(
                            if (pace.isOnTrack) "● On Track" else "● Overpace",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pace.isOnTrack) Color(0xFF34D399) else Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                val progress = (pace.spentSoFar / pace.totalBudget.coerceAtLeast(1.0)).toFloat().coerceIn(0.05f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = palette.primary,
                    trackColor = palette.surfaceElevated
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${pace.remainingDays} days left in month", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Text(
                        "${MoneyPaise.formatCurrency(pace.dailySafeLimit, curr, stealth, mask)}/day safe limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Safe-to-Spend Today Card
        item {
            LiquidGlassComponents(settings = settings, palette = palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛡️", fontSize = 16.sp)
                        Text(
                            "SAFE-TO-SPEND TODAY",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        "${MoneyPaise.formatCurrency(safeToSpend.dailyAllowance, curr, stealth, mask)} / day",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Spent today: ${MoneyPaise.formatCurrency(safeToSpend.spentToday, curr, stealth, mask)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                    Text(
                        "${safeToSpend.remainingDaysInMonth}d left · Unallocated ${MoneyPaise.formatCurrency(safeToSpend.unallocatedBalance, curr, stealth, mask)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // 4. 1-Tap Quick Spend Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("⚡", fontSize = 18.sp)
                        Text(
                            "1-TAP QUICK SPEND",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.primary.copy(alpha = 0.2f),
                        modifier = Modifier
                            .border(1.dp, palette.primary, RoundedCornerShape(16.dp))
                            .clickable { showAddChipDialog = true }
                            .testTag("add_quick_chip_button")
                    ) {
                        Text(
                            "+ Chip",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.quickSpendChips, key = { it.id }) { chip ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(palette.surface.copy(alpha = 0.9f))
                                .border(1.2.dp, palette.primary.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                                .clickable { viewModel.executeQuickSpendChip(chip) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("quick_chip_${chip.id}")
                        ) {
                            Text(chip.emoji)
                            Text(chip.title, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("$curr${chip.amount.toInt()}", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFB7185).copy(alpha = 0.25f))
                                    .clickable { viewModel.deleteQuickSpendChip(chip) }
                            ) {
                                Text("×", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddChipDialog) {
        var emoji by remember { mutableStateOf("🍔") }
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddChipDialog = false },
            containerColor = palette.surfaceElevated,
            title = { Text("⚡ Add 1-Tap Quick Spend Chip", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Emoji") }, singleLine = true)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Label (e.g. Metro, Snacks)") }, singleLine = true)
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ($curr)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amt > 0.0) {
                            viewModel.addQuickSpendChip(emoji.ifBlank { "⚡" }, title.trim(), amt)
                            showAddChipDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) { Text("Save Chip", color = palette.background, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddChipDialog = false }) { Text("Cancel", color = palette.textSecondary) }
            }
        )
    }
}

@Composable
fun AccountsScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel
) {
    val curr = settings.currencySymbol
    val mask = settings.privacyMaskActive
    val stealth = settings.stealthModeActive

    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showCreateAccount by remember { mutableStateOf(false) }
    var allocatingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var cashCountingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var reconcilingAccount by remember { mutableStateOf<AccountEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("accounts_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Accounts & Wallets", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary, fontWeight = FontWeight.ExtraBold)
                Button(
                    onClick = { showCreateAccount = true },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("+ Add Account", color = palette.background, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // 1-Tap ATM Withdrawal (Bank -> Cash) Card
        item {
            LiquidGlassComponents(settings = settings, palette = palette) {
                Text("🏧 1-Tap ATM Withdrawal (Bank → Cash)", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                Text("Instant atomic transfer from primary Bank to Cash Wallet", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(500.0, 1000.0, 2000.0).forEach { amt ->
                        Button(
                            onClick = { viewModel.performAtmWithdrawal(amt) },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$curr${amt.toInt()}", color = palette.primary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        items(state.accounts, key = { it.id }) { acc ->
            val computedBalance = FinancialCalculator.balance(acc, state.transactions)
            val allocations = remember(acc.allocationsJson) {
                NativeLedgerCalculator.parseAllocations(acc.allocationsJson)
            }
            val allocatedSum = allocations.sumOf { it.allocatedAmount }
            val unallocated = (computedBalance - allocatedSum).coerceAtLeast(0.0)
            val isCreditCard = acc.accountType.equals("Credit Card", true)
            val ccUtilization = if (isCreditCard && acc.creditLimit > 0.0) (computedBalance / acc.creditLimit) * 100.0 else 0.0

            LiquidGlassComponents(settings = settings, palette = palette) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(acc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                            Surface(shape = RoundedCornerShape(8.dp), color = palette.surfaceElevated) {
                                Text(acc.accountType, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = palette.primary)
                            }
                        }
                        if (isCreditCard) {
                            Text(
                                "Stmt Day ${acc.statementDay} · Due Day ${acc.dueDay} · Limit $curr${"%,.0f".format(acc.creditLimit)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Text(
                        MoneyPaise.formatCurrency(computedBalance, curr, stealth, mask),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.primary
                    )
                }

                if (isCreditCard && ccUtilization > 30.0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF78350F).copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            "⚠️ >30% Credit Utilization (${"%.1f".format(ccUtilization)}%) — Pay early to protect credit score!",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (allocations.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        allocations.forEach { a ->
                            Surface(shape = RoundedCornerShape(10.dp), color = palette.surfaceElevated) {
                                Text(
                                    "${a.category} · $curr${"%,.0f".format(a.allocatedAmount)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary
                                )
                            }
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = palette.surface) {
                            Text(
                                "Unallocated · $curr${"%,.0f".format(unallocated)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { allocatingAccount = acc }, shape = RoundedCornerShape(12.dp)) {
                        Text("📂 Allocate", color = palette.primary, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = { reconcilingAccount = acc }, shape = RoundedCornerShape(12.dp)) {
                        Text("⚖️ Reconcile", color = palette.textPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                    if (acc.accountType.equals("Cash", true) || acc.accountType.equals("Wallet", true)) {
                        OutlinedButton(onClick = { cashCountingAccount = acc }, shape = RoundedCornerShape(12.dp)) {
                            Text("💵 Cash Count", color = palette.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedButton(onClick = { editingAccount = acc }, shape = RoundedCornerShape(12.dp)) {
                        Text("✏️ Edit", color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showCreateAccount || editingAccount != null) {
        AccountEditDialog(
            initialAccount = editingAccount,
            palette = palette,
            onDismiss = {
                showCreateAccount = false
                editingAccount = null
            },
            onSave = { name, type, bal, limit, stmt, due ->
                viewModel.saveAccount(editingAccount?.id, name, type, bal, limit, stmt, due)
                showCreateAccount = false
                editingAccount = null
            }
        )
    }

    allocatingAccount?.let { acc ->
        AccountAllocationDialog(
            account = acc,
            palette = palette,
            currencySymbol = curr,
            liveBalance = FinancialCalculator.balance(acc, state.transactions),
            onDismiss = { allocatingAccount = null },
            onSaveAllocations = { list ->
                viewModel.updateAccountAllocations(acc, list)
                allocatingAccount = null
            }
        )
    }

    cashCountingAccount?.let { acc ->
        CashCountDenominationDialog(
            account = acc,
            palette = palette,
            currencySymbol = curr,
            onDismiss = { cashCountingAccount = null },
            onApplyReconcile = { countedTotal, note, lines ->
                viewModel.applyCashDenominationCount(lines, note, acc.id)
                cashCountingAccount = null
            }
        )
    }

    reconcilingAccount?.let { acc ->
        var actualBalText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reconcilingAccount = null },
            containerColor = palette.surfaceElevated,
            title = { Text("⚖️ Reconcile ${acc.name}", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter actual current balance to auto-log a Balance Adjustment:", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    OutlinedTextField(
                        value = actualBalText,
                        onValueChange = { actualBalText = it },
                        label = { Text("Actual Balance ($curr)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = actualBalText.toDoubleOrNull()
                        if (target != null) {
                            viewModel.reconcileAccountBalance(acc, target)
                            reconcilingAccount = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) { Text("Reconcile", color = palette.background, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { reconcilingAccount = null }) { Text("Cancel", color = palette.textSecondary) }
            }
        )
    }
}
