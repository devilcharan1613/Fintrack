package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.AccountAllocation
import com.tracemeo.app.data.CashCountLine
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.local.AccountEntity

@Composable
fun AccountEditDialog(
    initialAccount: AccountEntity? = null,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onSave: (name: String, accountType: String, balance: Double, creditLimit: Double, statementDay: Int, dueDay: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var accountType by remember { mutableStateOf(initialAccount?.accountType ?: "Bank") }
    var balanceText by remember {
        val initBal = initialAccount?.let { if (it.balance != 0.0) it.balance else MoneyPaise.toRupees(it.balancePaise) } ?: 0.0
        mutableStateOf(if (initBal == 0.0 && initialAccount == null) "" else initBal.toString())
    }
    var creditLimitText by remember { mutableStateOf((initialAccount?.creditLimit ?: 50000.0).toString()) }
    var statementDayText by remember { mutableStateOf((initialAccount?.statementDay ?: 5).toString()) }
    var dueDayText by remember { mutableStateOf((initialAccount?.dueDay ?: 25).toString()) }

    val types = listOf("Bank", "Wallet", "Cash", "Savings", "Credit Card", "UPI Lite")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Text(
                if (initialAccount == null) "➕ Add Account / Wallet" else "✏️ Edit ${initialAccount.name}",
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account / Wallet Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Account Type", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.take(3).forEach { t ->
                        val selected = accountType.equals(t, true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) palette.primary.copy(alpha = 0.22f) else palette.surface)
                                .border(1.dp, if (selected) palette.primary else palette.surfaceBorder, RoundedCornerShape(10.dp))
                                .clickable { accountType = t }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t, style = MaterialTheme.typography.labelSmall, color = if (selected) palette.primary else palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.drop(3).forEach { t ->
                        val selected = accountType.equals(t, true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) palette.primary.copy(alpha = 0.22f) else palette.surface)
                                .border(1.dp, if (selected) palette.primary else palette.surfaceBorder, RoundedCornerShape(10.dp))
                                .clickable { accountType = t }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t, style = MaterialTheme.typography.labelSmall, color = if (selected) palette.primary else palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text(if (accountType == "Credit Card") "Current Used Balance (₹)" else "Opening Balance (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (accountType.equals("Credit Card", true)) {
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = { creditLimitText = it },
                        label = { Text("Credit Card Total Limit (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = statementDayText,
                            onValueChange = { statementDayText = it },
                            label = { Text("Stmt Day (1-28)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dueDayText,
                            onValueChange = { dueDayText = it },
                            label = { Text("Due Day (1-28)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim().ifBlank { "My $accountType" }
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    val limit = creditLimitText.toDoubleOrNull() ?: 0.0
                    val sDay = (statementDayText.toIntOrNull() ?: 5).coerceIn(1, 28)
                    val dDay = (dueDayText.toIntOrNull() ?: 25).coerceIn(1, 28)
                    onSave(cleanName, accountType, bal, limit, sDay, dDay)
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
            ) {
                Text("Save Account", color = palette.background, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun AccountAllocationDialog(
    account: AccountEntity,
    palette: ThemePalette,
    currencySymbol: String,
    liveBalance: Double? = null,
    onDismiss: () -> Unit,
    onSaveAllocations: (List<AccountAllocation>) -> Unit
) {
    val existing = remember(account.allocationsJson) {
        NativeLedgerCalculator.parseAllocations(account.allocationsJson)
    }
    var allocations by remember { mutableStateOf(existing) }
    var bucketName by remember { mutableStateOf("") }
    var bucketAmount by remember { mutableStateOf("") }
    var autoSplitPct by remember { mutableStateOf("") }

    val accBal = liveBalance ?: (if (account.balance != 0.0) account.balance else MoneyPaise.toRupees(account.balancePaise))
    val totalAllocated = allocations.sumOf { it.allocatedAmount }
    val unallocated = (accBal - totalAllocated).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Column {
                Text("📂 ${account.name} Allocations", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                Text(
                    "Allocated: $currencySymbol${"%,.0f".format(totalAllocated)} · Unallocated: $currencySymbol${"%,.0f".format(unallocated)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                allocations.forEach { alloc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surface)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(alloc.category, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(
                                "$currencySymbol${"%,.0f".format(alloc.allocatedAmount)} · Auto-Split ${alloc.percentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                        TextButton(onClick = { allocations = allocations.filterNot { it.id == alloc.id } }) {
                            Text("✕", color = palette.secondary)
                        }
                    }
                }
                OutlinedTextField(
                    value = bucketName,
                    onValueChange = { bucketName = it },
                    label = { Text("Bucket Name (e.g. Personal, Bills)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bucketAmount,
                        onValueChange = { bucketAmount = it },
                        label = { Text("Amount ($currencySymbol)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = autoSplitPct,
                        onValueChange = { autoSplitPct = it },
                        label = { Text("Auto-Split %") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = {
                        val amt = (bucketAmount.toDoubleOrNull() ?: 0.0).coerceAtMost(unallocated)
                        val pct = (autoSplitPct.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
                        if (bucketName.isNotBlank()) {
                            allocations = allocations + AccountAllocation(
                                id = "alloc_${System.currentTimeMillis()}",
                                category = bucketName.trim(),
                                allocatedAmount = amt,
                                percentage = pct
                            )
                            bucketName = ""
                            bucketAmount = ""
                            autoSplitPct = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Set Allocation Bucket", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveAllocations(allocations) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
            ) {
                Text("Save Buckets", color = palette.background, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = palette.textSecondary) }
        }
    )
}

@Composable
fun CashCountDenominationDialog(
    account: AccountEntity,
    palette: ThemePalette,
    currencySymbol: String,
    availableAccounts: List<AccountEntity> = emptyList(),
    onSelectAccount: ((AccountEntity) -> Unit)? = null,
    onDismiss: () -> Unit,
    onApplyReconcile: (countedTotal: Double, note: String, lines: List<CashCountLine>) -> Unit
) {
    val notes = listOf(500, 200, 100, 50, 20, 10)
    val coins = listOf(20, 10, 5, 2, 1)

    var selectedType by remember { mutableStateOf("NOTE") }
    var selectedDenom by remember { mutableIntStateOf(500) }
    var quantity by remember { mutableIntStateOf(1) }
    var addedLines by remember { mutableStateOf(listOf<CashCountLine>()) }
    var noteText by remember { mutableStateOf("") }

    val countedTotal = remember(addedLines) {
        NativeLedgerCalculator.computeCashDenominationTotal(addedLines)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Column {
                Text("💵 3-Step Cash Counter (${account.name})", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                Text("Counted Total: $currencySymbol${"%,.0f".format(countedTotal)}", style = MaterialTheme.typography.titleSmall, color = palette.primary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (availableAccounts.size > 1 && onSelectAccount != null) {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableAccounts.forEach { acc ->
                            val sel = acc.id == account.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) palette.primary else palette.surface)
                                    .clickable { onSelectAccount(acc) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(acc.name, style = MaterialTheme.typography.labelSmall, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Text("Step 1: Pick Note or Coin", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NOTE" to "💵 Notes (₹500–₹10)", "COIN" to "🪙 Coins (₹20–₹1)").forEach { (type, label) ->
                        val sel = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) palette.primary.copy(alpha = 0.22f) else palette.surface)
                                .border(1.dp, if (sel) palette.primary else palette.surfaceBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedType = type
                                    selectedDenom = if (type == "NOTE") 500 else 20
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) palette.primary else palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                val denomOptions = if (selectedType == "NOTE") notes else coins
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    denomOptions.forEach { d ->
                        val sel = selectedDenom == d
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) palette.primary else palette.surface)
                                .clickable { selectedDenom = d }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("₹$d", style = MaterialTheme.typography.labelSmall, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("Step 2: Quantity Stepper", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { if (quantity > 1) quantity-- },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("−", color = palette.textPrimary, fontWeight = FontWeight.Bold) }
                        Text("$quantity × ₹$selectedDenom = ₹${quantity * selectedDenom}", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Button(
                            onClick = { quantity++ },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("+", color = palette.textPrimary, fontWeight = FontWeight.Bold) }
                    }
                    Button(
                        onClick = {
                            addedLines = addedLines + CashCountLine(selectedType, selectedDenom, quantity)
                            quantity = 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                    ) {
                        Text("+ Add", color = palette.background, fontWeight = FontWeight.Bold)
                    }
                }

                if (addedLines.isNotEmpty()) {
                    Text("Step 3: Added Denominations", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    addedLines.forEachIndexed { idx, line ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(palette.surface).padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${line.denomType}: ₹${line.value} × ${line.count} = ₹${line.value * line.count}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                            Text("✕", color = palette.secondary, modifier = Modifier.clickable {
                                addedLines = addedLines.filterIndexed { i, _ -> i != idx }
                            })
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Optional Cash Count Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyReconcile(countedTotal, noteText.ifBlank { "Cash Denomination Count" }, addedLines) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
            ) {
                Text("Apply & Reconcile Cash", color = palette.background, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textSecondary) }
        }
    )
}
