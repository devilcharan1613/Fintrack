package com.charan.fintrack.ui.nativeapp

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charan.fintrack.data.MoneyPaise
import com.charan.fintrack.data.NativeLedgerCalculator
import com.charan.fintrack.data.local.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

private enum class FinPage(val label: String) {
    HOME("Home"), TRANSACTIONS("Transactions"), ACCOUNTS("Accounts"),
    REPORTS("Reports"), MORE("More"), PEOPLE("People"), BUDGETS("Budgets"),
    GOALS("Savings Goals"), LOANS("Loans"), CATEGORIES("Categories"), SETTINGS("Settings")
}
private enum class EditorKind { NONE, TRANSACTION, ACCOUNT, PERSON, BUDGET, GOAL, LOAN, CONTRIBUTION, REPAYMENT, CATEGORY }

@Composable
fun NativeFinTrackApp(viewModel: NativeFinTrackViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var dark by remember { mutableStateOf(context.getSharedPreferences("fintrack_native_settings", 0).getBoolean("dark_theme", true)) }

    val backupSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let(viewModel::exportBackup)
    }
    val backupOpen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(viewModel::importBackup)
    }
    val csvSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        it?.let(viewModel::exportCsv)
    }
    val xlsxSave = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { it?.let(viewModel::exportXlsx) }
    val pdfSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {
        it?.let(viewModel::exportPdf)
    }

    FinTrackNativeTheme(darkTheme = dark) {
        NativeFinTrackContent(
            viewModel = viewModel,
            darkTheme = dark,
            onThemeChange = {
                dark = it
                context.getSharedPreferences("fintrack_native_settings", 0).edit().putBoolean("dark_theme", it).apply()
            },
            onExportBackup = { backupSave.launch("FinTrack_Backup.json") },
            onImportBackup = { backupOpen.launch(arrayOf("application/json", "text/json", "application/octet-stream")) },
            onExportCsv = { csvSave.launch("FinTrack_Transactions.csv") },
            onExportXlsx = { xlsxSave.launch("FinTrack_Export.xlsx") },
            onExportPdf = { pdfSave.launch("FinTrack_Statement.pdf") }
        )
    }
}

