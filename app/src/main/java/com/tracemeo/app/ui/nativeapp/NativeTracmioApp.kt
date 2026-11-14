package com.tracemeo.app.ui.nativeapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracemeo.app.data.local.TransactionEntity

@Composable
fun NativeTracmioApp(
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()
    val palette = ThemePresets.getPalette(settings.themePreset, settings.customPrimaryHex, settings.customSecondaryHex)

    val backStack = remember { mutableStateListOf("dashboard") }
    val currentRoute = backStack.lastOrNull() ?: "dashboard"
    val topTabs = remember { setOf("dashboard", "accounts", "transactions", "more") }

    fun navigateTo(route: String) {
        if (route in topTabs) {
            backStack.clear()
            backStack.add(route)
        } else if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else if (currentRoute != "dashboard") {
            backStack.clear()
            backStack.add("dashboard")
        }
    }

    BackHandler(enabled = backStack.size > 1 || currentRoute != "dashboard") {
        navigateBack()
    }

    // Subtle SensorManager tilt parallax (updates floats without triggering full-tree recomposition)
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    DisposableEffect(settings.deviceMotionEnabled) {
        if (!settings.deviceMotionEnabled) {
            tiltX = 0f
            tiltY = 0f
            onDispose { }
        } else {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val nx = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                    val ny = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                    tiltX = tiltX * 0.85f + nx * 0.15f
                    tiltY = tiltY * 0.85f + ny * 0.15f
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            if (accel != null) {
                sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose { sm?.unregisterListener(listener) }
        }
    }

    var showAddTxSheet by remember { mutableStateOf(false) }
    var addTxInitialType by remember { mutableStateOf("EXPENSE") }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var receiptTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Trigger Add Sheet when launched from notification quick action (+ Add income / + Add expense)
    LaunchedEffect(state.pendingNotificationAction) {
        when (state.pendingNotificationAction?.lowercase()) {
            "income" -> {
                editingTransaction = null
                addTxInitialType = "INCOME"
                showAddTxSheet = true
                viewModel.consumePendingNotificationAction()
            }
            "expense" -> {
                editingTransaction = null
                addTxInitialType = "EXPENSE"
                showAddTxSheet = true
                viewModel.consumePendingNotificationAction()
            }
        }
    }

    // Status message + 2-second UNDO Snackbar
    LaunchedEffect(state.statusMessage, state.hasPendingUndo) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            val res = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (state.hasPendingUndo) "UNDO" else null,
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed && state.hasPendingUndo) {
                viewModel.performPendingUndo()
            }
            viewModel.clearStatusMessage()
        }
    }

    TracmioNativeTheme(settings = settings) {
        Box(modifier = Modifier.fillMaxSize()) {
            LiquidMeshBackground(
                settings = settings,
                tiltXProvider = { tiltX },
                tiltYProvider = { tiltY }
            )

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(bottom = 86.dp)
                    )
                },
                topBar = {
                    TracmioTopBar(
                        settings = settings,
                        palette = palette,
                        onSingleTapPrivacy = { viewModel.togglePrivacyMask() },
                        onTripleTapStealth = {
                            viewModel.toggleStealthMode()
                            viewModel.postStatus(
                                if (!settings.stealthModeActive) "Stealth Mode Activated" else "Stealth Mode Off"
                            )
                        },
                        onQuickAdd = {
                            editingTransaction = null
                            addTxInitialType = "EXPENSE"
                            showAddTxSheet = true
                        },
                        modifier = Modifier.statusBarsPadding()
                    )
                },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        FloatingGlassBottomNav(
                            currentRoute = currentRoute,
                            palette = palette,
                            onSelectTab = { navigateTo(it) },
                            onQuickAdd = {
                                editingTransaction = null
                                addTxInitialType = "EXPENSE"
                                showAddTxSheet = true
                            }
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    AnimatedContent(
                        targetState = currentRoute,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tracmio_screen"
                    ) { route ->
                        when (route) {
                            "dashboard" -> DashboardScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                onQuickAdd = {
                                    editingTransaction = null
                                    addTxInitialType = "EXPENSE"
                                    showAddTxSheet = true
                                },
                                onOpenTransactionReceipt = { receiptTransaction = it },
                                onNavigate = { navigateTo(it) }
                            )
                            "accounts" -> AccountsScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel
                            )
                            "transactions" -> TransactionsListScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                onOpenReceipt = { receiptTransaction = it }
                            )
                            "more" -> MoreScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                onNavigate = { navigateTo(it) }
                            )
                            "tools_hub" -> ToolsHubFolderScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                onBack = { navigateBack() },
                                onNavigate = { navigateTo(it) }
                            )
                            "guide" -> InAppGuideScreen(
                                settings = settings,
                                palette = palette,
                                onBack = { navigateBack() },
                                onNavigate = { navigateTo(it) }
                            )
                            "cash_counter" -> StandaloneCashCounterScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            // Planning, People, Khata, Wishlist, Cashback, PPU, Round-Up, Runway, Debt Planner
                            "people", "khata" -> PlanningAndPeopleScreens(
                                subRoute = "people",
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                hostCallbacks = hostCallbacks,
                                onSelectSubRoute = { navigateTo(it) }
                            )
                            "planning", "budgets", "runway" -> PlanningAndPeopleScreens(
                                subRoute = "budgets",
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                hostCallbacks = hostCallbacks,
                                onSelectSubRoute = { navigateTo(it) }
                            )
                            "goals", "wishlist", "cashback", "price_per_use", "round_up" -> PlanningAndPeopleScreens(
                                subRoute = "goals",
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                hostCallbacks = hostCallbacks,
                                onSelectSubRoute = { navigateTo(it) }
                            )
                            "loans", "debt_planner" -> PlanningAndPeopleScreens(
                                subRoute = "loans",
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                hostCallbacks = hostCallbacks,
                                onSelectSubRoute = { navigateTo(it) }
                            )
                            // Reports & Intelligence
                            "reports", "financial_xray", "money_story", "spending_dna", "calendar" -> ReportsAndIntelligenceScreens(
                                subRoute = route,
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                onSelectSubRoute = { navigateTo(it) }
                            )
                            // Trackers & Time Machine
                            "custom_trackers", "time_machine" -> TrackersAndTimeMachineScreens(
                                subRoute = route,
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                onSelectSubRoute = { navigateTo(it) }
                            )
                            // Tools Hub utility screens
                            "refunds" -> PendingRefundsScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            "subscriptions", "recharge", "academic" -> SubscriptionsAndAcademicScreen(
                                subRoute = route,
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                onBack = { navigateBack() },
                                onSelectSubRoute = { navigateTo(it) }
                            )
                            "global_search" -> GlobalSearchScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                onBack = { navigateBack() },
                                onNavigate = { navigateTo(it) }
                            )
                            "reminders", "warranty_vault" -> RemindersAndWarrantyScreen(
                                subRoute = route,
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                hostCallbacks = hostCallbacks,
                                onBack = { navigateBack() },
                                onSelectSubRoute = { navigateTo(it) }
                            )
                            "data_export" -> DataAndExportScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                hostCallbacks = hostCallbacks,
                                onBack = { navigateBack() }
                            )
                            "permissions" -> PrivacyAndSecurityAuditScreen(
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                hostCallbacks = hostCallbacks,
                                onBack = { navigateBack() }
                            )
                            "app_lock" -> AppLockScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                hostCallbacks = hostCallbacks,
                                onBack = { navigateBack() }
                            )
                            "themes" -> ThemesStudioScreen(
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                hostCallbacks = hostCallbacks,
                                onBack = { navigateBack() }
                            )
                            else -> DashboardScreen(
                                state = state,
                                settings = settings,
                                palette = palette,
                                viewModel = viewModel,
                                onQuickAdd = {
                                    editingTransaction = null
                                    addTxInitialType = "EXPENSE"
                                    showAddTxSheet = true
                                },
                                onOpenTransactionReceipt = { receiptTransaction = it },
                                onNavigate = { navigateTo(it) }
                            )
                        }
                    }
                }
            }

            if (showAddTxSheet) {
                AddEditTransactionSheet(
                    state = state,
                    settings = settings,
                    palette = palette,
                    viewModel = viewModel,
                    editingTransaction = editingTransaction,
                    initialType = addTxInitialType,
                    onDismiss = {
                        showAddTxSheet = false
                        editingTransaction = null
                    }
                )
            }

            receiptTransaction?.let { tx ->
                ExpenseReceiptModal(
                    transaction = tx,
                    palette = palette,
                    currencySymbol = settings.currencySymbol,
                    onDismiss = { receiptTransaction = null },
                    onDelete = {
                        viewModel.deleteTransaction(it)
                        receiptTransaction = null
                    },
                    onEdit = {
                        receiptTransaction = null
                        editingTransaction = it
                        addTxInitialType = it.type
                        showAddTxSheet = true
                    },
                    onRepeat = {
                        viewModel.repeatTransaction(it)
                        receiptTransaction = null
                    }
                )
            }

            if (!settings.hasCompletedOnboarding) {
                OnboardingDialog(
                    settings = settings,
                    palette = palette,
                    onComplete = { name, curr ->
                        viewModel.completeOnboarding(name, curr)
                    }
                )
            }

            if (state.isAppLocked) {
                AppLockOverlayScreen(
                    state = state,
                    settings = settings,
                    palette = palette,
                    viewModel = viewModel,
                    hostCallbacks = hostCallbacks
                )
            }
        }
    }
}

