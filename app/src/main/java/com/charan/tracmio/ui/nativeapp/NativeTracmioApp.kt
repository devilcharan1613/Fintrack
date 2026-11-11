package com.charan.tracmio.ui.nativeapp

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charan.tracmio.data.TransactionDateTime
import com.charan.tracmio.data.local.AccountEntity
import com.charan.tracmio.data.local.TransactionEntity
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

enum class TracmioScreen {
    HOME,
    ACCOUNTS,
    TRANSACTIONS,
    MORE,
    REPORTS,
    THEMES,
    BUDGETS,
    GOALS,
    LOANS,
    PERSONS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeTracmioApp(
    viewModel: NativeTracmioViewModel,
    initialQuickAction: String? = null,
    onConsumeQuickAction: () -> Unit = {},
    onChooseBackgroundPhoto: () -> Unit = {},
    onExportBackupJson: () -> Unit = {},
    onImportBackupJson: () -> Unit = {},
    onExportExcelWorkbook: () -> Unit = {},
    onExportPdfStatement: () -> Unit = {},
    onScheduleReminder: (Boolean, Int, Int, Boolean, String) -> Unit = { _, _, _, _, _ -> },
    onTriggerTestNotification: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(TracmioScreen.HOME) }
    var showAddTxnSheet by remember { mutableStateOf(false) }
    var initialTxnType by remember { mutableStateOf("EXPENSE") }
    var showOpeningSplash by remember { mutableStateOf(true) }
    var isBottomDockVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1350L)
        showOpeningSplash = false
    }

    LaunchedEffect(initialQuickAction) {
        if (!initialQuickAction.isNullOrBlank()) {
            initialTxnType = if (initialQuickAction.equals("income", ignoreCase = true)) {
                "INCOME"
            } else {
                "EXPENSE"
            }
            showAddTxnSheet = true
            onConsumeQuickAction()
        }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    isBottomDockVisible = false
                } else if (available.y > 10f) {
                    isBottomDockVisible = true
                }
                return Offset.Zero
            }
        }
    }

    BackHandler(enabled = showAddTxnSheet || currentScreen != TracmioScreen.HOME) {
        when {
            showAddTxnSheet -> showAddTxnSheet = false
            currentScreen in setOf(
                TracmioScreen.REPORTS,
                TracmioScreen.THEMES,
                TracmioScreen.BUDGETS,
                TracmioScreen.GOALS,
                TracmioScreen.LOANS,
                TracmioScreen.PERSONS,
                TracmioScreen.SETTINGS
            ) -> {
                currentScreen = TracmioScreen.MORE
                isBottomDockVisible = true
            }
            else -> {
                currentScreen = TracmioScreen.HOME
                isBottomDockVisible = true
            }
        }
    }

    TracmioNativeTheme(preferences = preferences) {
        val visuals = LocalTracmioVisuals.current
        val bgBitmap = remember(preferences.customBgImagePath) {
            val path = preferences.customBgImagePath
            if (path.isNotBlank() && File(path).exists()) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            } else null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            visuals.bgSpec.bgTop,
                            visuals.bgSpec.bgMiddle,
                            visuals.bgSpec.bgBottom
                        )
                    )
                )
        ) {
            if (bgBitmap != null && preferences.bgPhotoOpacity > 0.01f) {
                Image(
                    bitmap = bgBitmap,
                    contentDescription = "Custom Background Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(preferences.bgPhotoOpacity)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF030D14).copy(alpha = 0.55f))
                )
            }

            if (!preferences.isWelcomed) {
                TracmioWelcomeOverlay(
                    initialName = preferences.userName,
                    initialCurrency = preferences.defaultCurrency,
                    onCompleteWelcome = { name, currency ->
                        viewModel.completeWelcome(name, currency)
                    }
                )
            } else {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.statusBars,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TracmioTopHeader(
                            userName = preferences.userName,
                            onOpenThemes = { currentScreen = TracmioScreen.THEMES },
                            onOpenSettings = { currentScreen = TracmioScreen.SETTINGS }
                        )
                    },
                    bottomBar = {
                        AnimatedVisibility(
                            visible = isBottomDockVisible,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            TracmioFloatingBottomBar(
                                currentScreen = currentScreen,
                                onSelectScreen = { screen ->
                                    currentScreen = screen
                                    isBottomDockVisible = true
                                },
                                onOpenAddTransaction = {
                                    initialTxnType = "EXPENSE"
                                    showAddTxnSheet = true
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
                            TracmioScreen.HOME -> HomeScreen(
                                state = state,
                                currencySymbol = preferences.currencySymbol,
                                onOpenAddTxn = { type ->
                                    initialTxnType = type
                                    showAddTxnSheet = true
                                },
                                onNavigate = { currentScreen = it },
                                onDeleteTxn = { viewModel.deleteTransaction(it) }
                            )

                            TracmioScreen.ACCOUNTS -> AccountsScreen(
                                state = state,
                                currencySymbol = preferences.currencySymbol,
                                defaultCurrency = preferences.defaultCurrency,
                                onAddAccount = { name, type, bal, curr ->
                                    viewModel.addAccount(name, type, bal, curr)
                                },
                                onArchiveAccount = { viewModel.archiveAccount(it) }
                            )

                            TracmioScreen.TRANSACTIONS -> TransactionsScreen(
                                state = state,
                                currencySymbol = preferences.currencySymbol,
                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                onOpenAddModal = {
                                    initialTxnType = "EXPENSE"
                                    showAddTxnSheet = true
                                }
                            )

                            TracmioScreen.MORE -> MoreHubScreen(
                                onNavigate = { currentScreen = it }
                            )

                            TracmioScreen.REPORTS -> ReportsScreen(
                                state = state,
                                currencySymbol = preferences.currencySymbol,
                                onExportExcel = onExportExcelWorkbook,
                                onExportPdf = onExportPdfStatement,
                                onBack = { currentScreen = TracmioScreen.MORE }
                            )

                            TracmioScreen.THEMES -> ThemesAndAppearanceScreen(
                                preferences = preferences,
                                onSelectBgTheme = { viewModel.setBgTheme(it) },
                                onSelectGlassUi = { viewModel.setGlassUi(it) },
                                onApplyCustomNeon = { p, s -> viewModel.setCustomNeonColors(p, s) },
                                onChoosePhoto = onChooseBackgroundPhoto,
                                onRemovePhoto = { viewModel.setCustomBgImagePath("") },
                                onChangePhotoOpacity = { viewModel.setBgPhotoOpacity(it) },
                                onChangeUiScale = { viewModel.setUiScale(it) },
                                onPreviewWelcomeAndSplash = {
                                    showOpeningSplash = true
                                    viewModel.reopenWelcome()
                                },
                                onBack = { currentScreen = TracmioScreen.MORE }
                            )

                            TracmioScreen.BUDGETS -> BudgetsAndGoalsScreen(
                                state = state,
                                currencySymbol = preferences.currencySymbol,
                                initialTab = 0,
                                onAddBudget = { cat, lim, m -> viewModel.addBudget(cat, lim, m) },
                                onDeleteBudget = { viewModel.deleteBudget(it) },
                                onAddGoal = { name, target, saved -> viewModel.addGoal(name, target, saved) },
                                onContributeGoal = { gId, amt, accId -> viewModel.contributeToGoal(gId, amt, accId) },
                                onDeleteGoal = { viewModel.deleteGoal(it) },
                                onBack = { currentScreen = TracmioScreen.MORE }
                            )

                            TracmioScreen.GOALS -> BudgetsAndGoalsScreen(
                                state = state,
                                currencySymbol = preferences.currencySymbol,
                                initialTab = 1,
                                onAddBudget = { cat, lim, m -> viewModel.addBudget(cat, lim, m) },
                                onDeleteBudget = { viewModel.deleteBudget(it) },
                                onAddGoal = { name, target, saved -> viewModel.addGoal(name, target, saved) },
                                onContributeGoal = { gId, amt, accId -> viewModel.contributeToGoal(gId, amt, accId) },
                                onDeleteGoal = { viewModel.deleteGoal(it) },
                                onBack = { currentScreen = TracmioScreen.MORE }
                            )

                            TracmioScreen.LOANS -> LoansAndPersonsScreen(
                                state = state,
                                currencySymbol = preferences.currencySymbol,
                                initialTab = 0,
                                onAddLoan = { name, p, emi, rate ->
                                    viewModel.addLoan(name, p, 0.0, emi, rate)
                                },
                                onPayLoan = { lId, amt, accId -> viewModel.payLoanEmi(lId, amt, accId) },
                                onDeleteLoan = { viewModel.deleteLoan(it) },
                                onAddPerson = { name, phone -> viewModel.addPerson(name, phone) },
                                onDeletePerson = { viewModel.deletePerson(it) },
                                onBack = { currentScreen = TracmioScreen.MORE }
                            )

                            TracmioScreen.PERSONS -> LoansAndPersonsScreen(
                                state = state,
                                currencySymbol = preferences.currencySymbol,
                                initialTab = 1,
                                onAddLoan = { name, p, emi, rate ->
                                    viewModel.addLoan(name, p, 0.0, emi, rate)
                                },
                                onPayLoan = { lId, amt, accId -> viewModel.payLoanEmi(lId, amt, accId) },
                                onDeleteLoan = { viewModel.deleteLoan(it) },
                                onAddPerson = { name, phone -> viewModel.addPerson(name, phone) },
                                onDeletePerson = { viewModel.deletePerson(it) },
                                onBack = { currentScreen = TracmioScreen.MORE }
                            )

                            TracmioScreen.SETTINGS -> SettingsAndDataScreen(
                                preferences = preferences,
                                onOpenThemesStudio = { currentScreen = TracmioScreen.THEMES },
                                onUpdateReminders = { enabled, h, m, meme, cat ->
                                    viewModel.updateReminderSettings(enabled, h, m, meme, cat)
                                    onScheduleReminder(enabled, h, m, meme, cat)
                                },
                                onTriggerTestNotification = onTriggerTestNotification,
                                onExportJson = onExportBackupJson,
                                onImportJson = onImportBackupJson,
                                onExportExcel = onExportExcelWorkbook,
                                onExportPdf = onExportPdfStatement,
                                onResetAllData = { viewModel.resetAllData() },
                                onBack = { currentScreen = TracmioScreen.MORE }
                            )
                        }
                    }
                }
            }

            if (showAddTxnSheet) {
                AddTransactionModalSheet(
                    state = state,
                    preferences = preferences,
                    initialType = initialTxnType,
                    onDismiss = { showAddTxnSheet = false },
                    onCreateUpiApp = { viewModel.addCustomUpiApp(it) },
                    onSaveTransaction = { accId, type, amt, cat, note, method, upi, toAccId, personId ->
                        viewModel.addTransaction(
                            accountId = accId,
                            type = type,
                            amount = amt,
                            category = cat,
                            note = note,
                            paymentMethod = method,
                            upiApp = upi,
                            transferAccountId = toAccId,
                            personId = personId
                        )
                        showAddTxnSheet = false
                    }
                )
            }

            TracmioAppOpenSplash(
                visible = showOpeningSplash,
                onDismiss = { showOpeningSplash = false }
            )
        }
    }
}