@Composable
private fun NativeFinTrackContent(
    viewModel: NativeFinTrackViewModel,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportPdf: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val people by viewModel.people.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val totals by viewModel.dashboard.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf(FinPage.HOME) }
    var editor by remember { mutableStateOf(EditorKind.NONE) }
    var selectedGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var selectedLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var selectedBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF062A3A), MaterialTheme.colorScheme.background))
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NativeBottomBar(page, { page = it }, { editor = EditorKind.TRANSACTION })
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                NativeHeader(
                    title = if (page == FinPage.HOME) "FinTrack" else page.label,
                    subtitle = if (page == FinPage.HOME) greetingText() else "FinTrack Pro • Offline Edition",
                    onAdd = {
                        editor = when (page) {
                            FinPage.ACCOUNTS -> EditorKind.ACCOUNT
                            FinPage.PEOPLE -> EditorKind.PERSON
                            FinPage.BUDGETS -> EditorKind.BUDGET
                            FinPage.GOALS -> EditorKind.GOAL
                            FinPage.LOANS -> EditorKind.LOAN
                            FinPage.CATEGORIES -> EditorKind.CATEGORY
                            else -> EditorKind.TRANSACTION
                        }
                    }
                )
                when (page) {
                    FinPage.HOME -> DashboardScreen(accounts, transactions, totals, { editor = EditorKind.ACCOUNT }, { page = FinPage.TRANSACTIONS })
                    FinPage.TRANSACTIONS -> TransactionsScreen(
                        transactions, viewModel::deleteTransaction,
                        onEdit = { selectedTransaction = it; editor = EditorKind.TRANSACTION }
                    )
                    FinPage.ACCOUNTS -> AccountsScreen(
                        accounts, transactions, viewModel::archiveAccount,
                        onEdit = { selectedAccount = it; editor = EditorKind.ACCOUNT }
                    )
                    FinPage.REPORTS -> ReportsScreen(accounts, transactions, totals)
                    FinPage.MORE -> MoreScreen { page = it }
                    FinPage.PEOPLE -> PeopleScreen(
                        people, transactions, viewModel::deletePerson,
                        onEdit = { selectedPerson = it; editor = EditorKind.PERSON }
                    )
                    FinPage.BUDGETS -> BudgetsScreen(
                        budgets, transactions, viewModel::deleteBudget,
                        onEdit = { selectedBudget = it; editor = EditorKind.BUDGET }
                    )
                    FinPage.GOALS -> GoalsScreen(
                        goals,
                        onContribute = { selectedGoal = it; editor = EditorKind.CONTRIBUTION },
                        onDelete = viewModel::deleteGoal,
                        onEdit = { selectedGoal = it; editor = EditorKind.GOAL },
                        viewModel = viewModel
                    )
                    FinPage.LOANS -> LoansScreen(
                        loans,
                        onRepay = { selectedLoan = it; editor = EditorKind.REPAYMENT },
                        onDelete = viewModel::deleteLoan,
                        onEdit = { selectedLoan = it; editor = EditorKind.LOAN },
                        viewModel = viewModel
                    )
                    FinPage.CATEGORIES -> CategoriesScreen(
                        categories, viewModel::deleteCategory,
                        onEdit = { selectedCategory = it; editor = EditorKind.CATEGORY }
                    )
                    FinPage.SETTINGS -> SettingsScreen(
                        darkTheme, onThemeChange, onExportBackup, onImportBackup,
                        onExportCsv, onExportXlsx, onExportPdf
                    )
                }
            }
        }

        if (editor != EditorKind.NONE) {
            NativeEditorDialog(
                kind = editor, accounts = accounts, people = people,
                selectedGoal = selectedGoal, selectedLoan = selectedLoan,
                selectedTransaction = selectedTransaction, selectedAccount = selectedAccount,
                selectedPerson = selectedPerson, selectedBudget = selectedBudget,
                selectedCategory = selectedCategory,
                onDismiss = {
                    editor = EditorKind.NONE
                    selectedGoal = null; selectedLoan = null; selectedTransaction = null
                    selectedAccount = null; selectedPerson = null; selectedBudget = null; selectedCategory = null
                },
                onSave = { values ->
                    when (editor) {
                        EditorKind.ACCOUNT -> {
                            val current = selectedAccount
                            if (current == null) viewModel.addAccount(values["name"].orEmpty(), values["type"].orEmpty(), values["amount"].orEmpty(), values["creditLimit"].orEmpty())
                            else viewModel.updateAccount(current, values["name"].orEmpty(), values["type"].orEmpty(), values["amount"].orEmpty(), values["creditLimit"].orEmpty())
                        }
                        EditorKind.TRANSACTION -> {
                            val current = selectedTransaction
                            if (current == null) viewModel.addTransaction(
                                values["type"].orEmpty(), values["amount"].orEmpty(),
                                values["accountId"]?.toLongOrNull() ?: 0L,
                                values["category"].orEmpty(), values["note"].orEmpty(),
                                values["toAccountId"]?.toLongOrNull(),
                                values["personId"]?.toLongOrNull(),
                                values["paymentMethod"].orEmpty(), values["upiApp"].orEmpty(),
                                values["date"].orEmpty(), values["time"].orEmpty()
                            ) else viewModel.updateTransaction(
                                current, values["type"].orEmpty(), values["amount"].orEmpty(),
                                values["accountId"]?.toLongOrNull() ?: current.accountId,
                                values["category"].orEmpty(), values["note"].orEmpty(),
                                values["toAccountId"]?.toLongOrNull(),
                                values["personId"]?.toLongOrNull(),
                                values["paymentMethod"].orEmpty(), values["upiApp"].orEmpty(),
                                values["date"].orEmpty(), values["time"].orEmpty()
                            )
                        }
                        EditorKind.PERSON -> {
                            val current = selectedPerson
                            if (current == null) viewModel.addPerson(values["name"].orEmpty(), values["phone"].orEmpty(), values["note"].orEmpty())
                            else viewModel.updatePerson(current, values["name"].orEmpty(), values["phone"].orEmpty(), values["note"].orEmpty())
                        }
                        EditorKind.CATEGORY -> {
                            val current = selectedCategory
                            if (current == null) viewModel.addCategory(values["name"].orEmpty(), values["type"].orEmpty())
                            else viewModel.updateCategory(current, values["name"].orEmpty(), values["type"].orEmpty(), values["icon"].orEmpty())
                        }
                        EditorKind.BUDGET -> {
                            val current = selectedBudget
                            if (current == null) viewModel.addBudget(values["category"].orEmpty(), values["amount"].orEmpty(), values["month"].orEmpty(), values["period"].orEmpty())
                            else viewModel.updateBudget(current, values["category"].orEmpty(), values["amount"].orEmpty(), values["month"].orEmpty(), values["period"].orEmpty())
                        }
                        EditorKind.GOAL -> {
                            val current = selectedGoal
                            if (current == null) viewModel.addGoal(values["name"].orEmpty(), values["amount"].orEmpty())
                            else viewModel.updateGoal(current, values["name"].orEmpty(), values["amount"].orEmpty())
                        }
                        EditorKind.LOAN -> {
                            val current = selectedLoan
                            if (current == null) viewModel.addLoan(values["name"].orEmpty(), values["amount"].orEmpty(), values["monthly"].orEmpty(), values["interest"].orEmpty())
                            else viewModel.updateLoan(current, values["name"].orEmpty(), values["amount"].orEmpty(), values["monthly"].orEmpty(), values["interest"].orEmpty())
                        }
                        EditorKind.CONTRIBUTION -> selectedGoal?.let { viewModel.contribute(it.id, values["amount"].orEmpty(), values["note"].orEmpty()) }
                        EditorKind.REPAYMENT -> selectedLoan?.let { viewModel.repay(it.id, values["amount"].orEmpty(), values["note"].orEmpty()) }
                        EditorKind.NONE -> Unit
                    }
                    editor = EditorKind.NONE
                    selectedGoal = null; selectedLoan = null; selectedTransaction = null
                    selectedAccount = null; selectedPerson = null; selectedBudget = null; selectedCategory = null
                }
            )
        }
    }
}

