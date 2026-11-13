package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.LiquidGlassSettings
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.local.CustomTrackerEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackerFolderWorkspaceDialog(
    tracker: CustomTrackerEntity,
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) } // 0=Entries, 1=Members & Settle, 2=Files & Settings
    var showQuickEntry by remember { mutableStateOf(false) }
    var entryType by remember { mutableStateOf("EXPENSE") }
    var entryIsRefund by remember { mutableStateOf(false) }
    var entryAmount by remember { mutableStateOf("") }
    var entryCategory by remember { mutableStateOf("Travel") }
    var entryNote by remember { mutableStateOf("") }

    var memberName by remember { mutableStateOf("") }
    var settleAmt by remember { mutableStateOf("") }
    var settleNote by remember { mutableStateOf("") }

    var editName by remember { mutableStateOf(tracker.name) }
    var editCat by remember { mutableStateOf(tracker.customTypeName) }
    var editBudget by remember { mutableStateOf(if (tracker.budgetAmount > 0) MoneyPaise.formatInput(MoneyPaise.toPaise(tracker.budgetAmount)) else "") }
    var editNotes by remember { mutableStateOf(tracker.notes) }

    val links = state.trackerLinks.filter { it.trackerUid == tracker.stableUid }
    val linkByTx = links.associateBy { it.transactionUid }
    val linkedTxns = state.transactions.filter { it.stableUid in linkByTx.keys }
    val members = state.trackerPeople.filter { it.trackerUid == tracker.stableUid }
    val settlements = state.trackerSettlements.filter { it.trackerUid == tracker.stableUid }
    val attachments = state.trackerAttachments.filter { it.trackerUid == tracker.stableUid }

    val grossOut = linkedTxns.filter { linkByTx[it.stableUid]?.isRefund != true && it.type in setOf("EXPENSE", "LEND") }.sumOf { it.amount }
    val refundsIn = linkedTxns.filter { linkByTx[it.stableUid]?.isRefund == true || it.type == "INCOME" }.sumOf { it.amount }
    val netTracked = (grossOut - refundsIn).coerceAtLeast(0.0)
    val budgetPct = if (tracker.budgetAmount > 0) (netTracked / tracker.budgetAmount).toFloat().coerceIn(0f, 1f) else 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${tracker.icon} ${tracker.name}", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Text("${tracker.customTypeName} · ${linkedTxns.size} entries · ${members.size} members", style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                }
                IconButton(onClick = {
                    val lines = buildList {
                        add("${tracker.icon} TRACKER FOLDER REPORT" to true)
                        add("Folder: ${tracker.name} (${tracker.customTypeName})" to false)
                        add("Net Spend: ${MoneyPaise.formatCurrency(netTracked)} | Refunds: ${MoneyPaise.formatCurrency(refundsIn)}" to true)
                        if (tracker.budgetAmount > 0) add("Budget: ${MoneyPaise.formatCurrency(tracker.budgetAmount)}" to false)
                        add("--- ENTRIES (${linkedTxns.size}) ---" to true)
                        linkedTxns.forEach { t ->
                            val tag = if (linkByTx[t.stableUid]?.isRefund == true) "REFUND" else t.type
                            add("$tag | ${t.category} | ${MoneyPaise.formatCurrency(t.amount)} | ${t.note}" to false)
                        }
                    }
                    hostCallbacks.printCustomReport("Tracmio_Tracker_${tracker.name.take(20)}", tracker.name, lines)
                }) {
                    Icon(Icons.Default.Print, contentDescription = "Export Tracker PDF", tint = palette.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceElevated)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Spend: ${displayAmount(netTracked, settings)}", color = palette.primary, fontWeight = FontWeight.Bold)
                        Text("Refunds: ${displayAmount(refundsIn, settings)}", color = palette.secondary, style = MaterialTheme.typography.labelMedium)
                    }
                    if (tracker.budgetAmount > 0) {
                        LinearProgressIndicator(
                            progress = { budgetPct },
                            color = if (budgetPct >= 0.9f) Color(0xFFEF4444) else palette.primary,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Text("Budget ${displayAmount(tracker.budgetAmount, settings)} (${(budgetPct * 100).toInt()}% used)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Entries (${linkedTxns.size})") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Members (${members.size})") })
                    FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("Files & Info") })
                }

                if (tab == 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showQuickEntry = !showQuickEntry; entryType = "EXPENSE"; entryIsRefund = false },
                            modifier = Modifier.weight(1f).testTag("tracker_add_entry_btn")
                        ) { Text(if (showQuickEntry) "Hide Form" else "+ Add Entry") }
                        val latestUnlinked = state.transactions.firstOrNull { it.stableUid !in linkByTx.keys }
                        if (latestUnlinked != null) {
                            OutlinedButton(
                                onClick = { viewModel.linkTransactionToTracker(tracker.stableUid, latestUnlinked.stableUid) },
                                modifier = Modifier.weight(1f)
                            ) { Text("+ Link Latest") }
                        }
                    }

                    if (showQuickEntry) {
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(palette.surfaceElevated).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = entryType == "EXPENSE", onClick = { entryType = "EXPENSE"; entryIsRefund = false }, label = { Text("Expense") })
                                FilterChip(selected = entryType == "INCOME", onClick = { entryType = "INCOME"; entryIsRefund = true }, label = { Text("Income/Refund") })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = entryIsRefund, onCheckedChange = { entryIsRefund = it })
                                Text("Treat as refund (reduces net)", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                            }
                            OutlinedTextField(
                                value = entryAmount,
                                onValueChange = { entryAmount = it },
                                label = { Text("Amount (${settings.currencySymbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(value = entryCategory, onValueChange = { entryCategory = it }, label = { Text("Category") })
                            OutlinedTextField(value = entryNote, onValueChange = { entryNote = it }, label = { Text("Note") })
                            Button(
                                onClick = {
                                    val amt = entryAmount.toDoubleOrNull() ?: return@Button
                                    val accId = state.accounts.firstOrNull { !it.isArchived }?.id ?: return@Button
                                    if (amt > 0 && entryCategory.isNotBlank()) {
                                        viewModel.saveOrUpdateTransaction(
                                            id = null,
                                            accountId = accId,
                                            type = entryType,
                                            amount = amt,
                                            category = entryCategory.trim(),
                                            note = entryNote.trim(),
                                            trackerUid = tracker.stableUid,
                                            isTrackerRefund = entryIsRefund
                                        )
                                        entryAmount = ""; entryNote = ""; showQuickEntry = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("tracker_save_entry_btn")
                            ) { Text("Save to Tracker") }
                        }
                    }

                    if (linkedTxns.isEmpty()) {
                        Text("No transactions linked yet.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    } else {
                        linkedTxns.forEach { tx ->
                            val isRef = linkByTx[tx.stableUid]?.isRefund == true
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(palette.surfaceElevated).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${tx.category}${if (isRef) " (Refund)" else ""}", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(tx.note.ifBlank { tx.paymentMethod }, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        (if (isRef || tx.type == "INCOME") "+" else "-") + displayAmount(tx.amount, settings),
                                        color = if (isRef || tx.type == "INCOME") palette.primary else Color(0xFFF43F5E),
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { viewModel.unlinkTransactionFromTracker(tracker.stableUid, tx.stableUid) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Unlink", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else if (tab == 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = memberName, onValueChange = { memberName = it }, label = { Text("Member Name") }, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            if (memberName.isNotBlank()) {
                                viewModel.upsertTrackerPerson(tracker.stableUid, memberName)
                                memberName = ""
                            }
                        }) { Text("Add") }
                    }
                    members.forEach { m ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(palette.surfaceElevated).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("👤 ${m.name}", color = palette.textPrimary)
                            IconButton(onClick = { viewModel.deleteTrackerPerson(m.personUid) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove member", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (members.size >= 2) {
                        Text("Record Settlement", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = settleAmt,
                                onValueChange = { settleAmt = it },
                                label = { Text("Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(value = settleNote, onValueChange = { settleNote = it }, label = { Text("Note") }, modifier = Modifier.weight(1f))
                        }
                        Button(onClick = {
                            val amt = settleAmt.toDoubleOrNull() ?: return@Button
                            if (amt > 0) {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                viewModel.addTrackerSettlement(tracker.stableUid, members[0].personUid, members[1].personUid, amt, today, settleNote)
                                settleAmt = ""; settleNote = ""
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Settle ${members[0].name} → ${members[1].name}") }
                    }
                    settlements.forEach { st ->
                        val fromN = members.firstOrNull { it.personUid == st.fromPersonUid }?.name ?: "Member"
                        val toN = members.firstOrNull { it.personUid == st.toPersonUid }?.name ?: "Member"
                        Text("• $fromN paid $toN ${displayAmount(st.amount, settings)} (${st.date})", style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = {
                            hostCallbacks.pickTrackerAttachment { fn, mime, path, size ->
                                viewModel.addTrackerAttachment(tracker.stableUid, fn, mime, path, size)
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" File/PDF")
                        }
                        OutlinedButton(onClick = {
                            hostCallbacks.captureReceiptPhoto { fn, mime, path, size ->
                                viewModel.addTrackerAttachment(tracker.stableUid, fn, mime, path, size)
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" Camera")
                        }
                    }
                    attachments.forEach { att ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(palette.surfaceElevated).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("📎 ${att.fileName}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteTrackerAttachment(att.attachmentUid) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete attachment", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Folder Name") })
                    OutlinedTextField(value = editCat, onValueChange = { editCat = it }, label = { Text("Category") })
                    OutlinedTextField(value = editBudget, onValueChange = { editBudget = it }, label = { Text("Budget") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = editNotes, onValueChange = { editNotes = it }, label = { Text("Folder Notes") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            viewModel.updateCustomTracker(tracker, editName, editCat, editBudget.toDoubleOrNull() ?: 0.0, tracker.startDate, tracker.endDate, editNotes)
                        }, modifier = Modifier.weight(1f)) { Text("Save Info") }
                        OutlinedButton(onClick = {
                            viewModel.archiveCustomTracker(tracker.stableUid, !tracker.isArchived)
                            onDismiss()
                        }, modifier = Modifier.weight(1f)) { Text(if (tracker.isArchived) "Unarchive" else "Archive") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
