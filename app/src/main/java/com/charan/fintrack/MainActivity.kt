package com.charan.fintrack

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF071521)
private val Panel = Color(0xFF102535)
private val Green = Color(0xFF19D99A)
private val Muted = Color(0xFF9CB2C1)
private val White = Color(0xFFF4FAFC)
private val Red = Color(0xFFFF6B75)

class FinDatabase(context: Context) : SQLiteOpenHelper(context, "fintrack.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE accounts(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, opening REAL NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT, accountId INTEGER NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, note TEXT, date INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE budgets(id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT NOT NULL, amount REAL NOT NULL, month TEXT NOT NULL)")
        db.execSQL("CREATE TABLE goals(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, target REAL NOT NULL, saved REAL NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE loans(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, principal REAL NOT NULL, emi REAL NOT NULL, paid REAL NOT NULL DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    private fun insert(table: String, values: ContentValues) {
        writableDatabase.insertOrThrow(table, null, values)
    }

    fun addAccount(name: String, type: String, opening: Double) = insert("accounts", ContentValues().apply {
        put("name", name); put("type", type); put("opening", opening)
    })

    fun addTransaction(accountId: Long, type: String, amount: Double, category: String, note: String) {
        insert("transactions", ContentValues().apply {
            put("accountId", accountId); put("type", type); put("amount", amount)
            put("category", category); put("note", note); put("date", System.currentTimeMillis())
        })
    }

    fun addBudget(category: String, amount: Double) = insert("budgets", ContentValues().apply {
        put("category", category); put("amount", amount)
        put("month", SimpleDateFormat("yyyy-MM", Locale.US).format(Date()))
    })

    fun addGoal(name: String, target: Double) = insert("goals", ContentValues().apply {
        put("name", name); put("target", target)
    })

    fun addLoan(name: String, principal: Double) = insert("loans", ContentValues().apply {
        put("name", name); put("principal", principal); put("emi", 0.0)
    })

    fun rows(table: String): List<Map<String, String>> {
        require(table in setOf("accounts", "transactions", "budgets", "goals", "loans"))
        return buildList {
            readableDatabase.rawQuery("SELECT * FROM $table ORDER BY id DESC", null).use { cursor ->
                while (cursor.moveToNext()) add(buildMap {
                    for (index in 0 until cursor.columnCount) {
                        put(cursor.getColumnName(index), cursor.getString(index).orEmpty())
                    }
                })
            }
        }
    }

    fun balance(accountId: Long): Double {
        var result = 0.0
        readableDatabase.rawQuery("SELECT opening FROM accounts WHERE id=?", arrayOf(accountId.toString())).use {
            if (it.moveToFirst()) result = it.getDouble(0)
        }
        readableDatabase.rawQuery("SELECT type, amount FROM transactions WHERE accountId=?", arrayOf(accountId.toString())).use {
            while (it.moveToNext()) result += if (it.getString(0) == "Income") it.getDouble(1) else -it.getDouble(1)
        }
        return result
    }

    fun totalBalance() = rows("accounts").sumOf { balance(it["id"]?.toLongOrNull() ?: 0) }

    fun totals(): Pair<Double, Double> {
        var income = 0.0
        var expense = 0.0
        readableDatabase.rawQuery("SELECT type, SUM(amount) FROM transactions GROUP BY type", null).use {
            while (it.moveToNext()) {
                if (it.getString(0) == "Income") income = it.getDouble(1)
                if (it.getString(0) == "Expense") expense = it.getDouble(1)
            }
        }
        return income to expense
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { FinTrackApp(FinDatabase(this)) }
    }
}

private enum class Page(val title: String) { Home("Home"), Accounts("Accounts"), Activity("Activity"), Reports("Reports"), More("More") }

