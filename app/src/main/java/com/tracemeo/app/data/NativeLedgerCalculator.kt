package com.tracemeo.app.data

import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.CategoryEntity
import com.tracemeo.app.data.local.GoalContributionEntity
import com.tracemeo.app.data.local.GoalEntity
import com.tracemeo.app.data.local.LoanEntity
import com.tracemeo.app.data.local.LoanPaymentEntity
import com.tracemeo.app.data.local.PersonEntity
import com.tracemeo.app.data.local.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToLong

data class AccountAllocation(
    val id: String,
    val name: String,
    val allocatedAmount: Double,
    val autoSplitPercent: Double = 0.0
)

data class PersonLedgerSummary(
    val person: PersonEntity,
    val borrowedPaise: Long,
    val lentPaise: Long,
    val netPaise: Long,
    val transactions: List<TransactionEntity>
) {
    val borrowed: Double get() = MoneyPaise.toRupees(borrowedPaise)
    val lent: Double get() = MoneyPaise.toRupees(lentPaise)
    val net: Double get() = MoneyPaise.toRupees(netPaise)
}

data class SafeToSpendSummary(
    val dailyAllowance: Double,
    val unallocatedLiquidBalance: Double,
    val spentToday: Double,
    val remainingToday: Double,
    val remainingDaysInMonth: Int
)

data class DebtPayoffStrategyResult(
    val strategyName: String,
    val totalMonths: Int,
    val totalInterestPaid: Double,
    val payoffOrder: List<String>
)

data class WeekendRadarSummary(
    val weekdayAvgPerDay: Double,
    val weekendAvgPerDay: Double,
    val weekendTotal: Double,
    val weekdayTotal: Double
)

data class MonthComparisonSummary(
    val currentMonthLabel: String,
    val previousMonthLabel: String,
    val currentIncome: Double,
    val previousIncome: Double,
    val currentExpense: Double,
    val previousExpense: Double,
    val expenseDeltaPct: Double
)

data class HashtagSummary(
    val tag: String,
    val count: Int,
    val totalExpense: Double,
    val totalIncome: Double
)

enum class SpendingChartWindow(val label: String) {
    WEEKLY("Weekly (7d)"),
    MONTHLY("Monthly (30d)")
}

data class DashboardCategorySlice(
    val category: String,
    val emoji: String,
    val amount: Double,
    val amountPaise: Long,
    val percentage: Float,
    val txCount: Int
) {
    val icon: String get() = emoji
    val transactionCount: Int get() = txCount
}

data class DashboardSpendingDistribution(
    val window: SpendingChartWindow,
    val windowLabel: String,
    val totalExpense: Double,
    val totalExpensePaise: Long,
    val totalTxCount: Int,
    val slices: List<DashboardCategorySlice>
) {
    val totalSpent: Double get() = totalExpense
    val transactionCount: Int get() = totalTxCount
}

object NativeLedgerCalculator {
    private val inflowTypes = setOf("INCOME", "TRANSFER_IN", "BORROW")

    fun txPaise(tx: TransactionEntity): Long =
        if (tx.amountPaise != 0L || tx.amount == 0.0) tx.amountPaise else MoneyPaise.toPaise(tx.amount)

    fun accountOpeningPaise(account: AccountEntity): Long =
        if (account.balancePaise != 0L || account.balance == 0.0) account.balancePaise else MoneyPaise.toPaise(account.balance)

    fun accountBalancePaise(account: AccountEntity, transactions: List<TransactionEntity>): Long {
        val delta = transactions
            .asSequence()
            .filter { it.accountId == account.id }
            .sumOf { tx ->
                val amount = txPaise(tx)
                if (tx.type in inflowTypes) amount else -amount
            }
        return accountOpeningPaise(account) + delta
    }

    fun accountBalance(account: AccountEntity, transactions: List<TransactionEntity>): Double =
        MoneyPaise.toRupees(accountBalancePaise(account, transactions))

    fun totalBalancePaise(accounts: List<AccountEntity>, transactions: List<TransactionEntity>): Long =
        accounts.filter { !it.isArchived }.sumOf { accountBalancePaise(it, transactions) }

    fun totalBalance(accounts: List<AccountEntity>, transactions: List<TransactionEntity>): Double =
        MoneyPaise.toRupees(totalBalancePaise(accounts, transactions))

