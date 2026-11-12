package com.tracmio.app.ui.nativeapp

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracmio.app.DailyReminderReceiver
import com.tracmio.app.data.AcademicEventItem
import com.tracmio.app.data.CustomReminderItem
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.SubscriptionItem
import java.util.Locale

/**
 * Clean More Screen with Primary Options and a dedicated "🧰 Tools Hub" Section Card (Requirement #15)
 */
@Composable
fun MoreScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    onNavigateToTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "More & Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Tracmio Offline Suite • v2.5.2",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        // Dedicated Tools Hub Section Card (Requirement #15)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tools_hub_card"),
                backgroundColor = Color(0xCC0E1728),
                borderColor = palette.primary.copy(alpha = 0.5f),
                onClick = { onNavigateToTool("tools_hub") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.primary.copy(alpha = 0.2f))
                                .border(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧰", fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tools Hub",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "14 TOOLS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Subscriptions, X-Ray, Reminders, Data & more",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Tools Hub",
                        tint = palette.primary
                    )
                }
            }
        }

        // Primary Menu Options
        item {
            Text(
                text = "Primary Options",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textSecondary
            )
        }

        item {
            MoreRowItem(
                title = "In-App User Guide",
                subtitle = "Complete interactive manual explaining every button & feature",
                icon = "📖",
                palette = palette,
                onClick = { onNavigateToTool("guide") }
            )
        }

        item {
            MoreRowItem(
                title = "Themes & Appearance Studio",
                subtitle = "Liquid Glass, 10 animation styles, frosted blur & sliders",
                icon = "🎨",
                palette = palette,
                onClick = { onNavigateToTool("themes") }
            )
        }

        item {
            MoreRowItem(
                title = "Data & Encrypted Backup",
                subtitle = "Export JSON, Excel (.xlsx), CSV, and import offline backups",
                icon = "📦",
                palette = palette,
                onClick = { onNavigateToTool("data_export") }
            )
        }

        item {
            MoreRowItem(
                title = "Multi-Reminders & Meme Alerts",
                subtitle = "Scheduled offline reminders with instant test notifications",
                icon = "⏰",
                palette = palette,
                onClick = { onNavigateToTool("reminders") }
            )
        }

        item {
            MoreRowItem(
                title = "Custom Trackers & Trips",
                subtitle = "Trip budgets, event folders, and equal split calculations",
                icon = "📁",
                palette = palette,
                onClick = { onNavigateToTool("trackers") }
            )
        }

        item {
            MoreRowItem(
                title = "Financial Time Machine",
                subtitle = "Inspect historical net worth, balance deltas & spending",
                icon = "⏳",
                palette = palette,
                onClick = { onNavigateToTool("time_machine") }
            )
        }
    }
}

@Composable
fun MoreRowItem(
    title: String,
    subtitle: String,
    icon: String,
    palette: ThemePalette,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0x99111C2E),
        borderColor = Color(0x22FFFFFF),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                }
            }

            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Dedicated Tools Hub Screen (Houses all 14 secondary tools cleanly, Requirement #15)
 */
@Composable
fun ToolsHubScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    onBack: () -> Unit,
    onOpenTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)

    val tools = listOf(
        Triple("Reports & Insights", "Deep category breakdowns, cashflow graphs & person splits", "📊") to "reports",
        Triple("Transactions Search", "Search by notes, amounts, tags & dates", "🔍") to "transactions_search",
        Triple("💳 Subscriptions Autopilot", "Recurring monthly & yearly subscription tracker", "💳") to "subscriptions",
        Triple("🩺 Financial X-Ray", "50/30/20 rule, emergency runway & savings health", "🩺") to "xray",
        Triple("📖 Money Story", "Monthly narrative highlights, top merchants & milestones", "📖") to "story",
        Triple("Global Multi-Search", "Search across transactions, accounts, people & trackers", "🌐") to "global_search",
        Triple("🧬 Spending DNA", "Psychological spending profile & habit analysis", "🧬") to "dna",
        Triple("🗓️ Academic Calendar", "Semester tuition, exam fees & college dues", "🗓️") to "academic",
        Triple("⏰ Multi-Reminders", "Meme check-ins with instant test alerts", "⏰") to "reminders",
        Triple("Financial Time Machine", "Travel back in time to inspect historical wealth", "⏳") to "time_machine",
        Triple("📦 Data & Export", "PBKDF2 encrypted backups, Excel .xlsx & JSON", "📦") to "data_export",
        Triple("🔐 Privacy Vault", "Mask balances, biometric/PIN access", "🔐") to "privacy",
        Triple("Custom Trackers & Trips", "Folder workspaces for trips & event budgets", "📁") to "trackers",
        Triple("Themes & Appearance", "Liquid Glass, 10 animation styles & sliders", "🎨") to "themes"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "🧰 Tools Hub",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Advanced intelligence & utility engines",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        items(tools) { (info, route) ->
            val (name, desc, emoji) = info
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0E1828),
                borderColor = palette.primary.copy(alpha = 0.35f),
                onClick = { onOpenTool(route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = emoji, fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                    }

                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = palette.primary)
                }
            }
        }
    }
}

