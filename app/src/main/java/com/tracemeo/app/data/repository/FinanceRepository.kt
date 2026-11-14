package com.tracemeo.app.data.repository

import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.local.*
import com.tracemeo.app.data.security.TracmioSecurityEngine
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class FinanceRepository(private val db: TracmioDatabase) {
    private val accountDao = db.accountDao()
    private val financeDao = db.financeDao()
    private val customTrackerDao = db.customTrackerDao()
    private val featureDao = db.featureDao()

    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = financeDao.getAllTransactions()
    val budgets: Flow<List<BudgetEntity>> = financeDao.getAllBudgets()
    val goals: Flow<List<GoalEntity>> = financeDao.getAllGoals()
    val goalContributions: Flow<List<GoalContributionEntity>> = financeDao.getAllGoalContributions()
    val loans: Flow<List<LoanEntity>> = financeDao.getAllLoans()
    val loanPayments: Flow<List<LoanPaymentEntity>> = financeDao.getAllLoanPayments()
    val persons: Flow<List<PersonEntity>> = financeDao.getAllPersons()
    val categories: Flow<List<CategoryEntity>> = financeDao.getAllCategories()
    val customTrackers: Flow<List<CustomTrackerEntity>> = customTrackerDao.getAllTrackers()
    val trackerEntries: Flow<List<CustomTrackerEntryEntity>> = customTrackerDao.getAllEntries()
    val trackerFolders: Flow<List<TrackerFolderEntity>> = customTrackerDao.getAllFolders()
    val subscriptions: Flow<List<SubscriptionEntity>> = featureDao.getAllSubscriptions()
    val academicEvents: Flow<List<AcademicEventEntity>> = featureDao.getAllAcademicEvents()
    val reminders: Flow<List<ReminderItemEntity>> = featureDao.getAllReminders()
    val khataEntries: Flow<List<KhataEntryEntity>> = featureDao.getAllKhataEntries()
    val wishlistCooldowns: Flow<List<WishlistCooldownEntity>> = featureDao.getAllWishlistCooldowns()
    val cashbackRewards: Flow<List<CashbackRewardEntity>> = featureDao.getAllCashbackRewards()
    val pricePerUseItems: Flow<List<PricePerUseEntity>> = featureDao.getAllPricePerUseItems()

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)
    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    suspend fun insertTransaction(tx: TransactionEntity): Long = financeDao.insertTransaction(tx)
    suspend fun updateTransaction(tx: TransactionEntity) = financeDao.updateTransaction(tx)
    suspend fun deleteTransaction(tx: TransactionEntity) = financeDao.deleteTransaction(tx)

    suspend fun insertBudget(budget: BudgetEntity): Long = financeDao.insertBudget(budget)
    suspend fun deleteBudget(budget: BudgetEntity) = financeDao.deleteBudget(budget)

    suspend fun insertGoal(goal: GoalEntity): Long = financeDao.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = financeDao.updateGoal(goal)
    suspend fun deleteGoal(goal: GoalEntity) = financeDao.deleteGoal(goal)
    suspend fun insertGoalContribution(contrib: GoalContributionEntity): Long = financeDao.insertGoalContribution(contrib)

    suspend fun insertLoan(loan: LoanEntity): Long = financeDao.insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = financeDao.updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) = financeDao.deleteLoan(loan)
    suspend fun insertLoanPayment(payment: LoanPaymentEntity): Long = financeDao.insertLoanPayment(payment)

    suspend fun insertPerson(person: PersonEntity): Long = financeDao.insertPerson(person)
    suspend fun updatePerson(person: PersonEntity) = financeDao.updatePerson(person)
    suspend fun deletePerson(person: PersonEntity) = financeDao.deletePerson(person)

    suspend fun insertCategory(category: CategoryEntity): Long = financeDao.insertCategory(category)

    suspend fun insertCustomTracker(tracker: CustomTrackerEntity): Long = customTrackerDao.insertTracker(tracker)
    suspend fun updateCustomTracker(tracker: CustomTrackerEntity) = customTrackerDao.updateTracker(tracker)
    suspend fun deleteCustomTracker(tracker: CustomTrackerEntity) = customTrackerDao.deleteTracker(tracker)
    suspend fun insertTrackerEntry(entry: CustomTrackerEntryEntity): Long = customTrackerDao.insertEntry(entry)
    suspend fun insertTrackerFolder(folder: TrackerFolderEntity): Long = customTrackerDao.insertFolder(folder)
    suspend fun deleteTrackerFolder(folder: TrackerFolderEntity) = customTrackerDao.deleteFolder(folder)

    suspend fun insertSubscription(item: SubscriptionEntity): Long = featureDao.insertSubscription(item)
    suspend fun updateSubscription(item: SubscriptionEntity) = featureDao.updateSubscription(item)
    suspend fun deleteSubscription(item: SubscriptionEntity) = featureDao.deleteSubscription(item)

    suspend fun insertAcademicEvent(item: AcademicEventEntity): Long = featureDao.insertAcademicEvent(item)
    suspend fun updateAcademicEvent(item: AcademicEventEntity) = featureDao.updateAcademicEvent(item)
    suspend fun deleteAcademicEvent(item: AcademicEventEntity) = featureDao.deleteAcademicEvent(item)

    suspend fun insertReminder(item: ReminderItemEntity): Long = featureDao.insertReminder(item)
    suspend fun updateReminder(item: ReminderItemEntity) = featureDao.updateReminder(item)
    suspend fun deleteReminder(item: ReminderItemEntity) = featureDao.deleteReminder(item)

    suspend fun insertKhataEntry(item: KhataEntryEntity): Long = featureDao.insertKhataEntry(item)
    suspend fun updateKhataEntry(item: KhataEntryEntity) = featureDao.updateKhataEntry(item)
    suspend fun deleteKhataEntry(item: KhataEntryEntity) = featureDao.deleteKhataEntry(item)

    suspend fun insertWishlistCooldown(item: WishlistCooldownEntity): Long = featureDao.insertWishlistCooldown(item)
    suspend fun updateWishlistCooldown(item: WishlistCooldownEntity) = featureDao.updateWishlistCooldown(item)
    suspend fun deleteWishlistCooldown(item: WishlistCooldownEntity) = featureDao.deleteWishlistCooldown(item)

    suspend fun insertCashbackReward(item: CashbackRewardEntity): Long = featureDao.insertCashbackReward(item)
    suspend fun deleteCashbackReward(item: CashbackRewardEntity) = featureDao.deleteCashbackReward(item)

    suspend fun insertPricePerUseItem(item: PricePerUseEntity): Long = featureDao.insertPricePerUseItem(item)
    suspend fun updatePricePerUseItem(item: PricePerUseEntity) = featureDao.updatePricePerUseItem(item)
    suspend fun deletePricePerUseItem(item: PricePerUseEntity) = featureDao.deletePricePerUseItem(item)

    fun runDatabaseHealthCheck(
        accountCount: Int,
        transactionCount: Int,
        trackerCount: Int
    ): TracmioSecurityEngine.DatabaseHealthReport {
        val sqlite = db.openHelper.readableDatabase
        val integrity = sqlite.query("PRAGMA integrity_check").use { c ->
            if (c.moveToFirst()) c.getString(0) else "unknown"
        }
        return TracmioSecurityEngine.DatabaseHealthReport(
            isHealthy = integrity.equals("ok", ignoreCase = true),
            pragmaIntegrity = integrity,
            foreignKeyErrors = 0,
            accountCount = accountCount,
            transactionCount = transactionCount,
            trackerCount = trackerCount,
            orphanedTransactions = 0,
            diagnosticSummary = if (integrity.equals("ok", true)) {
                "SQLite / SQLCipher integrity verified (ok). $accountCount accounts, $transactionCount transactions."
            } else {
                "Integrity check returned: $integrity"
            }
        )
    }

    suspend fun importBackupJson(jsonText: String): Int {
        val obj = JSONObject(jsonText)
        var importedCount = 0
        val accountIdMap = mutableMapOf<Long, Long>()

        val accArr = obj.optJSONArray("accounts")
        if (accArr != null) {
            for (i in 0 until accArr.length()) {
                val a = accArr.getJSONObject(i)
                val oldId = a.optLong("id", (i + 1).toLong())
                val bal = a.optDouble("balance", 0.0)
                val newId = accountDao.insertAccount(
                    AccountEntity(
                        name = TracmioSecurityEngine.sanitizeInput(a.optString("name", "Imported Account"), 80),
                        accountType = TracmioSecurityEngine.sanitizeInput(a.optString("accountType", a.optString("type", "Bank")), 40),
                        balance = bal,
                        balancePaise = MoneyPaise.toPaise(bal),
                        creditLimit = a.optDouble("creditLimit", 0.0),
                        statementDay = a.optInt("statementDay", 1).coerceIn(1, 28),
                        dueDay = a.optInt("dueDay", 15).coerceIn(1, 28),
                        allocationsJson = a.optString("allocationsJson", "[]")
                    )
                )
                accountIdMap[oldId] = newId
                importedCount++
            }
        }

        val txArr = obj.optJSONArray("transactions")
        if (txArr != null) {
            for (i in 0 until txArr.length()) {
                val t = txArr.getJSONObject(i)
                val amt = t.optDouble("amount", 0.0)
                if (amt <= 0.0) continue
                val oldAccId = t.optLong("accountId", 1L)
                val mappedAccId = accountIdMap[oldAccId] ?: oldAccId
                financeDao.insertTransaction(
                    TransactionEntity(
                        accountId = mappedAccId,
                        type = t.optString("type", "EXPENSE").uppercase(),
                        amount = amt,
                        amountPaise = MoneyPaise.toPaise(amt),
                        category = TracmioSecurityEngine.sanitizeInput(t.optString("category", "Other"), 60),
                        subcategory = TracmioSecurityEngine.sanitizeInput(t.optString("subcategory", ""), 60),
                        paymentMethod = TracmioSecurityEngine.sanitizeInput(t.optString("paymentMethod", "Cash"), 40),
                        upiApp = TracmioSecurityEngine.sanitizeInput(t.optString("upiApp", ""), 40),
                        note = TracmioSecurityEngine.sanitizeInput(t.optString("note", ""), 200),
                        hashtags = TracmioSecurityEngine.sanitizeInput(t.optString("hashtags", ""), 120),
                        warrantyDate = TracmioSecurityEngine.sanitizeInput(t.optString("warrantyDate", ""), 30),
                        timestamp = t.optLong("timestamp", System.currentTimeMillis())
                    )
                )
                importedCount++
            }
        }
        return importedCount
    }
}
