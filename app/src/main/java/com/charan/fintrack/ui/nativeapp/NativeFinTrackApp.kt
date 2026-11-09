package com.charan.fintrack.ui.nativeapp

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.charan.fintrack.FinTrackReminderScheduler
import com.charan.fintrack.data.MoneyPaise
import com.charan.fintrack.data.NativeLedgerCalculator
import com.charan.fintrack.data.local.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private enum class FinPage(val label: String, val route: String) {
    HOME("Home", "home"),
    TRANSACTIONS("Transactions", "transactions"),
    ACCOUNTS("Accounts", "accounts"),
    REPORTS("Reports", "reports"),
    MORE("More", "more"),
    PEOPLE("People", "people"),
    BUDGETS("Budgets", "budgets"),
    GOALS("Savings Goals", "goals"),
    LOANS("Loans", "loans"),
    CATEGORIES("Categories", "categories"),
    SETTINGS("Settings", "settings");

    companion object {
        fun fromRoute(route: String?): FinPage =
            values().firstOrNull { it.route == route } ?: HOME
    }
}
private enum class EditorKind { NONE, TRANSACTION, ACCOUNT, PERSON, BUDGET, GOAL, LOAN, CONTRIBUTION, REPAYMENT, CATEGORY }

@Composable
fun NativeFinTrackApp(viewModel: NativeFinTrackViewModel, initialQuickAction: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appPrefs = remember { context.getSharedPreferences("fintrack_native_settings", 0) }
    var dark by remember { mutableStateOf(appPrefs.getBoolean("dark_theme", true)) }
    var backgroundUri by remember { mutableStateOf(appPrefs.getString("background_uri", null)) }
    var backgroundOpacity by remember { mutableFloatStateOf(appPrefs.getInt("background_opacity", 35).coerceIn(0, 100).toFloat()) }
    var iconScalePercent by remember { mutableFloatStateOf(appPrefs.getInt("icon_size_percent", 100).coerceIn(80, 140).toFloat()) }
    val backgroundScope = rememberCoroutineScope()
    SideEffect {
        (context as? android.app.Activity)?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    var pendingApkUri by remember { mutableStateOf<Uri?>(null) }
    var pendingXlsxFromDate by remember { mutableStateOf("") }
    var pendingXlsxToDate by remember { mutableStateOf("") }

    val backupSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let(viewModel::exportBackup)
    }
    val backupOpen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(viewModel::importBackup)
    }
    val csvSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        it?.let(viewModel::exportCsv)
    }
    val xlsxSave = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { it?.let(viewModel::exportXlsx) }
    val xlsxRangeSave = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) viewModel.exportXlsxRange(uri, pendingXlsxFromDate, pendingXlsxToDate)
    }
    val pdfSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {
        it?.let(viewModel::exportPdf)
    }
    // Copy the image while Android's picker grant is active so the photo survives restarts.
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            backgroundScope.launch {
                try {
                    val savedFile = withContext(Dispatchers.IO) { persistBackgroundImage(context, uri) }
                    val savedUri = Uri.fromFile(savedFile).toString()
                    backgroundUri = savedUri
                    appPrefs.edit().putString("background_uri", savedUri).apply()
                    Toast.makeText(context, "Background photo saved on this device.", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(context, "Could not save photo: ${error.message ?: "unsupported image"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    // Move old gallery-provider URIs to private storage when their existing grant still works.
    LaunchedEffect(backgroundUri) {
        val current = backgroundUri ?: return@LaunchedEffect
        val source = runCatching { Uri.parse(current) }.getOrNull() ?: return@LaunchedEffect
        if (source.scheme == "content") {
            runCatching { withContext(Dispatchers.IO) { persistBackgroundImage(context, source) } }
                .onSuccess { savedFile ->
                    // Do not let an old migration job overwrite a newer photo selection or Remove action.
                    if (backgroundUri == current) {
                        val savedUri = Uri.fromFile(savedFile).toString()
                        backgroundUri = savedUri
                        appPrefs.edit().putString("background_uri", savedUri).apply()
                    } else {
                        savedFile.delete()
                    }
                }
                .onFailure {
                    if (backgroundUri == current) {
                        Toast.makeText(context, "The previous background photo is no longer accessible. Choose it again.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
    val apkPermissionSettings = rememberLauncherForActivityResult(StartActivityForResult()) {
        val uri = pendingApkUri
        pendingApkUri = null
        if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.packageManager.canRequestPackageInstalls()) {
            openSelectedApk(context, uri)
        } else if (uri != null) {
            Toast.makeText(context, "Allow install permission, then select the APK again.", Toast.LENGTH_LONG).show()
        }
    }
    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                pendingApkUri = uri
                apkPermissionSettings.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            } else {
                openSelectedApk(context, uri)
            }
        }
    }

    val decodedBackground by produceState<Bitmap?>(initialValue = null, backgroundUri) {
        value = backgroundUri?.let { uriString ->
            withContext(Dispatchers.IO) {
                decodeBackgroundBitmap(context, Uri.parse(uriString))
            }
        }
    }
    DisposableEffect(decodedBackground) {
        onDispose { decodedBackground?.takeIf { !it.isRecycled }?.recycle() }
    }

    FinTrackNativeTheme(darkTheme = dark) {
        NativeFinTrackContent(
            viewModel = viewModel,
            darkTheme = dark,
            onThemeChange = {
                dark = it
                context.getSharedPreferences("fintrack_native_settings", 0).edit().putBoolean("dark_theme", it).apply()
            },
            onExportBackup = { backupSave.launch("FinTrack_Backup.json") },
            onImportBackup = { backupOpen.launch(arrayOf("application/json", "text/json", "application/octet-stream")) },
            onExportCsv = { csvSave.launch("FinTrack_Transactions.csv") },
            onExportXlsx = { xlsxSave.launch("FinTrack_Export.xlsx") },
            onExportXlsxRange = { fromDate, toDate ->
                pendingXlsxFromDate = fromDate
                pendingXlsxToDate = toDate
                xlsxRangeSave.launch("FinTrack_${fromDate}_to_${toDate}.xlsx")
            },
            onExportPdf = { pdfSave.launch("FinTrack_Statement.pdf") },
            backgroundBitmap = decodedBackground,
            backgroundOpacity = backgroundOpacity / 100f,
            onChooseBackground = { backgroundPicker.launch(arrayOf("image/*")) },
            onRemoveBackground = {
                backgroundUri = null
                appPrefs.edit().remove("background_uri").apply()
                File(context.filesDir, "appearance").listFiles()?.forEach { it.delete() }
                File(context.filesDir, "fintrack_legacy_background.img").delete()
            },
            onBackgroundOpacity = { backgroundOpacity = it.coerceIn(0f, 100f) },
            onBackgroundOpacityFinished = {
                appPrefs.edit().putInt("background_opacity", it.roundToInt().coerceIn(0, 100)).apply()
            },
            iconScalePercent = iconScalePercent,
            onIconScaleChange = { iconScalePercent = it.coerceIn(80f, 140f) },
            onIconScaleChangeFinished = {
                appPrefs.edit().putInt("icon_size_percent", it.roundToInt().coerceIn(80, 140)).apply()
            },
            onInstallApk = { apkPicker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) },
            initialQuickAction = initialQuickAction
        )
    }
}

@Composable
private fun NativeFinTrackContent(
    viewModel: NativeFinTrackViewModel,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportXlsxRange: (String, String) -> Unit,
    onExportPdf: () -> Unit,
    backgroundBitmap: Bitmap?,
    backgroundOpacity: Float,
    onChooseBackground: () -> Unit,
    onRemoveBackground: () -> Unit,
    onBackgroundOpacity: (Float) -> Unit,
    onBackgroundOpacityFinished: (Float) -> Unit,
    iconScalePercent: Float,
    onIconScaleChange: (Float) -> Unit,
    onIconScaleChangeFinished: (Float) -> Unit,
    onInstallApk: () -> Unit,
    initialQuickAction: String?
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val people by viewModel.people.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val totals by viewModel.dashboard.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val page = FinPage.fromRoute(currentBackStackEntry?.destination?.route)
    val navigateTo: (FinPage) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(FinPage.HOME.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    var pendingQuickAction by remember { mutableStateOf(initialQuickAction) }
    var editor by remember {
        mutableStateOf(
            when (initialQuickAction) {
                "expense", "income" -> EditorKind.TRANSACTION
                else -> EditorKind.NONE
            }
        )
    }
    var selectedGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var selectedLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var selectedBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF062A3A), MaterialTheme.colorScheme.background))
        )
    ) {
        backgroundBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Custom FinTrack background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = backgroundOpacity
            )
        }
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            ),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (!imeVisible) {
                    NativeBottomBar(page, navigateTo, { editor = EditorKind.TRANSACTION }, iconScalePercent / 100f)
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                NativeHeader(
                    title = if (page == FinPage.HOME) "FinTrack" else page.label,
                    subtitle = if (page == FinPage.HOME) greetingText() else "FinTrack Pro • Offline Edition",
                    iconScale = iconScalePercent / 100f,
                    onAdd = {
                        editor = when (page) {
                            FinPage.ACCOUNTS -> EditorKind.ACCOUNT
                            FinPage.PEOPLE -> EditorKind.PERSON
                            FinPage.BUDGETS -> EditorKind.BUDGET
                            FinPage.GOALS -> EditorKind.GOAL
                            FinPage.LOANS -> EditorKind.LOAN
                            FinPage.CATEGORIES -> EditorKind.CATEGORY
                            else -> EditorKind.TRANSACTION
                        }
                    }
                )
                NavHost(
                    navController = navController,
                    startDestination = FinPage.HOME.route,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    composable(FinPage.HOME.route) {
                        DashboardScreen(accounts, transactions, totals, { editor = EditorKind.ACCOUNT }, { navigateTo(FinPage.TRANSACTIONS) })
                    }
                    composable(FinPage.TRANSACTIONS.route) {
                        TransactionsScreen(
                            transactions, accounts, viewModel::deleteTransaction,
                            onEdit = { selectedTransaction = it; editor = EditorKind.TRANSACTION }
                        )
                    }
                    composable(FinPage.ACCOUNTS.route) {
                        AccountsScreen(
                            accounts, transactions, viewModel::archiveAccount,
                            onEdit = { selectedAccount = it; editor = EditorKind.ACCOUNT }
                        )
                    }
                    composable(FinPage.REPORTS.route) {
                        ReportsScreen(accounts, transactions, totals, people)
                    }
                    composable(FinPage.MORE.route) {
                        MoreScreen(onNavigate = navigateTo)
                    }
                    composable(FinPage.PEOPLE.route) {
                        PeopleScreen(
                            people, transactions, viewModel::deletePerson,
                            onEdit = { selectedPerson = it; editor = EditorKind.PERSON }
                        )
                    }
                    composable(FinPage.BUDGETS.route) {
                        BudgetsScreen(
                            budgets, transactions, viewModel::deleteBudget,
                            onEdit = { selectedBudget = it; editor = EditorKind.BUDGET }
                        )
                    }
                    composable(FinPage.GOALS.route) {
                        GoalsScreen(
                            goals,
                            onContribute = { selectedGoal = it; editor = EditorKind.CONTRIBUTION },
                            onDelete = viewModel::deleteGoal,
                            onEdit = { selectedGoal = it; editor = EditorKind.GOAL },
                            viewModel = viewModel
                        )
                    }
                    composable(FinPage.LOANS.route) {
                        LoansScreen(
                            loans,
                            onRepay = { selectedLoan = it; editor = EditorKind.REPAYMENT },
                            onDelete = viewModel::deleteLoan,
                            onEdit = { selectedLoan = it; editor = EditorKind.LOAN },
                            viewModel = viewModel
                        )
                    }
                    composable(FinPage.CATEGORIES.route) {
                        CategoriesScreen(
                            categories, viewModel::deleteCategory,
                            onEdit = { selectedCategory = it; editor = EditorKind.CATEGORY }
                        )
                    }
                    composable(FinPage.SETTINGS.route) {
                        SettingsScreen(
                            darkTheme = darkTheme,
                            onThemeChange = onThemeChange,
                            onExportBackup = onExportBackup,
                            onImportBackup = onImportBackup,
                            onExportCsv = onExportCsv,
                            onExportXlsx = onExportXlsx,
                            onExportXlsxRange = onExportXlsxRange,
                            onExportPdf = onExportPdf,
                            onChooseBackground = onChooseBackground,
                            onRemoveBackground = onRemoveBackground,
                            backgroundOpacity = backgroundOpacity,
                            onBackgroundOpacity = onBackgroundOpacity,
                            onBackgroundOpacityFinished = onBackgroundOpacityFinished,
                            iconScalePercent = iconScalePercent,
                            onIconScaleChange = onIconScaleChange,
                            onIconScaleChangeFinished = onIconScaleChangeFinished,
                            onInstallApk = onInstallApk,
                            onResetData = viewModel::resetAllData
                        )
                    }
                }
            }
        }

        if (editor != EditorKind.NONE) {
            NativeEditorDialog(
                kind = editor, accounts = accounts, people = people,
                selectedGoal = selectedGoal, selectedLoan = selectedLoan,
                selectedTransaction = selectedTransaction, selectedAccount = selectedAccount,
                selectedPerson = selectedPerson, selectedBudget = selectedBudget,
                selectedCategory = selectedCategory,
                initialQuickAction = pendingQuickAction,
                onDismiss = {
                    editor = EditorKind.NONE
                    selectedGoal = null; selectedLoan = null; selectedTransaction = null
                    selectedAccount = null; selectedPerson = null; selectedBudget = null; selectedCategory = null
                    pendingQuickAction = null
                },
                onSave = { values ->
                    when (editor) {
                        EditorKind.ACCOUNT -> {
                            val current = selectedAccount
                            if (current == null) viewModel.addAccount(
                                values["name"].orEmpty(), values["type"].orEmpty(),
                                values["amount"].orEmpty(), values["creditLimit"].orEmpty(),
                                values["institution"].orEmpty(), values["accountNumber"].orEmpty(),
                                values["accountNotes"].orEmpty()
                            ) else viewModel.updateAccount(
                                current, values["name"].orEmpty(), values["type"].orEmpty(),
                                values["amount"].orEmpty(), values["creditLimit"].orEmpty(),
                                values["institution"].orEmpty(), values["accountNumber"].orEmpty(),
                                values["accountNotes"].orEmpty()
                            )
                        }
                        EditorKind.TRANSACTION -> {
                            val current = selectedTransaction
                            if (current == null) viewModel.addTransaction(
                                values["type"].orEmpty(), values["amount"].orEmpty(),
                                values["accountId"]?.toLongOrNull() ?: 0L,
                                values["category"].orEmpty(), values["note"].orEmpty(),
                                values["toAccountId"]?.toLongOrNull(),
                                values["personId"]?.toLongOrNull(),
                                values["paymentMethod"].orEmpty(), values["upiApp"].orEmpty(),
                                values["date"].orEmpty(), values["time"].orEmpty(),
                                values["subcategory"].orEmpty()
                            ) else viewModel.updateTransaction(
                                current, values["type"].orEmpty(), values["amount"].orEmpty(),
                                values["accountId"]?.toLongOrNull() ?: current.accountId,
                                values["category"].orEmpty(), values["note"].orEmpty(),
                                values["toAccountId"]?.toLongOrNull(),
                                values["personId"]?.toLongOrNull(),
                                values["paymentMethod"].orEmpty(), values["upiApp"].orEmpty(),
                                values["date"].orEmpty(), values["time"].orEmpty(),
                                values["subcategory"].orEmpty()
                            )
                        }
                        EditorKind.PERSON -> {
                            val current = selectedPerson
                            if (current == null) viewModel.addPerson(values["name"].orEmpty(), values["phone"].orEmpty(), values["note"].orEmpty())
                            else viewModel.updatePerson(current, values["name"].orEmpty(), values["phone"].orEmpty(), values["note"].orEmpty())
                        }
                        EditorKind.CATEGORY -> {
                            val current = selectedCategory
                            if (current == null) viewModel.addCategory(
                                values["name"].orEmpty(), values["type"].orEmpty(),
                                values["icon"].orEmpty(), values["subcategories"].orEmpty()
                            ) else viewModel.updateCategory(
                                current, values["name"].orEmpty(), values["type"].orEmpty(),
                                values["icon"].orEmpty(), values["subcategories"].orEmpty()
                            )
                        }
                        EditorKind.BUDGET -> {
                            val current = selectedBudget
                            if (current == null) viewModel.addBudget(values["category"].orEmpty(), values["amount"].orEmpty(), values["month"].orEmpty(), values["period"].orEmpty())
                            else viewModel.updateBudget(current, values["category"].orEmpty(), values["amount"].orEmpty(), values["month"].orEmpty(), values["period"].orEmpty())
                        }
                        EditorKind.GOAL -> {
                            val current = selectedGoal
                            if (current == null) viewModel.addGoal(values["name"].orEmpty(), values["amount"].orEmpty())
                            else viewModel.updateGoal(current, values["name"].orEmpty(), values["amount"].orEmpty())
                        }
                        EditorKind.LOAN -> {
                            val current = selectedLoan
                            if (current == null) viewModel.addLoan(values["name"].orEmpty(), values["amount"].orEmpty(), values["monthly"].orEmpty(), values["interest"].orEmpty())
                            else viewModel.updateLoan(current, values["name"].orEmpty(), values["amount"].orEmpty(), values["monthly"].orEmpty(), values["interest"].orEmpty())
                        }
                        EditorKind.CONTRIBUTION -> selectedGoal?.let { viewModel.contribute(it.id, values["amount"].orEmpty(), values["note"].orEmpty()) }
                        EditorKind.REPAYMENT -> selectedLoan?.let { viewModel.repay(it.id, values["amount"].orEmpty(), values["note"].orEmpty()) }
                        EditorKind.NONE -> Unit
                    }
                    editor = EditorKind.NONE
                    selectedGoal = null; selectedLoan = null; selectedTransaction = null
                    selectedAccount = null; selectedPerson = null; selectedBudget = null; selectedCategory = null
                    pendingQuickAction = null
                }
            )
        }
    }
}

