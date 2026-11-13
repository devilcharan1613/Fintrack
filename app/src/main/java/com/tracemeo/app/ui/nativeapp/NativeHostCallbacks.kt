package com.tracemeo.app.ui.nativeapp

import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TransactionEntity
import com.tracemeo.app.data.security.DeviceSecurityCapability

interface NativeHostCallbacks {
    fun chooseWallpaperImage()
    fun captureWallpaperImage()
    fun pickTrackerAttachment(onPicked: (fileName: String, mimeType: String, localPath: String, sizeBytes: Long) -> Unit)
    fun captureReceiptPhoto(onCaptured: (fileName: String, mimeType: String, localPath: String, sizeBytes: Long) -> Unit)
    fun exportPlainJsonBackup(fileName: String)
    fun exportEncryptedJsonBackup(fileName: String, password: CharArray)
    fun importJsonBackup(decryptPassword: CharArray?)
    fun exportWorkbookXlsx(fileName: String)
    fun exportCsv(fileName: String)
    fun importCsv()
    fun printReceipt(tx: TransactionEntity, accountName: String)
    fun printStatement(accounts: List<AccountEntity>, transactions: List<TransactionEntity>)
    fun printCustomReport(jobName: String, title: String, lines: List<Pair<String, Boolean>>)
    fun copyToClipboard(text: String)
    fun readClipboardText(): String?
    fun requestNotificationPermission()
    fun hasNotificationPermission(): Boolean
    fun hasCameraPermission(): Boolean
    fun getDeviceSecurityCapability(): DeviceSecurityCapability
    fun authenticateBiometric(onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun authenticateDeviceCredential(onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun scheduleDailyReminder(enabled: Boolean, hour: Int, minute: Int, userName: String)
    fun dispatchTestReminder()
    fun scheduleCalendarEventReminder(title: String, message: String, triggerAtMillis: Long, eventId: String)
    fun scheduleMonthlyBackupReminder(enabled: Boolean, dayOfMonth: Int, hour: Int, minute: Int)
}
