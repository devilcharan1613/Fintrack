package com.tracmio.app.ui.nativeapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tracmio.app.DailyReminderReceiver
import com.tracmio.app.TracmioReminderScheduler
import com.tracmio.app.data.AcademicEventItem
import com.tracmio.app.data.AccountAllocation
import com.tracmio.app.data.CashCountLine
import com.tracmio.app.data.CustomReminderItem
import com.tracmio.app.data.KhataEntryItem
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.NativeLedgerCalculator
import com.tracmio.app.data.PendingRefundItem
import com.tracmio.app.data.PersonLedgerSummary
import com.tracmio.app.data.PersonUpiCard
import com.tracmio.app.data.PreferencesAndThemeStore
import com.tracmio.app.data.RechargeTrackerItem
import com.tracmio.app.data.SubscriptionItem
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.BudgetEntity
import com.tracmio.app.data.local.CashbackEntryEntity
import com.tracmio.app.data.local.CategoryEntity
import com.tracmio.app.data.local.CustomTrackerEntity
import com.tracmio.app.data.local.GoalContributionEntity
import com.tracmio.app.data.local.GoalEntity
import com.tracmio.app.data.local.LoanEntity
import com.tracmio.app.data.local.LoanPaymentEntity
import com.tracmio.app.data.local.PersonEntity
import com.tracmio.app.data.local.QuickTemplateEntity
import com.tracmio.app.data.local.TrackerAttachmentEntity
import com.tracmio.app.data.local.TrackerPersonEntity
import com.tracmio.app.data.local.TrackerSettlementEntity
import com.tracmio.app.data.local.TrackerTransactionCrossRefEntity
import com.tracmio.app.data.local.TransactionEntity
import com.tracmio.app.data.local.ValueItemEntity
import com.tracmio.app.data.local.WishlistItemEntity
import com.tracmio.app.data.repository.FinanceRepository
import com.tracmio.app.data.security.AppLockManager
import com.tracmio.app.data.security.AppLockState
import com.tracmio.app.data.security.TracmioSecurityEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NativeTracmioUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val goalContributions: List<GoalContributionEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val loanPayments: List<LoanPaymentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val customTrackers: List<CustomTrackerEntity> = emptyList(),
    val trackerLinks: List<TrackerTransactionCrossRefEntity> = emptyList(),
    val trackerPeople: List<TrackerPersonEntity> = emptyList(),
    val trackerSettlements: List<TrackerSettlementEntity> = emptyList(),
    val trackerAttachments: List<TrackerAttachmentEntity> = emptyList(),
    val quickTemplates: List<QuickTemplateEntity> = emptyList(),
    val wishlistItems: List<WishlistItemEntity> = emptyList(),
    val cashbackEntries: List<CashbackEntryEntity> = emptyList(),
    val valueItems: List<ValueItemEntity> = emptyList(),
    val subscriptions: List<SubscriptionItem> = emptyList(),
    val academicEvents: List<AcademicEventItem> = emptyList(),
    val customReminders: List<CustomReminderItem> = emptyList(),
    val khataEntries: List<KhataEntryItem> = emptyList(),
    val rechargeTrackers: List<RechargeTrackerItem> = emptyList(),
    val pendingRefunds: List<PendingRefundItem> = emptyList(),
    val personUpiCards: Map<Long, PersonUpiCard> = emptyMap(),
    val appLockState: AppLockState = AppLockState(),
    val isAppLocked: Boolean = false,
    val pendingNotificationAction: String? = null,
    val statusMessage: String? = null,
    val recentlyDeletedTransaction: TransactionEntity? = null,
    val hasPendingUndo: Boolean = false
) {
    val totalBalance: Double
        get() = NativeLedgerCalculator.totalBalance(accounts, transactions)

    val totalBalancePaise: Long
        get() = NativeLedgerCalculator.totalBalancePaise(accounts, transactions)

    val totalIncome: Double
        get() = NativeLedgerCalculator.totalIncome(transactions)

    val totalExpense: Double
        get() = NativeLedgerCalculator.totalExpense(transactions)

    val personSummaries: List<PersonLedgerSummary>
        get() = NativeLedgerCalculator.personSummaries(persons, transactions)
}