@Composable
private fun NativeHeader(title: String, subtitle: String, iconScale: Float, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.BarChart, null, tint = Color(0xFF00E6D0), modifier = Modifier.size(30.dp * iconScale))
                Spacer(Modifier.width(9.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(24.dp)) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(24.dp * iconScale))
            Text("Add")
        }
    }
}

@Composable
private fun NativeBottomBar(current: FinPage, onNavigate: (FinPage) -> Unit, onAdd: () -> Unit, iconScale: Float) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xCC082C3D))
            .border(1.dp, Color(0x6655D8E6), RoundedCornerShape(28.dp))
            .padding(horizontal = 3.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavItem("Home", Icons.Filled.Home, current == FinPage.HOME, iconScale) { onNavigate(FinPage.HOME) }
        BottomNavItem("Transactions", Icons.Filled.ReceiptLong, current == FinPage.TRANSACTIONS, iconScale) { onNavigate(FinPage.TRANSACTIONS) }
        FloatingActionButton(
            onClick = onAdd, modifier = Modifier.size(50.dp), shape = CircleShape,
            containerColor = Color(0xFF00DCCB), contentColor = Color(0xFF03202A)
        ) { Icon(Icons.Filled.Add, "Add transaction", modifier = Modifier.size(28.dp * iconScale)) }
        BottomNavItem("Accounts", Icons.Filled.AccountBalanceWallet, current == FinPage.ACCOUNTS, iconScale) { onNavigate(FinPage.ACCOUNTS) }
        BottomNavItem("Reports", Icons.Filled.BarChart, current == FinPage.REPORTS, iconScale) { onNavigate(FinPage.REPORTS) }
        BottomNavItem("More", Icons.Filled.MoreHoriz, current == FinPage.MORE, iconScale) { onNavigate(FinPage.MORE) }
    }
}

