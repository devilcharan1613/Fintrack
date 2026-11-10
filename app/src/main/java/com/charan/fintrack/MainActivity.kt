package com.charan.fintrack

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.Settings
import android.util.Base64
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.charan.fintrack.data.backup.NativeBackupService
import com.charan.fintrack.data.local.FinTrackDatabase
import com.charan.fintrack.ui.nativeapp.NativeFinTrackActivity
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Calendar

internal object ReminderTime {
    fun nextTriggerAt(nowMillis: Long, hour: Int, minute: Int): Long {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
}

internal object FinTrackReminderScheduler {
    private const val REQUEST_CODE = 2501
    private const val PREFS = "fintrack_reminders"

    fun schedule(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, Intent(context, DailyReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (!enabled) {
            manager.cancel(pending)
            return
        }
        val triggerAt = ReminderTime.nextTriggerAt(
            System.currentTimeMillis(), hour.coerceIn(0, 23), minute.coerceIn(0, 59)
        )
        manager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, triggerAt, AlarmManager.INTERVAL_DAY, pending
        )
    }

    fun restoreAfterBoot(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("enabled", false)) {
            schedule(context, true, prefs.getInt("hour", 20), prefs.getInt("minute", 0))
        }
    }
}

class DailyReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val prefs = context.getSharedPreferences("fintrack_reminders", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        val memeMode = prefs.getBoolean("memeMode", false)
        val normal = listOf(
            "Your daily money check-in is ready. Add today's income and expenses to keep your balance accurate.",
            "A one-minute update helps you stay on top of your money. Log today's transactions in FinTrack.",
            "Small daily updates make monthly reports useful. Add any income or expenses you made today."
        )
        val funny = listOf(
            "Your wallet called. It wants today's transactions logged 😄 Add your income and expenses!",
            "Receipts are not a filing system 😂 Add today's transactions in FinTrack.",
            "Your budget is waiting for its daily update 🍀 Log today's income and expenses.",
            "Money moved? Tell FinTrack about it before you forget 😅 Add a transaction now."
        )
        val messages = if (memeMode) funny else normal
        val messageKey = if (memeMode) "last_meme_index" else "last_normal_index"
        val previousIndex = prefs.getInt(messageKey, -1)
        val nextIndex = (previousIndex + 1) % messages.size
        prefs.edit().putInt(messageKey, nextIndex).apply()
        val message = messages[nextIndex]
        createChannel(context)

        val openApp = PendingIntent.getActivity(
            context, 2502,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val expenseAction = quickAction(context, "expense", 2503)
        val incomeAction = quickAction(context, "income", 2504)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, "fintrack_daily")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_fintrack_notification)
            .setContentTitle("FinTrack daily reminder")
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_fintrack_notification), "Add expense", expenseAction).build())
            .addAction(Notification.Action.Builder(Icon.createWithResource(context, R.mipmap.ic_launcher), "Add income", incomeAction).build())
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(2501, notification)
    }

    private fun quickAction(context: Context, type: String, requestCode: Int): PendingIntent {
        return PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_QUICK_ACTION, type)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "fintrack_daily", "FinTrack reminders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily offline finance check-in reminders" }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}

class ReminderBootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            FinTrackReminderScheduler.restoreAfterBoot(context)
        }
    }
}

