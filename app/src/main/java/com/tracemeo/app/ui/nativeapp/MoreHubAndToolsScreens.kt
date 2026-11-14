package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracemeo.app.data.NativeTracmioUiState

data class ToolCatalogItem(
    val id: String,
    val route: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accent: Color
)

val ALL_TOOLS_CATALOG: List<ToolCatalogItem> = listOf(
    ToolCatalogItem("khata", "khata", "📓", "Khata / Pay Later", "Shop & friend tabs", Color(0xFF00F5D4)),
    ToolCatalogItem("recharge", "recharge", "📶", "SIM & WiFi Recharge", "28 / 56 / 84d validity", Color(0xFF38BDF8)),
    ToolCatalogItem("refunds", "refunds", "📦", "Pending Refunds", "Returns & order credits", Color(0xFF34D399)),
    ToolCatalogItem("wishlist", "wishlist", "🧊", "24h Cooling Wishlist", "Impulse pause & save", Color(0xFFA855F7)),
    ToolCatalogItem("cashback", "cashback", "🎁", "Cashback & Rewards", "UPI & card rewards log", Color(0xFFFBBF24)),
    ToolCatalogItem("price_per_use", "price_per_use", "🧮", "Price-Per-Use Calc", "Value per real usage", Color(0xFF22D3EE)),
    ToolCatalogItem("round_up", "round_up", "🪙", "Round-Up Change Jar", "Spare change to goals", Color(0xFF10B981)),
    ToolCatalogItem("runway", "runway", "🛟", "Emergency Runway", "Months survival meter", Color(0xFF38BDF8)),
    ToolCatalogItem("debt_planner", "debt_planner", "📉", "Debt Payoff Planner", "Snowball vs Avalanche", Color(0xFFFB7185)),
    ToolCatalogItem("warranty_vault", "warranty_vault", "🛡️", "Receipt Warranty Vault", "Deadlines & return dates", Color(0xFF00F5D4)),
    ToolCatalogItem("reports", "reports", "📈", "Reports & Radar", "4 chart styles & UPI", Color(0xFF00F5D4)),
    ToolCatalogItem("subscriptions", "subscriptions", "🔁", "Subscriptions", "Recurring monthly bills", Color(0xFFA855F7)),
    ToolCatalogItem("financial_xray", "financial_xray", "🩻", "Financial X-Ray", "10-point health score", Color(0xFF34D399)),
    ToolCatalogItem("money_story", "money_story", "📖", "Money Story", "Monthly cash narrative", Color(0xFF38BDF8)),
    ToolCatalogItem("global_search", "global_search", "🔍", "Global Search", "Ledger, #tags & tools", Color(0xFFFBBF24)),
    ToolCatalogItem("spending_dna", "spending_dna", "🧬", "Spending DNA", "50 / 30 / 20 breakdown", Color(0xFFC084FC)),
    ToolCatalogItem("calendar", "calendar", "🔥", "Calendar & Streak", "No-spend milestones", Color(0xFFFB7185)),
    ToolCatalogItem("reminders", "reminders", "⏰", "9 PM Summary & Alerts", "Daily & event alerts", Color(0xFFFBBF24)),
    ToolCatalogItem("time_machine", "time_machine", "⏳", "Time Machine", "Compound wealth sim", Color(0xFF00F5D4)),
    ToolCatalogItem("data_export", "data_export", "💾", "Full JSON Backup", "AES-GCM, XLSX, CSV, PDF", Color(0xFF34D399)),
    ToolCatalogItem("app_lock", "app_lock", "🔐", "App Lock & Biometrics", "PIN, biometrics & key", Color(0xFFFB7185)),
    ToolCatalogItem("permissions", "permissions", "🛡️", "Offline Security Audit", "Zero internet & DB check", Color(0xFF38BDF8)),
    ToolCatalogItem("custom_trackers", "custom_trackers", "🎯", "Custom Trackers", "Challenges & folders", Color(0xFFA855F7)),
    ToolCatalogItem("themes", "themes", "🎨", "Liquid Glass UI Studio", "Themes, mesh & scale", Color(0xFF00F5D4))
)

