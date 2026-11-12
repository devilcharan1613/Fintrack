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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.TransactionEntity
import java.util.Calendar

@Composable
fun DashboardScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    onTogglePrivacy: () -> Unit,
    onOpenAddTransaction: (String) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onViewReceipt: (TransactionEntity) -> Unit,
    onOpenCashCounter: () -> Unit,
    onOpenTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Centered App Logo & Balanced Header (Requirement #6)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        palette.primary.copy(alpha = 0.8f),
                                        palette.secondary.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.5.dp, palette.primary.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💎",
                            fontSize = 28.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "TRACMIO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )

                    Text(
                        text = "100% Private • Local Offline Vault",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Hero Balance Card with Privacy Toggle
        item {
            val balanceDisplay = if (settings.privacyMaskEnabled) {
                "••••••••"
            } else {
                MoneyPaise.formatCurrency(state.totalBalance)
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_balance_card"),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = palette.surface.copy(alpha = 0.85f),
                borderColor = palette.primary.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NET LIQUID WORTH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondary,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = onTogglePrivacy,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (settings.privacyMaskEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle privacy",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = balanceDisplay,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Income vs Expense summary pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x2210B981))
                                .border(1.dp, Color(0x3310B981), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Income",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (settings.privacyMaskEnabled) "••••" else MoneyPaise.formatCurrency(state.totalIncome),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x22F43F5E))
                                .border(1.dp, Color(0x33F43F5E), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Expenses",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFECDD3)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (settings.privacyMaskEnabled) "••••" else MoneyPaise.formatCurrency(state.totalExpense),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF43F5E)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Safe-To-Spend Daily Allowance Pill
        item {
            val calendar = Calendar.getInstance()
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)

            val netRemaining = (state.totalIncome - state.totalExpense).coerceAtLeast(0.0)
            val dailyAllowance = netRemaining / remainingDays

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0x991E293B),
                borderColor = palette.secondary.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛡️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Safe-to-Spend Daily Allowance",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "$remainingDays days remaining in cycle",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Text(
                        text = if (settings.privacyMaskEnabled) "••••/day" else "${MoneyPaise.formatCurrency(dailyAllowance)}/day",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                }
            }
        }

        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onOpenAddTransaction("INCOME") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_income"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.85f)
                    )
                ) {
                    Text("+ Income", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = { onOpenAddTransaction("EXPENSE") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_expense"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF43F5E).copy(alpha = 0.85f)
                    )
                ) {
                    Text("+ Expense", fontWeight = FontWeight.Bold, color = Color.White)
                }

                IconButton(
                    onClick = onOpenCashCounter,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x331E293B))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Text("💵", fontSize = 20.sp)
                }

                IconButton(
                    onClick = onOpenTransfer,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x331E293B))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Transfer",
                        tint = palette.primary
                    )
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "${state.transactions.size} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        // Recent Transactions Visual Cards (with top-right pencil edit button, Requirement #3)
        if (state.transactions.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x441E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "✨", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions logged yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "Tap + Income or + Expense to add your first record",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.transactions.take(8), key = { it.id }) { tx ->
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

@Composable
fun TransactionVisualCard(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    palette: ThemePalette,
    onEditClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type.equals("EXPENSE", ignoreCase = true)
    val amountColor = if (isExpense) Color(0xFFF43F5E) else Color(0xFF10B981)
    val sign = if (isExpense) "-" else "+"
    val accountName = accounts.firstOrNull { it.id == transaction.accountId }?.name ?: "Wallet"

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_card_${transaction.id}"),
        backgroundColor = Color(0xCC0E1726),
        borderColor = if (isExpense) Color(0x22F43F5E) else Color(0x2210B981),
        onClick = onCardClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(amountColor.copy(alpha = 0.15f))
                    .border(1.dp, amountColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryIcon(transaction.category),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = " • ${transaction.note}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Amount & Dedicated Top-Right Edit Pencil (Requirement #3)
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$sign${MoneyPaise.formatCurrency(transaction.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Dedicated Pencil Edit Button (Requirement #3)
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_tx_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit transaction",
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = transaction.paymentMethod,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

fun getCategoryIcon(category: String): String {
    return when {
        category.contains("Food", true) || category.contains("Dining", true) -> "🍔"
        category.contains("Grocer", true) -> "🛒"
        category.contains("Transport", true) || category.contains("Fuel", true) -> "🚕"
        category.contains("Shop", true) -> "🛍️"
        category.contains("Bill", true) || category.contains("Utility", true) -> "⚡"
        category.contains("Rent", true) || category.contains("House", true) -> "🏠"
        category.contains("Health", true) || category.contains("Med", true) -> "💊"
        category.contains("Entertain", true) || category.contains("Movie", true) -> "🎬"
        category.contains("Salary", true) -> "💼"
        category.contains("Freelance", true) -> "💻"
        category.contains("Invest", true) -> "📈"
        category.contains("Gift", true) -> "🎁"
        else -> "💰"
    }
}

@Composable
fun AccountsScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    onAddAccount: (name: String, type: String, balance: Double, institution: String) -> Unit,
    onOpenCashCounter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Accounts & Wallets",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Manage all bank, cash, and digital balances",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Wallet", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Cash Denomination Quick Banner
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0x331E293B),
                borderColor = Color(0x44F59E0B),
                onClick = onOpenCashCounter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💵", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cash Denomination Counter",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Count ₹500, ₹200, ₹100, ₹50 notes & coins",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = palette.primary
                    )
                }
            }
        }

        items(state.accounts, key = { it.id }) { acc ->
            val icon = when (acc.accountType.uppercase()) {
                "CASH" -> Icons.Default.Payments
                "BANK", "SAVINGS" -> Icons.Default.AccountBalance
                "CREDIT CARD" -> Icons.Default.CreditCard
                "INVESTMENT" -> Icons.Default.Savings
                else -> Icons.Default.AccountBalanceWallet
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC111C2E),
                borderColor = palette.primary.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.primary.copy(alpha = 0.15f))
                                .border(1.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = palette.primary)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = acc.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${acc.accountType} • ${acc.institution.ifBlank { "Local" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Text(
                        text = MoneyPaise.formatCurrency(acc.balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            palette = palette,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, balance, inst ->
                onAddAccount(name, type, balance, inst)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddAccountDialog(
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, balance: Double, institution: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Bank") }
    var balanceText by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }

    val types = listOf("Bank", "Cash", "Credit Card", "Digital Wallet", "Investment")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add New Account / Wallet", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. HDFC Salary, Pocket Cash)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Account Type:", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.take(3).forEach { t ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (type == t) palette.primary else Color(0x331E293B),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { type = t }
                        ) {
                            Text(
                                text = t,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (type == t) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Opening Balance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Bank / Institution Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, type, bal, institution)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
            ) {
                Text("Add Wallet", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textSecondary)
            }
        },
        containerColor = Color(0xFF131D2D)
    )
}