class NativeTracmioViewModel(
    application: Application,
    val repository: FinanceRepository
) : AndroidViewModel(application) {

    private val prefsStore = PreferencesAndThemeStore.getInstance(application)
    private val appLockManager = AppLockManager(application)

    val settingsFlow: StateFlow<LiquidGlassSettings> = prefsStore.settings
    val subscriptionsFlow: StateFlow<List<SubscriptionItem>> = prefsStore.subscriptions
    val academicEventsFlow: StateFlow<List<AcademicEventItem>> = prefsStore.academicEvents
    val customRemindersFlow: StateFlow<List<CustomReminderItem>> = prefsStore.customReminders
    val khataEntriesFlow: StateFlow<List<KhataEntryItem>> = prefsStore.khataEntries
    val rechargeTrackersFlow: StateFlow<List<RechargeTrackerItem>> = prefsStore.rechargeTrackers
    val pendingRefundsFlow: StateFlow<List<PendingRefundItem>> = prefsStore.pendingRefunds
    val personUpiCardsFlow: StateFlow<Map<Long, PersonUpiCard>> = prefsStore.personUpiCards

    private val _appLockState = MutableStateFlow(appLockManager.getState())
    private val _isAppLocked = MutableStateFlow(appLockManager.getState().isEnabled)
    private val _pendingQuickAction = MutableStateFlow<String?>(null)
    private val statusMessage = MutableStateFlow<String?>(null)
    private val recentlyDeleted = MutableStateFlow<TransactionEntity?>(null)
    private val _hasPendingUndo = MutableStateFlow(false)
    private var pendingUndoAction: (suspend () -> Unit)? = null

    private fun emitWithUndo(message: String, undoBlock: suspend () -> Unit) {
        pendingUndoAction = undoBlock
        _hasPendingUndo.value = true
        statusMessage.value = message
    }

    fun performPendingUndo() {
        val customUndo = pendingUndoAction
        if (customUndo != null) {
            pendingUndoAction = null
            _hasPendingUndo.value = false
            viewModelScope.launch {
                runCatching { customUndo() }
                    .onSuccess { statusMessage.value = "Restored ✓" }
            }
            return
        }
        undoDeleteTransaction()
    }

    private data class CoreSlice(
        val accounts: List<AccountEntity>,
        val transactions: List<TransactionEntity>,
        val budgets: List<BudgetEntity>,
        val goals: List<GoalEntity>,
        val goalContributions: List<GoalContributionEntity>
    )

    private data class LedgerSlice(
        val loans: List<LoanEntity>,
        val loanPayments: List<LoanPaymentEntity>,
        val categories: List<CategoryEntity>,
        val persons: List<PersonEntity>,
        val customTrackers: List<CustomTrackerEntity>
    )

    private data class TrackerFeatureSlice(
        val trackerLinks: List<TrackerTransactionCrossRefEntity>,
        val trackerPeople: List<TrackerPersonEntity>,
        val trackerSettlements: List<TrackerSettlementEntity>,
        val trackerAttachments: List<TrackerAttachmentEntity>,
        val quickTemplates: List<QuickTemplateEntity>,
        val wishlist: List<WishlistItemEntity>
    )

    private data class MiscFeatureSlice(
        val cashback: List<CashbackEntryEntity>,
        val valueItems: List<ValueItemEntity>,
        val subscriptions: List<SubscriptionItem>,
        val academicEvents: List<AcademicEventItem>,
        val customReminders: List<CustomReminderItem>,
        val khataEntries: List<KhataEntryItem>,
        val rechargeTrackers: List<RechargeTrackerItem>,
        val pendingRefunds: List<PendingRefundItem>,
        val personUpiCards: Map<Long, PersonUpiCard>
    )

    private val coreFlow = combine(
        repository.accountList,
        repository.transactions,
        repository.budgets,
        repository.goals,
        repository.goalContributions
    ) { a, t, b, g, gc -> CoreSlice(a, t, b, g, gc) }

    private val ledgerFlow = combine(
        repository.loans,
        repository.loanPaymentsAll,
        repository.categories,
        repository.persons,
        repository.activeTrackers
    ) { l, lp, c, p, ct -> LedgerSlice(l, lp, c, p, ct) }

    private val trackerSubFlow = combine(
        repository.trackerLinks,
        repository.trackerPeople,
        repository.trackerSettlements,
        repository.trackerAttachments
    ) { tl, tp, ts, ta -> arrayOf<Any>(tl, tp, ts, ta) }

    private val trackerFeatureFlow = combine(
        trackerSubFlow,
        repository.quickTemplates,
        repository.wishlistItems
    ) { sub, qt, wl ->
        @Suppress("UNCHECKED_CAST")
        TrackerFeatureSlice(
            trackerLinks = sub[0] as List<TrackerTransactionCrossRefEntity>,
            trackerPeople = sub[1] as List<TrackerPersonEntity>,
            trackerSettlements = sub[2] as List<TrackerSettlementEntity>,
            trackerAttachments = sub[3] as List<TrackerAttachmentEntity>,
            quickTemplates = qt,
            wishlist = wl
        )
    }

    private val dailyUseSubFlow = combine(
        khataEntriesFlow,
        rechargeTrackersFlow,
        pendingRefundsFlow,
        personUpiCardsFlow
    ) { kh, rt, pr, puc -> arrayOf<Any>(kh, rt, pr, puc) }

    private val miscBaseFlow = combine(
        repository.cashbackEntries,
        repository.valueItems,
        subscriptionsFlow,
        academicEventsFlow,
        customRemindersFlow
    ) { cb, vi, subs, acad, rems -> arrayOf<Any>(cb, vi, subs, acad, rems) }

    private val miscFeatureFlow = combine(
        miscBaseFlow,
        dailyUseSubFlow
    ) { base, daily ->
        @Suppress("UNCHECKED_CAST")
        MiscFeatureSlice(
            cashback = base[0] as List<CashbackEntryEntity>,
            valueItems = base[1] as List<ValueItemEntity>,
            subscriptions = base[2] as List<SubscriptionItem>,
            academicEvents = base[3] as List<AcademicEventItem>,
            customReminders = base[4] as List<CustomReminderItem>,
            khataEntries = daily[0] as List<KhataEntryItem>,
            rechargeTrackers = daily[1] as List<RechargeTrackerItem>,
            pendingRefunds = daily[2] as List<PendingRefundItem>,
            personUpiCards = daily[3] as Map<Long, PersonUpiCard>
        )
    }

    private val securityAndStatusFlow = combine(
        _appLockState,
        _isAppLocked,
        _pendingQuickAction,
        statusMessage,
        combine(recentlyDeleted, _hasPendingUndo) { del, undo -> del to undo }
    ) { lockState, locked, quickAct, msg, delAndUndo ->
        arrayOf<Any?>(lockState, locked, quickAct, msg, delAndUndo.first, delAndUndo.second)
    }

    val uiState: StateFlow<NativeTracmioUiState> = combine(
        coreFlow,
        ledgerFlow,
        trackerFeatureFlow,
        miscFeatureFlow,
        securityAndStatusFlow
    ) { core, ledger, tf, mf, sec ->
        @Suppress("UNCHECKED_CAST")
        NativeTracmioUiState(
            accounts = core.accounts,
            transactions = core.transactions,
            budgets = core.budgets,
            goals = core.goals,
            goalContributions = core.goalContributions,
            loans = ledger.loans,
            loanPayments = ledger.loanPayments,
            categories = ledger.categories,
            persons = ledger.persons,
            customTrackers = ledger.customTrackers,
            trackerLinks = tf.trackerLinks,
            trackerPeople = tf.trackerPeople,
            trackerSettlements = tf.trackerSettlements,
            trackerAttachments = tf.trackerAttachments,
            quickTemplates = tf.quickTemplates,
            wishlistItems = tf.wishlist,
            cashbackEntries = mf.cashback,
            valueItems = mf.valueItems,
            subscriptions = mf.subscriptions,
            academicEvents = mf.academicEvents,
            customReminders = mf.customReminders,
            khataEntries = mf.khataEntries,
            rechargeTrackers = mf.rechargeTrackers,
            pendingRefunds = mf.pendingRefunds,
            personUpiCards = mf.personUpiCards,
            appLockState = sec[0] as AppLockState,
            isAppLocked = sec[1] as Boolean,
            pendingNotificationAction = sec[2] as String?,
            statusMessage = sec[3] as String?,
            recentlyDeletedTransaction = sec[4] as TransactionEntity?,
            hasPendingUndo = sec[5] as Boolean
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NativeTracmioUiState(
            appLockState = appLockManager.getState(),
            isAppLocked = appLockManager.getState().isEnabled
        )
    )

    init {
        viewModelScope.launch {
            repository.seedDefaultCategories()
            repository.ensureDefaultAccountIfEmpty()
            repository.seedDefaultQuickTemplatesIfEmpty()
            withContext(Dispatchers.IO) {
                repository.ensureSeededRoomSettingsAndReminders()
            }
        }
        viewModelScope.launch {
            coreFlow.collect { core ->
                val summary = NativeLedgerCalculator.computeTodayExpenseSummary(core.accounts, core.transactions)
                val line = DailyReminderReceiver.formatDailySpendSummaryLine(
                    todaySpentFormatted = MoneyPaise.formatCurrency(summary.todaySpent),
                    txCount = summary.todayTxCount,
                    safeRemainingFormatted = MoneyPaise.formatCurrency(summary.safeToSpendRemaining)
                )
                TracmioReminderScheduler.setDailySummaryLine(getApplication(), line)
            }
        }
    }

    fun setPendingNotificationAction(action: String?) {
        _pendingQuickAction.value = action
    }

    fun consumePendingNotificationAction() {
        _pendingQuickAction.value = null
    }

    // Privacy & Stealth
    fun togglePrivacyMask() {
        val current = settingsFlow.value.privacyMaskEnabled
        prefsStore.updateSettings { it.copy(privacyMaskEnabled = !current) }
        syncRoomSettingsAsync()
    }

    fun toggleStealthMode() {
        val next = !settingsFlow.value.stealthModeActive
        prefsStore.updateSettings { it.copy(stealthModeActive = next) }
        syncRoomSettingsAsync()
        statusMessage.value = if (next) "Stealth Mode active (decoy amounts shown)" else "Stealth Mode disabled"
    }

    // App Lock & Recovery
    fun unlockWithPin(pin: String): Boolean {
        val ok = appLockManager.verifyPin(pin)
        if (ok) _isAppLocked.value = false
        return ok
    }

    fun verifyPin(pin: String): Boolean = appLockManager.verifyPin(pin)

    fun unlockWithBiometricSuccess() {
        _isAppLocked.value = false
    }

    fun lockNow() {
        if (_appLockState.value.isEnabled) {
            _isAppLocked.value = true
        }
    }

    fun enableAppLock(
        pin: String,
        biometricEnabled: Boolean,
        securityQuestion: String,
        securityAnswer: String
    ): Result<String> = runCatching {
        val recoveryKey = appLockManager.enableLock(pin, biometricEnabled, securityQuestion, securityAnswer)
        _appLockState.value = appLockManager.getState()
        statusMessage.value = "App Lock enabled. Save your Recovery Key safely."
        recoveryKey
    }

    fun updateBiometricOptIn(enabled: Boolean) {
        appLockManager.setBiometricEnabled(enabled)
        _appLockState.value = appLockManager.getState()
    }

    fun recoverPinWithSecret(secretInput: String, newPin: String? = null): Boolean {
        val ok = runCatching { appLockManager.verifyRecoveryAndReset(secretInput, newPin) }.getOrDefault(false)
        if (ok) {
            _appLockState.value = appLockManager.getState()
            _isAppLocked.value = false
            statusMessage.value = if (newPin != null) "PIN updated & unlocked" else "App Lock recovered and unlocked"
        }
        return ok
    }

    fun disableAppLock() {
        appLockManager.disableLock()
        _appLockState.value = appLockManager.getState()
        _isAppLocked.value = false
        statusMessage.value = "App Lock disabled"
    }

    // Settings & UI Studio
    private fun syncRoomSettingsAsync() {
        val snapshot = prefsStore.settings.value
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.syncSettingsToRoom(snapshot) }
        }
    }

    fun updateSettings(transform: (LiquidGlassSettings) -> LiquidGlassSettings) {
        prefsStore.updateSettings(transform)
        syncRoomSettingsAsync()
    }

    fun completeOnboarding(userName: String, currencySymbol: String) {
        prefsStore.updateSettings {
            it.copy(
                userName = TracmioSecurityEngine.sanitizeInput(userName, 40),
                currencySymbol = currencySymbol,
                hasCompletedOnboarding = true
            )
        }
        syncRoomSettingsAsync()
        statusMessage.value = "Welcome to Tracmio${if (userName.isNotBlank()) ", ${userName.trim()}" else ""}!"
    }

    fun addCustomUpiApp(appName: String) {
        val clean = TracmioSecurityEngine.sanitizeInput(appName, 30)
        if (clean.isBlank()) return
        prefsStore.updateSettings { cur ->
            if (cur.customUpiApps.any { it.equals(clean, true) }) cur
            else cur.copy(customUpiApps = cur.customUpiApps + clean)
        }
        syncRoomSettingsAsync()
    }

    fun toggleToolInHub(toolId: String) {
        prefsStore.updateSettings { cur ->
            val currentList = cur.toolsHubIds.toMutableList()
            if (toolId in currentList) currentList.remove(toolId) else currentList.add(toolId)
            cur.copy(toolsHubIds = currentList)
        }
        syncRoomSettingsAsync()
    }

    // Accounts, Allocations, Reconcile & 3-Step Cash Count
    fun addAccount(
        name: String,
        type: String,
        balance: Double,
        currency: String = "INR",
        institution: String = "",
        accountNumber: String = "",
        notes: String = "",
        creditLimit: Double = 0.0,
        billingCycleDay: Int = 1,
        dueDay: Int = 15
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addAccount(
                    AccountEntity(
                        name = name.trim(),
                        accountType = type.trim().ifBlank { "Bank" },
                        balance = balance,
                        balancePaise = MoneyPaise.toPaise(balance),
                        currency = currency,
                        institution = institution.trim(),
                        accountNumber = accountNumber.trim(),
                        notes = notes.trim(),
                        creditLimit = creditLimit,
                        billingCycleDay = billingCycleDay.coerceIn(1, 28),
                        dueDay = dueDay.coerceIn(1, 28)
                    )
                )
            }.onSuccess {
                statusMessage.value = "Account saved"
            }.onFailure {
                statusMessage.value = it.message ?: "Failed to save account"
            }
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            runCatching { repository.updateAccount(account) }
                .onSuccess { statusMessage.value = "Account updated" }
                .onFailure { statusMessage.value = it.message ?: "Failed to update account" }
        }
    }

    fun archiveAccount(account: AccountEntity) {
        viewModelScope.launch {
            runCatching { repository.archiveAccount(account) }
                .onSuccess { statusMessage.value = "${account.name} archived" }
                .onFailure { statusMessage.value = it.message ?: "Archive failed" }
        }
    }

    fun updateAccountAllocations(account: AccountEntity, allocations: List<AccountAllocation>) {
        viewModelScope.launch {
            runCatching {
                val json = NativeLedgerCalculator.serializeAllocations(allocations)
                repository.updateAccount(account.copy(allocationsJson = json))
            }.onSuccess {
                statusMessage.value = "Allocations updated"
            }
        }
    }

    fun reconcileAccountBalance(account: AccountEntity, actualBalance: Double, note: String) {
        viewModelScope.launch {
            runCatching { repository.reconcileAccountBalance(account, actualBalance, note) }
                .onSuccess { statusMessage.value = "${account.name} reconciled" }
                .onFailure { statusMessage.value = it.message ?: "Reconcile failed" }
        }
    }

    fun applyCashDenominationCount(lines: List<CashCountLine>, note: String, cashAccount: AccountEntity?) {
        prefsStore.updateCashCountSession(lines, note)
        val total = NativeLedgerCalculator.computeCashDenominationTotal(lines)
        if (cashAccount != null) {
            reconcileAccountBalance(
                account = cashAccount,
                actualBalance = total,
                note = note.ifBlank { "Cash denomination count reconciliation" }
            )
        } else {
            statusMessage.value = "Cash count saved: ${MoneyPaise.formatCurrency(total)}"
        }
    }

    // Transactions & Quick Chips
    fun saveOrUpdateTransaction(
        id: Long?,
        accountId: Long,
        type: String,
        amount: Double,
        category: String,
        subcategory: String = "",
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        paymentMethod: String = "Cash",
        upiApp: String = "",
        warrantyDate: String = "",
        personId: Long? = null,
        transferToAccountId: Long? = null,
        allocationId: String = "",
        attachmentUri: String = "",
        trackerUid: String = "",
        isTrackerRefund: Boolean = false
    ) {
        viewModelScope.launch {
            runCatching {
                val normalizedType = type.uppercase()
                if (normalizedType == "TRANSFER" && (id == null || id <= 0L)) {
                    val toId = transferToAccountId ?: throw IllegalArgumentException("Select destination account")
                    repository.addTransfer(accountId, toId, amount, note, timestamp, paymentMethod, upiApp)
                    statusMessage.value = "Transfer completed"
                    return@runCatching
                }
                if (id != null && id > 0L) {
                    val existing = uiState.value.transactions.firstOrNull { it.id == id }
                    if (existing != null) {
                        repository.updateTransaction(
                            existing.copy(
                                accountId = accountId,
                                type = normalizedType,
                                amount = amount,
                                amountPaise = MoneyPaise.toPaise(amount),
                                category = category,
                                subcategory = subcategory,
                                note = note,
                                timestamp = timestamp,
                                paymentMethod = paymentMethod,
                                upiApp = upiApp,
                                warrantyDate = warrantyDate,
                                personId = personId,
                                allocationId = allocationId,
                                attachmentUri = attachmentUri
                            )
                        )
                        if (trackerUid.isNotBlank()) {
                            repository.linkTransactionToTracker(trackerUid, existing.stableUid, isRefund = isTrackerRefund)
                        }
                        statusMessage.value = "Transaction updated"
                    }
                } else {
                    val stableUid = UUID.randomUUID().toString()
                    repository.saveTransaction(
                        TransactionEntity(
                            stableUid = stableUid,
                            accountId = accountId,
                            type = normalizedType,
                            amount = amount,
                            amountPaise = MoneyPaise.toPaise(amount),
                            category = category,
                            subcategory = subcategory,
                            note = note,
                            timestamp = timestamp,
                            paymentMethod = paymentMethod,
                            upiApp = upiApp,
                            warrantyDate = warrantyDate,
                            personId = personId,
                            allocationId = allocationId,
                            attachmentUri = attachmentUri
                        )
                    )
                    if (trackerUid.isNotBlank()) {
                        repository.linkTransactionToTracker(trackerUid, stableUid, isRefund = isTrackerRefund)
                    }
                    statusMessage.value = "Transaction saved"
                }
            }.onFailure {
                statusMessage.value = it.message ?: "Failed to save transaction"
            }
        }
    }

    fun repeatTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            runCatching {
                repository.saveTransaction(
                    tx.copy(
                        id = 0L,
                        stableUid = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }.onSuccess {
                statusMessage.value = "Repeated ${tx.category} (${MoneyPaise.formatCurrency(tx.amount)})"
            }.onFailure {
                statusMessage.value = it.message ?: "Failed to repeat transaction"
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            recentlyDeleted.value = transaction
            emitWithUndo("Transaction deleted") {
                repository.saveTransaction(transaction.copy(id = 0L))
                recentlyDeleted.value = null
            }
        }
    }

    fun clearTransactionWarranty(transaction: TransactionEntity) {
        if (transaction.warrantyDate.isBlank()) return
        val oldDate = transaction.warrantyDate
        viewModelScope.launch {
            runCatching {
                repository.updateTransaction(transaction.copy(warrantyDate = ""))
            }.onSuccess {
                emitWithUndo("Warranty removed from ${transaction.category}") {
                    repository.updateTransaction(transaction.copy(warrantyDate = oldDate))
                }
            }
        }
    }

    fun undoDeleteTransaction() {
        val tx = recentlyDeleted.value ?: return
        viewModelScope.launch {
            repository.saveTransaction(tx.copy(id = 0L))
            recentlyDeleted.value = null
            _hasPendingUndo.value = false
            pendingUndoAction = null
            statusMessage.value = "Transaction restored"
        }
    }

    fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double, note: String = "") {
        viewModelScope.launch {
            runCatching {
                repository.addTransfer(fromAccountId, toAccountId, amount, note)
            }.onSuccess {
                statusMessage.value = "Transfer completed"
            }.onFailure {
                statusMessage.value = it.message ?: "Transfer failed"
            }
        }
    }

    fun addCategory(name: String, type: String, icon: String = "🏷️") {
        viewModelScope.launch {
            runCatching {
                repository.addCategory(CategoryEntity(name = name, type = type.uppercase(), icon = icon))
            }.onSuccess {
                statusMessage.value = "Category '$name' added"
            }.onFailure {
                statusMessage.value = it.message ?: "Failed to add category"
            }
        }
    }

    fun addQuickTemplate(icon: String, label: String, amount: Double, category: String) {
        viewModelScope.launch {
            runCatching {
                repository.addQuickTemplate(
                    QuickTemplateEntity(icon = icon.ifBlank { "⚡" }, label = label, amount = amount, category = category)
                )
            }.onSuccess { statusMessage.value = "Quick Spend chip added" }
        }
    }

    fun deleteQuickTemplate(item: QuickTemplateEntity) {
        viewModelScope.launch {
            repository.deleteQuickTemplate(item)
            emitWithUndo("Chip '${item.label}' removed") {
                repository.addQuickTemplate(item.copy(id = 0L))
            }
        }
    }

    fun triggerQuickTemplate(item: QuickTemplateEntity) {
        val targetAcc = uiState.value.accounts.firstOrNull { !it.isArchived } ?: return
        viewModelScope.launch {
            runCatching {
                val tx = TransactionEntity(
                    stableUid = UUID.randomUUID().toString(),
                    accountId = targetAcc.id,
                    type = "EXPENSE",
                    amount = item.amount,
                    amountPaise = MoneyPaise.toPaise(item.amount),
                    category = item.category,
                    note = item.label,
                    paymentMethod = targetAcc.accountType
                )
                val insertedId = repository.saveTransaction(tx)
                emitWithUndo("⚡ Logged ${MoneyPaise.formatCurrency(item.amount)} · ${item.label}") {
                    val saved = uiState.value.transactions.firstOrNull { it.id == insertedId } ?: tx.copy(id = insertedId)
                    repository.deleteTransaction(saved)
                }
            }.onFailure {
                statusMessage.value = it.message ?: "Failed to log quick chip"
            }
        }
    }

    // Budgets, Goals, Wishlist, Cashback, Value Items, Loans, People
    fun addBudget(category: String, limitAmount: Double, month: String) {
        viewModelScope.launch {
            runCatching {
                repository.addBudget(BudgetEntity(category = category, limitAmount = limitAmount, month = month))
            }.onSuccess { statusMessage.value = "Budget saved" }
        }
    }

    fun updateBudget(budget: BudgetEntity, category: String, limitAmount: Double, month: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateBudget(budget.copy(category = category.trim(), limitAmount = limitAmount, month = month.trim()))
            }.onSuccess { statusMessage.value = "Budget updated" }
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            emitWithUndo("Budget '${budget.category}' deleted") {
                repository.addBudget(budget.copy(id = 0L))
            }
        }
    }

    fun addGoal(name: String, targetAmount: Double, savedAmount: Double = 0.0, deadline: Long? = null) {
        viewModelScope.launch {
            runCatching {
                repository.addGoal(GoalEntity(name = name, targetAmount = targetAmount, savedAmount = savedAmount, deadline = deadline))
            }.onSuccess { statusMessage.value = "Goal created" }
        }
    }

    fun updateGoal(goal: GoalEntity, name: String, targetAmount: Double, deadline: Long? = goal.deadline) {
        viewModelScope.launch {
            runCatching {
                repository.updateGoal(goal.copy(name = name.trim(), targetAmount = targetAmount, deadline = deadline))
            }.onSuccess { statusMessage.value = "Goal updated" }
        }
    }

    fun addGoalContribution(goalId: Long, amount: Double, accountId: Long? = null, note: String = "") {
        viewModelScope.launch {
            runCatching { repository.addGoalContribution(goalId, amount, accountId, note) }
                .onSuccess { statusMessage.value = "Goal progress updated" }
                .onFailure { statusMessage.value = it.message ?: "Failed" }
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            emitWithUndo("Goal '${goal.name}' deleted") {
                repository.addGoal(goal.copy(id = 0L))
            }
        }
    }

    fun addWishlistItem(name: String, price: Double) {
        viewModelScope.launch {
            runCatching { repository.addWishlistItem(name, price) }
                .onSuccess { statusMessage.value = "Added to 24h cooling wishlist" }
        }
    }

    fun buyWishlistItem(item: WishlistItemEntity, accountId: Long, category: String = "Shopping") {
        viewModelScope.launch {
            runCatching {
                if (item.price > 0) {
                    repository.saveTransaction(
                        TransactionEntity(
                            accountId = accountId,
                            type = "EXPENSE",
                            amount = item.price,
                            category = category,
                            note = "Wishlist purchase: ${item.name}",
                            paymentMethod = "UPI"
                        )
                    )
                }
                repository.updateWishlistItem(item.copy(status = "BOUGHT"))
            }.onSuccess { statusMessage.value = "Purchased '${item.name}' and logged expense" }
                .onFailure { statusMessage.value = it.message ?: "Purchase failed" }
        }
    }

    fun updateWishlistStatus(item: WishlistItemEntity, status: String) {
        viewModelScope.launch {
            repository.updateWishlistItem(item.copy(status = status))
            if (status.equals("SKIPPED", ignoreCase = true)) {
                statusMessage.value = "🎉 Skipped '${item.name}' — You saved ${MoneyPaise.formatCurrency(item.price)}!"
            }
        }
    }

    fun deleteWishlistItem(item: WishlistItemEntity) {
        viewModelScope.launch {
            repository.deleteWishlistItem(item)
            emitWithUndo("Wishlist item '${item.name}' removed") {
                repository.addWishlistItem(item.name, item.price)
            }
        }
    }

    fun addCashbackEntry(source: String, amount: Double, note: String, creditToAccountId: Long? = null) {
        viewModelScope.launch {
            runCatching {
                repository.addCashbackEntry(source, amount, note)
                if (creditToAccountId != null && amount > 0.0) {
                    repository.saveTransaction(
                        TransactionEntity(
                            accountId = creditToAccountId,
                            type = "INCOME",
                            amount = amount,
                            amountPaise = MoneyPaise.toPaise(amount),
                            category = "Family & Income",
                            subcategory = "Cashback / Refund",
                            note = "Cashback reward: ${source.trim()} ${note.trim()}".trim(),
                            paymentMethod = "UPI"
                        )
                    )
                }
            }.onSuccess {
                statusMessage.value = if (creditToAccountId != null) {
                    "Cashback (${MoneyPaise.formatCurrency(amount)}) logged & credited to wallet ✓"
                } else {
                    "Cashback reward logged"
                }
            }
        }
    }

    fun deleteCashbackEntry(item: CashbackEntryEntity) {
        viewModelScope.launch {
            repository.deleteCashbackEntry(item)
            emitWithUndo("Cashback entry removed") {
                repository.addCashbackEntry(item.source, item.amount, item.note)
            }
        }
    }

    fun addValueItem(name: String, cost: Double, uses: Int = 1) {
        viewModelScope.launch {
            runCatching { repository.addValueItem(name, cost, uses) }
                .onSuccess { statusMessage.value = "Price-Per-Use item added" }
        }
    }

    fun updateValueItem(item: ValueItemEntity, name: String, cost: Double, uses: Int) {
        viewModelScope.launch {
            runCatching {
                repository.updateValueItem(
                    item.copy(
                        name = TracmioSecurityEngine.sanitizeInput(name, 80).ifBlank { item.name },
                        cost = cost.coerceAtLeast(0.01),
                        uses = uses.coerceAtLeast(1)
                    )
                )
            }.onSuccess { statusMessage.value = "Price-Per-Use item updated" }
        }
    }

    fun incrementValueItemUse(item: ValueItemEntity) {
        viewModelScope.launch { repository.updateValueItem(item.copy(uses = item.uses + 1)) }
    }

    fun deleteValueItem(item: ValueItemEntity) {
        viewModelScope.launch {
            repository.deleteValueItem(item)
            emitWithUndo("Removed '${item.name}'") {
                repository.addValueItem(item.name, item.cost, item.uses)
            }
        }
    }

    fun addLoan(name: String, principal: Double, monthlyPayment: Double, interestRate: Double, dueDay: Int = 5) {
        viewModelScope.launch {
            runCatching {
                repository.addLoan(
                    LoanEntity(
                        name = name,
                        principal = principal,
                        paidAmount = 0.0,
                        monthlyPayment = monthlyPayment,
                        interestRate = interestRate,
                        dueDay = dueDay.coerceIn(1, 28)
                    )
                )
            }.onSuccess { statusMessage.value = "Debt/Loan saved" }
        }
    }

    fun updateLoan(loan: LoanEntity, name: String, principal: Double, monthlyPayment: Double, interestRate: Double, dueDay: Int = loan.dueDay) {
        viewModelScope.launch {
            runCatching {
                repository.updateLoan(
                    loan.copy(
                        name = name.trim(),
                        principal = principal,
                        monthlyPayment = monthlyPayment,
                        interestRate = interestRate,
                        dueDay = dueDay.coerceIn(1, 28)
                    )
                )
            }.onSuccess { statusMessage.value = "Loan updated" }
        }
    }

    fun addLoanPayment(loanId: Long, amount: Double, accountId: Long? = null, note: String = "") {
        viewModelScope.launch {
            runCatching {
                repository.addLoanPayment(LoanPaymentEntity(loanId = loanId, accountId = accountId, amount = amount, note = note))
            }.onSuccess { statusMessage.value = "EMI payment recorded" }
                .onFailure { statusMessage.value = it.message ?: "Payment failed" }
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            emitWithUndo("Loan '${loan.name}' deleted") {
                repository.addLoan(loan.copy(id = 0L))
            }
        }
    }

    fun addPerson(name: String, phone: String, notes: String, upiId: String = "") {
        viewModelScope.launch {
            runCatching {
                val id = repository.addPerson(PersonEntity(name = name, phone = phone, notes = notes))
                if (upiId.isNotBlank() && id > 0L) {
                    prefsStore.upsertPersonUpiCard(id, upiId.trim(), "")
                }
            }
                .onSuccess { statusMessage.value = "Contact added" }
                .onFailure { statusMessage.value = it.message ?: "Failed to add contact" }
        }
    }

    fun updatePerson(person: PersonEntity, name: String, phone: String, notes: String) {
        viewModelScope.launch {
            runCatching {
                repository.updatePerson(person.copy(name = name.trim(), phone = phone.trim(), notes = notes.trim()))
            }.onSuccess { statusMessage.value = "Contact updated" }
        }
    }

    fun settlePersonBalance(personId: Long, accountId: Long, netAmount: Double, note: String = "Full settlement") {
        if (kotlin.math.abs(netAmount) < 0.01) return
        val settleType = if (netAmount > 0) "BORROW" else "LEND"
        saveOrUpdateTransaction(
            id = null,
            accountId = accountId,
            type = settleType,
            amount = kotlin.math.abs(netAmount),
            category = "Settlement",
            note = note,
            personId = personId
        )
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch { repository.deletePerson(person) }
    }

    fun logSplitShareForPerson(personId: Long, accountId: Long, amount: Double, title: String, youOweThem: Boolean) {
        saveOrUpdateTransaction(
            id = null,
            accountId = accountId,
            type = if (youOweThem) "BORROW" else "LEND",
            amount = amount,
            category = "Split Bill",
            note = title,
            personId = personId
        )
    }

    // Trackers, Subscriptions, Calendar, Reminders & Vault
    fun createCustomTracker(
        name: String,
        category: String,
        budget: Double,
        startDate: String,
        endDate: String,
        icon: String = "🎯"
    ) {
        viewModelScope.launch {
            runCatching {
                repository.upsertCustomTracker(
                    CustomTrackerEntity(
                        stableUid = UUID.randomUUID().toString(),
                        name = name.trim(),
                        trackerType = "CUSTOM",
                        customTypeName = category.trim(),
                        icon = icon.ifBlank { "🎯" },
                        budgetAmount = budget,
                        startDate = startDate,
                        endDate = endDate,
                        status = "ACTIVE"
                    )
                )
            }.onSuccess { statusMessage.value = "Custom tracker created" }
        }
    }

    fun updateCustomTracker(
        tracker: CustomTrackerEntity,
        name: String,
        category: String,
        budget: Double,
        startDate: String,
        endDate: String,
        notes: String
    ) {
        viewModelScope.launch {
            runCatching {
                repository.upsertCustomTracker(
                    tracker.copy(
                        name = name.trim(),
                        customTypeName = category.trim(),
                        budgetAmount = budget,
                        startDate = startDate,
                        endDate = endDate,
                        notes = notes
                    )
                )
            }.onSuccess { statusMessage.value = "Tracker updated" }
        }
    }

    fun archiveCustomTracker(trackerUid: String, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveCustomTracker(trackerUid, archived)
            statusMessage.value = if (archived) "Tracker archived" else "Tracker restored"
        }
    }

    fun deleteCustomTracker(trackerUid: String) {
        viewModelScope.launch {
            repository.deleteCustomTracker(trackerUid)
            statusMessage.value = "Tracker deleted"
        }
    }

    fun linkTransactionToTracker(trackerUid: String, transactionUid: String, isRefund: Boolean = false) {
        viewModelScope.launch {
            repository.linkTransactionToTracker(trackerUid, transactionUid, isRefund = isRefund)
            statusMessage.value = "Linked transaction to tracker"
        }
    }

    fun unlinkTransactionFromTracker(trackerUid: String, transactionUid: String) {
        viewModelScope.launch {
            repository.unlinkTransactionFromTracker(trackerUid, transactionUid)
            statusMessage.value = "Unlinked from tracker"
        }
    }

    fun upsertTrackerPerson(trackerUid: String, name: String, phone: String = "") {
        viewModelScope.launch {
            repository.upsertTrackerPerson(
                TrackerPersonEntity(
                    personUid = UUID.randomUUID().toString(),
                    trackerUid = trackerUid,
                    name = name.trim(),
                    phone = phone.trim()
                )
            )
            statusMessage.value = "Added member to tracker"
        }
    }

    fun deleteTrackerPerson(personUid: String) {
        viewModelScope.launch { repository.deleteTrackerPerson(personUid) }
    }

    fun addTrackerSettlement(trackerUid: String, fromUid: String, toUid: String, amount: Double, date: String, note: String) {
        viewModelScope.launch {
            repository.addTrackerSettlement(
                TrackerSettlementEntity(
                    settlementUid = UUID.randomUUID().toString(),
                    trackerUid = trackerUid,
                    fromPersonUid = fromUid,
                    toPersonUid = toUid,
                    amount = amount,
                    date = date,
                    note = note
                )
            )
            statusMessage.value = "Tracker settlement recorded"
        }
    }

    fun addTrackerAttachment(trackerUid: String, fileName: String, mimeType: String, localPath: String, sizeBytes: Long) {
        viewModelScope.launch {
            repository.addTrackerAttachment(
                TrackerAttachmentEntity(
                    attachmentUid = UUID.randomUUID().toString(),
                    trackerUid = trackerUid,
                    fileName = fileName,
                    mimeType = mimeType,
                    localPath = localPath,
                    fileSizeBytes = sizeBytes
                )
            )
            statusMessage.value = "Attachment saved to tracker"
        }
    }

    fun deleteTrackerAttachment(attachmentUid: String) {
        viewModelScope.launch {
            repository.deleteTrackerAttachment(attachmentUid)
            statusMessage.value = "Attachment removed"
        }
    }

    fun addSubscription(item: SubscriptionItem) {
        prefsStore.saveSubscriptions(prefsStore.subscriptions.value + item)
        statusMessage.value = "Subscription added"
    }

    fun updateSubscription(updated: SubscriptionItem) {
        prefsStore.saveSubscriptions(
            prefsStore.subscriptions.value.map { if (it.id == updated.id) updated else it }
        )
        statusMessage.value = "Subscription updated"
    }

    fun payAndLogSubscription(sub: SubscriptionItem, accountId: Long) {
        saveOrUpdateTransaction(
            id = null,
            accountId = accountId,
            type = "EXPENSE",
            amount = sub.amount,
            category = sub.category.ifBlank { "Bills" },
            subcategory = "Subscription",
            note = "Subscription payment: ${sub.name} (${sub.billingCycle})",
            paymentMethod = "UPI"
        )
    }

    fun deleteSubscription(id: String) {
        val target = prefsStore.subscriptions.value.firstOrNull { it.id == id }
        prefsStore.saveSubscriptions(prefsStore.subscriptions.value.filterNot { it.id == id })
        if (target != null) {
            emitWithUndo("Subscription '${target.name}' removed") {
                prefsStore.saveSubscriptions(prefsStore.subscriptions.value + target)
            }
        }
    }

    fun addAcademicEvent(item: AcademicEventItem) {
        prefsStore.saveAcademicEvents(prefsStore.academicEvents.value + item)
        statusMessage.value = "Calendar event saved"
    }

    fun toggleAcademicEventPaid(id: String) {
        prefsStore.saveAcademicEvents(
            prefsStore.academicEvents.value.map { if (it.id == id) it.copy(isPaid = !it.isPaid) else it }
        )
    }

    fun deleteAcademicEvent(id: String) {
        val target = prefsStore.academicEvents.value.firstOrNull { it.id == id }
        prefsStore.saveAcademicEvents(prefsStore.academicEvents.value.filterNot { it.id == id })
        if (target != null) {
            emitWithUndo("Deadline '${target.title}' removed") {
                prefsStore.saveAcademicEvents(prefsStore.academicEvents.value + target)
            }
        }
    }

    fun saveCustomReminders(list: List<CustomReminderItem>) {
        prefsStore.saveCustomReminders(list)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.syncCustomRemindersToRoom(list) }
        }
    }

    fun recordScheduleReminderInVault(
        stableUid: String,
        title: String,
        kind: String,
        hour: Int,
        minute: Int,
        dayOfMonth: Int = 1,
        triggerDate: String = "",
        isEnabled: Boolean = true,
        note: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.upsertReminderEntity(
                    com.tracmio.app.data.local.ReminderEntity(
                        stableUid = stableUid,
                        title = TracmioSecurityEngine.sanitizeInput(title, 80),
                        kind = kind,
                        hour = hour.coerceIn(0, 23),
                        minute = minute.coerceIn(0, 59),
                        dayOfMonth = dayOfMonth.coerceIn(1, 28),
                        triggerDate = triggerDate,
                        isEnabled = isEnabled,
                        note = TracmioSecurityEngine.sanitizeInput(note, 200)
                    )
                )
            }
        }
    }

    // Daily-Use Features: 1-Tap ATM Withdrawal, Split Cash + UPI, Khata, UPI Card, Recharge Tracker, Refunds
    fun performAtmWithdrawal(amount: Double, bankAccountId: Long? = null, cashAccountId: Long? = null) {
        viewModelScope.launch {
            runCatching {
                repository.performAtmWithdrawal(amount, bankAccountId, cashAccountId)
            }.onSuccess {
                statusMessage.value = "🏧 Withdrew ${MoneyPaise.formatCurrency(amount)} (Bank → Cash Wallet)"
            }.onFailure {
                statusMessage.value = it.message ?: "ATM withdrawal failed"
            }
        }
    }

    fun saveSplitCashUpiTransaction(
        cashAmount: Double,
        upiAmount: Double,
        category: String,
        subcategory: String = "",
        note: String = "",
        upiApp: String = "GPay",
        warrantyDate: String = "",
        attachmentUri: String = "",
        preferredUpiAccountId: Long? = null,
        preferredCashAccountId: Long? = null,
        personId: Long? = null
    ) {
        val activeAccounts = uiState.value.accounts.filter { !it.isArchived }
        val cashAcc = activeAccounts.firstOrNull { it.id == preferredCashAccountId }
            ?: activeAccounts.firstOrNull { it.accountType.contains("Cash", ignoreCase = true) || it.name.contains("Cash", ignoreCase = true) }
            ?: activeAccounts.firstOrNull()
        val upiAcc = activeAccounts.firstOrNull { it.id == preferredUpiAccountId && !it.accountType.contains("Cash", ignoreCase = true) }
            ?: activeAccounts.firstOrNull { !it.accountType.contains("Cash", ignoreCase = true) }
            ?: activeAccounts.firstOrNull { it.id == preferredUpiAccountId }
            ?: activeAccounts.firstOrNull()
        if (cashAcc == null || upiAcc == null) {
            statusMessage.value = "Please create a Cash wallet and Bank/UPI account first"
            return
        }
        saveSplitCashUpiTransaction(
            cashAccountId = cashAcc.id,
            upiAccountId = upiAcc.id,
            type = "EXPENSE",
            cashAmount = cashAmount,
            upiAmount = upiAmount,
            category = category,
            subcategory = subcategory,
            note = note,
            upiApp = upiApp,
            warrantyDate = warrantyDate,
            personId = personId,
            attachmentUri = attachmentUri
        )
    }

    fun saveSplitCashUpiTransaction(
        cashAccountId: Long,
        upiAccountId: Long,
        type: String,
        cashAmount: Double,
        upiAmount: Double,
        category: String,
        subcategory: String = "",
        note: String = "",
        upiApp: String = "",
        warrantyDate: String = "",
        personId: Long? = null,
        attachmentUri: String = ""
    ) {
        viewModelScope.launch {
            runCatching {
                repository.saveSplitCashUpiTransaction(
                    cashAccountId = cashAccountId,
                    upiAccountId = upiAccountId,
                    type = type,
                    cashAmount = cashAmount,
                    upiAmount = upiAmount,
                    category = category,
                    subcategory = subcategory,
                    note = note,
                    upiApp = upiApp,
                    warrantyDate = warrantyDate,
                    personId = personId,
                    attachmentUri = attachmentUri
                )
            }.onSuccess {
                val total = cashAmount + upiAmount
                statusMessage.value = "Split payment saved (${MoneyPaise.formatCurrency(total)}: Cash + UPI)"
            }.onFailure {
                statusMessage.value = it.message ?: "Failed to save split payment"
            }
        }
    }

    fun addKhataEntry(shopOrPersonName: String, itemTitle: String, amount: Double, category: String = "Khata & Kirana") {
        val cleanName = TracmioSecurityEngine.sanitizeInput(shopOrPersonName, 80)
        val cleanItem = TracmioSecurityEngine.sanitizeInput(itemTitle, 120).ifBlank { category.ifBlank { "Canteen / Kirana Credit" } }
        if (cleanName.isBlank() || amount <= 0.0) return
        val newEntry = KhataEntryItem(
            id = "khata_${UUID.randomUUID().toString().take(8)}",
            shopOrPersonName = cleanName,
            itemTitle = cleanItem,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            isSettled = false
        )
        prefsStore.saveKhataEntries(listOf(newEntry) + prefsStore.khataEntries.value)
        statusMessage.value = "Logged ${MoneyPaise.formatCurrency(amount)} in $cleanName Khata"
    }

    fun clearKhataForShopOrPerson(shopOrPersonName: String, accountId: Long? = null) {
        val all = prefsStore.khataEntries.value
        val pending = all.filter { !it.isSettled && it.shopOrPersonName.equals(shopOrPersonName, ignoreCase = true) }
        if (pending.isEmpty()) return
        val total = pending.sumOf { it.amount }
        val updated = all.map {
            if (!it.isSettled && it.shopOrPersonName.equals(shopOrPersonName, ignoreCase = true)) {
                it.copy(isSettled = true)
            } else it
        }
        prefsStore.saveKhataEntries(updated)
        val targetAccId = accountId ?: uiState.value.accounts.firstOrNull { !it.isArchived }?.id
        if (targetAccId != null && total > 0.0) {
            saveOrUpdateTransaction(
                id = null,
                accountId = targetAccId,
                type = "EXPENSE",
                amount = total,
                category = "Khata & Kirana",
                subcategory = "Kirana Store",
                note = "Cleared Khata: $shopOrPersonName (${pending.size} items)",
                paymentMethod = "UPI"
            )
        }
        statusMessage.value = "Cleared $shopOrPersonName Khata (${MoneyPaise.formatCurrency(total)})"
    }

    fun settleSingleKhataEntry(entry: KhataEntryItem, accountId: Long? = null) {
        settleSingleKhataEntry(entry.id, accountId)
    }

    fun settleSingleKhataEntry(id: String, accountId: Long? = null) {
        val all = prefsStore.khataEntries.value
        val target = all.firstOrNull { it.id == id } ?: return
        if (target.isSettled) return
        prefsStore.saveKhataEntries(all.map { if (it.id == id) it.copy(isSettled = true) else it })
        val targetAccId = accountId ?: uiState.value.accounts.firstOrNull { !it.isArchived }?.id
        if (targetAccId != null && target.amount > 0.0) {
            saveOrUpdateTransaction(
                id = null,
                accountId = targetAccId,
                type = "EXPENSE",
                amount = target.amount,
                category = "Khata & Kirana",
                subcategory = "Canteen",
                note = "Khata settled: ${target.shopOrPersonName} · ${target.itemTitle}",
                paymentMethod = "UPI"
            )
        }
        statusMessage.value = "Settled ${target.shopOrPersonName} (${MoneyPaise.formatCurrency(target.amount)})"
    }

    fun updateKhataEntry(id: String, shopOrPersonName: String, itemTitle: String, amount: Double) {
        val cleanName = TracmioSecurityEngine.sanitizeInput(shopOrPersonName, 80)
        val cleanItem = TracmioSecurityEngine.sanitizeInput(itemTitle, 120).ifBlank { "Canteen / Kirana Credit" }
        if (cleanName.isBlank() || amount <= 0.0) return
        val updated = prefsStore.khataEntries.value.map {
            if (it.id == id) it.copy(shopOrPersonName = cleanName, itemTitle = cleanItem, amount = amount) else it
        }
        prefsStore.saveKhataEntries(updated)
        statusMessage.value = "Khata entry updated"
    }

    fun deleteKhataEntry(id: String) {
        val removed = prefsStore.khataEntries.value.firstOrNull { it.id == id }
        prefsStore.saveKhataEntries(prefsStore.khataEntries.value.filterNot { it.id == id })
        if (removed != null) {
            emitWithUndo("Khata item '${removed.itemTitle}' removed") {
                prefsStore.saveKhataEntries(listOf(removed) + prefsStore.khataEntries.value)
            }
        }
    }

    fun savePersonUpiCard(personId: Long, upiId: String, qrImageUri: String = "") {
        prefsStore.upsertPersonUpiCard(personId, upiId, qrImageUri)
        statusMessage.value = "Saved UPI details"
    }

    fun addRechargeTracker(
        providerName: String,
        validityDays: Int,
        amount: Double,
        rechargeDate: String
    ) {
        addRechargeTracker(
            providerName = providerName,
            planLabel = "${validityDays}-Day Plan",
            amount = amount,
            rechargeDate = rechargeDate,
            validityDays = validityDays,
            logExpenseAccountId = null
        )
    }

    fun addRechargeTracker(
        providerName: String,
        planLabel: String,
        amount: Double,
        rechargeDate: String,
        validityDays: Int,
        logExpenseAccountId: Long? = null
    ) {
        val safeDays = validityDays.coerceIn(1, 365)
        val cleanDate = rechargeDate.trim().ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        val item = RechargeTrackerItem(
            id = "rech_${UUID.randomUUID().toString().take(8)}",
            providerName = TracmioSecurityEngine.sanitizeInput(providerName, 40).ifBlank { "Jio / WiFi" },
            planLabel = TracmioSecurityEngine.sanitizeInput(planLabel, 80).ifBlank { "${safeDays}-Day Plan" },
            amount = amount.coerceAtLeast(0.0),
            rechargeDate = cleanDate,
            validityDays = safeDays
        )
        prefsStore.saveRechargeTrackers(listOf(item) + prefsStore.rechargeTrackers.value)

        // Link with Academic Calendar / Deadlines automatically on expiry date
        runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startMs = fmt.parse(cleanDate)?.time ?: System.currentTimeMillis()
            val expiryDateStr = fmt.format(Date(startMs + safeDays.toLong() * 86_400_000L))
            val deadlineEvent = AcademicEventItem(
                id = "acad_rech_${item.id}",
                title = "📶 ${item.providerName} Recharge Expiry (${safeDays}d)",
                date = expiryDateStr,
                feeAmount = item.amount,
                isPaid = false,
                note = "Plan: ${item.planLabel} (recharged on $cleanDate)",
                type = "Recharge"
            )
            prefsStore.saveAcademicEvents(prefsStore.academicEvents.value + deadlineEvent)
        }

        if (logExpenseAccountId != null && item.amount > 0.0) {
            saveOrUpdateTransaction(
                id = null,
                accountId = logExpenseAccountId,
                type = "EXPENSE",
                amount = item.amount,
                category = "SIM, WiFi & Bills",
                subcategory = if (item.providerName.contains("WiFi", true) || item.providerName.contains("Broadband", true)) {
                    "Broadband / WiFi"
                } else {
                    "Mobile Recharge (28/56/84d)"
                },
                note = "${item.providerName} ${item.planLabel} (${safeDays}d validity)",
                paymentMethod = "UPI"
            )
        } else {
            val (daysLeft, _) = NativeLedgerCalculator.computeRechargeDaysLeft(cleanDate, safeDays)
            statusMessage.value = "${item.providerName} (${safeDays}d) saved · $daysLeft days left"
        }
    }

    fun updateRechargeTracker(
        id: String,
        providerName: String,
        validityDays: Int,
        amount: Double,
        rechargeDate: String
    ) {
        val safeDays = validityDays.coerceIn(1, 365)
        val cleanDate = rechargeDate.trim().ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
        val cleanOp = TracmioSecurityEngine.sanitizeInput(providerName, 40).ifBlank { "Jio / WiFi" }
        val updated = prefsStore.rechargeTrackers.value.map {
            if (it.id == id) {
                it.copy(
                    providerName = cleanOp,
                    planLabel = "${safeDays}-Day Plan",
                    amount = amount.coerceAtLeast(0.0),
                    rechargeDate = cleanDate,
                    validityDays = safeDays
                )
            } else it
        }
        prefsStore.saveRechargeTrackers(updated)
        runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startMs = fmt.parse(cleanDate)?.time ?: System.currentTimeMillis()
            val expiryDateStr = fmt.format(Date(startMs + safeDays.toLong() * 86_400_000L))
            val acadId = "acad_rech_$id"
            val existingAcad = prefsStore.academicEvents.value
            if (existingAcad.any { it.id == acadId }) {
                prefsStore.saveAcademicEvents(
                    existingAcad.map {
                        if (it.id == acadId) it.copy(
                            title = "📶 $cleanOp Recharge Expiry (${safeDays}d)",
                            date = expiryDateStr,
                            feeAmount = amount.coerceAtLeast(0.0)
                        ) else it
                    }
                )
            }
        }
        statusMessage.value = "Recharge plan updated"
    }

    fun renewRechargeTracker(item: RechargeTrackerItem, logExpenseAccountId: Long? = null) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = fmt.format(Date())
        val (daysLeft, currentExpiryStr) = NativeLedgerCalculator.computeRechargeDaysLeft(item.rechargeDate, item.validityDays)
        val newStartDateStr = if (daysLeft > 0) currentExpiryStr else todayStr
        updateRechargeTracker(
            id = item.id,
            providerName = item.providerName,
            validityDays = item.validityDays,
            amount = item.amount,
            rechargeDate = newStartDateStr
        )
        val targetAccId = logExpenseAccountId ?: uiState.value.accounts.firstOrNull { !it.isArchived && !it.accountType.contains("Cash", true) }?.id
            ?: uiState.value.accounts.firstOrNull { !it.isArchived }?.id
        if (targetAccId != null && item.amount > 0.0) {
            saveOrUpdateTransaction(
                id = null,
                accountId = targetAccId,
                type = "EXPENSE",
                amount = item.amount,
                category = "SIM, WiFi & Bills",
                subcategory = if (item.providerName.contains("WiFi", true) || item.providerName.contains("Broadband", true)) {
                    "Broadband / WiFi"
                } else {
                    "Mobile Recharge (28/56/84d)"
                },
                note = "Renewed ${item.providerName} (+${item.validityDays}d)",
                paymentMethod = "UPI"
            )
        }
        val (newDaysLeft, _) = NativeLedgerCalculator.computeRechargeDaysLeft(newStartDateStr, item.validityDays)
        statusMessage.value = "Renewed ${item.providerName} (+${item.validityDays}d) · $newDaysLeft days active"
    }

    fun deleteRechargeTracker(id: String) {
        val removed = prefsStore.rechargeTrackers.value.firstOrNull { it.id == id }
        prefsStore.saveRechargeTrackers(prefsStore.rechargeTrackers.value.filterNot { it.id == id })
        if (removed != null) {
            emitWithUndo("Recharge plan '${removed.providerName}' removed") {
                prefsStore.saveRechargeTrackers(listOf(removed) + prefsStore.rechargeTrackers.value)
            }
        }
    }

    fun addPendingRefund(
        title: String,
        expectedAmount: Double,
        sourceName: String,
        expectedByLabel: String
    ) {
        val combinedLabel = if (sourceName.isNotBlank() && !title.contains(sourceName, ignoreCase = true)) {
            "${title.trim()} (${sourceName.trim()})"
        } else {
            title.trim().ifBlank { sourceName.trim() }
        }
        addPendingRefund(combinedLabel, expectedAmount, expectedByLabel)
    }

    fun addPendingRefund(sourceName: String, expectedAmount: Double, expectedByLabel: String) {
        val cleanSource = TracmioSecurityEngine.sanitizeInput(sourceName, 100)
        if (cleanSource.isBlank() || expectedAmount <= 0.0) return
        val item = PendingRefundItem(
            id = "ref_${UUID.randomUUID().toString().take(8)}",
            sourceName = cleanSource,
            expectedAmount = expectedAmount,
            expectedByLabel = TracmioSecurityEngine.sanitizeInput(expectedByLabel, 60).ifBlank { "Expected soon" },
            isReceived = false
        )
        prefsStore.savePendingRefunds(listOf(item) + prefsStore.pendingRefunds.value)
        statusMessage.value = "Added pending refund: $cleanSource (${MoneyPaise.formatCurrency(expectedAmount)})"
    }

    fun updatePendingRefund(
        id: String,
        title: String,
        expectedAmount: Double,
        sourceName: String,
        expectedByLabel: String
    ) {
        val combinedLabel = if (sourceName.isNotBlank() && !title.contains(sourceName, ignoreCase = true)) {
            "${title.trim()} (${sourceName.trim()})"
        } else {
            title.trim().ifBlank { sourceName.trim() }
        }
        val cleanSource = TracmioSecurityEngine.sanitizeInput(combinedLabel, 100)
        if (cleanSource.isBlank() || expectedAmount <= 0.0) return
        val updated = prefsStore.pendingRefunds.value.map {
            if (it.id == id) {
                it.copy(
                    sourceName = cleanSource,
                    expectedAmount = expectedAmount,
                    expectedByLabel = TracmioSecurityEngine.sanitizeInput(expectedByLabel, 60).ifBlank { "Expected soon" }
                )
            } else it
        }
        prefsStore.savePendingRefunds(updated)
        statusMessage.value = "Pending refund updated"
    }

    fun markPendingRefundReceived(item: PendingRefundItem, creditToAccountId: Long? = null) {
        if (item.isReceived) return
        prefsStore.savePendingRefunds(
            prefsStore.pendingRefunds.value.map { if (it.id == item.id) it.copy(isReceived = true) else it }
        )
        val targetAccId = creditToAccountId
            ?: uiState.value.accounts.firstOrNull { !it.isArchived && !it.accountType.contains("Cash", ignoreCase = true) }?.id
            ?: uiState.value.accounts.firstOrNull { !it.isArchived }?.id
        val accName = uiState.value.accounts.firstOrNull { it.id == targetAccId }?.name ?: "Wallet"
        if (targetAccId != null && item.expectedAmount > 0.0) {
            saveOrUpdateTransaction(
                id = null,
                accountId = targetAccId,
                type = "INCOME",
                amount = item.expectedAmount,
                category = "Family & Income",
                subcategory = "Cashback / Refund",
                note = "Refund received: ${item.sourceName}",
                paymentMethod = "UPI"
            )
        }
        statusMessage.value = "Refund received ✓ Credited ${MoneyPaise.formatCurrency(item.expectedAmount)} to $accName"
    }

    fun deletePendingRefund(id: String) {
        val removed = prefsStore.pendingRefunds.value.firstOrNull { it.id == id }
        prefsStore.savePendingRefunds(prefsStore.pendingRefunds.value.filterNot { it.id == id })
        if (removed != null) {
            emitWithUndo("Removed refund '${removed.sourceName}'") {
                prefsStore.savePendingRefunds(listOf(removed) + prefsStore.pendingRefunds.value)
            }
        }
    }

    suspend fun exportJsonString(): String = withContext(Dispatchers.IO) { repository.exportJson() }

    fun restoreJsonFromText(json: String) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.restoreJson(json) } }
                .onSuccess { statusMessage.value = "Backup restored cleanly" }
                .onFailure { statusMessage.value = it.message ?: "Restore failed" }
        }
    }

    suspend fun exportCsvString(): String = withContext(Dispatchers.IO) { repository.exportCsv() }

    fun importCsvFromText(csv: String) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.importCsv(csv) } }
                .onSuccess { count -> statusMessage.value = "Imported $count CSV transactions" }
                .onFailure { statusMessage.value = it.message ?: "CSV import failed" }
        }
    }

    suspend fun checkDatabaseHealth(): TracmioSecurityEngine.DatabaseHealthReport =
        withContext(Dispatchers.IO) { TracmioSecurityEngine.checkDatabaseIntegrity(repository.database) }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            statusMessage.value = "All ledger data reset"
        }
    }

    fun showStatus(msg: String) {
        statusMessage.value = msg
    }

    fun clearStatusMessage() {
        statusMessage.value = null
        _hasPendingUndo.value = false
        pendingUndoAction = null
    }

    companion object {
        fun factory(
            application: Application,
            repository: FinanceRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NativeTracmioViewModel(application, repository) as T
                }
            }
    }
}
