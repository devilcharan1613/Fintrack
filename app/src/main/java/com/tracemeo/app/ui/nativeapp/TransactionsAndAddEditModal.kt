package com.tracmio.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tracmio.app.data.HashtagSummary
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.NativeLedgerCalculator
import com.tracmio.app.data.UpiSmsDraft
import com.tracmio.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsListScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onEditTransaction: (TransactionEntity) -> Unit,
    onShowReceipt: (TransactionEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("ALL") }
    var showFilterPanel by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var filterAccountId by remember { mutableStateOf<Long?>(null) }
    var quickRange by remember { mutableStateOf("ALL") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var selectedHashtag by remember { mutableStateOf<HashtagSummary?>(null) }

    val hashtagSummaries = remember(state.transactions) {
        NativeLedgerCalculator.summarizeHashtags(state.transactions)
    }

    val filtered = remember(state.transactions, search, typeFilter, filterAccountId, quickRange, fromDate, toDate) {
        val nowCal = Calendar.getInstance()
        val todayStart = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterdayStart = todayStart - 86_400_000L
        val weekStart = (nowCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        val last7DaysStart = todayStart - 6L * 86_400_000L
        val monthStart = (nowCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        val lastMonthStart = (nowCal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val fromMs = runCatching { if (fromDate.length == 10) dateFmt.parse(fromDate)?.time else null }.getOrNull()
        val toMs = runCatching { if (toDate.length == 10) dateFmt.parse(toDate)?.time?.plus(86_399_999L) else null }.getOrNull()

        state.transactions.filter { tx ->
            val matchType = when (typeFilter) {
                "INCOME" -> tx.type.equals("INCOME", true) || tx.type.equals("BORROW", true)
                "EXPENSE" -> tx.type.equals("EXPENSE", true) || tx.type.equals("LEND", true)
                "TRANSFER" -> tx.type.startsWith("TRANSFER", true)
                else -> true
            }
            val matchAcc = filterAccountId == null || tx.accountId == filterAccountId
            val matchRange = when (quickRange) {
                "TODAY" -> tx.timestamp >= todayStart
                "YESTERDAY" -> tx.timestamp in yesterdayStart until todayStart
                "THIS_WEEK" -> tx.timestamp >= weekStart
                "LAST_7D" -> tx.timestamp >= last7DaysStart
                "MONTH" -> tx.timestamp >= monthStart
                "LAST_MONTH" -> tx.timestamp in lastMonthStart until monthStart
                else -> true
            }
            val matchCustomDate = (fromMs == null || tx.timestamp >= fromMs) && (toMs == null || tx.timestamp <= toMs)
            val matchSearch = search.isBlank() ||
                tx.category.contains(search, true) ||
                tx.subcategory.contains(search, true) ||
                tx.note.contains(search, true) ||
                tx.upiApp.contains(search, true)
            matchType && matchAcc && matchRange && matchCustomDate && matchSearch
        }
    }

    val groupedByDay = remember(filtered) {
        filtered.groupBy { NativeLedgerCalculator.dayKey(it.timestamp) }.toList()
    }
    val dayDisplayFmt = remember { SimpleDateFormat("dd MMM yyyy (EEE)", Locale.getDefault()) }
    val dayParseFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top-Left Filter Button + Export Menu + Entry Count
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showFilterPanel = !showFilterPanel },
                        modifier = Modifier.testTag("tx_top_left_filter_btn")
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter transactions", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (showFilterPanel) "Hide Filter" else "Filter")
                    }
                    Box {
                        OutlinedButton(
                            onClick = { showExportMenu = true },
                            modifier = Modifier.testTag("tx_export_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export ledger", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export")
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("📊 Excel Workbook (.xlsx)") },
                                onClick = { showExportMenu = false; hostCallbacks.exportWorkbookXlsx("Tracmio_Ledger_16.13.xlsx") }
                            )
                            DropdownMenuItem(
                                text = { Text("📄 CSV Ledger") },
                                onClick = { showExportMenu = false; hostCallbacks.exportCsv("tracmio_ledger.csv") }
                            )
                            DropdownMenuItem(
                                text = { Text("🖨️ PDF Statement") },
                                onClick = { showExportMenu = false; hostCallbacks.printStatement(state.accounts, filtered) }
                            )
                        }
                    }
                }
                Text("${filtered.size} entries", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
        }

        if (showFilterPanel) {
            item {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Account", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = filterAccountId == null, onClick = { filterAccountId = null }, label = { Text("All Accounts") })
                            state.accounts.filter { !it.isArchived }.forEach { acc ->
                                FilterChip(selected = filterAccountId == acc.id, onClick = { filterAccountId = acc.id }, label = { Text(acc.name) })
                            }
                        }
                        Text("Quick Date Range", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "ALL" to "All dates",
                                "TODAY" to "Today",
                                "YESTERDAY" to "Yesterday",
                                "THIS_WEEK" to "This week",
                                "LAST_7D" to "Last 7 days",
                                "MONTH" to "This month",
                                "LAST_MONTH" to "Last month",
                                "CUSTOM" to "Custom date range"
                            ).forEach { (k, lbl) ->
                                FilterChip(selected = quickRange == k, onClick = { quickRange = k }, label = { Text(lbl) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = fromDate, onValueChange = { fromDate = it; if (it.isNotBlank()) quickRange = "CUSTOM" }, label = { Text("From date (YYYY-MM-DD)") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = toDate, onValueChange = { toDate = it; if (it.isNotBlank()) quickRange = "CUSTOM" }, label = { Text("To date (YYYY-MM-DD)") }, modifier = Modifier.weight(1f))
                        }
                        TextButton(onClick = {
                            filterAccountId = null; quickRange = "ALL"; fromDate = ""; toDate = ""; typeFilter = "ALL"; search = ""
                        }) { Text("Clear filters", color = ThemePresets.AURORA_EXPENSE_ROSE) }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search notes, #hashtags, UPI, categories...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("tx_search_input")
            )
        }

        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "INCOME", "EXPENSE", "TRANSFER").forEach { t ->
                    FilterChip(selected = typeFilter == t, onClick = { typeFilter = t }, label = { Text(t) })
                }
            }
        }

        // #Hashtag Expense Totals Strip
        if (hashtagSummaries.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("#Hashtag Totals", style = MaterialTheme.typography.labelMedium, color = palette.secondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hashtagSummaries, key = { it.tag }) { h ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.secondary.copy(alpha = 0.16f),
                                modifier = Modifier.clickable { selectedHashtag = h }.testTag("hashtag_chip_${h.tag}")
                            ) {
                                Text(
                                    text = "${h.tag} (${h.count}) · ${displayAmount(h.totalExpense, settings)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Date-Grouped Transaction Ledger
        groupedByDay.forEach { (dayStr, dayTxns) ->
            val dayIn = dayTxns.filter { it.type in setOf("INCOME", "BORROW") }.sumOf { it.amount }
            val dayOut = dayTxns.filter { it.type in setOf("EXPENSE", "LEND") }.sumOf { it.amount }
            val niceDate = runCatching { dayParseFmt.parse(dayStr)?.let { dayDisplayFmt.format(it) } }.getOrNull() ?: dayStr

            item(key = "header_$dayStr") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(niceDate, style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                    Text(
                        "In ${displayAmount(dayIn, settings)} · Out ${displayAmount(dayOut, settings)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }

            items(dayTxns, key = { it.id }) { tx ->
                val accName = state.accounts.firstOrNull { it.id == tx.accountId }?.name ?: "Wallet"
                val isExpense = tx.type.equals("EXPENSE", true) || tx.type.equals("LEND", true) || tx.type.equals("TRANSFER_OUT", true)
                val warrantyBadge = remember(tx.warrantyDate) {
                    if (tx.warrantyDate.length != 10) null
                    else runCatching {
                        val expMs = dayParseFmt.parse(tx.warrantyDate)?.time ?: return@runCatching null
                        val daysLeft = ((expMs - System.currentTimeMillis()) / 86_400_000L).toInt()
                        if (daysLeft < 0) "Expired" else "${daysLeft}d left"
                    }.getOrNull()
                }
                GlassCard(
                    settings = settings,
                    onClick = { onShowReceipt(tx) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            GlassBadge3D(
                                emoji = if (isExpense) "💸" else "💰",
                                accent = if (isExpense) ThemePresets.AURORA_EXPENSE_ROSE else palette.primary,
                                sizeDp = 38.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        tx.category + if (tx.subcategory.isNotBlank()) " · ${tx.subcategory}" else "",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (warrantyBadge != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = palette.tertiary.copy(alpha = 0.22f),
                                            modifier = Modifier.clickable { viewModel.clearTransactionWarranty(tx) }
                                        ) {
                                            Text(
                                                "🛡️ $warrantyBadge ×",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.tertiary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (tx.attachmentUri.isNotBlank()) {
                                        Text("📎", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(
                                    listOfNotNull(accName, tx.note.takeIf { it.isNotBlank() }, tx.upiApp.takeIf { it.isNotBlank() }).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(timeFmt.format(Date(tx.timestamp)), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onEditTransaction(tx) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                (if (isExpense) "-" else "+") + displayAmount(tx.amount, settings),
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

    selectedHashtag?.let { h ->
        val matching = state.transactions.filter { h.tag in NativeLedgerCalculator.extractHashtags(it.note) }
        AlertDialog(
            onDismissRequest = { selectedHashtag = null },
            containerColor = palette.surface,
            title = { Text("${h.tag} Breakdown (${h.count})", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total Expense: ${displayAmount(h.totalExpense, settings)}", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
                    Text("Total Income: ${displayAmount(h.totalIncome, settings)}", color = palette.primary, fontWeight = FontWeight.Bold)
                    matching.take(8).forEach { t ->
                        Text("• ${t.category}: ${displayAmount(t.amount, settings)} (${t.note})", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { search = h.tag; selectedHashtag = null }) { Text("Filter List") } },
            dismissButton = { TextButton(onClick = { selectedHashtag = null }) { Text("Close") } }
        )
    }
}

@Composable
fun VisualReceiptDialog(
    transaction: TransactionEntity,
    accountName: String,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onRepeat: (TransactionEntity) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onPrintPdf: (TransactionEntity, String) -> Unit
) {
    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }
    val isExpense = transaction.type.equals("EXPENSE", true) || transaction.type.equals("LEND", true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onDelete(transaction); onDismiss() },
                    modifier = Modifier.size(36.dp).testTag("receipt_red_delete_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete receipt", tint = ThemePresets.AURORA_EXPENSE_ROSE)
                }
                Text("🧾 Tracmio Receipt", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onEdit(transaction); onDismiss() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit receipt", tint = palette.primary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    (if (isExpense) "-" else "+") + displayAmount(transaction.amount, settings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isExpense) ThemePresets.AURORA_EXPENSE_ROSE else palette.primary
                )
                Text("Type: ${transaction.type}", color = palette.textSecondary)
                Text("Category: ${transaction.category}" + if (transaction.subcategory.isNotBlank()) " / ${transaction.subcategory}" else "", color = palette.textPrimary)
                Text("Account: $accountName", color = palette.textPrimary)
                Text("Method: ${transaction.paymentMethod}" + if (transaction.upiApp.isNotBlank()) " (${transaction.upiApp})" else "", color = palette.textSecondary)
                Text("Date: $dateStr", color = palette.textSecondary)
                if (transaction.warrantyDate.isNotBlank()) {
                    Text("Warranty / Deadline: ${transaction.warrantyDate}", color = palette.tertiary, fontWeight = FontWeight.SemiBold)
                }
                if (transaction.attachmentUri.isNotBlank()) {
                    Text("Attachment: 📎 ${transaction.attachmentUri.substringAfterLast('/')}", color = palette.secondary)
                }
                if (transaction.note.isNotBlank()) {
                    Text("Note: ${transaction.note}", color = palette.textPrimary)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onPrintPdf(transaction, accountName) },
                    modifier = Modifier.testTag("receipt_pdf_btn")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF")
                }
                Button(
                    onClick = { onRepeat(transaction); onDismiss() },
                    modifier = Modifier.testTag("receipt_repeat_btn")
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("↻ Repeat This Transaction")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    initialType: String,
    initialAccountId: Long? = null,
    initialSmsDraft: UpiSmsDraft? = null,
    editing: TransactionEntity?,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeAccounts = remember(state.accounts) { state.accounts.filter { !it.isArchived } }
    var type by remember { mutableStateOf(editing?.type ?: initialSmsDraft?.type ?: initialType.uppercase()) }
    var amountText by remember {
        mutableStateOf(
            editing?.let { MoneyPaise.formatInput(MoneyPaise.toPaise(it.amount)) }
                ?: initialSmsDraft?.let { MoneyPaise.formatInput(MoneyPaise.toPaise(it.amount)) }
                ?: ""
        )
    }
    var category by remember { mutableStateOf(editing?.category ?: "") } // Neutral "Select Category" default!
    var subcategory by remember { mutableStateOf(editing?.subcategory ?: "") }
    var accountId by remember {
        mutableLongStateOf(editing?.accountId ?: initialAccountId ?: activeAccounts.firstOrNull()?.id ?: 0L)
    }
    var transferToId by remember {
        mutableLongStateOf(activeAccounts.firstOrNull { it.id != accountId }?.id ?: 0L)
    }
    var allocationId by remember { mutableStateOf(editing?.allocationId ?: "") }
    var selectedTrackerUid by remember { mutableStateOf("") }
    var isTrackerRefund by remember { mutableStateOf(false) }
    var attachmentUri by remember { mutableStateOf(editing?.attachmentUri ?: "") }
    var note by remember { mutableStateOf(editing?.note ?: initialSmsDraft?.note ?: "") }
    var paymentMethod by remember { mutableStateOf(editing?.paymentMethod ?: initialSmsDraft?.paymentMethod ?: "UPI") }
    var splitCashAmountText by remember { mutableStateOf("") }
    var splitUpiAmountText by remember { mutableStateOf("") }
    var upiApp by remember { mutableStateOf(editing?.upiApp ?: initialSmsDraft?.upiApp ?: "") }
    var warrantyDate by remember { mutableStateOf(editing?.warrantyDate ?: "") }
    var personId by remember { mutableStateOf(editing?.personId) }

    var showAccountMenu by remember { mutableStateOf(false) }
    var showToAccountMenu by remember { mutableStateOf(false) }
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var showAddCatDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var showAddSubDialog by remember { mutableStateOf(false) }
    var newSubName by remember { mutableStateOf("") }
    var showAddUpiDialog by remember { mutableStateOf(false) }
    var newUpiName by remember { mutableStateOf("") }
    var showNewTrackerDialog by remember { mutableStateOf(false) }
    var newTrackerName by remember { mutableStateOf("") }
    var hadAmountFocus by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val selectedAcc = activeAccounts.firstOrNull { it.id == accountId }
    val accountAllocs = remember(selectedAcc?.allocationsJson) {
        selectedAcc?.let { NativeLedgerCalculator.parseAllocations(it.allocationsJson) }.orEmpty()
    }
    val allUpiApps = remember(settings.customUpiApps) {
        (listOf("GPay", "PhonePe", "Paytm", "BHIM") + settings.customUpiApps + listOf("Other UPI")).distinct()
    }
    val availableCats = remember(state.categories, type) {
        val catType = if (type in setOf("INCOME", "BORROW")) "INCOME" else "EXPENSE"
        val topLevel = state.categories
            .filter { it.type.equals(catType, true) && it.parentId == null }
            .map { it.name }
        val defaultExpanded = if (catType == "INCOME") {
            listOf("Family & Income", "Salary", "Freelance", "Business & Studio", "Investment", "Gifts & Celebrations", "Other")
        } else {
            listOf(
                "Food & Chai",
                "Khata & Kirana",
                "Transport & Travel",
                "SIM, WiFi & Bills",
                "College & Exams",
                "Family & Income",
                "Shopping & Personal",
                "Health & Medical",
                "Entertainment",
                "Investment",
                "Fuel & Commute",
                "Rent & Housing",
                "Recharge & WiFi",
                "Gifts & Celebrations",
                "Business & Studio",
                "Pets & Care",
                "Other"
            )
        }
        (defaultExpanded + topLevel).distinct()
    }
    val availableSubcats = remember(category, state.categories) {
        NativeLedgerCalculator.subcategoriesFor(category, state.categories)
    }
    val hasMathOperators = remember(amountText) {
        amountText.drop(1).any { it in setOf('+', '-', '*', '/', '×', '÷', '(') }
    }
    val liveMathResult = remember(amountText, hasMathOperators) {
        if (hasMathOperators) evalSimpleMath(amountText) else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface
    ) {
        // Stable scroll container with imePadding() prevents scroll jump when typing Note
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editing != null) "Edit Transaction" else "New Transaction",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary
            )

            // 5 Transaction Types including TRANSFER
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                val allowedTypes = if (editing == null) listOf("EXPENSE", "INCOME", "TRANSFER", "BORROW", "LEND")
                else listOf("EXPENSE", "INCOME", "BORROW", "LEND")
                allowedTypes.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t; if (editing == null) category = if (t == "TRANSFER") "Transfer" else "" },
                        label = { Text(t) }
                    )
                }
            }

            // Feature #10: Parent / Family Pocket-Money vs. Own Earnings Split Tag Toggle on Income
            if (type == "INCOME") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Income Source Tag (Family Allowance vs. Own Earnings)",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isFamilyTag = subcategory.contains("Allowance", true) || category.contains("Allowance", true)
                        val isOwnTag = subcategory.contains("Own Earnings", true) || subcategory.contains("Freelance", true)
                        FilterChip(
                            selected = isFamilyTag,
                            onClick = {
                                category = "Family & Income"
                                subcategory = "Family Allowance"
                            },
                            label = { Text("👨‍👩‍👧 Family / Allowance") },
                            modifier = Modifier.testTag("income_tag_family")
                        )
                        FilterChip(
                            selected = isOwnTag,
                            onClick = {
                                category = "Family & Income"
                                subcategory = "Own Earnings"
                            },
                            label = { Text("💼 Own Earnings / Freelance") },
                            modifier = Modifier.testTag("income_tag_own_earnings")
                        )
                    }
                }
            }

            // Feature #1: Inline Math in the Amount Box (auto-evaluates on blur or '=' button)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (${settings.currencySymbol}) or math e.g. 120+45+30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    hadAmountFocus = true
                                } else if (hadAmountFocus && amountText.drop(1).any { it in setOf('+', '-', '*', '/', '×', '÷') }) {
                                    evalSimpleMath(amountText)?.let { res ->
                                        amountText = MoneyPaise.formatInput(MoneyPaise.toPaise(res))
                                    }
                                }
                            }
                            .testTag("tx_amount_input")
                    )
                    OutlinedButton(
                        onClick = {
                            evalSimpleMath(amountText)?.let { res ->
                                amountText = MoneyPaise.formatInput(MoneyPaise.toPaise(res))
                            }
                        },
                        modifier = Modifier.testTag("tx_inline_math_equals_btn")
                    ) { Text("=") }
                }
                if (liveMathResult != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.primary.copy(alpha = 0.16f),
                        modifier = Modifier
                            .clickable {
                                amountText = MoneyPaise.formatInput(MoneyPaise.toPaise(liveMathResult))
                            }
                            .testTag("tx_inline_math_preview")
                    ) {
                        Text(
                            text = "🧮 Inline Math Preview: = ${displayAmount(liveMathResult, settings)} (tap or press '=' to apply)",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Account / Wallet Field with both "+ Add Wallet" and "+ Add Account" buttons & dropdown items
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account / Wallet", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAddWalletDialog = true },
                            modifier = Modifier.height(34.dp).testTag("tx_add_wallet_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("+ Add Wallet", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { showAddWalletDialog = true },
                            modifier = Modifier.height(34.dp).testTag("tx_add_account_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("+ Add Account", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedAccName = selectedAcc?.name ?: "Select Account / Wallet"
                    OutlinedButton(onClick = { showAccountMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (type == "TRANSFER") "From: $selectedAccName" else "Account: $selectedAccName", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = showAccountMenu, onDismissRequest = { showAccountMenu = false }) {
                        activeAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.accountType})") },
                                onClick = { accountId = acc.id; allocationId = ""; showAccountMenu = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("➕ + Add New Wallet...", color = palette.primary, fontWeight = FontWeight.SemiBold) },
                            onClick = { showAccountMenu = false; showAddWalletDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("🏦 + Add New Bank / Account...", color = palette.secondary, fontWeight = FontWeight.SemiBold) },
                            onClick = { showAccountMenu = false; showAddWalletDialog = true }
                        )
                    }
                }
            }

            if (type == "TRANSFER") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val toName = activeAccounts.firstOrNull { it.id == transferToId }?.name ?: "Select Destination Account"
                    OutlinedButton(onClick = { showToAccountMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("To Account: $toName", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = showToAccountMenu, onDismissRequest = { showToAccountMenu = false }) {
                        activeAccounts.filter { it.id != accountId }.forEach { acc ->
                            DropdownMenuItem(text = { Text("${acc.name} (${acc.accountType})") }, onClick = { transferToId = acc.id; showToAccountMenu = false })
                        }
                    }
                }
            }

            // Account Allocation Pocket Selector
            if (accountAllocs.isNotEmpty() && type != "TRANSFER") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Account Allocation Pocket", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = allocationId.isBlank(), onClick = { allocationId = "" }, label = { Text("Unallocated") })
                        accountAllocs.forEach { al ->
                            FilterChip(selected = allocationId == al.id, onClick = { allocationId = al.id }, label = { Text("📂 ${al.name}") })
                        }
                    }
                }
            }

            // Neutral Select Category + Subcategory Pills + "+ Category" & "+ Sub"
            if (type != "TRANSFER") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (category.isBlank()) "Select Category (Required)" else "Category: $category" + if (subcategory.isNotBlank()) " · $subcategory" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (category.isBlank()) palette.tertiary else palette.primary
                        )
                        Row {
                            TextButton(onClick = { showAddCatDialog = true }) { Text("+ Category") }
                            TextButton(onClick = { showAddSubDialog = true }) { Text("+ Sub") }
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableCats.forEach { catName ->
                            FilterChip(
                                selected = category == catName,
                                onClick = {
                                    category = catName
                                    if (subcategory !in NativeLedgerCalculator.subcategoriesFor(catName, state.categories)) {
                                        subcategory = ""
                                    }
                                },
                                label = { Text(catName) }
                            )
                        }
                    }
                    if (availableSubcats.isNotEmpty()) {
                        Text("Subcategories for $category:", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableSubcats.forEach { subName ->
                                FilterChip(
                                    selected = subcategory == subName,
                                    onClick = { subcategory = if (subcategory == subName) "" else subName },
                                    label = { Text(subName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            // Payment Method including Feature #5: Half-Cash + Half-UPI Split Payment
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val methods = if (editing == null && type == "EXPENSE") {
                    listOf("UPI", "Cash", "Cash + UPI Split", "Card", "Bank Transfer")
                } else {
                    listOf("UPI", "Cash", "Card", "Bank Transfer")
                }
                methods.forEach { pm ->
                    FilterChip(
                        selected = paymentMethod == pm,
                        onClick = {
                            paymentMethod = pm
                            if (pm == "Cash + UPI Split") {
                                val totalAmt = evalSimpleMath(amountText) ?: 0.0
                                if (totalAmt > 0 && splitCashAmountText.isBlank() && splitUpiAmountText.isBlank()) {
                                    val half = totalAmt / 2.0
                                    splitCashAmountText = MoneyPaise.formatInput(MoneyPaise.toPaise(half))
                                    splitUpiAmountText = MoneyPaise.formatInput(MoneyPaise.toPaise(totalAmt - half))
                                }
                            }
                        },
                        label = { Text(pm) },
                        modifier = Modifier.testTag("pm_chip_${pm.lowercase().replace(" ", "_")}")
                    )
                }
            }

            if (paymentMethod == "Cash + UPI Split" && editing == null) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "💵 + 📱 Half-Cash + Half-UPI Split Payment",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Deducts the Cash portion from your Cash Wallet and the UPI portion from your Bank/UPI account in one submission.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = splitCashAmountText,
                                onValueChange = {
                                    splitCashAmountText = it
                                    val c = evalSimpleMath(it) ?: 0.0
                                    val u = evalSimpleMath(splitUpiAmountText) ?: 0.0
                                    if (c + u > 0) amountText = MoneyPaise.formatInput(MoneyPaise.toPaise(c + u))
                                },
                                label = { Text("💵 Cash (${settings.currencySymbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("split_cash_input")
                            )
                            OutlinedTextField(
                                value = splitUpiAmountText,
                                onValueChange = {
                                    splitUpiAmountText = it
                                    val c = evalSimpleMath(splitCashAmountText) ?: 0.0
                                    val u = evalSimpleMath(it) ?: 0.0
                                    if (c + u > 0) amountText = MoneyPaise.formatInput(MoneyPaise.toPaise(c + u))
                                },
                                label = { Text("📱 UPI (${settings.currencySymbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("split_upi_input")
                            )
                        }
                    }
                }
            }

            if (paymentMethod == "UPI" || paymentMethod == "Cash + UPI Split") {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    allUpiApps.forEach { app ->
                        FilterChip(selected = upiApp == app, onClick = { upiApp = if (upiApp == app) "" else app }, label = { Text(app) })
                    }
                    TextButton(onClick = { showAddUpiDialog = true }) { Text("+ UPI App") }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note & #hashtags (e.g. #trip #coffee)") },
                modifier = Modifier.fillMaxWidth().testTag("tx_note_input")
            )

            if (type != "TRANSFER") {
                OutlinedTextField(
                    value = warrantyDate,
                    onValueChange = { warrantyDate = it },
                    label = { Text("Warranty / Deadline (YYYY-MM-DD, optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Custom Tracker Folder Linking
            if (type != "TRANSFER") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tracker Folder (Optional)", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                        TextButton(onClick = { showNewTrackerDialog = true }) { Text("+ New Tracker") }
                    }
                    if (state.customTrackers.isNotEmpty()) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = selectedTrackerUid.isBlank(), onClick = { selectedTrackerUid = "" }, label = { Text("None") })
                            state.customTrackers.forEach { tr ->
                                FilterChip(selected = selectedTrackerUid == tr.stableUid, onClick = { selectedTrackerUid = tr.stableUid }, label = { Text("${tr.icon} ${tr.name}") })
                            }
                        }
                        if (selectedTrackerUid.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isTrackerRefund, onCheckedChange = { isTrackerRefund = it })
                                Text("Refund / reduces tracker net spend", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                            }
                        }
                    }
                }
            }

            // Receipt Attachment (Camera or File/PDF)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    hostCallbacks.captureReceiptPhoto { _, _, localPath, _ ->
                        attachmentUri = localPath
                        viewModel.showStatus("Receipt photo attached")
                    }
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Camera")
                }
                OutlinedButton(onClick = {
                    hostCallbacks.pickTrackerAttachment { _, _, localPath, _ ->
                        attachmentUri = localPath
                        viewModel.showStatus("Receipt file attached")
                    }
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Photo/PDF")
                }
            }
            if (attachmentUri.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(palette.surfaceElevated).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📎 ${attachmentUri.substringAfterLast('/')}", style = MaterialTheme.typography.bodySmall, color = palette.primary)
                    TextButton(onClick = { attachmentUri = "" }) { Text("Remove", color = Color(0xFFEF4444)) }
                }
            }

            if (state.persons.isNotEmpty() && type in setOf("BORROW", "LEND", "EXPENSE")) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = personId == null, onClick = { personId = null }, label = { Text("No Person") })
                    state.persons.forEach { p ->
                        FilterChip(selected = personId == p.id, onClick = { personId = p.id }, label = { Text(p.name) })
                    }
                }
            }

            Button(
                onClick = {
                    if (paymentMethod == "Cash + UPI Split" && editing == null) {
                        val cashPortion = evalSimpleMath(splitCashAmountText) ?: 0.0
                        val upiPortion = evalSimpleMath(splitUpiAmountText) ?: 0.0
                        if (cashPortion <= 0.0 && upiPortion <= 0.0) {
                            viewModel.showStatus("Enter Cash and/or UPI split portions")
                            return@Button
                        }
                        if (category.isBlank()) {
                            viewModel.showStatus("Please select a category")
                            return@Button
                        }
                        isSubmitting = true
                        viewModel.saveSplitCashUpiTransaction(
                            cashAmount = cashPortion,
                            upiAmount = upiPortion,
                            category = category,
                            subcategory = subcategory,
                            note = note,
                            upiApp = upiApp.ifBlank { "GPay" },
                            warrantyDate = warrantyDate,
                            attachmentUri = attachmentUri,
                            preferredUpiAccountId = accountId
                        )
                        onDismiss()
                        return@Button
                    }

                    val amt = evalSimpleMath(amountText)
                    if (amt == null || amt <= 0.0) {
                        viewModel.showStatus("Enter a positive amount")
                        return@Button
                    }
                    if (type != "TRANSFER" && category.isBlank()) {
                        viewModel.showStatus("Please select a category")
                        return@Button
                    }
                    if (accountId <= 0L) {
                        viewModel.showStatus("Please select or add an account")
                        return@Button
                    }
                    if (type == "TRANSFER" && (transferToId <= 0L || transferToId == accountId)) {
                        viewModel.showStatus("Choose a different destination account")
                        return@Button
                    }
                    isSubmitting = true
                    viewModel.saveOrUpdateTransaction(
                        id = editing?.id,
                        accountId = accountId,
                        type = type,
                        amount = amt,
                        category = if (type == "TRANSFER") "Transfer" else category,
                        subcategory = subcategory,
                        note = note,
                        timestamp = editing?.timestamp ?: System.currentTimeMillis(),
                        paymentMethod = paymentMethod,
                        upiApp = upiApp,
                        warrantyDate = warrantyDate,
                        personId = personId,
                        transferToAccountId = if (type == "TRANSFER") transferToId else null,
                        allocationId = allocationId,
                        attachmentUri = attachmentUri,
                        trackerUid = selectedTrackerUid,
                        isTrackerRefund = isTrackerRefund
                    )
                    // Explicitly NO automatic receipt pop-up after saving!
                    onDismiss()
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_tx_btn")
            ) {
                Text(if (editing != null) "Update Transaction" else "Save Transaction", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddWalletDialog) {
        AddAccountDialog(
            palette = palette,
            onDismiss = { showAddWalletDialog = false },
            onSave = { name, accType, bal, inst, num, ccLim, billDay, dueDay ->
                viewModel.addAccount(name, accType, bal, "INR", inst, num, "", ccLim, billDay, dueDay)
                showAddWalletDialog = false
            }
        )
    }

    if (showAddCatDialog) {
        AlertDialog(
            onDismissRequest = { showAddCatDialog = false },
            containerColor = palette.surface,
            title = { Text("+ New Category", color = palette.textPrimary) },
            text = { OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("Category Name") }) },
            confirmButton = {
                Button(onClick = {
                    if (newCatName.isNotBlank()) {
                        val c = newCatName.trim()
                        viewModel.addCategory(c, if (type in setOf("INCOME", "BORROW")) "INCOME" else "EXPENSE")
                        category = c
                        newCatName = ""; showAddCatDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddCatDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddSubDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubDialog = false },
            containerColor = palette.surface,
            title = { Text("+ Subcategory", color = palette.textPrimary) },
            text = { OutlinedTextField(value = newSubName, onValueChange = { newSubName = it }, label = { Text("Subcategory Label") }) },
            confirmButton = {
                Button(onClick = {
                    subcategory = newSubName.trim()
                    newSubName = ""; showAddSubDialog = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showAddSubDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddUpiDialog) {
        AlertDialog(
            onDismissRequest = { showAddUpiDialog = false },
            containerColor = palette.surface,
            title = { Text("+ Custom UPI App", color = palette.textPrimary) },
            text = { OutlinedTextField(value = newUpiName, onValueChange = { newUpiName = it }, label = { Text("App Name (e.g. Cred UPI)") }) },
            confirmButton = {
                Button(onClick = {
                    if (newUpiName.isNotBlank()) {
                        val clean = newUpiName.trim()
                        viewModel.addCustomUpiApp(clean)
                        upiApp = clean
                        newUpiName = ""; showAddUpiDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddUpiDialog = false }) { Text("Cancel") } }
        )
    }

    if (showNewTrackerDialog) {
        AlertDialog(
            onDismissRequest = { showNewTrackerDialog = false },
            containerColor = palette.surface,
            title = { Text("+ New Tracker Folder", color = palette.textPrimary) },
            text = { OutlinedTextField(value = newTrackerName, onValueChange = { newTrackerName = it }, label = { Text("Tracker Name (e.g. Ladakh Trip)") }) },
            confirmButton = {
                Button(onClick = {
                    if (newTrackerName.isNotBlank()) {
                        viewModel.createCustomTracker(newTrackerName.trim(), "Trip", 0.0, "", "")
                        newTrackerName = ""; showNewTrackerDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewTrackerDialog = false }) { Text("Cancel") } }
        )
    }
}