    fun parseAllocations(json: String): List<AccountAllocation> {
        if (json.isBlank() || json == "[]") return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        AccountAllocation(
                            id = obj.optString("id", "alloc_$i"),
                            name = obj.optString("name", "Pocket"),
                            allocatedAmount = obj.optDouble("allocatedAmount", obj.optDouble("amount", 0.0)),
                            autoSplitPercent = obj.optDouble("autoSplitPercent", obj.optDouble("autoSplitPct", 0.0))
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun serializeAllocations(list: List<AccountAllocation>): String {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("allocatedAmount", a.allocatedAmount)
                put("autoSplitPercent", a.autoSplitPercent)
            })
        }
        return arr.toString()
    }

    fun applyAutoSplitToAllocations(account: AccountEntity, incomeAmount: Double): AccountEntity {
        val allocs = parseAllocations(account.allocationsJson)
        if (allocs.isEmpty() || incomeAmount <= 0.0) return account
        val updated = allocs.map { alloc ->
            if (alloc.autoSplitPercent > 0.0) {
                val add = (incomeAmount * (alloc.autoSplitPercent / 100.0))
                val roundedAdd = (add * 100.0).roundToLong() / 100.0
                alloc.copy(allocatedAmount = alloc.allocatedAmount + roundedAdd)
            } else alloc
        }
        return account.copy(
            allocationsJson = serializeAllocations(updated),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun unallocatedBalance(account: AccountEntity, transactions: List<TransactionEntity>): Double {
        val bal = accountBalance(account, transactions)
        val allocated = parseAllocations(account.allocationsJson).sumOf { it.allocatedAmount }
        return (bal - allocated).coerceAtLeast(0.0)
    }

    fun adjustAllocationsForTransaction(
        account: AccountEntity,
        allocationId: String,
        txType: String,
        amount: Double,
        reverse: Boolean = false
    ): AccountEntity {
        if (allocationId.isBlank() || amount <= 0.0) return account
        val allocs = parseAllocations(account.allocationsJson)
        if (allocs.none { it.id == allocationId }) return account
        val isInflow = txType.uppercase() in inflowTypes
        val signedDelta = (if (isInflow) amount else -amount) * (if (reverse) -1.0 else 1.0)
        val updated = allocs.map { alloc ->
            if (alloc.id == allocationId) {
                alloc.copy(allocatedAmount = (alloc.allocatedAmount + signedDelta).coerceAtLeast(0.0))
            } else alloc
        }
        return account.copy(
            allocationsJson = serializeAllocations(updated),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun creditCardUtilizationPct(account: AccountEntity, transactions: List<TransactionEntity>): Double {
        if (!account.accountType.equals("Credit Card", ignoreCase = true) &&
            !account.accountType.equals("Credit", ignoreCase = true)
        ) return 0.0
        if (account.creditLimit <= 0.0) return 0.0
        val currentBal = accountBalance(account, transactions)
        val used = (-currentBal).coerceAtLeast(0.0)
        return ((used / account.creditLimit) * 100.0).coerceIn(0.0, 200.0)
    }

    fun computeSafeToSpend(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): SafeToSpendSummary {
        val liquidAccounts = accounts.filter {
            !it.isArchived && (it.accountType.equals("Cash", true) ||
                it.accountType.equals("Bank", true) ||
                it.accountType.equals("Wallet", true) ||
                it.accountType.equals("Savings", true))
        }
        val unallocatedLiquid = liquidAccounts.sumOf { unallocatedBalance(it, transactions) }

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val remainingDays = (maxDays - dayOfMonth + 1).coerceAtLeast(1)

        val todayKey = dayKey(nowMillis)
        val spentToday = transactions
            .filter { it.type.equals("EXPENSE", true) && dayKey(it.timestamp) == todayKey }
            .sumOf { it.amount }

        val dailyBase = (unallocatedLiquid / remainingDays).coerceAtLeast(0.0)
        val remainingToday = (dailyBase - spentToday).coerceAtLeast(0.0)

        return SafeToSpendSummary(
            dailyAllowance = dailyBase,
            unallocatedLiquidBalance = unallocatedLiquid,
            spentToday = spentToday,
            remainingToday = remainingToday,
            remainingDaysInMonth = remainingDays
        )
    }

    fun computeRoundUpSpareChange(amount: Double, step: Int): Double {
        if (amount <= 0.0 || step <= 0) return 0.0
        val remainder = amount % step
        return if (remainder == 0.0) 0.0 else ((step - remainder) * 100.0).roundToLong() / 100.0
    }

    fun computeEmergencyRunwayMonths(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): Double {
        val totalLiquid = accounts
            .filter { !it.isArchived && !it.accountType.contains("Credit", ignoreCase = true) }
            .sumOf { accountBalance(it, transactions) }
            .coerceAtLeast(0.0)
        val expenses = transactions.filter { it.type.equals("EXPENSE", true) }
        if (expenses.isEmpty()) return if (totalLiquid > 0) 12.0 else 0.0
        val byMonth = expenses.groupBy { monthKey(it.timestamp) }.mapValues { (_, list) -> list.sumOf { it.amount } }
        val avgMonthlyBurn = (byMonth.values.average()).coerceAtLeast(1000.0)
        return (totalLiquid / avgMonthlyBurn).coerceIn(0.0, 120.0)
    }

    fun simulateDebtPayoff(
        loans: List<LoanEntity>,
        payments: List<LoanPaymentEntity>,
        extraMonthly: Double,
        isAvalanche: Boolean
    ): DebtPayoffStrategyResult {
        data class SimLoan(val name: String, var balance: Double, val ratePct: Double, val minEmi: Double)

        val active = loans.mapNotNull { loan ->
            val rem = MoneyPaise.toRupees(loanRemainingPaise(loan, payments))
            if (rem <= 0.0) null
            else SimLoan(
                name = loan.name,
                balance = rem,
                ratePct = loan.interestRate.coerceAtLeast(0.0),
                minEmi = loan.monthlyPayment.coerceAtLeast(500.0)
            )
        }
        if (active.isEmpty()) {
            return DebtPayoffStrategyResult(
                strategyName = if (isAvalanche) "Avalanche" else "Snowball",
                totalMonths = 0,
                totalInterestPaid = 0.0,
                payoffOrder = emptyList()
            )
        }
        val sorted = if (isAvalanche) {
            active.sortedByDescending { it.ratePct }.map { it.copy() }
        } else {
            active.sortedBy { it.balance }.map { it.copy() }
        }
        val order = sorted.map { it.name }
        var months = 0
        var totalInterest = 0.0
        while (sorted.any { it.balance > 0.01 } && months < 360) {
            months++
            var availableExtra = extraMonthly.coerceAtLeast(0.0)
            for (item in sorted) {
                if (item.balance <= 0.01) {
                    availableExtra += item.minEmi
                    continue
                }
                val interest = item.balance * (item.ratePct / 100.0 / 12.0)
                totalInterest += interest
                item.balance += interest
                val minPay = minOf(item.balance, item.minEmi)
                item.balance -= minPay
            }
            for (item in sorted) {
                if (item.balance > 0.01 && availableExtra > 0.0) {
                    val pay = minOf(item.balance, availableExtra)
                    item.balance -= pay
                    availableExtra -= pay
                }
            }
        }
        return DebtPayoffStrategyResult(
            strategyName = if (isAvalanche) "Avalanche (Highest APR)" else "Snowball (Smallest First)",
            totalMonths = months,
            totalInterestPaid = totalInterest,
            payoffOrder = order
        )
    }

    fun extractHashtags(note: String, explicitHashtags: String = ""): List<String> {
        return Regex("#([A-Za-z0-9_]+)").findAll("$note $explicitHashtags")
            .map { "#${it.groupValues[1].lowercase()}" }
            .distinct()
            .toList()
    }

    fun formatHashtags(note: String, explicitHashtags: String = ""): String =
        extractHashtags(note, explicitHashtags).joinToString(" ")

    fun summarizeHashtags(transactions: List<TransactionEntity>): List<HashtagSummary> {
        val map = linkedMapOf<String, MutableList<TransactionEntity>>()
        transactions.forEach { tx ->
            extractHashtags(tx.note, tx.hashtags).forEach { tag ->
                map.getOrPut(tag) { mutableListOf() }.add(tx)
            }
        }
        return map.map { (tag, txs) ->
            HashtagSummary(
                tag = tag,
                count = txs.size,
                totalExpense = txs.filter { it.type.equals("EXPENSE", true) }.sumOf { it.amount },
                totalIncome = txs.filter { it.type.equals("INCOME", true) }.sumOf { it.amount }
            )
        }.sortedByDescending { it.totalExpense + it.totalIncome }
    }

    fun computeWeekendRadar(transactions: List<TransactionEntity>): WeekendRadarSummary {
        val expenses = transactions.filter { it.type.equals("EXPENSE", true) }
        val cal = Calendar.getInstance()
        val weekdayDays = mutableSetOf<String>()
        val weekendDays = mutableSetOf<String>()
        var weekdayTotal = 0.0
        var weekendTotal = 0.0

        expenses.forEach { tx ->
            cal.timeInMillis = tx.timestamp
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val key = dayKey(tx.timestamp)
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                weekendTotal += tx.amount
                weekendDays += key
            } else {
                weekdayTotal += tx.amount
                weekdayDays += key
            }
        }
        return WeekendRadarSummary(
            weekdayAvgPerDay = if (weekdayDays.isEmpty()) 0.0 else weekdayTotal / weekdayDays.size,
            weekendAvgPerDay = if (weekendDays.isEmpty()) 0.0 else weekendTotal / weekendDays.size,
            weekendTotal = weekendTotal,
            weekdayTotal = weekdayTotal
        )
    }

    fun computeMonthComparison(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthComparisonSummary {
        val fmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val curKey = monthKey(cal.timeInMillis)
        val curLabel = fmt.format(Date(cal.timeInMillis))
        cal.add(Calendar.MONTH, -1)
        val prevKey = monthKey(cal.timeInMillis)
        val prevLabel = fmt.format(Date(cal.timeInMillis))

        val curIncome = transactions.filter { monthKey(it.timestamp) == curKey && it.type.equals("INCOME", true) }.sumOf { it.amount }
        val prevIncome = transactions.filter { monthKey(it.timestamp) == prevKey && it.type.equals("INCOME", true) }.sumOf { it.amount }
        val curExpense = transactions.filter { monthKey(it.timestamp) == curKey && it.type.equals("EXPENSE", true) }.sumOf { it.amount }
        val prevExpense = transactions.filter { monthKey(it.timestamp) == prevKey && it.type.equals("EXPENSE", true) }.sumOf { it.amount }
        val deltaPct = if (prevExpense > 0.0) ((curExpense - prevExpense) / prevExpense) * 100.0 else 0.0

        return MonthComparisonSummary(
            currentMonthLabel = curLabel,
            previousMonthLabel = prevLabel,
            currentIncome = curIncome,
            previousIncome = prevIncome,
            currentExpense = curExpense,
            previousExpense = prevExpense,
            expenseDeltaPct = deltaPct
        )
    }

    fun computeNoSpendStreakDays(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val expenseDays = transactions
            .filter { it.type.equals("EXPENSE", true) }
            .map { dayKey(it.timestamp) }
            .toSet()
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        var streak = 0
        repeat(60) {
            val key = dayKey(cal.timeInMillis)
            if (key !in expenseDays) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                return streak
            }
        }
        return streak
    }

    fun computeCashDenominationTotal(lines: List<CashCountLine>): Double =
        lines.sumOf { (it.value.toLong() * it.count.toLong()) }.toDouble()

    fun totalIncomePaise(
        transactions: List<TransactionEntity>,
        from: Long? = null,
        toInclusive: Long? = null
    ): Long = transactions
        .asSequence()
        .filter { it.type in setOf("INCOME", "BORROW") }
        .filter { from == null || it.timestamp >= from }
        .filter { toInclusive == null || it.timestamp <= toInclusive }
        .sumOf { txPaise(it) }

    fun totalIncome(
        transactions: List<TransactionEntity>,
        from: Long? = null,
        toInclusive: Long? = null
    ): Double = MoneyPaise.toRupees(totalIncomePaise(transactions, from, toInclusive))

    fun totalExpensePaise(
        transactions: List<TransactionEntity>,
        from: Long? = null,
        toInclusive: Long? = null
    ): Long = transactions
        .asSequence()
        .filter { it.type in setOf("EXPENSE", "LEND") }
        .filter { from == null || it.timestamp >= from }
        .filter { toInclusive == null || it.timestamp <= toInclusive }
        .sumOf { txPaise(it) }

    fun totalExpense(
        transactions: List<TransactionEntity>,
        from: Long? = null,
        toInclusive: Long? = null
    ): Double = MoneyPaise.toRupees(totalExpensePaise(transactions, from, toInclusive))

    fun goalSavedPaise(goal: GoalEntity, contributions: List<GoalContributionEntity>): Long {
        val base = if (goal.savedAmountPaise != 0L || goal.savedAmount == 0.0) {
            goal.savedAmountPaise
        } else {
            MoneyPaise.toPaise(goal.savedAmount)
        }
        val matching = contributions.filter { it.goalId == goal.id }
        if (matching.isEmpty()) return base
        val contribTotal = matching.sumOf {
            if (it.amountPaise != 0L || it.amount == 0.0) it.amountPaise else MoneyPaise.toPaise(it.amount)
        }
        return maxOf(base, contribTotal)
    }

    fun loanPaidPaise(loan: LoanEntity, payments: List<LoanPaymentEntity>): Long {
        val base = if (loan.paidAmountPaise != 0L || loan.paidAmount == 0.0) {
            loan.paidAmountPaise
        } else {
            MoneyPaise.toPaise(loan.paidAmount)
        }
        val matching = payments.filter { it.loanId == loan.id }
        if (matching.isEmpty()) return base
        val paymentTotal = matching.sumOf {
            if (it.amountPaise != 0L || it.amount == 0.0) it.amountPaise else MoneyPaise.toPaise(it.amount)
        }
        return maxOf(base, paymentTotal)
    }

    fun loanRemainingPaise(loan: LoanEntity, payments: List<LoanPaymentEntity>): Long {
        val principal = if (loan.principalPaise != 0L || loan.principal == 0.0) {
            loan.principalPaise
        } else {
            MoneyPaise.toPaise(loan.principal)
        }
        return (principal - loanPaidPaise(loan, payments)).coerceAtLeast(0L)
    }

    fun personSummaries(
        persons: List<PersonEntity>,
        transactions: List<TransactionEntity>
    ): List<PersonLedgerSummary> = persons.map { person ->
        val personTx = transactions.filter { it.personId == person.id }
        val borrowedPaise = personTx.filter { it.type == "BORROW" }.sumOf { txPaise(it) }
        val lentPaise = personTx.filter { it.type == "LEND" }.sumOf { txPaise(it) }
        PersonLedgerSummary(
            person = person,
            borrowedPaise = borrowedPaise,
            lentPaise = lentPaise,
            netPaise = lentPaise - borrowedPaise,
            transactions = personTx
        )
    }

    fun dayKey(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

    fun monthKey(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(timestamp))

    fun computeSpendingDistribution(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity> = emptyList(),
        window: SpendingChartWindow,
        nowMillis: Long = System.currentTimeMillis()
    ): DashboardSpendingDistribution {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (window == SpendingChartWindow.WEEKLY) {
                add(Calendar.DAY_OF_YEAR, -6)
            } else {
                set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val fromMillis = startCal.timeInMillis
        val endCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val toMillis = endCal.timeInMillis

        val windowLabel = if (window == SpendingChartWindow.WEEKLY) {
            "Last 7 Days"
        } else {
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(nowMillis))
        }

        val matchingExpenses = transactions.filter {
            it.type.equals("EXPENSE", ignoreCase = true) &&
                it.timestamp in fromMillis..toMillis
        }

        val totalPaise = matchingExpenses.sumOf { txPaise(it) }
        if (totalPaise <= 0L || matchingExpenses.isEmpty()) {
            return DashboardSpendingDistribution(
                window = window,
                windowLabel = windowLabel,
                totalExpense = 0.0,
                totalExpensePaise = 0L,
                totalTxCount = 0,
                slices = emptyList()
            )
        }

        val iconLookup = categories.associateBy({ it.name.trim().lowercase() }, { it.icon })
        val fallbackEmojis = mapOf(
            "food" to "🍽️",
            "food & dining" to "🍽️",
            "travel" to "✈️",
            "transport" to "🚆",
            "bills" to "📄",
            "utilities" to "⚡",
            "shopping" to "🛍️",
            "health" to "💊",
            "entertainment" to "🎬",
            "education" to "📚",
            "loan emi" to "🏦",
            "subscriptions" to "🔄",
            "other" to "📦"
        )

        val slices = matchingExpenses
            .groupBy { it.category.ifBlank { "Other" } }
            .map { (catName, list) ->
                val catPaise = list.sumOf { txPaise(it) }
                val key = catName.trim().lowercase()
                val emoji = iconLookup[key]?.takeIf { it.isNotBlank() }
                    ?: fallbackEmojis[key]
                    ?: "💠"
                DashboardCategorySlice(
                    category = catName,
                    emoji = emoji,
                    amount = MoneyPaise.toRupees(catPaise),
                    amountPaise = catPaise,
                    percentage = ((catPaise.toDouble() / totalPaise.toDouble()) * 100.0).toFloat(),
                    txCount = list.size
                )
            }
            .sortedByDescending { it.amountPaise }

        return DashboardSpendingDistribution(
            window = window,
            windowLabel = windowLabel,
            totalExpense = MoneyPaise.toRupees(totalPaise),
            totalExpensePaise = totalPaise,
            totalTxCount = matchingExpenses.size,
            slices = slices
        )
    }

    data class ExpandedCategorySpec(
        val name: String,
        val type: String,
        val icon: String,
        val subcategories: List<String>
    )

    data class IncomeSourceBreakdown(
        val familyAllowanceTotal: Double,
        val ownEarningsTotal: Double,
        val familyTxCount: Int,
        val ownTxCount: Int
    ) {
        val totalIncome: Double get() = familyAllowanceTotal + ownEarningsTotal
        val familyPercent: Float
            get() = if (totalIncome > 0.0) ((familyAllowanceTotal / totalIncome) * 100.0).toFloat() else 0f
        val ownPercent: Float
            get() = if (totalIncome > 0.0) ((ownEarningsTotal / totalIncome) * 100.0).toFloat() else 0f

        val familyAllowance: Double get() = familyAllowanceTotal
        val familyIncome: Double get() = familyAllowanceTotal
        val ownEarnings: Double get() = ownEarningsTotal
        val familyPercentage: Float get() = familyPercent
        val familyPct: Float get() = familyPercent
        val ownPercentage: Float get() = ownPercent
        val ownPct: Float get() = ownPercent
    }

    data class TodayExpenseCheckInSummary(
        val todaySpent: Double,
        val todayTxCount: Int,
        val safeToSpendRemaining: Double,
        val dailyAllowance: Double
    )

    val EXPANDED_CATEGORY_CATALOG: List<ExpandedCategorySpec> = listOf(
        ExpandedCategorySpec(
            name = "Food & Chai",
            type = "EXPENSE",
            icon = "☕",
            subcategories = listOf(
                "Tea / Coffee",
                "Snacks",
                "Breakfast",
                "Lunch",
                "Dinner",
                "Tiffin / Mess",
                "Groceries",
                "Restaurant"
            )
        ),
        ExpandedCategorySpec(
            name = "Khata & Kirana",
            type = "EXPENSE",
            icon = "🏪",
            subcategories = listOf(
                "Kirana Store",
                "Canteen",
                "Milk / Water Can",
                "Bakery",
                "Laundry / Iron"
            )
        ),
        ExpandedCategorySpec(
            name = "Transport & Fuel",
            type = "EXPENSE",
            icon = "🛺",
            subcategories = listOf(
                "Auto / Cab",
                "Bus / Metro",
                "Petrol / Diesel",
                "Parking / Toll",
                "Bike Service"
            )
        ),
        ExpandedCategorySpec(
            name = "SIM, WiFi & Bills",
            type = "EXPENSE",
            icon = "📶",
            subcategories = listOf(
                "Mobile Recharge (28/56/84d)",
                "Broadband / WiFi",
                "Electricity",
                "Water",
                "Gas",
                "OTT"
            )
        ),
        ExpandedCategorySpec(
            name = "College & Exams",
            type = "EXPENSE",
            icon = "🎓",
            subcategories = listOf(
                "Exam Fees",
                "Books & Xerox",
                "Hostel / PG",
                "Stationery",
                "Course / Lab"
            )
        ),
        ExpandedCategorySpec(
            name = "Shopping & Personal",
            type = "EXPENSE",
            icon = "🛍️",
            subcategories = listOf(
                "Clothes",
                "Electronics",
                "Grooming",
                "Medical / Pharmacy",
                "Gifts"
            )
        ),
        ExpandedCategorySpec(
            name = "Family & Income",
            type = "INCOME",
            icon = "💰",
            subcategories = listOf(
                "Family Allowance",
                "Own Earnings",
                "Freelance / Studio",
                "Cashback / Refund",
                "Gift Received"
            )
        )
    )

    fun subcategoriesFor(categoryName: String, categories: List<CategoryEntity> = emptyList()): List<String> {
        val clean = categoryName.trim()
        if (clean.isBlank()) return emptyList()
        val parent = categories.firstOrNull { it.parentId == null && it.name.equals(clean, ignoreCase = true) }
        val dbSubs = if (parent != null) {
            categories.filter { it.parentId == parent.id }.map { it.name }
        } else {
            emptyList()
        }
        val catalogSubs = EXPANDED_CATEGORY_CATALOG
            .firstOrNull { it.name.equals(clean, ignoreCase = true) }
            ?.subcategories
            ?: when {
                clean.equals("Food", true) -> EXPANDED_CATEGORY_CATALOG.first { it.name == "Food & Chai" }.subcategories
                clean.equals("Travel", true) -> EXPANDED_CATEGORY_CATALOG.first { it.name == "Transport & Fuel" }.subcategories
                clean.equals("Bills", true) -> EXPANDED_CATEGORY_CATALOG.first { it.name == "SIM, WiFi & Bills" }.subcategories
                clean.equals("Education", true) -> EXPANDED_CATEGORY_CATALOG.first { it.name == "College & Exams" }.subcategories
                clean.equals("Shopping", true) -> EXPANDED_CATEGORY_CATALOG.first { it.name == "Shopping & Personal" }.subcategories
                clean.equals("Salary", true) || clean.equals("Other Income", true) ->
                    EXPANDED_CATEGORY_CATALOG.first { it.name == "Family & Income" }.subcategories
                else -> emptyList()
            }
        return (catalogSubs + dbSubs).distinct()
    }

    /**
     * Safe arithmetic parser for inline math in the Amount box (e.g. "120+45+30" or "50*3").
     * Supports +, -, *, / (and ×, ÷), parentheses, and decimal numbers without eval().
     */
    fun evaluateInlineMath(expr: String): Double? {
        val cleaned = expr
            .replace("₹", "")
            .replace("$", "")
            .replace("€", "")
            .replace("£", "")
            .replace(",", "")
            .replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .trim()
            .removeSuffix("=")
            .trim()
        if (cleaned.isBlank()) return null
        cleaned.toDoubleOrNull()?.let { return if (it.isFinite()) it else null }
        if (!cleaned.all { it.isDigit() || it in setOf('.', '+', '-', '*', '/', '(', ')') }) return null

        return runCatching {
            val parser = object {
                var pos = 0
                fun peek(): Char? = if (pos < cleaned.length) cleaned[pos] else null
                fun consume(): Char = cleaned[pos++]

                fun parseExpression(): Double {
                    var value = parseTerm()
                    while (true) {
                        when (peek()) {
                            '+' -> {
                                consume()
                                value += parseTerm()
                            }
                            '-' -> {
                                consume()
                                value -= parseTerm()
                            }
                            else -> break
                        }
                    }
                    return value
                }

                fun parseTerm(): Double {
                    var value = parseFactor()
                    while (true) {
                        when (peek()) {
                            '*' -> {
                                consume()
                                value *= parseFactor()
                            }
                            '/' -> {
                                consume()
                                val divisor = parseFactor()
                                if (divisor == 0.0) throw ArithmeticException("Division by zero")
                                value /= divisor
                            }
                            else -> break
                        }
                    }
                    return value
                }

                fun parseFactor(): Double {
                    if (peek() == '+') {
                        consume()
                        return parseFactor()
                    }
                    if (peek() == '-') {
                        consume()
                        return -parseFactor()
                    }
                    if (peek() == '(') {
                        consume()
                        val inside = parseExpression()
                        if (peek() != ')') throw IllegalArgumentException("Unclosed parenthesis")
                        consume()
                        return inside
                    }
                    val start = pos
                    var dotCount = 0
                    while (pos < cleaned.length && (cleaned[pos].isDigit() || cleaned[pos] == '.')) {
                        if (cleaned[pos] == '.') dotCount++
                        if (dotCount > 1) throw IllegalArgumentException("Invalid decimal")
                        pos++
                    }
                    if (start == pos) throw IllegalArgumentException("Expected number")
                    return cleaned.substring(start, pos).toDouble()
                }
            }

            val result = parser.parseExpression()
            if (parser.pos != cleaned.length || !result.isFinite()) null else result
        }.getOrNull()
    }

    fun formatInlineMathResult(expr: String): String? {
        val result = evaluateInlineMath(expr) ?: return null
        val roundedPaise = (result * 100.0).roundToLong()
        return if (roundedPaise % 100L == 0L) {
            (roundedPaise / 100L).toString()
        } else {
            String.format(Locale.US, "%.2f", roundedPaise / 100.0)
        }
    }

    fun computeIncomeSourceSplit(transactions: List<TransactionEntity>): IncomeSourceBreakdown {
        val incomes = transactions.filter { it.type.equals("INCOME", ignoreCase = true) }
        var familyPaise = 0L
        var ownPaise = 0L
        var familyCount = 0
        var ownCount = 0
        incomes.forEach { tx ->
            val detail = "${tx.subcategory} ${tx.note} ${tx.hashtags}".lowercase(Locale.US)
            val cat = tx.category.lowercase(Locale.US)
            val isExplicitOwn = detail.contains("own") ||
                detail.contains("freelance") ||
                detail.contains("studio") ||
                detail.contains("salary") ||
                detail.contains("stipend") ||
                detail.contains("cashback") ||
                detail.contains("refund")
            val isExplicitFamily = detail.contains("family") ||
                detail.contains("allowance") ||
                detail.contains("pocket") ||
                detail.contains("parent") ||
                detail.contains("home") ||
                detail.contains("gift received")
            val isFamily = when {
                isExplicitOwn -> false
                isExplicitFamily -> true
                else -> cat.contains("allowance") || cat.contains("pocket") || (cat.contains("family") && !cat.contains("income"))
            }
            if (isFamily) {
                familyPaise += txPaise(tx)
                familyCount++
            } else {
                ownPaise += txPaise(tx)
                ownCount++
            }
        }
        return IncomeSourceBreakdown(
            familyAllowanceTotal = MoneyPaise.toRupees(familyPaise),
            ownEarningsTotal = MoneyPaise.toRupees(ownPaise),
            familyTxCount = familyCount,
            ownTxCount = ownCount
        )
    }

    fun computeRechargeDaysLeft(
        rechargeDateStr: String,
        validityDays: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Int, String> {
        val safeValidity = validityDays.coerceAtLeast(1)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startMs = runCatching { fmt.parse(rechargeDateStr.trim())?.time }.getOrNull() ?: nowMillis
        val expiryMs = startMs + safeValidity.toLong() * 86_400_000L
        val expiryStr = fmt.format(Date(expiryMs))
        val todayCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = expiryMs - todayCal.timeInMillis
        val days = ceil(diffMs.toDouble() / 86_400_000.0).toInt()
        return days to expiryStr
    }

    fun computeTodayExpenseSummary(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): TodayExpenseCheckInSummary {
        val todayStr = dayKey(nowMillis)
        val todayExpenses = transactions.filter {
            it.type.equals("EXPENSE", ignoreCase = true) && dayKey(it.timestamp) == todayStr
        }
        val spentPaise = todayExpenses.sumOf { txPaise(it) }
        val safe = computeSafeToSpend(accounts, transactions, nowMillis)
        return TodayExpenseCheckInSummary(
            todaySpent = MoneyPaise.toRupees(spentPaise),
            todayTxCount = todayExpenses.size,
            safeToSpendRemaining = safe.remainingToday,
            dailyAllowance = safe.dailyAllowance
        )
    }
}

