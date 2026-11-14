package com.tracemeo.app.data.repository

import androidx.room.withTransaction
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

    suspend fun <T> runInTransaction(block: suspend () -> T): T = db.withTransaction(block)

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)
    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    suspend fun getTransactionById(id: Long): TransactionEntity? = financeDao.getTransactionById(id)
    suspend fun getTransactionsByTransferId(transferId: String): List<TransactionEntity> = financeDao.getTransactionsByTransferId(transferId)
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
        return db.withTransaction {
            var importedCount = 0
            val accountIdMap = mutableMapOf<Long, Long>()
            val goalIdMap = mutableMapOf<Long, Long>()
            val loanIdMap = mutableMapOf<Long, Long>()
            val folderIdMap = mutableMapOf<Long, Long>()
            val trackerIdMap = mutableMapOf<Long, Long>()

            val accArr = obj.optJSONArray("accounts")
            if (accArr != null) {
                for (i in 0 until accArr.length()) {
                    val a = accArr.getJSONObject(i)
                    val oldId = a.optLong("id", (i + 1).toLong())
                    val bal = a.optDouble("balance", 0.0)
                    val limit = a.optDouble("creditLimit", 0.0)
                    val accType = TracmioSecurityEngine.sanitizeInput(a.optString("accountType", a.optString("type", "Bank")), 40)
                    val newId = accountDao.insertAccount(
                        AccountEntity(
                            name = TracmioSecurityEngine.sanitizeInput(a.optString("name", "Imported Account"), 80),
                            accountType = accType,
                            type = accType,
                            balance = bal,
                            balancePaise = MoneyPaise.toPaise(bal),
                            creditLimit = limit,
                            creditLimitPaise = MoneyPaise.toPaise(limit),
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
                    val oldTransferAccId = if (t.has("transferAccountId") && !t.isNull("transferAccountId")) t.optLong("transferAccountId", -1L).takeIf { it > 0 } else null
                    val mappedTransferAccId = oldTransferAccId?.let { accountIdMap[it] ?: it }
                    val transferId = if (t.has("transferId") && !t.isNull("transferId")) TracmioSecurityEngine.sanitizeInput(t.optString("transferId", ""), 64).ifBlank { null } else null
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
                            allocationId = TracmioSecurityEngine.sanitizeInput(t.optString("allocationId", ""), 40),
                            transferAccountId = mappedTransferAccId,
                            transferId = transferId,
                            timestamp = t.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                    importedCount++
                }
            }

            val budgetsArr = obj.optJSONArray("budgets")
            if (budgetsArr != null) {
                for (i in 0 until budgetsArr.length()) {
                    val b = budgetsArr.getJSONObject(i)
                    val limit = b.optDouble("monthlyLimit", 0.0)
                    if (limit > 0.0) {
                        financeDao.insertBudget(
                            BudgetEntity(
                                category = TracmioSecurityEngine.sanitizeInput(b.optString("category", "Other"), 60),
                                monthlyLimit = limit,
                                limitPaise = MoneyPaise.toPaise(limit),
                                month = TracmioSecurityEngine.sanitizeInput(b.optString("month", b.optString("monthKey", "")), 20)
                            )
                        )
                        importedCount++
                    }
                }
            }

            val goalsArr = obj.optJSONArray("goals")
            if (goalsArr != null) {
                for (i in 0 until goalsArr.length()) {
                    val g = goalsArr.getJSONObject(i)
                    val oldId = g.optLong("id", (i + 1).toLong())
                    val target = g.optDouble("targetAmount", 0.0)
                    val current = g.optDouble("currentAmount", 0.0)
                    val title = TracmioSecurityEngine.sanitizeInput(g.optString("title", g.optString("name", "Goal")), 80)
                    val newId = financeDao.insertGoal(
                        GoalEntity(
                            name = title,
                            title = title,
                            targetAmount = target,
                            targetPaise = MoneyPaise.toPaise(target),
                            currentAmount = current,
                            currentPaise = MoneyPaise.toPaise(current),
                            deadline = TracmioSecurityEngine.sanitizeInput(g.optString("deadline", ""), 40),
                            icon = TracmioSecurityEngine.sanitizeInput(g.optString("icon", "🎯"), 12),
                            colorHex = TracmioSecurityEngine.sanitizeInput(g.optString("colorHex", "#00F5D4"), 16)
                        )
                    )
                    goalIdMap[oldId] = newId
                    importedCount++
                }
            }

            val goalContribArr = obj.optJSONArray("goalContributions")
            if (goalContribArr != null) {
                for (i in 0 until goalContribArr.length()) {
                    val c = goalContribArr.getJSONObject(i)
                    val oldGoalId = c.optLong("goalId", 1L)
                    val amt = c.optDouble("amount", 0.0)
                    if (amt > 0.0) {
                        financeDao.insertGoalContribution(
                            GoalContributionEntity(
                                goalId = goalIdMap[oldGoalId] ?: oldGoalId,
                                amount = amt,
                                note = TracmioSecurityEngine.sanitizeInput(c.optString("note", ""), 120),
                                timestamp = c.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                        importedCount++
                    }
                }
            }

            val loansArr = obj.optJSONArray("loans")
            if (loansArr != null) {
                for (i in 0 until loansArr.length()) {
                    val l = loansArr.getJSONObject(i)
                    val oldId = l.optLong("id", (i + 1).toLong())
                    val principal = l.optDouble("principal", 0.0)
                    val emi = l.optDouble("monthlyPayment", 0.0)
                    val name = TracmioSecurityEngine.sanitizeInput(l.optString("name", l.optString("lender", "Loan")), 80)
                    val newId = financeDao.insertLoan(
                        LoanEntity(
                            name = name,
                            lender = name,
                            principal = principal,
                            principalPaise = MoneyPaise.toPaise(principal),
                            interestRate = l.optDouble("interestRate", 0.0),
                            tenureMonths = l.optInt("tenureMonths", 12).coerceAtLeast(1),
                            monthlyPayment = emi,
                            emiPaise = MoneyPaise.toPaise(emi),
                            isClosed = l.optBoolean("isClosed", false)
                        )
                    )
                    loanIdMap[oldId] = newId
                    importedCount++
                }
            }

            val loanPayArr = obj.optJSONArray("loanPayments")
            if (loanPayArr != null) {
                for (i in 0 until loanPayArr.length()) {
                    val lp = loanPayArr.getJSONObject(i)
                    val oldLoanId = lp.optLong("loanId", 1L)
                    val amt = lp.optDouble("amount", 0.0)
                    if (amt > 0.0) {
                        financeDao.insertLoanPayment(
                            LoanPaymentEntity(
                                loanId = loanIdMap[oldLoanId] ?: oldLoanId,
                                amount = amt,
                                amountPaise = MoneyPaise.toPaise(amt),
                                note = TracmioSecurityEngine.sanitizeInput(lp.optString("note", ""), 120),
                                timestamp = lp.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                        importedCount++
                    }
                }
            }

            val personsArr = obj.optJSONArray("persons")
            if (personsArr != null) {
                for (i in 0 until personsArr.length()) {
                    val p = personsArr.getJSONObject(i)
                    val bal = p.optDouble("netBalance", 0.0)
                    financeDao.insertPerson(
                        PersonEntity(
                            name = TracmioSecurityEngine.sanitizeInput(p.optString("name", "Contact"), 80),
                            phone = TracmioSecurityEngine.sanitizeInput(p.optString("phone", ""), 20),
                            upiId = TracmioSecurityEngine.sanitizeInput(p.optString("upiId", ""), 80),
                            netBalance = bal,
                            netBalancePaise = MoneyPaise.toPaise(bal),
                            notes = TracmioSecurityEngine.sanitizeInput(p.optString("notes", ""), 160)
                        )
                    )
                    importedCount++
                }
            }

            val foldersArr = obj.optJSONArray("trackerFolders")
            if (foldersArr != null) {
                for (i in 0 until foldersArr.length()) {
                    val f = foldersArr.getJSONObject(i)
                    val oldId = f.optLong("id", (i + 1).toLong())
                    val newId = customTrackerDao.insertFolder(
                        TrackerFolderEntity(
                            folderName = TracmioSecurityEngine.sanitizeInput(f.optString("folderName", "Folder"), 60),
                            icon = TracmioSecurityEngine.sanitizeInput(f.optString("icon", "📂"), 12),
                            colorHex = TracmioSecurityEngine.sanitizeInput(f.optString("colorHex", "#00F5D4"), 16),
                            sortOrder = f.optInt("sortOrder", i)
                        )
                    )
                    folderIdMap[oldId] = newId
                    importedCount++
                }
            }

            val trackersArr = obj.optJSONArray("customTrackers")
            if (trackersArr != null) {
                for (i in 0 until trackersArr.length()) {
                    val t = trackersArr.getJSONObject(i)
                    val oldId = t.optLong("id", (i + 1).toLong())
                    val oldFolder = t.optLong("folderId", 0L)
                    val newId = customTrackerDao.insertTracker(
                        CustomTrackerEntity(
                            title = TracmioSecurityEngine.sanitizeInput(t.optString("title", "Tracker"), 80),
                            trackerType = TracmioSecurityEngine.sanitizeInput(t.optString("trackerType", "SAVINGS"), 30),
                            targetValue = t.optDouble("targetValue", 0.0),
                            currentValue = t.optDouble("currentValue", 0.0),
                            unit = TracmioSecurityEngine.sanitizeInput(t.optString("unit", "₹"), 12),
                            icon = TracmioSecurityEngine.sanitizeInput(t.optString("icon", "🎯"), 12),
                            colorHex = TracmioSecurityEngine.sanitizeInput(t.optString("colorHex", "#00F5D4"), 16),
                            folderId = if (oldFolder > 0L) (folderIdMap[oldFolder] ?: 0L) else 0L,
                            isPinned = t.optBoolean("isPinned", false)
                        )
                    )
                    trackerIdMap[oldId] = newId
                    importedCount++
                }
            }

            val entriesArr = obj.optJSONArray("trackerEntries")
            if (entriesArr != null) {
                for (i in 0 until entriesArr.length()) {
                    val e = entriesArr.getJSONObject(i)
                    val oldT = e.optLong("trackerId", 1L)
                    customTrackerDao.insertEntry(
                        CustomTrackerEntryEntity(
                            trackerId = trackerIdMap[oldT] ?: oldT,
                            deltaValue = e.optDouble("deltaValue", 0.0),
                            note = TracmioSecurityEngine.sanitizeInput(e.optString("note", ""), 120),
                            timestamp = e.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                    importedCount++
                }
            }

            val subsArr = obj.optJSONArray("subscriptions")
            if (subsArr != null) {
                for (i in 0 until subsArr.length()) {
                    val s = subsArr.getJSONObject(i)
                    val amt = s.optDouble("amount", 0.0)
                    featureDao.insertSubscription(
                        SubscriptionEntity(
                            name = TracmioSecurityEngine.sanitizeInput(s.optString("name", "Subscription"), 80),
                            amount = amt,
                            amountPaise = MoneyPaise.toPaise(amt),
                            billingCycle = TracmioSecurityEngine.sanitizeInput(s.optString("billingCycle", "Monthly"), 20),
                            nextDueDateMillis = s.optLong("nextDueDateMillis", System.currentTimeMillis()),
                            category = TracmioSecurityEngine.sanitizeInput(s.optString("category", "SIM, WiFi & Bills"), 40),
                            isSimOrWifi = s.optBoolean("isSimOrWifi", false),
                            validityDays = s.optInt("validityDays", 28),
                            isActive = s.optBoolean("isActive", true)
                        )
                    )
                    importedCount++
                }
            }

            val acadArr = obj.optJSONArray("academicEvents")
            if (acadArr != null) {
                for (i in 0 until acadArr.length()) {
                    val ev = acadArr.getJSONObject(i)
                    featureDao.insertAcademicEvent(
                        AcademicEventEntity(
                            title = TracmioSecurityEngine.sanitizeInput(ev.optString("title", "Academic Fee"), 80),
                            feeAmount = ev.optDouble("feeAmount", 0.0),
                            paidAmount = ev.optDouble("paidAmount", 0.0),
                            dueDate = TracmioSecurityEngine.sanitizeInput(ev.optString("dueDate", ""), 30),
                            semester = TracmioSecurityEngine.sanitizeInput(ev.optString("semester", "Current"), 30),
                            isPaid = ev.optBoolean("isPaid", false)
                        )
                    )
                    importedCount++
                }
            }

            val remArr = obj.optJSONArray("reminders")
            if (remArr != null) {
                for (i in 0 until remArr.length()) {
                    val r = remArr.getJSONObject(i)
                    featureDao.insertReminder(
                        ReminderItemEntity(
                            title = TracmioSecurityEngine.sanitizeInput(r.optString("title", "Reminder"), 100),
                            amount = r.optDouble("amount", 0.0),
                            dueDate = TracmioSecurityEngine.sanitizeInput(r.optString("dueDate", ""), 30),
                            timeHour = r.optInt("timeHour", 9),
                            timeMinute = r.optInt("timeMinute", 0),
                            isCompleted = r.optBoolean("isCompleted", false),
                            isRecurring = r.optBoolean("isRecurring", false)
                        )
                    )
                    importedCount++
                }
            }

            val khataArr = obj.optJSONArray("khataEntries")
            if (khataArr != null) {
                for (i in 0 until khataArr.length()) {
                    val k = khataArr.getJSONObject(i)
                    featureDao.insertKhataEntry(
                        KhataEntryEntity(
                            shopOrFriend = TracmioSecurityEngine.sanitizeInput(k.optString("shopOrFriend", "Shop"), 80),
                            itemDescription = TracmioSecurityEngine.sanitizeInput(k.optString("itemDescription", ""), 120),
                            amount = k.optDouble("amount", 0.0),
                            isSettled = k.optBoolean("isSettled", false),
                            timestamp = k.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                    importedCount++
                }
            }

            val wishlistArr = obj.optJSONArray("wishlistCooldowns")
            if (wishlistArr != null) {
                for (i in 0 until wishlistArr.length()) {
                    val w = wishlistArr.getJSONObject(i)
                    featureDao.insertWishlistCooldown(
                        WishlistCooldownEntity(
                            itemName = TracmioSecurityEngine.sanitizeInput(w.optString("itemName", "Wishlist Item"), 80),
                            price = w.optDouble("price", 0.0),
                            createdAtMillis = w.optLong("createdAtMillis", System.currentTimeMillis()),
                            status = TracmioSecurityEngine.sanitizeInput(w.optString("status", "COOLING"), 24)
                        )
                    )
                    importedCount++
                }
            }

            val cashbackArr = obj.optJSONArray("cashbackRewards")
            if (cashbackArr != null) {
                for (i in 0 until cashbackArr.length()) {
                    val c = cashbackArr.getJSONObject(i)
                    featureDao.insertCashbackReward(
                        CashbackRewardEntity(
                            sourceApp = TracmioSecurityEngine.sanitizeInput(c.optString("sourceApp", "UPI App"), 50),
                            amount = c.optDouble("amount", 0.0),
                            note = TracmioSecurityEngine.sanitizeInput(c.optString("note", ""), 120),
                            timestamp = c.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                    importedCount++
                }
            }

            val ppuArr = obj.optJSONArray("pricePerUseItems")
            if (ppuArr != null) {
                for (i in 0 until ppuArr.length()) {
                    val p = ppuArr.getJSONObject(i)
                    featureDao.insertPricePerUseItem(
                        PricePerUseEntity(
                            itemName = TracmioSecurityEngine.sanitizeInput(p.optString("itemName", "Item"), 80),
                            purchasePrice = p.optDouble("purchasePrice", 0.0),
                            usageCount = p.optInt("usageCount", 1).coerceAtLeast(1),
                            targetUses = p.optInt("targetUses", 30).coerceAtLeast(1),
                            createdAtMillis = p.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                    importedCount++
                }
            }

            importedCount
        }
    }
}