/** Prevent an unreadable legacy WebView save from being migrated as an empty seed database. */
internal fun requireReadableLegacyState(recoveryRequired: Boolean) {
    require(!recoveryRequired) {
        "Saved finance data could not be read. The legacy recovery copy was preserved and native migration was stopped. Restore a valid backup before retrying."
    }
}

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_QUICK_ACTION = "com.charan.fintrack.EXTRA_QUICK_ACTION"
        private const val REMINDER_PREFS = "fintrack_reminders"
        private const val NATIVE_MIGRATION_PREFS = "fintrack_native_migration"
    }
    private lateinit var webView: WebView
    private lateinit var backupSaveLauncher: ActivityResultLauncher<String>
    private lateinit var workbookSaveLauncher: ActivityResultLauncher<String>
    private lateinit var csvSaveLauncher: ActivityResultLauncher<String>
    private lateinit var backupOpenLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var apkOpenLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var backgroundOpenLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var installSettingsLauncher: ActivityResultLauncher<Intent>

    private var pendingExportBytes: ByteArray? = null
    private var pendingInstallUri: Uri? = null
    private var pendingReminder: ReminderConfig? = null
    private var pendingQuickAction: String? = null
    private var pageReady = false
    @Volatile private var migrationInProgress = false
    private var latestInsetsScript = "window.finTrackApplyInsets(0,0,0,0,0);"
    private val printWebViews = mutableListOf<WebView>()

    private data class ReminderConfig(
        val enabled: Boolean,
        val hour: Int,
        val minute: Int,
        val memeMode: Boolean
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backupSaveLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { writePendingExport(it) }
        workbookSaveLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        ) { writePendingExport(it) }
        csvSaveLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv")
        ) { writePendingExport(it) }
        backupOpenLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            readBackup(it)
        }
        apkOpenLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            handleSelectedApk(it)
        }
        backgroundOpenLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            handleSelectedBackgroundImage(it)
        }
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            val reminder = pendingReminder
            pendingReminder = null
            if (granted && reminder != null) {
                applyReminder(reminder)
                notifyReminderPermissionResult(true, reminder.enabled)
            } else if (reminder?.enabled == true) {
                applyReminder(reminder.copy(enabled = false))
                notifyReminderPermissionResult(false, false)
                Toast.makeText(this, "Allow FinTrack notifications in Android Settings to enable reminders.", Toast.LENGTH_LONG).show()
            }
        }
        installSettingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            val uri = pendingInstallUri
            pendingInstallUri = null
            if (uri != null && canInstallPackages()) launchApkInstaller(uri)
            else if (uri != null) Toast.makeText(this, "Install permission was not granted.", Toast.LENGTH_LONG).show()
        }

        if (getSharedPreferences(NATIVE_MIGRATION_PREFS, Context.MODE_PRIVATE)
                .getBoolean("complete", false)
        ) {
            val nativeIntent = Intent(this, NativeFinTrackActivity::class.java)
            intent?.getStringExtra(EXTRA_QUICK_ACTION)?.let {
                nativeIntent.putExtra(EXTRA_QUICK_ACTION, it)
            }
            startActivity(nativeIntent)
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        pendingQuickAction = intent?.getStringExtra(EXTRA_QUICK_ACTION)
        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.setSupportZoom(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true
            }
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            addJavascriptInterface(FinTrackBridge(), "FinTrackBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    return handleWebViewNavigation(request.url)
                }

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return handleWebViewNavigation(Uri.parse(url))
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    pageReady = true
                    // The first insets callback can arrive before the HTML bridge exists.
                    // Reapply the latest system-bar/IME insets after the page is ready.
                    view.evaluateJavascript(latestInsetsScript, null)
                    flushPendingQuickAction()
                    // First app launch after the native upgrade migrates legacy local data
                    // into Room before opening Compose. Source WebView storage is retained.
                    if (!getSharedPreferences(NATIVE_MIGRATION_PREFS, Context.MODE_PRIVATE)
                            .getBoolean("complete", false)
                    ) {
                        requestNativeMigrationFromLegacyWebView()
                    }
                }
            }
            webChromeClient = WebChromeClient()
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                latestInsetsScript =
                    "window.finTrackApplyInsets(${safe.left},${safe.top},${safe.right},${safe.bottom},${ime.bottom});"
                // Keep the WebView edge-to-edge; the HTML applies these insets exactly once.
                evaluateJavascript(latestInsetsScript, null)
                view.setPadding(0, 0, 0, 0)
                insets
            }
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(
                    "window.finTrackHandleBack ? window.finTrackHandleBack() : false;"
                ) { result ->
                    if (result != "true") {
                        if (webView.canGoBack()) webView.goBack()
                        else (this@MainActivity as Activity).finish()
                    }
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingQuickAction = intent.getStringExtra(EXTRA_QUICK_ACTION)
        flushPendingQuickAction()
    }

    /**
     * Export the already-loaded legacy WebView state through its same-origin JavaScript
     * context, then merge it transactionally into Room before entering the native app.
     * A failed import never sets the completion marker, so the legacy app remains usable.
     */
    private fun requestNativeMigrationFromLegacyWebView() {
        if (!::webView.isInitialized || !pageReady) {
            Toast.makeText(this, "FinTrack is still loading. Try again in a moment.", Toast.LENGTH_SHORT).show()
            return
        }
        if (migrationInProgress) return

        val script = """
            (function () {
                var current = window.state;
                if (!current || !Array.isArray(current.accounts) || !Array.isArray(current.transactions)) return null;
                var reminder = null;
                try {
                    var savedReminder = localStorage.getItem('fintrack_reminder_settings');
                    reminder = savedReminder ? JSON.parse(savedReminder) : null;
                } catch (error) {}
                return JSON.stringify({
                    recoveryRequired: window.stateLoadWarning === true,
                    state: current,
                    theme: localStorage.getItem('fintrack_theme') || 'dark',
                    backgroundPhoto: localStorage.getItem('fintrack_custom_background') || '',
                    backgroundVisibility: localStorage.getItem('fintrack_background_visibility') || '35',
                    reminder: reminder
                });
            })()
        """.trimIndent()

        webView.evaluateJavascript(script) { encoded ->
            try {
                if (encoded.isNullOrBlank() || encoded == "null") {
                    throw IllegalStateException("Legacy finance data is not ready to migrate.")
                }
                val payload = JSONObject("{\"value\":$encoded}").getString("value")
                val wrapper = JSONObject(payload)
                requireReadableLegacyState(wrapper.optBoolean("recoveryRequired", false))
                migrateLegacyPayload(payload)
            } catch (error: Exception) {
                Toast.makeText(
                    this,
                    "Native migration could not read your existing data: ${error.message ?: "unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun migrateLegacyPayload(payload: String) {
        if (migrationInProgress) return
        migrationInProgress = true
        Thread {
            try {
                val wrapper = JSONObject(payload)
                val legacyState = wrapper.optJSONObject("state")
                    ?: throw IllegalArgumentException("Legacy finance records are missing.")
                val database = FinTrackDatabase.getDatabase(applicationContext)
                val report = runBlocking(Dispatchers.IO) {
                    NativeBackupService(database).mergeJson(legacyState.toString())
                }

                persistLegacyPreferences(wrapper)
                getSharedPreferences(NATIVE_MIGRATION_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("complete", true)
                    .putLong("completed_at", System.currentTimeMillis())
                    .apply()

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    val message = "Migration complete: ${report.totalAdded} records imported; ${report.duplicatesSkipped} duplicates skipped."
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    val nativeIntent = Intent(this, NativeFinTrackActivity::class.java)
                    pendingQuickAction?.let { nativeIntent.putExtra(EXTRA_QUICK_ACTION, it) }
                    startActivity(nativeIntent)
                    finish()
                }
            } catch (error: Exception) {
                migrationInProgress = false
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(
                            this,
                            "Migration failed. Your original FinTrack data is unchanged: ${error.message ?: "unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }.start()
    }

    /** Runs on the migration worker thread. The source WebView storage is left untouched. */
    private fun persistLegacyPreferences(wrapper: JSONObject) {
        val appearance = getSharedPreferences("fintrack_native_settings", Context.MODE_PRIVATE)
        val theme = wrapper.optString("theme", "dark").lowercase()
        val opacity = wrapper.optString("backgroundVisibility", "35")
            .toFloatOrNull()?.toInt()?.coerceIn(0, 100) ?: 35
        val appearanceEditor = appearance.edit()
            .putBoolean("dark_theme", theme != "light")
            .putInt("background_opacity", opacity)

        val photo = wrapper.optString("backgroundPhoto", "")
        if (photo.startsWith("data:image/") && photo.contains(",")) {
            try {
                val encoded = photo.substringAfter(',')
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                require(bytes.size <= 2_000_000) { "Background photo is larger than the supported local import size." }
                val file = File(filesDir, "fintrack_legacy_background.img")
                file.writeBytes(bytes)
                appearanceEditor.putString("background_uri", Uri.fromFile(file).toString())
            } catch (_: Exception) {
                // A background image failure must not discard successfully imported finance data.
                appearanceEditor.remove("background_uri")
            }
        } else {
            appearanceEditor.remove("background_uri")
        }
        appearanceEditor.apply()

        val reminder = wrapper.optJSONObject("reminder") ?: return
        val timeParts = reminder.optString("time", "20:00").split(':')
        val hour = timeParts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: 20
        val minute = timeParts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: 0
        val enabled = reminder.optBoolean("enabled", false)
        getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", enabled)
            .putInt("hour", hour)
            .putInt("minute", minute)
            .putBoolean("memeMode", reminder.optBoolean("memeMode", false))
            .apply()
        FinTrackReminderScheduler.schedule(this, enabled, hour, minute)
    }

    private fun handleWebViewNavigation(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme == "file" && uri.toString().startsWith("file:///android_asset/")) {
            return false
        }
        if (scheme == "https" || scheme == "http") {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (error: Exception) {
                Toast.makeText(this, "No app can open this link.", Toast.LENGTH_SHORT).show()
            }
        }
        // Only the bundled asset is allowed inside this WebView because it exposes
        // the native FinTrackBridge. Block all other main-frame schemes/URLs.
        return true
    }

    private fun flushPendingQuickAction() {
        val action = pendingQuickAction ?: return
        if (!pageReady || !::webView.isInitialized) return
        pendingQuickAction = null
        webView.evaluateJavascript(
            "if(window.finTrackHandleNotificationAction){window.finTrackHandleNotificationAction(${JSONObject.quote(action)});}",
            null
        )
    }

    private fun printHtmlDocument(html: String, jobName: String) {
        try {
            val printView = WebView(this)
            printWebViews.add(printView)
            printView.settings.javaScriptEnabled = false
            printView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    try {
                        val manager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                        manager.print(
                            jobName,
                            view.createPrintDocumentAdapter(jobName),
                            PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setResolution(PrintAttributes.Resolution("fintrack", "FinTrack", 300, 300))
                                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                .build()
                        )
                    } catch (error: Exception) {
                        Toast.makeText(this@MainActivity, "PDF print failed: " + (error.message ?: "unknown error"), Toast.LENGTH_LONG).show()
                    }
                }
            }
            printView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } catch (error: Exception) {
            Toast.makeText(this, "PDF print could not start: " + (error.message ?: "unknown error"), Toast.LENGTH_LONG).show()
        }
    }

    private fun writePendingExport(uri: Uri?) {
        val bytes = pendingExportBytes
        pendingExportBytes = null
        if (uri == null || bytes == null) return
        try {
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IllegalStateException("Unable to open selected file")
            Toast.makeText(this, "File saved successfully.", Toast.LENGTH_LONG).show()
        } catch (error: Exception) {
            Toast.makeText(this, "Save failed: " + (error.message ?: "unknown error"), Toast.LENGTH_LONG).show()
        }
    }

    private fun readBackup(uri: Uri?) {
        if (uri == null) return
        Thread {
            try {
                val json = contentResolver.openInputStream(uri)?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var totalBytes = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        output.write(buffer, 0, count)
                    }
                    String(output.toByteArray(), Charsets.UTF_8)
                } ?: throw IllegalStateException("Unable to read selected backup")

                val backup = JSONObject(json)
                if (backup.optJSONArray("accounts") == null ||
                    backup.optJSONArray("transactions") == null
                ) {
                    throw IllegalArgumentException("This is not a valid FinTrack backup.")
                }

                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        webView.evaluateJavascript(
                            "if(window.__importBackupFromAndroid){window.__importBackupFromAndroid(${JSONObject.quote(json)});}",
                            null
                        )
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(
                            this,
                            "Backup import failed: " + (error.message ?: "unknown error"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }.start()
    }

    private fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()

    private fun handleSelectedApk(uri: Uri?) {
        if (uri == null) return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
            // A temporary document-provider grant remains valid for this installation attempt.
        }
        if (!canInstallPackages()) {
            pendingInstallUri = uri
            installSettingsLauncher.launch(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
            )
        } else launchApkInstaller(uri)
    }

    private fun launchApkInstaller(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (error: Exception) {
            Toast.makeText(this, "Could not open Android package installer: " + (error.message ?: "unknown error"), Toast.LENGTH_LONG).show()
        }
    }

    private fun notifyReminderPermissionResult(granted: Boolean, reminderEnabled: Boolean = false) {
        if (!::webView.isInitialized || !pageReady) return
        runOnUiThread {
            webView.evaluateJavascript(
                "if(window.finTrackReminderPermissionResult){window.finTrackReminderPermissionResult($granted,$reminderEnabled);}",
                null
            )
        }
    }

    private fun handleSelectedBackgroundImage(uri: Uri?) {
        if (uri == null) return
        Thread {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val boundsStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Unable to read selected photo")
                boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    throw IllegalArgumentException("Selected file is not a supported image")
                }
                var sample = 1
                while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val decoded = contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: throw IllegalStateException("Unable to decode selected photo")
                val maxEdge = 900
                val scale = minOf(1.0, maxEdge.toDouble() / maxOf(decoded.width, decoded.height))
                val outputBitmap = if (scale < 1.0) {
                    Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * scale).toInt().coerceAtLeast(1),
                        (decoded.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else decoded
                val bytes = ByteArrayOutputStream().use { output ->
                    outputBitmap.compress(Bitmap.CompressFormat.JPEG, 58, output)
                    output.toByteArray()
                }
                if (outputBitmap !== decoded) outputBitmap.recycle()
                decoded.recycle()
                if (bytes.size > 650_000) {
                    throw IllegalArgumentException("Photo is too large for reliable offline storage. Choose a smaller image.")
                }
                val dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                runOnUiThread {
                    webView.evaluateJavascript(
                        "if(window.setFinTrackBackgroundFromNative){window.setFinTrackBackgroundFromNative(${JSONObject.quote(dataUrl)});}",
                        null
                    )
                }
            } catch (error: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Background photo could not be saved: " + (error.message ?: "unsupported image"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun applyReminder(config: ReminderConfig) {
        getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", config.enabled)
            .putInt("hour", config.hour.coerceIn(0, 23))
            .putInt("minute", config.minute.coerceIn(0, 59))
            .putBoolean("memeMode", config.memeMode)
            .apply()
        FinTrackReminderScheduler.schedule(this, config.enabled, config.hour, config.minute)
        Toast.makeText(
            this,
            if (config.enabled) "Daily FinTrack reminder saved." else "Daily reminders turned off.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private inner class FinTrackBridge {
        @JavascriptInterface
        fun exportBackup(json: String, fileName: String) {
            runOnUiThread {
                pendingExportBytes = json.toByteArray(Charsets.UTF_8)
                backupSaveLauncher.launch(safeFileName(fileName, "fintrack-backup.json"))
            }
        }

        @JavascriptInterface
        fun exportWorkbook(json: String, fileName: String) {
            Thread {
                try {
                    val bytes = XlsxExporter.create(json)
                    runOnUiThread {
                        pendingExportBytes = bytes
                        workbookSaveLauncher.launch(safeFileName(fileName, "fintrack-export.xlsx"))
                    }
                } catch (error: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Excel export failed: " + (error.message ?: "unknown error"), Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        @JavascriptInterface
        fun exportCsv(csv: String, fileName: String) {
            runOnUiThread {
                pendingExportBytes = ("\uFEFF" + csv).toByteArray(Charsets.UTF_8)
                csvSaveLauncher.launch(safeFileName(fileName, "fintrack-transactions.csv"))
            }
        }

        @JavascriptInterface
        fun printDocument(html: String, jobName: String) {
            runOnUiThread {
                printHtmlDocument(html, safeFileName(jobName, "FinTrack Statement"))
            }
        }

        @JavascriptInterface
        fun importBackup() {
            runOnUiThread { backupOpenLauncher.launch(arrayOf("application/json", "text/json", "application/octet-stream")) }
        }

        @JavascriptInterface
        fun installApk() {
            runOnUiThread {
                apkOpenLauncher.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
            }
        }

        @JavascriptInterface
        fun scheduleDailyReminder(enabled: Boolean, hour: Int, minute: Int, memeMode: Boolean) {
            runOnUiThread {
                val config = ReminderConfig(enabled, hour.coerceIn(0, 23), minute.coerceIn(0, 59), memeMode)
                if (enabled && Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingReminder = config
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    applyReminder(config)
                    notifyReminderPermissionResult(true, config.enabled)
                }
            }
        }

        @JavascriptInterface
        fun chooseBackgroundImage() {
            runOnUiThread {
                backgroundOpenLauncher.launch(arrayOf("image/*"))
            }
        }
    }
}


/** Restrict suggested document names to a safe, portable filename. */
private fun safeFileName(candidate: String, fallback: String): String {
    val cleaned = candidate
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .filter { it.isLetterOrDigit() || it in "._ -" }
        .trim()
        .trim('.')
        .take(120)
    return cleaned.ifBlank { fallback }
}
