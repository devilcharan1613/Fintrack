package com.charan.tracmio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.charan.tracmio.data.local.TracmioDatabase
import com.charan.tracmio.data.repository.FinanceRepository
import com.charan.tracmio.data.repository.NativeStatementPdf
import com.charan.tracmio.ui.nativeapp.NativeTracmioApp
import com.charan.tracmio.ui.nativeapp.NativeTracmioViewModel
import com.charan.tracmio.ui.nativeapp.TracmioPreferencesStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: NativeTracmioViewModel
    private lateinit var preferencesStore: TracmioPreferencesStore

    private var pendingExportJson: String? = null
    private var pendingWorkbookBytes: ByteArray? = null
    private var quickActionState by mutableStateOf<String?>(null)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.isNotEmpty() && results.values.all { it }) {
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

    private val createBackupDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val content = pendingExportJson
        pendingExportJson = null
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

    private val openBackupDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val jsonText = contentResolver.openInputStream(uri)
                    ?.bufferedReader(StandardCharsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
                if (jsonText.isNotBlank() && ::viewModel.isInitialized) {
                    viewModel.importBackupJson(jsonText)
                }
            }.onFailure {
                Toast.makeText(this, "Import failed: ${it.message}", Toast.LENGTH_SHORT).show()
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

    private val createPdfStatementLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && ::viewModel.isInitialized) {
            runCatching {
                val currentState = viewModel.uiState.value
                contentResolver.openOutputStream(uri)?.use { out ->
                    NativeStatementPdf.write(
                        outputStream = out,
                        accounts = currentState.accounts,
                        transactions = currentState.transactions,
                        periodLabel = "v1.00 Statement"
                    )
                }
                Toast.makeText(this, "PDF statement saved", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "PDF export failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TracmioDatabase.getDatabase(applicationContext)
        val repository = FinanceRepository(database)
        preferencesStore = TracmioPreferencesStore(applicationContext)
        viewModel = ViewModelProvider(
            this,
            NativeTracmioViewModel.factory(repository, preferencesStore)
        )[NativeTracmioViewModel::class.java]

        quickActionState = intent?.getStringExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION)

        setContent {
            NativeTracmioApp(
                viewModel = viewModel,
                initialQuickAction = quickActionState,
                onConsumeQuickAction = { quickActionState = null },
                onChooseBackgroundPhoto = { chooseBackgroundImageLauncher.launch("image/*") },
                onExportBackupJson = {
                    viewModel.exportBackupJson { json ->
                        pendingExportJson = json
                        createBackupDocumentLauncher.launch("tracmio_backup_v1.00.json")
                    }
                },
                onImportBackupJson = {
                    openBackupDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                onExportExcelWorkbook = {
                    viewModel.exportExcelWorkbook { bytes ->
                        pendingWorkbookBytes = bytes
                        createWorkbookDocumentLauncher.launch("Tracmio_Workbook_v1.00.xlsx")
                    }
                },
                onExportPdfStatement = {
                    createPdfStatementLauncher.launch("Tracmio_Statement_v1.00.pdf")
                },
                onScheduleReminder = { enabled, hour, minute, memeMode, alertCategory ->
                    TracmioReminderScheduler.update(
                        context = this,
                        enabled = enabled,
                        hour = hour,
                        minute = minute,
                        memeMode = memeMode,
                        alertCategory = alertCategory
                    )
                },
                onTriggerTestNotification = {
                    requestRuntimePermissions(includeNotifications = true, includeMedia = false)
                    DailyReminderReceiver.showReminderNotification(
                        this,
                        preferencesStore.state.value.reminderMemeMode
                    )
                }
            )
        }

        TracmioReminderScheduler.rescheduleFromPrefs(this)
        maybeRequestInitialPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.getStringExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION)
        if (!action.isNullOrBlank()) {
            quickActionState = action
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
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
            val destFile = File(filesDir, "tracmio_custom_bg.jpg")
            destFile.writeBytes(out.toByteArray())
            viewModel.setCustomBgImagePath(destFile.absolutePath)
            Toast.makeText(this, "Background photo applied", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Could not load photo: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