@Composable
private fun BottomNavItem(label: String, icon: ImageVector, selected: Boolean, iconScale: Float, onClick: () -> Unit) {
    Column(
        Modifier.widthIn(min = 42.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (selected) Color(0xFF16E7D7) else Color(0xFFD8E7F0), modifier = Modifier.size(19.dp * iconScale))
        Text(label, color = if (selected) Color(0xFF16E7D7) else Color(0xFFD8E7F0), style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun DashboardScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    totals: com.charan.fintrack.data.repository.NativeDashboardTotals,
    onAddAccount: () -> Unit,
    onTransactions: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp)) {
        GlassCard {
            Text("TOTAL BALANCE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(money(totals.balancePaise), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricMini("Income", money(totals.incomePaise), Color(0xFF12D6A0), Icons.Filled.ArrowDownward, Modifier.weight(1f))
                MetricMini("Expense", money(totals.expensePaise), Color(0xFFFF687B), Icons.Filled.ArrowUpward, Modifier.weight(1f))
            }
        }
        SectionLabel("QUICK STATS")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallMetric("Borrowed", money(totals.borrowedPaise), Icons.Filled.ArrowDownward, Color(0xFFA980FF), Modifier.weight(1f))
            SmallMetric("Lent", money(totals.lentPaise), Icons.Filled.ArrowUpward, Color(0xFFFFB76B), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallMetric("Net Worth", money(totals.netWorthPaise), Icons.Filled.Diamond, Color(0xFF15DDD1), Modifier.weight(1f))
            SmallMetric("Transactions", totals.transactionCount.toString(), Icons.Filled.BarChart, Color(0xFF8D83FF), Modifier.weight(1f))
        }
        SectionLabel("ACCOUNTS & WALLETS")
        if (accounts.isEmpty()) {
            GlassCard {
                Text("No accounts yet", fontWeight = FontWeight.Bold)
                Text("Add your first cash, bank or wallet account to start tracking balances.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onAddAccount) { Icon(Icons.Filled.Add, null); Text("Add Account") }
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accounts.forEach { account ->
                    val balance = NativeLedgerCalculator.accountBalancePaise(account, transactions)
                    GlassCard(Modifier.width(190.dp)) {
                        Text(account.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(account.accountType.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(money(balance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                GlassCard(Modifier.width(150.dp).clickable(onClick = onAddAccount)) {
                    Icon(Icons.Filled.Add, null, tint = Color(0xFF00DCCB), modifier = Modifier.size(32.dp))
                    Text("Add Account", fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("RECENT TRANSACTIONS")
            TextButton(onClick = onTransactions) { Text("View all") }
        }
        if (transactions.isEmpty()) GlassCard { Text("Your latest transactions will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else transactions.filterNot { it.type.equals("TRANSFER_IN", true) }.take(5).forEach { TransactionItem(it, null) }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onDelete: (TransactionEntity) -> Unit,
    onEdit: (TransactionEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    var accountFilter by remember { mutableStateOf("ALL") }
    var accountExpanded by remember { mutableStateOf(false) }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Newest first") }
    var sortExpanded by remember { mutableStateOf(false) }

    val visibleRows = transactions.filterNot { it.type.equals("TRANSFER_IN", true) }
    val fromBound = parseDateBound(fromDate, endOfDay = false)
    val toBound = parseDateBound(toDate, endOfDay = true)
    val filtered = visibleRows.filter { row ->
        val typeMatches = filter == "ALL" || row.type.equals(filter, true) ||
            (filter == "TRANSFER" && row.type.startsWith("TRANSFER", true))
        val accountMatches = accountFilter == "ALL" || row.accountId.toString() == accountFilter
        val textMatches = search.isBlank() || row.note.contains(search, true) ||
            row.category.contains(search, true) || row.type.contains(search, true) ||
            row.amountPaise.toString().contains(search)
        val fromMatches = fromBound == null || row.timestamp >= fromBound
        val toMatches = toBound == null || row.timestamp <= toBound
        typeMatches && accountMatches && textMatches && fromMatches && toMatches
    }.let { rows ->
        when (sort) {
            "Oldest first" -> rows.sortedBy { it.timestamp }
            "Highest amount" -> rows.sortedByDescending { it.amountPaise }
            else -> rows.sortedByDescending { it.timestamp }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Search transactions, notes or amount") }
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("ALL", "INCOME", "EXPENSE", "TRANSFER", "BORROW", "LEND").forEach { item ->
                FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = if (accountFilter == "ALL") "All accounts" else accounts.firstOrNull { it.id.toString() == accountFilter }?.name ?: "All accounts",
                    onValueChange = {}, readOnly = true, label = { Text("Account") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true
                )
                ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                    DropdownMenuItem(text = { Text("All accounts") }, onClick = { accountFilter = "ALL"; accountExpanded = false })
                    accounts.forEach { account ->
                        DropdownMenuItem(text = { Text(account.name) }, onClick = { accountFilter = account.id.toString(); accountExpanded = false })
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = it }) {
                OutlinedTextField(
                    value = sort, onValueChange = {}, readOnly = true, label = { Text("Sort") },
                    modifier = Modifier.menuAnchor().width(142.dp), singleLine = true
                )
                ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    listOf("Newest first", "Oldest first", "Highest amount").forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { sort = option; sortExpanded = false })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(fromDate, { fromDate = it }, modifier = Modifier.weight(1f), label = { Text("From date") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true)
            OutlinedTextField(toDate, { toDate = it }, modifier = Modifier.weight(1f), label = { Text("To date") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true)
        }
        if ((fromDate.isNotBlank() && fromBound == null) || (toDate.isNotBlank() && toBound == null)) {
            Text("Dates must use YYYY-MM-DD.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
        Text(filtered.size.toString() + " transactions", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        if (filtered.isEmpty()) EmptyState("No matching transactions", "Change the search or filters, or add a new transaction.")
        else LazyColumn(contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.stableUid }) { row -> TransactionItem(row, onDelete, onEdit) }
        }
    }
}
@Composable
private fun AccountsScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    onArchive: (Long) -> Unit,
    onEdit: (AccountEntity) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        SectionLabel("ACCOUNTS & WALLETS")
        if (accounts.isEmpty()) {
            EmptyState("No accounts and wallets", "Create a cash, bank or digital wallet account using Add.")
        } else LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        items(accounts, key = { it.stableUid }) { account ->
            val current = NativeLedgerCalculator.accountBalancePaise(account, transactions)
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color(0xFF30D8CC), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.name, fontWeight = FontWeight.Bold)
                        Text(account.accountType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(money(current), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                Text("Opening balance: " + money(account.openingBalancePaise), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEdit(account) }) { Text("Edit") }
                    TextButton(onClick = { onArchive(account.id) }) { Text("Archive") }
                }
            }
        }
    }
    }
}
@Composable
private fun ReportsScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    totals: com.charan.fintrack.data.repository.NativeDashboardTotals,
    people: List<PersonEntity>
) {
    var period by remember { mutableStateOf("This month") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var customRange by remember { mutableStateOf(false) }

    val now = Calendar.getInstance()
    val from = Calendar.getInstance().apply {
        when (period) {
            "Today" -> {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            "This week" -> {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
            }
            "This year" -> {
                set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            "All time" -> timeInMillis = 0L
            else -> {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
        }
    }
    val to = Calendar.getInstance().apply {
        when (period) {
            "Today" -> {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }
            "This week" -> {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                val daysSinceMonday = (get(Calendar.DAY_OF_WEEK) + 5) % 7
                add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
                add(Calendar.DAY_OF_YEAR, 7)
                add(Calendar.MILLISECOND, -1)
            }
            "This year" -> {
                set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.YEAR, 1); add(Calendar.MILLISECOND, -1)
            }
            "All time" -> timeInMillis = Long.MAX_VALUE
            else -> {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1)
            }
        }
    }

    val selectedFrom = if (customRange) parseDateBound(fromDate, false) ?: Long.MIN_VALUE else from.timeInMillis
    val selectedTo = if (customRange) parseDateBound(toDate, true) ?: Long.MAX_VALUE else to.timeInMillis
    val rangeValid = !customRange || (parseDateBound(fromDate, false) != null && parseDateBound(toDate, true) != null && selectedFrom <= selectedTo)
    val periodRows = if (rangeValid) transactions.filter {
        it.timestamp >= selectedFrom && it.timestamp <= selectedTo &&
            !it.type.equals("TRANSFER_IN", true)
    } else emptyList()
    val income = periodRows.filter { it.type.equals("INCOME", true) }.sumOf { it.amountPaise }
    val expense = periodRows.filter { it.type.equals("EXPENSE", true) }.sumOf { it.amountPaise }
    val borrow = periodRows.filter { it.type.equals("BORROW", true) }.sumOf { it.amountPaise }
    val lend = periodRows.filter { it.type.equals("LEND", true) }.sumOf { it.amountPaise }
    val maxValue = maxOf(income, expense, 1L).toFloat()
    val peopleMap = people.associateBy { it.id }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        SectionLabel("REPORT PERIOD")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Today", "This week", "This month", "This year", "All time").forEach { option ->
                FilterChip(selected = !customRange && period == option, onClick = { period = option; customRange = false }, label = { Text(option) })
            }
            FilterChip(selected = customRange, onClick = { customRange = true }, label = { Text("Custom") })
        }
        if (customRange) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(fromDate, { fromDate = it }, modifier = Modifier.weight(1f), label = { Text("From") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true)
                OutlinedTextField(toDate, { toDate = it }, modifier = Modifier.weight(1f), label = { Text("To") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true)
            }
            if (!rangeValid) Text("Enter a valid date range using YYYY-MM-DD.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
        SectionLabel("INCOME VS EXPENSE")
        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) { Text("Income", color = Color(0xFF19D5A3)); Text(money(income), fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f)) { Text("Expense", color = Color(0xFFFF7380)); Text(money(expense), fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(125.dp)) {
                val base = size.height - 8f
                val barWidth = size.width / 5f
                val incomeHeight = (income.toFloat() / maxValue) * (size.height - 18f)
                val expenseHeight = (expense.toFloat() / maxValue) * (size.height - 18f)
                drawLine(Color(0xFF18D4A6), Offset(barWidth, base), Offset(barWidth, base - incomeHeight), barWidth * .45f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFF687B), Offset(barWidth * 3, base), Offset(barWidth * 3, base - expenseHeight), barWidth * .45f, cap = StrokeCap.Round)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Borrowed " + money(borrow), color = Color(0xFFB59BFF), style = MaterialTheme.typography.bodySmall)
                Text("Lent " + money(lend), color = Color(0xFFFFBC72), style = MaterialTheme.typography.bodySmall)
            }
            Text("Net cash flow " + money(income - expense), fontWeight = FontWeight.Bold)
        }
        SectionLabel("ACCOUNT BALANCES")
        accounts.forEach { account ->
            val current = NativeLedgerCalculator.accountBalancePaise(account, transactions)
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(account.name, fontWeight = FontWeight.SemiBold)
                    Text(money(current), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        SectionLabel("SPENDING BY CATEGORY")
        val categoryRows = periodRows.filter { it.type.equals("EXPENSE", true) }.groupBy { it.category }
        if (categoryRows.isEmpty()) GlassCard { Text("No expense records for the selected period.") }
        categoryRows.entries.sortedByDescending { e -> e.value.fold(0L) { sum, row -> Math.addExact(sum, row.amountPaise) } }.forEach { (name, rows) ->
            val sum = rows.fold(0L) { acc, row -> Math.addExact(acc, row.amountPaise) }
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name); Text(money(sum), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        SectionLabel("PERSON-WISE SUMMARY")
        val peopleTotals = periodRows.filter { it.personId != null }.groupBy { it.personId!! }
        if (peopleTotals.isEmpty()) GlassCard { Text("No person-linked borrowing or lending in the selected period.") }
        peopleTotals.forEach { (personId, rows) ->
            val person = peopleMap[personId]
            val borrowedByPerson = rows.filter { it.type.equals("BORROW", true) }.sumOf { it.amountPaise }
            val lentByPerson = rows.filter { it.type.equals("LEND", true) }.sumOf { it.amountPaise }
            GlassCard {
                Text(person?.name ?: "Person", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Borrowed " + money(borrowedByPerson), color = Color(0xFFB59BFF), style = MaterialTheme.typography.bodySmall)
                    Text("Lent " + money(lentByPerson), color = Color(0xFFFFBC72), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("Transfers are excluded from income and expense summaries.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}
@Composable
private fun MoreScreen(onNavigate: (FinPage) -> Unit) {
    val entries = listOf(
        Triple(FinPage.PEOPLE, "People", Icons.Filled.People),
        Triple(FinPage.BUDGETS, "Budgets", Icons.Filled.Category),
        Triple(FinPage.GOALS, "Savings Goals", Icons.Filled.Savings),
        Triple(FinPage.LOANS, "Loans & Repayments", Icons.Filled.AccountBalance),
        Triple(FinPage.CATEGORIES, "Categories", Icons.Filled.Category),
        Triple(FinPage.SETTINGS, "Settings & Appearance", Icons.Filled.Settings)
    )
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { GlassCard { Text("Manage your finances", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("All records stay on this device in the Offline Edition.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        items(entries) { item ->
            val destination = item.first
            GlassCard(Modifier.fillMaxWidth().clickable { onNavigate(destination) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(item.third, null, tint = Color(0xFF21DACA), modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(item.second, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

@Composable
private fun PeopleScreen(
    people: List<PersonEntity>,
    transactions: List<TransactionEntity>,
    onDelete: (PersonEntity) -> Unit,
    onEdit: (PersonEntity) -> Unit
) {
    if (people.isEmpty()) {
        EmptyState("No people added", "Add people to track money borrowed from or lent to each person.")
    } else LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(people, key = { it.stableUid }) { person ->
            val rows = transactions.filter { it.personId == person.id }
            val borrowed = rows.filter { it.type.equals("BORROW", true) }.sumOf { it.amountPaise }
            val lent = rows.filter { it.type.equals("LEND", true) }.sumOf { it.amountPaise }
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(person.name, fontWeight = FontWeight.Bold)
                        if (person.phone.isNotBlank()) Text(person.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onEdit(person) }) { Text("Edit") }
                    TextButton(onClick = { onDelete(person) }) { Text("Delete") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Borrowed " + money(borrowed), color = Color(0xFFB59BFF), style = MaterialTheme.typography.bodySmall)
                    Text("Lent " + money(lent), color = Color(0xFFFFBC72), style = MaterialTheme.typography.bodySmall)
                }
                if (rows.isNotEmpty()) {
                    Text("Linked transactions: ${rows.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
@Composable
private fun BudgetsScreen(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    onDelete: (BudgetEntity) -> Unit,
    onEdit: (BudgetEntity) -> Unit
) {
    if (budgets.isEmpty()) {
        EmptyState("No budgets created", "Set category spending limits and monitor progress.")
    } else LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(budgets, key = { it.stableUid }) { budget ->
            val range = budgetWindow(budget)
            val spent = NativeLedgerCalculator.categoryExpensePaise(
                transactions, budget.category, range.first, range.second
            )
            val remaining = budget.limitAmountPaise - spent
            val ratio = if (budget.limitAmountPaise <= 0L) 0f
                else (spent.toDouble() / budget.limitAmountPaise).toFloat().coerceIn(0f, 1.2f)
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(budget.category, fontWeight = FontWeight.Bold)
                        Text("${budget.period} • ${budget.month}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    Row {
                        TextButton(onClick = { onEdit(budget) }) { Text("Edit") }
                        TextButton(onClick = { onDelete(budget) }) { Text("Delete") }
                    }
                }
                Text(money(spent) + " of " + money(budget.limitAmountPaise))
                LinearProgressIndicator(
                    progress = { ratio.coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth(),
                    color = if (ratio >= 1f) Color(0xFFFF6779) else Color(0xFF20D7BE)
                )
                Text(
                    if (remaining < 0L) "Over budget by " + money(-remaining) else "Remaining " + money(remaining),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
@Composable
private fun GoalsScreen(
    goals: List<GoalEntity>,
    onContribute: (GoalEntity) -> Unit,
    onDelete: (GoalEntity) -> Unit,
    onEdit: (GoalEntity) -> Unit,
    viewModel: NativeFinTrackViewModel
) {
    if (goals.isEmpty()) {
        EmptyState("No savings goals", "Create a target and record contributions as separate tracking entries.")
    } else LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(goals, key = { it.stableUid }) { goal ->
            val ratio = if (goal.targetAmountPaise <= 0L) 0f
                else (goal.savedAmountPaise.toDouble() / goal.targetAmountPaise).toFloat().coerceIn(0f, 1f)
            val contributions by viewModel.goalContributions(goal.id).collectAsStateWithLifecycle(initialValue = emptyList())
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(goal.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Row {
                        TextButton(onClick = { onEdit(goal) }) { Text("Edit") }
                        TextButton(onClick = { onDelete(goal) }) { Text("Delete") }
                    }
                }
                Text(money(goal.savedAmountPaise) + " saved of " + money(goal.targetAmountPaise))
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF19D9C8))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Remaining " + money((goal.targetAmountPaise - goal.savedAmountPaise).coerceAtLeast(0L)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onContribute(goal) }) { Text("Add contribution") }
                }
                Text("Contribution tracking does not change account balances.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (contributions.isNotEmpty()) {
                    Text("Contribution history", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    contributions.take(3).forEach { entry ->
                        Text(
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.timestamp)) +
                                " • " + money(entry.amountPaise),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun LoansScreen(
    loans: List<LoanEntity>,
    onRepay: (LoanEntity) -> Unit,
    onDelete: (LoanEntity) -> Unit,
    onEdit: (LoanEntity) -> Unit,
    viewModel: NativeFinTrackViewModel
) {
    if (loans.isEmpty()) {
        EmptyState("No loan records", "Add a loan and record partial or full repayments.")
    } else LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(loans, key = { it.stableUid }) { loan ->
            val remaining = (loan.principalPaise - loan.paidAmountPaise).coerceAtLeast(0L)
            val ratio = if (loan.principalPaise <= 0L) 0f
                else (loan.paidAmountPaise.toDouble() / loan.principalPaise).toFloat().coerceIn(0f, 1f)
            val payments by viewModel.loanPayments(loan.id).collectAsStateWithLifecycle(initialValue = emptyList())
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(loan.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Row {
                        TextButton(onClick = { onEdit(loan) }) { Text("Edit") }
                        TextButton(onClick = { onDelete(loan) }) { Text("Delete") }
                    }
                }
                Text("Outstanding " + money(remaining), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Principal " + money(loan.principalPaise) + " • Paid " + money(loan.paidAmountPaise), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF25D8C5))
                TextButton(onClick = { onRepay(loan) }) { Text("Record repayment") }
                Text("Repayment tracking does not change account balances.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (payments.isNotEmpty()) {
                    Text("Repayment history", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    payments.take(3).forEach { entry ->
                        Text(
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.timestamp)) +
                                " • " + money(entry.amountPaise),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onDelete: (CategoryEntity) -> Unit,
    onEdit: (CategoryEntity) -> Unit
) {
    if (categories.isEmpty()) {
        EmptyState("No categories available", "Create income and expense categories for transaction classification.")
    } else LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.stableUid }) { category ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.icon.ifBlank { "•" }, color = Color(0xFF23D7CE), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.SemiBold)
                        Text(category.type, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(
                            "Subcategories: ${runCatching { org.json.JSONArray(category.subcategoriesJson).length() }.getOrDefault(0)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (!category.isSystem) {
                        TextButton(onClick = { onEdit(category) }) { Text("Edit") }
                        TextButton(onClick = { onDelete(category) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}
@Composable
private fun SettingsScreen(
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportXlsxRange: (String, String) -> Unit,
    onExportPdf: () -> Unit,
    onChooseBackground: () -> Unit,
    onRemoveBackground: () -> Unit,
    backgroundOpacity: Float,
    onBackgroundOpacity: (Float) -> Unit,
    onBackgroundOpacityFinished: (Float) -> Unit,
    iconScalePercent: Float,
    onIconScaleChange: (Float) -> Unit,
    onIconScaleChangeFinished: (Float) -> Unit,
    onInstallApk: () -> Unit,
    onResetData: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val reminderPrefs = remember { context.getSharedPreferences("fintrack_reminders", 0) }
    var remindersEnabled by remember { mutableStateOf(reminderPrefs.getBoolean("enabled", false)) }
    var reminderHour by remember { mutableIntStateOf(reminderPrefs.getInt("hour", 20)) }
    var reminderMinute by remember { mutableIntStateOf(reminderPrefs.getInt("minute", 0)) }
    var memeMode by remember { mutableStateOf(reminderPrefs.getBoolean("memeMode", false)) }
    var pendingReminderEnable by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var xlsxFromDate by remember {
        mutableStateOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
                Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.time
            )
        )
    }
    var xlsxToDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    val xlsxStartBound = parseDateBound(xlsxFromDate, endOfDay = false)
    val xlsxEndBound = parseDateBound(xlsxToDate, endOfDay = true)
    val xlsxRangeValid = xlsxStartBound != null && xlsxEndBound != null && xlsxStartBound <= xlsxEndBound

    fun saveReminder(enabled: Boolean, hour: Int = reminderHour, minute: Int = reminderMinute, funMode: Boolean = memeMode) {
        reminderPrefs.edit().putBoolean("enabled", enabled).putInt("hour", hour)
            .putInt("minute", minute).putBoolean("memeMode", funMode).apply()
        remindersEnabled = enabled
        reminderHour = hour
        reminderMinute = minute
        memeMode = funMode
        FinTrackReminderScheduler.schedule(context, enabled, hour, minute)
    }

    val notificationPermission = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        val enable = pendingReminderEnable
        pendingReminderEnable = false
        if (granted && enable) saveReminder(true)
        else if (enable) Toast.makeText(context, "Notification permission is required for reminders.", Toast.LENGTH_LONG).show()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassCard {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (darkTheme) "Dark Liquid Glass theme" else "Light theme")
                Switch(checked = darkTheme, onCheckedChange = onThemeChange)
            }
            Text("Custom background image", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChooseBackground, modifier = Modifier.weight(1f)) { Text("Choose photo") }
                TextButton(onClick = onRemoveBackground) { Text("Remove") }
            }
            Text("Background visibility", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = (backgroundOpacity * 100f).coerceIn(0f, 100f),
                onValueChange = onBackgroundOpacity,
                onValueChangeFinished = { onBackgroundOpacityFinished(backgroundOpacity * 100f) },
                valueRange = 0f..100f
            )
            Text("${(backgroundOpacity * 100f).roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text("Navigation and action icon size", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = iconScalePercent,
                onValueChange = onIconScaleChange,
                onValueChangeFinished = { onIconScaleChangeFinished(iconScalePercent) },
                valueRange = 80f..140f
            )
            Text("${iconScalePercent.roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        GlassCard {
            Text("Local backup and restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "JSON backup includes accounts, transactions, people, categories, budgets, savings goals, contributions, loans and repayment history. Restore merges by stable record ID and does not replace existing data.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) { Text("Export full JSON backup") }
            OutlinedButton(onClick = onImportBackup, modifier = Modifier.fillMaxWidth()) { Text("Restore / merge JSON backup") }
        }
        GlassCard {
            Text("Export reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) { Text("Export transactions CSV") }
            OutlinedButton(onClick = onExportXlsx, modifier = Modifier.fillMaxWidth()) { Text("Export Excel workbook") }
            OutlinedButton(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) { Text("Export PDF statement") }
        }
        GlassCard {
            Text("Date-range Excel export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Export transactions between two local calendar dates. Other workbook sheets retain their complete records.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                xlsxFromDate, { xlsxFromDate = it },
                label = { Text("Start date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                xlsxToDate, { xlsxToDate = it },
                label = { Text("End date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            if (!xlsxRangeValid) {
                Text(
                    "Enter valid dates in YYYY-MM-DD format. The start date must not be after the end date.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            OutlinedButton(
                onClick = { onExportXlsxRange(xlsxFromDate.trim(), xlsxToDate.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = xlsxRangeValid
            ) { Text("Export date-range Excel") }
        }
        GlassCard {
            Text("Daily reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (remindersEnabled) "Reminder enabled" else "Reminder disabled")
                    Text(String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            pendingReminderEnable = true
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else saveReminder(enabled)
                    }
                )
            }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> saveReminder(remindersEnabled, hour, minute) },
                        reminderHour, reminderMinute, true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Change reminder time") }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Fun reminder messages")
                Switch(checked = memeMode, onCheckedChange = { saveReminder(remindersEnabled, funMode = it) })
            }
            Text("Reminders are scheduled locally. Android battery settings may delay delivery.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        GlassCard {
            Text("Manual app update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Choose a compatible FinTrack APK. Android will show its own installation confirmation.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onInstallApk, modifier = Modifier.fillMaxWidth()) { Text("Select update APK") }
        }
        GlassCard {
            Text("Offline privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("This edition has no sign-in, OTP, cloud sync or cloud backup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Files are created only after you choose a local save destination.", style = MaterialTheme.typography.bodySmall)
        }
        GlassCard {
            Text("App security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("No app lock is enabled in this release, as requested.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        GlassCard {
            Text("Reset finance data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Permanently remove accounts, transactions, people, categories, budgets, goals, loans and their histories from this device. Export a backup first.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Reset all finance data") }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all finance data?") },
            text = { Text("This permanently deletes all finance records stored in FinTrack on this device. Export a backup before continuing. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onResetData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete all data") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeEditorDialog(
    kind: EditorKind,
    accounts: List<AccountEntity>,
    people: List<PersonEntity>,
    selectedGoal: GoalEntity?,
    selectedLoan: LoanEntity?,
    selectedTransaction: TransactionEntity?,
    selectedAccount: AccountEntity?,
    selectedPerson: PersonEntity?,
    selectedBudget: BudgetEntity?,
    selectedCategory: CategoryEntity?,
    initialQuickAction: String?,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    fun rupees(paise: Long): String = MoneyPaise.fromSignedPaise(paise).toRupeesDecimal().toPlainString()
    val selectedTimestamp = selectedTransaction?.timestamp ?: System.currentTimeMillis()
    var name by remember(kind, selectedGoal?.id, selectedLoan?.id, selectedAccount?.id, selectedPerson?.id, selectedBudget?.id, selectedCategory?.id) {
        mutableStateOf(
            selectedAccount?.name ?: selectedPerson?.name ?: selectedBudget?.category ?:
            selectedGoal?.name ?: selectedLoan?.name ?: selectedCategory?.name.orEmpty()
        )
    }
    var amount by remember(kind, selectedTransaction?.id, selectedAccount?.id, selectedBudget?.id, selectedGoal?.id, selectedLoan?.id) {
        mutableStateOf(
            when (kind) {
                EditorKind.TRANSACTION -> selectedTransaction?.let { rupees(it.amountPaise) }.orEmpty()
                EditorKind.ACCOUNT -> selectedAccount?.let { rupees(it.openingBalancePaise) }.orEmpty()
                EditorKind.BUDGET -> selectedBudget?.let { rupees(it.limitAmountPaise) }.orEmpty()
                EditorKind.GOAL -> selectedGoal?.let { rupees(it.targetAmountPaise) }.orEmpty()
                EditorKind.LOAN -> selectedLoan?.let { rupees(it.principalPaise) }.orEmpty()
                else -> ""
            }
        )
    }
    var type by remember(kind, selectedTransaction?.id, selectedAccount?.id, selectedCategory?.id, initialQuickAction) {
        mutableStateOf(
            when (kind) {
                EditorKind.TRANSACTION -> when (initialQuickAction) {
                    "expense" -> "EXPENSE"
                    "income" -> "INCOME"
                    else -> when (selectedTransaction?.type?.uppercase()) {
                        "TRANSFER_OUT", "TRANSFER_IN" -> "TRANSFER"
                        else -> selectedTransaction?.type?.uppercase() ?: "INCOME"
                    }
                }
                EditorKind.ACCOUNT -> selectedAccount?.accountType ?: "Wallet"
                EditorKind.CATEGORY -> selectedCategory?.type ?: "EXPENSE"
                else -> "INCOME"
            }
        )
    }
    var category by remember(kind, selectedTransaction?.id, selectedBudget?.id) {
        mutableStateOf(selectedTransaction?.category ?: selectedBudget?.category ?: "Other")
    }
    var subcategory by remember(kind, selectedTransaction?.id) {
        mutableStateOf(selectedTransaction?.subcategory.orEmpty())
    }
    var note by remember(kind, selectedTransaction?.id, selectedPerson?.id) {
        mutableStateOf(selectedTransaction?.note ?: selectedPerson?.notes.orEmpty())
    }
    var phone by remember(kind, selectedPerson?.id) { mutableStateOf(selectedPerson?.phone.orEmpty()) }
    var accountId by remember(kind, selectedTransaction?.id, accounts.firstOrNull()?.id) {
        mutableStateOf(selectedTransaction?.accountId?.toString() ?: accounts.firstOrNull()?.id?.toString().orEmpty())
    }
    var toAccountId by remember(kind, selectedTransaction?.id, accounts.getOrNull(1)?.id) {
        mutableStateOf(selectedTransaction?.transferAccountId?.toString() ?: accounts.getOrNull(1)?.id?.toString().orEmpty())
    }
    var personId by remember(kind, selectedTransaction?.id, people.firstOrNull()?.id) {
        mutableStateOf(selectedTransaction?.personId?.toString() ?: people.firstOrNull()?.id?.toString().orEmpty())
    }
    var period by remember(kind, selectedBudget?.id) {
        mutableStateOf(selectedBudget?.period ?: "Monthly")
    }
    var month by remember(kind, selectedBudget?.id) {
        mutableStateOf(selectedBudget?.month ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Date()))
    }
    var monthly by remember(kind, selectedLoan?.id) {
        mutableStateOf(selectedLoan?.let { rupees(it.monthlyPaymentPaise) }.orEmpty())
    }
    var interest by remember(kind, selectedLoan?.id) {
        mutableStateOf(selectedLoan?.interestRate?.toString() ?: "0")
    }
    var creditLimit by remember(kind, selectedAccount?.id) {
        mutableStateOf(selectedAccount?.let { rupees(it.creditLimitPaise) }.orEmpty())
    }
    var accountInstitution by remember(kind, selectedAccount?.id) {
        mutableStateOf(selectedAccount?.institution.orEmpty())
    }
    var accountNumber by remember(kind, selectedAccount?.id) {
        mutableStateOf(selectedAccount?.accountNumber.orEmpty())
    }
    var accountNotes by remember(kind, selectedAccount?.id) {
        mutableStateOf(selectedAccount?.notes.orEmpty())
    }
    var subcategoriesText by remember(kind, selectedCategory?.id) {
        mutableStateOf(runCatching {
            val json = org.json.JSONArray(selectedCategory?.subcategoriesJson ?: "[]")
            (0 until json.length()).joinToString("\n") { json.optString(it) }
        }.getOrDefault(""))
    }
    var icon by remember(kind, selectedCategory?.id) { mutableStateOf(selectedCategory?.icon ?: "🏷️") }
    var date by remember(kind, selectedTransaction?.id) {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(selectedTimestamp)))
    }
    var time by remember(kind, selectedTransaction?.id) {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.US).format(Date(selectedTimestamp)))
    }
    var paymentMethod by remember(kind, selectedTransaction?.id) {
        mutableStateOf(selectedTransaction?.paymentMethod ?: "Cash")
    }
    var upiApp by remember(kind, selectedTransaction?.id) {
        mutableStateOf(selectedTransaction?.upiApp.orEmpty())
    }
    var typeExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var personExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }

    val editing = selectedTransaction != null || selectedAccount != null || selectedPerson != null ||
        selectedBudget != null || selectedGoal != null && kind == EditorKind.GOAL ||
        selectedLoan != null && kind == EditorKind.LOAN || selectedCategory != null
    val title = when (kind) {
        EditorKind.TRANSACTION -> if (editing) "Edit Transaction" else "Add Transaction"
        EditorKind.ACCOUNT -> if (editing) "Edit Account or Wallet" else "Add Account or Wallet"
        EditorKind.PERSON -> if (editing) "Edit Person" else "Add Person"
        EditorKind.BUDGET -> if (editing) "Edit Budget" else "Create Budget"
        EditorKind.GOAL -> if (editing) "Edit Savings Goal" else "Create Savings Goal"
        EditorKind.LOAN -> if (editing) "Edit Loan" else "Create Loan"
        EditorKind.CONTRIBUTION -> "Add Goal Contribution"
        EditorKind.REPAYMENT -> "Record Loan Repayment"
        EditorKind.CATEGORY -> if (editing) "Edit Category" else "Add Category"
        EditorKind.NONE -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                when (kind) {
                    EditorKind.TRANSACTION -> {
                        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                            OutlinedTextField(
                                value = type, onValueChange = {}, readOnly = true,
                                label = { Text("Transaction type") },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                listOf("INCOME", "EXPENSE", "BORROW", "LEND", "TRANSFER").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeExpanded = false })
                                }
                            }
                        }
                        MoneyField("Amount (₹)", amount) { amount = it }
                        ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = it }) {
                            OutlinedTextField(
                                value = accounts.firstOrNull { it.id.toString() == accountId }?.name ?: "Select account",
                                onValueChange = {}, readOnly = true,
                                label = { Text(if (type == "TRANSFER") "From account" else "Account") },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = { Text(account.name) },
                                        onClick = { accountId = account.id.toString(); accountExpanded = false }
                                    )
                                }
                            }
                        }
                        if (type == "TRANSFER") {
                            ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = it }) {
                                OutlinedTextField(
                                    value = accounts.firstOrNull { it.id.toString() == toAccountId }?.name ?: "Select destination",
                                    onValueChange = {}, readOnly = true, label = { Text("To account") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                                    accounts.filter { it.id.toString() != accountId }.forEach { account ->
                                        DropdownMenuItem(
                                            text = { Text(account.name) },
                                            onClick = { toAccountId = account.id.toString(); toExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                        if (type != "TRANSFER") {
                            OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(
                                subcategory, { subcategory = it },
                                label = { Text("Subcategory (optional)") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                        }
                        if (type == "BORROW" || type == "LEND") {
                            ExposedDropdownMenuBox(expanded = personExpanded, onExpandedChange = { personExpanded = it }) {
                                OutlinedTextField(
                                    value = people.firstOrNull { it.id.toString() == personId }?.name ?: "Select person",
                                    onValueChange = {}, readOnly = true, label = { Text("Person") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = personExpanded, onDismissRequest = { personExpanded = false }) {
                                    people.forEach { person ->
                                        DropdownMenuItem(
                                            text = { Text(person.name) },
                                            onClick = { personId = person.id.toString(); personExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(time, { time = it }, label = { Text("Time (HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        ExposedDropdownMenuBox(expanded = paymentExpanded, onExpandedChange = { paymentExpanded = it }) {
                            OutlinedTextField(
                                paymentMethod, {}, readOnly = true, label = { Text("Payment method") },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = paymentExpanded, onDismissRequest = { paymentExpanded = false }) {
                                listOf("Cash", "Card", "UPI", "Bank Transfer", "Other").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { paymentMethod = option; paymentExpanded = false })
                                }
                            }
                        }
                        if (paymentMethod == "UPI") {
                            OutlinedTextField(upiApp, { upiApp = it }, label = { Text("UPI app") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        OutlinedTextField(note, { note = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    }
                    EditorKind.ACCOUNT -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Account name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(type, { type = it }, label = { Text("Account type (Cash / Bank / Wallet / Credit)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        MoneyField("Opening balance (₹)", amount) { amount = it }
                        if (type.contains("credit", true)) MoneyField("Credit limit (₹)", creditLimit) { creditLimit = it }
                        OutlinedTextField(
                            accountInstitution, { accountInstitution = it },
                            label = { Text("Bank / Institution (optional)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            accountNumber, { accountNumber = it },
                            label = { Text("Account reference (optional)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            accountNotes, { accountNotes = it },
                            label = { Text("Account notes") }, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    EditorKind.PERSON -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Person name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(phone, { phone = it }, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(note, { note = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    }
                    EditorKind.CATEGORY -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Category name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                            OutlinedTextField(type, {}, readOnly = true, label = { Text("Type") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                listOf("INCOME", "EXPENSE").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeExpanded = false })
                                }
                            }
                        }
                        OutlinedTextField(icon, { icon = it }, label = { Text("Category icon") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(
                            subcategoriesText, { subcategoriesText = it },
                            label = { Text("Subcategories (one per line)") },
                            modifier = Modifier.fillMaxWidth(), minLines = 3
                        )
                    }
                    EditorKind.BUDGET -> {
                        OutlinedTextField(category, { category = it }, label = { Text("Budget category") }, modifier = Modifier.fillMaxWidth())
                        MoneyField("Budget limit (₹)", amount) { amount = it }
                        ExposedDropdownMenuBox(expanded = periodExpanded, onExpandedChange = { periodExpanded = it }) {
                            OutlinedTextField(period, {}, readOnly = true, label = { Text("Period") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = periodExpanded, onDismissRequest = { periodExpanded = false }) {
                                listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { period = option; periodExpanded = false })
                                }
                            }
                        }
                        OutlinedTextField(month, { month = it }, label = { Text("Budget month (YYYY-MM)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    EditorKind.GOAL -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Goal name") }, modifier = Modifier.fillMaxWidth())
                        MoneyField("Target amount (₹)", amount) { amount = it }
                    }
                    EditorKind.LOAN -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Loan name") }, modifier = Modifier.fillMaxWidth())
                        MoneyField("Principal (₹)", amount) { amount = it }
                        MoneyField("Monthly payment (₹)", monthly) { monthly = it }
                        OutlinedTextField(interest, { interest = it }, label = { Text("Annual interest rate (%)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                    EditorKind.CONTRIBUTION -> {
                        Text(selectedGoal?.name.orEmpty(), fontWeight = FontWeight.SemiBold)
                        MoneyField("Contribution amount (₹)", amount) { amount = it }
                        OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                        Text("Tracking only. Account balances are not changed.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    EditorKind.REPAYMENT -> {
                        Text(selectedLoan?.name.orEmpty(), fontWeight = FontWeight.SemiBold)
                        MoneyField("Repayment amount (₹)", amount) { amount = it }
                        OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                        Text("Tracking only. Record any corresponding account transaction separately.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    EditorKind.NONE -> Unit
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    mapOf(
                        "name" to name, "amount" to amount, "type" to type, "category" to category,
                        "note" to note, "phone" to phone, "accountId" to accountId,
                        "toAccountId" to toAccountId, "period" to period, "month" to month,
                        "monthly" to monthly, "interest" to interest, "creditLimit" to creditLimit,
                        "personId" to personId, "paymentMethod" to paymentMethod,
                        "upiApp" to upiApp, "date" to date, "time" to time,
                        "subcategory" to subcategory, "icon" to icon,
                        "institution" to accountInstitution, "accountNumber" to accountNumber,
                        "accountNotes" to accountNotes, "subcategories" to subcategoriesText
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MoneyField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value, onValue, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    onDelete: ((TransactionEntity) -> Unit)?,
    onEdit: ((TransactionEntity) -> Unit)? = null
) {
    val transfer = transaction.type.startsWith("TRANSFER", true)
    val positive = transaction.type in setOf("INCOME", "BORROW")
    val tint = if (transfer) Color(0xFF37D8EA) else if (positive) Color(0xFF18D6A4) else Color(0xFFFF7180)
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Icon(if (positive) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward, null, tint = tint)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.category.ifBlank { transaction.type }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(transaction.type + " • " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.subcategory.isNotBlank()) Text(transaction.subcategory, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (transaction.note.isNotBlank()) Text(transaction.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text((if (transfer) "↔ " else if (positive) "+" else "−") + money(transaction.amountPaise), color = tint, fontWeight = FontWeight.Bold)
            if (onEdit != null) TextButton(onClick = { onEdit(transaction) }) { Text("Edit") }
            if (onDelete != null) IconButton(onClick = { onDelete(transaction) }) {
                Icon(Icons.Filled.Delete, "Delete transaction", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String, tint: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color(0x6620CED1))
            .border(1.dp, tint.copy(alpha = .45f), RoundedCornerShape(18.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = tint)
        Text(value, fontWeight = FontWeight.ExtraBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SmallMetric(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color(0x99213F54))
            .border(1.dp, tint.copy(alpha = .45f), RoundedCornerShape(18.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xA8173C4E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x554ED8E8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, Modifier.padding(top = 18.dp, bottom = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun EmptyState(title: String, description: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        GlassCard {
            Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color(0xFF18DCCB), modifier = Modifier.size(34.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun persistBackgroundImage(context: android.content.Context, sourceUri: Uri): File {
    fun decode(options: BitmapFactory.Options): Bitmap? {
        return if (sourceUri.scheme == "file") {
            sourceUri.path?.let { BitmapFactory.decodeFile(it, options) }
        } else {
            context.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    decode(bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) {
        "The selected file is not a supported image."
    }
    var sample = 1
    while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    val decoded = decode(options) ?: throw IllegalArgumentException("Android could not decode this photo.")
    val scale = minOf(1.0, 1400.0 / maxOf(decoded.width, decoded.height))
    val outputBitmap = if (scale < 1.0) {
        Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else decoded
    val directory = File(context.filesDir, "appearance")
    if (!directory.exists() && !directory.mkdirs()) {
        if (outputBitmap !== decoded) outputBitmap.recycle()
        decoded.recycle()
        throw IllegalStateException("Could not create FinTrack's private photo folder.")
    }
    val output = File(directory, "background_${System.currentTimeMillis()}_${System.nanoTime()}.jpg")
    val temporary = File(directory, output.name + ".tmp")
    try {
        var quality = 88
        do {
            temporary.delete()
            FileOutputStream(temporary).use { stream ->
                if (!outputBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                    throw IllegalStateException("Photo compression failed.")
                }
                stream.flush()
                stream.fd.sync()
            }
            quality -= 8
        } while (temporary.length() > 1_800_000L && quality >= 56)
        require(temporary.length() > 0L) { "The selected photo is empty." }
        if (!temporary.renameTo(output)) {
            throw IllegalStateException("Could not save the photo into FinTrack storage.")
        }
        directory.listFiles()
            ?.filter { it.isFile && it.name.startsWith("background_") && it != output }
            ?.forEach { it.delete() }
        return output
    } finally {
        temporary.delete()
        if (outputBitmap !== decoded && !outputBitmap.isRecycled) outputBitmap.recycle()
        if (!decoded.isRecycled) decoded.recycle()
    }
}

private fun decodeBackgroundBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        fun decode(options: BitmapFactory.Options): Bitmap? {
            return if (uri.scheme == "file") {
                uri.path?.let { BitmapFactory.decodeFile(it, options) }
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        decode(options)
    } catch (_: Exception) {
        null
    }
}

private fun openSelectedApk(context: android.content.Context, uri: Uri) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    } catch (error: Exception) {
        Toast.makeText(context, "Could not open Android package installer: ${error.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
    }
}

private fun parseDateBound(value: String, endOfDay: Boolean): Long? {
    if (!value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val parsed = runCatching { format.parse(value) }.getOrNull() ?: return null
    if (format.format(parsed) != value) return null
    val calendar = Calendar.getInstance().apply {
        time = parsed
        set(Calendar.HOUR_OF_DAY, if (endOfDay) 23 else 0)
        set(Calendar.MINUTE, if (endOfDay) 59 else 0)
        set(Calendar.SECOND, if (endOfDay) 59 else 0)
        set(Calendar.MILLISECOND, if (endOfDay) 999 else 0)
    }
    return calendar.timeInMillis
}

private fun budgetWindow(budget: BudgetEntity): Pair<Long, Long> {
    val now = Calendar.getInstance()
    val start = Calendar.getInstance()
    val end = Calendar.getInstance()
    when (budget.period.lowercase(Locale.ROOT)) {
        "daily" -> {
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            end.timeInMillis = start.timeInMillis
            end.add(Calendar.DAY_OF_YEAR, 1)
        }
        "weekly" -> {
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            val day = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7
            start.add(Calendar.DAY_OF_YEAR, -day)
            end.timeInMillis = start.timeInMillis
            end.add(Calendar.DAY_OF_YEAR, 7)
        }
        "yearly" -> {
            start.set(Calendar.DAY_OF_YEAR, 1)
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            end.timeInMillis = start.timeInMillis
            end.add(Calendar.YEAR, 1)
        }
        else -> {
            val parts = budget.month.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.YEAR)
            val month = (parts.getOrNull(1)?.toIntOrNull()?.minus(1))?.coerceIn(0, 11) ?: now.get(Calendar.MONTH)
            start.set(year, month, 1, 0, 0, 0)
            start.set(Calendar.MILLISECOND, 0)
            end.timeInMillis = start.timeInMillis
            end.add(Calendar.MONTH, 1)
        }
    }
    return start.timeInMillis to end.timeInMillis
}

private fun money(paise: Long): String = MoneyPaise.fromSignedPaise(paise).formatInr()
private fun greetingText(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning 👋"
        hour < 17 -> "Good Afternoon 👋"
        else -> "Good Evening 👋"
    }
}