@Composable
fun MoreScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onNavigate: (String) -> Unit
) {
    val primaryCards = listOf(
        ToolCatalogItem("budgets", "budgets", "📊", "Planning & Budgets", "Budgets, Runway & Goals", palette.primary),
        ToolCatalogItem("people", "people", "👥", "People & Khata", "Smart Split & Balances", Color(0xFFA855F7)),
        ToolCatalogItem("cash_counter", "cash_counter", "💵", "3-Step Cash Counter", "Notes & Coins Reconcile", Color(0xFF34D399)),
        ToolCatalogItem("guide", "guide", "📘", "App Guide & Manual", "Quick-jump help directory", Color(0xFF38BDF8))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("more_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "More & Control Center",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary
            )
        }

        // Dedicated Single Tools Hub Folder inside More
        item {
            GlassCard(
                settings = settings,
                shape = RoundedCornerShape(24.dp),
                borderColor = palette.primary.copy(alpha = 0.55f),
                onClick = { onNavigate("tools_hub") },
                modifier = Modifier.fillMaxWidth().testTag("open_tools_hub_folder")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    palette.primary.copy(alpha = 0.22f),
                                    palette.surfaceElevated.copy(alpha = 0.92f),
                                    palette.secondary.copy(alpha = 0.16f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            GlassBadge3D(emoji = "🗂️", accent = palette.primary, sizeDp = 48.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "Tools Hub",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    "All 24 offline calculators, security, trackers & studio tools",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.primary.copy(alpha = 0.2f),
                            modifier = Modifier.border(1.dp, palette.primary, RoundedCornerShape(14.dp))
                        ) {
                            Text(
                                "24 Tools →",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Primary 2-Column Uniform Height Feature Cards
        items(primaryCards.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { card ->
                    GlassCard(
                        settings = settings,
                        onClick = { onNavigate(card.route) },
                        modifier = Modifier.weight(1f).fillMaxHeight().testTag("more_card_${card.id}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            GlassBadge3D(emoji = card.emoji, accent = card.accent, sizeDp = 40.dp)
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    card.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    card.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ToolsHubFolderScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var organizeMode by remember { mutableStateOf(false) }

    val orderedTools = remember(state.pinnedToolIds) {
        val pinnedSet = state.pinnedToolIds.toSet()
        val pinned = ALL_TOOLS_CATALOG.filter { it.id in pinnedSet }
        val rest = ALL_TOOLS_CATALOG.filterNot { it.id in pinnedSet }
        pinned + rest
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("tools_hub_folder_grid"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("tools_hub_back_button")
                    ) {
                        Text("← Back", color = palette.primary, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text(
                            "Tools Hub",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Text(
                            "${orderedTools.size} offline tools",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (organizeMode) palette.primary else palette.surfaceElevated,
                    modifier = Modifier
                        .border(1.dp, palette.primary, RoundedCornerShape(14.dp))
                        .clickable { organizeMode = !organizeMode }
                        .testTag("tools_hub_organize_button")
                ) {
                    Text(
                        if (organizeMode) "✓ Done" else "⇄ Organize",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (organizeMode) palette.background else palette.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        if (organizeMode) {
            item {
                LiquidGlassComponents(settings = settings, palette = palette) {
                    Text(
                        "Tap any tool below to Pin / Unpin it to the top of Tools Hub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        items(orderedTools.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { tool ->
                    val isPinned = state.pinnedToolIds.contains(tool.id)
                    GlassCard(
                        settings = settings,
                        borderColor = if (isPinned) palette.primary else null,
                        onClick = {
                            if (organizeMode) {
                                viewModel.toggleToolPin(tool.id)
                            } else {
                                onNavigate(tool.route)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("tool_card_${tool.id}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                GlassBadge3D(emoji = tool.emoji, accent = tool.accent, sizeDp = 40.dp)
                                if (isPinned || organizeMode) {
                                    Text(
                                        if (isPinned) "📌" else "＋",
                                        fontSize = 14.sp,
                                        color = if (isPinned) palette.primary else palette.textSecondary
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    tool.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    tool.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun InAppGuideScreen(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val guideSections = remember {
        listOf(
            Triple("🏠 Home & Quick Spend", "Check Net Balance, Monthly Pace, Safe-to-Spend Today, and tap chips to log instant expenses.", "dashboard"),
            Triple("🏦 Accounts & Allocations", "Create Bank, Cash, Wallet, or Credit Card accounts. Use 1-Tap ATM withdrawal, Auto-Split %, and >30% CC utilization alerts.", "accounts"),
            Triple("🧾 Transactions & Hashtags", "Use Top-Left Filter for Account/Date, #hashtags for trip/event totals, Half-Cash + Half-UPI Split, and 1-Tap Repeat (↻).", "transactions"),
            Triple("💵 3-Step Cash Counter", "Count physical notes (₹500–₹10) and coins (₹20–₹1) and reconcile directly into Cash Wallet.", "cash_counter")
        ) + ALL_TOOLS_CATALOG.map {
            Triple("${it.emoji} ${it.title}", it.subtitle, it.route)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("app_guide_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(14.dp)) {
                    Text("← Back", color = palette.primary, fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text("App Guide & Manual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                    Text("Quick-jump directory for every Tracmio v16.13 feature", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                }
            }
        }

        items(guideSections) { (title, desc, route) ->
            LiquidGlassComponents(settings = settings, palette = palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onNavigate(route) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("guide_open_$route")
                    ) {
                        Text("Open →", color = palette.background, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
