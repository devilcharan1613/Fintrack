package com.tracmio.app.ui.nativeapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracmio.app.data.local.AccountEntity
import kotlinx.coroutines.delay
import java.util.Locale

val TracmioPrimaryGreen = Color(0xFF00F5D4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeTracmioApp(viewModel: NativeTracmioViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    var privacyMasked by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }

    fun formatMoney(amount: Double): String {
        return if (privacyMasked) "₹••••••" else String.format(Locale.US, "₹%.2f", amount)
    }

    TracmioNativeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Tracmio 1.3", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "Native Offline Financial Ecosystem",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { privacyMasked = !privacyMasked },
                            modifier = Modifier.testTag("privacy_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (privacyMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle privacy amount mask",
                                tint = Color(0xFF00F5D4)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF090D14),
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Transactions") },
                        label = { Text("Transactions") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Accounts") },
                        label = { Text("Accounts") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                        label = { Text("Reports") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 }
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0 || selectedTab == 1) {
                    FloatingActionButton(
                        onClick = { showAddTxDialog = true },
                        containerColor = Color(0xFF00F5D4),
                        contentColor = Color(0xFF04161E),
                        modifier = Modifier.testTag("fab_add_txn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                } else if (selectedTab == 2) {
                    FloatingActionButton(
                        onClick = { showAddAccountDialog = true },
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF04161E),
                        modifier = Modifier.testTag("fab_add_account")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Account")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF090D14), Color(0xFF04161E), Color(0xFF020609))
                        )
                    )
            ) {
                when (selectedTab) {
                    0 -> DashboardTab(state, ::formatMoney, onAddTxClick = { showAddTxDialog = true })
                    1 -> TransactionsTab(state, ::formatMoney)
                    2 -> AccountsTab(state, ::formatMoney, onAddAccountClick = { showAddAccountDialog = true })
                    3 -> ReportsTab(state, ::formatMoney)
                    4 -> SettingsTab(state)
                }
            }
        }

        if (showAddTxDialog) {
            AddTransactionDialog(
                accounts = state.accounts,
                onDismiss = { showAddTxDialog = false },
                onSave = { accId, type, amount, cat, note ->
                    viewModel.addTransaction(accId, type, amount, cat, note)
                    showAddTxDialog = false
                }
            )
        }

        if (showAddAccountDialog) {
            AddAccountDialog(
                onDismiss = { showAddAccountDialog = false },
                onSave = { name, type, bal ->
                    viewModel.addAccount(name, type, bal)
                    showAddAccountDialog = false
                }
            )
        }
    }
}

@Composable
fun DashboardTab(state: NativeTracmioUiState, formatMoney: (Double) -> String, onAddTxClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("dashboard_tab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2230)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TOTAL NET WEALTH",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFA7F3D0),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatMoney(state.totalBalance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Inflow", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(formatMoney(state.totalIncome), color = Color(0xFF22F59B), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Outflow", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(formatMoney(state.totalExpense), color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(onClick = onAddTxClick) {
                    Text("+ Add New", color = Color(0xFF00F5D4))
                }
            }
        }

        if (state.transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2230))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No transactions yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap + Add New to record income or expenses", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        } else {
            itemsIndexed(state.transactions.take(10), key = { _, tx -> tx.id }) { index, tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2230)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tx.category, fontWeight = FontWeight.Bold, color = Color.White)
                            if (tx.note.isNotBlank()) {
                                Text(tx.note, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            }
                            Text(tx.type, style = MaterialTheme.typography.labelSmall, color = if (tx.type == "INCOME") Color(0xFF22F59B) else Color(0xFFFF6B6B))
                        }
                        Text(
                            text = (if (tx.type == "INCOME") "+" else "-") + formatMoney(tx.amount),
                            fontWeight = FontWeight.Bold,
                            color = if (tx.type == "INCOME") Color(0xFF22F59B) else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionsTab(state: NativeTracmioUiState, formatMoney: (Double) -> String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("All Transactions (${state.transactions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
        itemsIndexed(state.transactions, key = { _, tx -> tx.id }) { _, tx ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2230)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tx.category, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(tx.note.ifBlank { "No description" }, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        Text(tx.paymentMethod, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7F3D0))
                    }
                    Text(
                        text = (if (tx.type == "INCOME") "+" else "-") + formatMoney(tx.amount),
                        fontWeight = FontWeight.Bold,
                        color = if (tx.type == "INCOME") Color(0xFF22F59B) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AccountsTab(state: NativeTracmioUiState, formatMoney: (Double) -> String, onAddAccountClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bank Accounts & Wallets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Button(onClick = onAddAccountClick) {
                    Text("+ Add Account")
                }
            }
        }

        if (state.accounts.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2230))) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("No accounts created yet", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Add a bank account or wallet to track balances.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        } else {
            itemsIndexed(state.accounts, key = { _, acc -> acc.id }) { _, acc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2230)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(acc.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text(acc.accountType, style = MaterialTheme.typography.bodySmall, color = Color(0xFF38BDF8))
                        }
                        Text(formatMoney(acc.balance), fontWeight = FontWeight.ExtraBold, color = Color(0xFF00F5D4), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsTab(state: NativeTracmioUiState, formatMoney: (Double) -> String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Financial Reports & Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2230))) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cashflow Summary", fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Income", color = Color(0xFF94A3B8))
                        Text(formatMoney(state.totalIncome), color = Color(0xFF22F59B), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Expenses", color = Color(0xFF94A3B8))
                        Text(formatMoney(state.totalExpense), color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Savings", color = Color(0xFF94A3B8))
                        Text(formatMoney(state.totalIncome - state.totalExpense), color = Color(0xFF00F5D4), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(state: NativeTracmioUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Tracmio Settings & Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2230))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("App Version", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("Tracmio v1.3 (Native Compose)", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Privacy & Security", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("100% Offline-First. Data stored locally via Room Database.", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, String, Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("Food & Dining") }
    var noteText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { type = "EXPENSE" }, enabled = type != "EXPENSE") { Text("Expense") }
                    Button(onClick = { type = "INCOME" }, enabled = type != "INCOME") { Text("Income") }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    label = { Text("Category") }
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Description") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onSave(selectedAccId, type, amt, categoryText, noteText)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color(0xFF0A2230)
    )
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var typeText by remember { mutableStateOf("Bank") }
    var balanceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bank Account / Wallet", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Account Name (e.g. HDFC Bank)") }
                )
                OutlinedTextField(
                    value = typeText,
                    onValueChange = { typeText = it },
                    label = { Text("Type (Bank, Wallet, Cash)") }
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Initial Balance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val bal = balanceText.toDoubleOrNull() ?: 0.0
                if (nameText.isNotBlank()) {
                    onSave(nameText, typeText, bal)
                }
            }) {
                Text("Create Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color(0xFF0A2230)
    )
}
