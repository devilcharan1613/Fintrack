package com.charan.fintrack

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline Android host for the bundled FinTrack finance interface.
 * The UI is loaded from assets and persists locally in WebView storage.
 * On first launch it reads the previous fintrack.db without changing or deleting it.
 */
class MainActivity : Activity() {
    companion object {
        private const val CREATE_BACKUP = 4101
        private const val OPEN_BACKUP = 4102
        private const val LEGACY_DB = "fintrack.db"
    }

    private lateinit var webView: WebView
    private var pendingBackup: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 17, 25)
        window.navigationBarColor = Color.rgb(15, 23, 32)
        window.decorView.systemUiVisibility = 0

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(13, 17, 23))
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                defaultTextEncodingName = "UTF-8"
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return !request?.url?.toString().orEmpty().startsWith("file:///android_asset/")
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(view: WebView, url: String, message: String, result: android.webkit.JsResult): Boolean {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(message)
                        .setPositiveButton("OK") { _, _ -> result.confirm() }
                        .setOnCancelListener { result.cancel() }
                        .show()
                    return true
                }

                override fun onJsConfirm(view: WebView, url: String, message: String, result: android.webkit.JsResult): Boolean {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(message)
                        .setPositiveButton("Continue") { _, _ -> result.confirm() }
                        .setNegativeButton("Cancel") { _, _ -> result.cancel() }
                        .setOnCancelListener { result.cancel() }
                        .show()
                    return true
                }

                override fun onJsPrompt(
                    view: WebView,
                    url: String,
                    message: String,
                    defaultValue: String?,
                    result: android.webkit.JsPromptResult
                ): Boolean {
                    val field = EditText(this@MainActivity).apply { setText(defaultValue.orEmpty()) }
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(message)
                        .setView(field)
                        .setPositiveButton("OK") { _, _ -> result.confirm(field.text.toString()) }
                        .setNegativeButton("Cancel") { _, _ -> result.cancel() }
                        .setOnCancelListener { result.cancel() }
                        .show()
                    return true
                }
            }
            addJavascriptInterface(FinTrackBridge(), "FinTrackBridge")
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    private inner class FinTrackBridge {
        @JavascriptInterface
        fun getLegacyData(): String = try {
            readLegacyDatabase()
        } catch (_: Exception) {
            emptyState().toString()
        }

        @JavascriptInterface
        fun exportBackup(json: String, filename: String?) {
            runOnUiThread {
                pendingBackup = json
                val safeName = filename?.takeIf { it.isNotBlank() }
                    ?.replace(Regex("[^A-Za-z0-9._-]"), "_") ?: "fintrack_backup.json"
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, safeName)
                }
                startActivityForResult(intent, CREATE_BACKUP)
            }
        }

        @JavascriptInterface
        fun importBackup() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivityForResult(intent, OPEN_BACKUP)
            }
        }
    }

    private fun emptyState() = JSONObject().apply {
        put("accounts", JSONArray())
        put("transactions", JSONArray())
        put("persons", JSONArray())
        put("budgets", JSONArray())
        put("goals", JSONArray())
        put("loans", JSONArray())
        put("categories", JSONArray())
        put("settings", JSONObject().put("currency", "₹").put("name", "FinTrack").put("version", "4.0"))
    }

    /** Read old SQLite records only; the legacy database file is never modified. */
    private fun readLegacyDatabase(): String {
        val file = getDatabasePath(LEGACY_DB)
        if (!file.exists() || file.length() == 0L) return emptyState().toString()
        val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val result = emptyState()
            val accounts = JSONArray()
            val accountById = linkedMapOf<Long, JSONObject>()
            if (hasTable(db, "accounts")) {
                db.rawQuery("SELECT * FROM accounts ORDER BY id ASC", null).use { c ->
                    while (c.moveToNext()) {
                        val id = c.longValue("id")
                        val item = JSONObject().apply {
                            put("id", id.toString())
                            put("name", c.stringValue("name", "Account"))
                            put("type", c.stringValue("type", "wallet").lowercase(Locale.ROOT))
                            put("balance", c.doubleValue("opening", c.doubleValue("balance", 0.0)))
                            put("creditLimit", c.doubleValue("creditLimit"))
                            put("createdAt", isoDateTime(c.longValue("createdAt", System.currentTimeMillis())))
                        }
                        accounts.put(item)
                        accountById[id] = item
                    }
                }
            }

            val transactions = JSONArray()
            if (hasTable(db, "transactions")) {
                db.rawQuery("SELECT * FROM transactions ORDER BY id ASC", null).use { c ->
                    while (c.moveToNext()) {
                        val type = c.stringValue("type", "expense").lowercase(Locale.ROOT)
                        val amount = c.doubleValue("amount")
                        val fromId = c.longValue("accountId")
                        val toId = c.longValue("toAccountId")
                        transactions.put(JSONObject().apply {
                            put("id", c.longValue("id").toString())
                            put("type", type)
                            put("amount", amount)
                            put("accountId", fromId.toString())
                            put("toAccountId", if (toId == 0L) "" else toId.toString())
                            put("category", c.stringValue("category", "other"))
                            put("subcategory", c.stringValue("subcategory"))
                            put("personId", c.stringValue("personId"))
                            put("notes", c.stringValue("note", c.stringValue("notes")))
                            put("date", isoDate(c.longValue("date", System.currentTimeMillis())))
                            put("createdAt", isoDateTime(c.longValue("createdAt", System.currentTimeMillis())))
                        })
                        when (type) {
                            "income", "borrow" -> changeBalance(accountById[fromId], amount)
                            "expense", "lend" -> changeBalance(accountById[fromId], -amount)
                            "transfer" -> {
                                changeBalance(accountById[fromId], -amount)
                                changeBalance(accountById[toId], amount)
                            }
                        }
                    }
                }
            }

            val persons = JSONArray()
            if (hasTable(db, "persons")) {
                db.rawQuery("SELECT * FROM persons ORDER BY id ASC", null).use { c ->
                    while (c.moveToNext()) persons.put(JSONObject().apply {
                        put("id", c.stringValue("id"))
                        put("name", c.stringValue("name", "Person"))
                        put("phone", c.stringValue("phone"))
                        put("note", c.stringValue("note"))
                        put("createdAt", isoDateTime(c.longValue("createdAt", System.currentTimeMillis())))
                    })
                }
            }

            val budgets = JSONArray()
            if (hasTable(db, "budgets")) {
                db.rawQuery("SELECT * FROM budgets ORDER BY id ASC", null).use { c ->
                    while (c.moveToNext()) budgets.put(JSONObject().apply {
                        val category = c.stringValue("category", "Budget")
                        put("id", c.longValue("id").toString())
                        put("name", category)
                        put("category", category)
                        put("amount", c.doubleValue("amount"))
                        put("spent", c.doubleValue("spent"))
                        put("period", c.stringValue("month", "Monthly"))
                    })
                }
            }

            val goals = JSONArray()
            if (hasTable(db, "goals")) {
                db.rawQuery("SELECT * FROM goals ORDER BY id ASC", null).use { c ->
                    while (c.moveToNext()) goals.put(JSONObject().apply {
                        put("id", c.longValue("id").toString())
                        put("name", c.stringValue("name", "Savings goal"))
                        put("target", c.doubleValue("target"))
                        put("saved", c.doubleValue("saved"))
                    })
                }
            }

            val loans = JSONArray()
            if (hasTable(db, "loans")) {
                db.rawQuery("SELECT * FROM loans ORDER BY id ASC", null).use { c ->
                    while (c.moveToNext()) loans.put(JSONObject().apply {
                        put("id", c.longValue("id").toString())
                        put("name", c.stringValue("name", "Loan"))
                        put("principal", c.doubleValue("principal"))
                        put("emi", c.doubleValue("emi"))
                        put("paid", c.doubleValue("paid"))
                    })
                }
            }

            result.put("accounts", accounts)
            result.put("transactions", transactions)
            result.put("persons", persons)
            result.put("budgets", budgets)
            result.put("goals", goals)
            result.put("loans", loans)
            return result.toString()
        } finally {
            db.close()
        }
    }

    private fun changeBalance(account: JSONObject?, delta: Double) {
        if (account != null) account.put("balance", account.optDouble("balance") + delta)
    }

    private fun hasTable(db: SQLiteDatabase, name: String): Boolean =
        db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(name)).use { it.moveToFirst() }

    private fun Cursor.stringValue(name: String, fallback: String = ""): String {
        val index = getColumnIndex(name)
        return if (index >= 0 && !isNull(index)) getString(index).orEmpty() else fallback
    }

    private fun Cursor.longValue(name: String, fallback: Long = 0L): Long {
        val index = getColumnIndex(name)
        return if (index >= 0 && !isNull(index)) getLong(index) else fallback
    }

    private fun Cursor.doubleValue(name: String, fallback: Double = 0.0): Double {
        val index = getColumnIndex(name)
        return if (index >= 0 && !isNull(index)) getDouble(index) else fallback
    }

    private fun isoDate(value: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(value))
    private fun isoDateTime(value: Long) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(value))

    @Deprecated("Deprecated in Android API; retained for compatibility with minimum SDK")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_BACKUP) {
            val json = pendingBackup
            val uri = data?.data
            if (resultCode == RESULT_OK && uri != null && json != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(StandardCharsets.UTF_8)) }
                        ?: throw IllegalStateException("Could not open backup destination")
                    Toast.makeText(this, "FinTrack backup saved", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "Could not save backup", Toast.LENGTH_LONG).show()
                }
            }
            pendingBackup = null
        } else if (requestCode == OPEN_BACKUP && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                try {
                    val json = contentResolver.openInputStream(uri)?.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.toString("UTF-8")
                    } ?: throw IllegalStateException("Could not read backup")
                    webView.evaluateJavascript(
                        "window.__importBackupFromAndroid && window.__importBackupFromAndroid(${JSONObject.quote(json)})",
                        null
                    )
                } catch (_: Exception) {
                    Toast.makeText(this, "Could not read backup file", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Android API; retained for compatibility with minimum SDK")
    override fun onBackPressed() {
        if (!::webView.isInitialized) {
            super.onBackPressed()
            return
        }
        webView.evaluateJavascript("window.handleAndroidBack ? window.handleAndroidBack() : false") { handled ->
            if (handled != "true") finish()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("FinTrackBridge")
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
        super.onDestroy()
    }
}
