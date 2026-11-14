package com.tracmio.app.ui.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tracmio.app.data.AcademicEventItem
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.NativeLedgerCalculator
import com.tracmio.app.data.RechargeTrackerItem
import com.tracmio.app.data.TransactionDateTime
import com.tracmio.app.data.local.CustomTrackerEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun CalendarAndAcademicScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    val streak = remember(state.transactions) { NativeLedgerCalculator.computeNoSpendStreakDays(state.transactions) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDateKey by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-12-15") }
    var fee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var showAddRecharge by remember { mutableStateOf(false) }
    var editingRechargePack by remember { mutableStateOf<RechargeTrackerItem?>(null) }
    var rechargeOperator by remember { mutableStateOf("Jio") }
    var rechargeValidity by remember { mutableIntStateOf(28) }
    var customValidityText by remember { mutableStateOf("28") }
    var rechargeAmount by remember { mutableStateOf("299") }
    var rechargeStartDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }

    val displayCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val monthHeader = remember(displayCal.timeInMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(displayCal.timeInMillis))
    }
    val ymPrefix = remember(displayCal.timeInMillis) {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(displayCal.timeInMillis))
    }
    val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startBlankCells = ((displayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7) // Monday-first grid (0..6)

    val dayTxnCount = remember(state.transactions, ymPrefix) {
        state.transactions
            .groupBy { TransactionDateTime.formatDateInput(it.timestamp) }
            .mapValues { (_, list) -> list.size }
    }
    val dayEventMap = remember(state.academicEvents) {
        state.academicEvents.groupBy { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }
                Text("📅 Calendar & Streak", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        editingRechargePack = null
                        rechargeOperator = "Jio"
                        rechargeValidity = 28
                        customValidityText = "28"
                        rechargeAmount = "299"
                        rechargeStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        showAddRecharge = true
                    },
                    modifier = Modifier.testTag("add_recharge_btn")
                ) {
                    Text("+ SIM/WiFi")
                }
                Button(onClick = { showAdd = true }, modifier = Modifier.testTag("add_deadline_btn")) { Text("+ Deadline") }
            }
        }

        // Feature #8: SIM / WiFi Recharge Countdown (28 / 56 / 84 Days)
        GlassCard(
            settings = settings,
            modifier = Modifier.fillMaxWidth().testTag("calendar_recharge_countdown_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "📶 SIM / WiFi Recharge Countdown (28 / 56 / 84d)",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Auto-calculates pack expiry from recharge date and adds a calendar deadline.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    OutlinedButton(onClick = {
                        editingRechargePack = null
                        rechargeOperator = "Jio"
                        rechargeValidity = 28
                        customValidityText = "28"
                        rechargeAmount = "299"
                        rechargeStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        showAddRecharge = true
                    }) {
                        Text("+ Add Recharge Plan", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (state.rechargeTrackers.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceElevated.copy(alpha = 0.45f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No active Jio / Airtel / Vi / WiFi packs tracked yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { editingRechargePack = null; showAddRecharge = true }) {
                            Text("+ Add Plan", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                        }
                    }
                } else {
                    state.rechargeTrackers.forEach { pack ->
                        val (daysLeft, expiryDate) = NativeLedgerCalculator.computeRechargeDaysLeft(
                            pack.rechargeDate,
                            pack.validityDays
                        )
                        val isUrgent = daysLeft <= 3
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceElevated.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "📡 ${pack.operatorName} (${pack.validityDays}d) · ${displayAmount(pack.amount, settings)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Recharged: ${pack.rechargeDate} · Expires: $expiryDate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                                Text(
                                    text = if (daysLeft < 0) "Expired" else "$daysLeft days left",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isUrgent) Color(0xFFEF4444) else palette.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.renewRechargeTracker(pack) },
                                    modifier = Modifier.testTag("renew_recharge_${pack.id}")
                                ) {
                                    Text("Renew +${pack.validityDays}d", style = MaterialTheme.typography.labelSmall)
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingRechargePack = pack
                                            rechargeOperator = pack.operatorName
                                            rechargeValidity = pack.validityDays
                                            customValidityText = pack.validityDays.toString()
                                            rechargeAmount = MoneyPaise.formatInput(MoneyPaise.toPaise(pack.amount))
                                            rechargeStartDate = pack.rechargeDate
                                            showAddRecharge = true
                                        },
                                        modifier = Modifier.size(30.dp).testTag("edit_recharge_${pack.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit pack", tint = palette.secondary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteRechargeTracker(pack.id) },
                                        modifier = Modifier.size(30.dp).testTag("delete_recharge_${pack.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete pack", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // No-Spend Streak & Milestones Card
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔥 No-Spend Streak & Milestones", style = MaterialTheme.typography.titleMedium, color = palette.primary)
                Text("$streak consecutive no-spend day(s)", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary, fontWeight = FontWeight.ExtraBold)
                val badge = when {
                    streak >= 14 -> "🏆 Vault Master (14+ days)"
                    streak >= 7 -> "🥇 Weekly Warrior (7+ days)"
                    streak >= 3 -> "🔥 Discipline Spark (3+ days)"
                    else -> "🌱 Start your 3-day no-spend milestone today"
                }
                Text(badge, style = MaterialTheme.typography.bodySmall, color = palette.secondary)
            }
        }

        // 7-Column Interactive Calendar Grid
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthOffset-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = palette.textPrimary)
                    }
                    Text(monthHeader, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { monthOffset++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = palette.textPrimary)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                        Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
                val totalCells = startBlankCells + daysInMonth
                val weeks = (totalCells + 6) / 7
                repeat(weeks) { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) { col ->
                            val cellIndex = week * 7 + col
                            val dayNum = cellIndex - startBlankCells + 1
                            if (dayNum < 1 || dayNum > daysInMonth) {
                                Spacer(Modifier.weight(1f).height(38.dp))
                            } else {
                                val dateKey = "$ymPrefix-${String.format(Locale.US, "%02d", dayNum)}"
                                val hasTx = (dayTxnCount[dateKey] ?: 0) > 0
                                val hasEvt = !dayEventMap[dateKey].isNullOrEmpty()
                                val selected = selectedDateKey == dateKey
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) palette.primary.copy(alpha = 0.22f) else palette.surfaceElevated.copy(alpha = 0.4f))
                                        .clickable { selectedDateKey = dateKey; date = dateKey },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("$dayNum", style = MaterialTheme.typography.labelMedium, color = if (selected) palette.primary else palette.textPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        if (hasTx) Box(Modifier.size(5.dp).clip(CircleShape).background(palette.primary))
                                        if (hasEvt) Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Day Inspector
                val selTxns = state.transactions.filter { TransactionDateTime.formatDateInput(it.timestamp) == selectedDateKey }
                val selEvents = state.academicEvents.filter { it.date == selectedDateKey }
                Text("Selected: $selectedDateKey (${selTxns.size} txns · ${selEvents.size} deadlines)", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                selTxns.take(5).forEach { tx ->
                    Text("• ${tx.category} (${tx.type}): ${displayAmount(tx.amount, settings)}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                }
                selEvents.forEach { ev ->
                    Text("• 📌 ${ev.title}: ${displayAmount(ev.feeAmount, settings)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF59E0B))
                }
            }
        }

        Text("Upcoming Deadlines & Academic Fees", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
        state.academicEvents.forEach { ev ->
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.title, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                            Text("Due: ${ev.date} · ${displayAmount(ev.feeAmount, settings)} · ${if (ev.isPaid) "PAID ✅" else "PENDING"}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        Row {
                            IconButton(onClick = { viewModel.toggleAcademicEventPaid(ev.id) }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Toggle paid", tint = if (ev.isPaid) palette.primary else palette.textSecondary)
                            }
                            IconButton(onClick = {
                                val triggerMs = System.currentTimeMillis() + 60_000L
                                hostCallbacks.scheduleCalendarEventReminder(ev.title, "Due on ${ev.date} (${displayAmount(ev.feeAmount, settings)})", triggerMs, ev.id)
                                viewModel.showStatus("Event reminder scheduled")
                            }) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Remind me", tint = palette.secondary)
                            }
                            IconButton(onClick = { viewModel.deleteAcademicEvent(ev.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete event", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRecharge || editingRechargePack != null) {
        val ed = editingRechargePack
        AlertDialog(
            onDismissRequest = { showAddRecharge = false; editingRechargePack = null },
            containerColor = palette.surface,
            title = {
                Text(
                    if (ed == null) "📶 Add SIM / WiFi Recharge Countdown" else "✏️ Edit SIM / WiFi Recharge",
                    color = palette.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Operator / Broadband", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Jio", "Airtel", "Vi", "BSNL", "Broadband / WiFi").forEach { op ->
                            FilterChip(
                                selected = rechargeOperator == op,
                                onClick = { rechargeOperator = op },
                                label = { Text(op) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = rechargeOperator,
                        onValueChange = { rechargeOperator = it },
                        label = { Text("Provider Name") }
                    )
                    Text("Pack Validity (28 / 56 / 84 or Custom Days)", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(28, 56, 84, 30, 365).forEach { vDays ->
                            FilterChip(
                                selected = rechargeValidity == vDays,
                                onClick = {
                                    rechargeValidity = vDays
                                    customValidityText = vDays.toString()
                                },
                                label = { Text("${vDays}d") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customValidityText,
                        onValueChange = {
                            customValidityText = it
                            it.toIntOrNull()?.takeIf { d -> d > 0 }?.let { d -> rechargeValidity = d }
                        },
                        label = { Text("Validity Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = rechargeAmount,
                        onValueChange = { rechargeAmount = it },
                        label = { Text("Pack Price (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = rechargeStartDate,
                        onValueChange = { rechargeStartDate = it },
                        label = { Text("Recharge Date (YYYY-MM-DD)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = rechargeAmount.toDoubleOrNull() ?: 0.0
                    val days = (customValidityText.toIntOrNull() ?: rechargeValidity).coerceAtLeast(1)
                    if (ed == null) {
                        viewModel.addRechargeTracker(rechargeOperator, days, amt, rechargeStartDate)
                    } else {
                        viewModel.updateRechargeTracker(ed.id, rechargeOperator, days, amt, rechargeStartDate)
                    }
                    editingRechargePack = null
                    showAddRecharge = false
                }) { Text("Save Countdown") }
            },
            dismissButton = { TextButton(onClick = { showAddRecharge = false; editingRechargePack = null }) { Text("Cancel") } }
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = palette.surface,
            title = { Text("Add Calendar Deadline / Fee", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") })
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") })
                    OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Amount (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addAcademicEvent(
                            AcademicEventItem(
                                id = "acad_${UUID.randomUUID().toString().take(8)}",
                                title = title.trim(),
                                date = date.trim(),
                                feeAmount = fee.toDoubleOrNull() ?: 0.0,
                                isPaid = false,
                                note = note.trim()
                            )
                        )
                        title = ""; fee = ""; note = ""; showAdd = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TimeMachineScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) } // 0=Past Travel, 1=Future Forecast, 2=Compare Dates
    val sorted = remember(state.transactions) { state.transactions.sortedBy { it.timestamp } }
    var sliderVal by remember { mutableFloatStateOf(1f) }
    var compareSliderA by remember { mutableFloatStateOf(0.5f) }
    var compareSliderB by remember { mutableFloatStateOf(1f) }
    var forecastMonths by remember { mutableIntStateOf(24) }

    val cutoffIndex = (sorted.size * sliderVal).toInt().coerceIn(0, sorted.size)
    val subTxns = sorted.take(cutoffIndex)
    val balAtTime = NativeLedgerCalculator.totalBalance(state.accounts, subTxns)
    val dateLabel = subTxns.lastOrNull()?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.timestamp)) } ?: "Opening Ledger"

    fun applyDaysPreset(daysBack: Int) {
        if (sorted.isEmpty()) { sliderVal = 0f; return }
        if (daysBack < 0) { sliderVal = 0f; return }
        val targetMs = System.currentTimeMillis() - daysBack * 86_400_000L
        val count = sorted.count { it.timestamp <= targetMs }
        sliderVal = (count.toFloat() / sorted.size.toFloat()).coerceIn(0f, 1f)
    }

    val currentBal = state.totalBalance
    val activeMonths = sorted.map { NativeLedgerCalculator.monthKey(it.timestamp) }.distinct().size.coerceAtLeast(1)
    val avgMonthlyNet = ((state.totalIncome - state.totalExpense) / activeMonths).coerceAtLeast(0.0)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Text("⏳ Financial Time Machine", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Past Travel") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Future Forecast") })
            FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("Compare Dates") })
        }

        if (tab == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                OutlinedButton(onClick = { applyDaysPreset(-1) }) { Text("Start") }
                OutlinedButton(onClick = { applyDaysPreset(365) }) { Text("-1y") }
                OutlinedButton(onClick = { applyDaysPreset(180) }) { Text("-6m") }
                OutlinedButton(onClick = { applyDaysPreset(90) }) { Text("-3m") }
                OutlinedButton(onClick = { applyDaysPreset(30) }) { Text("-1m") }
                OutlinedButton(onClick = { applyDaysPreset(7) }) { Text("-7d") }
            }
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("SNAPSHOT DATE: $dateLabel", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                    Text(displayAmount(balAtTime, settings), style = MaterialTheme.typography.headlineMedium, color = palette.primary, fontWeight = FontWeight.ExtraBold)
                    Text("${subTxns.size} of ${sorted.size} transactions replayed", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    GlassSlider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        title = "Rewind / Replay Timeline",
                        valueLabel = "${(sliderVal * 100).toInt()}%",
                        accentColor = palette.primary,
                        textColor = palette.textPrimary
                    )
                }
            }
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Per-Account Balances at $dateLabel", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                    state.accounts.filter { !it.isArchived }.forEach { acc ->
                        val accBal = NativeLedgerCalculator.accountBalance(acc, subTxns)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(acc.name, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                            Text(displayAmount(accBal, settings), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = palette.primary)
                        }
                    }
                }
            }
        } else if (tab == 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12, 24, 36, 60).forEach { m ->
                    FilterChip(selected = forecastMonths == m, onClick = { forecastMonths = m }, label = { Text("${m}m") })
                }
            }
            val projectedPoints = remember(currentBal, avgMonthlyNet, forecastMonths) {
                (0..forecastMonths).map { m -> currentBal + avgMonthlyNet * m }
            }
            val finalProjected = projectedPoints.lastOrNull() ?: currentBal
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PROJECTED NET WORTH IN $forecastMonths MONTHS", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                    Text(displayAmount(finalProjected, settings), style = MaterialTheme.typography.headlineMedium, color = palette.primary, fontWeight = FontWeight.ExtraBold)
                    Text("Based on average monthly surplus of ${displayAmount(avgMonthlyNet, settings)}/mo", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        val maxP = (projectedPoints.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                        val minP = (projectedPoints.minOrNull() ?: 0.0).coerceAtMost(maxP - 1.0)
                        val span = (maxP - minP).coerceAtLeast(1.0)
                        val path = Path()
                        projectedPoints.forEachIndexed { idx, v ->
                            val x = (idx.toFloat() / (projectedPoints.size - 1).coerceAtLeast(1)) * size.width
                            val y = size.height - (((v - minP) / span).toFloat() * size.height * 0.85f)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = palette.primary, style = Stroke(width = 5f))
                    }
                }
            }
        } else {
            val idxA = (sorted.size * compareSliderA).toInt().coerceIn(0, sorted.size)
            val idxB = (sorted.size * compareSliderB).toInt().coerceIn(0, sorted.size)
            val subA = sorted.take(idxA)
            val subB = sorted.take(idxB)
            val balA = NativeLedgerCalculator.totalBalance(state.accounts, subA)
            val balB = NativeLedgerCalculator.totalBalance(state.accounts, subB)
            val dateA = subA.lastOrNull()?.let { TransactionDateTime.formatDateInput(it.timestamp) } ?: "Opening"
            val dateB = subB.lastOrNull()?.let { TransactionDateTime.formatDateInput(it.timestamp) } ?: "Opening"
            val delta = balB - balA

            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassSlider(value = compareSliderA, onValueChange = { compareSliderA = it }, title = "Date A ($dateA)", valueLabel = displayAmount(balA, settings), accentColor = palette.secondary, textColor = palette.textPrimary)
                    GlassSlider(value = compareSliderB, onValueChange = { compareSliderB = it }, title = "Date B ($dateB)", valueLabel = displayAmount(balB, settings), accentColor = palette.primary, textColor = palette.textPrimary)
                    Text(
                        "Net Worth Change ($dateA → $dateB): ${if (delta >= 0) "+" else ""}${displayAmount(delta, settings)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (delta >= 0) palette.primary else Color(0xFFF43F5E),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = {
                        hostCallbacks.printCustomReport(
                            "Tracmio_TimeMachine_Comparison",
                            "Time Machine Comparison",
                            listOf(
                                "TIME MACHINE SNAPSHOT COMPARISON" to true,
                                "Date A ($dateA): ${MoneyPaise.formatCurrency(balA)}" to false,
                                "Date B ($dateB): ${MoneyPaise.formatCurrency(balB)}" to false,
                                "Net Worth Delta: ${MoneyPaise.formatCurrency(delta)}" to true
                            )
                        )
                    }) { Text("Export Comparison PDF") }
                }
            }
        }
    }
}