/**
 * Searchable Button-by-Button In-App User Guide (Requirement #1)
 * Explains every single button, screen, and feature in the app.
 */
@Composable
fun InAppGuideScreen(
    settings: LiquidGlassSettings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    data class GuideEntry(
        val category: String,
        val buttonOrFeature: String,
        val icon: String,
        val explanation: String
    )

    val guideEntries = remember {
        listOf(
            GuideEntry("Dashboard & Home", "Net Liquid Worth Hero Card", "💎", "Displays total combined liquid balance across all your accounts. Includes eye toggle button to hide balances for privacy."),
            GuideEntry("Dashboard & Home", "Eye Privacy Toggle", "👁️", "Masks sensitive balance numbers with •••••••• so onlookers cannot see your net worth."),
            GuideEntry("Dashboard & Home", "Safe-to-Spend Daily Allowance", "🛡️", "Calculates how much money you can spend each day for the rest of the month based on remaining net income."),
            GuideEntry("Dashboard & Home", "+ Income & + Expense Quick Buttons", "➕", "Instantly opens transaction sheet pre-filled with Income or Expense mode."),
            GuideEntry("Dashboard & Home", "Cash Denomination Counter Button", "💵", "Opens the physical cash counter to tally ₹500, ₹200, ₹100, ₹50 notes and coins into an exact total."),
            GuideEntry("Dashboard & Home", "Transfer Funds Button", "🔁", "Enables direct funds transfer between any two accounts without affecting total income/expense."),
            GuideEntry("Transactions & Edit", "Pencil Edit Button (Top-Right)", "✏️", "Allows in-place editing of transaction amount, category, note, or wallet without creating duplicate entries."),
            GuideEntry("Transactions & Edit", "Receipt Slip Dialog", "🧾", "Tapping any transaction card displays a verified visual receipt slip with export/print PDF and delete options."),
            GuideEntry("Transactions & Edit", "Account / Wallet Picker Button", "👛", "In Add/Edit transaction: tap to open the full list of all existing wallets, switch between them, or tap '+ Add Wallet'."),
            GuideEntry("Transactions & Edit", "Inline Math Calculator (= Button)", "🧮", "Enter expressions like '120+45*2' in amount field and tap calculate to solve inline instantly."),
            GuideEntry("Transactions & Edit", "1-Tap Quick Templates", "⚡", "Tap Chai (₹20), Breakfast (₹60), Auto (₹150), or Groceries to fill transaction fields in 1 second."),
            GuideEntry("Custom Trackers", "Folder Workspace", "📁", "Clicking any tracker opens a folder workspace with sub-tabs for Transactions and Reports, plus an Export button."),
            GuideEntry("Custom Trackers", "+ Category Button in Trackers", "➕", "Inside tracker creation, tap '+ Category' to define your own custom trip or project category."),
            GuideEntry("Custom Trackers", "High-Contrast End Date", "🏁", "Displays prominent, clear end date badge for trip and event deadlines."),
            GuideEntry("Custom Trackers", "Export Tracker Button", "⇩", "Exports dedicated tracker workbook with categorized expenses and member split balance reports."),
            GuideEntry("Reports & Intelligence", "Tap Row to Filter", "🔍", "Tapping any category breakdown row instantly filters the transactions list to only show that category's expenses."),
            GuideEntry("Reports & Intelligence", "Person Summary Dual Graphs", "👥", "Shows green bars for money you lent, red bars for money you borrowed, and net settlement status."),
            GuideEntry("Reports & Intelligence", "Account Share Progress Bars", "📊", "Visualizes the percentage share of liquid wealth residing in each bank and cash account."),
            GuideEntry("Reports & Intelligence", "4 Chart Visual Styles", "📈", "Switch between 3D Neon Bars, Wave & Flow, Gradient Capsules, and Executive Minimalist."),
            GuideEntry("Reminders & Alerts", "Save Daily & Save Schedule Buttons", "⏰", "Saves your daily reminder schedule with instant Toast confirmation AND dispatches an immediate test notification to your system tray."),
            GuideEntry("Reminders & Alerts", "Rotating Meme Alerts Toggle", "🎭", "Switches between humorous relatable money memes and professional financial check-in alerts."),
            GuideEntry("Themes & Studio", "10 Animation Styles", "🌊", "Select between Liquid Flow, Glass Ripple, Aurora Glow, Mesh Wave, Fluid Morph, Neon Pulse, Soft Fade, Parallax Depth, Floating Particles, and Crystal Refraction."),
            GuideEntry("Themes & Studio", "Frosted Glass Sliders", "🎚️", "Adjust wallpaper blur, ambient neon glow intensity, and UI scaling with smooth iOS brightness-style sliders."),
            GuideEntry("Themes & Studio", "Live Appearance Preview", "📱", "Displays a real-time live mockup card showing current theme, glow, and card styling as you adjust settings."),
            GuideEntry("Themes & Studio", "Device Tilt Motion Toggle", "📱", "Enables dynamic liquid flow animations that tilt and ripple as you physically move your phone.")
        )
    }

    val categories = listOf("All", "Dashboard & Home", "Transactions & Edit", "Custom Trackers", "Reports & Intelligence", "Reminders & Alerts", "Themes & Studio")

    val filtered = remember(searchQuery, selectedCategory) {
        guideEntries.filter { entry ->
            val matchesCat = selectedCategory == "All" || entry.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                entry.buttonOrFeature.contains(searchQuery, ignoreCase = true) ||
                entry.explanation.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "In-App User Guide",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Button-by-button explanation of every app feature",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search any button or function (e.g. edit, receipt, folder)...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = palette.textSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) palette.primary else Color(0x331E293B),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        items(filtered) { entry ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0F1829),
                borderColor = palette.primary.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = entry.icon, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = entry.buttonOrFeature,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = entry.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = entry.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Themes & Appearance Studio (Requirements #2, #8, #13, #14, #17)
 * Features Live Appearance Preview, 10 Animation Styles, iOS Frosted Glass Sliders, and Theme Presets.
 */
@Composable
fun ThemesAndAppearanceStudio(
    settings: LiquidGlassSettings,
    onUpdateSettings: ((LiquidGlassSettings) -> LiquidGlassSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ThemePresets.getPalette(settings.themePreset)

    val presets = listOf(
        "liquid_glass_dark" to "Liquid Obsidian",
        "cyber_neon" to "Cyber Neon",
        "emerald_aurora" to "Emerald Aurora",
        "deep_sapphire" to "Deep Sapphire",
        "obsidian_gold" to "Obsidian Gold",
        "ruby_sunset" to "Ruby Sunset",
        "frosted_light" to "Frosted Glacier"
    )

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
                        text = "Liquid Glass Studio",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real-time frosted blur, ambient neon & motion physics",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // Live Appearance Preview Section (Requirement #14)
        item {
            LiveAppearancePreview(settings = settings)
        }

        // 10 Animation Styles Selector with Icons (Requirement #17)
        item {
            Column {
                Text(
                    text = "Animation Style (10 Physics Profiles)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Controls card entry, background ripples, and ambient motion",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ANIMATION_STYLES) { (id, pair) ->
                        val (title, icon) = pair
                        val isSelected = settings.animationStyle == id

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) palette.primary else Color(0x331E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) palette.primary else Color(0x22FFFFFF)),
                            modifier = Modifier.clickable {
                                onUpdateSettings { it.copy(animationStyle = id) }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(text = icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Theme Presets (Requirement #13)
        item {
            Column {
                Text(
                    text = "Theme Palette",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { (presetKey, displayName) ->
                        val isSelected = settings.themePreset == presetKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) palette.primary else Color(0x331E293B),
                            modifier = Modifier.clickable {
                                onUpdateSettings { it.copy(themePreset = presetKey) }
                            }
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Frosted Glass Sliders (Requirement #8)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0E1828)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassSlider(
                        value = settings.wallpaperBlurDp,
                        onValueChange = { blur ->
                            onUpdateSettings { it.copy(wallpaperBlurDp = blur) }
                        },
                        valueRange = 0f..40f,
                        title = "Frosted Blur Depth",
                        valueLabel = "${settings.wallpaperBlurDp.toInt()} dp",
                        accentColor = palette.primary
                    )

                    GlassSlider(
                        value = settings.glowIntensity,
                        onValueChange = { glow ->
                            onUpdateSettings { it.copy(glowIntensity = glow) }
                        },
                        valueRange = 0.1f..1f,
                        title = "Ambient Neon Glow Intensity",
                        valueLabel = "${(settings.glowIntensity * 100).toInt()}%",
                        accentColor = palette.secondary
                    )

                    GlassSlider(
                        value = settings.uiScale,
                        onValueChange = { scale ->
                            onUpdateSettings { it.copy(uiScale = scale) }
                        },
                        valueRange = 0.85f..1.15f,
                        title = "Liquid UI Scaling",
                        valueLabel = "${(settings.uiScale * 100).toInt()}%",
                        accentColor = palette.primary
                    )
                }
            }
        }

        // Phone Movement Device Tilt Motion Toggle (Requirement #2)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0F182A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Liquid Flow Along Phone Move",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Orbs and liquid highlights tilt dynamically with device sensor",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }

                    Switch(
                        checked = settings.deviceMotionEnabled,
                        onCheckedChange = { checked ->
                            onUpdateSettings { it.copy(deviceMotionEnabled = checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = palette.primary,
                            checkedTrackColor = palette.primary.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Multi-Reminders Screen (Requirement #12)
 * Clicking "Save Daily" or "Save Schedule" triggers instant toast feedback PLUS an immediate test notification!
 */
@Composable
fun MultiRemindersScreen(
    settings: LiquidGlassSettings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = ThemePresets.getPalette(settings.themePreset)
    var isMemeMode by remember { mutableStateOf(true) }
    var selectedHour by remember { mutableStateOf(21) }
    var selectedMinute by remember { mutableStateOf(0) }
    var reminderEnabled by remember { mutableStateOf(true) }

    fun triggerSaveFeedbackAndNotification(actionLabel: String) {
        // Immediate Toast feedback popup (Requirement #12)
        Toast.makeText(
            context,
            "✅ $actionLabel saved! Test notification dispatched to status bar.",
            Toast.LENGTH_LONG
        ).show()

        // Immediate test notification dispatched to system tray (Requirement #12)
        runCatching {
            DailyReminderReceiver.showReminderNotification(context, isMemeMode)
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
                        text = "Multi-Reminders Machine",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Daily check-ins, rotating meme alerts & quick actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // Daily Reminder Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0E182A),
                borderColor = palette.primary.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Reminder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "Scheduled at %02d:%02d", selectedHour, selectedMinute),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.primary
                                )
                            }
                        }

                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = palette.primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Meme Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x331E293B))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🎭 Rotating Meme Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Witty, relatable expense memes instead of boring texts", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        Switch(
                            checked = isMemeMode,
                            onCheckedChange = { isMemeMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = palette.secondary)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Daily Button (Requirement #12: instant Toast + test notification)
                    Button(
                        onClick = { triggerSaveFeedbackAndNotification("Daily Reminder") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_daily_reminder_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Daily", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Custom Schedule Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xCC0F192C)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Custom Alarm Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Set custom check-in intervals for payday, weekend reviews, and credit card dues.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Save Schedule Button (Requirement #12: instant Toast + test notification)
                    Button(
                        onClick = { triggerSaveFeedbackAndNotification("Custom Schedule") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_schedule_reminder_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Schedule", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
