package com.charan.tracmio.ui.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charan.tracmio.data.MoneyPaise
import com.charan.tracmio.data.NativeLedgerCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemesAndAppearanceScreen(
    preferences: TracmioUserPreferences,
    onSelectBgTheme: (String) -> Unit,
    onSelectGlassUi: (String) -> Unit,
    onApplyCustomNeon: (String, String) -> Unit,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onChangePhotoOpacity: (Float) -> Unit,
    onChangeUiScale: (Float) -> Unit,
    onPreviewWelcomeAndSplash: () -> Unit,
    onBack: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    var customPrimary by remember(preferences.customPrimaryHex) {
        mutableStateOf(preferences.customPrimaryHex.ifBlank { "#22F59B" })
    }
    var customSecondary by remember(preferences.customSecondaryHex) {
        mutableStateOf(preferences.customSecondaryHex.ifBlank { "#18E0D0" })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("page_themes"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Tracmio3DBadge(icon = Icons.Default.Palette, tint = visuals.primaryAccent)
                Column {
                    Text(
                        text = "Themes & Appearance Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "10 Background Themes × 10 Liquid Glass UI Styles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "1. 10 Atmospheric Background Themes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select a deep multi-stop atmospheric canvas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TRACMIO_BG_THEMES.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { bg ->
                                val selected = preferences.bgThemeId == bg.id && preferences.customPrimaryHex.isBlank()
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onSelectBgTheme(bg.id) }
                                        .testTag("bg_theme_${bg.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = bg.bgTop,
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) bg.primary else Color.White.copy(alpha = 0.16f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(bg.primary)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(bg.secondary)
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Active",
                                                    tint = bg.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = bg.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = bg.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "2. 10 Liquid Glass UI Surface Styles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize card refraction, corner geometry & specular borders",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TRACMIO_GLASS_UI_STYLES.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { ui ->
                                val selected = preferences.glassUiId == ui.id
                                val shape = RoundedCornerShape(ui.cornerRadiusDp.coerceAtMost(24).dp)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shape)
                                        .clickable { onSelectGlassUi(ui.id) }
                                        .testTag("glass_ui_${ui.id}"),
                                    shape = shape,
                                    color = ui.tintColor.copy(alpha = ui.surfaceAlpha + 0.05f),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) visuals.primaryAccent else Color.White.copy(alpha = ui.borderTopAlpha)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "R:${ui.cornerRadiusDp}dp",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = visuals.primaryAccent
                                            )
                                            if (selected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Active",
                                                    tint = visuals.primaryAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = ui.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ui.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "3. Custom Neon Theme Studio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customPrimary,
                        onValueChange = { customPrimary = it },
                        label = { Text("Primary Hex") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customSecondary,
                        onValueChange = { customSecondary = it },
                        label = { Text("Secondary Hex") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onApplyCustomNeon(customPrimary, customSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = visuals.primaryAccent,
                        contentColor = Color(0xFF031A12)
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Custom Neon Palette", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "4. Personal Background Photo & Visibility",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onChoosePhoto,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = visuals.primaryAccent,
                            contentColor = Color(0xFF031A12)
                        )
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose Photo", fontWeight = FontWeight.Bold)
                    }
                    if (preferences.customBgImagePath.isNotBlank()) {
                        OutlinedButton(
                            onClick = onRemovePhoto,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Remove Photo")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Photo Visibility: ${(preferences.bgPhotoOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = preferences.bgPhotoOpacity,
                    onValueChange = onChangePhotoOpacity,
                    valueRange = 0f..1f
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "5. App UI Scale (80% – 130%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChangeUiScale((preferences.uiScale - 0.05f).coerceAtLeast(0.8f)) }
                    ) {
                        Text("− Smaller")
                    }
                    Text(
                        text = "${(preferences.uiScale * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = visuals.primaryAccent
                    )
                    OutlinedButton(
                        onClick = { onChangeUiScale((preferences.uiScale + 0.05f).coerceAtMost(1.3f)) }
                    ) {
                        Text("+ Larger")
                    }
                }
                Slider(
                    value = preferences.uiScale,
                    onValueChange = onChangeUiScale,
                    valueRange = 0.8f..1.3f
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onPreviewWelcomeAndSplash,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Replay 3D Opening Logo & Welcome Screen")
                }
            }
        }
    }
}

@Composable
fun BudgetsAndGoalsScreen(
    state: NativeTracmioUiState,
    currencySymbol: String,
    initialTab: Int = 0,
    onAddBudget: (String, Double, String) -> Unit,
    onDeleteBudget: (com.charan.tracmio.data.local.BudgetEntity) -> Unit,
    onAddGoal: (String, Double, Double) -> Unit,
    onContributeGoal: (Long, Double, Long?) -> Unit,
    onDeleteGoal: (com.charan.tracmio.data.local.GoalEntity) -> Unit,
    onBack: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var contributeGoalId by remember { mutableStateOf<Long?>(null) }

    val currentMonth = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Tracmio3DBadge(
                        icon = if (tab == 0) Icons.Default.PieChart else Icons.Default.Savings,
                        tint = visuals.primaryAccent
                    )
                    Text(
                        text = if (tab == 0) "Monthly Budgets" else "Savings Goals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Button(
                    onClick = { if (tab == 0) showBudgetDialog = true else showGoalDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = visuals.primaryAccent,
                        contentColor = Color(0xFF031A12)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (tab == 0) "Budget" else "Goal", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text("Budgets (${state.budgets.size})") }
                )
                FilterChip(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text("Savings Goals (${state.goals.size})") }
                )
            }
        }

        if (tab == 0) {
            if (state.budgets.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No monthly budgets yet. Tap '+ Budget' to set category spending caps with 80% & 100% threshold alerts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.budgets.size) { idx ->
                    val b = state.budgets[idx]
                    val spent = state.transactions
                        .filter {
                            it.type.equals("EXPENSE", true) &&
                                it.category.equals(b.category, true)
                        }
                        .sumOf { it.amount }
                    val ratio = (spent / b.limitAmount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    val barColor = when {
                        ratio >= 1.0f -> visuals.expenseColor
                        ratio >= 0.8f -> visuals.warningColor
                        else -> visuals.primaryAccent
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Tracmio3DBadge(
                                    icon = vectorIconForCategoryOrType(b.category),
                                    tint = barColor,
                                    size = 38.dp
                                )
                                Column {
                                    Text(b.category, fontWeight = FontWeight.Bold)
                                    Text(
                                        String.format(
                                            Locale.US,
                                            "%s%.2f of %s%.2f (%d%%)",
                                            currencySymbol,
                                            spent,
                                            currencySymbol,
                                            b.limitAmount,
                                            (ratio * 100).toInt()
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = barColor
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteBudget(b) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = barColor,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        } else {
            if (state.goals.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No savings goals yet. Tap '+ Goal' to create a piggy bank target.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.goals.size) { idx ->
                    val g = state.goals[idx]
                    val savedPaise = NativeLedgerCalculator.goalSavedPaise(g, state.goalContributions)
                    val saved = MoneyPaise.toRupees(savedPaise)
                    val ratio = (saved / g.targetAmount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Tracmio3DBadge(icon = Icons.Default.Flag, tint = visuals.primaryAccent)
                                Column {
                                    Text(g.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        String.format(
                                            Locale.US,
                                            "Saved %s%.2f / %s%.2f (%d%%)",
                                            currencySymbol,
                                            saved,
                                            currencySymbol,
                                            g.targetAmount,
                                            (ratio * 100).toInt()
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = visuals.primaryAccent
                                    )
                                }
                            }
                            Row {
                                OutlinedButton(onClick = { contributeGoalId = g.id }) {
                                    Text("+ Save")
                                }
                                IconButton(onClick = { onDeleteGoal(g) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = visuals.primaryAccent,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        var category by remember { mutableStateOf("Food & Dining") }
        var limitText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Create Monthly Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Monthly Limit ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = limitText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onAddBudget(category, amt, currentMonth)
                            showBudgetDialog = false
                        }
                    }
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showGoalDialog) {
        var name by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("") }
        var savedText by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Create Savings Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Target Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = savedText,
                        onValueChange = { savedText = it },
                        label = { Text("Initial Saved ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = targetText.toDoubleOrNull() ?: 0.0
                        val saved = savedText.toDoubleOrNull() ?: 0.0
                        if (target > 0) {
                            onAddGoal(name.ifBlank { "Emergency Fund" }, target, saved)
                            showGoalDialog = false
                        }
                    }
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (contributeGoalId != null) {
        var amountText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { contributeGoalId = null },
            title = { Text("Add Goal Contribution") },
            text = {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val gId = contributeGoalId
                        if (amt > 0 && gId != null) {
                            onContributeGoal(gId, amt, state.accounts.firstOrNull()?.id)
                            contributeGoalId = null
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { contributeGoalId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LoansAndPersonsScreen(
    state: NativeTracmioUiState,
    currencySymbol: String,
    initialTab: Int = 0,
    onAddLoan: (String, Double, Double, Double) -> Unit,
    onPayLoan: (Long, Double, Long?) -> Unit,
    onDeleteLoan: (com.charan.tracmio.data.local.LoanEntity) -> Unit,
    onAddPerson: (String, String) -> Unit,
    onDeletePerson: (com.charan.tracmio.data.local.PersonEntity) -> Unit,
    onBack: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showLoanDialog by remember { mutableStateOf(false) }
    var showPersonDialog by remember { mutableStateOf(false) }
    var payLoanId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Tracmio3DBadge(
                        icon = if (tab == 0) Icons.Default.Handshake else Icons.Default.People,
                        tint = visuals.primaryAccent
                    )
                    Text(
                        text = if (tab == 0) "Loans & EMI" else "People Ledger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Button(
                    onClick = { if (tab == 0) showLoanDialog = true else showPersonDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = visuals.primaryAccent,
                        contentColor = Color(0xFF031A12)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (tab == 0) "Loan" else "Person", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text("Loans (${state.loans.size})") }
                )
                FilterChip(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text("People (${state.persons.size})") }
                )
            }
        }

        if (tab == 0) {
            if (state.loans.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No active loans tracked. Tap '+ Loan' to track principal, paid amount, and EMI schedule.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.loans.size) { idx ->
                    val loan = state.loans[idx]
                    val paidPaise = NativeLedgerCalculator.loanPaidPaise(loan, state.loanPayments)
                    val remPaise = NativeLedgerCalculator.loanRemainingPaise(loan, state.loanPayments)
                    val paid = MoneyPaise.toRupees(paidPaise)
                    val rem = MoneyPaise.toRupees(remPaise)
                    val ratio = (paid / loan.principal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(loan.name, fontWeight = FontWeight.Bold)
                                Text(
                                    String.format(
                                        Locale.US,
                                        "Paid: %s%.2f  •  Remaining: %s%.2f",
                                        currencySymbol,
                                        paid,
                                        currencySymbol,
                                        rem
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                OutlinedButton(onClick = { payLoanId = loan.id }) {
                                    Text("Pay EMI")
                                }
                                IconButton(onClick = { onDeleteLoan(loan) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = visuals.primaryAccent
                        )
                    }
                }
            }
        } else {
            val summaries = state.personSummaries
            if (summaries.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Add contacts to track Borrowed and Lent balances per person.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(summaries.size) { idx ->
                    val s = summaries[idx]
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Tracmio3DBadge(icon = Icons.Default.People, tint = visuals.secondaryAccent)
                                Column {
                                    Text(s.person.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        String.format(
                                            Locale.US,
                                            "Lent: %s%.0f  •  Borrowed: %s%.0f",
                                            currencySymbol,
                                            s.lent,
                                            currencySymbol,
                                            s.borrowed
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.US, "%s%.2f", currencySymbol, s.net),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (s.net >= 0) visuals.incomeColor else visuals.expenseColor
                                )
                                IconButton(onClick = { onDeletePerson(s.person) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLoanDialog) {
        var name by remember { mutableStateOf("") }
        var principalText by remember { mutableStateOf("") }
        var emiText by remember { mutableStateOf("") }
        var rateText by remember { mutableStateOf("8.5") }
        AlertDialog(
            onDismissRequest = { showLoanDialog = false },
            title = { Text("Add Loan / EMI") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Loan Name") }, singleLine = true)
                    OutlinedTextField(
                        value = principalText,
                        onValueChange = { principalText = it },
                        label = { Text("Principal ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = emiText,
                        onValueChange = { emiText = it },
                        label = { Text("Monthly EMI ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Interest Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = principalText.toDoubleOrNull() ?: 0.0
                        val emi = emiText.toDoubleOrNull() ?: 0.0
                        val rate = rateText.toDoubleOrNull() ?: 0.0
                        if (p > 0) {
                            onAddLoan(name.ifBlank { "Loan" }, p, emi, rate)
                            showLoanDialog = false
                        }
                    }
                ) {
                    Text("Save Loan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoanDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPersonDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPersonDialog = false },
            title = { Text("Add Person") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Person Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (Optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddPerson(name, phone)
                            showPersonDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPersonDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (payLoanId != null) {
        var amountText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { payLoanId = null },
            title = { Text("Record Loan EMI Payment") },
            text = {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val lId = payLoanId
                        if (amt > 0 && lId != null) {
                            onPayLoan(lId, amt, state.accounts.firstOrNull()?.id)
                            payLoanId = null
                        }
                    }
                ) {
                    Text("Pay")
                }
            },
            dismissButton = {
                TextButton(onClick = { payLoanId = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndDataScreen(
    preferences: TracmioUserPreferences,
    onOpenThemesStudio: () -> Unit,
    onUpdateReminders: (Boolean, Int, Int, Boolean, String) -> Unit,
    onTriggerTestNotification: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onResetAllData: () -> Unit,
    onBack: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    var showResetConfirm by remember { mutableStateOf(false) }
    var remHour by remember(preferences.reminderHour) { mutableIntStateOf(preferences.reminderHour) }
    var remMinute by remember(preferences.reminderMinute) { mutableIntStateOf(preferences.reminderMinute) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("page_settings"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Tracmio3DBadge(icon = Icons.Default.Security, tint = visuals.primaryAccent)
                Column {
                    Text(
                        text = "Settings & Data Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Offline Backups, Smart Scheduled Alerts & Privacy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenThemesStudio,
                accentBorderColor = visuals.primaryAccent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Tracmio3DBadge(icon = Icons.Default.Palette, tint = visuals.primaryAccent)
                        Column {
                            Text(
                                text = "Themes & Appearance Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "10 Background Themes + 10 Liquid Glass UI Styles, Photo & Scale",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text("Open →", color = visuals.primaryAccent, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Tracmio3DBadge(icon = Icons.Default.NotificationsActive, tint = visuals.primaryAccent)
                        Column {
                            Text(
                                text = "Daily Smart Reminders (100 Alerts)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rotates offline through 100 witty meme & finance alerts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = preferences.reminderEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateReminders(
                                enabled,
                                remHour,
                                remMinute,
                                preferences.reminderMemeMode,
                                preferences.reminderAlertCategory
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Scheduled Alert Category Filter",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val catOptions = listOf(
                    "ALL" to "All 100 Alerts",
                    "MEME" to "Meme & Humor (25)",
                    "MILESTONES" to "Milestones & Streaks",
                    "HYGIENE" to "Financial Hygiene"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    catOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = preferences.reminderAlertCategory == key,
                            onClick = {
                                onUpdateReminders(
                                    preferences.reminderEnabled,
                                    remHour,
                                    remMinute,
                                    key == "MEME" || key == "ALL",
                                    key
                                )
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = visuals.primaryAccent.copy(alpha = 0.22f),
                                selectedLabelColor = visuals.primaryAccent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "Reminder Time: %02d:%02d", remHour, remMinute),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                remHour = (remHour + 23) % 24
                                onUpdateReminders(
                                    preferences.reminderEnabled,
                                    remHour,
                                    remMinute,
                                    preferences.reminderMemeMode,
                                    preferences.reminderAlertCategory
                                )
                            }
                        ) { Text("-1h") }
                        OutlinedButton(
                            onClick = {
                                remHour = (remHour + 1) % 24
                                onUpdateReminders(
                                    preferences.reminderEnabled,
                                    remHour,
                                    remMinute,
                                    preferences.reminderMemeMode,
                                    preferences.reminderAlertCategory
                                )
                            }
                        ) { Text("+1h") }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onTriggerTestNotification,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Scheduled Notification Preview Now")
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Data Management & Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportJson,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = visuals.primaryAccent,
                            contentColor = Color(0xFF031A12)
                        )
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Data", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onImportJson,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Data")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportExcel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TableView, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excel (.xlsx)")
                    }
                    OutlinedButton(
                        onClick = onExportPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Statement")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, visuals.expenseColor.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = visuals.expenseColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset All Data", color = visuals.expenseColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Privacy & App Identity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Storage Mode", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Local Only (All financial records stay on this device)", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Internet Connection", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Not required", fontWeight = FontWeight.SemiBold, color = visuals.primaryAccent)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "1.00",
                        fontWeight = FontWeight.ExtraBold,
                        color = visuals.primaryAccent,
                        modifier = Modifier.testTag("info_app_version")
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TracmioAnimated3DLogo(size = 76.dp, showSonarRings = false)
                    TracmioBrandWordmark(
                        fontSizeSp = 22,
                        showSubtitle = false,
                        horizontalAlignment = Alignment.CenterHorizontally
                    )
                    Text(
                        text = "FINANCE & EXPENSES TRACKER · v1.00",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Data?") },
            text = {
                Text("This will permanently erase all local accounts, transactions, budgets, goals, and loans on this device and relaunch the Welcome Screen.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = visuals.expenseColor)
                ) {
                    Text("Reset Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