@Composable
fun CustomTrackersScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var activeTracker by remember { mutableStateOf<CustomTrackerEntity?>(null) }
    var trackerToDelete by remember { mutableStateOf<CustomTrackerEntity?>(null) }
    var statusFilter by remember { mutableStateOf("ACTIVE") } // ACTIVE, ALL
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Trip") }
    var budget by remember { mutableStateOf("") }

    val displayedTrackers = remember(state.customTrackers, statusFilter) {
        if (statusFilter == "ACTIVE") state.customTrackers.filter { !it.status.equals("ARCHIVED", true) }
        else state.customTrackers
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }
                Text("🗂️ Custom Trackers", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            }
            Button(onClick = { showCreate = true }, modifier = Modifier.testTag("add_tracker_btn")) { Text("+ Tracker") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = statusFilter == "ACTIVE", onClick = { statusFilter = "ACTIVE" }, label = { Text("Active") })
            FilterChip(selected = statusFilter == "ALL", onClick = { statusFilter = "ALL" }, label = { Text("All Trackers") })
        }

        if (displayedTrackers.isEmpty()) {
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "No custom trackers yet. Create one for trips, weddings, events, or projects!",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { showCreate = true }) { Text("+ Create Tracker") }
                }
            }
        }

        displayedTrackers.forEach { tracker ->
            val links = state.trackerLinks.filter { it.trackerUid == tracker.stableUid }
            val refundUids = links.filter { it.isRefund }.map { it.transactionUid }.toSet()
            val linkedUids = links.map { it.transactionUid }.toSet()
            val linkedTxns = state.transactions.filter { it.stableUid in linkedUids }
            val spent = linkedTxns.filter { it.type.equals("EXPENSE", true) && it.stableUid !in refundUids }.sumOf { it.amount }
            val attachmentsCount = state.trackerAttachments.count { it.trackerUid == tracker.stableUid }
            val pct = if (tracker.budgetAmount > 0) (spent / tracker.budgetAmount).toFloat().coerceIn(0f, 1f) else 0f

            GlassCard(
                settings = settings,
                onClick = { activeTracker = tracker },
                modifier = Modifier.fillMaxWidth().testTag("tracker_card_${tracker.stableUid}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${tracker.icon} ${tracker.name}", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                            Text("${tracker.customTypeName} · Budget: ${displayAmount(tracker.budgetAmount, settings)}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        Row {
                            IconButton(onClick = {
                                hostCallbacks.pickTrackerAttachment { fileName, mime, uriStr, sizeBytes ->
                                    viewModel.addTrackerAttachment(tracker.stableUid, fileName, mime, uriStr, sizeBytes)
                                }
                            }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach receipt", tint = palette.secondary)
                            }
                            IconButton(onClick = { trackerToDelete = tracker }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete tracker", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                    Text("Tracked Spend: ${displayAmount(spent, settings)} (${linkedTxns.size} txns · $attachmentsCount files)", color = palette.primary, style = MaterialTheme.typography.bodySmall)
                    if (tracker.budgetAmount > 0) {
                        LinearProgressIndicator(progress = { pct }, color = if (pct >= 0.9f) Color(0xFFEF4444) else palette.primary, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)))
                    }
                    val latestUnlinked = state.transactions.firstOrNull { it.stableUid !in linkedUids }
                    if (latestUnlinked != null) {
                        OutlinedButton(onClick = { viewModel.linkTransactionToTracker(tracker.stableUid, latestUnlinked.stableUid) }) {
                            Text("+ Link Latest (${latestUnlinked.category})")
                        }
                    }
                }
            }
        }
    }

    trackerToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { trackerToDelete = null },
            containerColor = palette.surface,
            title = { Text("Delete ${target.name}?", color = palette.textPrimary) },
            text = {
                Text(
                    "Delete this Custom Tracker folder? Your main wallet transactions remain intact.",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    onClick = {
                        viewModel.deleteCustomTracker(target.stableUid)
                        trackerToDelete = null
                    }
                ) { Text("Delete Tracker") }
            },
            dismissButton = { TextButton(onClick = { trackerToDelete = null }) { Text("Cancel") } }
        )
    }

    activeTracker?.let { current ->
        val fresh = state.customTrackers.firstOrNull { it.stableUid == current.stableUid }
        if (fresh != null) {
            TrackerFolderWorkspaceDialog(
                tracker = fresh,
                state = state,
                settings = settings,
                palette = palette,
                viewModel = viewModel,
                hostCallbacks = hostCallbacks,
                onDismiss = { activeTracker = null }
            )
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = palette.surface,
            title = { Text("New Custom Tracker", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tracker Name (e.g. Goa Trip)") })
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Folder / Type") })
                    OutlinedTextField(value = budget, onValueChange = { budget = it }, label = { Text("Target Budget (${settings.currencySymbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createCustomTracker(name, category, budget.toDoubleOrNull() ?: 0.0, "", "")
                        name = ""; budget = ""; showCreate = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}
