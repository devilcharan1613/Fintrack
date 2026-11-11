package com.charan.tracmio.ui.nativeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.charan.tracmio.data.MoneyPaise
import com.charan.tracmio.data.NativeLedgerCalculator
import com.charan.tracmio.data.PersonLedgerSummary
import com.charan.tracmio.data.XlsxExporter
import com.charan.tracmio.data.local.AccountEntity
import com.charan.tracmio.data.local.BudgetEntity
import com.charan.tracmio.data.local.CategoryEntity
import com.charan.tracmio.data.local.GoalContributionEntity
import com.charan.tracmio.data.local.GoalEntity
import com.charan.tracmio.data.local.LoanEntity
import com.charan.tracmio.data.local.LoanPaymentEntity
import com.charan.tracmio.data.local.PersonEntity
import com.charan.tracmio.data.local.TransactionEntity
import com.charan.tracmio.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val statusMessage: String? = null
) {
    val totalBalance: Double
        get() = NativeLedgerCalculator.totalBalance(accounts, transactions)

    val totalIncome: Double
        get() = NativeLedgerCalculator.totalIncome(transactions)

    val totalExpense: Double
        get() = NativeLedgerCalculator.totalExpense(transactions)

    val totalBorrowed: Double
        get() = transactions.filter { it.type.equals("BORROW", ignoreCase = true) }.sumOf { it.amount }

    val totalLent: Double
        get() = transactions.filter { it.type.equals("LEND", ignoreCase = true) }.sumOf { it.amount }

    val personSummaries: List<PersonLedgerSummary>
        get() = NativeLedgerCalculator.personSummaries(persons, transactions)

    fun accountBalance(account: AccountEntity): Double {
        return MoneyPaise.toRupees(NativeLedgerCalculator.accountBalancePaise(account, transactions))
    }
}

