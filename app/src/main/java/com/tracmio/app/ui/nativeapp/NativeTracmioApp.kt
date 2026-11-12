package com.tracmio.app.ui.nativeapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracmio.app.data.local.TransactionEntity
import kotlinx.coroutines.launch

@Composable
fun NativeTracmioApp(
    viewModel: NativeTracmioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()
    val palette = ThemePresets.getPalette(settings.themePreset)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation State
    var currentScreen by remember { mutableStateOf("dashboard") }

    // Modal Sheet States
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionForReceipt by remember { mutableStateOf<TransactionEntity?>(null) }
    var defaultAddTxType by remember { mutableStateOf("EXPENSE") }
    var showCashCounterDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Device Tilt Sensor (Low-pass filtered for smooth liquid parallax without lag)
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(settings.deviceMotionEnabled) {
        if (!settings.deviceMotionEnabled) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                // Low-pass filter
                val rawX = event.values[0] / 9.8f
                val rawY = event.values[1] / 9.8f
                tiltX += (rawX - tiltX) * 0.15f
                tiltY += (rawY - tiltY) * 0.15f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Status Message & Undo Snackbar Handling
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            if (state.recentlyDeletedTransaction != null) {
                val result = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDeleteTransaction()
                }
            } else {
                snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            }
            viewModel.clearStatusMessage()
        }
    }

    // BackHandler for sub-screens
    BackHandler(enabled = currentScreen != "dashboard") {
        when (currentScreen) {
            "tools_hub" -> currentScreen = "more"
            "guide", "themes", "reminders", "trackers", "time_machine", "subscriptions", "xray", "reports", "transactions_search" -> {
                currentScreen = "tools_hub"
            }
            else -> currentScreen = "dashboard"
        }
    }

    TracmioNativeTheme {
        Box(modifier = modifier.fillMaxSize()) {
            // Liquid Glass 3D Mesh Aurora Canvas Background
            LiquidMeshBackground(
                settings = settings,
                tiltX = tiltX,
                tiltY = tiltY
            )

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (isMainTabScreen(currentScreen)) {
                        FloatingGlassBottomNav(
                            currentTab = currentScreen,
                            palette = palette,
                            onTabSelected = { currentScreen = it },
                            onOpenAdd = {
                                defaultAddTxType = "EXPENSE"
                                transactionToEdit = null
                                showAddTransactionSheet = true
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        "dashboard" -> DashboardScreen(
                            state = state,
                            settings = settings,
                            onTogglePrivacy = { viewModel.togglePrivacyMask() },
                            onOpenAddTransaction = { type ->
                                defaultAddTxType = type
                                transactionToEdit = null
                                showAddTransactionSheet = true
                            },
                            onEditTransaction = { tx ->
                                transactionToEdit = tx
                                showAddTransactionSheet = true
                            },
                            onViewReceipt = { tx -> transactionForReceipt = tx },
                            onOpenCashCounter = { showCashCounterDialog = true },
                            onOpenTransfer = { showTransferDialog = true }
                        )

                        "accounts" -> AccountsScreen(
                            state = state,
                            settings = settings,
                            onAddAccount = { name, type, bal, inst ->
                                viewModel.addAccount(name = name, type = type, balance = bal, institution = inst)
                            },
                            onOpenCashCounter = { showCashCounterDialog = true }
                        )

                        "transactions" -> TransactionsListScreen(
                            state = state,
                            settings = settings,
                            onEditTransaction = { tx ->
                                transactionToEdit = tx
                                showAddTransactionSheet = true
                            },
                            onViewReceipt = { tx -> transactionForReceipt = tx },
                            onOpenAdd = { type ->
                                defaultAddTxType = type
                                transactionToEdit = null
                                showAddTransactionSheet = true
                            }
                        )

                        "more" -> MoreScreen(
                            state = state,
                            settings = settings,
                            onNavigateToTool = { route -> currentScreen = route }
                        )

                        "tools_hub" -> ToolsHubScreen(
                            state = state,
                            settings = settings,
                            onBack = { currentScreen = "more" },
                            onOpenTool = { route -> currentScreen = route }
                        )

                        "guide" -> InAppGuideScreen(
                            settings = settings,
                            onBack = { currentScreen = "tools_hub" }
                        )

                        "themes" -> ThemesAndAppearanceStudio(
                            settings = settings,
                            onUpdateSettings = { viewModel.updateSettings(it) },
                            onBack = { currentScreen = "tools_hub" }
                        )

                        "reminders" -> MultiRemindersScreen(
                            settings = settings,
                            onBack = { currentScreen = "tools_hub" }
                        )

                        "trackers" -> CustomTrackersHubScreen(
                            trackers = state.customTrackers,
                            transactions = state.transactions,
                            accounts = state.accounts,
                            settings = settings,
                            onCreateTracker = { name, cat, budget, sDate, eDate ->
                                viewModel.createCustomTracker(name, cat, budget, sDate, eDate)
                            },
                            onExportTrackerWorkbook = {}
                        )

                        "time_machine" -> FinancialTimeMachineScreen(
                            state = state,
                            settings = settings
                        )

                        "reports" -> ReportsAndIntelligenceScreen(
                            state = state,
                            settings = settings
                        )

                        "subscriptions" -> SubscriptionsScreen(
                            subscriptions = state.subscriptions,
                            settings = settings,
                            onBack = { currentScreen = "tools_hub" },
                            onAddSubscription = {}
                        )

                        "xray" -> FinancialXRayScreen(
                            state = state,
                            settings = settings,
                            onBack = { currentScreen = "tools_hub" }
                        )
                    }
                }
            }

            // Visual Receipt Slip Dialog (Requirement #3)
            if (transactionForReceipt != null) {
                VisualReceiptDialog(
                    transaction = transactionForReceipt!!,
                    accounts = state.accounts,
                    palette = palette,
                    onDismiss = { transactionForReceipt = null },
                    onEdit = {
                        val tx = transactionForReceipt
                        transactionForReceipt = null
                        transactionToEdit = tx
                        showAddTransactionSheet = true
                    },
                    onDelete = {
                        val tx = transactionForReceipt
                        transactionForReceipt = null
                        if (tx != null) viewModel.deleteTransaction(tx)
                    },
                    onPrint = {
                        transactionForReceipt = null
                        // Print action
                    }
                )
            }

            // Add / Edit Transaction Sheet (Requirements #3 & #4)
            if (showAddTransactionSheet) {
                AddEditTransactionSheet(
                    initialTransaction = transactionToEdit,
                    defaultType = defaultAddTxType,
                    accounts = state.accounts,
                    categories = state.categories,
                    palette = palette,
                    onDismiss = {
                        showAddTransactionSheet = false
                        transactionToEdit = null
                    },
                    onSave = { id, accId, type, amt, cat, note, method, upi ->
                        viewModel.saveOrUpdateTransaction(id, accId, type, amt, cat, note, method, upi)
                        showAddTransactionSheet = false
                        transactionToEdit = null
                    },
                    onAddNewAccount = { name, accType, bal ->
                        viewModel.addAccount(name = name, type = accType, balance = bal)
                    }
                )
            }

            // Cash Denomination Counter Dialog
            if (showCashCounterDialog) {
                CashDenominationDialog(
                    initialCounts = settings.cashDenominations,
                    palette = palette,
                    onDismiss = { showCashCounterDialog = false },
                    onSaveCounts = { counts, total ->
                        viewModel.updateCashDenominations(counts)
                    }
                )
            }

            // Transfer Funds Dialog
            if (showTransferDialog) {
                TransferFundsDialog(
                    accounts = state.accounts,
                    palette = palette,
                    onDismiss = { showTransferDialog = false },
                    onConfirmTransfer = { from, to, amt ->
                        viewModel.transferFunds(from, to, amt)
                    }
                )
            }
        }
    }
}

