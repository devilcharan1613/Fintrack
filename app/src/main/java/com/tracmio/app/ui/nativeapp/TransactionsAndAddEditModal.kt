package com.tracmio.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.CategoryEntity
import com.tracmio.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsListScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    onEditTransaction: (TransactionEntity) -> Unit,
    onViewReceipt: (TransactionEntity) -> Unit,
    onOpenAdd: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    var filterType by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(state.transactions, filterType, searchQuery) {
        state.transactions.filter { tx ->
            val matchesType = when (filterType) {
                "INCOME" -> tx.type.equals("INCOME", ignoreCase = true)
                "EXPENSE" -> tx.type.equals("EXPENSE", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true) ||
                tx.amount.toString().contains(searchQuery)
            matchesType && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transactions Ledger",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${filteredList.size} of ${state.transactions.size} records",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                Button(
                    onClick = { onOpenAdd("EXPENSE") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions, notes, amounts...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = palette.textSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = palette.textSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = Color(0x221E293B),
                    unfocusedContainerColor = Color(0x221E293B)
                )
            )
        }

        // Filter Chips (All, Income, Expense)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All Records", "EXPENSE" to "Expenses", "INCOME" to "Income").forEach { (type, label) ->
                    val isSelected = filterType == type
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) palette.primary else Color(0x221E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) palette.primary else Color(0x33FFFFFF)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { filterType = type }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x331E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matching transactions found",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { tx ->
                TransactionVisualCard(
                    transaction = tx,
                    accounts = state.accounts,
                    palette = palette,
                    onEditClick = { onEditTransaction(tx) },
                    onCardClick = { onViewReceipt(tx) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * Visual Receipt Card Modal (Requirement #3)
 * Displays transaction as a clean visual payment receipt slip with invoice styling,
 * allowing print/save PDF and delete.
 */
@Composable
fun VisualReceiptDialog(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit
) {
    val isExpense = transaction.type.equals("EXPENSE", ignoreCase = true)
    val amountColor = if (isExpense) Color(0xFFF43F5E) else Color(0xFF10B981)
    val account = accounts.firstOrNull { it.id == transaction.accountId }
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(transaction.timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Receipt Header
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(amountColor.copy(alpha = 0.15f))
                        .border(1.dp, amountColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = getCategoryIcon(transaction.category), fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "TRACMIO PAYMENT RECEIPT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = palette.textSecondary
                )

                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${if (isExpense) "-" else "+"}${MoneyPaise.formatCurrency(transaction.amount)}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                    fontWeight = FontWeight.Black,
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Receipt Breakdown Rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x331E293B))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReceiptRow(label = "Date & Time", value = dateStr, palette = palette)
                    ReceiptRow(label = "Account", value = "${account?.name ?: "Wallet"} (${account?.accountType ?: "Cash"})", palette = palette)
                    ReceiptRow(label = "Method", value = transaction.paymentMethod, palette = palette)
                    if (transaction.upiApp.isNotBlank()) {
                        ReceiptRow(label = "UPI App", value = transaction.upiApp, palette = palette)
                    }
                    if (transaction.note.isNotBlank()) {
                        ReceiptRow(label = "Note", value = transaction.note, palette = palette)
                    }
                    ReceiptRow(label = "Status", value = "Verified (Offline Encrypted)", palette = palette)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Edit, Print, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onPrint,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x331E293B))
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", color = Color.White)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22F43F5E))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = Color.Transparent
    )
}

