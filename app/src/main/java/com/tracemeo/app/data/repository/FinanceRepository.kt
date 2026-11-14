package com.tracmio.app.data.repository

import androidx.room.withTransaction
import com.tracmio.app.data.CustomReminderItem
import com.tracmio.app.data.LiquidGlassSettings
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.NativeLedgerCalculator
import com.tracmio.app.data.PreferencesAndThemeStore
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.AppSettingsEntity
import com.tracmio.app.data.local.BudgetEntity
import com.tracmio.app.data.local.CashbackEntryEntity
import com.tracmio.app.data.local.CategoryEntity
import com.tracmio.app.data.local.CategorySpendProjection
import com.tracmio.app.data.local.CustomTrackerEntity
import com.tracmio.app.data.local.GoalContributionEntity
import com.tracmio.app.data.local.GoalEntity
import com.tracmio.app.data.local.LoanEntity
import com.tracmio.app.data.local.LoanPaymentEntity
import com.tracmio.app.data.local.PersonEntity
import com.tracmio.app.data.local.QuickTemplateEntity
import com.tracmio.app.data.local.ReminderEntity
import com.tracmio.app.data.local.TracmioDatabase
import com.tracmio.app.data.local.TrackerActivityLogEntity
import com.tracmio.app.data.local.TrackerAttachmentEntity
import com.tracmio.app.data.local.TrackerPersonEntity
import com.tracmio.app.data.local.TrackerSettlementEntity
import com.tracmio.app.data.local.TrackerTransactionCrossRefEntity
import com.tracmio.app.data.local.TransactionEntity
import com.tracmio.app.data.local.ValueItemEntity
import com.tracmio.app.data.local.WishlistItemEntity
import com.tracmio.app.data.security.TracmioSecurityEngine
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class FinanceRepository(
    val database: TracmioDatabase,
    private val prefsStore: PreferencesAndThemeStore? = null
) {
    private val accounts = database.accountDao()
    private val finance = database.financeDao()
    private val customTrackerDao = database.customTrackerDao()
    private val features = database.featureDao()

    val accountList: Flow<List<AccountEntity>> = accounts.getAllAccounts()
    val allAccountsIncludingArchived: Flow<List<AccountEntity>> = accounts.getAllAccountsIncludingArchived()
    val transactions: Flow<List<TransactionEntity>> = finance.transactions()
    val budgets: Flow<List<BudgetEntity>> = finance.budgets()
    val goals: Flow<List<GoalEntity>> = finance.goals()
    val goalContributions: Flow<List<GoalContributionEntity>> = finance.allGoalContributions()
    val loans: Flow<List<LoanEntity>> = finance.loans()
    val loanPaymentsAll: Flow<List<LoanPaymentEntity>> = finance.allLoanPayments()
    val categories: Flow<List<CategoryEntity>> = finance.categories()
    val persons: Flow<List<PersonEntity>> = finance.persons()

    val activeTrackers: Flow<List<CustomTrackerEntity>> = customTrackerDao.getActiveTrackers()
    val archivedTrackers: Flow<List<CustomTrackerEntity>> = customTrackerDao.getArchivedTrackers()
    val trackerLinks: Flow<List<TrackerTransactionCrossRefEntity>> = customTrackerDao.observeAllTrackerLinks()
    val trackerPeople: Flow<List<TrackerPersonEntity>> = customTrackerDao.observeAllTrackerPeople()
    val trackerSettlements: Flow<List<TrackerSettlementEntity>> = customTrackerDao.observeAllTrackerSettlements()
    val trackerAttachments: Flow<List<TrackerAttachmentEntity>> = customTrackerDao.observeAllTrackerAttachments()

    val quickTemplates: Flow<List<QuickTemplateEntity>> = features.quickTemplates()
    val wishlistItems: Flow<List<WishlistItemEntity>> = features.wishlistItems()
    val cashbackEntries: Flow<List<CashbackEntryEntity>> = features.cashbackEntries()
    val valueItems: Flow<List<ValueItemEntity>> = features.valueItems()
    val reminders: Flow<List<ReminderEntity>> = features.reminders()
    val appSettings: Flow<AppSettingsEntity?> = features.observeAppSettings()

    fun observeExpenseCategoryTotals(fromMillis: Long, toMillis: Long): Flow<List<CategorySpendProjection>> =
        finance.observeExpenseCategoryTotals(fromMillis, toMillis)

    suspend fun seedDefaultCategories() {
        val current = categories.first().toMutableList()
        val classicParents = listOf(
            Triple("Food & Chai", "EXPENSE", "☕"),
            Triple("Khata & Kirana", "EXPENSE", "🏪"),
            Triple("Transport & Fuel", "EXPENSE", "🛺"),
            Triple("SIM, WiFi & Bills", "EXPENSE", "📶"),
            Triple("College & Exams", "EXPENSE", "🎓"),
            Triple("Shopping & Personal", "EXPENSE", "🛍️"),
            Triple("Family & Income", "INCOME", "💰"),
            Triple("Food", "EXPENSE", "🍽️"),
            Triple("Travel", "EXPENSE", "✈️"),
            Triple("Bills", "EXPENSE", "📄"),
            Triple("Shopping", "EXPENSE", "🛍️"),
            Triple("Health", "EXPENSE", "💊"),
            Triple("Entertainment", "EXPENSE", "🎬"),
            Triple("Education", "EXPENSE", "📚"),
            Triple("Other", "EXPENSE", "📦"),
            Triple("Salary", "INCOME", "💼"),
            Triple("Investment", "INCOME", "📈"),
            Triple("Other Income", "INCOME", "💰")
        )
        classicParents.forEach { (name, type, icon) ->
            val exists = current.firstOrNull {
                it.parentId == null && it.name.equals(name, ignoreCase = true) && it.type.equals(type, ignoreCase = true)
            }
            if (exists == null) {
                val insertedId = finance.insertCategory(
                    CategoryEntity(name = name, type = type, icon = icon, isSystem = true, parentId = null)
                )
                current.add(CategoryEntity(id = insertedId, name = name, type = type, icon = icon, isSystem = true, parentId = null))
            }
        }
        NativeLedgerCalculator.EXPANDED_CATEGORY_CATALOG.forEach { spec ->
            val parent = current.firstOrNull {
                it.parentId == null && it.name.equals(spec.name, ignoreCase = true) && it.type.equals(spec.type, ignoreCase = true)
            } ?: return@forEach
            spec.subcategories.forEach { subName ->
                val subExists = current.any {
                    it.parentId == parent.id && it.name.equals(subName, ignoreCase = true)
                }
                if (!subExists) {
                    val subId = finance.insertCategory(
                        CategoryEntity(
                            name = subName,
                            type = spec.type,
                            icon = spec.icon,
                            isSystem = true,
                            parentId = parent.id
                        )
                    )
                    current.add(
                        CategoryEntity(
                            id = subId,
                            name = subName,
                            type = spec.type,
                            icon = spec.icon,
                            isSystem = true,
                            parentId = parent.id
                        )
                    )
                }
            }
        }
    }

    suspend fun seedDefaultQuickTemplatesIfEmpty() {
        val current = quickTemplates.first()
        if (current.isNotEmpty()) return
        listOf(
            QuickTemplateEntity(icon = "☕", label = "Chai / Coffee", amount = 20.0, category = "Food & Chai"),
            QuickTemplateEntity(icon = "🍳", label = "Breakfast", amount = 60.0, category = "Food & Chai"),
            QuickTemplateEntity(icon = "🛺", label = "Auto / Metro", amount = 100.0, category = "Transport & Fuel"),
            QuickTemplateEntity(icon = "🛒", label = "Groceries", amount = 350.0, category = "Food & Chai")
        ).forEach { features.insertQuickTemplate(it) }
    }

    suspend fun ensureDefaultAccountIfEmpty() {
        val current = accounts.getAllAccountsIncludingArchived().first()
        if (current.isEmpty()) {
            accounts.insertAccount(
                AccountEntity(
                    name = "Cash Wallet",
                    accountType = "Cash",
                    balance = 0.0,
                    balancePaise = 0L,
                    currency = "INR"
                )
            )
        }
    }

    val totalBalance: Flow<Double> = accounts.getComputedTotalBalance()
    val income: Flow<Double> = finance.totalIncome()
    val expense: Flow<Double> = finance.totalExpense()

    suspend fun addAccount(account: AccountEntity): Long {
        val cleanName = TracmioSecurityEngine.sanitizeInput(account.name, 80)
        require(cleanName.isNotEmpty()) { "Account name cannot be empty" }
        val cleanType = TracmioSecurityEngine.sanitizeInput(account.accountType, 30).ifBlank { "Bank" }
        val existing = accounts.getAllAccountsIncludingArchived().first().firstOrNull {
            !it.isArchived &&
                it.name.equals(cleanName, ignoreCase = true) &&
                it.accountType.equals(cleanType, ignoreCase = true)
        }
        require(existing == null) { "An account named '$cleanName' ($cleanType) already exists" }

        return accounts.insertAccount(
            account.copy(
                name = cleanName,
                accountType = cleanType,
                balancePaise = MoneyPaise.toPaise(account.balance),
                institution = TracmioSecurityEngine.sanitizeInput(account.institution, 80),
                accountNumber = TracmioSecurityEngine.sanitizeInput(account.accountNumber, 32),
                notes = TracmioSecurityEngine.sanitizeInput(account.notes, 250)
            )
        )
    }

    suspend fun updateAccount(account: AccountEntity) = accounts.updateAccount(
        account.copy(
            name = TracmioSecurityEngine.sanitizeInput(account.name, 80).ifBlank { "Account" },
            accountType = TracmioSecurityEngine.sanitizeInput(account.accountType, 30).ifBlank { "Bank" },
            balancePaise = MoneyPaise.toPaise(account.balance),
            institution = TracmioSecurityEngine.sanitizeInput(account.institution, 80),
            accountNumber = TracmioSecurityEngine.sanitizeInput(account.accountNumber, 32),
            notes = TracmioSecurityEngine.sanitizeInput(account.notes, 250),
            updatedAt = System.currentTimeMillis()
        )
    )

    suspend fun archiveAccount(account: AccountEntity) = accounts.archiveAccount(account.id)

    suspend fun reconcileAccountBalance(
        account: AccountEntity,
        actualBalance: Double,
        note: String = "Balance reconciliation"
    ) {
        val currentTxns = transactions.first()
        val computed = NativeLedgerCalculator.accountBalance(account, currentTxns)
        val delta = actualBalance - computed
        if (kotlin.math.abs(delta) < 0.01) return
        val type = if (delta > 0) "INCOME" else "EXPENSE"
        val absAmt = kotlin.math.abs(delta)
        saveTransaction(
            TransactionEntity(
                accountId = account.id,
                type = type,
                amount = absAmt,
                amountPaise = MoneyPaise.toPaise(absAmt),
                category = "Other",
                subcategory = "Reconcile",
                note = TracmioSecurityEngine.sanitizeInput(note, 200),
                paymentMethod = account.accountType
            )
        )
    }

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        require(transaction.amount > 0) { "Transaction amount must be positive" }
        val cleanCat = TracmioSecurityEngine.sanitizeInput(transaction.category, 60)
        require(cleanCat.isNotBlank()) { "Transaction category is required" }
        val targetAcc = accounts.getAccountById(transaction.accountId)
            ?: throw IllegalArgumentException("Selected account does not exist")

        val cleanNote = TracmioSecurityEngine.sanitizeInput(transaction.note, 400)
        val sanitized = transaction.copy(
            amountPaise = MoneyPaise.toPaise(transaction.amount),
            category = cleanCat,
            subcategory = TracmioSecurityEngine.sanitizeInput(transaction.subcategory, 60),
            note = cleanNote,
            hashtags = NativeLedgerCalculator.formatHashtags(cleanNote, transaction.hashtags),
            paymentMethod = TracmioSecurityEngine.sanitizeInput(transaction.paymentMethod, 30).ifBlank { "Cash" },
            upiApp = TracmioSecurityEngine.sanitizeInput(transaction.upiApp, 30),
            warrantyDate = TracmioSecurityEngine.sanitizeInput(transaction.warrantyDate, 20),
            attachmentUri = TracmioSecurityEngine.sanitizeInput(transaction.attachmentUri, 300),
            allocationId = TracmioSecurityEngine.sanitizeInput(transaction.allocationId, 64)
        )

        var newId = 0L
        database.withTransaction {
            newId = finance.insertTransaction(sanitized)
            var currentAcc = targetAcc
            if (sanitized.type.equals("INCOME", ignoreCase = true) && sanitized.allocationId.isBlank()) {
                val autoSplitAcc = NativeLedgerCalculator.applyAutoSplitToAllocations(currentAcc, sanitized.amount)
                if (autoSplitAcc.allocationsJson != currentAcc.allocationsJson) {
                    currentAcc = autoSplitAcc
                }
            }
            if (sanitized.allocationId.isNotBlank()) {
                currentAcc = NativeLedgerCalculator.adjustAllocationsForTransaction(
                    account = currentAcc,
                    allocationId = sanitized.allocationId,
                    txType = sanitized.type,
                    amount = sanitized.amount,
                    reverse = false
                )
            }
            if (currentAcc.allocationsJson != targetAcc.allocationsJson) {
                accounts.updateAccount(currentAcc)
            }
            if (sanitized.type.equals("EXPENSE", ignoreCase = true) && prefsStore != null) {
                val s = prefsStore.settings.value
                val goalId = s.roundUpGoalId
                if (s.roundUpEnabled && goalId != null) {
                    val spare = NativeLedgerCalculator.computeRoundUpSpareChange(sanitized.amount, s.roundUpStep)
                    if (spare > 0.0) {
                        val goal = finance.getGoalById(goalId)
                        if (goal != null) {
                            val sparePaise = MoneyPaise.toPaise(spare)
                            finance.insertGoalContribution(
                                GoalContributionEntity(
                                    goalId = goalId,
                                    accountId = sanitized.accountId,
                                    transactionId = newId,
                                    amount = spare,
                                    amountPaise = sparePaise,
                                    timestamp = sanitized.timestamp,
                                    note = "Round-up spare change (${s.currencySymbol}${s.roundUpStep})"
                                )
                            )
                            val newSavedPaise = goal.savedAmountPaise + sparePaise
                            finance.updateGoal(
                                goal.copy(
                                    savedAmount = MoneyPaise.toRupees(newSavedPaise),
                                    savedAmountPaise = newSavedPaise
                                )
                            )
                        }
                    }
                }
            }
        }
        return newId
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        require(transaction.id > 0L) { "Cannot update transaction without a valid ID" }
        require(transaction.amount > 0) { "Transaction amount must be positive" }
        val cleanCat = TracmioSecurityEngine.sanitizeInput(transaction.category, 60)
        require(cleanCat.isNotBlank()) { "Transaction category is required" }
        val newAcc = accounts.getAccountById(transaction.accountId)
            ?: throw IllegalArgumentException("Selected account does not exist")

        val cleanNote = TracmioSecurityEngine.sanitizeInput(transaction.note, 400)
        val updatedEntity = transaction.copy(
            amountPaise = MoneyPaise.toPaise(transaction.amount),
            category = cleanCat,
            subcategory = TracmioSecurityEngine.sanitizeInput(transaction.subcategory, 60),
            note = cleanNote,
            hashtags = NativeLedgerCalculator.formatHashtags(cleanNote, transaction.hashtags),
            paymentMethod = TracmioSecurityEngine.sanitizeInput(transaction.paymentMethod, 30).ifBlank { "Cash" },
            upiApp = TracmioSecurityEngine.sanitizeInput(transaction.upiApp, 30),
            warrantyDate = TracmioSecurityEngine.sanitizeInput(transaction.warrantyDate, 20),
            attachmentUri = TracmioSecurityEngine.sanitizeInput(transaction.attachmentUri, 300),
            allocationId = TracmioSecurityEngine.sanitizeInput(transaction.allocationId, 64)
        )
        database.withTransaction {
            val existing = finance.getTransactionById(transaction.id)
            if (existing != null) {
                if (existing.allocationId.isNotBlank()) {
                    accounts.getAccountById(existing.accountId)?.let { oldAcc ->
                        val reverted = NativeLedgerCalculator.adjustAllocationsForTransaction(
                            oldAcc, existing.allocationId, existing.type, existing.amount, reverse = true
                        )
                        if (reverted.allocationsJson != oldAcc.allocationsJson) accounts.updateAccount(reverted)
                    }
                }
                if (updatedEntity.allocationId.isNotBlank()) {
                    val freshNewAcc = accounts.getAccountById(updatedEntity.accountId) ?: newAcc
                    val applied = NativeLedgerCalculator.adjustAllocationsForTransaction(
                        freshNewAcc, updatedEntity.allocationId, updatedEntity.type, updatedEntity.amount, reverse = false
                    )
                    if (applied.allocationsJson != freshNewAcc.allocationsJson) accounts.updateAccount(applied)
                }
                finance.updateTransaction(
                    updatedEntity.copy(
                        stableUid = existing.stableUid.ifBlank { transaction.stableUid }
                    )
                )
            } else {
                finance.insertTransaction(updatedEntity)
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            if (transaction.allocationId.isNotBlank()) {
                accounts.getAccountById(transaction.accountId)?.let { acc ->
                    val reverted = NativeLedgerCalculator.adjustAllocationsForTransaction(
                        acc, transaction.allocationId, transaction.type, transaction.amount, reverse = true
                    )
                    if (reverted.allocationsJson != acc.allocationsJson) accounts.updateAccount(reverted)
                }
            }
            if (transaction.transferId != null) {
                finance.deleteTransfer(transaction.transferId)
            } else {
                finance.deleteTransaction(transaction)
            }
        }
    }

    suspend fun addTransfer(
        from: Long,
        to: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        paymentMethod: String = "Transfer",
        upiApp: String = ""
    ) {
        require(from != to) { "Transfer accounts must be different" }
        require(amount > 0) { "Transfer amount must be positive" }
        require(accounts.getAccountById(from) != null) { "Source account does not exist" }
        require(accounts.getAccountById(to) != null) { "Destination account does not exist" }
        val transferId = UUID.randomUUID().toString()
        val paise = MoneyPaise.toPaise(amount)
        val cleanNote = TracmioSecurityEngine.sanitizeInput(note, 250)
        database.withTransaction {
            finance.insertTransaction(
                TransactionEntity(
                    accountId = from,
                    type = "TRANSFER_OUT",
                    amount = amount,
                    amountPaise = paise,
                    category = "Transfer",
                    note = cleanNote,
                    timestamp = timestamp,
                    transferAccountId = to,
                    transferId = transferId,
                    paymentMethod = paymentMethod,
                    upiApp = upiApp
                )
            )
            finance.insertTransaction(
                TransactionEntity(
                    accountId = to,
                    type = "TRANSFER_IN",
                    amount = amount,
                    amountPaise = paise,
                    category = "Transfer",
                    note = cleanNote,
                    timestamp = timestamp,
                    transferAccountId = from,
                    transferId = transferId,
                    paymentMethod = paymentMethod,
                    upiApp = upiApp
                )
            )
        }
    }

    suspend fun performAtmWithdrawal(
        amount: Double,
        explicitBankId: Long? = null,
        explicitCashId: Long? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        require(amount > 0) { "ATM withdrawal amount must be positive" }
        val active = accounts.getAllAccounts().first().filter { !it.isArchived }
        val cashAcc = active.firstOrNull { it.id == explicitCashId }
            ?: active.firstOrNull { it.accountType.equals("Cash", ignoreCase = true) }
            ?: run {
                val newCashId = accounts.insertAccount(
                    AccountEntity(
                        name = "Cash Wallet",
                        accountType = "Cash",
                        balance = 0.0,
                        balancePaise = 0L,
                        currency = "INR"
                    )
                )
                accounts.getAccountById(newCashId)!!
            }

        val bankAcc = active.firstOrNull { it.id == explicitBankId && it.id != cashAcc.id }
            ?: active.firstOrNull {
                it.id != cashAcc.id && (
                    it.accountType.equals("Bank", ignoreCase = true) ||
                        it.accountType.equals("Savings", ignoreCase = true)
                    )
            }
            ?: active.firstOrNull { it.id != cashAcc.id }
            ?: run {
                val newBankId = accounts.insertAccount(
                    AccountEntity(
                        name = "Primary Bank",
                        accountType = "Bank",
                        balance = 0.0,
                        balancePaise = 0L,
                        currency = "INR"
                    )
                )
                accounts.getAccountById(newBankId)!!
            }

        addTransfer(
            from = bankAcc.id,
            to = cashAcc.id,
            amount = amount,
            note = "🏧 ATM Withdrawal (${bankAcc.name} → ${cashAcc.name})",
            timestamp = timestamp,
            paymentMethod = "ATM Withdrawal"
        )
    }

    suspend fun saveSplitCashUpiTransaction(
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
        attachmentUri: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        require(cashAmount > 0.0 || upiAmount > 0.0) { "Enter Cash or UPI split amount" }
        val baseNote = note.trim().ifBlank { "Split Cash + UPI payment" }
        database.withTransaction {
            if (cashAmount > 0.0) {
                saveTransaction(
                    TransactionEntity(
                        stableUid = UUID.randomUUID().toString(),
                        accountId = cashAccountId,
                        type = type.uppercase(),
                        amount = cashAmount,
                        amountPaise = MoneyPaise.toPaise(cashAmount),
                        category = category,
                        subcategory = subcategory,
                        note = "$baseNote (Cash part)",
                        timestamp = timestamp,
                        paymentMethod = "Cash",
                        warrantyDate = warrantyDate,
                        personId = personId,
                        attachmentUri = attachmentUri
                    )
                )
            }
            if (upiAmount > 0.0) {
                saveTransaction(
                    TransactionEntity(
                        stableUid = UUID.randomUUID().toString(),
                        accountId = upiAccountId,
                        type = type.uppercase(),
                        amount = upiAmount,
                        amountPaise = MoneyPaise.toPaise(upiAmount),
                        category = category,
                        subcategory = subcategory,
                        note = "$baseNote (UPI part)",
                        timestamp = timestamp,
                        paymentMethod = "UPI",
                        upiApp = upiApp,
                        warrantyDate = warrantyDate,
                        personId = personId,
                        attachmentUri = attachmentUri
                    )
                )
            }
        }
    }

    suspend fun addBudget(budget: BudgetEntity) = finance.insertBudget(
        budget.copy(
            category = TracmioSecurityEngine.sanitizeInput(budget.category, 60),
            limitAmountPaise = MoneyPaise.toPaise(budget.limitAmount)
        )
    )

    suspend fun updateBudget(budget: BudgetEntity) = finance.updateBudget(
        budget.copy(limitAmountPaise = MoneyPaise.toPaise(budget.limitAmount))
    )

    suspend fun deleteBudget(budget: BudgetEntity) = finance.deleteBudget(budget)

    suspend fun addGoal(goal: GoalEntity) = finance.insertGoal(
        goal.copy(
            name = TracmioSecurityEngine.sanitizeInput(goal.name, 80),
            targetAmountPaise = MoneyPaise.toPaise(goal.targetAmount),
            savedAmountPaise = MoneyPaise.toPaise(goal.savedAmount)
        )
    )

    suspend fun updateGoal(goal: GoalEntity) = finance.updateGoal(
        goal.copy(
            name = TracmioSecurityEngine.sanitizeInput(goal.name, 80),
            targetAmountPaise = MoneyPaise.toPaise(goal.targetAmount),
            savedAmountPaise = MoneyPaise.toPaise(goal.savedAmount)
        )
    )

    suspend fun deleteGoal(goal: GoalEntity) {
        database.withTransaction {
            val contributions = finance.goalContributions(goal.id).first()
            contributions.forEach { contrib ->
                contrib.transactionId?.let { txId -> finance.deleteTransactionById(txId) }
            }
            finance.deleteGoalContributionsForGoal(goal.id)
            finance.deleteGoal(goal)
        }
    }

    suspend fun addGoalContribution(
        goalId: Long,
        amount: Double,
        accountId: Long? = null,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        require(amount > 0) { "Contribution amount must be positive" }
        val goal = finance.getGoalById(goalId) ?: error("Goal not found")
        val paise = MoneyPaise.toPaise(amount)
        database.withTransaction {
            val txId = if (accountId != null && accounts.getAccountById(accountId) != null) {
                finance.insertTransaction(
                    TransactionEntity(
                        accountId = accountId,
                        type = "EXPENSE",
                        amount = amount,
                        amountPaise = paise,
                        category = "Savings Goal",
                        subcategory = goal.name,
                        note = note.ifBlank { "Saved towards ${goal.name}" },
                        timestamp = timestamp
                    )
                )
            } else null

            finance.insertGoalContribution(
                GoalContributionEntity(
                    goalId = goalId,
                    accountId = accountId,
                    transactionId = txId,
                    amount = amount,
                    amountPaise = paise,
                    timestamp = timestamp,
                    note = TracmioSecurityEngine.sanitizeInput(note, 200)
                )
            )
            val newSavedPaise = goal.savedAmountPaise + paise
            finance.updateGoal(
                goal.copy(
                    savedAmount = MoneyPaise.toRupees(newSavedPaise),
                    savedAmountPaise = newSavedPaise
                )
            )
        }
    }

    suspend fun addLoan(loan: LoanEntity) = finance.insertLoan(
        loan.copy(
            name = TracmioSecurityEngine.sanitizeInput(loan.name, 80),
            principalPaise = MoneyPaise.toPaise(loan.principal),
            paidAmountPaise = MoneyPaise.toPaise(loan.paidAmount),
            monthlyPaymentPaise = MoneyPaise.toPaise(loan.monthlyPayment)
        )
    )

    suspend fun updateLoan(loan: LoanEntity) = finance.updateLoan(
        loan.copy(
            principalPaise = MoneyPaise.toPaise(loan.principal),
            paidAmountPaise = MoneyPaise.toPaise(loan.paidAmount),
            monthlyPaymentPaise = MoneyPaise.toPaise(loan.monthlyPayment)
        )
    )

    suspend fun deleteLoan(loan: LoanEntity) {
        database.withTransaction {
            val payments = finance.loanPayments(loan.id).first()
            payments.forEach { payment ->
                payment.transactionId?.let { txId -> finance.deleteTransactionById(txId) }
            }
            finance.deleteLoanPaymentsForLoan(loan.id)
            finance.deleteLoan(loan)
        }
    }

    suspend fun addLoanPayment(payment: LoanPaymentEntity): Long {
        require(payment.amount > 0) { "Payment amount must be positive" }
        val paise = MoneyPaise.toPaise(payment.amount)
        var insertedId = 0L
        database.withTransaction {
            val loan = finance.getLoanById(payment.loanId)
            val txId = if (payment.accountId != null && accounts.getAccountById(payment.accountId) != null) {
                finance.insertTransaction(
                    TransactionEntity(
                        accountId = payment.accountId,
                        type = "EXPENSE",
                        amount = payment.amount,
                        amountPaise = paise,
                        category = "Loan EMI",
                        subcategory = loan?.name.orEmpty(),
                        note = payment.note.ifBlank { "Loan payment: ${loan?.name.orEmpty()}" },
                        timestamp = payment.timestamp
                    )
                )
            } else payment.transactionId

            insertedId = finance.insertLoanPayment(
                payment.copy(
                    transactionId = txId,
                    amountPaise = paise
                )
            )
            if (loan != null) {
                val newPaidPaise = loan.paidAmountPaise + paise
                finance.updateLoan(
                    loan.copy(
                        paidAmount = MoneyPaise.toRupees(newPaidPaise),
                        paidAmountPaise = newPaidPaise
                    )
                )
            }
        }
        return insertedId
    }

    suspend fun addCategory(category: CategoryEntity): Long {
        val cleanName = TracmioSecurityEngine.sanitizeInput(category.name, 50)
        require(cleanName.isNotEmpty()) { "Category name is required" }
        val existing = categories.first().firstOrNull {
            it.name.equals(cleanName, ignoreCase = true) && it.type.equals(category.type, ignoreCase = true)
        }
        if (existing != null) {
            finance.updateCategory(existing.copy(icon = category.icon.ifBlank { existing.icon }))
            return existing.id
        }
        return finance.insertCategory(category.copy(name = cleanName))
    }

    suspend fun updateCategory(category: CategoryEntity) = finance.updateCategory(category)

    suspend fun deleteCategorySafely(category: CategoryEntity): Result<String> {
        val txCount = transactions.first().count { it.category.equals(category.name, ignoreCase = true) }
        val budgetCount = budgets.first().count { it.category.equals(category.name, ignoreCase = true) }
        if (txCount > 0 || budgetCount > 0) {
            return Result.failure(
                IllegalStateException(
                    "Category '${category.name}' is protected because it is used by $txCount transaction(s) and $budgetCount budget(s)."
                )
            )
        }
        finance.deleteCategory(category)
        return Result.success("Category '${category.name}' deleted.")
    }

    suspend fun addPerson(person: PersonEntity): Long {
        val cleanName = TracmioSecurityEngine.sanitizeInput(person.name, 80)
        require(cleanName.isNotEmpty()) { "Person name is required" }
        return finance.insertPerson(
            person.copy(
                name = cleanName,
                phone = TracmioSecurityEngine.sanitizeInput(person.phone, 30),
                notes = TracmioSecurityEngine.sanitizeInput(person.notes, 200)
            )
        )
    }

    suspend fun updatePerson(person: PersonEntity) = finance.updatePerson(person)
    suspend fun deletePerson(person: PersonEntity) = finance.deletePerson(person)

    // Feature Entities CRUD
    suspend fun addQuickTemplate(item: QuickTemplateEntity): Long = features.insertQuickTemplate(
        item.copy(
            label = TracmioSecurityEngine.sanitizeInput(item.label, 40),
            category = TracmioSecurityEngine.sanitizeInput(item.category, 40)
        )
    )

    suspend fun deleteQuickTemplate(item: QuickTemplateEntity) = features.deleteQuickTemplate(item)

    suspend fun addWishlistItem(name: String, price: Double): Long = features.insertWishlistItem(
        WishlistItemEntity(
            name = TracmioSecurityEngine.sanitizeInput(name, 80),
            price = price.coerceAtLeast(0.0)
        )
    )

    suspend fun updateWishlistItem(item: WishlistItemEntity) = features.updateWishlistItem(item)
    suspend fun deleteWishlistItem(item: WishlistItemEntity) = features.deleteWishlistItem(item)

    suspend fun addCashbackEntry(source: String, amount: Double, note: String): Long =
        features.insertCashbackEntry(
            CashbackEntryEntity(
                source = TracmioSecurityEngine.sanitizeInput(source, 60),
                amount = amount.coerceAtLeast(0.0),
                note = TracmioSecurityEngine.sanitizeInput(note, 160)
            )
        )

    suspend fun deleteCashbackEntry(item: CashbackEntryEntity) = features.deleteCashbackEntry(item)

    suspend fun addValueItem(name: String, cost: Double, uses: Int = 1): Long = features.insertValueItem(
        ValueItemEntity(
            name = TracmioSecurityEngine.sanitizeInput(name, 80),
            cost = cost.coerceAtLeast(0.0),
            uses = uses.coerceAtLeast(1)
        )
    )

    suspend fun updateValueItem(item: ValueItemEntity) = features.updateValueItem(item)
    suspend fun deleteValueItem(item: ValueItemEntity) = features.deleteValueItem(item)

    // Custom Trackers Operations
    suspend fun upsertCustomTracker(tracker: CustomTrackerEntity): Long {
        val now = System.currentTimeMillis()
        val clean = tracker.copy(
            name = TracmioSecurityEngine.sanitizeInput(tracker.name, 80),
            customTypeName = TracmioSecurityEngine.sanitizeInput(tracker.customTypeName, 40),
            description = TracmioSecurityEngine.sanitizeInput(tracker.description, 200),
            notes = TracmioSecurityEngine.sanitizeInput(tracker.notes, 250),
            updatedAt = now
        )
        val id = customTrackerDao.upsertTracker(clean)
        customTrackerDao.insertActivityLog(
            TrackerActivityLogEntity(
                logUid = UUID.randomUUID().toString(),
                trackerUid = clean.stableUid,
                actionType = "UPSERT",
                description = "Saved tracker '${clean.name}'",
                timestamp = now
            )
        )
        return id
    }

    suspend fun archiveCustomTracker(trackerUid: String, archived: Boolean) {
        val status = if (archived) "ARCHIVED" else "ACTIVE"
        customTrackerDao.setTrackerArchived(trackerUid, archived, status)
    }

    suspend fun deleteCustomTracker(trackerUid: String) {
        customTrackerDao.deleteTrackerByUid(trackerUid)
    }

    suspend fun linkTransactionToTracker(
        trackerUid: String,
        transactionUid: String,
        isRefund: Boolean = false,
        paidByPersonUid: String = "",
        splitMode: String = "EQUAL"
    ) {
        customTrackerDao.linkTransactionToTracker(
            TrackerTransactionCrossRefEntity(
                trackerUid = trackerUid,
                transactionUid = transactionUid,
                isRefund = isRefund,
                paidByPersonUid = paidByPersonUid,
                splitMode = splitMode
            )
        )
    }

    suspend fun unlinkTransactionFromTracker(trackerUid: String, transactionUid: String) {
        customTrackerDao.unlinkTransactionFromTracker(trackerUid, transactionUid)
    }

    suspend fun upsertTrackerPerson(person: TrackerPersonEntity) =
        customTrackerDao.upsertTrackerPerson(person)

    suspend fun deleteTrackerPerson(personUid: String) =
        customTrackerDao.deleteTrackerPerson(personUid)

    suspend fun addTrackerSettlement(settlement: TrackerSettlementEntity) =
        customTrackerDao.upsertTrackerSettlement(settlement)

    suspend fun addTrackerAttachment(attachment: TrackerAttachmentEntity) =
        customTrackerDao.upsertTrackerAttachment(attachment)

    suspend fun deleteTrackerAttachment(attachmentUid: String) =
        customTrackerDao.deleteTrackerAttachment(attachmentUid)

    suspend fun exportJson(): String = NativeBackupService(database, prefsStore).exportJson()

    suspend fun restoreJson(json: String) {
        NativeBackupService(database, prefsStore).restoreJson(json)
    }

    suspend fun exportCsv(): String {
        val rows = transactions.first()
        return buildString {
            appendLine("id,accountId,type,amount,category,note,timestamp,paymentMethod,upiApp,warrantyDate")
            rows.forEach {
                appendLine(
                    listOf(
                        it.id,
                        it.accountId,
                        it.type,
                        it.amount,
                        it.category,
                        it.note,
                        it.timestamp,
                        it.paymentMethod,
                        it.upiApp,
                        it.warrantyDate
                    ).joinToString(",") { value -> "\"${value.toString().replace("\"", "\"\"")}\"" }
                )
            }
        }
    }

    suspend fun importCsv(csv: String): Int {
        val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.size > 1) { "CSV file is empty" }
        val header = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        val required = listOf("accountid", "type", "amount", "category")
        require(required.all { it in header }) { "CSV headers are invalid" }
        val index = header.withIndex().associate { it.value to it.index }
        var imported = 0
        database.withTransaction {
            lines.drop(1).forEach { line ->
                val values = parseCsvLine(line)
                fun value(name: String): String = values.getOrNull(index[name] ?: -1).orEmpty()
                val accountId = value("accountid").toLongOrNull() ?: return@forEach
                if (accounts.getAccountById(accountId) == null) return@forEach
                val amount = value("amount").toDoubleOrNull() ?: return@forEach
                val type = value("type").uppercase()
                if (amount <= 0 || type !in setOf("INCOME", "EXPENSE", "BORROW", "LEND")) return@forEach
                finance.insertTransaction(
                    TransactionEntity(
                        accountId = accountId,
                        type = type,
                        amount = amount,
                        amountPaise = MoneyPaise.toPaise(amount),
                        category = TracmioSecurityEngine.sanitizeInput(value("category"), 60).ifBlank { "Other" },
                        note = TracmioSecurityEngine.sanitizeInput(value("note"), 400),
                        timestamp = value("timestamp").toLongOrNull() ?: System.currentTimeMillis()
                    )
                )
                imported++
            }
        }
        require(imported > 0) { "No valid transactions found" }
        return imported
    }

    suspend fun clearAllData() {
        database.withTransaction {
            finance.deleteAllGoalContributions()
            finance.deleteAllLoanPayments()
            finance.deleteAllTransactions()
            finance.deleteAllBudgets()
            finance.deleteAllGoals()
            finance.deleteAllLoans()
            finance.deleteAllCategories()
            finance.deleteAllPersons()
            features.deleteAllQuickTemplates()
            features.deleteAllWishlistItems()
            features.deleteAllCashbackEntries()
            features.deleteAllValueItems()
            features.deleteAllReminders()
            accounts.deleteAll()
        }
        seedDefaultCategories()
        ensureDefaultAccountIfEmpty()
        seedDefaultQuickTemplatesIfEmpty()
    }

    suspend fun syncSettingsToRoom(s: LiquidGlassSettings) {
        val upiArr = JSONArray().apply { s.customUpiApps.forEach { put(it) } }.toString()
        val hubArr = JSONArray().apply { s.toolsHubIds.forEach { put(it) } }.toString()
        val cashArr = JSONArray().apply {
            s.cashCountsList.forEach { line ->
                put(JSONObject().apply {
                    put("denomType", line.denomType)
                    put("value", line.value)
                    put("count", line.count)
                })
            }
        }.toString()
        features.upsertAppSettings(
            AppSettingsEntity(
                id = 1,
                themePreset = s.themePreset,
                glassUiStyle = s.glassUiStyle,
                wallpaperPreset = s.wallpaperPreset,
                backgroundVisibility = s.backgroundVisibility,
                wallpaperBlurDp = s.wallpaperBlurDp,
                glowIntensity = s.glowIntensity,
                uiScale = s.uiScale,
                deviceMotionEnabled = s.deviceMotionEnabled,
                animationStyle = s.animationStyle,
                customPrimaryHex = s.customPrimaryHex,
                customSecondaryHex = s.customSecondaryHex,
                privacyMaskEnabled = s.privacyMaskEnabled,
                stealthModeActive = s.stealthModeActive,
                customWallpaperPath = s.customWallpaperPath,
                userName = s.userName,
                currencySymbol = s.currencySymbol,
                hasCompletedOnboarding = s.hasCompletedOnboarding,
                customUpiAppsJson = upiArr,
                toolsHubIdsJson = hubArr,
                roundUpEnabled = s.roundUpEnabled,
                roundUpGoalId = s.roundUpGoalId,
                roundUpStep = s.roundUpStep,
                monthlyBackupEnabled = s.monthlyBackupEnabled,
                monthlyBackupDay = s.monthlyBackupDay,
                monthlyBackupHour = s.monthlyBackupHour,
                monthlyBackupMinute = s.monthlyBackupMinute,
                cashCountsJson = cashArr,
                cashNote = s.cashNote,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun syncCustomRemindersToRoom(list: List<CustomReminderItem>) {
        database.withTransaction {
            val existing = features.getAllRemindersSync().filter { it.kind == "CUSTOM" }
            existing.forEach { features.deleteReminder(it) }
            list.forEach { item ->
                features.insertReminder(
                    ReminderEntity(
                        stableUid = item.id,
                        title = TracmioSecurityEngine.sanitizeInput(item.title, 80),
                        kind = "CUSTOM",
                        hour = item.hour.coerceIn(0, 23),
                        minute = item.minute.coerceIn(0, 59),
                        isEnabled = item.isEnabled
                    )
                )
            }
        }
    }

    suspend fun upsertReminderEntity(entity: ReminderEntity): Long =
        features.insertReminder(entity)

    suspend fun ensureSeededRoomSettingsAndReminders() {
        val store = prefsStore ?: return
        if (features.getAppSettingsSync() == null) {
            syncSettingsToRoom(store.settings.value)
        }
        if (features.getAllRemindersSync().isEmpty()) {
            syncCustomRemindersToRoom(store.customReminders.value)
        }
    }
}
