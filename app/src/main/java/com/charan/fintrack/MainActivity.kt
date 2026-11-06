package com.charan.fintrack

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charan.fintrack.data.local.AccountEntity
import com.charan.fintrack.data.local.BudgetEntity
import com.charan.fintrack.data.local.CategoryEntity
import com.charan.fintrack.data.local.FinTrackDatabase
import com.charan.fintrack.data.local.GoalEntity
import com.charan.fintrack.data.local.LoanEntity
import com.charan.fintrack.data.local.PersonEntity
import com.charan.fintrack.data.local.TransactionEntity
import com.charan.fintrack.data.FinancialCalculator
import com.charan.fintrack.data.repository.FinanceRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF071521)
private val Panel = Color(0xFF102535)
private val Green = Color(0xFF19D99A)
private val Muted = Color(0xFF9CB2C1)
private val White = Color(0xFFF4FAFC)
private val Red = Color(0xFFFF6B75)

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { FinTrackApp(this) }
    }
}

private enum class MainScreen(val title: String) {
    Dashboard("Home"), Accounts("Accounts"), Transactions("Transactions"), Reports("Reports"), More("More")
}

private enum class MoreScreen(val title: String) {
    More("More"), Budgets("Budgets"), Goals("Goals"), Loans("Loans"),
    Categories("Categories"), People("People"), Settings("Settings")
}

