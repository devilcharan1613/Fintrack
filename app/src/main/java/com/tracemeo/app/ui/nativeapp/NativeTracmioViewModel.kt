package com.tracemeo.app.ui.nativeapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tracemeo.app.data.*
import com.tracemeo.app.data.local.*
import com.tracemeo.app.data.repository.FinanceRepository
import com.tracemeo.app.data.security.AppLockManager
import com.tracemeo.app.data.security.TracmioSecurityEngine
import com.tracemeo.app.data.services.NativeBackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NativeTracmioViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val prefsStore = PreferencesAndThemeStore(appContext)
    private val appLockManager = AppLockManager(appContext)
    private val db = TracmioDatabase.getInstance(appContext)
    private val repository = FinanceRepository(db)

    val settingsFlow: StateFlow<LiquidGlassSettings> = prefsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = prefsStore.getInitialSettings()
    )

    private val quickChipsFlow = MutableStateFlow(prefsStore.getQuickSpendChips())
    private val pendingRefundsFlow = MutableStateFlow(prefsStore.getPendingRefunds())
    private val pinnedToolIdsFlow = MutableStateFlow(prefsStore.getPinnedToolIds())
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val deletedTxFlow = MutableStateFlow<TransactionEntity?>(null)
    private val deletedChipFlow = MutableStateFlow<QuickSpendChip?>(null)
    private val isAppLockedFlow = MutableStateFlow(appLockManager.getState().isEnabled)
    private val appLockStateFlow = MutableStateFlow(appLockManager.getState())
    private val pendingNotificationActionFlow = MutableStateFlow<String?>(null)
    private val dailyReminderEnabledFlow = MutableStateFlow(prefsStore.isDailyReminderEnabled())

    private val coreLedgerFlow = combine(
        repository.accounts,
        repository.transactions,
        repository.budgets,
        repository.goals,
        repository.loans
    ) { accounts, transactions, budgets, goals, loans ->
        CoreLedgerSnapshot(accounts, transactions, budgets, goals, loans)
    }

    private val planningExtrasFlow = combine(
        repository.goalContributions,
        repository.loanPayments,
        repository.persons,
        repository.customTrackers,
        repository.trackerFolders
    ) { contribs, payments, persons, trackers, folders ->
        PlanningExtrasSnapshot(contribs, payments, persons, trackers, folders)
    }

    private val toolsEntitiesFlow = combine(
        repository.subscriptions,
        repository.academicEvents,
        repository.reminders,
        repository.khataEntries,
        repository.wishlistCooldowns
    ) { subs, academic, reminders, khata, wishlist ->
        ToolsEntitiesSnapshot(subs, academic, reminders, khata, wishlist)
    }

    private val rewardAndTrackFlow = combine(
        repository.cashbackRewards,
        repository.pricePerUseItems,
        repository.trackerEntries,
        quickChipsFlow,
        pendingRefundsFlow
    ) { cashback, ppu, trackerEntries, chips, refunds ->
        RewardAndTrackSnapshot(cashback, ppu, trackerEntries, chips, refunds)
    }

    private val uiFlagsFlow = combine(
        statusMessageFlow,
        deletedTxFlow,
        deletedChipFlow,
        isAppLockedFlow,
        appLockStateFlow
    ) { msg, delTx, delChip, locked, lockState ->
        UiFlagsSnapshot(msg, delTx, delChip, locked, lockState)
    }

    private val auxFlagsFlow = combine(
        pinnedToolIdsFlow,
        pendingNotificationActionFlow,
        dailyReminderEnabledFlow
    ) { pinned, action, dailyRem ->
        Triple(pinned, action, dailyRem)
    }

    val uiState: StateFlow<NativeTracmioUiState> = combine(
        coreLedgerFlow,
        planningExtrasFlow,
        toolsEntitiesFlow,
        rewardAndTrackFlow,
        uiFlagsFlow,
        auxFlagsFlow
    ) { array ->
        val core = array[0] as CoreLedgerSnapshot
        val plan = array[1] as PlanningExtrasSnapshot
        val tools = array[2] as ToolsEntitiesSnapshot
        val rew = array[3] as RewardAndTrackSnapshot
        val flags = array[4] as UiFlagsSnapshot
        @Suppress("UNCHECKED_CAST")
        val aux = array[5] as Triple<List<String>, String?, Boolean>

        val totalBal = core.accounts.sumOf { FinancialCalculator.balance(it, core.transactions) }
        val totalInc = core.transactions.filter { it.type.equals("INCOME", true) }.sumOf { it.amount }
        val totalExp = core.transactions.filter { it.type.equals("EXPENSE", true) }.sumOf { it.amount }

        NativeTracmioUiState(
            accounts = core.accounts,
            transactions = core.transactions,
            budgets = core.budgets,
            goals = core.goals,
            goalContributions = plan.goalContributions,
            loans = core.loans,
            loanPayments = plan.loanPayments,
            persons = plan.persons,
            customTrackers = plan.customTrackers,
            trackerEntries = rew.trackerEntries,
            trackerFolders = plan.trackerFolders,
            subscriptions = tools.subscriptions,
            academicEvents = tools.academicEvents,
            reminders = tools.reminders,
            khataEntries = tools.khataEntries,
            wishlistCooldowns = tools.wishlistCooldowns,
            cashbackRewards = rew.cashbackRewards,
            pricePerUseItems = rew.pricePerUseItems,
            pendingRefunds = rew.pendingRefunds,
            quickSpendChips = rew.quickSpendChips,
            totalBalance = totalBal,
            totalIncome = totalInc,
            totalExpense = totalExp,
            statusMessage = flags.statusMessage,
            recentlyDeletedTransaction = flags.deletedTx,
            recentlyDeletedChip = flags.deletedChip,
            hasPendingUndo = flags.deletedTx != null || flags.deletedChip != null,
            isAppLocked = flags.isAppLocked,
            appLockState = flags.appLockState,
            pendingNotificationAction = aux.second,
            pinnedToolIds = aux.first,
            dailyReminderEnabled = aux.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NativeTracmioUiState(
            quickSpendChips = quickChipsFlow.value,
            pendingRefunds = pendingRefundsFlow.value,
            isAppLocked = isAppLockedFlow.value,
            appLockState = appLockStateFlow.value,
            pinnedToolIds = pinnedToolIdsFlow.value,
            dailyReminderEnabled = dailyReminderEnabledFlow.value
        )
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            LegacyMigrationGuard.runOneTimeLegacyMigrationIfNeeded(appContext, repository)
            val existingAccounts = repository.accounts.first()
            if (existingAccounts.isEmpty() && !prefsStore.hasSeededDefaults()) {
                repository.insertAccount(
                    AccountEntity(
                        name = "Cash Wallet",
                        accountType = "Wallet",
                        balance = 0.0,
                        balancePaise = 0L,
                        colorHex = "#00F5D4"
                    )
                )
                repository.insertAccount(
                    AccountEntity(
                        name = "Primary Bank",
                        accountType = "Bank",
                        balance = 0.0,
                        balancePaise = 0L,
                        colorHex = "#38BDF8"
                    )
                )
                prefsStore.markSeededDefaults()
            }
        }
    }

    fun updateSettings(transform: (LiquidGlassSettings) -> LiquidGlassSettings) = prefsStore.updateSettings(transform)
    fun togglePrivacyMask() = prefsStore.updatePrivacyMask(!settingsFlow.value.privacyMaskActive)
    fun toggleStealthMode() = prefsStore.updateStealthMode(!settingsFlow.value.stealthModeActive)
    fun toggleRoundUp(enabled: Boolean) = prefsStore.updateSettings { it.copy(roundUpEnabled = enabled) }
    fun completeOnboarding(name: String, currency: String) = prefsStore.completeOnboarding(name, currency)
    fun triggerNotificationAction(action: String) { pendingNotificationActionFlow.value = action }
    fun consumePendingNotificationAction() { pendingNotificationActionFlow.value = null }
    fun clearStatusMessage() { statusMessageFlow.value = null }
    fun postStatus(message: String) { statusMessageFlow.value = message }

    fun toggleToolPin(toolId: String) {
        val current = pinnedToolIdsFlow.value.toMutableList()
        if (current.contains(toolId)) current.remove(toolId) else current.add(toolId)
        pinnedToolIdsFlow.value = current
        prefsStore.savePinnedToolIds(current)
    }

    fun executeQuickSpendChip(chip: QuickSpendChip) {
        viewModelScope.launch(Dispatchers.IO) {
            val accounts = repository.accounts.first()
            val targetAcc = accounts.firstOrNull { it.accountType.equals("Wallet", true) || it.accountType.equals("Cash", true) }
                ?: accounts.firstOrNull()
                ?: return@launch
            val tx = TransactionEntity(
                accountId = targetAcc.id,
                type = "EXPENSE",
                amount = chip.amount,
                amountPaise = MoneyPaise.toPaise(chip.amount),
                category = chip.category,
                paymentMethod = if (targetAcc.accountType.equals("Cash", true)) "Cash" else "UPI",
                note = "${chip.emoji} ${chip.title} (Quick Spend)"
            )
            val id = repository.insertTransaction(tx)
            applyRoundUpIfEnabled(chip.amount)
            deletedTxFlow.value = tx.copy(id = id)
            deletedChipFlow.value = null
            statusMessageFlow.value = "Logged ${chip.title} (${settingsFlow.value.currencySymbol}${chip.amount.toInt()})"
        }
    }

    fun addQuickSpendChip(emoji: String, title: String, amount: Double, category: String = "Food & Chai") {
        val newChip = QuickSpendChip(
            id = "chip_${System.currentTimeMillis()}",
            emoji = emoji,
            title = TracmioSecurityEngine.sanitizeInput(title, 30),
            amount = amount,
            category = category
        )
        val updated = quickChipsFlow.value + newChip
        quickChipsFlow.value = updated
        prefsStore.saveQuickSpendChips(updated)
        statusMessageFlow.value = "Added Quick Spend chip: $title"
    }

    fun deleteQuickSpendChip(chip: QuickSpendChip) {
        deletedChipFlow.value = chip
        deletedTxFlow.value = null
        val updated = quickChipsFlow.value.filterNot { it.id == chip.id }
        quickChipsFlow.value = updated
        prefsStore.saveQuickSpendChips(updated)
        statusMessageFlow.value = "Removed chip ${chip.title}"
    }

    fun performPendingUndo() {
        val delChip = deletedChipFlow.value
        if (delChip != null) {
            val restored = quickChipsFlow.value + delChip
            quickChipsFlow.value = restored
            prefsStore.saveQuickSpendChips(restored)
            deletedChipFlow.value = null
            statusMessageFlow.value = "Restored chip ${delChip.title}"
            return
        }
        val delTx = deletedTxFlow.value
        if (delTx != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val existing = repository.transactions.first().any { it.id == delTx.id }
                if (existing) {
                    repository.deleteTransaction(delTx)
                    statusMessageFlow.value = "Undid quick spend"
                } else {
                    repository.insertTransaction(delTx.copy(id = 0))
                    statusMessageFlow.value = "Restored deleted transaction"
                }
                deletedTxFlow.value = null
            }
        }
    }

    fun saveAccount(
        editingId: Long?,
        name: String,
        accountType: String,
        balance: Double,
        creditLimit: Double = 0.0,
        statementDay: Int = 1,
        dueDay: Int = 15
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanName = TracmioSecurityEngine.sanitizeInput(name, 60)
            if (editingId != null) {
                val old = repository.getAccountById(editingId)
                if (old != null) {
                    repository.updateAccount(
                        old.copy(
                            name = cleanName,
                            accountType = accountType,
                            type = accountType,
                            balance = balance,
                            balancePaise = MoneyPaise.toPaise(balance),
                            creditLimit = creditLimit,
                            creditLimitPaise = MoneyPaise.toPaise(creditLimit),
                            statementDay = statementDay,
                            dueDay = dueDay,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    statusMessageFlow.value = "Updated account $cleanName"
                    return@launch
                }
            }
            repository.insertAccount(
                AccountEntity(
                    name = cleanName,
                    accountType = accountType,
                    type = accountType,
                    balance = balance,
                    balancePaise = MoneyPaise.toPaise(balance),
                    creditLimit = creditLimit,
                    creditLimitPaise = MoneyPaise.toPaise(creditLimit),
                    statementDay = statementDay,
                    dueDay = dueDay
                )
            )
            statusMessageFlow.value = "Created account $cleanName"
        }
    }

    fun updateAccountAllocations(account: AccountEntity, allocations: List<AccountAllocation>) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = NativeLedgerCalculator.serializeAllocations(allocations)
            repository.updateAccount(account.copy(allocationsJson = json, updatedAt = System.currentTimeMillis()))
            statusMessageFlow.value = "Updated allocations for ${account.name}"
        }
    }

    fun reconcileAccountBalance(account: AccountEntity, targetActualBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val txns = repository.transactions.first()
            val currentBal = FinancialCalculator.balance(account, txns)
            val delta = targetActualBalance - currentBal
            if (kotlin.math.abs(delta) >= 0.01) {
                repository.insertTransaction(
                    TransactionEntity(
                        accountId = account.id,
                        type = if (delta >= 0) "INCOME" else "EXPENSE",
                        amount = kotlin.math.abs(delta),
                        amountPaise = MoneyPaise.toPaise(kotlin.math.abs(delta)),
                        category = "Balance Adjustment",
                        paymentMethod = account.accountType,
                        note = "Reconciled ${account.name} to ${settingsFlow.value.currencySymbol}${"%,.2f".format(targetActualBalance)}"
                    )
                )
                statusMessageFlow.value = "Reconciled ${account.name}"
            }
        }
    }

    fun applyCashDenominationCount(lines: List<CashCountLine>, note: String, accountId: Long?) {
        val total = NativeLedgerCalculator.computeCashDenominationTotal(lines)
        viewModelScope.launch(Dispatchers.IO) {
            val accounts = repository.accounts.first()
            val target = accounts.firstOrNull { it.id == accountId }
                ?: accounts.firstOrNull { it.accountType.equals("Cash", true) || it.accountType.equals("Wallet", true) }
                ?: return@launch
            reconcileAccountBalance(target, total)
            statusMessageFlow.value = "Cash count applied (${settingsFlow.value.currencySymbol}${"%,.0f".format(total)})"
        }
    }

    fun performAtmWithdrawal(amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val accounts = repository.accounts.first()
            val bank = accounts.firstOrNull { it.accountType.equals("Bank", true) } ?: accounts.firstOrNull() ?: return@launch
            val cash = accounts.firstOrNull { it.accountType.equals("Cash", true) || it.accountType.equals("Wallet", true) }
                ?: accounts.lastOrNull() ?: return@launch
            if (bank.id == cash.id) {
                statusMessageFlow.value = "Add a separate Bank and Cash/Wallet account to withdraw"
                return@launch
            }
            transferFunds(bank.id, cash.id, amount, "🏧 1-Tap ATM Withdrawal")
        }
    }

    fun transferFunds(fromId: Long, toId: Long, amount: Double, note: String) {
        if (amount <= 0.0 || fromId == toId) return
        viewModelScope.launch(Dispatchers.IO) {
            val xferId = "xfer_${System.currentTimeMillis()}"
            repository.insertTransaction(
                TransactionEntity(
                    accountId = fromId,
                    type = "TRANSFER_OUT",
                    amount = amount,
                    amountPaise = MoneyPaise.toPaise(amount),
                    category = "Transfer",
                    transferAccountId = toId,
                    transferId = xferId,
                    paymentMethod = "Bank Transfer",
                    note = TracmioSecurityEngine.sanitizeInput(note, 150)
                )
            )
            repository.insertTransaction(
                TransactionEntity(
                    accountId = toId,
                    type = "TRANSFER_IN",
                    amount = amount,
                    amountPaise = MoneyPaise.toPaise(amount),
                    category = "Transfer",
                    transferAccountId = fromId,
                    transferId = xferId,
                    paymentMethod = "Cash",
                    note = TracmioSecurityEngine.sanitizeInput(note, 150)
                )
            )
            statusMessageFlow.value = "Transferred ${settingsFlow.value.currencySymbol}${"%,.0f".format(amount)}"
        }
    }

    fun saveTransaction(
        editingId: Long?,
        accountId: Long,
        type: String,
        amount: Double,
        category: String,
        subcategory: String,
        paymentMethod: String,
        upiApp: String,
        note: String,
        warrantyDate: String,
        allocationId: String,
        cashPortion: Double,
        upiPortion: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanNote = TracmioSecurityEngine.sanitizeInput(note, 300)
            val hashtags = Regex("#[A-Za-z0-9_]+").findAll(cleanNote).joinToString(" ") { it.value }

            if (paymentMethod == "Half-Cash + Half-UPI Split" && editingId == null && (cashPortion > 0.0 || upiPortion > 0.0)) {
                val cashAmt = if (cashPortion > 0) cashPortion else amount / 2.0
                val upiAmt = if (upiPortion > 0) upiPortion else (amount - cashAmt).coerceAtLeast(0.0)
                val accounts = repository.accounts.first()
                val cashAcc = accounts.firstOrNull { it.accountType.equals("Cash", true) || it.accountType.equals("Wallet", true) }?.id ?: accountId
                if (cashAmt > 0.0) {
                    repository.insertTransaction(
                        TransactionEntity(
                            accountId = cashAcc, type = type, amount = cashAmt, amountPaise = MoneyPaise.toPaise(cashAmt),
                            category = category, subcategory = subcategory, paymentMethod = "Cash", note = "$cleanNote [Cash Split]".trim(), hashtags = hashtags, warrantyDate = warrantyDate
                        )
                    )
                }
                if (upiAmt > 0.0) {
                    repository.insertTransaction(
                        TransactionEntity(
                            accountId = accountId, type = type, amount = upiAmt, amountPaise = MoneyPaise.toPaise(upiAmt),
                            category = category, subcategory = subcategory, paymentMethod = "UPI", upiApp = upiApp.ifBlank { "UPI" }, note = "$cleanNote [UPI Split]".trim(), hashtags = hashtags, warrantyDate = warrantyDate
                        )
                    )
                }
                statusMessageFlow.value = "Saved Half-Cash + Half-UPI split"
                return@launch
            }

            val tx = TransactionEntity(
                id = editingId ?: 0L,
                accountId = accountId,
                type = type.uppercase(),
                amount = amount,
                amountPaise = MoneyPaise.toPaise(amount),
                category = category,
                subcategory = subcategory,
                paymentMethod = paymentMethod,
                upiApp = upiApp,
                note = cleanNote,
                hashtags = hashtags,
                warrantyDate = warrantyDate,
                allocationId = allocationId
            )
            if (editingId != null) {
                repository.updateTransaction(tx)
                statusMessageFlow.value = "Updated transaction"
            } else {
                repository.insertTransaction(tx)
                val acc = repository.getAccountById(accountId)
                if (acc != null) {
                    if (type.equals("INCOME", true)) {
                        val splitAcc = NativeLedgerCalculator.applyAutoSplitToAllocations(acc, amount)
                        if (splitAcc != acc) repository.updateAccount(splitAcc)
                    } else if (allocationId.isNotBlank()) {
                        val adjAcc = NativeLedgerCalculator.adjustAllocationsForTransaction(acc, allocationId, type, amount)
                        if (adjAcc != acc) repository.updateAccount(adjAcc)
                    }
                }
                if (type.equals("EXPENSE", true)) applyRoundUpIfEnabled(amount)
                statusMessageFlow.value = "Saved ${type.lowercase()} (${settingsFlow.value.currencySymbol}${"%,.0f".format(amount)})"
            }
        }
    }

    private suspend fun applyRoundUpIfEnabled(expenseAmount: Double) {
        if (!settingsFlow.value.roundUpEnabled) return
        val spare = NativeLedgerCalculator.computeRoundUpSpareChange(expenseAmount, 10)
        if (spare > 0.0) {
            val firstGoal = repository.goals.first().firstOrNull()
            if (firstGoal != null) {
                val cur = if (firstGoal.currentAmount > 0) firstGoal.currentAmount else MoneyPaise.toRupees(firstGoal.currentPaise)
                val next = cur + spare
                repository.updateGoal(firstGoal.copy(currentAmount = next, currentPaise = MoneyPaise.toPaise(next)))
                repository.insertGoalContribution(GoalContributionEntity(goalId = firstGoal.id, amount = spare, note = "Round-Up Spare Change"))
            }
        }
    }

    fun repeatTransaction(tx: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTransaction(tx.copy(id = 0, timestamp = System.currentTimeMillis()))
            statusMessageFlow.value = "Repeated transaction (${tx.category})"
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            deletedTxFlow.value = tx
            deletedChipFlow.value = null
            repository.deleteTransaction(tx)
            statusMessageFlow.value = "Deleted ${tx.category}"
        }
    }

    // Pending Refunds
    fun addPendingRefund(title: String, amount: Double, source: String = "Online Order") {
        val next = pendingRefundsFlow.value + PendingRefundItem(
            id = "ref_${System.currentTimeMillis()}",
            title = TracmioSecurityEngine.sanitizeInput(title, 80),
            source = source,
            amount = amount
        )
        pendingRefundsFlow.value = next
        prefsStore.savePendingRefunds(next)
        statusMessageFlow.value = "Added pending refund: $title"
    }

    fun markRefundReceived(ref: PendingRefundItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val acc = repository.accounts.first().firstOrNull()
            if (acc != null) {
                repository.insertTransaction(
                    TransactionEntity(
                        accountId = acc.id,
                        type = "INCOME",
                        amount = ref.amount,
                        amountPaise = MoneyPaise.toPaise(ref.amount),
                        category = "Other",
                        paymentMethod = "Bank Transfer",
                        note = "📦 Refund received: ${ref.title}"
                    )
                )
            }
            deletePendingRefund(ref.id)
            statusMessageFlow.value = "Credited refund: ${ref.title}"
        }
    }

    fun deletePendingRefund(id: String) {
        val next = pendingRefundsFlow.value.filterNot { it.id == id }
        pendingRefundsFlow.value = next
        prefsStore.savePendingRefunds(next)
    }

    // People & Khata
    fun addPerson(name: String, upiId: String, openingBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPerson(
                PersonEntity(
                    name = TracmioSecurityEngine.sanitizeInput(name, 60),
                    upiId = TracmioSecurityEngine.sanitizeInput(upiId, 80),
                    netBalance = openingBalance,
                    netBalancePaise = MoneyPaise.toPaise(openingBalance)
                )
            )
            statusMessageFlow.value = "Added $name"
        }
    }

    fun settlePerson(person: PersonEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePerson(person.copy(netBalance = 0.0, netBalancePaise = 0L))
            statusMessageFlow.value = "Settled balance with ${person.name}"
        }
    }

    fun addKhataEntry(shopOrFriend: String, itemDescription: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertKhataEntry(
                KhataEntryEntity(
                    shopOrFriend = TracmioSecurityEngine.sanitizeInput(shopOrFriend, 60),
                    itemDescription = TracmioSecurityEngine.sanitizeInput(itemDescription, 100),
                    amount = amount
                )
            )
            statusMessageFlow.value = "Added Khata entry for $shopOrFriend"
        }
    }

    fun settleKhataEntry(entry: KhataEntryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateKhataEntry(entry.copy(isSettled = true))
            val acc = repository.accounts.first().firstOrNull()
            if (acc != null) {
                repository.insertTransaction(
                    TransactionEntity(
                        accountId = acc.id,
                        type = "EXPENSE",
                        amount = entry.amount,
                        amountPaise = MoneyPaise.toPaise(entry.amount),
                        category = "Khata & Kirana",
                        paymentMethod = "UPI",
                        note = "Cleared Khata: ${entry.shopOrFriend} (${entry.itemDescription})"
                    )
                )
            }
            statusMessageFlow.value = "Cleared Khata for ${entry.shopOrFriend}"
        }
    }

    fun deleteKhataEntry(entry: KhataEntryEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteKhataEntry(entry) }
    }

    // Budgets, Goals, Loans, Wishlist, Cashback, PPU
    fun addBudget(category: String, limit: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBudget(BudgetEntity(category = category, monthlyLimit = limit, limitPaise = MoneyPaise.toPaise(limit)))
            statusMessageFlow.value = "Saved budget for $category"
        }
    }

    fun addGoal(title: String, target: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGoal(GoalEntity(name = title, title = title, targetAmount = target, targetPaise = MoneyPaise.toPaise(target)))
            statusMessageFlow.value = "Created goal: $title"
        }
    }

    fun contributeToGoal(goal: GoalEntity, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val cur = if (goal.currentAmount > 0) goal.currentAmount else MoneyPaise.toRupees(goal.currentPaise)
            val next = cur + amount
            repository.updateGoal(goal.copy(currentAmount = next, currentPaise = MoneyPaise.toPaise(next)))
            repository.insertGoalContribution(GoalContributionEntity(goalId = goal.id, amount = amount, note = "Manual deposit"))
            statusMessageFlow.value = "Added ${settingsFlow.value.currencySymbol}${amount.toInt()} to ${goal.title.ifBlank { goal.name }}"
        }
    }

    fun addLoan(name: String, principal: Double, apr: Double, monthlyEmi: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLoan(
                LoanEntity(
                    name = name, lender = name, principal = principal, principalPaise = MoneyPaise.toPaise(principal),
                    interestRate = apr, monthlyPayment = monthlyEmi, emiPaise = MoneyPaise.toPaise(monthlyEmi)
                )
            )
            statusMessageFlow.value = "Added loan: $name"
        }
    }

    fun recordLoanRepayment(loan: LoanEntity, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLoanPayment(LoanPaymentEntity(loanId = loan.id, amount = amount, amountPaise = MoneyPaise.toPaise(amount)))
            statusMessageFlow.value = "Recorded loan payment for ${loan.name.ifBlank { loan.lender }}"
        }
    }

    fun addWishlistCooldown(item: String, price: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertWishlistCooldown(WishlistCooldownEntity(itemName = item, price = price))
            statusMessageFlow.value = "Started 24h cooldown for $item"
        }
    }

    fun skipWishlistAndSave(item: WishlistCooldownEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWishlistCooldown(item.copy(status = "SKIPPED_SAVED"))
            statusMessageFlow.value = "Skipped ${item.itemName} and saved ${settingsFlow.value.currencySymbol}${item.price.toInt()}!"
        }
    }

    fun addCashbackReward(app: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCashbackReward(CashbackRewardEntity(sourceApp = app, amount = amount))
            statusMessageFlow.value = "Logged ${settingsFlow.value.currencySymbol}${amount.toInt()} cashback from $app"
        }
    }

    fun addPricePerUseItem(name: String, price: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPricePerUseItem(PricePerUseEntity(itemName = name, purchasePrice = price))
            statusMessageFlow.value = "Tracking value for $name"
        }
    }

    fun incrementItemUse(item: PricePerUseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePricePerUseItem(item.copy(usageCount = item.usageCount + 1))
        }
    }

    // Subscriptions, Academic, Reminders, Trackers
    fun addSubscription(name: String, amount: Double, cycle: String, isSim: Boolean = false, validityDays: Int = 28) {
        viewModelScope.launch(Dispatchers.IO) {
            val due = System.currentTimeMillis() + (validityDays.toLong() * 86_400_000L)
            repository.insertSubscription(
                SubscriptionEntity(
                    name = name, amount = amount, billingCycle = cycle, nextDueDateMillis = due,
                    isSimOrWifi = isSim, validityDays = validityDays
                )
            )
            statusMessageFlow.value = "Added $name"
        }
    }

    fun deleteSubscription(sub: SubscriptionEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteSubscription(sub) }
    }

    fun addAcademicEvent(title: String, fee: Double, dueDate: String, semester: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAcademicEvent(AcademicEventEntity(title = title, feeAmount = fee, dueDate = dueDate, semester = semester))
            statusMessageFlow.value = "Added academic event: $title"
        }
    }

    fun markAcademicEventPaid(event: AcademicEventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAcademicEvent(event.copy(isPaid = true, paidAmount = event.feeAmount))
            statusMessageFlow.value = "Marked ${event.title} paid"
        }
    }

    fun addReminder(title: String, amount: Double, dueDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertReminder(ReminderItemEntity(title = title, amount = amount, dueDate = dueDate))
            statusMessageFlow.value = "Added reminder: $title"
        }
    }

    fun toggleReminderCompleted(rem: ReminderItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateReminder(rem.copy(isCompleted = !rem.isCompleted))
        }
    }

    fun deleteReminder(rem: ReminderItemEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteReminder(rem) }
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        dailyReminderEnabledFlow.value = enabled
        prefsStore.saveDailyReminderConfig(enabled, 21, 0)
    }

    fun addCustomTracker(title: String, type: String, target: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCustomTracker(
                CustomTrackerEntity(
                    title = title, trackerType = type, targetValue = target,
                    unit = if (type == "SAVINGS") settingsFlow.value.currencySymbol else "pts"
                )
            )
            statusMessageFlow.value = "Started tracker: $title"
        }
    }

    fun incrementCustomTracker(tracker: CustomTrackerEntity, delta: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = CustomTrackerEntryEntity(trackerId = tracker.id, deltaValue = delta)
            repository.insertTrackerEntry(entry)
            repository.updateCustomTracker(CustomTrackersCalculator.applyEntryDelta(tracker, entry))
        }
    }

    fun deleteCustomTracker(tracker: CustomTrackerEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteCustomTracker(tracker) }
    }

    fun addTrackerFolder(folderName: String, icon: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.insertTrackerFolder(TrackerFolderEntity(folderName = folderName, icon = icon)) }
    }

    fun deleteTrackerFolder(folder: TrackerFolderEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteTrackerFolder(folder) }
    }

    // App Lock & Security Audit
    fun enableAppLock(pin: String, biometric: Boolean, question: String, answer: String): String {
        val recoveryKey = appLockManager.enableLock(pin, biometric, question, answer)
        appLockStateFlow.value = appLockManager.getState()
        statusMessageFlow.value = "App Lock enabled. Save recovery key: $recoveryKey"
        return recoveryKey
    }

    fun disableAppLock() {
        appLockManager.disableLock()
        appLockStateFlow.value = appLockManager.getState()
        isAppLockedFlow.value = false
        statusMessageFlow.value = "App Lock disabled"
    }

    fun unlockWithPin(pin: String): Boolean {
        val ok = appLockManager.verifyPin(pin)
        if (ok) isAppLockedFlow.value = false
        return ok
    }

    fun unlockWithBiometricSuccess() { isAppLockedFlow.value = false }

    fun recoverPinWithSecret(secret: String, newPin: String?): Boolean {
        val ok = appLockManager.verifyRecoveryAndReset(secret, newPin)
        if (ok) {
            appLockStateFlow.value = appLockManager.getState()
            isAppLockedFlow.value = false
            statusMessageFlow.value = if (newPin.isNullOrBlank()) "App Lock reset & unlocked" else "PIN updated & unlocked"
        }
        return ok
    }

    fun runDatabaseHealthReport(): TracmioSecurityEngine.DatabaseHealthReport {
        val st = uiState.value
        return repository.runDatabaseHealthCheck(st.accounts.size, st.transactions.size, st.customTrackers.size)
    }

    fun buildBackupJson(): String = NativeBackupService.buildFullBackupJson(uiState.value, settingsFlow.value)

    fun importBackupJsonText(rawText: String, password: CharArray? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val plain = runCatching {
                if (password != null && rawText.contains("\"cipherText\"")) {
                    TracmioSecurityEngine.decryptBackup(rawText, password)
                } else {
                    rawText
                }
            }.getOrElse {
                statusMessageFlow.value = "Import failed: wrong password or corrupted backup"
                return@launch
            }
            val count = runCatching { repository.importBackupJson(plain) }.getOrDefault(0)
            statusMessageFlow.value = "Imported $count records safely into offline vault"
        }
    }

    fun importCsvText(csvText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultAccId = repository.accounts.first().firstOrNull()?.id ?: 1L
            val parsed = CsvParser.parseCsvTransactions(csvText, defaultAccId)
            parsed.forEach { repository.insertTransaction(it) }
            statusMessageFlow.value = "Imported ${parsed.size} CSV transactions"
        }
    }

    private data class CoreLedgerSnapshot(
        val accounts: List<AccountEntity>,
        val transactions: List<TransactionEntity>,
        val budgets: List<BudgetEntity>,
        val goals: List<GoalEntity>,
        val loans: List<LoanEntity>
    )

    private data class PlanningExtrasSnapshot(
        val goalContributions: List<GoalContributionEntity>,
        val loanPayments: List<LoanPaymentEntity>,
        val persons: List<PersonEntity>,
        val customTrackers: List<CustomTrackerEntity>,
        val trackerFolders: List<TrackerFolderEntity>
    )

    private data class ToolsEntitiesSnapshot(
        val subscriptions: List<SubscriptionEntity>,
        val academicEvents: List<AcademicEventEntity>,
        val reminders: List<ReminderItemEntity>,
        val khataEntries: List<KhataEntryEntity>,
        val wishlistCooldowns: List<WishlistCooldownEntity>
    )

    private data class RewardAndTrackSnapshot(
        val cashbackRewards: List<CashbackRewardEntity>,
        val pricePerUseItems: List<PricePerUseEntity>,
        val trackerEntries: List<CustomTrackerEntryEntity>,
        val quickSpendChips: List<QuickSpendChip>,
        val pendingRefunds: List<PendingRefundItem>
    )

    private data class UiFlagsSnapshot(
        val statusMessage: String?,
        val deletedTx: TransactionEntity?,
        val deletedChip: QuickSpendChip?,
        val isAppLocked: Boolean,
        val appLockState: AppLockState
    )
}