@Composable
private fun NativeHeader(title: String, subtitle: String, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.BarChart, null, tint = Color(0xFF00E6D0), modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(9.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(24.dp)) {
            Icon(Icons.Filled.Add, null)
            Text("Add")
        }
    }
}

@Composable
private fun NativeBottomBar(current: FinPage, onNavigate: (FinPage) -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xCC082C3D))
            .border(1.dp, Color(0x6655D8E6), RoundedCornerShape(28.dp))
            .padding(horizontal = 3.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavItem("Home", Icons.Filled.Home, current == FinPage.HOME) { onNavigate(FinPage.HOME) }
        BottomNavItem("Transactions", Icons.Filled.ReceiptLong, current == FinPage.TRANSACTIONS) { onNavigate(FinPage.TRANSACTIONS) }
        FloatingActionButton(
            onClick = onAdd, modifier = Modifier.size(50.dp), shape = CircleShape,
            containerColor = Color(0xFF00DCCB), contentColor = Color(0xFF03202A)
        ) { Icon(Icons.Filled.Add, "Add transaction", modifier = Modifier.size(28.dp)) }
        BottomNavItem("Accounts", Icons.Filled.AccountBalanceWallet, current == FinPage.ACCOUNTS) { onNavigate(FinPage.ACCOUNTS) }
        BottomNavItem("Reports", Icons.Filled.BarChart, current == FinPage.REPORTS) { onNavigate(FinPage.REPORTS) }
        BottomNavItem("More", Icons.Filled.MoreHoriz, current == FinPage.MORE) { onNavigate(FinPage.MORE) }
    }
}

