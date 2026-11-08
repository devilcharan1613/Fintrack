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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
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
            "Take a moment to review today's spending.",
            "Your daily money check-in is ready.",
            "A quick review today can help tomorrow."
        )
        val funny = listOf(
            "Your wallet called. It wants a quick review. 😄",
            "Small expenses have big group chats. Check today's spending.",
            "Be the boss of your budget, not its mystery guest."
        )
        val messages = if (memeMode) funny else normal
        val message = messages[(System.currentTimeMillis() % messages.size).toInt()]
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FinTrack daily reminder")
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(Icon.createWithResource(context, R.mipmap.ic_launcher), "Add expense", expenseAction).build())
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

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var backupSaveLauncher: ActivityResultLauncher<String>
    private lateinit var workbookSaveLauncher: ActivityResultLauncher<String>
    private lateinit var csvSaveLauncher: ActivityResultLauncher<String>
    private lateinit var backupOpenLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var apkOpenLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var installSettingsLauncher: ActivityResultLauncher<Intent>

    private var pendingExportBytes: ByteArray? = null
    private var pendingInstallUri: Uri? = null
    private var pendingReminder: ReminderConfig? = null
    private var pendingQuickAction: String? = null
    private var pageReady = false
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
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            val reminder = pendingReminder
            pendingReminder = null
            if (granted && reminder != null) applyReminder(reminder)
            else if (reminder?.enabled == true) {
                Toast.makeText(this, "Allow notifications to enable daily reminders.", Toast.LENGTH_LONG).show()
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
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            addJavascriptInterface(FinTrackBridge(), "FinTrackBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    pageReady = true
                    flushPendingQuickAction()
                }
            }
            webChromeClient = WebChromeClient()
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                evaluateJavascript(
                    "window.finTrackApplyInsets(${safe.left},${safe.top},${safe.right},${safe.bottom},${ime.bottom});",
                    null
                )
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
                            PrintAttributes.Builder().build()
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
                val json = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException("Unable to read selected backup")
                runOnUiThread {
                    webView.evaluateJavascript(
                        "if(window.__importBackupFromAndroid){window.__importBackupFromAndroid(${JSONObject.quote(json)});}",
                        null
                    )
                }
            } catch (error: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Backup import failed: " + (error.message ?: "unknown error"), Toast.LENGTH_LONG).show()
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
                } else applyReminder(config)
            }
        }

        @JavascriptInterface
        fun getLegacyData(): String = "null"

        private fun safeFileName(input: String, fallback: String): String {
            val safe = input.substringAfterLast('/').substringAfterLast('\\')
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            return safe.take(100).ifBlank { fallback }
        }
    }

    override fun onDestroy() {
        pageReady = false
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("FinTrackBridge")
            webView.destroy()
        }
        printWebViews.forEach { it.destroy() }
        printWebViews.clear()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_QUICK_ACTION = "fintrack.quick_action"
        private const val REMINDER_PREFS = "fintrack_reminders"
    }
}