@Composable
private fun FinTrackApp(db: FinDatabase) {
    var page by remember { mutableStateOf(Page.Home) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Navy, surface = Panel, onSurface = White)) {
        Scaffold(
            containerColor = Navy,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    Page.entries.forEach { item ->
                        val icon = when (item) {
                            Page.Home -> Icons.Default.Home
                            Page.Accounts -> Icons.Default.AccountBalanceWallet
                            Page.Activity -> Icons.Default.ReceiptLong
                            Page.Reports -> Icons.Default.BarChart
                            Page.More -> Icons.Default.MoreHoriz
                        }
                        NavigationBarItem(page == item, { page = item }, { Icon(icon, item.title) }, label = { Text(item.title, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green,
                                indicatorColor = Navy, unselectedIconColor = Muted, unselectedTextColor = Muted))
                    }
                }
            },
            floatingActionButton = { FloatingActionButton({ dialog = "Transaction" }, containerColor = Green, contentColor = Navy) { Icon(Icons.Default.Add, "Add transaction") } }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                when (page) {
                    Page.Home -> HomeScreen(db, refresh) { dialog = it }
                    Page.Accounts -> AccountsScreen(db, refresh) { dialog = it }
                    Page.Activity -> ActivityScreen(db, refresh)
                    Page.Reports -> ReportsScreen(db, refresh)
                    Page.More -> MoreScreen { dialog = it }
                }
            }
        }
        dialog?.let { type ->
            EntryDialog(type, db) { dialog = null; refresh++ }
        }
    }
}

private fun money(value: Double) = "₹" + String.format(Locale("en", "IN"), "%,.2f", value)

@Composable
private fun Header(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun Card(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(20.dp)).padding(18.dp)) {
        Text(title, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text(value, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeScreen(db: FinDatabase, refresh: Int, add: (String) -> Unit) {
    Header("FinTrack", "Your money, clearly managed")
    val balance = remember(refresh) { db.totalBalance() }
    val totals = remember(refresh) { db.totals() }
    val transactions = remember(refresh) { db.rows("transactions") }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF123D3B), Panel)), RoundedCornerShape(24.dp)).padding(22.dp)) {
                Text("TOTAL BALANCE", color = Muted, fontSize = 12.sp)
                Text(money(balance), color = Green, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Across your active accounts", color = Muted, fontSize = 12.sp)
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Card("Income", money(totals.first), Modifier.weight(1f)); Card("Expenses", money(totals.second), Modifier.weight(1f)) } }
        item { Text("Quick actions", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickButton("Income", Icons.Default.AddCircle, Modifier.weight(1f)) { add("Transaction") }
                QuickButton("Expense", Icons.Default.RemoveCircle, Modifier.weight(1f)) { add("Transaction") }
                QuickButton("Account", Icons.Default.AccountBalance, Modifier.weight(1f)) { add("Account") }
            }
        }
        item { Text("Recent transactions", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        items(transactions.take(5)) { TransactionRow(it) }
        if (transactions.isEmpty()) item { EmptyState("No transactions yet", "Tap + to add your first transaction.") }
    }
}

@Composable
private fun QuickButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.background(Panel, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = Green); Spacer(Modifier.height(6.dp)); Text(label, color = White, fontSize = 11.sp)
    }
}

@Composable
private fun AccountsScreen(db: FinDatabase, refresh: Int, add: (String) -> Unit) {
    Header("Accounts", "Manage your money")
    val accounts = remember(refresh) { db.rows("accounts") }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Card("Total account balance", money(db.totalBalance()), Modifier.fillMaxWidth()) }
        item { Button({ add("Account") }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("+ Add account", color = Navy) } }
        items(accounts) { account ->
            val id = account["id"]?.toLongOrNull() ?: 0
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(18.dp)) {
                Text("${account["name"]}  (ID $id)", color = White, fontWeight = FontWeight.Bold)
                Text(account["type"].orEmpty(), color = Muted, fontSize = 12.sp)
                Text(money(db.balance(id)), color = Green, fontSize = 20.sp)
            }
        }
        if (accounts.isEmpty()) item { EmptyState("No accounts", "Add your bank, cash or wallet account.") }
    }
}

@Composable
private fun ActivityScreen(db: FinDatabase, refresh: Int) {
    Header("Transactions", "Your financial activity")
    val transactions = remember(refresh) { db.rows("transactions") }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(transactions) { TransactionRow(it) }
        if (transactions.isEmpty()) item { EmptyState("No transactions", "Your transactions will appear here.") }
    }
}