@Composable
private fun FinTrackApp(context: Context) {
    val database = remember { FinTrackDatabase.getDatabase(context) }
    val repository = remember { FinanceRepository(database) }
    val accounts by repository.accountList.collectAsStateWithLifecycle(emptyList())
    val transactions by repository.transactions.collectAsStateWithLifecycle(emptyList())
    val budgets by repository.budgets.collectAsStateWithLifecycle(emptyList())
    val goals by repository.goals.collectAsStateWithLifecycle(emptyList())
    val loans by repository.loans.collectAsStateWithLifecycle(emptyList())
    val categories by repository.categories.collectAsStateWithLifecycle(emptyList())
    val persons by repository.persons.collectAsStateWithLifecycle(emptyList())
    val income by repository.income.collectAsStateWithLifecycle(0.0)
    val expense by repository.expense.collectAsStateWithLifecycle(0.0)
    val totalBalance by repository.totalBalance.collectAsStateWithLifecycle(0.0)
    var screen by remember { mutableStateOf(MainScreen.Dashboard) }
    var moreScreen by remember { mutableStateOf(MoreScreen.More) }
    var transactionDialog by remember { mutableStateOf<TransactionEntity?>(null) }
    var dialogType by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { repository.seedDefaultCategories() }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri ->
        if (uri != null) scope.launch {
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(repository.exportCsv()) } }
                .onSuccess { message = "CSV backup saved" }.onFailure { message = "Backup failed: ${it.message}" }
        }
    }
    val jsonExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(repository.exportJson()) } }
                .onSuccess { message = "JSON backup saved" }.onFailure { message = "Backup failed: ${it.message}" }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { repository.restoreJson(it.readText()) } }
                .onSuccess { message = "Backup restored" }.onFailure { message = "Restore failed: ${it.message}" }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Navy, surface = Panel, onSurface = White)) {
        Scaffold(
            containerColor = Navy,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    MainScreen.entries.forEach { item ->
                        val icon = when (item) {
                            MainScreen.Dashboard -> Icons.Default.Home
                            MainScreen.Accounts -> Icons.Default.AccountBalanceWallet
                            MainScreen.Transactions -> Icons.Default.ReceiptLong
                            MainScreen.Reports -> Icons.Default.Assessment
                            MainScreen.More -> Icons.Default.MoreHoriz
                        }
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screen = item },
                            icon = { Icon(icon, item.title) },
                            label = { Text(item.title, fontSize = 10.sp) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (screen != MainScreen.Reports && !(screen == MainScreen.More && moreScreen == MoreScreen.Settings)) {
                    FloatingActionButton(
                        onClick = { transactionDialog = null; dialogType = if (screen == MainScreen.Accounts) "Account" else when (moreScreen) {
                            MoreScreen.Budgets -> "Budget"
                            MoreScreen.Goals -> "Goal"
                            MoreScreen.Loans -> "Loan"
                            MoreScreen.Categories -> "Category"
                            MoreScreen.People -> "Person"
                            else -> "Transaction"
                        } },
                        containerColor = Green,
                        contentColor = Navy
                    ) { Icon(Icons.Default.Add, "Add") }
                }
            }
        ) { padding ->
            AnimatedContent(screen, label = "main-screen", modifier = Modifier.padding(padding)) { selected ->
                when (selected) {
                    MainScreen.Dashboard -> DashboardScreen(accounts, transactions, totalBalance, income, expense, { dialogType = "Transaction" }, { dialogType = "Transfer" })
                    MainScreen.Accounts -> AccountsScreen(accounts, transactions, totalBalance) { dialogType = "Account" }
                    MainScreen.Transactions -> TransactionsScreen(accounts, transactions, onEdit = { transactionDialog = it }, onDelete = { scope.launch { repository.deleteTransaction(it) } })
                    MainScreen.Reports -> ReportsScreen(totalBalance, income, expense, transactions)
                    MainScreen.More -> MoreRoot(
                        moreScreen = moreScreen,
                        budgets = budgets,
                        goals = goals,
                        loans = loans,
                        categories = categories,
                        persons = persons,
                        transactions = transactions,
                        onNavigate = { moreScreen = it },
                        onDeleteBudget = { scope.launch { repository.deleteBudget(it) } },
                        onDeleteGoal = { scope.launch { repository.deleteGoal(it) } },
                        onDeleteLoan = { scope.launch { repository.deleteLoan(it) } },
                        onDeleteCategory = { scope.launch { repository.deleteCategory(it) } },
                        onDeletePerson = { scope.launch { repository.deletePerson(it) } },
                        onExportCsv = { exportLauncher.launch("fintrack-backup.csv") },
                        onExportJson = { jsonExportLauncher.launch("fintrack-backup.json") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/*")) }
                    )
                }
            }
        }
        transactionDialog?.let { existing ->
            TransactionDialog(existing, accounts, onDismiss = { transactionDialog = null }, onSave = {
                scope.launch { repository.updateTransaction(it); transactionDialog = null }
            })
        }
        dialogType?.let { type ->
            when (type) {
                "Transaction" -> TransactionDialog(null, accounts, { dialogType = null }) {
                    scope.launch { repository.saveTransaction(it); dialogType = null }
                }
                "Transfer" -> TransactionDialog(null, accounts, { dialogType = null }, onTransfer = { from, to, amount, note ->
                    scope.launch { repository.addTransfer(from, to, amount, note); dialogType = null }
                })
                "Account" -> AccountDialog({ dialogType = null }) {
                    scope.launch { repository.addAccount(it); dialogType = null }
                }
                "Budget" -> BudgetDialog({ dialogType = null }) {
                    scope.launch { repository.addBudget(it); dialogType = null }
                }
                "Goal" -> GoalDialog({ dialogType = null }) {
                    scope.launch { repository.addGoal(it); dialogType = null }
                }
                "Loan" -> LoanDialog({ dialogType = null }) {
                    scope.launch { repository.addLoan(it); dialogType = null }
                }
                "Category" -> CategoryDialog({ dialogType = null }) {
                    scope.launch { repository.addCategory(it); dialogType = null }
                }
                "Person" -> PersonDialog({ dialogType = null }) {
                    scope.launch { repository.addPerson(it); dialogType = null }
                }
            }
        }
        message?.let {
            LaunchedEffect(it) { kotlinx.coroutines.delay(2200); message = null }
            Text(it, color = Navy, modifier = Modifier.padding(18.dp).background(Green, RoundedCornerShape(12.dp)).padding(12.dp))
        }
    }
}

@Composable
private fun DashboardScreen(accounts: List<AccountEntity>, transactions: List<TransactionEntity>, balance: Double, income: Double, expense: Double, add: () -> Unit, transfer: () -> Unit) {
    val net = income - expense
    ScreenColumn("Dashboard", "Your financial overview") {
        AnimatedBalance(balance)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard("Income", income, Green, Modifier.weight(1f))
            SummaryCard("Expenses", expense, Red, Modifier.weight(1f))
        }
        Card("Net cash flow", money(net), Modifier.fillMaxWidth())
        Text("Accounts", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        accounts.take(3).forEach { account ->
            DataRow(account.name, account.accountType, money(accountBalance(account, transactions)), Green)
        }
        Text("Recent activity", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        transactions.take(4).forEach { TransactionRow(it) }
        if (accounts.isEmpty()) EmptyState("Start with an account", "Add your bank, cash or wallet account.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(add, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.weight(1f)) { Text("Add transaction", color = Navy) }
            OutlinedButton(transfer, modifier = Modifier.weight(1f)) { Text("Transfer") }
        }
    }
}

@Composable
private fun AccountsScreen(accounts: List<AccountEntity>, transactions: List<TransactionEntity>, total: Double, add: () -> Unit) {
    ScreenColumn("Accounts", "Manage your money") {
        AnimatedBalance(total)
        Button(add, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Add account", color = Navy) }
        accounts.forEach { account ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(account.name, color = White, fontWeight = FontWeight.Bold)
                    Text("${account.accountType} • ${account.currency}", color = Muted, fontSize = 12.sp)
                    Text(money(accountBalance(account, transactions)), color = Green, fontSize = 21.sp)
                }
            }
        }
        if (accounts.isEmpty()) EmptyState("No accounts", "Add an account to start tracking balances.")
    }
}

@Composable
private fun TransactionsScreen(accounts: List<AccountEntity>, transactions: List<TransactionEntity>, onEdit: (TransactionEntity) -> Unit, onDelete: (TransactionEntity) -> Unit) {
    ScreenColumn("Transactions", "Income, expenses and transfers") {
        transactions.forEach { transaction ->
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (transaction.type == "INCOME") Icons.Default.TrendingUp else if (transaction.type == "EXPENSE") Icons.Default.TrendingDown else Icons.Default.SwapHoriz, null, tint = if (transaction.type == "INCOME") Green else Red)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(transaction.category, color = White, fontWeight = FontWeight.Medium)
                    Text("${accounts.firstOrNull { it.id == transaction.accountId }?.name ?: "Account"} • ${transaction.note}", color = Muted, fontSize = 11.sp)
                    Text(formatDate(transaction.timestamp), color = Muted, fontSize = 10.sp)
                }
                Text(if (transaction.type == "INCOME" || transaction.type == "TRANSFER_IN") "+" else "-", color = if (transaction.type == "INCOME" || transaction.type == "TRANSFER_IN") Green else Red)
                Text(money(transaction.amount), color = White, fontWeight = FontWeight.Bold)
                IconButton({ onEdit(transaction) }) { Icon(Icons.Default.Edit, "Edit", tint = Green) }
                IconButton({ onDelete(transaction) }) { Icon(Icons.Default.Delete, "Delete", tint = Red) }
            }
        }
        if (transactions.isEmpty()) EmptyState("No transactions", "Record income, expenses or transfers.")
    }
}

@Composable
private fun ReportsScreen(balance: Double, income: Double, expense: Double, transactions: List<TransactionEntity>) {
    ScreenColumn("Reports", "Understand your cash flow") {
        AnimatedBalance(balance)
        SummaryCard("Income", income, Green, Modifier.fillMaxWidth())
        SummaryCard("Expenses", expense, Red, Modifier.fillMaxWidth())
        SummaryCard("Savings rate", if (income == 0.0) 0.0 else (income - expense) / income * 100, Green, Modifier.fillMaxWidth(), suffix = "%")
        Text("Categories", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        transactions.filter { it.type == "EXPENSE" }.groupBy { it.category }.entries.sortedByDescending { it.value.sumOf(TransactionEntity::amount) }.forEach {
            DataRow(it.key, "${it.value.size} transactions", money(it.value.sumOf(TransactionEntity::amount)), Red)
        }
    }
}

@Composable
private fun MoreRoot(
    moreScreen: MoreScreen,
    budgets: List<BudgetEntity>,
    goals: List<GoalEntity>,
    loans: List<LoanEntity>,
    categories: List<CategoryEntity>,
    persons: List<PersonEntity>,
    transactions: List<TransactionEntity>,
    onNavigate: (MoreScreen) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit,
    onDeleteGoal: (GoalEntity) -> Unit,
    onDeleteLoan: (LoanEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onDeletePerson: (PersonEntity) -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onRestore: () -> Unit
) {
    when (moreScreen) {
        MoreScreen.More -> ScreenColumn("More", "Planning and settings") {
            MoreCard("Budgets", "Set monthly category limits", Icons.Default.Assessment) { onNavigate(MoreScreen.Budgets) }
            MoreCard("Goals", "Track savings progress", Icons.Default.Flag) { onNavigate(MoreScreen.Goals) }
            MoreCard("Loans", "Track principal and repayments", Icons.Default.Payments) { onNavigate(MoreScreen.Loans) }
            MoreCard("Categories", "Organise income and expenses", Icons.Default.Assessment) { onNavigate(MoreScreen.Categories) }
            MoreCard("People", "Track borrowed and lent money", Icons.Default.AccountBalanceWallet) { onNavigate(MoreScreen.People) }
            MoreCard("Settings", "Backup, restore and app preferences", Icons.Default.Settings) { onNavigate(MoreScreen.Settings) }
        }
        MoreScreen.Budgets -> ScreenColumn("Budgets", "Monthly spending limits") {
            BackButton { onNavigate(MoreScreen.More) }
            budgets.forEach { budget ->
                val spent = transactions.filter { it.type == "EXPENSE" && it.category.equals(budget.category, true) }.sumOf { it.amount }
                ProgressCard(budget.category, spent, budget.limitAmount) { onDeleteBudget(budget) }
            }
            if (budgets.isEmpty()) EmptyState("No budgets", "Add a budget to see category progress.")
        }
        MoreScreen.Goals -> ScreenColumn("Goals", "Build toward what matters") {
            BackButton { onNavigate(MoreScreen.More) }
            goals.forEach { goal -> ProgressCard(goal.name, goal.savedAmount, goal.targetAmount) { onDeleteGoal(goal) } }
            if (goals.isEmpty()) EmptyState("No goals", "Create a savings goal.")
        }
        MoreScreen.Loans -> ScreenColumn("Loans", "Outstanding balances") {
            BackButton { onNavigate(MoreScreen.More) }
            loans.forEach { loan -> ProgressCard(loan.name, loan.paidAmount, loan.principal) { onDeleteLoan(loan) } }
            if (loans.isEmpty()) EmptyState("No loans", "Add a loan to track repayments.")
        }
        MoreScreen.Categories -> ScreenColumn("Categories", "Income and expense categories") {
            BackButton { onNavigate(MoreScreen.More) }
            categories.forEach { category ->
                DataRow(category.name, category.type.lowercase().replaceFirstChar(Char::uppercase), category.icon.ifBlank { "•" }, Green)
                if (!category.isSystem) IconButton({ onDeleteCategory(category) }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Red)
                }
            }
            if (categories.isEmpty()) EmptyState("No categories", "Add a category to organise transactions.")
        }
        MoreScreen.People -> ScreenColumn("People", "Borrowed and lent balances") {
            BackButton { onNavigate(MoreScreen.More) }
            persons.forEach { person ->
                DataRow(person.name, person.phone.ifBlank { "No phone number" }, person.notes.ifBlank { "No notes" }, Green)
                IconButton({ onDeletePerson(person) }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Red)
                }
            }
            if (persons.isEmpty()) EmptyState("No people", "Add people for borrow and lend tracking.")
        }
        MoreScreen.Settings -> ScreenColumn("Settings", "FinTrack is offline by design") {
            BackButton { onNavigate(MoreScreen.More) }
            MoreCard("CSV backup", "Export transaction history", Icons.Default.Backup, onExportCsv)
            MoreCard("JSON backup", "Export a complete local backup", Icons.Default.Backup, onExportJson)
            MoreCard("Restore backup", "Merge a JSON backup into this device", Icons.Default.Backup, onRestore)
            Text("No login, cloud sync, analytics or internet permission is required.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TransactionDialog(
    existing: TransactionEntity?,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit = {},
    onTransfer: ((Long, Long, Double, String) -> Unit)? = null
) {
    var type by remember { mutableStateOf(existing?.type ?: "EXPENSE") }
    var accountText by remember { mutableStateOf((existing?.accountId ?: accounts.firstOrNull()?.id ?: 0).toString()) }
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var targetAccountText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    FormDialog(if (existing == null) "New transaction" else "Edit transaction", onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("EXPENSE", "INCOME", "TRANSFER").forEach { value -> FilterChip(type == value, { type = value }, label = { Text(value.lowercase().replaceFirstChar(Char::uppercase)) }) }
        }
        Field(accountText, { accountText = it }, "Account ID")
        if (type == "TRANSFER") Field(targetAccountText, { targetAccountText = it }, "Destination account ID")
        Field(amount, { amount = it }, "Amount")
        if (type != "TRANSFER") Field(category, { category = it }, "Category")
        Field(note, { note = it }, "Note")
        SaveRow(error, onDismiss) {
            val parsed = amount.toDoubleOrNull()
            val accountId = accountText.toLongOrNull()
            val targetId = targetAccountText.toLongOrNull()
            if (accountId == null || parsed == null || parsed <= 0 || (type != "TRANSFER" && category.isBlank())) {
                error = "Choose an account and enter a positive amount and category"
            } else if (type == "TRANSFER" && (targetId == null || targetId == accountId || onTransfer == null)) {
                error = "Enter a different destination account ID"
            } else if (type == "TRANSFER") {
                onTransfer?.invoke(accountId, targetId!!, parsed, note.trim())
            } else {
                onSave(TransactionEntity(existing?.id ?: 0, accountId, type, parsed, category.trim(), note.trim(), existing?.timestamp ?: System.currentTimeMillis()))
            }
        }
    }
}

@Composable
private fun AccountDialog(onDismiss: () -> Unit, onSave: (AccountEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Bank") }
    var opening by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf("") }
    FormDialog("New account", onDismiss) {
        Field(name, { name = it }, "Name")
        Field(type, { type = it }, "Type")
        Field(opening, { opening = it }, "Opening balance")
        SaveRow(error, onDismiss) {
            val value = opening.toDoubleOrNull()
            if (name.isBlank() || value == null || value < 0) error = "Enter a name and non-negative balance"
            else onSave(AccountEntity(name = name.trim(), accountType = type.trim(), balance = value))
        }
    }
}

@Composable
private fun BudgetDialog(onDismiss: () -> Unit, onSave: (BudgetEntity) -> Unit) {
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    FormDialog("New budget", onDismiss) {
        Field(category, { category = it }, "Category")
        Field(amount, { amount = it }, "Monthly limit")
        SaveRow(error, onDismiss) {
            val value = amount.toDoubleOrNull()
            if (category.isBlank() || value == null || value <= 0) error = "Enter a category and positive limit"
            else onSave(BudgetEntity(category = category.trim(), limitAmount = value, month = monthKey()))
        }
    }
}

@Composable
private fun GoalDialog(onDismiss: () -> Unit, onSave: (GoalEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    FormDialog("New savings goal", onDismiss) {
        Field(name, { name = it }, "Goal name")
        Field(target, { target = it }, "Target amount")
        SaveRow(error, onDismiss) {
            val value = target.toDoubleOrNull()
            if (name.isBlank() || value == null || value <= 0) error = "Enter a name and positive target"
            else onSave(GoalEntity(name = name.trim(), targetAmount = value))
        }
    }
}

@Composable
private fun LoanDialog(onDismiss: () -> Unit, onSave: (LoanEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var principal by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    FormDialog("New loan", onDismiss) {
        Field(name, { name = it }, "Loan name")
        Field(principal, { principal = it }, "Principal")
        Field(payment, { payment = it }, "Monthly payment")
        SaveRow(error, onDismiss) {
            val value = principal.toDoubleOrNull()
            if (name.isBlank() || value == null || value <= 0) error = "Enter a name and positive principal"
            else onSave(LoanEntity(name = name.trim(), principal = value, monthlyPayment = payment.toDoubleOrNull() ?: 0.0))
        }
    }
}

@Composable
private fun CategoryDialog(onDismiss: () -> Unit, onSave: (CategoryEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var icon by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    FormDialog("New category", onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("EXPENSE", "INCOME").forEach { value ->
                FilterChip(type == value, { type = value }, label = { Text(value.lowercase().replaceFirstChar(Char::uppercase)) })
            }
        }
        Field(name, { name = it }, "Name")
        Field(icon, { icon = it }, "Icon or emoji (optional)")
        SaveRow(error, onDismiss) {
            if (name.isBlank()) error = "Enter a category name"
            else onSave(CategoryEntity(name = name.trim(), type = type, icon = icon.trim()))
        }
    }
}

@Composable
private fun PersonDialog(onDismiss: () -> Unit, onSave: (PersonEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    FormDialog("New person", onDismiss) {
        Field(name, { name = it }, "Name")
        Field(phone, { phone = it }, "Phone number")
        Field(notes, { notes = it }, "Notes")
        SaveRow(error, onDismiss) {
            if (name.isBlank()) error = "Enter a name"
            else onSave(PersonEntity(name = name.trim(), phone = phone.trim(), notes = notes.trim()))
        }
    }
}

@Composable
private fun FormDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun SaveRow(error: String, onDismiss: () -> Unit, onSave: () -> Unit) {
    if (error.isNotBlank()) Text(error, color = Red, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onDismiss) { Text("Cancel") }
        Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Save", color = Navy) }
    }
}

@Composable
private fun ScreenColumn(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item { Text(title, color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted) }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
    }
}

@Composable
private fun AnimatedBalance(value: Double) {
    val animated by animateFloatAsState(value.toFloat(), label = "balance")
    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) { Text("Current balance", color = Muted); Text(money(animated.toDouble()), color = Green, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SummaryCard(label: String, value: Double, color: Color, modifier: Modifier, suffix: String = "") {
    val animated by animateColorAsState(color, label = "summary")
    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = modifier) {
        Column(Modifier.padding(15.dp)) { Text(label, color = Muted, fontSize = 12.sp); Text(money(value) + suffix, color = animated, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
    }
}

@Composable
private fun ProgressCard(title: String, current: Double, target: Double, onDelete: () -> Unit) {
    val progress = if (target <= 0) 0f else (current / target).coerceIn(0.0, 1.0).toFloat()
    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Red) } }
            Text("${money(current)} of ${money(target)}", color = Muted, fontSize = 12.sp)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = Green, trackColor = Navy)
        }
    }
}

@Composable
private fun MoreCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Green, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(14.dp)); Column { Text(title, color = White, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 12.sp) }
    }
}

@Composable
private fun DataRow(title: String, subtitle: String, value: Any, color: Color) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = White, fontWeight = FontWeight.Medium); Text(subtitle, color = Muted, fontSize = 11.sp) }
        Text(value.toString(), color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TransactionRow(transaction: TransactionEntity) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (transaction.type == "INCOME") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = if (transaction.type == "INCOME") Green else Red)
        Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(transaction.category, color = White); Text(transaction.note, color = Muted, fontSize = 11.sp) }
        Text(money(transaction.amount), color = if (transaction.type == "INCOME") Green else Red)
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) { OutlinedButton(onClick) { Text("Back") } }

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(progress = { 0f }, color = Green, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp)); Text(title, color = White, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

private fun accountBalance(account: AccountEntity, transactions: List<TransactionEntity>): Double =
    FinancialCalculator.balance(account, transactions)

private fun money(value: Double) = "₹" + String.format(Locale("en", "IN"), "%,.2f", value)
private fun formatDate(timestamp: Long) = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
private fun monthKey() = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
