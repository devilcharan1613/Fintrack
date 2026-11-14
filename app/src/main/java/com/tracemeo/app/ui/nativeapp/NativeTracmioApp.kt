package com.tracmio.app.ui.nativeapp

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracmio.app.data.UpiSmsDraft
import com.tracmio.app.data.local.TransactionEntity

@Composable
fun NativeTracmioApp(
    viewModel: NativeTracmioViewModel,
    hostCallbacks: NativeHostCallbacks
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()
    val palette = ThemePresets.getPalette(settings.themePreset, settings.customPrimaryHex, settings.customSecondaryHex)

    val context = LocalContext.current
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(settings.deviceMotionEnabled) {
        if (!settings.deviceMotionEnabled) {
            tiltX = 0f; tiltY = 0f
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val vals = event?.values ?: return
                tiltX = (-vals[0] / 9.8f).coerceIn(-1f, 1f)
                tiltY = (vals[1] / 9.8f).coerceIn(-1f, 1f)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (accel != null) {
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val backStack = remember { mutableStateListOf("dashboard") }
    val currentRoute = backStack.lastOrNull() ?: "dashboard"

    fun navigateTo(route: String) {
        if (route == "cash_count") {
            return
        }
        val topTabs = setOf("dashboard", "accounts", "transactions", "more")
        if (route in topTabs) {
            backStack.clear()
            backStack.add(route)
        } else if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    var showAddTxSheet by remember { mutableStateOf(false) }
    var initialTxType by remember { mutableStateOf("EXPENSE") }
    var initialTxAccountId by remember { mutableStateOf<Long?>(null) }
    var initialSmsDraft by remember { mutableStateOf<UpiSmsDraft?>(null) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var receiptTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showCashCounter by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferFromAccountId by remember { mutableStateOf<Long?>(null) }
    var manualOnboardingOpen by remember { mutableStateOf(false) }
    val showOnboarding = !settings.hasCompletedOnboarding || manualOnboardingOpen

    fun handleRouteOrAction(route: String) {
        when (route) {
            "cash_count" -> showCashCounter = true
            "refunds" -> navigateTo("dashboard")
            "warranty_vault" -> navigateTo("transactions")
            else -> navigateTo(route)
        }
    }

    LaunchedEffect(state.pendingNotificationAction) {
        val act = state.pendingNotificationAction
        if (!act.isNullOrBlank()) {
            initialTxType = if (act.equals("income", true)) "INCOME" else "EXPENSE"
            initialTxAccountId = null
            initialSmsDraft = null
            editingTx = null
            showAddTxSheet = true
            viewModel.consumePendingNotificationAction()
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            val res = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (state.recentlyDeletedTransaction != null || state.hasPendingUndo) "UNDO" else null,
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed) {
                viewModel.performPendingUndo()
            } else {
                viewModel.clearStatusMessage()
            }
        }
    }

    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, settings.uiScale) {
        Density(density = baseDensity.density, fontScale = baseDensity.fontScale * settings.uiScale)
    }

    TracmioNativeTheme(settings = settings) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
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
                        SnackbarHost(snackbarHostState) { data ->
                            Snackbar(snackbarData = data, containerColor = palette.surfaceElevated, contentColor = palette.textPrimary)
                        }
                    },
                    topBar = {
                        TracmioTopBar(
                            settings = settings,
                            palette = palette,
                            onSingleTapPrivacy = { viewModel.togglePrivacyMask() },
                            onTripleTapStealth = { viewModel.toggleStealthMode() },
                            onOpenOnboarding = { manualOnboardingOpen = true },
                            onQuickAdd = {
                                initialTxType = "EXPENSE"
                                initialTxAccountId = null
                                initialSmsDraft = null
                                editingTx = null
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
                                    initialTxType = "EXPENSE"
                                    initialTxAccountId = null
                                    initialSmsDraft = null
                                    editingTx = null
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
                                    hostCallbacks = hostCallbacks,
                                    onOpenQuickTransaction = { txType ->
                                        initialTxType = txType
                                        initialTxAccountId = null
                                        initialSmsDraft = null
                                        editingTx = null
                                        showAddTxSheet = true
                                    },
                                    onOpenSmsDraft = { draft ->
                                        initialTxType = draft.type
                                        initialTxAccountId = null
                                        initialSmsDraft = draft
                                        editingTx = null
                                        showAddTxSheet = true
                                    },
                                    onOpenCashCounter = { showCashCounter = true },
                                    onOpenTransfer = { transferFromAccountId = null; showTransferDialog = true },
                                    onEditTransaction = { tx -> editingTx = tx; initialSmsDraft = null; showAddTxSheet = true },
                                    onShowReceipt = { tx -> receiptTx = tx },
                                    onNavigateRoute = { handleRouteOrAction(it) }
                                )
                                "accounts" -> AccountsScreen(
                                    state = state,
                                    settings = settings,
                                    palette = palette,
                                    viewModel = viewModel,
                                    onOpenCashCounter = { showCashCounter = true },
                                    onQuickTransactionForAccount = { accId, txType ->
                                        initialTxType = txType
                                        initialTxAccountId = accId
                                        initialSmsDraft = null
                                        editingTx = null
                                        showAddTxSheet = true
                                    },
                                    onTransferFromAccount = { accId ->
                                        transferFromAccountId = accId
                                        showTransferDialog = true
                                    }
                                )
                                "transactions" -> TransactionsListScreen(
                                    state = state,
                                    settings = settings,
                                    palette = palette,
                                    viewModel = viewModel,
                                    hostCallbacks = hostCallbacks,
                                    onEditTransaction = { tx -> editingTx = tx; initialSmsDraft = null; showAddTxSheet = true },
                                    onShowReceipt = { tx -> receiptTx = tx }
                                )
                                "more" -> MoreScreen(
                                    settings = settings,
                                    palette = palette,
                                    viewModel = viewModel,
                                    onNavigateRoute = { handleRouteOrAction(it) },
                                    onOpenCashCounter = { showCashCounter = true }
                                )
                                "tools_hub" -> ToolsHubFolderScreen(
                                    settings = settings,
                                    palette = palette,
                                    viewModel = viewModel,
                                    onNavigateRoute = { handleRouteOrAction(it) },
                                    onBack = {
                                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                    }
                                )
                                "planning", "runway" -> PlanningScreen(state, settings, palette, viewModel, initialTab = 0) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "wishlist", "cashback", "price_per_use", "round_up" -> PlanningScreen(state, settings, palette, viewModel, initialTab = 1) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "debt_planner" -> PlanningScreen(state, settings, palette, viewModel, initialTab = 2) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "people" -> PeopleScreen(state, settings, palette, viewModel, hostCallbacks, initialTab = "CONTACTS") {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "khata" -> PeopleScreen(state, settings, palette, viewModel, hostCallbacks, initialTab = "KHATA") {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "reports" -> ReportsScreen(state, settings, palette, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "subscriptions" -> SubscriptionsScreen(state, settings, palette, viewModel) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "xray" -> FinancialXRayScreen(state, settings, palette, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "story" -> MoneyStoryScreen(state, settings, palette, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "dna" -> SpendingDnaScreen(state, settings, palette, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "global_search" -> GlobalMultiSearchScreen(state, settings, palette, ::handleRouteOrAction) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "calendar", "recharge" -> CalendarAndAcademicScreen(state, settings, palette, viewModel, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "time_machine" -> TimeMachineScreen(state, settings, palette, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "trackers" -> CustomTrackersScreen(state, settings, palette, viewModel, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "themes" -> ThemesAndAppearanceStudio(settings, palette, viewModel, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "reminders" -> MultiRemindersScreen(settings, palette, viewModel, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "data_export" -> DataAndVaultScreen(state, settings, palette, viewModel, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "app_lock" -> AppLockSetupScreen(state, settings, palette, viewModel, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "permissions" -> PermissionsAuditScreen(settings, palette, hostCallbacks) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                "guide" -> InAppGuideScreen(settings, palette, ::handleRouteOrAction) {
                                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else navigateTo("more")
                                }
                                else -> DashboardScreen(
                                    state = state,
                                    settings = settings,
                                    palette = palette,
                                    viewModel = viewModel,
                                    hostCallbacks = hostCallbacks,
                                    onOpenQuickTransaction = { t -> initialTxType = t; editingTx = null; showAddTxSheet = true },
                                    onOpenSmsDraft = { d -> initialTxType = d.type; initialSmsDraft = d; editingTx = null; showAddTxSheet = true },
                                    onOpenCashCounter = { showCashCounter = true },
                                    onOpenTransfer = { transferFromAccountId = null; showTransferDialog = true },
                                    onEditTransaction = { tx -> editingTx = tx; showAddTxSheet = true },
                                    onShowReceipt = { receiptTx = it },
                                    onNavigateRoute = ::handleRouteOrAction
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
                        initialType = initialTxType,
                        initialAccountId = initialTxAccountId,
                        initialSmsDraft = initialSmsDraft,
                        editing = editingTx,
                        viewModel = viewModel,
                        hostCallbacks = hostCallbacks,
                        onDismiss = {
                            showAddTxSheet = false
                            editingTx = null
                            initialTxAccountId = null
                            initialSmsDraft = null
                        }
                    )
                }

                receiptTx?.let { tx ->
                    val accName = state.accounts.firstOrNull { it.id == tx.accountId }?.name ?: "Wallet"
                    VisualReceiptDialog(
                        transaction = tx,
                        accountName = accName,
                        settings = settings,
                        palette = palette,
                        onDismiss = { receiptTx = null },
                        onRepeat = { viewModel.repeatTransaction(it) },
                        onEdit = { editingTx = it; showAddTxSheet = true },
                        onDelete = { viewModel.deleteTransaction(it) },
                        onPrintPdf = { t, a -> hostCallbacks.printReceipt(t, a) }
                    )
                }

                if (showCashCounter) {
                    CashDenominationDialog(
                        accounts = state.accounts,
                        settings = settings,
                        palette = palette,
                        onDismiss = { showCashCounter = false },
                        onApplyReconcile = { lines, note, acc ->
                            viewModel.applyCashDenominationCount(lines, note, acc)
                            showCashCounter = false
                        }
                    )
                }

                if (showTransferDialog) {
                    TransferFundsDialog(
                        accounts = state.accounts,
                        initialFromId = transferFromAccountId,
                        palette = palette,
                        onDismiss = { showTransferDialog = false; transferFromAccountId = null },
                        onTransfer = { from, to, amt, note ->
                            viewModel.transferFunds(from, to, amt, note)
                            showTransferDialog = false
                            transferFromAccountId = null
                        }
                    )
                }

                if (showOnboarding && !(state.isAppLocked && state.appLockState.isEnabled)) {
                    WelcomeOnboardingDialog(
                        settings = settings,
                        palette = palette,
                        isFirstLaunch = !settings.hasCompletedOnboarding,
                        onDismiss = {
                            if (!settings.hasCompletedOnboarding) {
                                viewModel.completeOnboarding(settings.userName, settings.currencySymbol)
                            }
                            manualOnboardingOpen = false
                        },
                        onComplete = { name, sym ->
                            viewModel.completeOnboarding(name, sym)
                            manualOnboardingOpen = false
                        }
                    )
                }

                // Liquid Glass App Lock Gate
                if (state.isAppLocked && state.appLockState.isEnabled) {
                    AppLockOverlayScreen(
                        lockState = state.appLockState,
                        settings = settings,
                        palette = palette,
                        hostCallbacks = hostCallbacks,
                        onVerifyPin = { pin -> viewModel.unlockWithPin(pin) },
                        onBiometricUnlock = { viewModel.unlockWithBiometricSuccess() },
                        onRecoverWithSecret = { secret, newPin -> viewModel.recoverPinWithSecret(secret, newPin) }
                    )
                }
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

    val navBarBg = if (palette.isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xF0FFFFFF),
                Color(0xEBE4F4F8)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xE60E3040),
                Color(0xF5051824)
            )
        )
    }

    val navBarBorder = if (palette.isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                Color(0xFF4696AA).copy(alpha = 0.36f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                Color(0xFF67E8F9).copy(alpha = 0.58f),
                Color(0xFFA855F7).copy(alpha = 0.38f)
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(brush = navBarBg)
            .border(
                width = 1.6.dp,
                brush = navBarBorder,
                shape = RoundedCornerShape(30.dp)
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leftTabs.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            BottomNavTabItem(
                route = route,
                label = label,
                icon = icon,
                selected = selected,
                palette = palette,
                onClick = { onSelectTab(route) },
                modifier = Modifier.weight(1f)
            )
        }

        // Center glowing + button (.nav-add) matching APK & HTML prototype
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (palette.isLight) {
                            listOf(Color(0xFF26D0B8), Color(0xFF009F91))
                        } else {
                            listOf(Color(0xFF72FACA), Color(0xFF00E5BE), Color(0xFF00B496))
                        }
                    )
                )
                .border(
                    width = 2.dp,
                    color = if (palette.isLight) Color.White else Color(0xFFCCFBF1).copy(alpha = 0.90f),
                    shape = CircleShape
                )
                .clickable(onClick = onQuickAdd)
                .testTag("nav_quick_add_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick Add Transaction",
                tint = if (palette.isLight) Color.White else Color(0xFF03221C),
                modifier = Modifier.size(28.dp)
            )
        }

        rightTabs.forEach { (route, label, icon) ->
            val selected = currentRoute == route || (route == "more" && currentRoute == "tools_hub")
            BottomNavTabItem(
                route = route,
                label = label,
                icon = icon,
                selected = selected,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    palette: ThemePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBg = if (palette.isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF009F91).copy(alpha = 0.16f),
                Color(0xFF009F91).copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF00F5D4).copy(alpha = 0.22f),
                Color(0xFF08444E).copy(alpha = 0.65f)
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(brush = activeBg)
                        .border(
                            width = 1.4.dp,
                            color = if (palette.isLight) Color(0xFF009F91).copy(alpha = 0.50f) else Color(0xFF4AF7E4).copy(alpha = 0.72f),
                            shape = RoundedCornerShape(20.dp)
                        )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("nav_$route")
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) {
                if (palette.isLight) palette.primary else Color(0xFF36FFEB)
            } else {
                palette.textSecondary
            },
            modifier = Modifier.size(22.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (selected) {
                if (palette.isLight) palette.primary else Color(0xFF36FFEB)
            } else {
                palette.textSecondary
            },
            maxLines = 1
        )
    }
}