fun isMainTabScreen(screen: String): Boolean {
    return screen in listOf("dashboard", "accounts", "transactions", "more")
}

@Composable
fun FloatingGlassBottomNav(
    currentTab: String,
    palette: ThemePalette,
    onTabSelected: (String) -> Unit,
    onOpenAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xDD0D1626),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Tab
                NavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = currentTab == "dashboard",
                    palette = palette,
                    onClick = { onTabSelected("dashboard") }
                )

                // Accounts Tab
                NavItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Wallets",
                    isSelected = currentTab == "accounts",
                    palette = palette,
                    onClick = { onTabSelected("accounts") }
                )

                // Elevated Center + Add Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(palette.primary)
                        .clickable(onClick = onOpenAdd)
                        .testTag("center_add_tx_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction", tint = Color.Black, modifier = Modifier.size(28.dp))
                }

                // Transactions Tab
                NavItem(
                    icon = Icons.Default.ReceiptLong,
                    label = "Ledger",
                    isSelected = currentTab == "transactions",
                    palette = palette,
                    onClick = { onTabSelected("transactions") }
                )

                // More Tab
                NavItem(
                    icon = Icons.Default.Menu,
                    label = "More",
                    isSelected = currentTab == "more",
                    palette = palette,
                    onClick = { onTabSelected("more") }
                )
            }
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    palette: ThemePalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) palette.primary else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) palette.primary else Color(0xFF94A3B8),
            fontSize = 10.sp
        )
    }
}
