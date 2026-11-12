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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.SubscriptionItem
import com.tracmio.app.data.local.AccountEntity

/**
 * Cash Denomination Counter Dialog (₹500, ₹200, ₹100, ₹50, ₹20, ₹10, ₹5, ₹2, ₹1)
 */
@Composable
fun CashDenominationDialog(
    initialCounts: Map<Int, Int>,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onSaveCounts: (Map<Int, Int>, totalAmount: Double) -> Unit
) {
    val counts = remember { mutableStateMapOf<Int, Int>().apply { putAll(initialCounts) } }
    val denoms = listOf(500, 200, 100, 50, 20, 10, 5, 2, 1)

    val total = denoms.sumOf { it * (counts[it] ?: 0) }.toDouble()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cash Denominations", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    MoneyPaise.formatCurrency(total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = palette.primary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(denoms) { denom ->
                    val count = counts[denom] ?: 0
                    val subTotal = denom * count

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x331E293B))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("₹$denom", fontWeight = FontWeight.Bold, color = Color.White)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (count > 0) counts[denom] = count - 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontSize = 20.sp, color = palette.textSecondary)
                            }

                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )

                            IconButton(
                                onClick = { counts[denom] = count + 1 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 20.sp, color = palette.primary)
                            }
                        }

                        Text(
                            text = "= ₹$subTotal",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveCounts(counts.toMap(), total)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
            ) {
                Text("Save Count", color = Color.Black, fontWeight = FontWeight.Bold)
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

/**
 * Transfer Funds Modal Dialog
 */
@Composable
fun TransferFundsDialog(
    accounts: List<AccountEntity>,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onConfirmTransfer: (fromAccountId: Long, toAccountId: Long, amount: Double) -> Unit
) {
    var fromAccountId by remember { mutableLongStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var toAccountId by remember { mutableLongStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 1L) }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Transfer Funds", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("From Account:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                accounts.take(3).forEach { acc ->
                    val isSelected = acc.id == fromAccountId
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) palette.primary.copy(alpha = 0.2f) else Color(0x331E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) palette.primary else Color(0x22FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fromAccountId = acc.id }
                    ) {
                        Text(
                            text = "${acc.name} (${MoneyPaise.formatCurrency(acc.balance)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) palette.primary else Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("To Account:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                accounts.take(3).forEach { acc ->
                    val isSelected = acc.id == toAccountId
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) palette.secondary.copy(alpha = 0.2f) else Color(0x331E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) palette.secondary else Color(0x22FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toAccountId = acc.id }
                    ) {
                        Text(
                            text = "${acc.name} (${MoneyPaise.formatCurrency(acc.balance)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) palette.secondary else Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Transfer Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && fromAccountId != toAccountId) {
                        onConfirmTransfer(fromAccountId, toAccountId, amt)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
            ) {
                Text("Transfer Now", color = Color.Black, fontWeight = FontWeight.Bold)
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

/**
 * Subscriptions Autopilot Screen
 */
@Composable
fun SubscriptionsScreen(
    subscriptions: List<SubscriptionItem>,
    settings: LiquidGlassSettings,
    onBack: () -> Unit,
    onAddSubscription: (SubscriptionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    val monthlyTotal = subscriptions.sumOf {
        when (it.billingCycle.uppercase()) {
            "YEARLY" -> it.amount / 12.0
            "WEEKLY" -> it.amount * 4.33
            else -> it.amount
        }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "💳 Subscriptions Autopilot",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Track recurring Netflix, Spotify, Prime & gym renewals",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // Monthly Commitment Hero
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0E182A),
                borderColor = palette.primary.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("TOTAL MONTHLY COMMITMENT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(MoneyPaise.formatCurrency(monthlyTotal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                    Text("${subscriptions.size} active memberships on autopilot", style = MaterialTheme.typography.bodySmall, color = palette.primary)
                }
            }
        }

        items(subscriptions, key = { it.id }) { sub ->
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
                    Column {
                        Text(sub.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${sub.billingCycle} • Next: ${sub.nextBillingDate}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }

                    Text(MoneyPaise.formatCurrency(sub.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                }
            }
        }
    }
}

/**
 * Financial X-Ray Screen (50/30/20 rule, runway, EMI pre-payment)
 */
@Composable
fun FinancialXRayScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    val income = state.totalIncome.coerceAtLeast(1.0)
    val expense = state.totalExpense
    val savings = (income - expense).coerceAtLeast(0.0)

    val needsEst = expense * 0.60
    val wantsEst = expense * 0.40

    val needsPct = (needsEst / income).coerceIn(0.0, 1.0)
    val wantsPct = (wantsEst / income).coerceIn(0.0, 1.0)
    val savingsPct = (savings / income).coerceIn(0.0, 1.0)

    val monthlyBurn = expense.coerceAtLeast(1000.0)
    val runwayMonths = (state.totalBalance / monthlyBurn).coerceAtLeast(0.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "🩺 Financial X-Ray",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "50/30/20 rule health analysis & emergency runway",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // 50/30/20 Rule Breakdown
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0E182A),
                borderColor = palette.primary.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("50/30/20 BUDGET HEALTH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.textSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    XRayRuleRow(label = "Needs (Target ≤ 50%)", actualPct = (needsPct * 100).toInt(), target = "50%", amount = needsEst, color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(8.dp))
                    XRayRuleRow(label = "Wants (Target ≤ 30%)", actualPct = (wantsPct * 100).toInt(), target = "30%", amount = wantsEst, color = Color(0xFFF43F5E))
                    Spacer(modifier = Modifier.height(8.dp))
                    XRayRuleRow(label = "Savings (Target ≥ 20%)", actualPct = (savingsPct * 100).toInt(), target = "20%", amount = savings, color = Color(0xFF10B981))
                }
            }
        }

        // Emergency Fund Runway
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0F192C)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EMERGENCY FUND RUNWAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%.1f Months of Freedom", runwayMonths),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = palette.primary
                    )
                    Text("Based on current total balance vs. average monthly spending burn rate.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            }
        }
    }
}

@Composable
fun XRayRuleRow(label: String, actualPct: Int, target: String, amount: Double, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
            Text("$actualPct% (Target $target) • ${MoneyPaise.formatCurrency(amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x331E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((actualPct / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
