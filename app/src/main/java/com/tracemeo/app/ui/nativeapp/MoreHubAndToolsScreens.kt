package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.LiquidGlassSettings

data class ToolHubEntry(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accent: Color
)

val ALL_TOOLS_CATALOG = listOf(
    ToolHubEntry("khata", "📓", "Khata / Pay Later", "Canteen, kirana & chai-stall tabs with 1-tap Clear Khata", Color(0xFFF59E0B)),
    ToolHubEntry("recharge", "📶", "SIM & WiFi Recharge", "28 / 56 / 84-day Jio, Airtel, Vi & broadband countdowns", Color(0xFF06B6D4)),
    ToolHubEntry("refunds", "📦", "Pending Refunds & Returns", "Track Amazon/Flipkart returns & friend refunds", Color(0xFF10B981)),
    ToolHubEntry("wishlist", "⏳", "24h Cooling Wishlist", "Pause impulse buys for 24h before unlocking Buy Now", Color(0xFFEC4899)),
    ToolHubEntry("cashback", "🎁", "Cashback & Rewards", "Log UPI scratch cards, credit points & wallet rewards", Color(0xFF14B8A6)),
    ToolHubEntry("price_per_use", "🏷️", "Price-Per-Use Calculator", "Track cost per use on gadgets, shoes & subscriptions", Color(0xFF8B5CF6)),
    ToolHubEntry("round_up", "🪙", "Round-Up Spare Change Jar", "Auto-calculate spare change from daily spends into goals", Color(0xFF00D6A3)),
    ToolHubEntry("runway", "🛟", "Emergency Fund Runway", "Months of living expenses covered by liquid cash", Color(0xFF3B82F6)),
    ToolHubEntry("debt_planner", "📉", "Debt Payoff Planner", "Compare Snowball vs. Avalanche payoff months & interest", Color(0xFFF43F5E)),
    ToolHubEntry("warranty_vault", "🧾", "Receipt Warranty Vault", "Track bill expiry countdowns & warranty attachments", Color(0xFF6366F1)),
    ToolHubEntry("reports", "📊", "Reports & Radar", "4 Charts, UPI, Weekend Radar & Month Compare", Color(0xFF00D6A3)),
    ToolHubEntry("subscriptions", "🔄", "Subscriptions", "Recurring monthly/yearly bills & 1-tap Pay", Color(0xFF06B6D4)),
    ToolHubEntry("xray", "🩻", "Financial X-Ray", "0–100 health score ring & 10 diagnostic vitals", Color(0xFF3B82F6)),
    ToolHubEntry("story", "📖", "Money Story", "Your 50/30/20 cashflow narrative & milestones", Color(0xFFA855F7)),
    ToolHubEntry("global_search", "🔍", "Global Search", "Search transactions, accounts, goals & people", Color(0xFFF59E0B)),
    ToolHubEntry("dna", "🧬", "Spending DNA", "Weekday vs weekend money personality", Color(0xFFEC4899)),
    ToolHubEntry("calendar", "📅", "Calendar & Streak", "No-spend streak & academic fee deadlines", Color(0xFF10B981)),
    ToolHubEntry("reminders", "🔔", "9 PM Summary & Reminders", "Daily spend check-in & monthly backup alerts", Color(0xFFFB923C)),
    ToolHubEntry("time_machine", "⏳", "Time Machine", "Rewind or project your net worth across time", Color(0xFF6366F1)),
    ToolHubEntry("data_export", "💾", "Full JSON Backup", "Plain/AES-GCM JSON, XLSX, CSV & PDF Data Vault", Color(0xFF14B8A6)),
    ToolHubEntry("app_lock", "🛡️", "App Lock & Biometrics", "Biometric launch lock, Liquid Glass PIN & Recovery Key", Color(0xFFF43F5E)),
    ToolHubEntry("permissions", "✅", "Offline Security Audit", "Verify zero network permissions & device grants", Color(0xFF22C55E)),
    ToolHubEntry("trackers", "🗂️", "Custom Trackers", "Trips, events & projects with receipt attachments", Color(0xFF8B5CF6)),
    ToolHubEntry("themes", "🎨", "Liquid Glass UI Studio", "11 Themes, 10 Wallpapers, Scaling & Parallax", Color(0xFF00A9C7))
)