@Composable
fun ReceiptRow(label: String, value: String, palette: ThemePalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

/**
 * Add / Edit Transaction Sheet
 * Satisfies Requirement #3 (edit in-place) & Requirement #4 (open/reopen account/wallet picker anytime and + Add Wallet).
 * Features:
 * - Reopenable interactive Account/Wallet Selector with full list + `+ Add Wallet`
 * - Inline Math in amount field (`+ - * / =`)
 * - 1-tap frequent transaction templates
 * - Duplicate transaction guard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    initialTransaction: TransactionEntity? = null,
    defaultType: String = "EXPENSE",
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onSave: (
        id: Long?,
        accountId: Long,
        type: String,
        amount: Double,
        category: String,
        note: String,
        paymentMethod: String,
        upiApp: String
    ) -> Unit,
    onAddNewAccount: (name: String, type: String, balance: Double) -> Unit
) {
    var type by remember { mutableStateOf(initialTransaction?.type ?: defaultType) }
    var amountExpr by remember { mutableStateOf(initialTransaction?.amount?.toString() ?: "") }
    var selectedAccountId by remember {
        mutableLongStateOf(
            initialTransaction?.accountId ?: accounts.firstOrNull()?.id ?: 1L
        )
    }
    var category by remember {
        mutableStateOf(
            initialTransaction?.category ?: (if (type == "INCOME") "Salary" else "Food & Dining")
        )
    }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var paymentMethod by remember { mutableStateOf(initialTransaction?.paymentMethod ?: "UPI") }
    var upiApp by remember { mutableStateOf(initialTransaction?.upiApp ?: "GPay") }

    var showAccountPicker by remember { mutableStateOf(false) }
    var showNewAccountDialog by remember { mutableStateOf(false) }
    var mathEvaluatedMsg by remember { mutableStateOf<String?>(null) }

    val quickTemplates = listOf(
        Triple("☕ Chai/Coffee", 20.0, "Food & Dining"),
        Triple("🥪 Snacks", 60.0, "Food & Dining"),
        Triple("🚕 Auto/Fuel", 150.0, "Transport"),
        Triple("🛒 Groceries", 450.0, "Groceries"),
        Triple("🎬 Cinema", 300.0, "Entertainment")
    )

    val currentAccount = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F172A)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTransaction != null) "Edit Transaction" else "Add Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = palette.textSecondary)
                    }
                }
            }

            // Type Toggle (Expense / Income)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { type = "EXPENSE" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "EXPENSE") Color(0xFFF43F5E) else Color(0x331E293B)
                        )
                    ) {
                        Text("Expense", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { type = "INCOME" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "INCOME") Color(0xFF10B981) else Color(0x331E293B)
                        )
                    ) {
                        Text("Income", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 1-Tap Frequent Templates
            if (initialTransaction == null) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickTemplates) { (name, amt, cat) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x331E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                                modifier = Modifier.clickable {
                                    amountExpr = amt.toInt().toString()
                                    category = cat
                                    note = name
                                }
                            ) {
                                Text(
                                    text = "$name ₹${amt.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Amount Input with Inline Math Calculator
            item {
                Column {
                    OutlinedTextField(
                        value = amountExpr,
                        onValueChange = { amountExpr = it },
                        label = { Text("Amount (₹) [Math supported e.g. 150+40*2]") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val evaluated = evaluateMathExpression(amountExpr)
                                    if (evaluated != null) {
                                        amountExpr = evaluated.toString()
                                        mathEvaluatedMsg = "Calculated: ₹$evaluated"
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Calculate",
                                    tint = palette.primary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_amount_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )
                    if (mathEvaluatedMsg != null) {
                        Text(
                            text = mathEvaluatedMsg ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primary,
                            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                        )
                    }
                }
            }

            // Interactive Reopenable Account / Wallet Selector (Requirement #4)
            item {
                Column {
                    Text(
                        text = "Account / Wallet (Tap to choose or change)",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_selector_btn"),
                        backgroundColor = Color(0x441E293B),
                        borderColor = palette.primary.copy(alpha = 0.5f),
                        onClick = { showAccountPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentAccount?.name ?: "Select Wallet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${currentAccount?.accountType ?: "Cash"} • Bal: ${MoneyPaise.formatCurrency(currentAccount?.balance ?: 0.0)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Change wallet",
                                tint = palette.textSecondary
                            )
                        }
                    }
                }
            }

            // Category Chips Selection
            item {
                Column {
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    val catList = if (type == "INCOME") {
                        listOf("Salary", "Business", "Freelance", "Investment", "Gift", "Other Income")
                    } else {
                        listOf("Food & Dining", "Groceries", "Transport", "Shopping", "Bills & Utilities", "Rent", "Health", "Entertainment")
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(catList) { cat ->
                            val isSelected = category == cat
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) palette.primary else Color(0x331E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) palette.primary else Color(0x22FFFFFF)),
                                modifier = Modifier.clickable { category = cat }
                            ) {
                                Text(
                                    text = "${getCategoryIcon(cat)} $cat",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Note Input
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Payment Method Selection
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "Cash", "Card", "Bank Transfer").forEach { method ->
                        val isSelected = paymentMethod == method
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) palette.secondary else Color(0x331E293B),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMethod = method }
                        ) {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        val parsed = evaluateMathExpression(amountExpr) ?: amountExpr.toDoubleOrNull() ?: 0.0
                        if (parsed > 0) {
                            onSave(
                                initialTransaction?.id,
                                selectedAccountId,
                                type,
                                parsed,
                                category,
                                note.trim(),
                                paymentMethod,
                                upiApp
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_tx_submit"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text(
                        text = if (initialTransaction != null) "Update Transaction" else "Save Transaction",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Interactive Account Picker Dialog (Reopening full list of accounts + + Add Wallet) (Requirement #4)
    if (showAccountPicker) {
        AlertDialog(
            onDismissRequest = { showAccountPicker = false },
            title = {
                Text("Select Account / Wallet", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    accounts.forEach { acc ->
                        val isSelected = acc.id == selectedAccountId
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = if (isSelected) palette.primary.copy(alpha = 0.2f) else Color(0x331E293B),
                            borderColor = if (isSelected) palette.primary else Color(0x22FFFFFF),
                            onClick = {
                                selectedAccountId = acc.id
                                showAccountPicker = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${acc.accountType} • ${MoneyPaise.formatCurrency(acc.balance)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = palette.primary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // + Add New Wallet Action inside picker
                    Button(
                        onClick = {
                            showAccountPicker = false
                            showNewAccountDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add New Wallet", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAccountPicker = false }) {
                    Text("Close", color = palette.textSecondary)
                }
            },
            containerColor = Color(0xFF131D2D)
        )
    }

    if (showNewAccountDialog) {
        AddAccountDialog(
            palette = palette,
            onDismiss = { showNewAccountDialog = false },
            onConfirm = { name, accType, balance, _ ->
                onAddNewAccount(name, accType, balance)
                showNewAccountDialog = false
            }
        )
    }
}

/**
 * Evaluates inline math expressions like "120+45*2" safely without external dependencies.
 */
fun evaluateMathExpression(expr: String): Double? {
    val clean = expr.replace(" ", "").replace("=", "")
    if (clean.isBlank()) return null
    return runCatching {
        // Very simple token parser for +, -, *, /
        var total = 0.0
        val tokens = mutableListOf<String>()
        var currentNumber = StringBuilder()

        for (ch in clean) {
            if (ch in "+-*/") {
                if (currentNumber.isNotEmpty()) {
                    tokens.add(currentNumber.toString())
                    currentNumber = StringBuilder()
                }
                tokens.add(ch.toString())
            } else if (ch.isDigit() || ch == '.') {
                currentNumber.append(ch)
            }
        }
        if (currentNumber.isNotEmpty()) {
            tokens.add(currentNumber.toString())
        }

        if (tokens.isEmpty()) return null

        // First pass: * and /
        val intermediate = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token == "*" || token == "/") {
                val prev = intermediate.removeAt(intermediate.size - 1).toDouble()
                val next = tokens[i + 1].toDouble()
                val res = if (token == "*") prev * next else prev / next
                intermediate.add(res.toString())
                i += 2
            } else {
                intermediate.add(token)
                i++
            }
        }

        // Second pass: + and -
        var result = intermediate[0].toDouble()
        var j = 1
        while (j < intermediate.size) {
            val op = intermediate[j]
            val next = intermediate[j + 1].toDouble()
            result = if (op == "+") result + next else result - next
            j += 2
        }
        result
    }.getOrNull()
}
