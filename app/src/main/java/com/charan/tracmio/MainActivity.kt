package com.charan.tracmio

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.charan.tracmio.data.XlsxExporter
import com.charan.tracmio.data.local.CustomTrackerEntity
import com.charan.tracmio.data.local.TracmioDatabase
import com.charan.tracmio.data.local.TrackerActivityLogEntity
import com.charan.tracmio.data.local.TrackerAttachmentEntity
import com.charan.tracmio.data.local.TrackerCustomFieldEntity
import com.charan.tracmio.data.local.TrackerCustomValueEntity
import com.charan.tracmio.data.local.TrackerPersonEntity
import com.charan.tracmio.data.local.TrackerSettlementEntity
import com.charan.tracmio.data.local.TrackerTransactionCrossRefEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var printWebView: WebView? = null
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private var pendingExportContent: String? = null
    private var pendingWorkbookBytes: ByteArray? = null
    private var pendingQuickAction: String? = null
    private var pageLoaded: Boolean = false

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val notifGranted = results[Manifest.permission.POST_NOTIFICATIONS]
        if (notifGranted != null && ::webView.isInitialized && pageLoaded) {
            val reminderEnabled = TracmioReminderScheduler.getPrefs(this)
                .getBoolean(TracmioReminderScheduler.KEY_ENABLED, true)
            webView.post {
                webView.evaluateJavascript(
                    "window.tracmioReminderPermissionResult && window.tracmioReminderPermissionResult($notifGranted, $reminderEnabled);",
                    null
                )
            }
        }
        val allGranted = results.values.all { it }
        if (allGranted && results.isNotEmpty()) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val chooseBackgroundImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedBackgroundImage(uri)
        }
    }

    private val pickTrackerAttachmentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handlePickedTrackerAttachmentUri(uri)
        }
    }

    private val captureCameraPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            handleCapturedTrackerCameraPhoto(bitmap)
        }
    }

    private val genericFileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val callback = fileUploadCallback
        fileUploadCallback = null
        if (uri != null) {
            callback?.onReceiveValue(arrayOf(uri))
        } else {
            callback?.onReceiveValue(null)
        }
    }

    private val createBackupDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val content = pendingExportContent
        pendingExportContent = null
        if (uri != null && content != null) {
            runCatching {
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(StandardCharsets.UTF_8))
                }
                Toast.makeText(this, "Tracmio backup exported", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "Export failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val createCsvDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val content = pendingExportContent
        pendingExportContent = null
        if (uri != null && content != null) {
            runCatching {
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(StandardCharsets.UTF_8))
                }
                Toast.makeText(this, "CSV exported", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "CSV export failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val createWorkbookDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        val bytes = pendingWorkbookBytes
        pendingWorkbookBytes = null
        if (uri != null && bytes != null) {
            runCatching {
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                }
                Toast.makeText(this, "Excel workbook exported", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "Excel export failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val openBackupDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val jsonText = contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use {
                    it.readText()
                }.orEmpty()
                if (jsonText.isNotBlank()) {
                    val quoted = JSONObject.quote(jsonText)
                    webView.post {
                        webView.evaluateJavascript("window.__importBackupFromAndroid && window.__importBackupFromAndroid($quoted);", null)
                    }
                }
            }.onFailure {
                Toast.makeText(this, "Import failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#090D14")

        pendingQuickAction = intent?.getStringExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION)

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#090D14"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.textZoom = 100
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            val bridge = TracmioBridge()
            addJavascriptInterface(bridge, "TracmioBridge")
            addJavascriptInterface(bridge, "FinTrackBridge")

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageLoaded = true
                    ViewCompat.requestApplyInsets(this@apply)
                    dispatchPendingQuickActionIfReady()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileUploadCallback?.onReceiveValue(null)
                    fileUploadCallback = filePathCallback
                    val acceptTypes = fileChooserParams?.acceptTypes?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "*/*"
                    runCatching {
                        genericFileChooserLauncher.launch(acceptTypes)
                    }.onFailure {
                        fileUploadCallback = null
                        return false
                    }
                    return true
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(webView) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val density = view.resources.displayMetrics.density.coerceAtLeast(1f)
            val left = (systemBars.left / density).toInt()
            val top = (systemBars.top / density).toInt()
            val right = (systemBars.right / density).toInt()
            val bottom = (systemBars.bottom / density).toInt()
            val imeBottom = ((ime.bottom - systemBars.bottom).coerceAtLeast(0) / density).toInt()
            view.post {
                webView.evaluateJavascript(
                    "window.tracmioApplyInsets && window.tracmioApplyInsets($left, $top, $right, $bottom, $imeBottom);",
                    null
                )
            }
            windowInsets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!::webView.isInitialized) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    return
                }
                webView.evaluateJavascript("(function(){ return window.tracmioHandleBack ? window.tracmioHandleBack() : false; })()") { result ->
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

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")

        TracmioReminderScheduler.rescheduleFromPrefs(this)
        maybeRequestInitialPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.getStringExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION)
        if (!action.isNullOrBlank()) {
            pendingQuickAction = action
            dispatchPendingQuickActionIfReady()
        }
    }

    private fun dispatchPendingQuickActionIfReady() {
        val action = pendingQuickAction ?: return
        if (!pageLoaded || !::webView.isInitialized) return
        pendingQuickAction = null
        val quoted = JSONObject.quote(action)
        webView.post {
            webView.evaluateJavascript(
                "window.tracmioHandleNotificationAction && window.tracmioHandleNotificationAction($quoted);",
                null
            )
        }
    }

    private fun maybeRequestInitialPermissions() {
        val prefs = getSharedPreferences("tracmio_permissions", Context.MODE_PRIVATE)
        if (prefs.getBoolean("initial_prompt_shown", false)) return
        prefs.edit().putBoolean("initial_prompt_shown", true).apply()
        requestRuntimePermissions(includeNotifications = true, includeMedia = true)
    }

    private fun requestRuntimePermissions(includeNotifications: Boolean, includeMedia: Boolean) {
        val needed = mutableListOf<String>()
        if (includeNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.POST_NOTIFICATIONS
            }
        }
        if (includeMedia) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    needed += Manifest.permission.READ_MEDIA_IMAGES
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) != PackageManager.PERMISSION_GRANTED
                ) {
                    needed += Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    needed += Manifest.permission.READ_EXTERNAL_STORAGE
                }
            }
        }
        if (needed.isNotEmpty()) {
            requestPermissionsLauncher.launch(needed.toTypedArray())
        }
    }

    private fun handleSelectedBackgroundImage(uri: Uri) {
        runCatching {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
            val maxDim = 960
            var sampleSize = 1
            while ((boundsOpts.outWidth / sampleSize) > maxDim || (boundsOpts.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return
            val scale = minOf(
                maxDim.toFloat() / decoded.width.coerceAtLeast(1),
                maxDim.toFloat() / decoded.height.coerceAtLeast(1),
                1f
            )
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 60, out)
            val jpegBytes = out.toByteArray()
            File(filesDir, "tracmio_custom_bg.jpg").writeBytes(jpegBytes)
            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$base64"
            val quoted = JSONObject.quote(dataUrl)
            webView.post {
                webView.evaluateJavascript("window.setTracmioBackgroundFromNative($quoted);", null)
            }
        }.onFailure {
            Toast.makeText(this, "Could not load photo: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCapturedTrackerCameraPhoto(bitmap: Bitmap) {
        runCatching {
            val dir = File(filesDir, "tracker_attachments").apply { mkdirs() }
            val ts = System.currentTimeMillis()
            val fileName = "receipt_camera_$ts.jpg"
            val outFile = File(dir, fileName)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
            val bytes = out.toByteArray()
            outFile.writeBytes(bytes)
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val payload = JSONObject().apply {
                put("id", "att-$ts")
                put("fileName", fileName)
                put("mimeType", "image/jpeg")
                put("localPath", outFile.absolutePath)
                put("fileSizeBytes", bytes.size)
                put("dataUrl", "data:image/jpeg;base64,$base64")
                put("addedAt", ts)
            }
            val quoted = JSONObject.quote(payload.toString())
            webView.post {
                webView.evaluateJavascript("window.onTrackerAttachmentPicked && window.onTrackerAttachmentPicked($quoted);", null)
            }
        }.onFailure {
            Toast.makeText(this, "Camera capture failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePickedTrackerAttachmentUri(uri: Uri) {
        runCatching {
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            var displayName = "attachment_${System.currentTimeMillis()}"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
            val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
            val dir = File(filesDir, "tracker_attachments").apply { mkdirs() }
            val ts = System.currentTimeMillis()
            val safeFileName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val storedFile = File(dir, "${ts}_$safeFileName")

            var dataUrl = ""
            var finalBytes = rawBytes
            if (mimeType.startsWith("image/")) {
                val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                if (decoded != null) {
                    val maxDim = 960
                    val scale = minOf(
                        maxDim.toFloat() / decoded.width.coerceAtLeast(1),
                        maxDim.toFloat() / decoded.height.coerceAtLeast(1),
                        1f
                    )
                    val scaled = if (scale < 1f) {
                        Bitmap.createScaledBitmap(
                            decoded,
                            (decoded.width * scale).toInt().coerceAtLeast(1),
                            (decoded.height * scale).toInt().coerceAtLeast(1),
                            true
                        )
                    } else decoded
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
                    finalBytes = out.toByteArray()
                    dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(finalBytes, Base64.NO_WRAP)
                }
            }
            storedFile.writeBytes(finalBytes)

            val payload = JSONObject().apply {
                put("id", "att-$ts")
                put("fileName", displayName)
                put("mimeType", mimeType)
                put("localPath", storedFile.absolutePath)
                put("contentUri", uri.toString())
                put("fileSizeBytes", finalBytes.size)
                put("dataUrl", dataUrl)
                put("addedAt", ts)
            }
            val quoted = JSONObject.quote(payload.toString())
            webView.post {
                webView.evaluateJavascript("window.onTrackerAttachmentPicked && window.onTrackerAttachmentPicked($quoted);", null)
            }
        }.onFailure {
            Toast.makeText(this, "Could not attach file: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFileViaProvider(file: File, mimeType: String, title: String) {
        runCatching {
            val contentUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, title))
        }.onFailure {
            Toast.makeText(this, "Unable to open Share sheet: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    inner class TracmioBridge {

        @JavascriptInterface
        fun exportBackup(json: String, fileName: String?) {
            val safeName = fileName?.takeIf { it.isNotBlank() } ?: "tracmio_backup.json"
            runOnUiThread {
                pendingExportContent = json
                createBackupDocumentLauncher.launch(safeName)
            }
        }

        @JavascriptInterface
        fun importBackup() {
            runOnUiThread {
                openBackupDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
        }

        @JavascriptInterface
        fun exportCsv(csv: String, fileName: String?) {
            val safeName = fileName?.takeIf { it.isNotBlank() } ?: "tracmio_transactions.csv"
            runOnUiThread {
                pendingExportContent = csv
                createCsvDocumentLauncher.launch(safeName)
            }
        }

        @JavascriptInterface
        fun exportWorkbook(json: String, fileName: String?) {
            val safeName = fileName?.takeIf { it.isNotBlank() } ?: "tracmio_workbook.xlsx"
            runOnUiThread {
                runCatching {
                    pendingWorkbookBytes = XlsxExporter.create(json)
                    createWorkbookDocumentLauncher.launch(safeName)
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Failed to prepare Excel file: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun exportTrackerWorkbook(json: String, fileName: String?) {
            val safeName = fileName?.takeIf { it.isNotBlank() } ?: "tracmio_tracker_report.xlsx"
            runOnUiThread {
                runCatching {
                    pendingWorkbookBytes = XlsxExporter.createTrackerWorkbook(json)
                    createWorkbookDocumentLauncher.launch(safeName)
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Failed to prepare Tracker Excel file: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun shareExportFile(content: String, fileName: String?, mimeType: String?) {
            val safeName = (fileName?.takeIf { it.isNotBlank() } ?: "tracmio_export.json")
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val safeMime = mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
            runOnUiThread {
                runCatching {
                    val exportDir = File(cacheDir, "exports").apply { mkdirs() }
                    val outFile = File(exportDir, safeName)
                    outFile.writeText(content, StandardCharsets.UTF_8)
                    shareFileViaProvider(outFile, safeMime, "Share $safeName")
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Share failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun shareTrackerWorkbook(json: String, fileName: String?) {
            val safeName = (fileName?.takeIf { it.isNotBlank() } ?: "tracmio_tracker.xlsx")
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            runOnUiThread {
                runCatching {
                    val bytes = XlsxExporter.createTrackerWorkbook(json)
                    val exportDir = File(cacheDir, "exports").apply { mkdirs() }
                    val outFile = File(exportDir, safeName)
                    outFile.writeBytes(bytes)
                    shareFileViaProvider(
                        outFile,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "Share $safeName"
                    )
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Excel share failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun pickTrackerAttachment(kind: String?) {
            val mode = kind?.lowercase()?.trim() ?: "gallery"
            runOnUiThread {
                when (mode) {
                    "camera" -> runCatching { captureCameraPhotoLauncher.launch(null) }.onFailure {
                        pickTrackerAttachmentLauncher.launch("image/*")
                    }
                    "pdf" -> pickTrackerAttachmentLauncher.launch("application/pdf")
                    else -> pickTrackerAttachmentLauncher.launch("image/*")
                }
            }
        }

        @JavascriptInterface
        fun triggerTrackerBudgetAlert(trackerName: String, thresholdPct: Int, spentText: String, budgetText: String) {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@runOnUiThread
                }
                val channelId = DailyReminderReceiver.CHANNEL_ID
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
                    val channel = NotificationChannel(
                        channelId,
                        "Tracmio daily reminder",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    nm.createNotificationChannel(channel)
                }
                val notification = NotificationCompat.Builder(this@MainActivity, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("⚠️ $trackerName: $thresholdPct% Budget Used")
                    .setContentText("Spent $spentText of $budgetText budget in $trackerName.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                runCatching {
                    NotificationManagerCompat.from(this@MainActivity)
                        .notify((trackerName.hashCode() xor thresholdPct) and 0x7FFFFFFF, notification)
                }
            }
        }

        @JavascriptInterface
        fun syncCustomTrackers(json: String) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val arr = JSONArray(json)
                    val dao = TracmioDatabase.getDatabase(this@MainActivity).customTrackerDao()
                    val trackers = mutableListOf<CustomTrackerEntity>()
                    val links = mutableListOf<TrackerTransactionCrossRefEntity>()
                    val people = mutableListOf<TrackerPersonEntity>()
                    val settlements = mutableListOf<TrackerSettlementEntity>()
                    val attachments = mutableListOf<TrackerAttachmentEntity>()
                    val customFields = mutableListOf<TrackerCustomFieldEntity>()
                    val customValues = mutableListOf<TrackerCustomValueEntity>()
                    val logs = mutableListOf<TrackerActivityLogEntity>()

                    for (i in 0 until arr.length()) {
                        val t = arr.optJSONObject(i) ?: continue
                        val tUid = t.optString("id", "").ifBlank { continue }
                        trackers += CustomTrackerEntity(
                            stableUid = tUid,
                            name = t.optString("name", "Tracker"),
                            trackerType = t.optString("type", "Custom"),
                            customTypeName = t.optString("customType", ""),
                            description = t.optString("description", ""),
                            icon = t.optString("icon", "🎯"),
                            themeColor = t.optString("color", "#00f5d4"),
                            startDate = t.optString("startDate", ""),
                            endDate = t.optString("endDate", ""),
                            budgetAmount = t.optDouble("budget", 0.0),
                            budgetMode = t.optString("budgetMode", "NONE"),
                            currency = t.optString("currency", "₹"),
                            notes = t.optString("notes", ""),
                            status = t.optString("status", "ACTIVE"),
                            isArchived = t.optBoolean("archived", false),
                            createdAt = t.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = t.optLong("updatedAt", System.currentTimeMillis())
                        )

                        val txLinks = t.optJSONArray("transactionLinks") ?: JSONArray()
                        for (j in 0 until txLinks.length()) {
                            val lk = txLinks.optJSONObject(j) ?: continue
                            val txId = lk.optString("transactionId", "").ifBlank { continue }
                            links += TrackerTransactionCrossRefEntity(
                                trackerUid = tUid,
                                transactionUid = txId,
                                isRefund = lk.optBoolean("isRefund", false),
                                paidByPersonUid = lk.optString("paidByPersonId", ""),
                                splitMode = lk.optString("splitMode", "EQUAL"),
                                splitSharesJson = lk.optJSONObject("shares")?.toString() ?: "{}",
                                linkedAt = lk.optLong("linkedAt", System.currentTimeMillis())
                            )
                        }

                        val pArr = t.optJSONArray("people") ?: JSONArray()
                        for (j in 0 until pArr.length()) {
                            val p = pArr.optJSONObject(j) ?: continue
                            val pUid = p.optString("id", "").ifBlank { continue }
                            people += TrackerPersonEntity(
                                personUid = "$tUid-$pUid",
                                trackerUid = tUid,
                                name = p.optString("name", "Member"),
                                phone = p.optString("phone", ""),
                                notes = p.optString("notes", ""),
                                avatarColor = p.optString("color", "#00f5d4")
                            )
                        }

                        val sArr = t.optJSONArray("settlements") ?: JSONArray()
                        for (j in 0 until sArr.length()) {
                            val s = sArr.optJSONObject(j) ?: continue
                            val sUid = s.optString("id", "").ifBlank { continue }
                            settlements += TrackerSettlementEntity(
                                settlementUid = sUid,
                                trackerUid = tUid,
                                fromPersonUid = s.optString("fromPersonId", ""),
                                toPersonUid = s.optString("toPersonId", ""),
                                amount = s.optDouble("amount", 0.0),
                                isFullSettlement = s.optBoolean("isFull", false),
                                date = s.optString("date", ""),
                                note = s.optString("note", ""),
                                timestamp = s.optLong("timestamp", System.currentTimeMillis())
                            )
                        }

                        val aArr = t.optJSONArray("attachments") ?: JSONArray()
                        for (j in 0 until aArr.length()) {
                            val a = aArr.optJSONObject(j) ?: continue
                            val aUid = a.optString("id", "").ifBlank { continue }
                            attachments += TrackerAttachmentEntity(
                                attachmentUid = aUid,
                                trackerUid = tUid,
                                transactionUid = a.optString("transactionId", ""),
                                fileName = a.optString("fileName", "receipt.jpg"),
                                mimeType = a.optString("mimeType", "image/jpeg"),
                                localPath = a.optString("localPath", ""),
                                contentUri = a.optString("contentUri", ""),
                                fileSizeBytes = a.optLong("fileSizeBytes", 0L),
                                addedAt = a.optLong("addedAt", System.currentTimeMillis())
                            )
                        }

                        val cfArr = t.optJSONArray("customFields") ?: JSONArray()
                        for (j in 0 until cfArr.length()) {
                            val cf = cfArr.optJSONObject(j) ?: continue
                            val fUid = cf.optString("id", "").ifBlank { continue }
                            customFields += TrackerCustomFieldEntity(
                                fieldUid = "$tUid-$fUid",
                                trackerUid = tUid,
                                fieldName = cf.optString("name", "Field"),
                                fieldType = cf.optString("type", "TEXT"),
                                optionsJson = cf.optJSONArray("options")?.toString() ?: "[]"
                            )
                            customValues += TrackerCustomValueEntity(
                                trackerUid = tUid,
                                fieldUid = "$tUid-$fUid",
                                fieldValue = cf.optString("value", "")
                            )
                        }

                        val logArr = t.optJSONArray("timeline") ?: JSONArray()
                        for (j in 0 until logArr.length()) {
                            val lg = logArr.optJSONObject(j) ?: continue
                            val lUid = lg.optString("id", "$tUid-log-$j")
                            logs += TrackerActivityLogEntity(
                                logUid = lUid,
                                trackerUid = tUid,
                                actionType = lg.optString("action", "UPDATE"),
                                description = lg.optString("text", ""),
                                timestamp = lg.optLong("ts", System.currentTimeMillis())
                            )
                        }
                    }

                    dao.replaceAllTrackersFromSnapshot(
                        trackers = trackers,
                        links = links,
                        people = people,
                        settlements = settlements,
                        attachments = attachments,
                        customFields = customFields,
                        customValues = customValues,
                        logs = logs
                    )
                }
            }
        }

        @JavascriptInterface
        fun printDocument(html: String, jobName: String?) {
            val safeJobName = jobName?.takeIf { it.isNotBlank() } ?: "Tracmio_Statement"
            runOnUiThread {
                val pWebView = WebView(this@MainActivity)
                printWebView = pWebView
                pWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                        val adapter = pWebView.createPrintDocumentAdapter(safeJobName)
                        printManager.print(safeJobName, adapter, PrintAttributes.Builder().build())
                    }
                }
                pWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }

        @JavascriptInterface
        fun chooseBackgroundImage() {
            runOnUiThread {
                chooseBackgroundImageLauncher.launch("image/*")
            }
        }

        @JavascriptInterface
        fun scheduleDailyReminder(enabled: Boolean, hour: Int, minute: Int, memeMode: Boolean) {
            runOnUiThread {
                TracmioReminderScheduler.update(this@MainActivity, enabled, hour, minute, memeMode)
            }
        }

        @JavascriptInterface
        fun requestNotificationPermission() {
            runOnUiThread {
                requestRuntimePermissions(includeNotifications = true, includeMedia = false)
            }
        }

        @JavascriptInterface
        fun requestMediaPermission() {
            runOnUiThread {
                requestRuntimePermissions(includeNotifications = false, includeMedia = true)
            }
        }

        @JavascriptInterface
        fun requestAllPermissions() {
            runOnUiThread {
                requestRuntimePermissions(includeNotifications = true, includeMedia = true)
            }
        }

        @JavascriptInterface
        fun getLegacyData(): String? {
            val prefs = getSharedPreferences("tracmio_legacy_bridge", Context.MODE_PRIVATE)
            return prefs.getString("legacy_state_json", null)
        }
    }
}
