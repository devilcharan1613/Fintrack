package com.tracmio.app.ui.nativeapp

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.PersonLedgerSummary
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.TransactionEntity

@Composable
fun ReportsAndIntelligenceScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    var selectedChartStyle by remember { mutableStateOf("3D Neon Bars") }
    var selectedFilterCategory by remember { mutableStateOf<String?>(null) }

    val expenses = remember(state.transactions) {
        state.transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
    }
    val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }

    val categoryBreakdown = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val filteredTransactions = remember(state.transactions, selectedFilterCategory) {
        if (selectedFilterCategory == null) emptyList()
        else state.transactions.filter { it.category.equals(selectedFilterCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Reports & Intelligence",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Interactive breakdown, person summaries & account shares",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        // Chart Style Selector Chips (Requirement #7)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("3D Neon Bars", "Wave & Flow", "Gradient Capsules", "Executive Minimalist")) { style ->
                    val isSelected = selectedChartStyle == style
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) palette.primary else Color(0x331E293B),
                        modifier = Modifier.clickable { selectedChartStyle = style }
                    ) {
                        Text(
                            text = style,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Visual Graph Canvas
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0D172A),
                borderColor = palette.primary.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cashflow & Spending Wave ($selectedChartStyle)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val w = size.width
                        val h = size.height

                        // Draw baseline
                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = Offset(0f, h - 10f),
                            end = Offset(w, h - 10f),
                            strokeWidth = 2f
                        )

                        if (categoryBreakdown.isNotEmpty()) {
                            val maxVal = (categoryBreakdown.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
                            val barCount = categoryBreakdown.take(6).size
                            val barWidth = (w / (barCount * 2)).coerceIn(16f, 48f)

                            categoryBreakdown.take(6).forEachIndexed { idx, pair ->
                                val x = (idx * 2 + 1) * (w / (barCount * 2))
                                val barHeight = ((pair.second / maxVal) * (h - 30f)).toFloat().coerceAtLeast(10f)

                                when (selectedChartStyle) {
                                    "Wave & Flow" -> {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(palette.primary, palette.secondary.copy(alpha = 0.3f))
                                            ),
                                            center = Offset(x, h - barHeight - 10f),
                                            radius = barWidth * 0.7f
                                        )
                                    }
                                    "Gradient Capsules" -> {
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(palette.primary, palette.secondary)
                                            ),
                                            topLeft = Offset(x - barWidth / 2, h - barHeight - 10f),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                                        )
                                    }
                                    else -> {
                                        // 3D Neon Bars
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(palette.primary, Color(0xFF10B981))
                                            ),
                                            topLeft = Offset(x - barWidth / 2, h - barHeight - 10f),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(6f, 6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Category Breakdown (with interactive "Tap row to filter", Requirement #7)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                backgroundColor = Color(0xCC101A2E),
                borderColor = palette.primary.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tap any row below to filter period transactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary
                            )
                        }

                        if (selectedFilterCategory != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x22F43F5E),
                                modifier = Modifier.clickable { selectedFilterCategory = null }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear Filter", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF43F5E))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (categoryBreakdown.isEmpty()) {
                        Text("No expense records yet.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    } else {
                        categoryBreakdown.forEach { (cat, amount) ->
                            val pct = if (totalExpense > 0) (amount / totalExpense) else 0.0
                            val isSelected = selectedFilterCategory.equals(cat, ignoreCase = true)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) palette.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        selectedFilterCategory = if (isSelected) null else cat
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = getCategoryIcon(cat), fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) palette.primary else Color.White
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${(pct * 100).toInt()}% • ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                        Text(
                                            text = MoneyPaise.formatCurrency(amount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Progress share bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0x331E293B))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(pct.toFloat())
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isSelected) palette.primary else palette.secondary)
                                    )
                                }
                            }
                        }
                    }

                    // Display filtered transactions when a category is tapped (Requirement #7)
                    if (selectedFilterCategory != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Filtered Transactions ($selectedFilterCategory: ${filteredTransactions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        filteredTransactions.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33000000))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tx.note.ifBlank { tx.category },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                                Text(
                                    text = MoneyPaise.formatCurrency(tx.amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF43F5E)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Person Summary (Borrowed vs Lent comparison graphs, Requirement #7)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0E1A2D),
                borderColor = palette.primary.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Person Summary & Split Balances",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Borrowed vs. Lent breakdown with net settlement status",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.personSummaries.isEmpty()) {
                        Text("No person loan or split entries yet.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    } else {
                        state.personSummaries.forEach { ps ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(ps.person.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    val net = ps.net
                                    val statusColor = if (net > 0) Color(0xFF10B981) else if (net < 0) Color(0xFFF43F5E) else palette.textSecondary
                                    val statusText = if (net > 0) "You Get ${MoneyPaise.formatCurrency(net)}" else if (net < 0) "You Owe ${MoneyPaise.formatCurrency(-net)}" else "Settled"

                                    Text(statusText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = statusColor)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Dual Borrowed vs Lent Progress bars (Requirement #7)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Lent bar (green)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Lent: ${MoneyPaise.formatCurrency(ps.lent)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7F3D0))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF10B981))
                                        )
                                    }

                                    // Borrowed bar (red)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Borrowed: ${MoneyPaise.formatCurrency(ps.borrowed)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFECDD3))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFFF43F5E))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Account Balances (Distribution & Share bars, Requirement #7)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0F192C),
                borderColor = palette.primary.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Account Balances & Distribution Share",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Share of liquid wealth across accounts",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val totalBal = state.totalBalance.coerceAtLeast(1.0)
                    state.accounts.forEach { acc ->
                        val share = (acc.balance / totalBal).coerceIn(0.0, 1.0)

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${acc.name} (${acc.accountType})", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                Text("${(share * 100).toInt()}% • ${MoneyPaise.formatCurrency(acc.balance)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = palette.primary)
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
                                        .fillMaxWidth(share.toFloat())
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.primary)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
