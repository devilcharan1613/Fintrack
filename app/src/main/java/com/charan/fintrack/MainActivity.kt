package com.charan.fintrack

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charan.fintrack.data.FinancialCalculator
import com.charan.fintrack.data.local.AccountEntity
import com.charan.fintrack.data.local.BudgetEntity
import com.charan.fintrack.data.local.CategoryEntity
import com.charan.fintrack.data.local.FinTrackDatabase
import com.charan.fintrack.data.local.GoalEntity
import com.charan.fintrack.data.local.LoanEntity
import com.charan.fintrack.data.local.PersonEntity
import com.charan.fintrack.data.local.TransactionEntity
import com.charan.fintrack.data.repository.FinanceRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF020B12)
private val Navy = Color(0xFF06151F)
private val CardNavy = Color(0xFF0C222D)
private val CardRaised = Color(0xFF102B36)
private val Emerald = Color(0xFF12E89A)
private val EmeraldDark = Color(0xFF0A8E68)
private val TextPrimary = Color(0xFFF4FBF9)
private val TextMuted = Color(0xFF91A9B2)
private val Danger = Color(0xFFFF6570)
private val Amber = Color(0xFFFFC857)
private val Blue = Color(0xFF55A8FF)

private enum class MainTab(val label: String) {
    Home("Home"), Accounts("Accounts"), Transactions("Transactions"), Reports("Reports"), More("More")
}
private enum class DetailScreen(val label: String) {
    More("More"), Budgets("Budgets"), Goals("Goals"), Loans("Loans"), Categories("Categories"), People("People"), Settings("Settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { FinTrackApp(this) }
    }
}

