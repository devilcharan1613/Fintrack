package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracemeo.app.TracmioReminderScheduler
import com.tracemeo.app.data.CustomReminderItem
import com.tracemeo.app.data.LiquidGlassSettings
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ThemesAndAppearanceStudio(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
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
            Text("🎨 Liquid Glass UI Studio", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }

        LiveAppearancePreview(settings = settings)

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Theme Presets (11 Styles)", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemePresets.ALL_THEMES.forEach { (id, label) ->
                        FilterChip(
                            selected = settings.themePreset == id,
                            onClick = { viewModel.updateSettings { it.copy(themePreset = id) } },
                            label = { Text(label) }
                        )
                    }
                }
                if (settings.themePreset == "custom") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = settings.customPrimaryHex,
                            onValueChange = { hex -> viewModel.updateSettings { it.copy(customPrimaryHex = hex) } },
                            label = { Text("Primary Hex") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = settings.customSecondaryHex,
                            onValueChange = { hex -> viewModel.updateSettings { it.copy(customSecondaryHex = hex) } },
                            label = { Text("Secondary Hex") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Glass Profiles & Mesh Wallpapers", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemePresets.GLASS_UI_PROFILES.forEach { gp ->
                        FilterChip(
                            selected = settings.glassUiStyle == gp.id,
                            onClick = { viewModel.updateSettings { it.copy(glassUiStyle = gp.id) } },
                            label = { Text(gp.name) }
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WALLPAPER_PRESETS.forEach { (id, title) ->
                        FilterChip(
                            selected = settings.wallpaperPreset == id && settings.customWallpaperPath == null,
                            onClick = { viewModel.updateSettings { it.copy(wallpaperPreset = id, customWallpaperPath = null) } },
                            label = { Text(title) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { hostCallbacks.chooseWallpaperImage() }, modifier = Modifier.weight(1f)) { Text("Pick Photo") }
                    OutlinedButton(onClick = { hostCallbacks.captureWallpaperImage() }, modifier = Modifier.weight(1f)) { Text("Camera") }
                    if (settings.customWallpaperPath != null) {
                        TextButton(onClick = { viewModel.updateSettings { it.copy(customWallpaperPath = null) } }) { Text("Reset") }
                    }
                }
            }
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSlider(
                    value = settings.backgroundVisibility,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(backgroundVisibility = v) } },
                    valueRange = 0.15f..1f,
                    title = "Background Brightness / Visibility",
                    valueLabel = "${(settings.backgroundVisibility * 100).toInt()}%",
                    accentColor = palette.primary,
                    textColor = palette.textPrimary
                )
                GlassSlider(
                    value = settings.wallpaperBlurDp,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(wallpaperBlurDp = v) } },
                    valueRange = 0f..28f,
                    title = "Glass Frosted Blur",
                    valueLabel = "${settings.wallpaperBlurDp.toInt()} dp",
                    accentColor = palette.secondary,
                    textColor = palette.textPrimary
                )
                GlassSlider(
                    value = settings.glowIntensity,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(glowIntensity = v) } },
                    valueRange = 0.2f..1f,
                    title = "Aurora Glow Intensity",
                    valueLabel = "${(settings.glowIntensity * 100).toInt()}%",
                    accentColor = palette.primary,
                    textColor = palette.textPrimary
                )

                // +1 / -1 UI Scaling Stepper
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("UI Text Scale (${(settings.uiScale * 100).toInt()}%)", color = palette.textPrimary, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.updateSettings { it.copy(uiScale = (it.uiScale - 0.05f).coerceAtLeast(0.85f)) } },
                            modifier = Modifier.testTag("ui_scale_minus")
                        ) { Text("-1") }
                        OutlinedButton(
                            onClick = { viewModel.updateSettings { it.copy(uiScale = (it.uiScale + 0.05f).coerceAtMost(1.15f)) } },
                            modifier = Modifier.testTag("ui_scale_plus")
                        ) { Text("+1") }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sensor Tilt Liquid Parallax", color = palette.textPrimary)
                    Switch(checked = settings.deviceMotionEnabled, onCheckedChange = { m -> viewModel.updateSettings { it.copy(deviceMotionEnabled = m) } })
                }

                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ANIMATION_STYLES.forEach { (id, pair) ->
                        FilterChip(
                            selected = settings.animationStyle == id,
                            onClick = { viewModel.updateSettings { it.copy(animationStyle = id) } },
                            label = { Text("${pair.second} ${pair.first}") }
                        )
                    }
                }

                Text("Display Currency Symbol", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("₹" to "INR ₹", "$" to "USD $", "€" to "EUR €", "£" to "GBP £").forEach { (sym, label) ->
                        FilterChip(
                            selected = settings.currencySymbol == sym,
                            onClick = { viewModel.updateSettings { it.copy(currencySymbol = sym) } },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MultiRemindersScreen(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val remPrefs = remember { TracmioReminderScheduler.getPrefs(context) }
    val customReminders by viewModel.customRemindersFlow.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var enabled by remember { mutableStateOf(remPrefs.getBoolean(TracmioReminderScheduler.KEY_ENABLED, true)) }
    var hour by remember { mutableIntStateOf(remPrefs.getInt(TracmioReminderScheduler.KEY_HOUR, 21)) }
    var minute by remember { mutableIntStateOf(remPrefs.getInt(TracmioReminderScheduler.KEY_MINUTE, 0)) }
    var userName by remember { mutableStateOf(settings.userName) }
    var showAddReminder by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<CustomReminderItem?>(null) }
    var newRemTitle by remember { mutableStateOf("") }
    var newRemHour by remember { mutableIntStateOf(9) }
    var newRemMin by remember { mutableIntStateOf(30) }
    val summaryLine = remember(uiState.transactions, settings.currencySymbol) {
        TracmioReminderScheduler.getDailySummaryLine(context)
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
            Text("🔔 9 PM Summary & Reminders", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🌙 9 PM Daily Spend Summary Notification", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Switch(checked = enabled, onCheckedChange = {
                        enabled = it
                        hostCallbacks.scheduleDailyReminder(it, hour, minute, userName)
                    })
                }
                if (summaryLine.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.primary.copy(alpha = 0.14f))
                            .padding(10.dp)
                    ) {
                        Text(
                            "Live 9 PM Summary Preview: \"$summaryLine\"",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                OutlinedTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        viewModel.updateSettings { s -> s.copy(userName = it) }
                    },
                    label = { Text("Your Name (for personalized check-ins)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Reminder Time: %02d:%02d".format(hour, minute), color = palette.primary, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { hour = (hour + 23) % 24 }) { Text("-1h") }
                        OutlinedButton(onClick = { hour = (hour + 1) % 24 }) { Text("+1h") }
                        OutlinedButton(onClick = { minute = (minute + 15) % 60 }) { Text("+15m") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            hostCallbacks.scheduleDailyReminder(enabled, hour, minute, userName)
                            viewModel.showStatus("Daily reminder scheduled for %02d:%02d".format(hour, minute))
                        },
                        modifier = Modifier.weight(1f).testTag("save_daily_reminder_btn")
                    ) { Text("Save Daily Schedule") }
                    OutlinedButton(onClick = { hostCallbacks.dispatchTestReminder() }, modifier = Modifier.weight(1f)) {
                        Text("Send Test Alert")
                    }
                }
            }
        }

        // Custom Reminders CRUD
        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Custom Money Reminders (${customReminders.size})", style = MaterialTheme.typography.titleMedium, color = palette.primary)
                    Button(
                        onClick = {
                            editingReminder = null
                            newRemTitle = ""
                            newRemHour = 9
                            newRemMin = 30
                            showAddReminder = true
                        },
                        modifier = Modifier.testTag("add_custom_reminder_btn")
                    ) {
                        Text("+ Reminder")
                    }
                }
                if (customReminders.isEmpty()) {
                    Text("Add custom reminders for rent, SIP dates, or weekly expense audits.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                } else {
                    customReminders.forEach { rem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceElevated.copy(alpha = 0.65f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rem.title, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                Text("%02d:%02d daily".format(rem.hour, rem.minute), style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = rem.isEnabled,
                                    onCheckedChange = { en ->
                                        viewModel.saveCustomReminders(
                                            customReminders.map { if (it.id == rem.id) it.copy(isEnabled = en) else it }
                                        )
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        editingReminder = rem
                                        newRemTitle = rem.title
                                        newRemHour = rem.hour
                                        newRemMin = rem.minute
                                        showAddReminder = true
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit reminder", tint = palette.secondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.saveCustomReminders(customReminders.filterNot { it.id == rem.id })
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete reminder", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Monthly Encrypted Backup Reminder", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Switch(checked = settings.monthlyBackupEnabled, onCheckedChange = { en ->
                        viewModel.updateSettings { it.copy(monthlyBackupEnabled = en) }
                        hostCallbacks.scheduleMonthlyBackupReminder(en, settings.monthlyBackupDay, settings.monthlyBackupHour, settings.monthlyBackupMinute)
                    })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Day ${settings.monthlyBackupDay} at %02d:%02d".format(settings.monthlyBackupHour, settings.monthlyBackupMinute), color = palette.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = {
                            val next = (settings.monthlyBackupDay - 1).coerceAtLeast(1)
                            viewModel.updateSettings { it.copy(monthlyBackupDay = next) }
                        }) { Icon(Icons.Default.Remove, contentDescription = "Previous day", tint = palette.textPrimary) }
                        IconButton(onClick = {
                            val next = (settings.monthlyBackupDay + 1).coerceAtMost(28)
                            viewModel.updateSettings { it.copy(monthlyBackupDay = next) }
                        }) { Icon(Icons.Default.Add, contentDescription = "Next day", tint = palette.textPrimary) }
                    }
                }
            }
        }
    }

    if (showAddReminder || editingReminder != null) {
        val ed = editingReminder
        AlertDialog(
            onDismissRequest = { showAddReminder = false; editingReminder = null },
            containerColor = palette.surface,
            title = { Text(if (ed == null) "New Custom Reminder" else "Edit Custom Reminder", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newRemTitle, onValueChange = { newRemTitle = it }, label = { Text("Reminder Label (e.g. Review Weekly Spend)") }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Time: %02d:%02d".format(newRemHour, newRemMin), color = palette.primary, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { newRemHour = (newRemHour + 23) % 24 }) { Text("-1h") }
                            OutlinedButton(onClick = { newRemHour = (newRemHour + 1) % 24 }) { Text("+1h") }
                            OutlinedButton(onClick = { newRemMin = (newRemMin + 15) % 60 }) { Text("+15m") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newRemTitle.isNotBlank()) {
                        val item = CustomReminderItem(
                            id = ed?.id ?: "rem_${UUID.randomUUID().toString().take(8)}",
                            title = newRemTitle.trim(),
                            hour = newRemHour,
                            minute = newRemMin,
                            isEnabled = ed?.isEnabled ?: true
                        )
                        val updatedList = if (ed == null) customReminders + item
                        else customReminders.map { if (it.id == ed.id) item else it }
                        viewModel.saveCustomReminders(updatedList)
                        hostCallbacks.scheduleCalendarEventReminder(item.title, "Daily custom reminder (${"%02d:%02d".format(newRemHour, newRemMin)})", System.currentTimeMillis() + 60_000L, item.id)
                        editingReminder = null
                        showAddReminder = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddReminder = false; editingReminder = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun DataAndVaultScreen(
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var healthText by remember { mutableStateOf<String?>(null) }
    var showEncryptDialog by remember { mutableStateOf(false) }
    var showDecryptDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var showWipeConfirm by remember { mutableStateOf(false) }

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
            Text("💾 Data Vault & Full Backup", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Full JSON Backup & Restore (v16.13 Schema)", style = MaterialTheme.typography.titleMedium, color = palette.primary)
                Text("Includes all accounts, allocations, transactions, budgets, goals, loans, persons, trackers, UI theme preferences & reminders.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { hostCallbacks.exportPlainJsonBackup("tracmio_v16.13_backup.json") }, modifier = Modifier.weight(1f).testTag("export_plain_json_btn")) {
                        Text("Export JSON")
                    }
                    OutlinedButton(onClick = { hostCallbacks.importJsonBackup(null) }, modifier = Modifier.weight(1f)) {
                        Text("Import JSON")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { passwordInput = ""; showEncryptDialog = true }, modifier = Modifier.weight(1f).testTag("export_enc_json_btn")) {
                        Text("AES-GCM Export")
                    }
                    OutlinedButton(onClick = { passwordInput = ""; showDecryptDialog = true }, modifier = Modifier.weight(1f)) {
                        Text("Decrypt Import")
                    }
                }
            }
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Spreadsheets & Native PDF", style = MaterialTheme.typography.titleMedium, color = palette.secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { hostCallbacks.exportWorkbookXlsx("Tracmio_Ledger_16.13.xlsx") }, modifier = Modifier.weight(1f)) {
                        Text("Excel .xlsx")
                    }
                    OutlinedButton(onClick = { hostCallbacks.printStatement(state.accounts, state.transactions) }, modifier = Modifier.weight(1f)) {
                        Text("PDF Statement")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { hostCallbacks.exportCsv("tracmio_ledger.csv") }, modifier = Modifier.weight(1f)) {
                        Text("Export CSV")
                    }
                    OutlinedButton(onClick = { hostCallbacks.importCsv() }, modifier = Modifier.weight(1f)) {
                        Text("Import CSV")
                    }
                }
            }
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SQLCipher AES-256 Database Integrity", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val rpt = viewModel.checkDatabaseHealth()
                            healthText = rpt.diagnosticSummary
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("check_db_health_btn")
                ) { Text("Run Integrity & Keystore Audit") }
                healthText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = palette.primary) }
                Button(
                    onClick = { showWipeConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset All Ledger Data") }
            }
        }
    }

    if (showEncryptDialog) {
        AlertDialog(
            onDismissRequest = { showEncryptDialog = false }, containerColor = palette.surface,
            title = { Text("PBKDF2 + AES-GCM Backup Password", color = palette.textPrimary) },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Encryption Passphrase") },
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (passwordInput.length >= 4) {
                        val chars = passwordInput.toCharArray()
                        passwordInput = ""
                        showEncryptDialog = false
                        hostCallbacks.exportEncryptedJsonBackup("tracmio_v16.13_encrypted.json", chars)
                    }
                }) { Text("Encrypt & Export") }
            },
            dismissButton = { TextButton(onClick = { showEncryptDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDecryptDialog) {
        AlertDialog(
            onDismissRequest = { showDecryptDialog = false }, containerColor = palette.surface,
            title = { Text("Decrypt Backup File", color = palette.textPrimary) },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Backup Passphrase") },
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (passwordInput.isNotEmpty()) {
                        val chars = passwordInput.toCharArray()
                        passwordInput = ""
                        showDecryptDialog = false
                        hostCallbacks.importJsonBackup(chars)
                    }
                }) { Text("Choose File") }
            },
            dismissButton = { TextButton(onClick = { showDecryptDialog = false }) { Text("Cancel") } }
        )
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false }, containerColor = palette.surface,
            title = { Text("Confirm Ledger Reset", color = Color(0xFFEF4444)) },
            text = { Text("This deletes all local transactions, budgets, goals, and loans permanently.", color = palette.textPrimary) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    onClick = { viewModel.clearAllData(); showWipeConfirm = false }
                ) { Text("Reset Everything") }
            },
            dismissButton = { TextButton(onClick = { showWipeConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun PermissionsAuditScreen(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    hostCallbacks: NativeHostCallbacks,
    onBack: () -> Unit
) {
    val notifGranted = hostCallbacks.hasNotificationPermission()
    val camGranted = hostCallbacks.hasCameraPermission()

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
            Text("✅ Offline Security & Permissions", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = palette.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Zero Network Permissions (100% Offline)", color = palette.primary, fontWeight = FontWeight.Bold)
                }
                Text("Tracmio declares NO INTERNET or ACCESS_NETWORK_STATE permissions and uses zero WebViews.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Text("Media Selection uses Android zero-permission Photo Picker & Storage Access Framework.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
        }

        GlassCard(settings = settings, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Notifications (POST_NOTIFICATIONS)", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(if (notifGranted) "Granted ✅" else "Not granted", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                    if (!notifGranted) {
                        Button(onClick = { hostCallbacks.requestNotificationPermission() }) { Text("Allow") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Camera (Optional Wallpaper Capture)", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(if (camGranted) "Granted ✅" else "Requested on camera tap", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                }
            }
        }
    }
}
