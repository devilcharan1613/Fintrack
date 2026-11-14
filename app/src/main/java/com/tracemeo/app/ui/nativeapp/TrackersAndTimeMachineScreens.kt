package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.CustomTrackersCalculator
import com.tracemeo.app.data.FinancialCalculator
import com.tracemeo.app.data.NativeTracmioUiState

@Composable
fun TrackersAndTimeMachineScreens(
    subRoute: String,
    state: NativeTracmioUiState,
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    viewModel: NativeTracmioViewModel,
    onSelectSubRoute: (String) -> Unit
) {
    val curr = settings.currencySymbol
    var showFolderDialog by remember { mutableStateOf(false) }
    var trackerTitle by remember { mutableStateOf("") }
    var trackerTarget by remember { mutableStateOf("10000") }
    var selectedType by remember { mutableStateOf("SAVINGS") }

    // Time Machine Simulator state
    var monthlySaveSim by remember { mutableStateOf("5000") }
    var yearsSim by remember { mutableStateOf("5") }
    var annualReturnPct by remember { mutableStateOf("12") }

    val summary = remember(state.customTrackers) {
        CustomTrackersCalculator.summarizeTrackers(state.customTrackers)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("custom_trackers" to "🎯 Custom Trackers & Challenges", "time_machine" to "⏳ Financial Time Machine").forEach { (key, label) ->
                    val sel = subRoute == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) palette.primary else palette.surface)
                            .clickable { onSelectSubRoute(key) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (subRoute == "time_machine") {
            item {
                val monthly = monthlySaveSim.toDoubleOrNull() ?: 5000.0
                val years = (yearsSim.toIntOrNull() ?: 5).coerceIn(1, 40)
                val rate = (annualReturnPct.toDoubleOrNull() ?: 12.0) / 100.0 / 12.0
                val months = years * 12
                val futureVal = if (rate > 0) {
                    monthly * ((Math.pow(1.0 + rate, months.toDouble()) - 1.0) / rate) * (1.0 + rate)
                } else {
                    monthly * months
                }
                val invested = monthly * months

                LiquidGlassComponents(settings = settings, palette = palette) {
                    Text("⏳ Financial Time Machine Simulator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.primary)
                    Text("Project compound wealth if daily habits or SIPs continue:", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = monthlySaveSim, onValueChange = { monthlySaveSim = it }, label = { Text("Monthly ($curr)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = yearsSim, onValueChange = { yearsSim = it }, label = { Text("Years") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = annualReturnPct, onValueChange = { annualReturnPct = it }, label = { Text("Return %") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Text("Projected Future Wealth: $curr${"%,.0f".format(futureVal)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = palette.primary)
                    Text("Total Principal Invested: $curr${"%,.0f".format(invested)} · Compound Gain: $curr${"%,.0f".format(futureVal - invested)}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            }
        } else {
            // Savings Challenge & Milestone Badge Tracker + Custom Trackers
            item {
                LiquidGlassComponents(settings = settings, palette = palette) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("🏆 Savings Challenge & Milestone Badges", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                            Text("${summary.completedCount}/${summary.totalTrackers} Trackers Completed · Saved $curr${"%,.0f".format(summary.totalSavedValue)}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        OutlinedButton(onClick = { showFolderDialog = true }, shape = RoundedCornerShape(12.dp)) {
                            Text("📂 Folders", color = palette.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SAVINGS", "MILESTONE", "HABIT", "COUNTER").forEach { t ->
                            val sel = selectedType == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) palette.primary else palette.surface)
                                    .clickable { selectedType = t }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t, style = MaterialTheme.typography.labelSmall, color = if (sel) palette.background else palette.textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = trackerTitle, onValueChange = { trackerTitle = it }, label = { Text("Challenge / Tracker Name") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = trackerTarget, onValueChange = { trackerTarget = it }, label = { Text("Target") }, singleLine = true, modifier = Modifier.width(110.dp))
                    }

                    Button(
                        onClick = {
                            val target = trackerTarget.toDoubleOrNull() ?: 1000.0
                            if (trackerTitle.isNotBlank() && target > 0.0) {
                                viewModel.addCustomTracker(trackerTitle.trim(), selectedType, target)
                                trackerTitle = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Start Challenge / Custom Tracker", color = palette.background, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            state.customTrackers.forEach { tracker ->
                item(key = tracker.id) {
                    val prog = CustomTrackersCalculator.computeProgress(tracker)
                    LiquidGlassComponents(settings = settings, palette = palette) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${tracker.icon} ${tracker.title} (${tracker.trackerType})", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                                Text("${"%,.0f".format(tracker.currentValue)} / ${"%,.0f".format(tracker.targetValue)} ${tracker.unit}", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.incrementCustomTracker(tracker, if (tracker.trackerType == "SAVINGS") 500.0 else 1.0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated)
                                ) {
                                    Text(if (tracker.trackerType == "SAVINGS") "+ ₹500" else "+1", color = palette.primary, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { viewModel.deleteCustomTracker(tracker) }) {
                                    Text("✕", color = Color(0xFFFB7185))
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { prog },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = palette.primary,
                            trackColor = palette.surfaceElevated
                        )
                    }
                }
            }
        }
    }

    if (showFolderDialog) {
        TrackerFolderDialog(
            folders = state.trackerFolders,
            palette = palette,
            onDismiss = { showFolderDialog = false },
            onAddFolder = { name, icon -> viewModel.addTrackerFolder(name, icon) },
            onDeleteFolder = { folder -> viewModel.deleteTrackerFolder(folder) }
        )
    }
}
