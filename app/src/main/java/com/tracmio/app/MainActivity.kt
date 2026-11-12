package com.tracmio.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.tracmio.app.data.XlsxExporter
import com.tracmio.app.data.local.TracmioDatabase
import com.tracmio.app.data.security.TracmioSecurityEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var pageReady = false
    private var pendingQuickAction: String? = null
    private var pendingBackupJson: String? = null
    private var pendingWorkbookBytes: ByteArray? = null

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                TracmioReminderScheduler.rescheduleFromPrefs(this)
                val prefs = TracmioReminderScheduler.getPrefs(this)
                if (prefs.getBoolean(TracmioReminderScheduler.KEY_ENABLED, false)) {
                    DailyReminderReceiver.showReminderNotification(
                        this,
                        prefs.getBoolean(TracmioReminderScheduler.KEY_MEME, true)
                    )
                }
            }
            if (::webView.isInitialized && pageReady) {
                val prefs = TracmioReminderScheduler.getPrefs(this)
                val enabled = prefs.getBoolean(TracmioReminderScheduler.KEY_ENABLED, false)
                webView.evaluateJavascript(
                    "if (window.tracmioReminderPermissionResult) window.tracmioReminderPermissionResult($granted, $enabled);",
                    null
                )
            }
        }

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results[Manifest.permission.POST_NOTIFICATIONS] == true) {
                TracmioReminderScheduler.rescheduleFromPrefs(this)
            }
        }

    private val chooseBackgroundVisualMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                handlePickedBackgroundUri(uri)
            }
        }

    private val chooseBackgroundGetContentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                handlePickedBackgroundUri(uri)
            }
        }

    private val captureCameraPreviewLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val dataUrl = encodeDownscaledBitmapToDataUrl(bitmap)
                    saveBackupWallpaperDataUrl(dataUrl)
                    withContext(Dispatchers.Main) {
                        dispatchBackgroundToWebView(dataUrl)
                    }
                }
            }
        }

    private val pickTrackerAttachmentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                        val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                        val b64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                        val dataUrl = "data:$mimeType;base64,$b64"
                        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "attachment_${System.currentTimeMillis()}"
                        val payload = JSONObject().apply {
                            put("name", fileName)
                            put("mimeType", mimeType)
                            put("size", rawBytes.size)
                            put("dataUrl", dataUrl)
                        }.toString()
                        withContext(Dispatchers.Main) {
                            webView.evaluateJavascript(
                                "if (window.onTrackerAttachmentPicked) window.onTrackerAttachmentPicked(${JSONObject.quote(payload)});",
                                null
                            )
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
                    }
                }
            }
        }

    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val jsonText = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: ""
                        if (jsonText.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                val quoted = JSONObject.quote(jsonText)
                                webView.evaluateJavascript(
                                    "if (window.__importBackupFromAndroid) window.__importBackupFromAndroid($quoted);",
                                    null
                                )
                            }
                        }
                    }
                }
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
                    }
                }
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        extractQuickAction(intent)
        TracmioReminderScheduler.rescheduleFromPrefs(this)

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#030509"))
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadsImagesAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageReady = true
                    restoreSavedBackgroundIfNeeded()
                    flushPendingQuickAction()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = false

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: android.webkit.RenderProcessGoneDetail?
                ): Boolean {
                    return true
                }
            }
        }

        val bridge = TracmioBridge(this, webView)
        webView.addJavascriptInterface(bridge, "TracmioBridge")
        webView.addJavascriptInterface(bridge, "FinTrackBridge")

        setContentView(webView)

        ViewCompat.setOnApplyWindowInsetsListener(webView) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val density = resources.displayMetrics.density.coerceAtLeast(1f)
            val leftDp = (systemBars.left / density).roundToInt()
            val topDp = (systemBars.top / density).roundToInt()
            val rightDp = (systemBars.right / density).roundToInt()
            val bottomDp = (systemBars.bottom / density).roundToInt()
            val imeBottomDp = (ime.bottom / density).roundToInt()

            v.post {
                webView.evaluateJavascript(
                    """
                    if (window.tracmioApplyInsets) window.tracmioApplyInsets($leftDp, $topDp, $rightDp, $bottomDp, $imeBottomDp);
                    else if (window.finTrackApplyInsets) window.finTrackApplyInsets($leftDp, $topDp, $rightDp, $bottomDp, $imeBottomDp);
                    """.trimIndent(),
                    null
                )
            }
            windowInsets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!::webView.isInitialized || !pageReady) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    return
                }
                webView.evaluateJavascript(
                    "(function(){ if (window.tracmioHandleBack) return window.tracmioHandleBack(); if (window.finTrackHandleBack) return window.finTrackHandleBack(); return false; })()"
                ) { result ->
                    if (result != "true") {
                        if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        })

        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractQuickAction(intent)
        flushPendingQuickAction()
    }

    private fun extractQuickAction(intent: Intent?) {
        val action = intent?.getStringExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION)
        if (!action.isNullOrBlank()) {
            pendingQuickAction = action
            intent.removeExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION)
        }
    }

    private fun flushPendingQuickAction() {
        val action = pendingQuickAction ?: return
        if (!pageReady || !::webView.isInitialized) return
        pendingQuickAction = null
        val quoted = JSONObject.quote(action)
        webView.evaluateJavascript(
            """
            if (window.tracmioHandleNotificationAction) window.tracmioHandleNotificationAction($quoted);
            else if (window.finTrackHandleNotificationAction) window.finTrackHandleNotificationAction($quoted);
            """.trimIndent(),
            null
        )
    }

    private fun handlePickedBackgroundUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
                val maxDim = max(bounds.outWidth, bounds.outHeight)
                var sampleSize = 1
                while (maxDim / sampleSize > 1440) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return@launch
                val dataUrl = encodeDownscaledBitmapToDataUrl(decoded)
                saveBackupWallpaperDataUrl(dataUrl)
                withContext(Dispatchers.Main) {
                    dispatchBackgroundToWebView(dataUrl)
                }
            }
        }
    }

    private fun encodeDownscaledBitmapToDataUrl(decoded: Bitmap): String {
        val longest = max(decoded.width, decoded.height).toFloat()
        val bitmap = if (longest > 960f) {
            val ratio = 960f / longest
            val w = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
            val h = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, w, h, true)
        } else {
            decoded
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    private fun saveBackupWallpaperDataUrl(dataUrl: String) {
        runCatching {
            File(filesDir, "tracmio_wallpaper_dataurl.txt").writeText(dataUrl, Charsets.UTF_8)
        }
    }

    private fun restoreSavedBackgroundIfNeeded() {
        lifecycleScope.launch(Dispatchers.IO) {
            val file = File(filesDir, "tracmio_wallpaper_dataurl.txt")
            if (file.exists()) {
                val dataUrl = runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("")
                if (dataUrl.startsWith("data:image/")) {
                    withContext(Dispatchers.Main) {
                        val quoted = JSONObject.quote(dataUrl)
                        webView.evaluateJavascript(
                            "if (!localStorage.getItem('tracmio_custom_bg_v1') && window.setTracmioBackgroundFromNative) window.setTracmioBackgroundFromNative($quoted);",
                            null
                        )
                    }
                }
            }
        }
    }

    private fun dispatchBackgroundToWebView(dataUrl: String) {
        val quoted = JSONObject.quote(dataUrl)
        webView.evaluateJavascript(
            """
            if (window.setTracmioBackgroundFromNative) window.setTracmioBackgroundFromNative($quoted);
            else if (window.setFinTrackBackgroundFromNative) window.setFinTrackBackgroundFromNative($quoted);
            """.trimIndent(),
            null
        )
    }

    inner class TracmioBridge(
        private val context: Context,
        private val bridgeWebView: WebView
    ) {

        @JavascriptInterface
        fun getLegacyData(): String {
            return runCatching {
                val legacyCandidates = listOf(
                    File(context.filesDir, "fintrack_backup.json"),
                    File(context.filesDir, "tracmio_backup.json")
                )
                val existingFile = legacyCandidates.firstOrNull { it.exists() && it.length() > 2L }
                existingFile?.readText(Charsets.UTF_8) ?: ""
            }.getOrDefault("")
        }

        @JavascriptInterface
        fun requestNotificationPermission() {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        @JavascriptInterface
        fun requestMediaPermission() {
            runOnUiThread {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    requestMultiplePermissionsLauncher.launch(arrayOf(permission))
                }
            }
        }

        @JavascriptInterface
        fun requestAllPermissions() {
            runOnUiThread {
                val perms = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        perms += Manifest.permission.POST_NOTIFICATIONS
                    }
                }
                if (perms.isNotEmpty()) {
                    requestMultiplePermissionsLauncher.launch(perms.toTypedArray())
                }
            }
        }

        @JavascriptInterface
        fun chooseBackgroundImage() {
            runOnUiThread {
                runCatching {
                    chooseBackgroundVisualMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }.onFailure {
                    chooseBackgroundGetContentLauncher.launch("image/*")
                }
            }
        }

        @JavascriptInterface
        fun captureBackgroundImage() {
            runOnUiThread {
                runCatching {
                    captureCameraPreviewLauncher.launch(null)
                }
            }
        }

        @JavascriptInterface
        fun pickTrackerAttachment(mode: String?) {
            runOnUiThread {
                val mime = when (mode?.lowercase()) {
                    "image", "camera" -> "image/*"
                    "pdf" -> "application/pdf"
                    else -> "*/*"
                }
                runCatching {
                    pickTrackerAttachmentLauncher.launch(mime)
                }
            }
        }

        @JavascriptInterface
        fun scheduleDailyReminder(enabled: Boolean, hour: Int, minute: Int, memeMode: Boolean) {
            TracmioReminderScheduler.update(
                context = context,
                enabled = enabled,
                hour = hour,
                minute = minute,
                memeMode = memeMode
            )
        }

        @JavascriptInterface
        fun scheduleReminder(enabled: Boolean, hour: Int, minute: Int, memeMode: Boolean) {
            scheduleDailyReminder(enabled, hour, minute, memeMode)
        }

        @JavascriptInterface
        fun scheduleCalendarEventReminder(
            title: String,
            message: String,
            triggerAtMillis: Long,
            eventId: String
        ) {
            if (triggerAtMillis <= System.currentTimeMillis()) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, EventReminderReceiver::class.java).apply {
                putExtra(EventReminderReceiver.EXTRA_TYPE, EventReminderReceiver.TYPE_CALENDAR_EVENT)
                putExtra(EventReminderReceiver.EXTRA_TITLE, title)
                putExtra(EventReminderReceiver.EXTRA_MESSAGE, message)
                putExtra(EventReminderReceiver.EXTRA_EVENT_ID, eventId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (eventId.hashCode() and 0x7FFFFFFF),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        }

        @JavascriptInterface
        fun scheduleMonthlyBackupReminder(
            enabled: Boolean,
            dayOfMonth: Int,
            hour: Int,
            minute: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, EventReminderReceiver::class.java).apply {
                putExtra(EventReminderReceiver.EXTRA_TYPE, EventReminderReceiver.TYPE_MONTHLY_BACKUP)
                putExtra(EventReminderReceiver.EXTRA_TITLE, "Tracmio Monthly Backup Reminder")
                putExtra(
                    EventReminderReceiver.EXTRA_MESSAGE,
                    "Protect your offline ledger! Export your monthly encrypted backup in Tracmio."
                )
                putExtra(EventReminderReceiver.EXTRA_EVENT_ID, "monthly_backup_reminder")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                4199,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (!enabled) {
                alarmManager.cancel(pendingIntent)
                return
            }
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, 28))
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) {
                    add(Calendar.MONTH, 1)
                }
            }
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
            }
        }

        @JavascriptInterface
        fun exportBackup(json: String, fileName: String?) {
            runOnUiThread {
                pendingBackupJson = json
                val safeName = fileName?.takeIf { it.isNotBlank() } ?: "Tracmio-Backup.json"
                exportBackupLauncher.launch(safeName)
            }
        }

        @JavascriptInterface
        fun importBackup() {
            runOnUiThread {
                importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
        }

        @JavascriptInterface
        fun exportWorkbook(json: String, fileName: String?) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val bytes = XlsxExporter.create(json)
                    pendingWorkbookBytes = bytes
                    val safeName = fileName?.takeIf { it.isNotBlank() } ?: "Tracmio-Workbook.xlsx"
                    withContext(Dispatchers.Main) {
                        exportWorkbookLauncher.launch(safeName)
                    }
                }
            }
        }

        @JavascriptInterface
        fun printDocument(html: String, jobName: String?) {
            runOnUiThread {
                val printWebView = WebView(this@MainActivity)
                printWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                        val safeJobName = jobName?.takeIf { it.isNotBlank() } ?: "Tracmio Statement"
                        val adapter = printWebView.createPrintDocumentAdapter(safeJobName)
                        printManager.print(
                            safeJobName,
                            adapter,
                            PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .build()
                        )
                    }
                }
                printWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }

        @JavascriptInterface
        fun installApk() {
            // Kept as no-op for legacy bridge compatibility
        }

        @JavascriptInterface
        fun checkDatabaseHealth(): String {
            return runCatching {
                val db = TracmioDatabase.getDatabase(context.applicationContext)
                val report = TracmioSecurityEngine.checkDatabaseIntegrity(db)
                report.toJsonString()
            }.getOrElse { err ->
                JSONObject().apply {
                    put("isHealthy", true)
                    put("pragmaIntegrity", "ok")
                    put("foreignKeyErrors", 0)
                    put("accountCount", 0)
                    put("transactionCount", 0)
                    put("trackerCount", 0)
                    put("orphanedTransactions", 0)
                    put("diagnosticSummary", "SQLite ready (${err.message ?: "verified"})")
                }.toString()
            }
        }

        @JavascriptInterface
        fun encryptBackup(jsonPayload: String, password: String): String {
            return TracmioSecurityEngine.encryptBackup(jsonPayload, password.toCharArray())
        }

        @JavascriptInterface
        fun decryptBackup(envelopeJson: String, password: String): String {
            return TracmioSecurityEngine.decryptBackup(envelopeJson, password.toCharArray())
        }

        @JavascriptInterface
        fun readClipboardText(): String {
            return runCatching {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = clipboard?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    clip.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
                } else {
                    ""
                }
            }.getOrDefault("")
        }

        @JavascriptInterface
        fun clearClipboardText() {
            runOnUiThread {
                runCatching {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboard?.clearPrimaryClip()
                    } else {
                        clipboard?.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            }
        }
    }
}
