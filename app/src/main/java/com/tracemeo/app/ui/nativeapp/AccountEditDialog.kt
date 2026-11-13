package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.local.AccountEntity

@Composable
fun EditAccountDialog(
    account: AccountEntity,
    currencySymbol: String,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit,
    onArchive: (AccountEntity) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var type by remember { mutableStateOf(account.accountType) }
    var balance by remember { mutableStateOf(MoneyPaise.formatInput(MoneyPaise.toPaise(account.balance))) }
    var institution by remember { mutableStateOf(account.institution) }
    var accNumber by remember { mutableStateOf(account.accountNumber) }
    var notes by remember { mutableStateOf(account.notes) }
    var ccLimit by remember { mutableStateOf(MoneyPaise.formatInput(MoneyPaise.toPaise(account.creditLimit))) }
    var billDay by remember { mutableStateOf(account.billingCycleDay.toString()) }
    var dueDay by remember { mutableStateOf(account.dueDay.toString()) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    val types = listOf("Bank", "Cash", "Wallet", "Savings", "Credit Card")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("✏️ Edit ${account.name}", color = palette.textPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") })
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        FilterChip(selected = type.equals(t, true), onClick = { type = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Opening Balance ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(value = institution, onValueChange = { institution = it }, label = { Text("Bank / Institution") })
                OutlinedTextField(value = accNumber, onValueChange = { accNumber = it }, label = { Text("Last 4 Digits") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
                if (type.contains("Credit", true)) {
                    OutlinedTextField(
                        value = ccLimit,
                        onValueChange = { ccLimit = it },
                        label = { Text("Credit Limit ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = billDay, onValueChange = { billDay = it }, label = { Text("Bill Day") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = dueDay, onValueChange = { dueDay = it }, label = { Text("Due Day") }, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedButton(
                    onClick = { showArchiveConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth().testTag("archive_account_btn")
                ) {
                    Text("Archive Account")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            account.copy(
                                name = name.trim(),
                                accountType = type,
                                balance = balance.toDoubleOrNull() ?: account.balance,
                                institution = institution.trim(),
                                accountNumber = accNumber.trim(),
                                notes = notes.trim(),
                                creditLimit = ccLimit.toDoubleOrNull() ?: account.creditLimit,
                                billingCycleDay = (billDay.toIntOrNull() ?: account.billingCycleDay).coerceIn(1, 28),
                                dueDay = (dueDay.toIntOrNull() ?: account.dueDay).coerceIn(1, 28)
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_edit_account_btn")
            ) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            containerColor = palette.surface,
            title = { Text("Archive ${account.name}?", color = palette.textPrimary) },
            text = {
                Text(
                    "Archiving hides this wallet from active lists while preserving its historical transactions.",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    onClick = {
                        showArchiveConfirm = false
                        onArchive(account)
                    },
                    modifier = Modifier.testTag("confirm_archive_account_btn")
                ) { Text("Archive Wallet") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text("Keep Active") }
            }
        )
    }
}