@Composable
private fun TracmioTopHeader(
    userName: String,
    onOpenThemes: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TracmioAnimated3DLogo(size = 44.dp, showSonarRings = false)
            Column {
                TracmioBrandWordmark(fontSizeSp = 21, showSubtitle = true)
                if (userName.isNotBlank()) {
                    Text(
                        text = "Hello, $userName",
                        style = MaterialTheme.typography.labelSmall,
                        color = visuals.primaryAccent
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onOpenThemes,
                modifier = Modifier.testTag("top_themes_button")
            ) {
                Tracmio3DBadge(
                    icon = Icons.Default.Palette,
                    tint = visuals.primaryAccent,
                    size = 38.dp,
                    iconSize = 18.dp
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("top_settings_button")
            ) {
                Tracmio3DBadge(
                    icon = Icons.Default.Settings,
                    tint = visuals.secondaryAccent,
                    size = 38.dp,
                    iconSize = 18.dp
                )
            }
        }
    }
}

@Composable
private fun TracmioFloatingBottomBar(
    currentScreen: TracmioScreen,
    onSelectScreen: (TracmioScreen) -> Unit,
    onOpenAddTransaction: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    val navShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("tracmio_bottom_dock"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = navShape,
                    ambientColor = visuals.primaryAccent.copy(alpha = 0.25f),
                    spotColor = visuals.primaryAccent.copy(alpha = 0.35f)
                ),
            shape = navShape,
            color = Color(0xFF051520).copy(alpha = 0.94f),
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.38f),
                        visuals.primaryAccent.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.12f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomDockTab(
                    label = "Home",
                    icon = Icons.Default.Home,
                    selected = currentScreen == TracmioScreen.HOME,
                    onClick = { onSelectScreen(TracmioScreen.HOME) },
                    testTag = "nav_home"
                )
                BottomDockTab(
                    label = "Accounts",
                    icon = Icons.Default.AccountBalanceWallet,
                    selected = currentScreen == TracmioScreen.ACCOUNTS,
                    onClick = { onSelectScreen(TracmioScreen.ACCOUNTS) },
                    testTag = "nav_accounts"
                )

                Surface(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(12.dp, CircleShape, spotColor = visuals.primaryAccent)
                        .clickable(onClick = onOpenAddTransaction)
                        .testTag("nav_add_transaction"),
                    shape = CircleShape,
                    color = visuals.primaryAccent,
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.65f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            tint = Color(0xFF031B12),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                BottomDockTab(
                    label = "Transactions",
                    icon = Icons.Default.ReceiptLong,
                    selected = currentScreen == TracmioScreen.TRANSACTIONS,
                    onClick = { onSelectScreen(TracmioScreen.TRANSACTIONS) },
                    testTag = "nav_transactions"
                )
                BottomDockTab(
                    label = "More",
                    icon = Icons.Default.GridView,
                    selected = currentScreen in setOf(
                        TracmioScreen.MORE,
                        TracmioScreen.REPORTS,
                        TracmioScreen.THEMES,
                        TracmioScreen.BUDGETS,
                        TracmioScreen.GOALS,
                        TracmioScreen.LOANS,
                        TracmioScreen.PERSONS,
                        TracmioScreen.SETTINGS
                    ),
                    onClick = { onSelectScreen(TracmioScreen.MORE) },
                    testTag = "nav_more"
                )
            }
        }
    }
}