@Composable
private fun TransactionRow(row: Map<String, String>) {
    val income = row["type"] == "Income"
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(15.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (income) Icons.Default.TrendingUp else Icons.Default.TrendingDown, null, tint = if (income) Green else Red)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(row["category"].orEmpty(), color = White, fontWeight = FontWeight.Medium); Text(row["note"].orEmpty(), color = Muted, fontSize = 12.sp) }
        Text((if (income) "+" else "-") + money(row["amount"]?.toDoubleOrNull() ?: 0.0), color = if (income) Green else Red, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReportsScreen(db: FinDatabase, refresh: Int) {
    Header("Reports", "Understand your finances")
    val totals = remember(refresh) { db.totals() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Card("Total income", money(totals.first), Modifier.fillMaxWidth()) }
        item { Card("Total expenses", money(totals.second), Modifier.fillMaxWidth()) }
        item { Card("Net cash flow", money(totals.first - totals.second), Modifier.fillMaxWidth()) }
        item { Card("Current balance", money(db.totalBalance()), Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun MoreScreen(add: (String) -> Unit) {
    Header("More", "Planning and settings")
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("Budgets" to "Set monthly category limits", "Savings goals" to "Track your savings targets", "Loans" to "Track outstanding loans and EMI", "Backup" to "Export transaction CSV").forEach { (name, description) ->
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Panel, RoundedCornerShape(16.dp))
                        .clickable { add(if (name == "Budgets") "Budget" else if (name == "Savings goals") "Goal" else if (name == "Loans") "Loan" else "Export") }
                        .padding(18.dp)
                ) {
                    Text(name, color = White, fontWeight = FontWeight.Bold); Text(description, color = Muted, fontSize = 12.sp)
                }
            }
        }
        item { Text("FinTrack • Offline personal finance tracker", color = Muted, fontSize = 12.sp) }
    }
}

@Composable
private fun EntryDialog(type: String, db: FinDatabase, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var note by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Expense") }
    var error by remember { mutableStateOf("") }
    if (type == "Export") {
        AlertDialog(onDismissRequest = onClose, title = { Text("CSV export") }, text = { Text("Export is available from the local database.") }, confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
        return
    }
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add $type", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            if (type == "Transaction") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Expense", "Income").forEach { value -> FilterChip(selectedType == value, { selectedType = value }, label = { Text(value) }) } }
                Field(accountId, { accountId = it }, "Account ID")
                Field(category, { category = it }, "Category")
                Field(note, { note = it }, "Note")
            }
            if (type == "Account") { Field(name, { name = it }, "Account name"); Field(category, { category = it }, "Account type") }
            if (type == "Budget") Field(category, { category = it }, "Category")
            if (type == "Goal" || type == "Loan") Field(name, { name = it }, "Name")
            Field(amount, { amount = it }, if (type == "Goal") "Target amount" else "Amount (₹)")
            if (error.isNotEmpty()) Text(error, color = Red)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClose) { Text("Cancel") }
                Button(onClick = {
                    val value = amount.toDoubleOrNull()
                    when (type) {
                        "Account" -> if (name.isBlank() || value == null || value < 0) error = "Enter a name and valid opening balance" else { db.addAccount(name, category, value); onClose() }
                        "Transaction" -> if (accountId.toLongOrNull() == null || value == null || value <= 0 || category.isBlank()) error = "Enter a valid account ID, amount and category" else { db.addTransaction(accountId.toLong(), selectedType, value, category, note); onClose() }
                        "Budget" -> if (category.isBlank() || value == null || value <= 0) error = "Enter a category and valid budget" else { db.addBudget(category, value); onClose() }
                        "Goal" -> if (name.isBlank() || value == null || value <= 0) error = "Enter a name and target" else { db.addGoal(name, value); onClose() }
                        "Loan" -> if (name.isBlank() || value == null || value <= 0) error = "Enter a name and principal" else { db.addLoan(name, value); onClose() }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Save", color = Navy) }
            }
        }
    }
}

@Composable
private fun Field(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AccountBalanceWallet, null, tint = Muted, modifier = Modifier.size(35.dp))
        Spacer(Modifier.height(8.dp)); Text(title, color = White, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}