@Composable
private fun FinTrackApp(context: Context) {
    val database = remember { FinTrackDatabase.getDatabase(context) }
    val repo = remember { FinanceRepository(database) }
    val accounts by repo.accountList.collectAsStateWithLifecycle(emptyList())
    val transactions by repo.transactions.collectAsStateWithLifecycle(emptyList())
    val budgets by repo.budgets.collectAsStateWithLifecycle(emptyList())
    val goals by repo.goals.collectAsStateWithLifecycle(emptyList())
    val loans by repo.loans.collectAsStateWithLifecycle(emptyList())
    val categories by repo.categories.collectAsStateWithLifecycle(emptyList())
    val persons by repo.persons.collectAsStateWithLifecycle(emptyList())
    val income by repo.income.collectAsStateWithLifecycle(0.0)
    val expense by repo.expense.collectAsStateWithLifecycle(0.0)
    val balance by repo.totalBalance.collectAsStateWithLifecycle(0.0)
    var tab by remember { mutableStateOf(MainTab.Home) }
    var detail by remember { mutableStateOf(DetailScreen.More) }
    var dialog by remember { mutableStateOf<DialogKind?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { repo.seedDefaultCategories() }
    fun notify(message: String) { scope.launch { snackbar.showSnackbar(message) } }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(repo.exportJson()) } }.onSuccess { notify("JSON backup saved") }.onFailure { notify("Backup failed") } }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(repo.exportCsv()) } }.onSuccess { notify("CSV export saved") }.onFailure { notify("Export failed") } }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { repo.restoreJson(it.readText()) } }.onSuccess { notify("Backup restored") }.onFailure { notify("Restore failed") } }
    }
    val importCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { repo.importCsv(it.readText()) }
                    ?: error("Unable to read CSV file")
            }.onSuccess { count -> notify("$count transactions imported") }
                .onFailure { notify("CSV import failed: ${it.message ?: "invalid file"}") }
        }
    }
    val add = {
        dialog = when (tab) {
            MainTab.Accounts -> DialogKind.Account
            MainTab.More -> when (detail) {
                DetailScreen.Budgets -> DialogKind.Budget
                DetailScreen.Goals -> DialogKind.Goal
                DetailScreen.Loans -> DialogKind.Loan
                DetailScreen.Categories -> DialogKind.Category
                DetailScreen.People -> DialogKind.Person
                else -> DialogKind.Transaction
            }
            else -> DialogKind.Transaction
        }
    }
    FinTrackTheme {
        Scaffold(
            containerColor = Ink,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = Navy, tonalElevation = 0.dp) {
                    MainTab.entries.forEach { item ->
                        val icon = when (item) {
                            MainTab.Home -> Icons.Default.Home
                            MainTab.Accounts -> Icons.Default.AccountBalanceWallet
                            MainTab.Transactions -> Icons.Default.ReceiptLong
                            MainTab.Reports -> Icons.Default.Assessment
                            MainTab.More -> Icons.Default.MoreHoriz
                        }
                        NavigationBarItem(item == tab, { tab = item; if (item != MainTab.More) detail = DetailScreen.More }, { Icon(icon, item.label) }, label = { Text(item.label, fontSize = 9.sp) })
                    }
                }
            },
            floatingActionButton = {
                if (!(tab == MainTab.More && detail == DetailScreen.Settings) && tab != MainTab.Reports) {
                    FloatingActionButton(onClick = add, containerColor = Emerald, contentColor = Ink, shape = CircleShape) { Icon(Icons.Default.Add, "Add") }
                }
            }
        ) { padding ->
            AnimatedContent(tab, label = "tab", modifier = Modifier.padding(padding)) { selected ->
                when (selected) {
                    MainTab.Home -> DashboardScreen(accounts, transactions, balance, income, expense, { add() }, { dialog = DialogKind.Transfer })
                    MainTab.Accounts -> AccountsScreen(accounts, transactions, balance, { dialog = DialogKind.Account })
                    MainTab.Transactions -> TransactionsScreen(accounts, transactions, categories, { dialog = DialogKind.Transaction }, { transaction -> scope.launch { repo.deleteTransaction(transaction) } }, { transaction -> dialog = DialogKind.EditTransaction(transaction) })
                    MainTab.Reports -> ReportsScreen(balance, income, expense, transactions)
                    MainTab.More -> MoreScreen(detail, budgets, goals, loans, categories, persons, transactions, { detail = it }, { scope.launch { repo.deleteBudget(it) } }, { scope.launch { repo.deleteGoal(it) } }, { scope.launch { repo.deleteLoan(it) } }, { scope.launch { repo.deleteCategory(it) } }, { scope.launch { repo.deletePerson(it) } }, { exportCsv.launch("fintrack-transactions.csv") }, { exportJson.launch("fintrack-backup.json") }, { restore.launch(arrayOf("application/json", "text/*")) }, { importCsv.launch(arrayOf("text/csv", "text/*")) }, { confirmClear = true })
                }
            }
        }
    }
    dialog?.let { kind ->
        when (kind) {
            DialogKind.Account -> AccountForm({ dialog = null }) { scope.launch { repo.addAccount(it); dialog = null; notify("Account added") } }
            DialogKind.Transaction -> TransactionForm(accounts, categories, null, { dialog = null }) { scope.launch { repo.saveTransaction(it); dialog = null; notify("Transaction saved") } }
            is DialogKind.EditTransaction -> TransactionForm(accounts, categories, kind.transaction, { dialog = null }) { scope.launch { repo.updateTransaction(it); dialog = null; notify("Transaction updated") } }
            DialogKind.Transfer -> TransferForm(accounts, { dialog = null }) { from, to, amount, note -> scope.launch { runCatching { repo.addTransfer(from, to, amount, note) }.onSuccess { dialog = null; notify("Transfer completed") }.onFailure { notify(it.message ?: "Transfer failed") } } }
            DialogKind.Budget -> BudgetForm({ dialog = null }) { scope.launch { repo.addBudget(it); dialog = null; notify("Budget created") } }
            DialogKind.Goal -> GoalForm({ dialog = null }) { scope.launch { repo.addGoal(it); dialog = null; notify("Goal created") } }
            DialogKind.Loan -> LoanForm({ dialog = null }) { scope.launch { repo.addLoan(it); dialog = null; notify("Loan added") } }
            DialogKind.Category -> CategoryForm({ dialog = null }) { scope.launch { repo.addCategory(it); dialog = null } }
            DialogKind.Person -> PersonForm({ dialog = null }) { scope.launch { repo.addPerson(it); dialog = null } }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all data?") },
            text = { Text("This permanently removes all accounts, transactions, budgets, goals, loans, categories and people from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { repo.clearAllData() }
                            .onSuccess { notify("All local data cleared") }
                            .onFailure { notify("Could not clear data") }
                        confirmClear = false
                    }
                }) { Text("Delete everything", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

private sealed interface DialogKind {
    data object Account : DialogKind
    data object Transaction : DialogKind
    data class EditTransaction(val transaction: TransactionEntity) : DialogKind
    data object Transfer : DialogKind
    data object Budget : DialogKind
    data object Goal : DialogKind
    data object Loan : DialogKind
    data object Category : DialogKind
    data object Person : DialogKind
}

@Composable
private fun DashboardScreen(accounts: List<AccountEntity>, transactions: List<TransactionEntity>, balance: Double, income: Double, expense: Double, add: () -> Unit, transfer: () -> Unit) {
    Page("Good ${greeting()} 👋", "Let's make your money work smarter") {
        CardSurface(Modifier.fillMaxWidth(), gradient = true) {
            Text("Total Balance", color = TextMuted, fontSize = 13.sp)
            AnimatedMoney(balance, 30)
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Income", income, Emerald, Icons.Default.ArrowUpward)
                Metric("Expenses", expense, Danger, Icons.Default.ArrowDownward)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction("Add income", Icons.Default.TrendingUp, Emerald) { add() }
            QuickAction("Transfer", Icons.Default.SwapHoriz, Blue, transfer)
        }
        SectionTitle("Accounts", "See all")
        accounts.take(3).forEach { account -> AccountRow(account, accountBalance(account, transactions)) }
        if (accounts.isEmpty()) EmptyState("No accounts yet", "Tap + to add your first wallet.")
        SectionTitle("Recent Transactions", "See all")
        transactions.take(4).forEach { TransactionRow(it, accounts) }
        if (transactions.isEmpty()) EmptyState("No transactions yet", "Your recent activity will appear here.")
    }

@Composable
private fun AccountsScreen(accounts: List<AccountEntity>, transactions: List<TransactionEntity>, balance: Double, add: () -> Unit) {
    Page("Accounts", "Manage your wallets and cards", "Add account", add) {
        CardSurface(Modifier.fillMaxWidth(), gradient = true) { Text("Total Assets", color = TextMuted); AnimatedMoney(balance, 26); Text("${accounts.size} active accounts", color = Emerald, fontSize = 12.sp) }
        accounts.forEach { AccountRow(it, accountBalance(it, transactions), true) }
        if (accounts.isEmpty()) EmptyState("No accounts yet", "Add a bank account, wallet or card.")
    }
}

@Composable
private fun TransactionsScreen(accounts: List<AccountEntity>, transactions: List<TransactionEntity>, categories: List<CategoryEntity>, add: () -> Unit, delete: (TransactionEntity) -> Unit, edit: (TransactionEntity) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val filtered = transactions.filter { (query.isBlank() || "${it.category} ${it.subcategory} ${it.note}".contains(query, true)) && (filter == "All" || it.type == filter.uppercase() || (filter == "Transfer" && it.type.startsWith("TRANSFER_"))) }
    Page("Transactions", "Your complete money history", "Add transaction", add) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search transactions", color = TextMuted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("All", "Income", "Expense", "Borrow", "Lend", "Transfer").forEach { value -> FilterChip(filter == value, { filter = value }, label = { Text(value, fontSize = 11.sp) }) } }
        filtered.forEach { transaction ->
            TransactionRow(transaction, accounts, true, { edit(transaction) }, { delete(transaction) })
        }
        if (filtered.isEmpty()) EmptyState("No matching transactions", "Try another search or filter.")
    }
}

@Composable
private fun MoreScreen(detail: DetailScreen, budgets: List<BudgetEntity>, goals: List<GoalEntity>, loans: List<LoanEntity>, categories: List<CategoryEntity>, persons: List<PersonEntity>, transactions: List<TransactionEntity>, navigate: (DetailScreen) -> Unit, deleteBudget: (BudgetEntity) -> Unit, deleteGoal: (GoalEntity) -> Unit, deleteLoan: (LoanEntity) -> Unit, deleteCategory: (CategoryEntity) -> Unit, deletePerson: (PersonEntity) -> Unit, csv: () -> Unit, json: () -> Unit, restore: () -> Unit, importCsv: () -> Unit, clearData: () -> Unit) {
    when (detail) {
        DetailScreen.More -> Page("More", "Plan, manage and customise") {
            MoreTile("Budgets", "Monthly spending limits", Icons.Default.Assessment) { navigate(DetailScreen.Budgets) }
            MoreTile("Goals", "Build toward what matters", Icons.Default.Flag) { navigate(DetailScreen.Goals) }
            MoreTile("Loans", "Track repayments and interest", Icons.Default.Payments) { navigate(DetailScreen.Loans) }
            MoreTile("Categories", "Organise your spending", Icons.Default.Category) { navigate(DetailScreen.Categories) }
            MoreTile("People", "Borrowed and lent money", Icons.Default.Person) { navigate(DetailScreen.People) }
            MoreTile("Settings", "Backup and app preferences", Icons.Default.Settings) { navigate(DetailScreen.Settings) }
        }
        DetailScreen.Budgets -> Page("Budgets", "Track your monthly limits", back = { navigate(DetailScreen.More) }) { budgets.forEach { budget -> val spent = transactions.filter { t -> t.type == "EXPENSE" && t.category.equals(budget.category, true) }.sumOf { it.amount }; ProgressTile(budget.category, spent, budget.limitAmount, Amber) { deleteBudget(budget) } }; if (budgets.isEmpty()) EmptyState("No budgets yet", "Set a category limit with +.") }
        DetailScreen.Goals -> Page("Goals", "Save for what matters", back = { navigate(DetailScreen.More) }) { goals.forEach { ProgressTile(it.name, it.savedAmount, it.targetAmount, Emerald) { deleteGoal(it) } }; if (goals.isEmpty()) EmptyState("No goals yet", "Create a savings goal with +.") }
        DetailScreen.Loans -> Page("Loans", "Stay on top of repayments", back = { navigate(DetailScreen.More) }) { loans.forEach { ProgressTile(it.name, it.paidAmount, it.principal, Blue) { deleteLoan(it) } }; if (loans.isEmpty()) EmptyState("No loans yet", "Add a loan with +.") }
        DetailScreen.Categories -> Page("Categories", "Income and expense groups", back = { navigate(DetailScreen.More) }) { categories.forEach { DataTile(it.icon.ifBlank { "•" }, it.name, it.type.lowercase().replaceFirstChar(Char::uppercase), Emerald) { if (!it.isSystem) deleteCategory(it) } } }
        DetailScreen.People -> Page("People", "Borrowing and lending contacts", back = { navigate(DetailScreen.More) }) { persons.forEach { DataTile("•", it.name, it.phone.ifBlank { "No phone number" }, Blue) { deletePerson(it) } } }
        DetailScreen.Settings -> Page("Settings", "FinTrack is private and offline", back = { navigate(DetailScreen.More) }) { MoreTile("Backup & Restore", "Export or import your local data", Icons.Default.Backup) { json() }; MoreTile("CSV Export", "Export transaction history", Icons.Default.ReceiptLong, csv); MoreTile("CSV Import", "Add transactions from a CSV file", Icons.Default.ReceiptLong, importCsv); MoreTile("Restore JSON", "Merge a local backup", Icons.Default.Backup, restore); MoreTile("Clear all data", "Permanently remove local records", Icons.Default.Delete, clearData); MoreTile("App information", "FinTrack • INR • Offline only", Icons.Default.Settings) {} }
    }
}

@Composable
private fun ReportsScreen(balance: Double, income: Double, expense: Double, transactions: List<TransactionEntity>) {
    var period by remember { mutableStateOf("This Month") }
    val expenses = transactions.filter { it.type == "EXPENSE" || it.type == "LEND" }
    Page("Reports", "Your financial insights") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Overview", "Income", "Expense").forEach { FilterChip(period == it, { period = it }, label = { Text(it, fontSize = 11.sp) }) } }
        CardSurface(Modifier.fillMaxWidth()) { Text("Expenses", color = TextMuted); DonutChart(expenses.sumOf { it.amount }, income, expense); Text(money(expense), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Total spent this period", color = TextMuted, fontSize = 12.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { Summary("Income", income, Emerald); Summary("Expenses", expense, Danger); Summary("Net savings", income - expense, Blue) }
        Text("Spending by category", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        expenses.groupBy { it.category }.entries.sortedByDescending { it.value.sumOf(TransactionEntity::amount) }.forEachIndexed { index, (category, rows) -> CategoryBar(category, rows.sumOf { it.amount }, expense, chartColors[index % chartColors.size]) }
        CardSurface(Modifier.fillMaxWidth()) { Text("Balance trend", color = TextPrimary, fontWeight = FontWeight.Bold); TrendChart(transactions) }
        Text("Current balance: ${money(balance)}", color = TextMuted, fontSize = 12.sp)
    }
}

private val chartColors = listOf(Emerald, Blue, Amber, Color(0xFFB88CFF), Danger)

@Composable private fun CardSurface(modifier: Modifier = Modifier, gradient: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (gradient) CardRaised else CardNavy), content = { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content) })
}
@Composable private fun Page(title: String, subtitle: String, action: String? = null, onAction: (() -> Unit)? = null, back: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { if (back != null) IconButton(back) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary) }; Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = TextMuted, fontSize = 13.sp) }; if (action != null && onAction != null) TextButton(onAction) { Text(action, color = Emerald) } } }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
    }
}
@Composable private fun SectionTitle(title: String, action: String) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(action, color = Emerald, fontSize = 12.sp) } }
@Composable private fun Metric(label: String, value: Double, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = .15f)), Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }; Spacer(Modifier.width(7.dp)); Column { Text(label, color = TextMuted, fontSize = 11.sp); Text(money(value), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp) } } }
@Composable private fun RowScope.QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) { Card(Modifier.weight(1f).clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = CardNavy)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = color); Spacer(Modifier.width(8.dp)); Text(label, color = TextPrimary, fontSize = 12.sp) } } }
@Composable private fun AccountRow(account: AccountEntity, amount: Double, deletable: Boolean = false) { DataTile(accountIcon(account.accountType), account.name, "${account.accountType} • ${account.currency}", Emerald, amount, deletable) {} }
@Composable private fun DataTile(icon: String, title: String, subtitle: String, color: Color, value: Any? = null, deletable: Boolean = false, onDelete: () -> Unit = {}) { Row(Modifier.fillMaxWidth().background(CardNavy, RoundedCornerShape(15.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = .14f)), Alignment.Center) { Text(icon, fontSize = 18.sp) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold); Text(subtitle, color = TextMuted, fontSize = 11.sp) }; if (value != null) Text(if (value is Double) money(value) else value.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp); if (deletable) IconButton(onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Danger) } } }
@Composable private fun TransactionRow(transaction: TransactionEntity, accounts: List<AccountEntity>, actions: Boolean = false, edit: () -> Unit = {}, delete: () -> Unit = {}) { val positive = transaction.type == "INCOME" || transaction.type == "TRANSFER_IN" || transaction.type == "BORROW"; Row(Modifier.fillMaxWidth().background(CardNavy, RoundedCornerShape(15.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background((if (positive) Emerald else Danger).copy(alpha = .14f)), Alignment.Center) { Icon(if (positive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = if (positive) Emerald else Danger) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(transaction.category, color = TextPrimary, fontWeight = FontWeight.SemiBold); Text("${accounts.firstOrNull { it.id == transaction.accountId }?.name ?: "Account"} • ${formatDate(transaction.timestamp)}", color = TextMuted, fontSize = 10.sp) }; Text("${if (positive) "+" else "-"}${money(transaction.amount)}", color = if (positive) Emerald else Danger, fontWeight = FontWeight.Bold, fontSize = 12.sp); if (actions) { IconButton(edit) { Icon(Icons.Default.Edit, "Edit", tint = Emerald) }; IconButton(delete) { Icon(Icons.Default.Delete, "Delete", tint = Danger) } } } }
@Composable private fun ProgressTile(title: String, current: Double, target: Double, color: Color, delete: () -> Unit) { val progress by animateFloatAsState(FinancialCalculator.progress(current, target).toFloat(), label = "progress"); CardSurface(Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${(progress * 100).toInt()}%", color = color, fontWeight = FontWeight.Bold); IconButton(delete) { Icon(Icons.Default.Delete, "Delete", tint = Danger) } }; Text("${money(current)} of ${money(target)}", color = TextMuted, fontSize = 12.sp); LinearProgressIndicator({ progress }, Modifier.fillMaxWidth(), color, CardRaised) } }
@Composable private fun MoreTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(CardNavy, RoundedCornerShape(15.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Emerald, modifier = Modifier.size(25.dp)); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold); Text(subtitle, color = TextMuted, fontSize = 11.sp) }; Text("›", color = TextMuted, fontSize = 24.sp) } }
@Composable private fun RowScope.Summary(label: String, amount: Double, color: Color) { CardSurface(Modifier.weight(1f)) { Text(label, color = TextMuted, fontSize = 11.sp); Text(money(amount), color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp) } }
@Composable private fun CategoryBar(label: String, amount: Double, total: Double, color: Color) { val width = if (total == 0.0) 0f else (amount / total).toFloat().coerceIn(0f, 1f); Column { Row { Text(label, color = TextPrimary, modifier = Modifier.weight(1f)); Text(money(amount), color = TextMuted, fontSize = 12.sp) }; LinearProgressIndicator({ width }, Modifier.fillMaxWidth().padding(top = 5.dp), color, CardRaised) } }
@Composable private fun DonutChart(total: Double, income: Double, expense: Double) { Box(Modifier.fillMaxWidth().height(170.dp), Alignment.Center) { Canvas(Modifier.size(150.dp)) { val stroke = Stroke(width = 22f, cap = StrokeCap.Butt); drawArc(Emerald, -90f, if (income + expense == 0.0) 360f else (income / (income + expense) * 360).toFloat(), false, style = stroke); drawArc(Danger, -90f + if (income + expense == 0.0) 0f else (income / (income + expense) * 360).toFloat(), if (income + expense == 0.0) 0f else (expense / (income + expense) * 360).toFloat(), false, style = stroke) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(money(total), color = TextPrimary, fontWeight = FontWeight.Bold); Text("Expenses", color = TextMuted, fontSize = 11.sp) } } }
@Composable private fun TrendChart(transactions: List<TransactionEntity>) { Canvas(Modifier.fillMaxWidth().height(100.dp).padding(top = 10.dp)) { val values = transactions.take(12).reversed().runningFold(0.0) { total, t -> total + if (t.type == "INCOME" || t.type == "TRANSFER_IN") t.amount else -t.amount }.drop(1); if (values.size > 1) { val min = values.minOrNull() ?: 0.0; val max = values.maxOrNull() ?: 1.0; val points = values.mapIndexed { i, value -> Offset(size.width * i / (values.lastIndex).coerceAtLeast(1), size.height - ((value - min) / (max - min).coerceAtLeast(1.0) * size.height).toFloat()) }; for (i in 0 until points.lastIndex) drawLine(Emerald, points[i], points[i + 1], 4f, StrokeCap.Round) } } }
@Composable private fun EmptyState(title: String, subtitle: String) { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, null, tint = Emerald.copy(alpha = .7f), modifier = Modifier.size(34.dp)); Text(title, color = TextPrimary, fontWeight = FontWeight.Bold); Text(subtitle, color = TextMuted, fontSize = 12.sp) } }

