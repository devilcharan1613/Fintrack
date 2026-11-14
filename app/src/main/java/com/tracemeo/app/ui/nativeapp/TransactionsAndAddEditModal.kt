package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracemeo.app.data.FinancialCalculator
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.NativeTracmioUiState
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionsListScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    onOpenReceipt: (TransactionEntity) -> Unit
) {
    val curr = settings.currencySymbol
    val mask = settings.privacyMaskActive
    val stealth = settings.stealthModeActive

    var showFilterPanel by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var quickDateRange by remember { mutableStateOf("All dates") }
    var fromDateText by remember { mutableStateOf("") }
    var toDateText by remember { mutableStateOf("") }
    var selectedHashtag by remember { mutableStateOf<String?>(null) }

    val hashtagSummaries = remember(state.transactions) {
        NativeLedgerCalculator.summarizeHashtags(state.transactions)
    }

    val filteredList = remember(state.transactions, selectedAccountId, quickDateRange, fromDateText, toDateText, selectedHashtag) {
        val now = System.currentTimeMillis()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val fromMillis = fromDateText.trim().takeIf { it.length == 10 }?.let { runCatching { df.parse(it)?.time }.getOrNull() }
        val toMillis = toDateText.trim().takeIf { it.length == 10 }?.let { runCatching { df.parse(it)?.time?.plus(86_399_999L) }.getOrNull() }
        state.transactions.filter { tx ->
            val accMatch = selectedAccountId == null || tx.accountId == selectedAccountId
            val dateMatch = when (quickDateRange) {
                "Today" -> NativeLedgerCalculator.dayKey(tx.timestamp) == NativeLedgerCalculator.dayKey(now)
                "Yesterday" -> NativeLedgerCalculator.dayKey(tx.timestamp) == NativeLedgerCalculator.dayKey(now - 86_400_000L)
                "Last 7 days" -> tx.timestamp >= now - 7 * 86_400_000L
                "This month" -> tx.timestamp >= now - 30L * 86_400_000L
                else -> true
            }
            val customFromMatch = fromMillis == null || tx.timestamp >= fromMillis
            val customToMatch = toMillis == null || tx.timestamp <= toMillis
            val tagMatch = selectedHashtag == null ||
                tx.hashtags.contains(selectedHashtag!!, ignoreCase = true) ||
                tx.note.contains(selectedHashtag!!, ignoreCase = true)
            accMatch && dateMatch && customFromMatch && customToMatch && tagMatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("transactions_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top-Left Filter Button + Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showFilterPanel = !showFilterPanel },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (showFilterPanel) "▼ Filter" else "⚙️ Filter", color = palette.primary, fontWeight = FontWeight.ExtraBold)
                }
                Text("Transactions Ledger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
            }
        }

        if (showFilterPanel) {
            item {
                LiquidGlassComponents(settings = settings, palette = palette) {
                    Text("Filter by Account", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val allSel = selectedAccountId == null
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (allSel) palette.primary else palette.surface,
                            modifier = Modifier.clickable { selectedAccountId = null }
                        ) {
                            Text("All Accounts", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (allSel) palette.background else palette.textPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        state.accounts.forEach { acc ->
                            val sel = selectedAccountId == acc.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (sel) palette.primary else palette.surface,
                                modifier = Modifier.clickable { selectedAccountId = acc.id }
                            ) {
                                Text(acc.name, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (sel) palette.background else palette.textPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Quick Date Range", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All dates", "Today", "Yesterday", "Last 7 days", "This month").forEach { range ->
                            val sel = quickDateRange == range
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (sel) palette.primary else palette.surface,
                                modifier = Modifier.clickable { quickDateRange = range }
                            ) {
                                Text(range, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (sel) palette.background else palette.textPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromDateText,
                            onValueChange = { fromDateText = it },
                            label = { Text("From date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toDateText,
                            onValueChange = { toDateText = it },
                            label = { Text("To date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TextButton(
                        onClick = {
                            selectedAccountId = null
                            quickDateRange = "All dates"
                            fromDateText = ""
                            toDateText = ""
                            selectedHashtag = null
                        }
                    ) {
                        Text("✕ Clear filters", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // #Hashtag Expense Totals Strip
        if (hashtagSummaries.isNotEmpty()) {
            item {
                LiquidGlassComponents(settings = settings, palette = palette) {
                    Text("🏷️ #Hashtag Expense & Income Totals", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = palette.primary)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hashtagSummaries.forEach { tagItem ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .clickable { selectedHashtag = tagItem.tag }
                            ) {
                                Text(
                                    "${tagItem.tag} · $curr${"%,.0f".format(tagItem.totalExpense)}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                LiquidGlassComponents(settings = settings, palette = palette) {
                    Text("No transactions found for current filter.", color = palette.textSecondary)
                }
            }
        } else {
            items(filteredList, key = { it.id }) { tx ->
                val isIncome = tx.type.equals("INCOME", true) || tx.type.equals("BORROW", true)
                val dateFormatted = remember(tx.timestamp) {
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
                }
                GlassCard(
                    settings = settings,
                    onClick = { onOpenReceipt(tx) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tx.category, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                                if (tx.subcategory.isNotBlank()) {
                                    Text("· ${tx.subcategory}", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                }
                            }
                            Text("$dateFormatted · ${tx.paymentMethod}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            if (tx.note.isNotBlank()) {
                                Text(tx.note, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                            }
                        }
                        Text(
                            (if (isIncome) "+" else "-") + MoneyPaise.formatCurrency(tx.amount, curr, stealth, mask),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isIncome) palette.primary else Color(0xFFFB7185)
                        )
                    }
                }
            }
        }
    }

    selectedHashtag?.let { tag ->
        val summary = hashtagSummaries.firstOrNull { it.tag.equals(tag, true) }
        AlertDialog(
            onDismissRequest = { selectedHashtag = null },
            containerColor = palette.surfaceElevated,
            title = { Text("🏷️ Hashtag Summary: $tag", fontWeight = FontWeight.ExtraBold, color = palette.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Spent: $curr${"%,.2f".format(summary?.totalExpense ?: 0.0)}", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
                    Text("Total Income: $curr${"%,.2f".format(summary?.totalIncome ?: 0.0)}", color = palette.primary, fontWeight = FontWeight.Bold)
                    Text("Matching Transactions: ${summary?.count ?: 0}", color = palette.textSecondary)
                }
            },
            confirmButton = {
                Button(onClick = { selectedHashtag = null }, colors = ButtonDefaults.buttonColors(containerColor = palette.primary)) {
                    Text("Done", color = palette.background, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AddEditTransactionSheet(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    editingTransaction: TransactionEntity? = null,
    initialType: String = "EXPENSE",
    onDismiss: () -> Unit
) {
    val curr = settings.currencySymbol
    var txType by remember { mutableStateOf(editingTransaction?.type ?: initialType) }
    var amountExpression by remember { mutableStateOf(editingTransaction?.amount?.toString() ?: "") }
    var selectedAccountId by remember {
        mutableLongStateOf(editingTransaction?.accountId ?: state.accounts.firstOrNull()?.id ?: 1L)
    }
    var transferToAccountId by remember {
        mutableLongStateOf(
            editingTransaction?.transferAccountId
                ?: state.accounts.firstOrNull { it.id != selectedAccountId }?.id
                ?: selectedAccountId
        )
    }
    var selectedCategory by remember { mutableStateOf(editingTransaction?.category ?: "Select Category") }
    var subcategory by remember { mutableStateOf(editingTransaction?.subcategory ?: "") }
    var paymentMethod by remember { mutableStateOf(editingTransaction?.paymentMethod ?: "UPI") }
    var upiApp by remember { mutableStateOf(editingTransaction?.upiApp ?: "GPay") }
    var incomeSource by remember { mutableStateOf("💼 Own Earnings / Freelance") }
    var noteText by remember { mutableStateOf(editingTransaction?.note ?: "") }
    var warrantyDate by remember { mutableStateOf(editingTransaction?.warrantyDate ?: "") }
    var selectedAllocationId by remember { mutableStateOf(editingTransaction?.allocationId ?: "") }

    // Half-Cash + Half-UPI Split state
    var cashPortionText by remember { mutableStateOf("") }
    var upiPortionText by remember { mutableStateOf("") }

    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var createAccountDefaultType by remember { mutableStateOf("Wallet") }
    var isSubmitting by remember { mutableStateOf(false) }

    val selectedAccount = state.accounts.firstOrNull { it.id == selectedAccountId }
    val accountAllocations = remember(selectedAccount) {
        NativeLedgerCalculator.parseAllocations(selectedAccount?.allocationsJson)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = palette.surfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (editingTransaction == null) "➕ Add Transaction" else "✏️ Edit Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    TextButton(onClick = onDismiss) {
                        Text("✕ Close", color = palette.textSecondary, fontWeight = FontWeight.Bold)
                    }
                }

                // 5-Card Transaction Type Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("INCOME", "EXPENSE", "TRANSFER", "BORROW", "LEND").forEach { type ->
                        val sel = txType.equals(type, true)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) palette.primary else palette.surface)
                                .clickable { txType = type }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                type.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (sel) palette.background else palette.textPrimary
                            )
                        }
                    }
                }

                // Amount with Inline Math & = Calc button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountExpression,
                        onValueChange = { amountExpression = it },
                        label = { Text("Amount ($curr) · Supports 120+45+30") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            FinancialCalculator.evaluateMathExpression(amountExpression)?.let {
                                amountExpression = if (it % 1.0 == 0.0) it.toLong().toString() else "%.2f".format(it)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surface)
                    ) {
                        Text("= Calc", color = palette.primary, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Income Source Toggle when type == INCOME
                if (txType.equals("INCOME", true)) {
                    Text("Income Source", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("💼 Own Earnings / Freelance", "🏠 Parent / Family Pocket-Money").forEach { src ->
                            val sel = incomeSource == src
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) palette.primary.copy(alpha = 0.22f) else palette.surface)
                                    .border(1.dp, if (sel) palette.primary else palette.surfaceBorder, RoundedCornerShape(12.dp))
                                    .clickable { incomeSource = src }
                                    .padding(10.dp)
                            ) {
                                Text(src, style = MaterialTheme.typography.labelSmall, color = if (sel) palette.primary else palette.textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Account / Wallet Field + Quick Add Wallet / Add Account buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account / Wallet", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surface,
                            modifier = Modifier.clickable {
                                createAccountDefaultType = "Wallet"
                                showCreateAccountDialog = true
                            }
                        ) {
                            Text("+ Add Wallet", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surface,
                            modifier = Modifier.clickable {
                                createAccountDefaultType = "Bank"
                                showCreateAccountDialog = true
                            }
                        ) {
                            Text("+ Add Account", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.accounts.forEach { acc ->
                        val sel = selectedAccountId == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) palette.primary else palette.surface)
                                .clickable {
                                    selectedAccountId = acc.id
                                    if (transferToAccountId == acc.id) {
                                        transferToAccountId = state.accounts.firstOrNull { it.id != acc.id }?.id ?: acc.id
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(acc.name, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Destination account selector when logging a TRANSFER
                if (txType.equals("TRANSFER", true) && editingTransaction == null) {
                    val destAccounts = state.accounts.filter { it.id != selectedAccountId }
                    Text("Transfer To Account / Wallet", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                    if (destAccounts.isEmpty()) {
                        Text(
                            "Add a second account/wallet above to transfer funds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFBBF24)
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            destAccounts.forEach { acc ->
                                val sel = transferToAccountId == acc.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) palette.primary else palette.surface)
                                        .clickable { transferToAccountId = acc.id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(acc.name, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Account Allocation Selector if selected account has allocations
                if (accountAllocations.isNotEmpty()) {
                    Text("Deduct / Credit Account Allocation Bucket", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accountAllocations.forEach { alloc ->
                            val sel = selectedAllocationId == alloc.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) palette.primary else palette.surface)
                                    .clickable { selectedAllocationId = if (sel) "" else alloc.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "${alloc.category} ($curr${"%,.0f".format(alloc.allocatedAmount)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sel) palette.background else palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Neutral "Select Category" default + 17 Expanded Categories
                Text("Category (Neutral Default: Select Category)", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("Select Category") + state.categories).forEach { cat ->
                        val sel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) palette.primary else palette.surface)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Subcategory (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Method including Half-Cash + Half-UPI Split
                Text("Payment Method", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("UPI", "Cash", "Card", "Bank Transfer", "Half-Cash + Half-UPI Split").forEach { pm ->
                        val sel = paymentMethod == pm
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) palette.primary else palette.surface)
                                .clickable { paymentMethod = pm }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(pm, style = MaterialTheme.typography.labelSmall, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (paymentMethod == "UPI") {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("GPay", "PhonePe", "Paytm", "BHIM", "CRED", "Amazon Pay").forEach { app ->
                            val sel = upiApp == app
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) palette.primary.copy(alpha = 0.25f) else palette.surface)
                                    .border(1.dp, if (sel) palette.primary else palette.surfaceBorder, RoundedCornerShape(10.dp))
                                    .clickable { upiApp = app }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(app, style = MaterialTheme.typography.labelSmall, color = if (sel) palette.primary else palette.textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (paymentMethod == "Half-Cash + Half-UPI Split") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cashPortionText,
                            onValueChange = { cashPortionText = it },
                            label = { Text("Cash Portion ($curr)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = upiPortionText,
                            onValueChange = { upiPortionText = it },
                            label = { Text("UPI Portion ($curr)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Notes & #Hashtags (e.g. Coffee #cafe #weekend)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = warrantyDate,
                    onValueChange = { warrantyDate = it },
                    label = { Text("Warranty / Return Deadline Date (Optional YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        val evaluated = FinancialCalculator.evaluateMathExpression(amountExpression)
                            ?: amountExpression.toDoubleOrNull()
                            ?: 0.0
                        if (evaluated > 0.0) {
                            if (txType.equals("TRANSFER", true) && editingTransaction == null) {
                                if (transferToAccountId == selectedAccountId) {
                                    viewModel.postStatus("Select a different destination account for transfer")
                                    return@Button
                                }
                                isSubmitting = true
                                viewModel.transferFunds(
                                    fromId = selectedAccountId,
                                    toId = transferToAccountId,
                                    amount = evaluated,
                                    note = noteText.trim().ifBlank { "Account Transfer" }
                                )
                                onDismiss()
                                return@Button
                            }
                            isSubmitting = true
                            val finalCat = if (selectedCategory == "Select Category") "Other" else selectedCategory
                            viewModel.saveTransaction(
                                editingId = editingTransaction?.id,
                                accountId = selectedAccountId,
                                type = txType,
                                amount = evaluated,
                                category = finalCat,
                                subcategory = subcategory.trim(),
                                paymentMethod = paymentMethod,
                                upiApp = if (paymentMethod == "UPI") upiApp else "",
                                note = noteText.trim(),
                                warrantyDate = warrantyDate.trim(),
                                allocationId = selectedAllocationId,
                                cashPortion = cashPortionText.toDoubleOrNull() ?: 0.0,
                                upiPortion = upiPortionText.toDoubleOrNull() ?: 0.0
                            )
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(
                        if (editingTransaction == null) "Save Transaction" else "Update Transaction",
                        color = palette.background,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    if (showCreateAccountDialog) {
        AccountEditDialog(
            initialAccount = null,
            palette = palette,
            onDismiss = { showCreateAccountDialog = false },
            onSave = { name, type, bal, limit, stmt, due ->
                viewModel.saveAccount(null, name, type.ifBlank { createAccountDefaultType }, bal, limit, stmt, due)
                showCreateAccountDialog = false
            }
        )
    }
}

@Composable
fun ExpenseReceiptModal(
    transaction: TransactionEntity,
    palette: ThemePalette,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onRepeat: (TransactionEntity) -> Unit,
    onPrint: ((TransactionEntity) -> Unit)? = null
) {
    val dateFormatted = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left Red Delete Icon (#FB7185) ONLY on Expense Receipt Modal
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFB7185).copy(alpha = 0.22f),
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.dp, Color(0xFFFB7185), CircleShape)
                        .clickable { onDelete(transaction) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
                Text("🧾 Digital Receipt", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                // Top-Right Edit Pencil
                Surface(
                    shape = CircleShape,
                    color = palette.surface,
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.dp, palette.primary, CircleShape)
                        .clickable { onEdit(transaction) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✏️", fontSize = 15.sp)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "$currencySymbol${"%,.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (transaction.type.equals("INCOME", true)) palette.primary else Color(0xFFFB7185)
                )
                Text("Type: ${transaction.type}", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                Text("Category: ${transaction.category}${if (transaction.subcategory.isNotBlank()) " · ${transaction.subcategory}" else ""}", color = palette.textPrimary)
                Text("Payment: ${transaction.paymentMethod}${if (transaction.upiApp.isNotBlank()) " (${transaction.upiApp})" else ""}", color = palette.textSecondary)
                Text("Date: $dateFormatted", color = palette.textSecondary)
                if (transaction.note.isNotBlank()) {
                    Text("Note: ${transaction.note}", color = palette.textPrimary)
                }
                if (transaction.warrantyDate.isNotBlank()) {
                    Text("🛡️ Warranty / Return Deadline: ${transaction.warrantyDate}", color = palette.primary, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(6.dp))
                // 1-Tap Receipt Repeat (↻)
                Button(
                    onClick = { onRepeat(transaction) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("↻ Repeat This Transaction", color = palette.background, fontWeight = FontWeight.ExtraBold)
                }
                if (onPrint != null) {
                    OutlinedButton(
                        onClick = { onPrint(transaction) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🖨️ Print Receipt PDF", color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = palette.textSecondary)
            }
        }
    )
}
