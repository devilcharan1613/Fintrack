package com.tracemeo.app.ui.nativeapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.tracemeo.app.DailyReminderReceiver
import com.tracemeo.app.TracmioReminderScheduler
import com.tracemeo.app.data.CsvParser
import com.tracemeo.app.data.XlsxExporter
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TransactionEntity
import com.tracemeo.app.data.security.DeviceSecurityCapability
import com.tracemeo.app.data.security.TracmioSecurityEngine
import com.tracemeo.app.data.services.NativeStatementPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class NativeHostCallbacksImpl(
    private val activity: FragmentActivity,
    private val viewModel: NativeTracmioViewModel,
    private val onLaunchCreateDoc: (mimeType: String, suggestedName: String, onUriReady: (Uri) -> Unit) -> Unit,
    private val onLaunchOpenDoc: (mimeTypes: Array<String>, onUriReady: (Uri) -> Unit) -> Unit,
    private val onLaunchPickVisualMedia: (onUriReady: (Uri) -> Unit) -> Unit,
    private val onLaunchTakePicture: (outputUri: Uri, onCaptured: (Boolean) -> Unit) -> Unit,
    private val onRequestPermission: (permission: String, onResult: (Boolean) -> Unit) -> Unit
) : NativeHostCallbacks {

    private fun saveUriImageLocally(uri: Uri, folderName: String, fileName: String): Triple<String, String, Long> {
        val dir = File(activity.filesDir, folderName).apply { mkdirs() }
        val outFile = File(dir, fileName)
        activity.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        val mime = activity.contentResolver.getType(uri) ?: "image/jpeg"
        return Triple(outFile.name, outFile.absolutePath, outFile.length())
    }

    override fun chooseWallpaperImage() {
        onLaunchPickVisualMedia { uri ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                val (_, path, _) = saveUriImageLocally(uri, "wallpapers", "custom_wallpaper.jpg")
                viewModel.updateSettings { it.copy(customWallpaperPath = path) }
                viewModel.postStatus("Custom Liquid Glass wallpaper applied")
            }
        }
    }

    override fun captureWallpaperImage() {
        val captureAction = {
            val dir = File(activity.filesDir, "wallpapers").apply { mkdirs() }
            val photoFile = File(dir, "custom_wallpaper_cam.jpg")
            val photoUri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", photoFile)
            onLaunchTakePicture(photoUri) { ok ->
                if (ok) {
                    viewModel.updateSettings { it.copy(customWallpaperPath = photoFile.absolutePath) }
                    viewModel.postStatus("Captured wallpaper applied")
                }
            }
        }
        if (hasCameraPermission()) {
            captureAction()
        } else {
            onRequestPermission(Manifest.permission.CAMERA) { granted ->
                if (granted) captureAction() else viewModel.postStatus("Camera permission required to capture wallpaper")
            }
        }
    }

    override fun pickTrackerAttachment(onPicked: (fileName: String, mimeType: String, localPath: String, sizeBytes: Long) -> Unit) {
        onLaunchPickVisualMedia { uri ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                val ts = System.currentTimeMillis()
                val (name, path, size) = saveUriImageLocally(uri, "tracker_attachments", "att_$ts.jpg")
                onPicked(name, "image/jpeg", path, size)
            }
        }
    }

    override fun captureReceiptPhoto(onCaptured: (fileName: String, mimeType: String, localPath: String, sizeBytes: Long) -> Unit) {
        val action = {
            val dir = File(activity.filesDir, "tracker_attachments").apply { mkdirs() }
            val photoFile = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", photoFile)
            onLaunchTakePicture(uri) { ok ->
                if (ok) {
                    onCaptured(photoFile.name, "image/jpeg", photoFile.absolutePath, photoFile.length())
                }
            }
        }
        if (hasCameraPermission()) {
            action()
        } else {
            onRequestPermission(Manifest.permission.CAMERA) { granted ->
                if (granted) action() else viewModel.postStatus("Camera permission denied")
            }
        }
    }

    override fun exportPlainJsonBackup(fileName: String) {
        val json = viewModel.buildBackupJson()
        onLaunchCreateDoc("application/json", fileName) { uri ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    activity.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }.onSuccess {
                    viewModel.postStatus("Exported plain JSON backup")
                }.onFailure {
                    viewModel.postStatus("Export failed: ${it.localizedMessage}")
                }
            }
        }
    }

    override fun exportEncryptedJsonBackup(fileName: String, password: CharArray) {
        val plain = viewModel.buildBackupJson()
        onLaunchCreateDoc("application/json", fileName) { uri ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val encrypted = TracmioSecurityEngine.encryptBackup(plain, password)
                    activity.contentResolver.openOutputStream(uri)?.use { it.write(encrypted.toByteArray(Charsets.UTF_8)) }
                }.onSuccess {
                    viewModel.postStatus("Exported AES-256-GCM encrypted backup")
                }.onFailure {
                    viewModel.postStatus("Encrypted export failed: ${it.localizedMessage}")
                }
            }
        }
    }

    override fun importJsonBackup(decryptPassword: CharArray?) {
        onLaunchOpenDoc(arrayOf("application/json", "*/*")) { uri ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                val text = runCatching {
                    activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (!text.isNullOrBlank()) {
                    viewModel.importBackupJsonText(text, decryptPassword)
                } else {
                    viewModel.postStatus("Selected backup file is empty")
                }
            }
        }
    }

    override fun exportWorkbookXlsx(fileName: String) {
        val st = viewModel.uiState.value
        onLaunchCreateDoc("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fileName) { uri ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val bytes = XlsxExporter.createWorkbookBytes(st.accounts, st.transactions)
                    activity.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                }.onSuccess {
                    viewModel.postStatus("Exported Excel (.xlsx) workbook")
                }.onFailure {
                    viewModel.postStatus("Excel export failed: ${it.localizedMessage}")
                }
            }
        }
    }

    override fun exportCsv(fileName: String) {
        val st = viewModel.uiState.value
        onLaunchCreateDoc("text/csv", fileName) { uri ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val csv = CsvParser.exportTransactionsToCsv(st.transactions)
                    activity.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }.onSuccess {
                    viewModel.postStatus("Exported CSV ledger")
                }.onFailure {
                    viewModel.postStatus("CSV export failed")
                }
            }
        }
    }

    override fun importCsv() {
        onLaunchOpenDoc(arrayOf("text/csv", "text/comma-separated-values", "*/*")) { uri ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                val text = runCatching {
                    activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (!text.isNullOrBlank()) {
                    viewModel.importCsvText(text)
                } else {
                    viewModel.postStatus("Selected CSV file is empty")
                }
            }
        }
    }

    override fun printReceipt(tx: TransactionEntity, accountName: String) {
        val curr = viewModel.settingsFlow.value.currencySymbol
        val acc = AccountEntity(id = tx.accountId, name = accountName, accountType = tx.paymentMethod)
        val bytes = NativeStatementPdf.buildStatementPdfBytes(listOf(acc), listOf(tx), curr)
        dispatchPdfPrint("Tracmio Receipt #${tx.id}", bytes)
    }

    override fun printStatement(accounts: List<AccountEntity>, transactions: List<TransactionEntity>) {
        val curr = viewModel.settingsFlow.value.currencySymbol
        val bytes = NativeStatementPdf.buildStatementPdfBytes(accounts, transactions, curr)
        dispatchPdfPrint("Tracmio Ledger Statement", bytes)
    }

    override fun printCustomReport(jobName: String, title: String, lines: List<Pair<String, Boolean>>) {
        val st = viewModel.uiState.value
        val curr = viewModel.settingsFlow.value.currencySymbol
        val bytes = NativeStatementPdf.buildStatementPdfBytes(st.accounts, st.transactions, curr)
        dispatchPdfPrint(jobName, bytes)
    }

    private fun dispatchPdfPrint(jobName: String, pdfBytes: ByteArray) {
        val pm = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                runCatching {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        out.write(pdfBytes)
                    }
                }.onSuccess {
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }.onFailure {
                    callback.onWriteFailed(it.localizedMessage)
                }
            }
        }
        pm.print(jobName, adapter, null)
    }

    override fun copyToClipboard(text: String) {
        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Tracmio", text))
        viewModel.postStatus("Copied to clipboard")
    }

    override fun readClipboardText(): String? {
        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        return cm.primaryClip?.getItemAt(0)?.text?.toString()
    }

    override fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onRequestPermission(Manifest.permission.POST_NOTIFICATIONS) { granted ->
                viewModel.postStatus(if (granted) "Offline reminder alerts enabled" else "Notification permission denied")
            }
        }
    }

    override fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    override fun getDeviceSecurityCapability(): DeviceSecurityCapability {
        val bm = BiometricManager.from(activity)
        val res = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        return when (res) {
            BiometricManager.BIOMETRIC_SUCCESS -> DeviceSecurityCapability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> DeviceSecurityCapability.UNAVAILABLE
            else -> DeviceSecurityCapability.NONE
        }
    }

    override fun authenticateBiometric(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onFailure("Biometric verification failed")
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Tracmio Vault")
            .setSubtitle("Verify biometric or device credential")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }

    override fun authenticateDeviceCredential(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        authenticateBiometric(onSuccess, onFailure)
    }

    override fun scheduleDailyReminder(enabled: Boolean, hour: Int, minute: Int, userName: String) {
        if (enabled) {
            TracmioReminderScheduler.scheduleDailyReminder(activity, hour, minute, userName)
        } else {
            TracmioReminderScheduler.cancelDailyReminder(activity)
        }
    }

    override fun dispatchTestReminder() {
        val intent = Intent(activity, DailyReminderReceiver::class.java).apply {
            putExtra("user_name", viewModel.settingsFlow.value.userName)
        }
        DailyReminderReceiver().onReceive(activity, intent)
    }

    override fun scheduleCalendarEventReminder(title: String, message: String, triggerAtMillis: Long, eventId: String) {
        TracmioReminderScheduler.scheduleEventReminder(activity, title, message, triggerAtMillis, eventId)
    }

    override fun scheduleMonthlyBackupReminder(enabled: Boolean, dayOfMonth: Int, hour: Int, minute: Int) {
        TracmioReminderScheduler.scheduleMonthlyBackupReminder(activity, enabled, dayOfMonth, hour, minute)
    }
}