@Composable private fun AnimatedMoney(value: Double, size: Int) { val animated by animateFloatAsState(value.toFloat(), label = "money"); Text(money(animated.toDouble()), color = Emerald, fontSize = size.sp, fontWeight = FontWeight.Bold) }
private fun accountBalance(account: AccountEntity, transactions: List<TransactionEntity>) = FinancialCalculator.balance(account, transactions)
private fun accountIcon(type: String) = when { type.contains("cash", true) || type.contains("wallet", true) -> "▣"; type.contains("credit", true) -> "▤"; type.contains("saving", true) -> "◉"; else -> "⌂" }
private fun money(value: Double) = "₹" + String.format(Locale("en", "IN"), "%,.2f", value)
private fun formatDate(timestamp: Long) = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
private fun greeting() = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) { in 5..11 -> "morning"; in 12..17 -> "afternoon"; else -> "evening" }
private fun monthKey() = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

@Composable private fun FinTrackTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = darkColorScheme(primary = Emerald, secondary = Emerald, background = Ink, surface = CardNavy, onSurface = TextPrimary), content = content) }

@Composable private fun FormDialog(title: String, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) { AlertDialog(onDismissRequest = dismiss, title = { Text(title, color = TextPrimary) }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp), content = content) }, confirmButton = {}) }
@Composable private fun Field(value: String, change: (String) -> Unit, label: String) { OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
@Composable private fun SaveButtons(error: String, dismiss: () -> Unit, save: () -> Unit) { if (error.isNotBlank()) Text(error, color = Danger, fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(dismiss) { Text("Cancel") }; Button(save, colors = ButtonDefaults.buttonColors(containerColor = Emerald)) { Text("Save", color = Ink) } } }

@Composable private fun AccountForm(dismiss: () -> Unit, save: (AccountEntity) -> Unit) { var name by remember { mutableStateOf("") }; var type by remember { mutableStateOf("Bank account") }; var amount by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; FormDialog("Add account", dismiss) { Field(name, { name = it }, "Account name"); Field(type, { type = it }, "Type"); Field(amount, { amount = it }, "Starting balance"); SaveButtons(error, dismiss) { val parsed = amount.toDoubleOrNull(); if (name.isBlank() || parsed == null || parsed < 0) error = "Enter a name and valid balance" else save(AccountEntity(name = name.trim(), accountType = type.trim(), balance = parsed)) } } }
@Composable private fun TransactionForm(accounts: List<AccountEntity>, categories: List<CategoryEntity>, existing: TransactionEntity?, dismiss: () -> Unit, save: (TransactionEntity) -> Unit) { var type by remember { mutableStateOf(existing?.type ?: "EXPENSE") }; var account by remember { mutableStateOf(existing?.accountId ?: accounts.firstOrNull()?.id ?: 0L) }; var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }; var category by remember { mutableStateOf(existing?.category ?: categories.firstOrNull()?.name ?: "") }; var subcategory by remember { mutableStateOf(existing?.subcategory ?: "") }; var note by remember { mutableStateOf(existing?.note ?: "") }; var error by remember { mutableStateOf("") }; FormDialog(if (existing == null) "Add transaction" else "Edit transaction", dismiss) { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("EXPENSE", "INCOME", "BORROW", "LEND").forEach { value -> FilterChip(type == value, { type = value }, label = { Text(value.lowercase().replaceFirstChar(Char::uppercase), fontSize = 10.sp) }) } }; Field(account.toString(), { account = it.toLongOrNull() ?: account }, "Account ID"); Field(amount, { amount = it }, "Amount"); Field(category, { category = it }, "Category"); Field(subcategory, { subcategory = it }, "Subcategory (optional)"); Field(note, { note = it }, "Note"); SaveButtons(error, dismiss) { val parsed = amount.toDoubleOrNull(); if (account == 0L || parsed == null || parsed <= 0 || category.isBlank()) error = "Enter an account, category and positive amount" else save(TransactionEntity(existing?.id ?: 0, account, type, parsed, category.trim(), note.trim(), existing?.timestamp ?: System.currentTimeMillis(), existing?.transferAccountId, existing?.transferId, subcategory.trim(), existing?.personId)) } } }
@Composable private fun TransferForm(accounts: List<AccountEntity>, dismiss: () -> Unit, save: (Long, Long, Double, String) -> Unit) { var from by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }; var to by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: 0L) }; var amount by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; FormDialog("Transfer money", dismiss) { Field(from.toString(), { from = it.toLongOrNull() ?: from }, "From account ID"); Field(to.toString(), { to = it.toLongOrNull() ?: to }, "To account ID"); Field(amount, { amount = it }, "Amount"); Field(note, { note = it }, "Note"); SaveButtons(error, dismiss) { val parsed = amount.toDoubleOrNull(); if (from == 0L || to == 0L || from == to || parsed == null || parsed <= 0) error = "Choose two accounts and a positive amount" else save(from, to, parsed, note.trim()) } } }
@Composable private fun BudgetForm(dismiss: () -> Unit, save: (BudgetEntity) -> Unit) { var category by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; FormDialog("Create budget", dismiss) { Field(category, { category = it }, "Category"); Field(amount, { amount = it }, "Monthly limit"); SaveButtons(error, dismiss) { val parsed = amount.toDoubleOrNull(); if (category.isBlank() || parsed == null || parsed <= 0) error = "Enter a category and positive limit" else save(BudgetEntity(category = category.trim(), limitAmount = parsed, month = monthKey())) } } }
@Composable private fun GoalForm(dismiss: () -> Unit, save: (GoalEntity) -> Unit) { var name by remember { mutableStateOf("") }; var target by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; FormDialog("Create savings goal", dismiss) { Field(name, { name = it }, "Goal name"); Field(target, { target = it }, "Target amount"); SaveButtons(error, dismiss) { val parsed = target.toDoubleOrNull(); if (name.isBlank() || parsed == null || parsed <= 0) error = "Enter a name and positive target" else save(GoalEntity(name = name.trim(), targetAmount = parsed)) } } }
@Composable private fun LoanForm(dismiss: () -> Unit, save: (LoanEntity) -> Unit) { var name by remember { mutableStateOf("") }; var principal by remember { mutableStateOf("") }; var payment by remember { mutableStateOf("") }; var interest by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; FormDialog("Add loan", dismiss) { Field(name, { name = it }, "Loan name"); Field(principal, { principal = it }, "Principal"); Field(payment, { payment = it }, "Monthly EMI"); Field(interest, { interest = it }, "Interest rate %"); SaveButtons(error, dismiss) { val parsed = principal.toDoubleOrNull(); if (name.isBlank() || parsed == null || parsed <= 0) error = "Enter a name and positive principal" else save(LoanEntity(name = name.trim(), principal = parsed, monthlyPayment = payment.toDoubleOrNull() ?: 0.0, interestRate = interest.toDoubleOrNull() ?: 0.0)) } } }
@Composable private fun CategoryForm(dismiss: () -> Unit, save: (CategoryEntity) -> Unit) { var name by remember { mutableStateOf("") }; var type by remember { mutableStateOf("EXPENSE") }; var error by remember { mutableStateOf("") }; FormDialog("Add category", dismiss) { Row { listOf("EXPENSE", "INCOME").forEach { FilterChip(type == it, { type = it }, label = { Text(it) }) } }; Field(name, { name = it }, "Category name"); SaveButtons(error, dismiss) { if (name.isBlank()) error = "Enter a name" else save(CategoryEntity(name = name.trim(), type = type)) } } }
@Composable private fun PersonForm(dismiss: () -> Unit, save: (PersonEntity) -> Unit) { var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; FormDialog("Add person", dismiss) { Field(name, { name = it }, "Name"); Field(phone, { phone = it }, "Phone"); Field(notes, { notes = it }, "Notes"); SaveButtons(error, dismiss) { if (name.isBlank()) error = "Enter a name" else save(PersonEntity(name = name.trim(), phone = phone.trim(), notes = notes.trim())) } } }