@Composable
fun MoreScreen(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    onNavigateRoute: (String) -> Unit,
    onOpenCashCounter: () -> Unit
) {
    val activeToolCount = remember(settings.toolsHubIds) {
        ALL_TOOLS_CATALOG.count { it.id in settings.toolsHubIds }.coerceAtLeast(1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "More & Menu",
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Access core workspaces, or open the Tools Hub folder for all ${ALL_TOOLS_CATALOG.size} utility tools.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )

        // Single Tools Hub Folder Box alongside the other Menu boxes
        GlassCard(
            settings = settings,
            borderColor = palette.primary.copy(alpha = 0.55f),
            onClick = { onNavigateRoute("tools_hub") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inline_tools_hub")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("more_card_tools_hub"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    GlassBadge3D(emoji = "🧰", accent = palette.primary, sizeDp = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Tools Hub Folder",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "$activeToolCount active tools · Khata, SIM Recharge, Refunds, Wishlist, Backup, Studio & more",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = "Open Tools Hub Folder", tint = palette.primary)
            }
        }

        val primaryCards = listOf(
            Triple("planning", "🎯", "Planning, Budgets, Goals & Debt Planner"),
            Triple("people", "👥", "People, Ledgers, Khata & Smart Split WhatsApp"),
            Triple("cash_count", "💵", "3-Step Cash Counter (Notes & Coins)"),
            Triple("guide", "📘", "App Guide & Manual (All v16.13 Features)")
        )
        primaryCards.forEach { (route, emoji, label) ->
            GlassCard(
                settings = settings,
                onClick = { if (route == "cash_count") onOpenCashCounter() else onNavigateRoute(route) },
                modifier = Modifier.fillMaxWidth().testTag("more_card_$route")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        GlassBadge3D(emoji = emoji, accent = palette.secondary, sizeDp = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = palette.textSecondary)
                }
            }
        }
    }
}