class NativeTracmioViewModel(
    private val repository: FinanceRepository,
    private val preferencesStore: TracmioPreferencesStore? = null
) : ViewModel() {

    private val statusMessage = MutableStateFlow<String?>(null)
    private val fallbackPrefs = MutableStateFlow(TracmioUserPreferences(isWelcomed = true))

    val preferences: StateFlow<TracmioUserPreferences> =
        preferencesStore?.state ?: fallbackPrefs.asStateFlow()

    private val primaryFlow = combine(
        repository.accountList,
        repository.transactions,
        repository.budgets,
        repository.goals,
        repository.goalContributions
    ) { accounts, transactions, budgets, goals, goalContribs ->
        NativeTracmioUiState(
            accounts = accounts,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            goalContributions = goalContribs
        )
    }

    private val secondaryFlow = combine(
        repository.loans,
        repository.loanPaymentsAll,
        repository.categories,
        repository.persons,
        statusMessage
    ) { loans, loanPayments, categories, persons, message ->
        NativeTracmioUiState(
            loans = loans,
            loanPayments = loanPayments,
            categories = categories,
            persons = persons,
            statusMessage = message
        )
    }

    val uiState: StateFlow<NativeTracmioUiState> = combine(primaryFlow, secondaryFlow) { p, s ->
        p.copy(
            loans = s.loans,
            loanPayments = s.loanPayments,
            categories = s.categories,
            persons = s.persons,
            statusMessage = s.statusMessage
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NativeTracmioUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedDefaultCategories()
        }
    }

    fun completeWelcome(name: String, currency: String) {
        preferencesStore?.completeWelcome(name, currency)
        viewModelScope.launch {
            repository.seedDefaultCategories()
            val existing = repository.accountList.first()
            if (existing.isEmpty()) {
                repository.addAccount(
                    AccountEntity(
                        name = "Main Bank",
                        accountType = "Bank",
                        balance = 0.0,
                        currency = currency.ifBlank { "INR" }
                    )
                )
                repository.addAccount(
                    AccountEntity(
                        name = "Cash Wallet",
                        accountType = "Cash",
                        balance = 0.0,
                        currency = currency.ifBlank { "INR" }
                    )
                )
                repository.addAccount(
                    AccountEntity(
                        name = "UPI Wallet",
                        accountType = "UPI",
                        balance = 0.0,
                        currency = currency.ifBlank { "INR" }
                    )
                )
            }
            statusMessage.value = "Welcome to Tracmio, ${name.ifBlank { "Friend" }}!"
        }
    }

    fun reopenWelcome() {
        preferencesStore?.reopenWelcome()
    }

    fun setBgTheme(bgId: String) {
        preferencesStore?.setBgTheme(bgId)
    }

    fun setGlassUi(uiId: String) {
        preferencesStore?.setGlassUi(uiId)
    }

    fun setCustomNeonColors(primaryHex: String, secondaryHex: String) {
        preferencesStore?.setCustomNeonColors(primaryHex, secondaryHex)
    }

    fun setCustomBgImagePath(path: String) {
        preferencesStore?.setCustomBgImagePath(path)
    }

    fun setBgPhotoOpacity(opacity: Float) {
        preferencesStore?.setBgPhotoOpacity(opacity)
    }

    fun setUiScale(scale: Float) {
        preferencesStore?.setUiScale(scale)
    }

    fun setCurrency(currency: String) {
        preferencesStore?.setCurrency(currency)
    }

    fun addCustomUpiApp(appName: String) {
        preferencesStore?.addCustomUpiApp(appName)
        statusMessage.value = "Added UPI app: ${appName.trim()}"
    }

    fun setBiometricEnabled(enabled: Boolean) {
        preferencesStore?.setBiometricEnabled(enabled)
    }

    fun updateReminderSettings(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        memeMode: Boolean,
        alertCategory: String
    ) {
        preferencesStore?.updateReminderState(enabled, hour, minute, memeMode, alertCategory)
        statusMessage.value = if (enabled) {
            String.format("Daily reminder set for %02d:%02d", hour, minute)
        } else {
            "Daily reminder paused"
        }
    }

    fun addAccount(
        name: String,
        type: String,
        balance: Double,
        currency: String = "INR",
        institution: String = ""
    ) {
        viewModelScope.launch {
            repository.addAccount(
                AccountEntity(
                    name = name.trim().ifBlank { "Account" },
                    accountType = type.trim().ifBlank { "Bank" },
                    balance = balance,
                    balancePaise = MoneyPaise.toPaise(balance),
                    currency = currency,
                    institution = institution.trim()
                )
            )
            statusMessage.value = "Account saved"
        }
    }

    fun archiveAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.archiveAccount(account)
            statusMessage.value = "Account archived"
        }
    }

    fun addTransaction(
        accountId: Long,
        type: String,
        amount: Double,
        category: String,
        note: String = "",
        paymentMethod: String = "Cash",
        upiApp: String = "",
        transferAccountId: Long? = null,
        personId: Long? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            runCatching {
                val normalizedType = type.uppercase()
                if (normalizedType == "TRANSFER" && transferAccountId != null) {
                    repository.addTransfer(
                        from = accountId,
                        to = transferAccountId,
                        amount = amount,
                        note = note.trim(),
                        timestamp = timestamp,
                        paymentMethod = paymentMethod,
                        upiApp = upiApp
                    )
                } else {
                    repository.saveTransaction(
                        TransactionEntity(
                            accountId = accountId,
                            type = normalizedType,
                            amount = amount,
                            amountPaise = MoneyPaise.toPaise(amount),
                            category = category.trim().ifBlank { "General" },
                            note = note.trim(),
                            paymentMethod = paymentMethod,
                            upiApp = upiApp,
                            personId = personId,
                            timestamp = timestamp
                        )
                    )
                }
            }.onSuccess {
                statusMessage.value = "Transaction saved"
            }.onFailure {
                statusMessage.value = it.message
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            statusMessage.value = "Transaction deleted"
        }
    }

    fun addBudget(category: String, limitAmount: Double, month: String) {
        viewModelScope.launch {
            repository.addBudget(
                BudgetEntity(
                    category = category.trim().ifBlank { "General" },
                    limitAmount = limitAmount,
                    limitAmountPaise = MoneyPaise.toPaise(limitAmount),
                    month = month
                )
            )
            statusMessage.value = "Budget saved"
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            statusMessage.value = "Budget deleted"
        }
    }

    fun addGoal(name: String, targetAmount: Double, savedAmount: Double = 0.0, deadline: Long? = null) {
        viewModelScope.launch {
            repository.addGoal(
                GoalEntity(
                    name = name.trim().ifBlank { "Savings Goal" },
                    targetAmount = targetAmount,
                    targetAmountPaise = MoneyPaise.toPaise(targetAmount),
                    savedAmount = savedAmount,
                    savedAmountPaise = MoneyPaise.toPaise(savedAmount),
                    deadline = deadline
                )
            )
            statusMessage.value = "Goal created"
        }
    }

    fun contributeToGoal(goalId: Long, amount: Double, accountId: Long?, note: String = "") {
        viewModelScope.launch {
            runCatching {
                repository.addGoalContribution(
                    goalId = goalId,
                    amount = amount,
                    accountId = accountId,
                    note = note
                )
            }.onSuccess {
                statusMessage.value = "Contribution added"
            }.onFailure {
                statusMessage.value = it.message
            }
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            statusMessage.value = "Goal deleted"
        }
    }

    fun addLoan(
        name: String,
        principal: Double,
        paidAmount: Double = 0.0,
        monthlyPayment: Double = 0.0,
        interestRate: Double = 0.0,
        dueDay: Int = 5
    ) {
        viewModelScope.launch {
            repository.addLoan(
                LoanEntity(
                    name = name.trim().ifBlank { "Loan" },
                    principal = principal,
                    principalPaise = MoneyPaise.toPaise(principal),
                    paidAmount = paidAmount,
                    paidAmountPaise = MoneyPaise.toPaise(paidAmount),
                    monthlyPayment = monthlyPayment,
                    monthlyPaymentPaise = MoneyPaise.toPaise(monthlyPayment),
                    interestRate = interestRate,
                    dueDay = dueDay
                )
            )
            statusMessage.value = "Loan added"
        }
    }

    fun payLoanEmi(loanId: Long, amount: Double, accountId: Long?, note: String = "") {
        viewModelScope.launch {
            runCatching {
                repository.addLoanPayment(
                    LoanPaymentEntity(
                        loanId = loanId,
                        accountId = accountId,
                        amount = amount,
                        amountPaise = MoneyPaise.toPaise(amount),
                        note = note
                    )
                )
            }.onSuccess {
                statusMessage.value = "Loan payment recorded"
            }.onFailure {
                statusMessage.value = it.message
            }
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            statusMessage.value = "Loan deleted"
        }
    }

    fun addPerson(name: String, phone: String = "", notes: String = "") {
        viewModelScope.launch {
            repository.addPerson(
                PersonEntity(
                    name = name.trim().ifBlank { "Contact" },
                    phone = phone.trim(),
                    notes = notes.trim()
                )
            )
            statusMessage.value = "Person added"
        }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            repository.deletePerson(person)
            statusMessage.value = "Person removed"
        }
    }

    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            repository.addCategory(
                CategoryEntity(
                    name = name.trim().ifBlank { "Custom" },
                    type = type.uppercase()
                )
            )
            statusMessage.value = "Category added"
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            statusMessage.value = "Category deleted"
        }
    }

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.exportJson()
            }.onSuccess { json ->
                onReady(json)
            }.onFailure {
                statusMessage.value = "Export failed: ${it.message}"
            }
        }
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            runCatching {
                repository.restoreJson(json)
            }.onSuccess {
                statusMessage.value = "Backup restored successfully"
            }.onFailure {
                statusMessage.value = "Restore failed: ${it.message}"
            }
        }
    }

    fun exportExcelWorkbook(onReady: (ByteArray) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val json = repository.exportJson()
                XlsxExporter.create(json)
            }.onSuccess { bytes ->
                onReady(bytes)
            }.onFailure {
                statusMessage.value = "Excel export failed: ${it.message}"
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.seedDefaultCategories()
            preferencesStore?.resetAllPreferencesForNewUser()
            statusMessage.value = "All data reset"
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    companion object {
        fun factory(
            repository: FinanceRepository,
            preferencesStore: TracmioPreferencesStore? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NativeTracmioViewModel(repository, preferencesStore) as T
                }
            }
    }
}