@Composable
private fun BottomNavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.widthIn(min = 42.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (selected) Color(0xFF16E7D7) else Color(0xFFD8E7F0), modifier = Modifier.size(19.dp))
        Text(label, color = if (selected) Color(0xFF16E7D7) else Color(0xFFD8E7F0), style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun DashboardScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    totals: com.charan.fintrack.data.repository.NativeDashboardTotals,
    onAddAccount: () -> Unit,
    onTransactions: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp)) {
        GlassCard {
            Text("TOTAL BALANCE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(money(totals.balancePaise), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricMini("Income", money(totals.incomePaise), Color(0xFF12D6A0), Icons.Filled.ArrowDownward, Modifier.weight(1f))
                MetricMini("Expense", money(totals.expensePaise), Color(0xFFFF687B), Icons.Filled.ArrowUpward, Modifier.weight(1f))
            }
        }
        SectionLabel("QUICK STATS")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallMetric("Borrowed", money(totals.borrowedPaise), Icons.Filled.ArrowDownward, Color(0xFFA980FF), Modifier.weight(1f))
            SmallMetric("Lent", money(totals.lentPaise), Icons.Filled.ArrowUpward, Color(0xFFFFB76B), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallMetric("Net Worth", money(totals.netWorthPaise), Icons.Filled.Diamond, Color(0xFF15DDD1), Modifier.weight(1f))
            SmallMetric("Transactions", totals.transactionCount.toString(), Icons.Filled.BarChart, Color(0xFF8D83FF), Modifier.weight(1f))
        }
        SectionLabel("ACCOUNTS & WALLETS")
        if (accounts.isEmpty()) {
            GlassCard {
                Text("No accounts yet", fontWeight = FontWeight.Bold)
                Text("Add your first cash, bank or wallet account to start tracking balances.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onAddAccount) { Icon(Icons.Filled.Add, null); Text("Add Account") }
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accounts.forEach { account ->
                    val balance = NativeLedgerCalculator.accountBalancePaise(account, transactions)
                    GlassCard(Modifier.width(190.dp)) {
                        Text(account.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(account.accountType.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(money(balance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                GlassCard(Modifier.width(150.dp).clickable(onClick = onAddAccount)) {
                    Icon(Icons.Filled.Add, null, tint = Color(0xFF00DCCB), modifier = Modifier.size(32.dp))
                    Text("Add Account", fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("RECENT TRANSACTIONS")
            TextButton(onClick = onTransactions) { Text("View all") }
        }
        if (transactions.isEmpty()) GlassCard { Text("Your latest transactions will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else transactions.take(5).forEach { TransactionItem(it, null) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    onDelete: (TransactionEntity) -> Unit,
    onEdit: (TransactionEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    val filtered = transactions.filter { row ->
        (filter == "ALL" || row.type.equals(filter, true) || (filter == "TRANSFER" && row.type.startsWith("TRANSFER", true))) &&
            (search.isBlank() || row.note.contains(search, true) || row.category.contains(search, true) ||
                row.type.contains(search, true) || row.amountPaise.toString().contains(search))
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Search transactions") }
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("ALL", "INCOME", "EXPENSE", "TRANSFER", "BORROW", "LEND").forEach { item ->
                FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item) })
            }
        }
        Text(filtered.size.toString() + " transactions", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        if (filtered.isEmpty()) EmptyState("No matching transactions", "Add income, expenses, transfers, borrowing or lending records.")
        else LazyColumn(contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.stableUid }) { row -> TransactionItem(row, onDelete, onEdit) }
        }
    }
}

@Composable
private fun AccountsScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    onArchive: (Long) -> Unit,
    onEdit: (AccountEntity) -> Unit
) {
    if (accounts.isEmpty()) EmptyState("Accounts and wallets", "Create a cash, bank or digital wallet account using Add.")
    else LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(accounts, key = { it.stableUid }) { account ->
            val current = NativeLedgerCalculator.accountBalancePaise(account, transactions)
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color(0xFF30D8CC), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.name, fontWeight = FontWeight.Bold)
                        Text(account.accountType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(money(current), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                Text("Opening balance: " + money(account.openingBalancePaise), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEdit(account) }) { Text("Edit") }
                    TextButton(onClick = { onArchive(account.id) }) { Text("Archive") }
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    totals: com.charan.fintrack.data.repository.NativeDashboardTotals
) {
    val maxValue = maxOf(totals.incomePaise, totals.expensePaise, 1L).toFloat()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        SectionLabel("INCOME VS EXPENSE")
        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) { Text("Income", color = Color(0xFF19D5A3)); Text(money(totals.incomePaise), fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f)) { Text("Expense", color = Color(0xFFFF7380)); Text(money(totals.expensePaise), fontWeight = FontWeight.Bold) }
            }
            Canvas(Modifier.fillMaxWidth().height(125.dp)) {
                val base = size.height - 8f
                val barWidth = size.width / 5f
                val incomeHeight = (totals.incomePaise.toFloat() / maxValue) * (size.height - 18f)
                val expenseHeight = (totals.expensePaise.toFloat() / maxValue) * (size.height - 18f)
                drawLine(Color(0xFF18D4A6), Offset(barWidth, base), Offset(barWidth, base - incomeHeight), barWidth * .45f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFF687B), Offset(barWidth * 3, base), Offset(barWidth * 3, base - expenseHeight), barWidth * .45f, cap = StrokeCap.Round)
            }
        }
        SectionLabel("ACCOUNT BALANCES")
        accounts.forEach { account ->
            val current = NativeLedgerCalculator.accountBalancePaise(account, transactions)
            GlassCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(account.name, fontWeight = FontWeight.SemiBold); Text(money(current), fontWeight = FontWeight.Bold)
            } }
            Spacer(Modifier.height(8.dp))
        }
        SectionLabel("SPENDING BY CATEGORY")
        val categories = transactions.filter { it.type.equals("EXPENSE", true) }.groupBy { it.category }
        if (categories.isEmpty()) GlassCard { Text("No expense records for analysis yet.") }
        categories.entries.sortedByDescending { e -> e.value.sumOf { it.amountPaise } }.forEach { (name, rows) ->
            val sum = rows.fold(0L) { acc, row -> Math.addExact(acc, row.amountPaise) }
            GlassCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name); Text(money(sum), fontWeight = FontWeight.Bold)
            } }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MoreScreen(onNavigate: (FinPage) -> Unit) {
    val entries = listOf(
        Triple(FinPage.PEOPLE, "People", Icons.Filled.People),
        Triple(FinPage.BUDGETS, "Budgets", Icons.Filled.Category),
        Triple(FinPage.GOALS, "Savings Goals", Icons.Filled.Savings),
        Triple(FinPage.LOANS, "Loans & Repayments", Icons.Filled.AccountBalance),
        Triple(FinPage.CATEGORIES, "Categories", Icons.Filled.Category),
        Triple(FinPage.SETTINGS, "Settings & Appearance", Icons.Filled.Settings)
    )
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { GlassCard { Text("Manage your finances", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("All records stay on this device in the Offline Edition.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        items(entries) { item ->
            val destination = item.first
            GlassCard(Modifier.fillMaxWidth().clickable { onNavigate(destination) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(item.third, null, tint = Color(0xFF21DACA), modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(item.second, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

@Composable
private fun PeopleScreen(
    people: List<PersonEntity>,
    transactions: List<TransactionEntity>,
    onDelete: (PersonEntity) -> Unit,
    onEdit: (PersonEntity) -> Unit
) {
    if (people.isEmpty()) EmptyState("No people added", "Add people to track money borrowed from or lent to each person.")
    else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(people, key = { it.stableUid }) { person ->
            val rows = transactions.filter { it.personId == person.id }
            val borrowed = rows.filter { it.type.equals("BORROW", true) }.sumOf { it.amountPaise }
            val lent = rows.filter { it.type.equals("LEND", true) }.sumOf { it.amountPaise }
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(person.name, fontWeight = FontWeight.Bold); if (person.phone.isNotBlank()) Text(person.phone, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = { onEdit(person) }) { Text("Edit") }
                    TextButton(onClick = { onDelete(person) }) { Text("Delete") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Borrowed " + money(borrowed), color = Color(0xFFB59BFF), style = MaterialTheme.typography.bodySmall)
                    Text("Lent " + money(lent), color = Color(0xFFFFBC72), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun BudgetsScreen(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    onDelete: (BudgetEntity) -> Unit,
    onEdit: (BudgetEntity) -> Unit
) {
    if (budgets.isEmpty()) EmptyState("No budgets created", "Set category spending limits and monitor progress.")
    else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(budgets, key = { it.stableUid }) { budget ->
            val range = budgetWindow(budget)
            val spent = NativeLedgerCalculator.categoryExpensePaise(
                transactions, budget.category, range.first, range.second
            )
            val ratio = if (budget.limitAmountPaise <= 0) 0f else (spent.toDouble() / budget.limitAmountPaise).toFloat().coerceIn(0f, 1.2f)
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(budget.category, fontWeight = FontWeight.Bold); Text(budget.month, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
                    Row {
                        TextButton(onClick = { onEdit(budget) }) { Text("Edit") }
                        TextButton(onClick = { onDelete(budget) }) { Text("Delete") }
                    }
                }
                Text(money(spent) + " of " + money(budget.limitAmountPaise))
                LinearProgressIndicator(progress = { ratio.coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth(), color = if (ratio >= 1f) Color(0xFFFF6779) else Color(0xFF20D7BE))
                Text(
                    if (spent > budget.limitAmountPaise) "Over budget by " + money(spent - budget.limitAmountPaise)
                    else "Remaining " + money(budget.limitAmountPaise - spent),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun GoalsScreen(
    goals: List<GoalEntity>,
    onContribute: (GoalEntity) -> Unit,
    onDelete: (GoalEntity) -> Unit,
    onEdit: (GoalEntity) -> Unit,
    viewModel: NativeFinTrackViewModel
) {
    if (goals.isEmpty()) EmptyState("No savings goals", "Create a target and record contributions as separate tracking entries.")
    else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(goals, key = { it.stableUid }) { goal ->
            val ratio = if (goal.targetAmountPaise <= 0) 0f else (goal.savedAmountPaise.toDouble() / goal.targetAmountPaise).toFloat().coerceIn(0f, 1f)
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(goal.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Row {
                        TextButton(onClick = { onEdit(goal) }) { Text("Edit") }
                        TextButton(onClick = { onDelete(goal) }) { Text("Delete") }
                    }
                }
                Text(money(goal.savedAmountPaise) + " saved of " + money(goal.targetAmountPaise))
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF19D9C8))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Remaining " + money((goal.targetAmountPaise - goal.savedAmountPaise).coerceAtLeast(0)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onContribute(goal) }) { Text("Add contribution") }
                }
                val contributions by viewModel.goalContributions(goal.id).collectAsStateWithLifecycle(initialValue = emptyList())
                Text("Contribution tracking does not change account balances.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (contributions.isNotEmpty()) {
                    Text("Contribution history", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    contributions.take(3).forEach { entry ->
                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.timestamp)) + " • " + money(entry.amountPaise), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoansScreen(
    loans: List<LoanEntity>,
    onRepay: (LoanEntity) -> Unit,
    onDelete: (LoanEntity) -> Unit,
    onEdit: (LoanEntity) -> Unit,
    viewModel: NativeFinTrackViewModel
) {
    if (loans.isEmpty()) EmptyState("No loan records", "Add a loan and record partial or full repayments.")
    else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(loans, key = { it.stableUid }) { loan ->
            val remaining = (loan.principalPaise - loan.paidAmountPaise).coerceAtLeast(0L)
            val ratio = if (loan.principalPaise <= 0) 0f else (loan.paidAmountPaise.toDouble() / loan.principalPaise).toFloat().coerceIn(0f, 1f)
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(loan.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Row {
                        TextButton(onClick = { onEdit(loan) }) { Text("Edit") }
                        TextButton(onClick = { onDelete(loan) }) { Text("Delete") }
                    }
                }
                Text("Outstanding " + money(remaining), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Principal " + money(loan.principalPaise) + " • Paid " + money(loan.paidAmountPaise), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF25D8C5))
                TextButton(onClick = { onRepay(loan) }) { Text("Record repayment") }
                val payments by viewModel.loanPayments(loan.id).collectAsStateWithLifecycle(initialValue = emptyList())
                Text("Repayment tracking does not change account balances.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (payments.isNotEmpty()) {
                    Text("Repayment history", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    payments.take(3).forEach { entry ->
                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.timestamp)) + " • " + money(entry.amountPaise), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onDelete: (CategoryEntity) -> Unit,
    onEdit: (CategoryEntity) -> Unit
) {
    if (categories.isEmpty()) EmptyState("No categories available", "Create income and expense categories for transaction classification.")
    else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories, key = { it.stableUid }) { category ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.icon.ifBlank { "•" }, color = Color(0xFF23D7CE), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.SemiBold)
                        Text(category.type, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    if (!category.isSystem) {
                        TextButton(onClick = { onEdit(category) }) { Text("Edit") }
                        TextButton(onClick = { onDelete(category) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportPdf: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassCard {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (darkTheme) "Dark Liquid Glass theme" else "Light theme")
                Switch(checked = darkTheme, onCheckedChange = onThemeChange)
            }
        }
        GlassCard {
            Text("Local backup and restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "JSON backup includes accounts, transactions, people, categories, budgets, savings goals, contributions, loans and repayment history. Restore merges by stable record ID and does not replace existing data.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) { Text("Export full JSON backup") }
            OutlinedButton(onClick = onImportBackup, modifier = Modifier.fillMaxWidth()) { Text("Restore / merge JSON backup") }
        }
        GlassCard {
            Text("Export reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) { Text("Export transactions CSV") }
            OutlinedButton(onClick = onExportXlsx, modifier = Modifier.fillMaxWidth()) { Text("Export Excel workbook") }
            OutlinedButton(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) { Text("Export PDF statement") }
        }
        GlassCard {
            Text("Offline privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("This edition has no sign-in, OTP, cloud sync or cloud backup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Files are created only after you choose a local save destination.", style = MaterialTheme.typography.bodySmall)
        }
        GlassCard {
            Text("App security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("No app lock is enabled in this release, as requested.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeEditorDialog(
    kind: EditorKind,
    accounts: List<AccountEntity>,
    selectedGoal: GoalEntity?,
    selectedLoan: LoanEntity?,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    var name by remember(kind) { mutableStateOf(selectedGoal?.name ?: selectedLoan?.name.orEmpty()) }
    var amount by remember(kind) { mutableStateOf("") }
    var type by remember(kind) { mutableStateOf(if (kind == EditorKind.CATEGORY) "EXPENSE" else "INCOME") }
    var category by remember(kind) { mutableStateOf("Other") }
    var note by remember(kind) { mutableStateOf("") }
    var phone by remember(kind) { mutableStateOf("") }
    var accountId by remember(kind) { mutableStateOf(accounts.firstOrNull()?.id?.toString().orEmpty()) }
    var toAccountId by remember(kind) { mutableStateOf(accounts.getOrNull(1)?.id?.toString().orEmpty()) }
    var period by remember(kind) { mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.US).format(Date())) }
    var monthly by remember(kind) { mutableStateOf("") }
    var interest by remember(kind) { mutableStateOf("0") }
    var typeExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    val title = when (kind) {
        EditorKind.TRANSACTION -> "Add Transaction"
        EditorKind.ACCOUNT -> "Add Account or Wallet"
        EditorKind.PERSON -> "Add Person"
        EditorKind.BUDGET -> "Create Budget"
        EditorKind.GOAL -> "Create Savings Goal"
        EditorKind.LOAN -> "Create Loan"
        EditorKind.CONTRIBUTION -> "Add Goal Contribution"
        EditorKind.REPAYMENT -> "Record Loan Repayment"
        EditorKind.CATEGORY -> "Add Category"
        EditorKind.NONE -> ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                when (kind) {
                    EditorKind.TRANSACTION -> {
                        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                            OutlinedTextField(type, {}, readOnly = true, label = { Text("Transaction type") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                listOf("INCOME", "EXPENSE", "BORROW", "LEND", "TRANSFER").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeExpanded = false })
                                }
                            }
                        }
                        MoneyField("Amount (₹)", amount) { amount = it }
                        ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = it }) {
                            OutlinedTextField(
                                accounts.firstOrNull { it.id.toString() == accountId }?.name ?: "Select account",
                                {}, readOnly = true, label = { Text(if (type == "TRANSFER") "From account" else "Account") },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                                accounts.forEach { account -> DropdownMenuItem(text = { Text(account.name) }, onClick = { accountId = account.id.toString(); accountExpanded = false }) }
                            }
                        }
                        if (type == "TRANSFER") {
                            ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = it }) {
                                OutlinedTextField(
                                    accounts.firstOrNull { it.id.toString() == toAccountId }?.name ?: "Select destination",
                                    {}, readOnly = true, label = { Text("To account") }, modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                                    accounts.filter { it.id.toString() != accountId }.forEach { account ->
                                        DropdownMenuItem(text = { Text(account.name) }, onClick = { toAccountId = account.id.toString(); toExpanded = false })
                                    }
                                }
                            }
                        }
                        if (type != "TRANSFER") OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(note, { note = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    }
                    EditorKind.ACCOUNT -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Account name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(type, { type = it }, label = { Text("Account type (Cash / Bank / Wallet)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        MoneyField("Opening balance (₹)", amount) { amount = it }
                    }
                    EditorKind.PERSON -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Person name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(phone, { phone = it }, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(note, { note = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    }
                    EditorKind.CATEGORY -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Category name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                            OutlinedTextField(type, {}, readOnly = true, label = { Text("Type") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                listOf("INCOME", "EXPENSE").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeExpanded = false }) }
                            }
                        }
                    }
                    EditorKind.BUDGET -> {
                        OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                        MoneyField("Budget limit (₹)", amount) { amount = it }
                        OutlinedTextField(period, { period = it }, label = { Text("Month (YYYY-MM)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    EditorKind.GOAL -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Goal name") }, modifier = Modifier.fillMaxWidth())
                        MoneyField("Target amount (₹)", amount) { amount = it }
                    }
                    EditorKind.LOAN -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Loan name") }, modifier = Modifier.fillMaxWidth())
                        MoneyField("Principal (₹)", amount) { amount = it }
                        MoneyField("Monthly payment (₹)", monthly) { monthly = it }
                        OutlinedTextField(interest, { interest = it }, label = { Text("Annual interest rate (%)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                    EditorKind.CONTRIBUTION -> {
                        Text(selectedGoal?.name.orEmpty(), fontWeight = FontWeight.SemiBold)
                        MoneyField("Contribution amount (₹)", amount) { amount = it }
                        Text("Tracking only. Account balances are not changed.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    EditorKind.REPAYMENT -> {
                        Text(selectedLoan?.name.orEmpty(), fontWeight = FontWeight.SemiBold)
                        MoneyField("Repayment amount (₹)", amount) { amount = it }
                        OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                        Text("Tracking only. Record a corresponding account transaction separately.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    EditorKind.NONE -> Unit
                }
            }
        },
        confirmButton = { Button(onClick = {
            onSave(mapOf(
                "name" to name, "amount" to amount, "type" to type, "category" to category,
                "note" to note, "phone" to phone, "accountId" to accountId,
                "toAccountId" to toAccountId, "period" to period, "monthly" to monthly,
                "interest" to interest
            ))
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MoneyField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value, onValue, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    onDelete: ((TransactionEntity) -> Unit)?,
    onEdit: ((TransactionEntity) -> Unit)? = null
) {
    val positive = transaction.type in setOf("INCOME", "BORROW", "TRANSFER_IN")
    val tint = if (positive) Color(0xFF18D6A4) else Color(0xFFFF7180)
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Icon(if (positive) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward, null, tint = tint)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.category.ifBlank { transaction.type }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(transaction.type + " • " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.note.isNotBlank()) Text(transaction.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(money(transaction.amountPaise), color = tint, fontWeight = FontWeight.Bold)
            if (onEdit != null) TextButton(onClick = { onEdit(transaction) }) { Text("Edit") }
            if (onDelete != null) IconButton(onClick = { onDelete(transaction) }) {
                Icon(Icons.Filled.Delete, "Delete transaction", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String, tint: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color(0x6620CED1))
            .border(1.dp, tint.copy(alpha = .45f), RoundedCornerShape(18.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = tint)
        Text(value, fontWeight = FontWeight.ExtraBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SmallMetric(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color(0x99213F54))
            .border(1.dp, tint.copy(alpha = .45f), RoundedCornerShape(18.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xA8173C4E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x554ED8E8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, Modifier.padding(top = 18.dp, bottom = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun EmptyState(title: String, description: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        GlassCard {
            Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color(0xFF18DCCB), modifier = Modifier.size(34.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun budgetWindow(budget: BudgetEntity): Pair<Long, Long> {
    val now = Calendar.getInstance()
    val start = Calendar.getInstance()
    val end = Calendar.getInstance()
    when (budget.period.lowercase(Locale.ROOT)) {
        "daily" -> {
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            end.timeInMillis = start.timeInMillis
            end.add(Calendar.DAY_OF_YEAR, 1)
        }
        "weekly" -> {
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            val day = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7
            start.add(Calendar.DAY_OF_YEAR, -day)
            end.timeInMillis = start.timeInMillis
            end.add(Calendar.DAY_OF_YEAR, 7)
        }
        "yearly" -> {
            start.set(Calendar.DAY_OF_YEAR, 1)
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            end.timeInMillis = start.timeInMillis
            end.add(Calendar.YEAR, 1)
        }
        else -> {
            val parts = budget.month.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.YEAR)
            val month = (parts.getOrNull(1)?.toIntOrNull()?.minus(1))?.coerceIn(0, 11) ?: now.get(Calendar.MONTH)
            start.set(year, month, 1, 0, 0, 0)
            start.set(Calendar.MILLISECOND, 0)
            end.timeInMillis = start.timeInMillis
            end.add(Calendar.MONTH, 1)
        }
    }
    return start.timeInMillis to end.timeInMillis
}

private fun money(paise: Long): String = MoneyPaise.fromSignedPaise(paise).formatInr()
private fun greetingText(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning 👋"
        hour < 17 -> "Good Afternoon 👋"
        else -> "Good Evening 👋"
    }
}
