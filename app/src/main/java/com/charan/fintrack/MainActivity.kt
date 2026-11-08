package com.charan.fintrack

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.ByteArrayInputStream
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var pageReady = false
    private var pendingNotificationType: String? = null
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val pendingBackupFile get() = java.io.File(cacheDir, "fintrack_pending_backup.json")
    private var pendingImportJson: String? = null
    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = runCatching { pendingBackupFile.takeIf { it.exists() }?.readBytes() }.getOrNull()
        if (uri != null && payload != null) {
            runCatching {
                contentResolver.openOutputStream(uri)?.use { it.write(payload) }
                    ?: error("Unable to open the selected backup file")
            }.onSuccess {
                if (::webView.isInitialized) webView.evaluateJavascript("showToast('Backup saved successfully','success');", null)
            }.onFailure {
                if (::webView.isInitialized) webView.evaluateJavascript("showToast('Backup export failed','error');", null)
            }
        } else if (uri != null && ::webView.isInitialized) {
            webView.evaluateJavascript("showToast('Backup export could not be resumed. Please try again.','error');", null)
        }
        pendingBackupFile.delete()
    }
    private val pendingReportFile get() = java.io.File(cacheDir, "fintrack_pending_report.bin")
    private val exportPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> savePendingReport(uri) }
    private val exportDocxLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri -> savePendingReport(uri) }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val payload = runCatching {
                contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Unable to read the selected backup file")
            }.getOrNull()
            if (payload != null) {
                pendingImportJson = payload
                deliverPendingImport()
            } else if (::webView.isInitialized && pageReady) {
                webView.evaluateJavascript("showToast('Backup import failed','error');", null)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        ReminderNotifications.createChannel(this)
        // One-time migration for reminder work created before schedule revisions existed.
        if (ReminderPrefs.config(this).optBoolean("enabled", false) && ReminderPrefs.scheduleRevision(this) == 0L) {
            ReminderScheduler.scheduleAll(this)
        }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.setSupportZoom(false)
            addJavascriptInterface(FinTrackBridge(this@MainActivity), "FinTrackNative")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    pageReady = true
                    ViewCompat.requestApplyInsets(view)
                    handleNotificationIntent(intent)
                    deliverPendingImport()
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return handleNavigation(request.url)
                }

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return handleNavigation(Uri.parse(url))
                }

                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    return if (isBundledAsset(request.url) || request.url.toString() == "about:blank") {
                        null
                    } else {
                        WebResourceResponse(
                            "text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0))
                        )
                    }
                }
            }
            webChromeClient = WebChromeClient()
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                val density = view.resources.displayMetrics.density.coerceAtLeast(1f)
                fun cssPx(px: Int) = "%.2f".format(java.util.Locale.US, px / density)
                evaluateJavascript(
                    "window.finTrackApplyInsets(${cssPx(safe.left)},${cssPx(safe.top)},${cssPx(safe.right)},${cssPx(safe.bottom)},${cssPx(ime.bottom)});",
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
                if (!pageReady || !::webView.isInitialized) {
                    (this@MainActivity as Activity).finish()
                    return
                }
                webView.evaluateJavascript(
                    "(function(){return window.handleAndroidBack && window.handleAndroidBack() ? 'handled' : 'none';})()"
                ) { result ->
                    if (result != "\"handled\"") {
                        if (webView.canGoBack()) webView.goBack()
                        else (this@MainActivity as Activity).finish()
                    }
                }
            }
        })
    }

    private fun isBundledAsset(uri: Uri): Boolean {
        val path = uri.path ?: return false
        return uri.scheme == "file" && uri.host.isNullOrEmpty() &&
            path.startsWith("/android_asset/") && path.split('/').none { it == ".." }
    }

    private fun handleNavigation(uri: Uri): Boolean {
        if (isBundledAsset(uri) || uri.toString() == "about:blank") return false
        if (uri.scheme == "https" || uri.scheme == "http" || uri.scheme == "mailto" || uri.scheme == "tel") {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }
        return true
    }

    fun exportTransactionReport(raw: String, format: String, filename: String) {
        Thread({
            val generated = runCatching {
                val report = JSONObject(raw)
                val bytes = when (format.lowercase()) {
                    "pdf" -> TransactionReportExporter.createPdf(report)
                    "docx" -> TransactionReportExporter.createDocx(report)
                    else -> error("Unsupported report format: $format")
                }
                // Keep the pending export on disk while the system save picker is open.
                // This survives Activity recreation and process recreation.
                pendingReportFile.writeBytes(bytes)
                pendingReportFile
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                generated.onSuccess {
                    try {
                        if (format.equals("pdf", ignoreCase = true)) {
                            exportPdfLauncher.launch(filename.ifBlank { "FinTrack_Statement.pdf" })
                        } else {
                            exportDocxLauncher.launch(filename.ifBlank { "FinTrack_Statement.docx" })
                        }
                    } catch (error: Exception) {
                        pendingReportFile.delete()
                        notifyReportExport("Could not open the save dialog: ${error.localizedMessage ?: "unknown error"}", false)
                    }
                }.onFailure { error ->
                    pendingReportFile.delete()
                    notifyReportExport("Report generation failed: ${error.localizedMessage ?: "unknown error"}", false)
                }
            }
        }, "FinTrack-report-export").start()
    }

    private fun savePendingReport(uri: Uri?) {
        if (uri == null) {
            pendingReportFile.delete()
            notifyReportExport("Report export cancelled. No file was saved.", false)
            return
        }
        runCatching {
            check(pendingReportFile.isFile) { "The generated report is no longer available. Please export again." }
            val output = contentResolver.openOutputStream(uri)
                ?: error("Android could not open the selected save location.")
            output.use { stream ->
                pendingReportFile.inputStream().use { input -> input.copyTo(stream) }
                stream.flush()
            }
            check(pendingReportFile.length() > 0L) { "The generated report is empty." }
        }.onSuccess {
            notifyReportExport("Transaction report saved successfully.", true)
        }.onFailure { error ->
            notifyReportExport("Report export failed: ${error.localizedMessage ?: "unknown file error"}", false)
        }
        pendingReportFile.delete()
    }

    private fun notifyReportExport(message: String, success: Boolean) {
        if (!::webView.isInitialized) return
        val js = "showToast(${JSONObject.quote(message)},'${if (success) "success" else "error"}');"
        webView.post { webView.evaluateJavascript(js, null) }
    }

    fun exportBackup(json: String, filename: String) {
        runCatching { pendingBackupFile.writeText(json, Charsets.UTF_8) }
            .onSuccess { exportBackupLauncher.launch(filename.ifBlank { "fintrack_backup.json" }) }
            .onFailure {
                if (::webView.isInitialized) webView.evaluateJavascript("showToast('Could not prepare the backup file','error');", null)
            }
    }

    fun importBackup() {
        importBackupLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
    }

    private fun deliverPendingImport() {
        val payload = pendingImportJson ?: return
        if (!pageReady || !::webView.isInitialized) return
        pendingImportJson = null
        webView.post {
            webView.evaluateJavascript("window.importBackupText(${JSONObject.quote(payload)});", null)
        }
    }

    fun requestReminderPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(source: Intent?) {
        val type = source?.getStringExtra("fintrack_open_transaction")
        if (!type.isNullOrBlank()) {
            pendingNotificationType = if (type == "income") "income" else "expense"
            source.removeExtra("fintrack_open_transaction")
        }
        if (!pageReady || !::webView.isInitialized) return
        val safeType = pendingNotificationType ?: return
        pendingNotificationType = null
        webView.post {
            webView.evaluateJavascript(
                "(function(){showPage('dashboard');openModal('txn');setTxnType('$safeType');})();",
                null
            )
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
