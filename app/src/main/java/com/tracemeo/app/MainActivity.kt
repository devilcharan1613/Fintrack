package com.tracmio.app

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tracmio.app.data.LegacyMigrationGuard
import com.tracmio.app.data.PreferencesAndThemeStore
import com.tracmio.app.data.XlsxExporter
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.TracmioDatabase
import com.tracmio.app.data.local.TransactionEntity
import com.tracmio.app.data.repository.FinanceRepository
import com.tracmio.app.data.repository.NativeStatementPdf
import com.tracmio.app.data.security.BiometricCapabilityChecker
import com.tracmio.app.data.security.BiometricReadiness
import com.tracmio.app.data.security.DeviceSecurityCapability
import com.tracmio.app.data.security.TracmioSecurityEngine
import com.tracmio.app.ui.nativeapp.NativeHostCallbacks
import com.tracmio.app.ui.nativeapp.NativeTracmioApp
import com.tracmio.app.ui.nativeapp.NativeTracmioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : FragmentActivity(), NativeHostCallbacks {

    companion object {
        private const val MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024
        private val ALLOWED_ATTACHMENT_MIME_TYPES = setOf(
            "image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif", "application/pdf"
        )
    }

    private lateinit var viewModel: NativeTracmioViewModel
    private lateinit var database: TracmioDatabase
    private lateinit var prefsStore: PreferencesAndThemeStore

    private var pendingBackupJson: String? = null
    private var pendingWorkbookBytes: ByteArray? = null
    private var pendingCsvText: String? = null
    private var pendingDecryptPassword: CharArray? = null
    private var attachmentCallback: ((String, String, String, Long) -> Unit)? = null
    private var receiptCameraCallback: ((String, String, String, Long) -> Unit)? = null

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                TracmioReminderScheduler.rescheduleFromPrefs(this)
                viewModel.showStatus("Notification permission granted")
            } else {
                viewModel.showStatus("Notifications disabled in system settings")
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                runCatching { captureCameraPreviewLauncher.launch(null) }
            } else {
                receiptCameraCallback = null
                viewModel.showStatus("Camera permission required to take photo")
            }
        }

    private val chooseBackgroundVisualMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) handlePickedWallpaperUri(uri)
        }

    private val captureCameraPreviewLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            val receiptCb = receiptCameraCallback
            receiptCameraCallback = null
            if (bitmap != null) {
                if (receiptCb != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val dir = File(filesDir, "attachments").apply { mkdirs() }
                        val fileName = "receipt_${System.currentTimeMillis()}.jpg"
                        val dest = File(dir, fileName)
                        val bytesOut = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bytesOut)
                        val bytes = bytesOut.toByteArray()
                        dest.writeBytes(bytes)
                        withContext(Dispatchers.Main) {
                            receiptCb(fileName, "image/jpeg", dest.absolutePath, bytes.size.toLong())
                        }
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val savedPath = saveCompressedWallpaperBitmap(bitmap)
                        withContext(Dispatchers.Main) {
                            viewModel.updateSettings { it.copy(customWallpaperPath = savedPath) }
                            viewModel.showStatus("Custom camera wallpaper applied")
                        }
                    }
                }
            }
        }

    private val pickTrackerAttachmentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val cb = attachmentCallback
            attachmentCallback = null
            if (uri != null && cb != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val mimeType = (contentResolver.getType(uri) ?: "application/octet-stream").lowercase()
                        if (mimeType !in ALLOWED_ATTACHMENT_MIME_TYPES) return@launch
                        val rawBytes = contentResolver.openInputStream(uri)?.use { input ->
                            val out = ByteArrayOutputStream()
                            val buf = ByteArray(16 * 1024)
                            var total = 0
                            while (true) {
                                val n = input.read(buf)
                                if (n <= 0) break
                                total += n
                                if (total > MAX_ATTACHMENT_BYTES) return@launch
                                out.write(buf, 0, n)
                            }
                            out.toByteArray()
                        } ?: ByteArray(0)
                        if (rawBytes.isEmpty()) return@launch
                        val rawName = uri.lastPathSegment?.substringAfterLast('/') ?: "attachment_${System.currentTimeMillis()}"
                        val fileName = rawName.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(80)
                        val dir = File(filesDir, "attachments").apply { mkdirs() }
                        val dest = File(dir, "${System.currentTimeMillis()}_$fileName")
                        dest.writeBytes(rawBytes)
                        withContext(Dispatchers.Main) {
                            cb(fileName, mimeType, dest.absolutePath, rawBytes.size.toLong())
                        }
                    }
                }
            }
        }

    private val exportBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            val payload = pendingBackupJson
            pendingBackupJson = null
            if (uri != null && payload != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(payload)
                        }
                    }.onSuccess {
                        withContext(Dispatchers.Main) { viewModel.showStatus("Backup exported cleanly") }
                    }.onFailure {
                        withContext(Dispatchers.Main) { viewModel.showStatus("Export failed") }
                    }
                }
            }
        }

    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val pwd = pendingDecryptPassword
            pendingDecryptPassword = null
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val raw = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: ""
                        val plainJson = if (pwd != null && pwd.isNotEmpty() && raw.contains("\"cipherText\"")) {
                            try {
                                TracmioSecurityEngine.decryptBackup(raw, pwd)
                            } finally {
                                pwd.fill('\u0000')
                            }
                        } else {
                            pwd?.fill('\u0000')
                            raw
                        }
                        viewModel.restoreJsonFromText(plainJson)
                    }.onFailure { err ->
                        withContext(Dispatchers.Main) {
                            viewModel.showStatus(err.message ?: "Invalid or wrong backup password")
                        }
                    }
                }
            } else {
                pwd?.fill('\u0000')
            }
        }

    private val exportWorkbookLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        ) { uri: Uri? ->
            val bytes = pendingWorkbookBytes
            pendingWorkbookBytes = null
            if (uri != null && bytes != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(bytes)
                            out.flush()
                        }
                    }.onSuccess {
                        withContext(Dispatchers.Main) { viewModel.showStatus("Excel workbook exported") }
                    }
                }
            }
        }

    private val exportCsvLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
            val csv = pendingCsvText
            pendingCsvText = null
            if (uri != null && csv != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(csv)
                        }
                    }.onSuccess {
                        withContext(Dispatchers.Main) { viewModel.showStatus("CSV ledger exported") }
                    }
                }
            }
        }

    private val importCsvLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val csvText = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: ""
                        if (csvText.isNotBlank()) viewModel.importCsvFromText(csvText)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = TracmioDatabase.getDatabase(applicationContext)
        prefsStore = PreferencesAndThemeStore.getInstance(applicationContext)
        val repository = FinanceRepository(database, prefsStore)
        viewModel = ViewModelProvider(
            this,
            NativeTracmioViewModel.factory(application, repository)
        )[NativeTracmioViewModel::class.java]

        runFirstLaunchLegacyJsonMigration(repository)
        extractQuickAction(intent)
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { TracmioReminderScheduler.rescheduleFromPrefs(applicationContext) }
        }

        setContent {
            NativeTracmioApp(
                viewModel = viewModel,
                hostCallbacks = this
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractQuickAction(intent)
    }

    private fun extractQuickAction(intent: Intent?) {
        val action = intent?.getStringExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION)
        if (!action.isNullOrBlank()) {
            viewModel.setPendingNotificationAction(action)
            intent.removeExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION)
        }
    }

    private fun runFirstLaunchLegacyJsonMigration(repository: FinanceRepository) {
        lifecycleScope.launch(Dispatchers.IO) {
            val migrationPrefs = TracmioSecurityEngine.getEncryptedPrefs(applicationContext, "tracmio_migration_state")
            val alreadyMigrated = migrationPrefs.getBoolean("legacy_json_migrated_v16", false)
            if (alreadyMigrated) return@launch

            val existingAccounts = repository.allAccountsIncludingArchived.first().size
            val existingTxns = repository.transactions.first().size
            val candidates = listOf(
                File(filesDir, "tracmio_backup.json"),
                File(filesDir, "fintrack_backup.json")
            )
            val legacyJson = candidates.firstOrNull { it.exists() && it.length() > 2L }
                ?.runCatching { readText(Charsets.UTF_8) }
                ?.getOrNull()

            if (LegacyMigrationGuard.shouldImportLegacyJson(alreadyMigrated, existingAccounts, existingTxns, legacyJson)) {
                runCatching {
                    repository.restoreJson(legacyJson!!)
                }
            }
            migrationPrefs.edit().putBoolean("legacy_json_migrated_v16", true).apply()
        }
    }

    private fun handlePickedWallpaperUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
                val maxDim = max(bounds.outWidth, bounds.outHeight)
                var sampleSize = 1
                while (maxDim / sampleSize > 1440) sampleSize *= 2
                val decoded = BitmapFactory.decodeByteArray(
                    rawBytes, 0, rawBytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize }
                ) ?: return@launch
                val savedPath = saveCompressedWallpaperBitmap(decoded)
                withContext(Dispatchers.Main) {
                    viewModel.updateSettings { it.copy(customWallpaperPath = savedPath) }
                    viewModel.showStatus("Custom wallpaper applied")
                }
            }
        }
    }

    private fun saveCompressedWallpaperBitmap(decoded: Bitmap): String {
        val longest = max(decoded.width, decoded.height).toFloat()
        val bitmap = if (longest > 1080f) {
            val ratio = 1080f / longest
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).roundToInt().coerceAtLeast(1),
                (decoded.height * ratio).roundToInt().coerceAtLeast(1),
                true
            )
        } else decoded
        val outFile = File(filesDir, "tracmio_custom_wallpaper.jpg")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 78, fos)
        }
        return outFile.absolutePath
    }

    override fun chooseWallpaperImage() {
        runCatching {
            chooseBackgroundVisualMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    override fun captureWallpaperImage() {
        receiptCameraCallback = null
        if (hasCameraPermission()) {
            runCatching { captureCameraPreviewLauncher.launch(null) }
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun pickTrackerAttachment(onPicked: (String, String, String, Long) -> Unit) {
        attachmentCallback = onPicked
        pickTrackerAttachmentLauncher.launch(arrayOf("image/*", "application/pdf"))
    }

    override fun captureReceiptPhoto(onCaptured: (String, String, String, Long) -> Unit) {
        receiptCameraCallback = onCaptured
        if (hasCameraPermission()) {
            runCatching { captureCameraPreviewLauncher.launch(null) }
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun exportPlainJsonBackup(fileName: String) {
        lifecycleScope.launch {
            val json = viewModel.exportJsonString()
            pendingBackupJson = json
            exportBackupLauncher.launch(fileName)
        }
    }

    override fun exportEncryptedJsonBackup(fileName: String, password: CharArray) {
        lifecycleScope.launch {
            val encrypted = withContext(Dispatchers.IO) {
                val json = viewModel.exportJsonString()
                try {
                    TracmioSecurityEngine.encryptBackup(json, password)
                } finally {
                    password.fill('\u0000')
                }
            }
            pendingBackupJson = encrypted
            exportBackupLauncher.launch(fileName)
        }
    }

    override fun importJsonBackup(decryptPassword: CharArray?) {
        pendingDecryptPassword = decryptPassword
        importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    override fun exportWorkbookXlsx(fileName: String) {
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                val json = viewModel.exportJsonString()
                XlsxExporter.create(json)
            }
            pendingWorkbookBytes = bytes
            exportWorkbookLauncher.launch(fileName)
        }
    }

    override fun exportCsv(fileName: String) {
        lifecycleScope.launch {
            pendingCsvText = viewModel.exportCsvString()
            exportCsvLauncher.launch(fileName)
        }
    }

    override fun importCsv() {
        importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
    }

    override fun printReceipt(tx: TransactionEntity, accountName: String) {
        NativeStatementPdf.printReceipt(this, tx, accountName)
    }

    override fun printStatement(accounts: List<AccountEntity>, transactions: List<TransactionEntity>) {
        NativeStatementPdf.printLedgerStatement(this, accounts, transactions)
    }

    override fun printCustomReport(jobName: String, title: String, lines: List<Pair<String, Boolean>>) {
        NativeStatementPdf.printCustomReport(this, jobName, title, lines)
    }

    override fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Tracmio", text))
        viewModel.showStatus("Copied to clipboard")
    }

    override fun readClipboardText(): String? {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount <= 0) return null
        return clip.getItemAt(0)?.coerceToText(this)?.toString()?.trim()?.takeIf { it.isNotBlank() }
    }

    override fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    override fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    override fun getDeviceSecurityCapability(): DeviceSecurityCapability =
        BiometricCapabilityChecker.inspect(this)

    override fun authenticateBiometric(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val capability = getDeviceSecurityCapability()
        if (capability.biometricReadiness != BiometricReadiness.AVAILABLE) {
            if (capability.deviceCredentialAvailable) {
                authenticateDeviceCredential(onSuccess, onFailure)
            } else {
                onFailure(capability.statusText)
            }
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onFailure(errString.toString())
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Tracmio Vault")
            .setSubtitle("Verify Fingerprint or Face Unlock offline")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText("Use Liquid Glass PIN")
            .setConfirmationRequired(false)
            .build()
        runCatching { prompt.authenticate(info) }
            .onFailure { onFailure(it.message ?: "Biometric prompt unavailable") }
    }

    override fun authenticateDeviceCredential(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val capability = getDeviceSecurityCapability()
        if (!capability.deviceCredentialAvailable) {
            onFailure("No device PIN/pattern set — use your Liquid Glass PIN")
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onFailure(errString.toString())
                }
            }
        )
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Tracmio Vault")
            .setSubtitle("Verify with Device PIN, Pattern, or Biometrics")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        runCatching { prompt.authenticate(builder.build()) }
            .onFailure { onFailure(it.message ?: "Device lock screen unavailable") }
    }

    override fun scheduleDailyReminder(enabled: Boolean, hour: Int, minute: Int, userName: String) {
        TracmioReminderScheduler.setUserName(this, userName)
        TracmioReminderScheduler.update(
            context = this,
            enabled = enabled,
            hour = hour,
            minute = minute,
            memeMode = true,
            alertCategory = "ALL"
        )
        if (enabled && !hasNotificationPermission()) {
            requestNotificationPermission()
        }
    }

    override fun dispatchTestReminder() {
        if (hasNotificationPermission()) {
            DailyReminderReceiver.showReminderNotification(this, true)
        } else {
            requestNotificationPermission()
        }
    }

    override fun scheduleCalendarEventReminder(title: String, message: String, triggerAtMillis: Long, eventId: String) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        if (!hasNotificationPermission()) {
            requestNotificationPermission()
        }
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(this, EventReminderReceiver::class.java).apply {
            putExtra(EventReminderReceiver.EXTRA_TYPE, EventReminderReceiver.TYPE_CALENDAR_EVENT)
            putExtra(EventReminderReceiver.EXTRA_TITLE, title.trim().take(120))
            putExtra(EventReminderReceiver.EXTRA_MESSAGE, message.trim().take(300))
            putExtra(EventReminderReceiver.EXTRA_EVENT_ID, eventId.trim().take(80))
        }
        val pending = PendingIntent.getBroadcast(
            this,
            (eventId.hashCode() and 0x7FFFFFFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    override fun scheduleMonthlyBackupReminder(enabled: Boolean, dayOfMonth: Int, hour: Int, minute: Int) {
        val safeDay = dayOfMonth.coerceIn(1, 28)
        val safeHour = hour.coerceIn(0, 23)
        val safeMin = minute.coerceIn(0, 59)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(this, EventReminderReceiver::class.java).apply {
            putExtra(EventReminderReceiver.EXTRA_TYPE, EventReminderReceiver.TYPE_MONTHLY_BACKUP)
            putExtra(EventReminderReceiver.EXTRA_TITLE, "Tracmio Monthly Backup Reminder")
            putExtra(EventReminderReceiver.EXTRA_MESSAGE, "Export your encrypted JSON backup in Tracmio.")
            putExtra(EventReminderReceiver.EXTRA_EVENT_ID, "monthly_backup_reminder")
        }
        val pending = PendingIntent.getBroadcast(
            this,
            4199,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (!enabled) {
            alarmManager.cancel(pending)
            return
        }
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, safeDay)
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.MONTH, 1)
        }
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pending)
        }
        if (!hasNotificationPermission()) requestNotificationPermission()
    }
}
