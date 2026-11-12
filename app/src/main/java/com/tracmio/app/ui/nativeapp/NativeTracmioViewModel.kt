package com.tracmio.app.ui.nativeapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tracmio.app.data.CustomTrackerCalculator
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.NativeLedgerCalculator
import com.tracmio.app.data.PersonLedgerSummary
import com.tracmio.app.data.PreferencesAndThemeStore
import com.tracmio.app.data.SubscriptionItem
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.BudgetEntity
import com.tracmio.app.data.local.CategoryEntity
import com.tracmio.app.data.local.CustomTrackerEntity
import com.tracmio.app.data.local.GoalContributionEntity
import com.tracmio.app.data.local.GoalEntity
import com.tracmio.app.data.local.LoanEntity
import com.tracmio.app.data.local.LoanPaymentEntity
import com.tracmio.app.data.local.PersonEntity
import com.tracmio.app.data.local.TransactionEntity
import com.tracmio.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val subscriptions: List<SubscriptionItem> = emptyList(),
    val statusMessage: String? = null,
    val recentlyDeletedTransaction: TransactionEntity? = null
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
    private val repository: FinanceRepository
) : AndroidViewModel(application) {

    private val prefsStore = PreferencesAndThemeStore.getInstance(application)
    val settingsFlow: StateFlow<LiquidGlassSettings> = prefsStore.settings
    val subscriptionsFlow: StateFlow<List<SubscriptionItem>> = prefsStore.subscriptions

    private val statusMessage = MutableStateFlow<String?>(null)
    private val recentlyDeleted = MutableStateFlow<TransactionEntity?>(null)

    private val primaryFlow = combine(
        repository.accountList,
        repository.transactions,
        repository.budgets,
        repository.goals,
        repository.activeTrackers
    ) { accounts, transactions, budgets, goals, trackers ->
        NativeTracmioUiState(
            accounts = accounts,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            customTrackers = trackers
        )
    }

    private val secondaryFlow = combine(
        repository.loans,
        repository.loanPaymentsAll,
        repository.categories,
        repository.persons,
        subscriptionsFlow
    ) { loans, loanPayments, categories, persons, subs ->
        NativeTracmioUiState(
            loans = loans,
            loanPayments = loanPayments,
            categories = categories,
            persons = persons,
            subscriptions = subs
        )
    }

    val uiState: StateFlow<NativeTracmioUiState> = combine(
        primaryFlow,
        secondaryFlow,
        statusMessage,
        recentlyDeleted
    ) { p, s, msg, deleted ->
        p.copy(
            loans = s.loans,
            loanPayments = s.loanPayments,
            categories = s.categories,
            persons = s.persons,
            subscriptions = s.subscriptions,
            statusMessage = msg,
            recentlyDeletedTransaction = deleted
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NativeTracmioUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedDefaultCategories()
            repository.ensureDefaultAccountIfEmpty()
        }
    }

    fun updateSettings(transform: (LiquidGlassSettings) -> LiquidGlassSettings) {
        prefsStore.updateSettings(transform)
    }

    fun updateCashDenominations(map: Map<Int, Int>) {
        prefsStore.updateCashDenominations(map)
    }

    fun togglePrivacyMask() {
        val current = settingsFlow.value.privacyMaskEnabled
        prefsStore.updateSettings { it.copy(privacyMaskEnabled = !current) }
    }

    fun addAccount(
        name: String,
        type: String,
        balance: Double,
        currency: String = "INR",
        institution: String = "",
        accountNumber: String = "",
        notes: String = ""
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
                        notes = notes.trim()
                    )
                )
            }.onSuccess {
                statusMessage.value = "Wallet saved successfully"
            }.onFailure {
                statusMessage.value = it.message ?: "Failed to save wallet"
            }
        }
    }

    fun saveOrUpdateTransaction(
        id: Long?,
        accountId: Long,
        type: String,
        amount: Double,
        category: String,
        note: String = "",
        paymentMethod: String = "Cash",
        upiApp: String = ""
    ) {
        viewModelScope.launch {
            runCatching {
                if (id != null && id > 0L) {
                    val existing = uiState.value.transactions.firstOrNull { it.id == id }
                    if (existing != null) {
                        repository.updateTransaction(
                            existing.copy(
                                accountId = accountId,
                                type = type.uppercase(),
                                amount = amount,
                                amountPaise = MoneyPaise.toPaise(amount),
                                category = category,
                                note = note,
                                paymentMethod = paymentMethod,
                                upiApp = upiApp
                            )
                        )
                        statusMessage.value = "Transaction updated"
                    }
                } else {
                    repository.saveTransaction(
                        TransactionEntity(
                            accountId = accountId,
                            type = type.uppercase(),
                            amount = amount,
                            amountPaise = MoneyPaise.toPaise(amount),
                            category = category,
                            note = note,
                            paymentMethod = paymentMethod,
                            upiApp = upiApp
                        )
                    )
                    statusMessage.value = "Transaction saved"
                }
            }.onFailure {
                statusMessage.value = it.message ?: "Failed to save transaction"
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            recentlyDeleted.value = transaction
            statusMessage.value = "Transaction deleted"
        }
    }

    fun undoDeleteTransaction() {
        val tx = recentlyDeleted.value ?: return
        viewModelScope.launch {
            repository.saveTransaction(tx.copy(id = 0L))
            recentlyDeleted.value = null
            statusMessage.value = "Transaction restored"
        }
    }

    fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double) {
        viewModelScope.launch {
            runCatching {
                repository.saveTransaction(
                    TransactionEntity(
                        accountId = fromAccountId,
                        type = "EXPENSE",
                        amount = amount,
                        amountPaise = MoneyPaise.toPaise(amount),
                        category = "Transfer Out",
                        note = "Transfer to account #$toAccountId",
                        paymentMethod = "Transfer"
                    )
                )
                repository.saveTransaction(
                    TransactionEntity(
                        accountId = toAccountId,
                        type = "INCOME",
                        amount = amount,
                        amountPaise = MoneyPaise.toPaise(amount),
                        category = "Transfer In",
                        note = "Transfer from account #$fromAccountId",
                        paymentMethod = "Transfer"
                    )
                )
            }.onSuccess {
                statusMessage.value = "Transfer completed"
            }.onFailure {
                statusMessage.value = it.message ?: "Transfer failed"
            }
        }
    }

    fun createCustomTracker(
        name: String,
        category: String,
        budget: Double,
        startDate: String,
        endDate: String
    ) {
        viewModelScope.launch {
            runCatching {
                repository.upsertCustomTracker(
                    CustomTrackerEntity(
                        stableUid = java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        trackerType = "CUSTOM",
                        customTypeName = category.trim(),
                        budgetAmount = budget,
                        startDate = startDate,
                        endDate = endDate,
                        status = "ACTIVE"
                    )
                )
            }.onSuccess {
                statusMessage.value = "Custom tracker created"
            }.onFailure {
                statusMessage.value = it.message ?: "Failed to create tracker"
            }
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
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