@Composable
fun TracmioTopBar(
    settings: LiquidGlassSettings,
    palette: ThemePalette,
    onSingleTapPrivacy: () -> Unit,
    onTripleTapStealth: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceElevated)
                    .border(1.5.dp, palette.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("₹", fontWeight = FontWeight.ExtraBold, color = palette.primary)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trac", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                    Text("mio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = palette.primary)
                }
                Text(
                    if (settings.stealthModeActive) "Stealth Mode Active" else "Offline Finance (${settings.currencySymbol})",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastTapMs <= 750L) {
                        tapCount += 1
                    } else {
                        tapCount = 1
                    }
                    lastTapMs = now
                    if (tapCount >= 3) {
                        tapCount = 0
                        onTripleTapStealth()
                    } else {
                        onSingleTapPrivacy()
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(palette.surface, CircleShape)
                    .border(1.dp, palette.primary.copy(alpha = 0.4f), CircleShape)
                    .testTag("privacy_eye_button")
            ) {
                Icon(
                    imageVector = if (settings.privacyMaskActive || settings.stealthModeActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Privacy Eye",
                    tint = palette.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Button(
                onClick = onQuickAdd,
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("topbar_quick_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = palette.background, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", color = palette.background, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FloatingGlassBottomNav(
    currentRoute: String,
    palette: ThemePalette,
    onSelectTab: (String) -> Unit,
    onQuickAdd: () -> Unit
) {
    val leftTabs = listOf(
        Triple("dashboard", "Home", Icons.Default.Dashboard),
        Triple("accounts", "Accounts", Icons.Default.AccountBalanceWallet)
    )
    val rightTabs = listOf(
        Triple("transactions", "Transactions", Icons.AutoMirrored.Filled.ReceiptLong),
        Triple("more", "More", Icons.Default.GridView)
    )
    val coreTabRoutes = remember { setOf("dashboard", "accounts", "transactions") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(palette.surface.copy(alpha = 0.90f))
            .border(1.6.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leftTabs.forEach { (route, label, icon) ->
            BottomNavTabItem(
                route = route,
                label = label,
                icon = icon,
                selected = currentRoute == route,
                palette = palette,
                onClick = { onSelectTab(route) },
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(palette.primary)
                .clickable(onClick = onQuickAdd)
                .testTag("nav_quick_add_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = palette.background, modifier = Modifier.size(28.dp))
        }

        rightTabs.forEach { (route, label, icon) ->
            val isSelected = currentRoute == route || (route == "more" && currentRoute !in coreTabRoutes)
            BottomNavTabItem(
                route = route,
                label = label,
                icon = icon,
                selected = isSelected,
                palette = palette,
                onClick = { onSelectTab(route) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavTabItem(
    route: String,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    palette: ThemePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (selected) Modifier.background(palette.primary.copy(alpha = 0.2f)).border(1.4.dp, palette.primary, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("bottom_nav_$route")
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) palette.primary else palette.textSecondary, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (selected) palette.primary else palette.textSecondary,
            maxLines = 1
        )
    }
}