@Composable
private fun BottomDockTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val visuals = LocalTracmioVisuals.current
    val tint = if (selected) visuals.primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = tint,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HomeScreen(
    state: NativeTracmioUiState,
    currencySymbol: String,
    onOpenAddTxn: (String) -> Unit,
    onNavigate: (TracmioScreen) -> Unit,
    onDeleteTxn: (TransactionEntity) -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("tracmio_native_dashboard"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentBorderColor = visuals.primaryAccent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TOTAL NET BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%s%.2f", currencySymbol, state.totalBalance),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Tracmio3DBadge(
                        icon = Icons.Default.AccountBalanceWallet,
                        tint = visuals.primaryAccent,
                        size = 48.dp,
                        iconSize = 24.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenAddTxn("INCOME") },
                        shape = RoundedCornerShape(14.dp),
                        color = visuals.incomeColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, visuals.incomeColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = visuals.incomeColor
                            )
                            Column {
                                Text("Inflow", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = String.format(Locale.US, "%s%.2f", currencySymbol, state.totalIncome),
                                    fontWeight = FontWeight.Bold,
                                    color = visuals.incomeColor
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenAddTxn("EXPENSE") },
                        shape = RoundedCornerShape(14.dp),
                        color = visuals.expenseColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, visuals.expenseColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = visuals.expenseColor
                            )
                            Column {
                                Text("Outflow", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = String.format(Locale.US, "%s%.2f", currencySymbol, state.totalExpense),
                                    fontWeight = FontWeight.Bold,
                                    color = visuals.expenseColor
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    onClick = { onNavigate(TracmioScreen.PERSONS) }
                ) {
                    Text("Borrowed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        String.format(Locale.US, "%s%.0f", currencySymbol, state.totalBorrowed),
                        fontWeight = FontWeight.Bold,
                        color = visuals.warningColor
                    )
                }
                GlassCard(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    onClick = { onNavigate(TracmioScreen.PERSONS) }
                ) {
                    Text("Lent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        String.format(Locale.US, "%s%.0f", currencySymbol, state.totalLent),
                        fontWeight = FontWeight.Bold,
                        color = visuals.secondaryAccent
                    )
                }
                GlassCard(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    onClick = { onNavigate(TracmioScreen.TRANSACTIONS) }
                ) {
                    Text("Entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${state.transactions.size}",
                        fontWeight = FontWeight.Bold,
                        color = visuals.primaryAccent
                    )
                }
            }
        }

        if (state.accounts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Wallets & Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { onNavigate(TracmioScreen.ACCOUNTS) }) {
                            Text("Manage →", color = visuals.primaryAccent)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.accounts.size) { idx ->
                            val acc = state.accounts[idx]
                            val bal = state.accountBalance(acc)
                            GlassCard(
                                modifier = Modifier.width(175.dp),
                                contentPadding = PaddingValues(14.dp),
                                onClick = { onNavigate(TracmioScreen.ACCOUNTS) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Tracmio3DBadge(
                                        icon = vectorIconForCategoryOrType(acc.accountType),
                                        tint = visuals.primaryAccent,
                                        size = 34.dp,
                                        iconSize = 18.dp
                                    )
                                    Column {
                                        Text(acc.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(
                                            acc.accountType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = String.format(Locale.US, "%s%.2f", currencySymbol, bal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = visuals.primaryAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            InteractiveSixMonthTrendCard(
                transactions = state.transactions,
                currencySymbol = currencySymbol
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onNavigate(TracmioScreen.TRANSACTIONS) }) {
                    Text("See All →", color = visuals.primaryAccent)
                }
            }
        }

        if (state.transactions.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No transactions logged yet. Tap the glowing '+' button below to record your first Income, Expense, Transfer, or UPI payment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(state.transactions.take(15), key = { _, tx -> tx.id }) { index, tx ->
                TransactionRowItem(
                    tx = tx,
                    index = index,
                    currencySymbol = currencySymbol,
                    onDelete = { onDeleteTxn(tx) }
                )
            }
        }
    }
}

@Composable
private fun TransactionRowItem(
    tx: TransactionEntity,
    index: Int,
    currencySymbol: String,
    onDelete: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    var visible by remember(tx.id) { mutableStateOf(false) }
    LaunchedEffect(tx.id) {
        delay((index.coerceAtMost(8) * 24).toLong())
        visible = true
    }

    val isPositive = tx.type.uppercase() in setOf("INCOME", "BORROW", "TRANSFER_IN")
    val amtColor = if (isPositive) visuals.incomeColor else visuals.expenseColor
    val sign = if (isPositive) "+" else "-"

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(240)) + slideInVertically(tween(240), initialOffsetY = { it / 5 })
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("txn_row_${tx.id}"),
            contentPadding = PaddingValues(14.dp)
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
                    Tracmio3DBadge(
                        icon = vectorIconForCategoryOrType(tx.category),
                        tint = amtColor,
                        size = 42.dp,
                        iconSize = 20.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(tx.category, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeLabel = if (tx.upiApp.isNotBlank()) {
                                "UPI · ${tx.upiApp}"
                            } else {
                                tx.paymentMethod
                            }
                            if (badgeLabel.isNotBlank()) {
                                Surface(
                                    color = visuals.primaryAccent.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = visuals.primaryAccent
                                    )
                                }
                            }
                            if (tx.note.isNotBlank()) {
                                Text(
                                    text = tx.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%s%s%.2f", sign, currencySymbol, tx.amount),
                            fontWeight = FontWeight.ExtraBold,
                            color = amtColor
                        )
                        Text(
                            text = TransactionDateTime.formatDateInput(tx.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountsScreen(
    state: NativeTracmioUiState,
    currencySymbol: String,
    defaultCurrency: String,
    onAddAccount: (String, String, Double, String) -> Unit,
    onArchiveAccount: (AccountEntity) -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("page_accounts"),
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Tracmio3DBadge(icon = Icons.Default.AccountBalance, tint = visuals.primaryAccent)
                    Column {
                        Text("Accounts & Wallets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Bank, Cash, UPI, Credit Card & Investment",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = visuals.primaryAccent,
                        contentColor = Color(0xFF031A12)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(state.accounts.size) { idx ->
            val acc = state.accounts[idx]
            val currentBal = state.accountBalance(acc)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Tracmio3DBadge(
                            icon = vectorIconForCategoryOrType(acc.accountType),
                            tint = visuals.primaryAccent
                        )
                        Column {
                            Text(acc.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${acc.accountType} • ${acc.currency}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%s%.2f", currencySymbol, currentBal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = visuals.primaryAccent
                        )
                        IconButton(onClick = { onArchiveAccount(acc) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Archive")
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Bank") }
        var balanceText by remember { mutableStateOf("0") }
        val types = listOf("Bank", "Cash", "UPI", "Credit Card", "Investment")

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Account / Wallet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name") },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        types.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Opening Balance ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = balanceText.toDoubleOrNull() ?: 0.0
                        onAddAccount(name.ifBlank { type }, type, bal, defaultCurrency)
                        showCreateDialog = false
                    }
                ) {
                    Text("Create Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TransactionsScreen(
    state: NativeTracmioUiState,
    currencySymbol: String,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onOpenAddModal: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var fromDateText by remember { mutableStateOf("") }
    var toDateText by remember { mutableStateOf("") }

    val filtered = remember(state.transactions, searchQuery, selectedTypeFilter, fromDateText, toDateText) {
        state.transactions.filter { tx ->
            val matchesType = selectedTypeFilter == "ALL" || tx.type.equals(selectedTypeFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true) ||
                tx.upiApp.contains(searchQuery, ignoreCase = true)
            val txDate = TransactionDateTime.formatDateInput(tx.timestamp)
            val matchesFrom = fromDateText.isBlank() || txDate >= fromDateText
            val matchesTo = toDateText.isBlank() || txDate <= toDateText
            matchesType && matchesQuery && matchesFrom && matchesTo
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("page_transactions"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Tracmio3DBadge(icon = Icons.Default.ReceiptLong, tint = visuals.primaryAccent)
                    Column {
                        Text("Transactions Ledger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("${filtered.size} matching entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = onOpenAddModal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = visuals.primaryAccent,
                        contentColor = Color(0xFF031A12)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search category, note, or UPI app...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fromDateText,
                        onValueChange = { fromDateText = it },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        placeholder = { Text("Select start date") },
                        label = { Text("From (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = toDateText,
                        onValueChange = { toDateText = it },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        placeholder = { Text("Select end date") },
                        label = { Text("To (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "EXPENSE", "INCOME", "TRANSFER_OUT", "BORROW", "LEND").forEach { t ->
                        FilterChip(
                            selected = selectedTypeFilter == t,
                            onClick = { selectedTypeFilter = t },
                            label = { Text(t.replace("_OUT", "")) }
                        )
                    }
                }
            }
        }

        itemsIndexed(filtered, key = { _, tx -> tx.id }) { idx, tx ->
            TransactionRowItem(
                tx = tx,
                index = idx,
                currencySymbol = currencySymbol,
                onDelete = { onDeleteTransaction(tx) }
            )
        }
    }
}

@Composable
private fun MoreHubScreen(
    onNavigate: (TracmioScreen) -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    val items = listOf(
        Triple(TracmioScreen.THEMES, "Themes & Appearance", "10 Background Themes + 10 Liquid Glass UI Styles"),
        Triple(TracmioScreen.REPORTS, "Reports & Analytics", "4 Interactive Charts, Category Burn & Exports"),
        Triple(TracmioScreen.BUDGETS, "Monthly Budgets", "80% & 100% category threshold tracking"),
        Triple(TracmioScreen.GOALS, "Savings Goals", "Piggy bank targets & deposit contributions"),
        Triple(TracmioScreen.LOANS, "Loans & EMI Tracker", "Principal, interest & EMI repayment schedule"),
        Triple(TracmioScreen.PERSONS, "People Ledger (Borrow/Lend)", "Track money borrowed from & lent to contacts"),
        Triple(TracmioScreen.SETTINGS, "Settings & Data Studio", "100 Smart Alerts, JSON/Excel/PDF Export & Privacy")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("page_more"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "More Studio & Modules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        items(items.size) { index ->
            val (screen, title, subtitle) = items[index]
            val icon = when (screen) {
                TracmioScreen.THEMES -> Icons.Default.Palette
                TracmioScreen.REPORTS -> Icons.Default.Analytics
                TracmioScreen.BUDGETS -> Icons.Default.PieChart
                TracmioScreen.GOALS -> Icons.Default.Flag
                TracmioScreen.LOANS -> Icons.Default.Handshake
                TracmioScreen.PERSONS -> Icons.Default.People
                else -> Icons.Default.Settings
            }
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("more_tile_${screen.name.lowercase()}"),
                onClick = { onNavigate(screen) },
                accentBorderColor = if (screen == TracmioScreen.THEMES) visuals.primaryAccent else null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Tracmio3DBadge(icon = icon, tint = visuals.primaryAccent)
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text("→", color = visuals.primaryAccent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen(
    state: NativeTracmioUiState,
    currencySymbol: String,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onBack: () -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("page_reports"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Tracmio3DBadge(icon = Icons.Default.Analytics, tint = visuals.primaryAccent)
                Column {
                    Text("Reports & Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("4-Style Charts, Palettes & Statement Exports", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            ReportsInteractiveChartsCard(
                transactions = state.transactions,
                currencySymbol = currencySymbol
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onExportExcel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = visuals.primaryAccent,
                        contentColor = Color(0xFF031A12)
                    )
                ) {
                    Text("Export Excel (.xlsx)", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Print / Save PDF")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionModalSheet(
    state: NativeTracmioUiState,
    preferences: TracmioUserPreferences,
    initialType: String,
    onDismiss: () -> Unit,
    onCreateUpiApp: (String) -> Unit,
    onSaveTransaction: (
        accountId: Long,
        type: String,
        amount: Double,
        category: String,
        note: String,
        paymentMethod: String,
        upiApp: String,
        transferAccountId: Long?,
        personId: Long?
    ) -> Unit
) {
    val visuals = LocalTracmioVisuals.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember(initialType) { mutableStateOf(initialType.uppercase()) }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var noteText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("UPI") }
    var selectedUpiApp by remember(preferences.upiApps) {
        mutableStateOf(preferences.upiApps.firstOrNull().orEmpty())
    }
    var selectedAccountId by remember(state.accounts) {
        mutableLongStateOf(state.accounts.firstOrNull()?.id ?: 0L)
    }
    var selectedToAccountId by remember(state.accounts) {
        mutableLongStateOf(state.accounts.getOrNull(1)?.id ?: state.accounts.firstOrNull()?.id ?: 0L)
    }
    var showCreateUpiDialog by remember { mutableStateOf(false) }

    val typeCards = listOf(
        "EXPENSE" to "Expense",
        "INCOME" to "Income",
        "TRANSFER" to "Transfer",
        "BORROW" to "Borrow",
        "LEND" to "Lend"
    )

    val categories = remember(selectedType, state.categories) {
        val dbCats = state.categories.map { it.name }
        if (dbCats.isNotEmpty()) dbCats else listOf(
            "Food & Dining",
            "Transport",
            "Shopping",
            "Bills & Utilities",
            "Health",
            "Entertainment",
            "Salary",
            "Business"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF061622),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("add_transaction_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Log Financial Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeCards.forEach { (typeKey, label) ->
                    val active = selectedType == typeKey
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedType = typeKey
                                if (typeKey == "INCOME") selectedCategory = "Salary"
                                if (typeKey == "TRANSFER") selectedCategory = "Transfer"
                            }
                            .testTag("type_card_${typeKey.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) visuals.primaryAccent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) visuals.primaryAccent else Color.White.copy(alpha = 0.14f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = vectorIconForCategoryOrType(typeKey),
                                contentDescription = null,
                                tint = if (active) visuals.primaryAccent else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (active) visuals.primaryAccent else Color.White
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (${preferences.currencySymbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("txn_amount_input")
            )

            if (state.accounts.isNotEmpty()) {
                Text("Select Account", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }

            if (selectedType == "TRANSFER" && state.accounts.size > 1) {
                Text("Transfer To Account", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                        FilterChip(
                            selected = selectedToAccountId == acc.id,
                            onClick = { selectedToAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }

            if (selectedType != "TRANSFER") {
                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorIconForCategoryOrType(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Text("Payment Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("UPI", "Cash", "Card", "Bank Transfer").forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method) }
                    )
                }
            }

            if (paymentMethod == "UPI") {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = visuals.primaryAccent)
                            Text("Select UPI App", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { showCreateUpiDialog = true },
                            modifier = Modifier.testTag("add_upi_app_button")
                        ) {
                            Text("+ Add UPI App", color = visuals.primaryAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        preferences.upiApps.forEach { app ->
                            FilterChip(
                                selected = selectedUpiApp == app,
                                onClick = { selectedUpiApp = app },
                                label = { Text(app) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note / Description (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val accId = if (selectedAccountId != 0L) selectedAccountId else (state.accounts.firstOrNull()?.id ?: 0L)
                        if (amt > 0 && accId != 0L) {
                            onSaveTransaction(
                                accId,
                                selectedType,
                                amt,
                                selectedCategory,
                                noteText,
                                paymentMethod,
                                if (paymentMethod == "UPI") selectedUpiApp else "",
                                if (selectedType == "TRANSFER") selectedToAccountId else null,
                                null
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_transaction_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = visuals.primaryAccent,
                        contentColor = Color(0xFF031B12)
                    )
                ) {
                    Text("Save Entry", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    if (showCreateUpiDialog) {
        var customUpiName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateUpiDialog = false },
            title = { Text("Create Custom UPI App") },
            text = {
                OutlinedTextField(
                    value = customUpiName,
                    onValueChange = { customUpiName = it },
                    label = { Text("UPI App Name (e.g., Super.money, Jupiter)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customUpiName.isNotBlank()) {
                            onCreateUpiApp(customUpiName.trim())
                            selectedUpiApp = customUpiName.trim()
                            showCreateUpiDialog = false
                        }
                    }
                ) {
                    Text("Add UPI App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateUpiDialog = false }) { Text("Cancel") }
            }
        )
    }
}
