package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracemeo.app.data.FinancialCalculator
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.NativeTracmioUiState

@Composable
fun PlanningAndPeopleScreens(
    subRoute: String,
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onSelectSubRoute: (String) -> Unit
) {
    val curr = settings.currencySymbol
    val tabs = listOf(
        "people" to "👥 People & Khata",
        "budgets" to "📊 Budgets & Runway",
        "goals" to "🎯 Savings Goals",
        "loans" to "🏦 Loans & Debt Payoff"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { (routeKey, label) ->
                    val sel = subRoute == routeKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) palette.primary else palette.surface)
                            .border(1.dp, if (sel) palette.primary else palette.surfaceBorder, RoundedCornerShape(16.dp))
                            .clickable { onSelectSubRoute(routeKey) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        when (subRoute) {
            "people" -> {
                item {
                    PeopleAndKhataSection(state = state, settings = settings, palette = palette, viewModel = viewModel, hostCallbacks = hostCallbacks)
                }
            }
            "budgets" -> {
                item {
                    BudgetsAndRunwaySection(state = state, settings = settings, palette = palette, viewModel = viewModel)
                }
            }
            "goals" -> {
                item {
                    GoalsAndCoolingSection(state = state, settings = settings, palette = palette, viewModel = viewModel)
                }
            }
            "loans" -> {
                item {
                    LoansAndPayoffSection(state = state, settings = settings, palette = palette, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun PeopleAndKhataSection(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks
) {
    val curr = settings.currencySymbol
    var showAddPerson by remember { mutableStateOf(false) }
    var showSmartSplit by remember { mutableStateOf(false) }
    var khataShop by remember { mutableStateOf("") }
    var khataItem by remember { mutableStateOf("") }
    var khataAmount by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Top Action Bar: Smart Split Bill, WhatsApp Summary, + Person
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { showSmartSplit = true },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                modifier = Modifier.weight(1f)
            ) {
                Text("⚡ Smart Split", color = palette.background, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    val summary = buildString {
                        append("📋 *Tracmio Settlement Summary*\n")
                        state.persons.forEach { p ->
                            append("• ${p.name}: $curr${"%,.0f".format(p.netBalance)}\n")
                        }
                    }
                    hostCallbacks.copyToClipboard(summary)
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated),
                modifier = Modifier.weight(1f)
            ) {
                Text("📋 WhatsApp Copy", color = palette.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Button(
                onClick = { showAddPerson = true },
                colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated)
            ) {
                Text("+ Person", color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Person Cards
        state.persons.forEach { person ->
            LiquidGlassComponents(settings = settings, palette = palette) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(person.name, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                        if (person.upiId.isNotBlank()) {
                            Text(
                                "📋 ${person.upiId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                modifier = Modifier.clickable { hostCallbacks.copyToClipboard(person.upiId) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "$curr${"%,.0f".format(person.netBalance)}",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (person.netBalance >= 0) palette.primary else Color(0xFFFB7185)
                        )
                        OutlinedButton(onClick = { viewModel.settlePerson(person) }, shape = RoundedCornerShape(10.dp)) {
                            Text("✓ Settle", color = palette.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Friend / Shopkeeper Khata (Pay Later) Card
        LiquidGlassComponents(settings = settings, palette = palette) {
            Text("📓 Friend / Shopkeeper Khata (Pay Later)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = khataShop,
                    onValueChange = { khataShop = it },
                    label = { Text("Shop / Friend") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = khataAmount,
                    onValueChange = { khataAmount = it },
                    label = { Text("Amount ($curr)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = khataItem,
                    onValueChange = { khataItem = it },
                    label = { Text("Item (e.g. Milk, Groceries)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val amt = khataAmount.toDoubleOrNull() ?: 0.0
                        if (khataShop.isNotBlank() && amt > 0.0) {
                            viewModel.addKhataEntry(khataShop.trim(), khataItem.trim().ifBlank { "Khata & Kirana" }, amt)
                            khataShop = ""
                            khataItem = ""
                            khataAmount = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text("+ Add Khata", color = palette.background, fontWeight = FontWeight.ExtraBold)
                }
            }

            state.khataEntries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(palette.surface).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${entry.shopOrFriend} · ${entry.itemDescription}", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("$curr${"%,.0f".format(entry.amount)} · ${if (entry.isSettled) "Settled" else "Pending"}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!entry.isSettled) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.primary.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { viewModel.settleKhataEntry(entry) }
                            ) {
                                Text("Clear ✓", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("✕", color = Color(0xFFFB7185), modifier = Modifier.clickable { viewModel.deleteKhataEntry(entry) })
                    }
                }
            }
        }
    }

    if (showAddPerson) {
        var name by remember { mutableStateOf("") }
        var upi by remember { mutableStateOf("") }
        var bal by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPerson = false },
            containerColor = palette.surfaceElevated,
            title = { Text("👥 Add Person", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value = upi, onValueChange = { upi = it }, label = { Text("UPI ID (Optional)") }, singleLine = true)
                    OutlinedTextField(value = bal, onValueChange = { bal = it }, label = { Text("Opening Net Balance ($curr)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addPerson(name.trim(), upi.trim(), bal.toDoubleOrNull() ?: 0.0)
                            showAddPerson = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) { Text("Save", color = palette.background, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddPerson = false }) { Text("Cancel", color = palette.textSecondary) }
            }
        )
    }

    if (showSmartSplit) {
        var totalBill by remember { mutableStateOf("") }
        var headCount by remember { mutableStateOf("3") }
        var includeOwnShare by remember { mutableStateOf(true) }
        val bill = totalBill.toDoubleOrNull() ?: 0.0
        val count = (headCount.toIntOrNull() ?: 1).coerceAtLeast(1)
        val perPerson = bill / count.toDouble()
        val othersOwe = if (includeOwnShare) (bill - perPerson).coerceAtLeast(0.0) else bill

        AlertDialog(
            onDismissRequest = { showSmartSplit = false },
            containerColor = palette.surfaceElevated,
            title = { Text("⚡ Smart Split Bill Calculator", fontWeight = FontWeight.ExtraBold, color = palette.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = totalBill, onValueChange = { totalBill = it }, label = { Text("Total Bill ($curr)") }, singleLine = true)
                    OutlinedTextField(value = headCount, onValueChange = { headCount = it }, label = { Text("Number of People") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeOwnShare, onCheckedChange = { includeOwnShare = it })
                        Text("Include my own share", color = palette.textPrimary)
                    }
                    Text("Each Person Pays: $curr${"%,.2f".format(perPerson)}", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                    Text(
                        if (includeOwnShare) "Others Owe You: $curr${"%,.2f".format(othersOwe)} (${(count - 1).coerceAtLeast(0)} people)"
                        else "Others Owe You: $curr${"%,.2f".format(othersOwe)} ($count people)",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSmartSplit = false }, colors = ButtonDefaults.buttonColors(containerColor = palette.primary)) {
                    Text("Done", color = palette.background, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun BudgetsAndRunwaySection(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel
) {
    val curr = settings.currencySymbol
    val runwayMonths = remember(state.accounts, state.transactions) {
        NativeLedgerCalculator.computeEmergencyRunwayMonths(state.accounts, state.transactions)
    }
    var catName by remember { mutableStateOf("Food & Chai") }
    var limitText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Emergency Fund Runway Meter
        LiquidGlassComponents(settings = settings, palette = palette) {
            Text("🛟 Emergency Fund Runway Meter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.primary)
            Text(
                "${"%.1f".format(runwayMonths)} Months of Liquid Survival",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary
            )
            Text("Formula: (Bank + Savings) ÷ Monthly Expense Rate (Target: 6.0+ months)", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }

        // Add Category Budget
        LiquidGlassComponents(settings = settings, palette = palette) {
            Text("➕ Set Monthly Category Budget", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = limitText, onValueChange = { limitText = it }, label = { Text("Limit ($curr)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    val lim = limitText.toDoubleOrNull() ?: 0.0
                    if (catName.isNotBlank() && lim > 0.0) {
                        viewModel.addBudget(catName.trim(), lim)
                        limitText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Save Budget", color = palette.background, fontWeight = FontWeight.ExtraBold)
            }
        }

        state.budgets.forEach { b ->
            val lim = if (b.monthlyLimit > 0) b.monthlyLimit else MoneyPaise.toRupees(b.limitPaise)
            val spent = FinancialCalculator.categoryTotal(state.transactions, b.category)
            val prog = FinancialCalculator.progress(spent, lim).toFloat()
            LiquidGlassComponents(settings = settings, palette = palette) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(b.category, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                    Text("$curr${"%,.0f".format(spent)} / $curr${"%,.0f".format(lim)}", color = palette.primary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = palette.primary, trackColor = palette.surfaceElevated)
            }
        }
    }
}

@Composable
private fun GoalsAndCoolingSection(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel
) {
    val curr = settings.currencySymbol
    var goalTitle by remember { mutableStateOf("") }
    var goalTarget by remember { mutableStateOf("") }
    var wishItem by remember { mutableStateOf("") }
    var wishPrice by remember { mutableStateOf("") }
    var cashbackApp by remember { mutableStateOf("GPay") }
    var cashbackAmt by remember { mutableStateOf("") }
    var ppuName by remember { mutableStateOf("") }
    var ppuPrice by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Round-Up Spare Change Jar
        LiquidGlassComponents(settings = settings, palette = palette) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🪙 Round-Up Spare Change Jar", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                    Text("Automatically rounds every spend to nearest ₹10 into savings", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
                Switch(checked = settings.roundUpEnabled, onCheckedChange = { viewModel.toggleRoundUp(it) })
            }
        }

        // Savings Goals List & Creator
        LiquidGlassComponents(settings = settings, palette = palette) {
            Text("🎯 Savings Goals", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = goalTitle, onValueChange = { goalTitle = it }, label = { Text("Goal Name") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = goalTarget, onValueChange = { goalTarget = it }, label = { Text("Target ($curr)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    val t = goalTarget.toDoubleOrNull() ?: 0.0
                    if (goalTitle.isNotBlank() && t > 0.0) {
                        viewModel.addGoal(goalTitle.trim(), t)
                        goalTitle = ""
                        goalTarget = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Create Goal", color = palette.background, fontWeight = FontWeight.ExtraBold)
            }
            state.goals.forEach { g ->
                val cur = if (g.currentAmount > 0) g.currentAmount else MoneyPaise.toRupees(g.currentPaise)
                val tar = if (g.targetAmount > 0) g.targetAmount else MoneyPaise.toRupees(g.targetPaise)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(palette.surface).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(g.title.ifBlank { g.name }, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("$curr${"%,.0f".format(cur)} / $curr${"%,.0f".format(tar)}", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                    }
                    Button(
                        onClick = { viewModel.contributeToGoal(g, 500.0) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated)
                    ) {
                        Text("+ ₹500", color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 24h Cooling Wishlist
        LiquidGlassComponents(settings = settings, palette = palette) {
            Text("🧊 24h Impulse Cooling Wishlist", fontWeight = FontWeight.ExtraBold, color = palette.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = wishItem, onValueChange = { wishItem = it }, label = { Text("Impulse Item") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = wishPrice, onValueChange = { wishPrice = it }, label = { Text("Price ($curr)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    val p = wishPrice.toDoubleOrNull() ?: 0.0
                    if (wishItem.isNotBlank() && p > 0.0) {
                        viewModel.addWishlistCooldown(wishItem.trim(), p)
                        wishItem = ""
                        wishPrice = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Start 24h Cooldown", color = palette.background, fontWeight = FontWeight.ExtraBold)
            }
            state.wishlistCooldowns.forEach { w ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(palette.surface).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(w.itemName, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("$curr${"%,.0f".format(w.price)} · ${w.status}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                    if (w.status == "COOLING") {
                        Button(
                            onClick = { viewModel.skipWishlistAndSave(w) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("✓ Skip & Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Cashback & Rewards Tracker
        LiquidGlassComponents(settings = settings, palette = palette) {
            val totalCashback = state.cashbackRewards.sumOf { it.amount }
            Text("🎁 Cashback & Rewards (Total: $curr${"%,.0f".format(totalCashback)})", fontWeight = FontWeight.ExtraBold, color = palette.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = cashbackApp, onValueChange = { cashbackApp = it }, label = { Text("Source App") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = cashbackAmt, onValueChange = { cashbackAmt = it }, label = { Text("Cashback ($curr)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    val amt = cashbackAmt.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        viewModel.addCashbackReward(cashbackApp.trim(), amt)
                        cashbackAmt = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Log Cashback", color = palette.background, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Price-Per-Use Value Calculator
        LiquidGlassComponents(settings = settings, palette = palette) {
            Text("🧮 Price-Per-Use Value Calculator", fontWeight = FontWeight.ExtraBold, color = palette.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = ppuName, onValueChange = { ppuName = it }, label = { Text("Item (e.g. Headphones)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = ppuPrice, onValueChange = { ppuPrice = it }, label = { Text("Cost ($curr)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    val cost = ppuPrice.toDoubleOrNull() ?: 0.0
                    if (ppuName.isNotBlank() && cost > 0.0) {
                        viewModel.addPricePerUseItem(ppuName.trim(), cost)
                        ppuName = ""
                        ppuPrice = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Track Item Value", color = palette.background, fontWeight = FontWeight.ExtraBold)
            }
            state.pricePerUseItems.forEach { item ->
                val costPerUse = item.purchasePrice / item.usageCount.coerceAtLeast(1).toDouble()
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(palette.surface).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.itemName, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("${item.usageCount} uses · $curr${"%,.1f".format(costPerUse)} / use", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                    }
                    Button(
                        onClick = { viewModel.incrementItemUse(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated)
                    ) {
                        Text("+1 Use", color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoansAndPayoffSection(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel
) {
    val curr = settings.currencySymbol
    var extraMonthlyText by remember { mutableStateOf("2000") }
    var lenderName by remember { mutableStateOf("") }
    var principalText by remember { mutableStateOf("") }
    var aprText by remember { mutableStateOf("12") }
    var emiText by remember { mutableStateOf("1500") }

    val extraMonthly = extraMonthlyText.toDoubleOrNull() ?: 0.0
    val snowball = remember(state.loans, state.loanPayments, extraMonthly) {
        NativeLedgerCalculator.simulateDebtPayoff(state.loans, state.loanPayments, extraMonthly, isAvalanche = false)
    }
    val avalanche = remember(state.loans, state.loanPayments, extraMonthly) {
        NativeLedgerCalculator.simulateDebtPayoff(state.loans, state.loanPayments, extraMonthly, isAvalanche = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Snowball vs. Avalanche Debt Payoff Planner
        LiquidGlassComponents(settings = settings, palette = palette) {
            Text("📈 Snowball vs. Avalanche Debt Payoff Planner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.primary)
            OutlinedTextField(
                value = extraMonthlyText,
                onValueChange = { extraMonthlyText = it },
                label = { Text("Extra Monthly Payoff ($curr)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(palette.surface).padding(12.dp)) {
                    Column {
                        Text("❄️ Snowball (Smallest First)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text("${snowball.totalMonths} mos", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                        Text("Interest: $curr${"%,.0f".format(snowball.totalInterestPaid)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFB7185))
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(palette.surface).padding(12.dp)) {
                    Column {
                        Text("🏔️ Avalanche (Highest APR)", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                        Text("${avalanche.totalMonths} mos", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                        Text("Interest: $curr${"%,.0f".format(avalanche.totalInterestPaid)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399))
                    }
                }
            }
        }

        // Add Loan / EMI
        LiquidGlassComponents(settings = settings, palette = palette) {
            Text("🏦 Add Loan / EMI", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = lenderName, onValueChange = { lenderName = it }, label = { Text("Loan / Lender") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = principalText, onValueChange = { principalText = it }, label = { Text("Principal ($curr)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = aprText, onValueChange = { aprText = it }, label = { Text("Interest %") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = emiText, onValueChange = { emiText = it }, label = { Text("Monthly EMI ($curr)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    val p = principalText.toDoubleOrNull() ?: 0.0
                    val apr = aprText.toDoubleOrNull() ?: 0.0
                    val emi = emiText.toDoubleOrNull() ?: 1000.0
                    if (lenderName.isNotBlank() && p > 0.0) {
                        viewModel.addLoan(lenderName.trim(), p, apr, emi)
                        lenderName = ""
                        principalText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Save Loan", color = palette.background, fontWeight = FontWeight.ExtraBold)
            }

            state.loans.forEach { loan ->
                val remPaise = NativeLedgerCalculator.loanRemainingPaise(loan, state.loanPayments)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(palette.surface).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(loan.name.ifBlank { loan.lender }, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("Remaining: ${MoneyPaise.formatRupees(remPaise, curr)} · APR ${loan.interestRate}%", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                    Button(
                        onClick = { viewModel.recordLoanRepayment(loan, loan.monthlyPayment.coerceAtLeast(500.0)) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated)
                    ) {
                        Text("+ Pay EMI", color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