@Composable
fun ToolsHubFolderScreen(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    onNavigateRoute: (String) -> Unit,
    onBack: () -> Unit
) {
    var organizeMode by remember { mutableStateOf(false) }

    val quickTools = remember(settings.toolsHubIds) {
        val pinned = ALL_TOOLS_CATALOG.filter { it.id in settings.toolsHubIds }
        pinned.ifEmpty { ALL_TOOLS_CATALOG }
    }
    val extraTools = remember(settings.toolsHubIds) {
        ALL_TOOLS_CATALOG.filter { it.id !in settings.toolsHubIds }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp)
            .testTag("tools_hub_folder_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).testTag("tools_hub_back_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to More",
                        tint = palette.textPrimary
                    )
                }
                Column {
                    Text(
                        "🧰 Tools Hub",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "${quickTools.size} utilities in folder · Tap any tool to launch",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
            OutlinedButton(
                onClick = { organizeMode = !organizeMode },
                modifier = Modifier.testTag("tools_hub_organize_btn")
            ) {
                Text(if (organizeMode) "Done" else "⇄ Organize")
            }
        }

        if (organizeMode) {
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Check tools to pin inside your active 2-column Tools Hub folder grid, or uncheck to tuck them below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    ALL_TOOLS_CATALOG.forEach { tool ->
                        val pinned = tool.id in settings.toolsHubIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleToolInHub(tool.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(tool.emoji, modifier = Modifier.width(28.dp))
                                Column {
                                    Text(tool.title, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(tool.subtitle, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                            }
                            Checkbox(
                                checked = pinned,
                                onCheckedChange = { viewModel.toggleToolInHub(tool.id) }
                            )
                        }
                    }
                }
            }
        } else {
            quickTools.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { tool ->
                        GlassCard(
                            settings = settings,
                            onClick = { onNavigateRoute(tool.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("tool_hub_item_${tool.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlassBadge3D(emoji = tool.emoji, accent = tool.accent, sizeDp = 36.dp)
                                    Text(
                                        tool.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                               Spacer(Modifier.height(4.dp))
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
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (extraTools.isNotEmpty()) {
                GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Unpinned Tools (${extraTools.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        extraTools.forEach { tool ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateRoute(tool.id) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${tool.emoji} ${tool.title}", color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = palette.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class GuideItem(
    val title: String,
    val group: String,
    val whatItDoes: String,
    val howToUse: String,
    val kidExample: String,
    val route: String
) {
    val description: String
        get() = "$whatItDoes $howToUse Example: $kidExample"
}

val ALL_GUIDE_FEATURES = listOf(
    GuideItem(
        title = "🧰 Dedicated Tools Hub Folder & ⇄ Organize",
        group = "Navigation",
        whatItDoes = "A neat toy-box folder inside More that holds all 24 helper tools in one tidy place.",
        howToUse = "Tap More → Tools Hub Folder. Tap any tool card to open it, ← Back to leave, or ⇄ Organize to pick your favorites.",
        kidExample = "Instead of scattering 24 crayons all over the floor, you keep them inside one neat crayon box and open it whenever you want to color!",
        route = "tools_hub"
    ),
    GuideItem(
        title = "📊 Summary Dashboard Category Chart (Donut & Bar)",
        group = "Dashboard",
        whatItDoes = "A colorful donut ring and bar picture showing where your money went this week or month.",
        howToUse = "On the Home screen, tap '🍩 Donut' or '📊 Bar', and switch between 'Weekly (7d)' and 'Monthly'. Tap any category row to highlight it.",
        kidExample = "Like looking at a pizza to see how many slices went to Snacks, Toys, and Bus Rides!",
        route = "dashboard"
    ),
    GuideItem(
        title = "🛡️ Safe-to-Spend Daily Limit",
        group = "Dashboard",
        whatItDoes = "Tells you how much money you can safely spend today without running out before the month ends.",
        howToUse = "Look at the top green banner on the Home screen every morning before buying anything.",
        kidExample = "If you have 10 chocolates for 5 days, you can eat 2 chocolates today and still have enough for tomorrow!",
        route = "dashboard"
    ),
    GuideItem(
        title = "⚡ 1-Tap Quick Spend Chips (with Undo)",
        group = "Dashboard",
        whatItDoes = "Magic 1-tap buttons for tiny things you buy almost every day.",
        howToUse = "Tap a chip like '☕ Chai ₹20' on Home to log it in 1 tap (tap UNDO on the bottom bar if you tapped by mistake!), or tap '+ Chip' to make your own.",
        kidExample = "Like a vending machine button — one press logs your ₹20 evening tea without typing anything!",
        route = "dashboard"
    ),
    GuideItem(
        title = "🧮 Inline Math in the Amount Box",
        group = "Transactions",
        whatItDoes = "A built-in mini calculator right inside the money box when adding an expense.",
        howToUse = "Open '+ Expense', type '120+45+30' or '50*3' in the Amount box, and tap '=' (or move to the next box) to turn it into ₹195 automatically.",
        kidExample = "You bought a ₹20 biscuit, ₹15 chips, and ₹10 candy. Just type 20+15+10 and Tracmio writes ₹45 for you!",
        route = "transactions"
    ),
    GuideItem(
        title = "⚡ Quick-Filter Pills on Dashboard",
        group = "Dashboard",
        whatItDoes = "One-tap filter buttons above Recent Activity to see only Today, Yesterday, Cash, or UPI spends.",
        howToUse = "Scroll to Recent Activity on Home and tap 'All', 'Today', 'Yesterday', 'Cash', or 'UPI'.",
        kidExample = "Like sorting your Lego bricks so you only see the red ones from today!",
        route = "dashboard"
    ),
    GuideItem(
        title = "🌙 End-of-Day 'Anything Spent Today?' & 9 PM Summary",
        group = "Dashboard",
        whatItDoes = "A friendly bedtime check-in card and 9 PM reminder so you never forget to write down today's spends.",
        howToUse = "If you haven't logged anything today, tap '+ Log Spend' on the Home card, or configure your 9 PM alert in Reminders.",
        kidExample = "Like your mom asking at 9 PM: 'Did you spend any pocket money today?' and telling you how much is left!",
        route = "reminders"
    ),
    GuideItem(
        title = "🏧 1-Tap ATM Withdrawal (Bank → Cash)",
        group = "Accounts",
        whatItDoes = "Moves money from your Bank account into your Cash Wallet in a single tap.",
        howToUse = "Open Accounts and tap '🏧 ₹500', '🏧 ₹1,000', or '🏧 ₹2,000' on the ATM Withdrawal card.",
        kidExample = "When you take ₹500 paper notes out of the ATM machine, your Bank goes down by ₹500 and your pocket wallet goes up by ₹500!",
        route = "accounts"
    ),
    GuideItem(
        title = "💵+📱 Half-Cash + Half-UPI Split Payment",
        group = "Transactions",
        whatItDoes = "Lets you pay one single bill using partly paper Cash and partly phone UPI.",
        howToUse = "In Add Transaction, tap '💵+📱 Cash + UPI Split', type the Cash part and UPI part, and tap Save.",
        kidExample = "A pizza costs ₹500. You only have two ₹100 notes (₹200 Cash), so you pay the other ₹300 by GPay. Tracmio deducts ₹200 from Cash and ₹300 from Bank at once!",
        route = "transactions"
    ),
    GuideItem(
        title = "📓 Friend / Shopkeeper 'Khata' (Pay Later) Tab",
        group = "People",
        whatItDoes = "A small notebook for things you take today from a shop or friend and promise to pay for later.",
        howToUse = "Open People → '📓 Khata Pay Later' (or Tools Hub → Khata). Tap '+ Khata' to add an item, Edit/Delete anytime, or tap 'Clear Khata ✓' on payday.",
        kidExample = "You drink ₹30 tea at Raju's stall on Monday and Tuesday without paying cash yet. On Saturday you tap 'Clear Khata ✓' and pay Raju ₹60 in one go!",
        route = "khata"
    ),
    GuideItem(
        title = "📱 Saved UPI ID & QR Card per Person",
        group = "People",
        whatItDoes = "Saves your friend's UPI address and QR photo right inside their profile card.",
        howToUse = "Open People, tap a friend (or '+ Person'), save their UPI ID (like ravi@okaxis), and tap 'Copy UPI ID' whenever you pay them.",
        kidExample = "Like keeping your best friend's phone number on speed-dial so you never have to ask 'Hey, what's your UPI ID again?'",
        route = "people"
    ),
    GuideItem(
        title = "📶 SIM / WiFi Recharge Countdown (28 / 56 / 84 Days)",
        group = "Calendar",
        whatItDoes = "Counts down how many days are left before your phone SIM or home WiFi pack runs out.",
        howToUse = "Open Calendar & Streak (or Tools Hub → SIM & WiFi Recharge), tap '+ Add Recharge Plan', pick 28/56/84 days, or tap 'Renew +28d' when you recharge again.",
        kidExample = "Phone companies sell 28-day packs instead of a full month. This timer tells you '3 days left!' so your internet doesn't suddenly stop during class!",
        route = "recharge"
    ),
    GuideItem(
        title = "📦 'Waiting for Refund / Return' Tracker",
        group = "Dashboard",
        whatItDoes = "Remembers money that Amazon, Flipkart, or a friend promised to send back to you.",
        howToUse = "On the Home screen card, tap '+ Refund' to add a return. When the money arrives, tap 'Received ✓' and pick which Bank/UPI wallet got it.",
        kidExample = "You returned shoes that didn't fit for ₹1,200. This card watches that ₹1,200 until the shop puts the money back into your bank!",
        route = "refunds"
    ),
    GuideItem(
        title = "👨‍👩‍👧 Family Pocket-Money vs. 💼 Own Earnings Split",
        group = "Reports",
        whatItDoes = "Shows how much of your money came from home pocket-money versus money you earned yourself.",
        howToUse = "When logging Income, tap '👨‍👩‍👧 Family / Allowance' or '💼 Own Earnings', then open Reports to see the progress bar.",
        kidExample = "You got ₹2,000 from your parents and earned ₹1,000 tutoring a younger kid — Tracmio shows you earned one-third all by yourself!",
        route = "reports"
    ),
    GuideItem(
        title = "📂 Account Allocations & Auto-Split %",
        group = "Accounts",
        whatItDoes = "Divides one single bank account into smaller labeled envelopes and splits new income automatically.",
        howToUse = "In Accounts, tap '📂 Allocate' on any wallet, add pockets (like Emergency 20% or Rent 50%), or tap the pencil icon to edit %.",
        kidExample = "Your piggy bank has ₹1,000 inside, and you stick 3 labels on it: ₹500 for School Books, ₹300 for Snacks, and ₹200 for a Toy!",
        route = "accounts"
    ),
    GuideItem(
        title = "💵 3-Step Cash Counter (Notes & Coins)",
        group = "Accounts",
        whatItDoes = "Helps you count real paper notes and metal coins in your hand and match your Cash Wallet.",
        howToUse = "Tap '💵 Cash' on Home or Accounts. Step 1: Pick ₹500–₹10 Note or ₹20–₹1 Coin. Step 2: Tap +/- count and '+ Add'. Step 3: Tap 'Apply & Reconcile Cash'.",
        kidExample = "You empty your wallet on the bed: two ₹500 notes and three ₹10 coins. Tap them in and Tracmio counts ₹1,030 for you!",
        route = "cash_count"
    ),
    GuideItem(
        title = "⚖️ Balance Reconcile & >30% Credit Alert",
        group = "Accounts",
        whatItDoes = "Fixes your wallet balance if it doesn't match your real bank app, and warns you if credit card spending passes 30%.",
        howToUse = "In Accounts, tap '⚖️ Reconcile' and type your real balance. For Credit Cards, set your limit and Bill/Due day.",
        kidExample = "If your notebook says you have ₹500 left but your pocket only has ₹470, Reconcile fixes the ₹30 gap in 1 tap!",
        route = "accounts"
    ),
    GuideItem(
        title = "⏳ 24h Cooling Wishlist (Impulse Buyer Guard)",
        group = "Planning",
        whatItDoes = "Puts a 24-hour waiting timer on things you want to buy so you don't waste money on impulse.",
        howToUse = "Open Planning → Goals/Wishlist (or Tools Hub → Wishlist), tap '+ Add Wishlist Item'. After thinking, tap 'Skip & Save ✓' or 'Bought It → Log Expense'.",
        kidExample = "You see a shiny ₹999 toy online. You wait 1 full day on the timer — if you don't want it tomorrow, tap 'Skip & Save ✓' and celebrate saving ₹999!",
        route = "wishlist"
    ),
    GuideItem(
        title = "🎁 Cashback & 💎 Price-Per-Use Calculator",
        group = "Planning",
        whatItDoes = "Tracks free reward money you won and shows how cheap your shoes or gadgets become each time you use them.",
        howToUse = "In Planning (Tab 2), tap '+ Cashback' (with optional Credit to Wallet switch) or '+ Item' and tap '+1 Use' every time you wear/use it.",
        kidExample = "A ₹3,000 jacket worn 1 time costs ₹3,000 per wear. Wear it 100 times by tapping '+1 Use' and it costs only ₹30 per wear!",
        route = "price_per_use"
    ),
    GuideItem(
        title = "🪙 Round-Up Spare Change Jar",
        group = "Planning",
        whatItDoes = "Automatically drops spare change from every purchase into your savings goal.",
        howToUse = "Open Planning → Goals/Wishlist, flip ON the Round-Up Spare Change switch, and pick ₹10, ₹50, or ₹100 step.",
        kidExample = "You buy a ₹85 burger. Tracmio rounds it to ₹100 and drops the ₹15 spare change straight into your New Bicycle Goal!",
        route = "round_up"
    ),
    GuideItem(
        title = "🛟 Emergency Fund Runway",
        group = "Planning",
        whatItDoes = "Calculates how many months your family could live safely on your current cash if income stopped.",
        howToUse = "Check the '🛟 Emergency Runway' card on Home or inside Planning → Budgets & Runway.",
        kidExample = "Like checking how many days of food and water a spaceship has stored inside for a long journey!",
        route = "runway"
    ),
    GuideItem(
        title = "📉 Snowball vs. Avalanche Debt Payoff Planner",
        group = "Planning",
        whatItDoes = "Compares two smart ways to finish paying off loans faster and save interest money.",
        howToUse = "Open Planning → Loans, add your loans, type an Extra Monthly Payment, or tap 'Record Repayment' when you pay an EMI.",
        kidExample = "Snowball knocks out the smallest loan first for quick wins; Avalanche knocks out the most expensive interest loan first to save the most money!",
        route = "debt_planner"
    ),
    GuideItem(
        title = "👥 People Ledger & ⚡ Smart Split WhatsApp Summary",
        group = "People",
        whatItDoes = "Splits group restaurant or trip bills Equally, by %, or Exact amounts and copies a neat WhatsApp message.",
        howToUse = "In People, tap '⚖️ Split Bill', enter the total, pick friends, and tap 'WhatsApp Copy' or 'Log Split'. Tap any friend for '1-Tap Settle Up'.",
        kidExample = "4 friends eat a ₹400 pizza. Smart Split calculates ₹100 each and copies a ready-to-send WhatsApp message with your UPI ID!",
        route = "people"
    ),
    GuideItem(
        title = "🔁 5-Type Transaction Modal, Tracker Link & Receipts",
        group = "Transactions",
        whatItDoes = "Logs Expense, Income, Borrow, Lend, or Transfer with 39 categories, photo receipts, and custom UPI apps.",
        howToUse = "Tap '+' at the bottom bar, pick a category/subcategory, attach a bill photo or warranty date, and tap Save Transaction.",
        kidExample = "Like a super-smart diary page where you can also staple a photo of your shop bill!",
        route = "transactions"
    ),
    GuideItem(
        title = "↻ 1-Tap Receipt Repeat, Red Delete & Warranty Vault",
        group = "Transactions",
        whatItDoes = "Opens a clean digital receipt slip where you can repeat the same spend in 1 tap, delete mistakes, or track warranty dates.",
        howToUse = "Tap any transaction to open its Receipt. Tap '↻ Repeat' to clone it, the top-left red trash icon to delete (with Undo!), or '×' on a warranty badge.",
        kidExample = "Bought a new mixer with a 1-year warranty? Save the expiry date on the receipt so Tracmio counts down the warranty days left!",
        route = "warranty_vault"
    ),
    GuideItem(
        title = "🗂️ Expanded Categories (39 Presets) & #Hashtag Totals",
        group = "Transactions",
        whatItDoes = "Includes 39 everyday Indian & global categories plus automatic totals for any #word in your notes.",
        howToUse = "Pick categories like Food & Chai or College & Exams, or type '#goa' or '#coffee' in a note to see its total pill at the top of Transactions.",
        kidExample = "Write '#birthday' on 4 different cake and balloon purchases, and Tracmio adds all '#birthday' spends together automatically!",
        route = "transactions"
    ),
    GuideItem(
        title = "📤 Top-Left Filter & Excel / CSV / PDF Statement Export",
        group = "Transactions",
        whatItDoes = "Filters your ledger by Wallet or Date Range and exports real Excel (.xlsx), CSV, or printable PDF statements.",
        howToUse = "In Transactions, tap top-left 'Filter' to filter by account/date, or tap 'Export' to save an Excel, CSV, or PDF file.",
        kidExample = "Need to show your parents or accountant all spends from this month? Two taps prints a clean PDF report!",
        route = "transactions"
    ),
    GuideItem(
        title = "📲 Opt-In Paste Bank / UPI SMS Quick-Log",
        group = "Dashboard",
        whatItDoes = "Reads a bank or UPI SMS that you choose to paste and fills in the amount and merchant 100% offline.",
        howToUse = "Copy a bank SMS, tap 'Paste Bank SMS / UPI Quick-Log' on Home, and review the pre-filled form.",
        kidExample = "Copy 'Rs.250 debited via GPay at Swiggy' and tap the card — Tracmio fills ₹250 and GPay for you without reading your private SMS inbox!",
        route = "dashboard"
    ),
    GuideItem(
        title = "⚡ Monthly Expense Pace Meter & 2×2 Quick Stats",
        group = "Dashboard",
        whatItDoes = "Shows if you are spending too fast, right on track, or saving well compared to how many days have passed this month.",
        howToUse = "Check the Monthly Expense Pace Meter card on Home right below the Quick Spend Chips.",
        kidExample = "Like a speedometer on a bicycle that tells you if you're pedaling too fast through your monthly budget!",
        route = "dashboard"
    ),
    GuideItem(
        title = "👁️ Triple-Tap Stealth Mode & Privacy Eye",
        group = "Security",
        whatItDoes = "Hides your money with stars (••••) on 1 tap, or shows believable fake numbers when you triple-tap!",
        howToUse = "Tap the top-right Eye icon once to mask balances, or tap it 3 times fast to turn on Stealth Decoy Mode.",
        kidExample = "If a nosy friend is peeking over your shoulder on the bus, triple-tap the eye so they only see a small decoy balance!",
        route = "dashboard"
    ),
    GuideItem(
        title = "📊 Interactive Reports, Weekend Radar & Month Compare",
        group = "Reports",
        whatItDoes = "Draws 4 chart styles, compares this month vs last month, and checks if you spend more on weekends.",
        howToUse = "Open Reports from the bottom bar, switch chart styles or color palettes, and tap 'Export PDF'.",
        kidExample = "Shows whether Saturday ice-cream outings cost more than Monday-to-Friday school days!",
        route = "reports"
    ),
    GuideItem(
        title = "🩺 Financial X-Ray (0–100 Score Ring & 10 Vitals)",
        group = "Intelligence",
        whatItDoes = "Gives your money health a score out of 100 across 10 simple checkups.",
        howToUse = "Open Tools Hub → Financial X-Ray to inspect your score ring, 10 vitals, and smart tips.",
        kidExample = "Just like a doctor's report card that gives you an A+ when you save well and keep debts low!",
        route = "xray"
    ),
    GuideItem(
        title = "📖 Money Story (50/30/20) & 🧬 Spending DNA",
        group = "Intelligence",
        whatItDoes = "Tells the story of your Needs, Wants, and Savings (50/30/20 rule) and your favorite spending day of the week.",
        howToUse = "Open Tools Hub → Money Story or Spending DNA to read your breakdown and print a PDF summary.",
        kidExample = "Shows if 50% went to Needs (food/rent), 30% to Fun (movies/snacks), and 20% into your Piggy Bank!",
        route = "story"
    ),
    GuideItem(
        title = "🗂️ Custom Tracker Folder Workspace",
        group = "Intelligence",
        whatItDoes = "Creates separate mini-folders for a Goa Trip, Wedding, or Hostel Project with its own budget, group split, and bill photos.",
        howToUse = "Open Tools Hub → Custom Trackers, tap '+ Tracker', then tap the folder to add entries, people, or receipt files.",
        kidExample = "Like having a separate envelope just for your 'School Picnic' so picnic expenses don't get mixed up with home groceries!",
        route = "trackers"
    ),
    GuideItem(
        title = "⏳ 3-Tab Financial Time Machine",
        group = "Intelligence",
        whatItDoes = "Lets you rewind to see your exact balance on any past date or fast-forward 1 to 5 years into the future.",
        howToUse = "Open Tools Hub → Time Machine and slide the timeline bar in Past Travel, Future Forecast, or Compare Dates.",
        kidExample = "Slide back to last Diwali to see how much money you had then, or slide forward 2 years to see how big your savings will grow!",
        route = "time_machine"
    ),
    GuideItem(
        title = "🔄 Subscriptions Autopilot & 1-Tap Pay",
        group = "Planning",
        whatItDoes = "Tracks monthly or yearly memberships like Netflix, Spotify, or Gym and logs payments in 1 tap.",
        howToUse = "Open Tools Hub → Subscriptions, tap '+ Add', or tap 'Pay & Log' when a renewal bill is paid.",
        kidExample = "Keeps a list of every app or club that charges you every month so no surprise bill sneaks up on you!",
        route = "subscriptions"
    ),
    GuideItem(
        title = "🔥 7-Column Calendar, Fees & No-Spend Streak",
        group = "Calendar",
        whatItDoes = "Counts how many days in a row you spent ₹0 and marks school/college fee deadlines on a monthly calendar.",
        howToUse = "Open Tools Hub → Calendar & Streak, tap any date square to inspect spends, or tap '+ Deadline' to add an exam/fee date.",
        kidExample = "Every day you don't buy junk food, your 🔥 No-Spend Streak score goes up like a video game combo!",
        route = "calendar"
    ),
    GuideItem(
        title = "🛡️ Biometric Launch Lock, PIN & Recovery Key",
        group = "Security",
        whatItDoes = "Locks your money vault with your Fingerprint/Face Unlock, a 4/6-digit glass PIN, and an offline Recovery Key.",
        howToUse = "Open Tools Hub → App Lock & Biometrics, set a 4 or 6-digit PIN + Security Answer, and toggle Biometric Launch Unlock.",
        kidExample = "Like putting a secret fingerprint padlock on your diary, plus a spare key hidden safely in case you forget the code!",
        route = "app_lock"
    ),
    GuideItem(
        title = "🎨 Liquid Glass UI Studio, Scaling & Multi-Currency",
        group = "Appearance",
        whatItDoes = "Changes Tracmio's colors, 3D wallpapers, text size (-1/+1), tilt motion, and currency symbol (₹, $, €, £).",
        howToUse = "Open Tools Hub → Liquid Glass UI Studio (or tap the top-left Tracmio logo for quick name/currency setup).",
        kidExample = "Dress up your app in 11 glowing glass colors and make the letters bigger or smaller so it feels super comfy for your eyes!",
        route = "themes"
    ),
    GuideItem(
        title = "💾 Full JSON & AES-GCM Encrypted Backups (Room v9)",
        group = "Data Vault",
        whatItDoes = "Saves a complete locked backup file of all your wallets, transactions, Khata, recharges, and settings on your phone.",
        howToUse = "Open Tools Hub → Full JSON Backup to export or restore an Encrypted (.enc) or Plain JSON backup.",
        kidExample = "Like packing all your notebooks into a locked steel treasure chest so you can move them safely to a new phone anytime!",
        route = "data_export"
    )
)

@Composable
fun InAppGuideScreen(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onOpenFeature: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var groupFilter by remember { mutableStateOf("ALL") }
    val groups = listOf("ALL", "Navigation", "Dashboard", "Accounts", "Transactions", "Planning", "People", "Reports", "Intelligence", "Calendar", "Security", "Appearance", "Data Vault")

    val items = remember(query, groupFilter) {
        ALL_GUIDE_FEATURES.filter { item ->
            (groupFilter == "ALL" || item.group.equals(groupFilter, true)) &&
                (query.isBlank() ||
                    item.title.contains(query, true) ||
                    item.whatItDoes.contains(query, true) ||
                    item.howToUse.contains(query, true) ||
                    item.kidExample.contains(query, true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Column {
                Text("📘 Kid-Simple App Guide (v16.13)", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Every single feature explained simply with real-life examples & 1-tap Open → buttons.",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search any feature or example (e.g. Khata, piggy bank, ATM)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("guide_search_input")
        )

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.forEach { g ->
                FilterChip(selected = groupFilter == g, onClick = { groupFilter = g }, label = { Text(g) })
            }
        }

        items.forEachIndexed { idx, entry ->
            GlassCard(settings = settings, modifier = Modifier.fillMaxWidth().testTag("guide_card_$idx")) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                            Text(entry.group, style = MaterialTheme.typography.labelSmall, color = palette.primary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onOpenFeature(entry.route) },
                            modifier = Modifier.testTag("guide_open_$idx")
                        ) {
                            Text("Open →")
                        }
                    }
                    Text(
                        "✨ What it does: ${entry.whatItDoes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "👆 How to use: ${entry.howToUse}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Text(
                        "🧸 Real-Life Example: ${entry.kidExample}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
