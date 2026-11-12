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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.CustomTrackerEntity
import com.tracmio.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CustomTrackersHubScreen(
    trackers: List<CustomTrackerEntity>,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    settings: LiquidGlassSettings,
    onCreateTracker: (name: String, category: String, budget: Double, startDate: String, endDate: String) -> Unit,
    onExportTrackerWorkbook: (CustomTrackerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    var selectedTrackerForFolder by remember { mutableStateOf<CustomTrackerEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (selectedTrackerForFolder != null) {
        TrackerFolderWorkspaceView(
            tracker = selectedTrackerForFolder!!,
            allTransactions = transactions,
            accounts = accounts,
            palette = palette,
            onBack = { selectedTrackerForFolder = null },
            onExport = { onExportTrackerWorkbook(selectedTrackerForFolder!!) }
        )
        return
    }

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
                        text = "Custom Trackers & Trips",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Folder workspaces for trips, projects & events",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ New", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (trackers.isEmpty()) {
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
                        Text("📁", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No custom trackers created yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Track Goa Vacation, Wedding Prep, Home Renovation, College Fest, or Hackathon budgets with split calculations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                        ) {
                            Text("Create First Tracker", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(trackers, key = { it.id }) { tr ->
                TrackerFolderSummaryCard(
                    tracker = tr,
                    palette = palette,
                    onClick = { selectedTrackerForFolder = tr }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateCustomTrackerDialog(
            palette = palette,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, cat, budget, sDate, eDate ->
                onCreateTracker(name, cat, budget, sDate, eDate)
                showCreateDialog = false
            }
        )
    }
}

/**
 * Summary card for each custom tracker showing folder iconography, high-contrast end date, and click-to-open.
 */
@Composable
fun TrackerFolderSummaryCard(
    tracker: CustomTrackerEntity,
    palette: ThemePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val endDateStr = tracker.endDate.ifBlank { "Ongoing" }
    val startDateStr = tracker.startDate.ifBlank { "Started" }
    val categoryDisplay = tracker.customTypeName.ifBlank { tracker.trackerType }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = Color(0xCC0F192C),
        borderColor = palette.primary.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.primary.copy(alpha = 0.15f))
                            .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = palette.primary)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = tracker.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$categoryDisplay • Budget: ${MoneyPaise.formatCurrency(tracker.budgetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x3310B981)
                ) {
                    Text(
                        text = "Open Folder ➔",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // High-Contrast Clearly Visible End Date (Requirement #5)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.primary.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DURATION:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary
                        )
                    }

                    Text(
                        text = "$startDateStr ➔ $endDateStr",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Tracker Folder Workspace View (Requirement #9)
 * Shows sub-tabs for Transactions and Reports, with a top-right Export button (.xlsx) and Edit button.
 */
@Composable
fun TrackerFolderWorkspaceView(
    tracker: CustomTrackerEntity,
    allTransactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    palette: ThemePalette,
    onBack: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf("TRANSACTIONS") } // "TRANSACTIONS" or "REPORTS"
    val endDateStr = tracker.endDate.ifBlank { "Ongoing" }
    val categoryDisplay = tracker.customTypeName.ifBlank { tracker.trackerType }

    // Transactions matching tracker name or category
    val trackerTransactions = remember(allTransactions, tracker) {
        allTransactions.filter { tx ->
            tx.note.contains(tracker.name, ignoreCase = true) ||
            tx.category.contains(categoryDisplay, ignoreCase = true)
        }
    }
    val totalSpent = trackerTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val remainingBudget = (tracker.budgetAmount - totalSpent).coerceAtLeast(0.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar with Back, Folder Title, and Top-Right Export & Edit (Requirement #9)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tracker.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "$categoryDisplay Workspace",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }

                // Top-Right Export & Edit Actions (Requirement #9)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onExport,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Folder Workspace Header Card: Budget Burn & High-Contrast End Date
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC111D32),
                borderColor = palette.primary.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(MoneyPaise.formatCurrency(totalSpent), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Target Budget", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(MoneyPaise.formatCurrency(tracker.budgetAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // High-Contrast End Date Display (Requirement #5)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏁 Tracker End Date:", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold)
                            Text(endDateStr, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Sub-tabs: Transactions vs Reports (Requirement #9)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TRANSACTIONS" to "📑 Transactions", "REPORTS" to "📊 Folder Reports").forEach { (tabKey, title) ->
                    val isSelected = activeSubTab == tabKey
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) palette.primary else Color(0x331E293B),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSubTab = tabKey }
                    ) {
                        Text(
                            text = title,
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

        // Sub-Tab Content
        if (activeSubTab == "TRANSACTIONS") {
            if (trackerTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions linked to this date range yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(trackerTransactions, key = { it.id }) { tx ->
                    TransactionVisualCard(
                        transaction = tx,
                        accounts = accounts,
                        palette = palette,
                        onEditClick = {},
                        onCardClick = {}
                    )
                }
            }
        } else {
            // Reports Sub-tab
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xCC0E1A2D)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Category Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))

                        val catMap = trackerTransactions.groupBy { it.category }
                        catMap.forEach { (cat, list) ->
                            val sum = list.sumOf { it.amount }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$cat (${list.size})", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                Text(MoneyPaise.formatCurrency(sum), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = palette.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to create a custom tracker, including `+` button in category selector (Requirement #5)
 */
@Composable
fun CreateCustomTrackerDialog(
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, budget: Double, startDate: String, endDate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vacation & Trip") }
    var budgetText by remember { mutableStateOf("") }
    var daysDurationText by remember { mutableStateOf("7") }

    val categories = remember {
        mutableListOf("Vacation & Trip", "Wedding Prep", "Home Renovation", "College Fest", "Hackathon", "Vehicle Project")
    }
    var showAddCustomCategoryDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Custom Tracker Folder", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tracker Name (e.g. Goa Trip 2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector with + Button (Requirement #5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category:", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)

                    // + button to add custom category (Requirement #5)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.primary.copy(alpha = 0.2f),
                        modifier = Modifier.clickable { showAddCustomCategoryDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = palette.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("+ Category", style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = category == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) palette.primary else Color(0x331E293B),
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("Target Budget (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = daysDurationText,
                    onValueChange = { daysDurationText = it },
                    label = { Text("Duration in Days (Sets End Date)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val budget = budgetText.toDoubleOrNull() ?: 10000.0
                    val days = daysDurationText.toIntOrNull() ?: 7
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val sDate = dateFormat.format(Date())
                    val eDate = dateFormat.format(Date(System.currentTimeMillis() + days.toLong() * 24L * 60L * 60L * 1000L))
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), category, budget, sDate, eDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
            ) {
                Text("Create Tracker", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textSecondary)
            }
        },
        containerColor = Color(0xFF131D2D)
    )

    if (showAddCustomCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomCategoryDialog = false },
            title = { Text("Add Custom Category", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            categories.add(newCatName.trim())
                            category = newCatName.trim()
                            showAddCustomCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomCategoryDialog = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            },
            containerColor = Color(0xFF131D2D)
        )
    }
}

/**
 * Upgraded Financial Time Machine Screen (Requirement #10)
 * Features timeline jump presets, Then vs. Today comparison delta cards,
 * reconstructed account balances, and activity list up to that date.
 */
@Composable
fun FinancialTimeMachineScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    var selectedPreset by remember { mutableStateOf("End of Last Month") }
    val now = remember { System.currentTimeMillis() }

    val targetTimestamp = remember(selectedPreset) {
        val cal = Calendar.getInstance()
        when (selectedPreset) {
            "End of Last Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.DAY_OF_MONTH, -1)
                cal.timeInMillis
            }
            "3 Months Ago" -> {
                cal.add(Calendar.MONTH, -3)
                cal.timeInMillis
            }
            "6 Months Ago" -> {
                cal.add(Calendar.MONTH, -6)
                cal.timeInMillis
            }
            "1 Year Ago" -> {
                cal.add(Calendar.YEAR, -1)
                cal.timeInMillis
            }
            else -> now
        }
    }

    val pastTransactions = remember(state.transactions, targetTimestamp) {
        state.transactions.filter { it.timestamp <= targetTimestamp }
    }
    val pastIncome = pastTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val pastExpense = pastTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val pastNetWorth = pastIncome - pastExpense

    val currentNetWorth = state.totalIncome - state.totalExpense
    val netWorthDelta = currentNetWorth - pastNetWorth

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = palette.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Financial Time Machine",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Travel backward to inspect balances and spending deltas",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // Timeline Jump Presets Bar (Requirement #10)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("End of Last Month", "3 Months Ago", "6 Months Ago", "1 Year Ago")) { preset ->
                    val isSelected = selectedPreset == preset
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) palette.primary else Color(0x331E293B),
                        modifier = Modifier.clickable { selectedPreset = preset }
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Then vs Today Comparison Delta Card (Requirement #10)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0E1A2D),
                borderColor = palette.primary.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("THEN VS. TODAY COMPARISON", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.textSecondary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("As of $selectedPreset", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(MoneyPaise.formatCurrency(pastNetWorth), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Current Position", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text(MoneyPaise.formatCurrency(currentNetWorth), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delta badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (netWorthDelta >= 0) Color(0x2210B981) else Color(0x22F43F5E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (netWorthDelta >= 0) Color(0x5510B981) else Color(0x55F43F5E))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (netWorthDelta >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (netWorthDelta >= 0) Color(0xFF10B981) else Color(0xFFF43F5E),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Net Worth Delta: ${if (netWorthDelta >= 0) "+" else ""}${MoneyPaise.formatCurrency(netWorthDelta)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netWorthDelta >= 0) Color(0xFF10B981) else Color(0xFFF43F5E)
                            )
                        }
                    }
                }
            }
        }

        // Reconstructed Account Balances at Past Date
        item {
            Text("Reconstructed Balance at $selectedPreset", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
        }

        items(state.accounts, key = { it.id }) { acc ->
            val accTxs = pastTransactions.filter { it.accountId == acc.id }
            val accIncome = accTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val accExpense = accTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val reconstructedBal = acc.balance + accIncome - accExpense

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0x331E293B)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(acc.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(MoneyPaise.formatCurrency(reconstructedBal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
