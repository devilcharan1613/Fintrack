package com.tracemeo.app.data

import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.LoanEntity
import com.tracemeo.app.data.local.LoanPaymentEntity
import com.tracemeo.app.data.local.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.ceil

object NativeLedgerCalculator {
    data class SafeToSpendResult(
        val remainingToday: Double,
        val dailyAllowance: Double,
        val remainingDaysInMonth: Int,
        val spentToday: Double = 0.0,
        val unallocatedBalance: Double = 0.0
    )

    data class TodaySummary(val todayTxCount: Int, val todayExpense: Double)

    data class HashtagSummary(
        val tag: String,
        val totalExpense: Double,
        val totalIncome: Double,
        val count: Int
    )

    data class DebtPayoffSimulation(
        val totalMonths: Int,
        val totalInterestPaid: Double,
        val orderOfPayoff: List<String>
    )

    fun computeSafeToSpend(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): SafeToSpendResult {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)

        var unallocatedTotal = 0.0
        for (acc in accounts) {
            if (acc.isArchived || acc.accountType.equals("Credit Card", true)) continue
            val bal = FinancialCalculator.balance(acc, transactions).coerceAtLeast(0.0)
            val allocated = parseAllocations(acc.allocationsJson).sumOf { it.allocatedAmount }
            unallocatedTotal += (bal - allocated).coerceAtLeast(0.0)
        }

        val todaySummary = computeTodayExpenseSummary(accounts, transactions, nowMillis)
        val dailyAllowance = unallocatedTotal / remainingDays.toDouble()
        val remainingToday = (dailyAllowance - todaySummary.todayExpense).coerceAtLeast(0.0)

        return SafeToSpendResult(
            remainingToday = remainingToday,
            dailyAllowance = dailyAllowance,
            remainingDaysInMonth = remainingDays,
            spentToday = todaySummary.todayExpense,
            unallocatedBalance = unallocatedTotal
        )
    }

    fun computeTodayExpenseSummary(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): TodaySummary {
        val todayKey = dayKey(nowMillis)
        val todayExpenses = transactions.filter {
            it.type.equals("EXPENSE", true) && dayKey(it.timestamp) == todayKey
        }
        return TodaySummary(
            todayTxCount = todayExpenses.size,
            todayExpense = todayExpenses.sumOf { it.amount }
        )
    }

    fun computeEmergencyRunwayMonths(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): Double {
        val liquidSavings = accounts.filter {
            !it.isArchived && !it.accountType.equals("Credit Card", true)
        }.sumOf { FinancialCalculator.balance(it, transactions).coerceAtLeast(0.0) }

        val monthlyExpense = transactions.filter { it.type.equals("EXPENSE", true) }.sumOf { it.amount }
        if (monthlyExpense <= 0.0) return if (liquidSavings > 0.0) 12.0 else 6.0
        return (liquidSavings / monthlyExpense).coerceAtLeast(0.1)
    }

    fun loanRemainingPaise(loan: LoanEntity, payments: List<LoanPaymentEntity>): Long {
        val paid = payments.filter { it.loanId == loan.id }.sumOf { it.amountPaise }
        return (loan.principalPaise - paid).coerceAtLeast(0L)
    }

    fun dayKey(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    fun computeSpendingDistribution(
        transactions: List<TransactionEntity>,
        categories: List<String>,
        window: SpendingChartWindow
    ): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        transactions.filter { it.type.equals("EXPENSE", true) }.forEach { tx ->
            result[tx.category] = (result[tx.category] ?: 0.0) + tx.amount
        }
        return result
    }

    fun computeCashDenominationTotal(lines: List<CashCountLine>): Double {
        return lines.sumOf { (it.value * it.count).toDouble() }
    }

    fun summarizeHashtags(transactions: List<TransactionEntity>): List<HashtagSummary> {
        val regex = Regex("#[A-Za-z0-9_]+")
        val map = mutableMapOf<String, Triple<Double, Double, Int>>()
        for (tx in transactions) {
            val combined = "${tx.note} ${tx.hashtags}"
            val tags = regex.findAll(combined).map { it.value.lowercase() }.toSet()
            for (tag in tags) {
                val cur = map[tag] ?: Triple(0.0, 0.0, 0)
                val exp = if (tx.type.equals("EXPENSE", true)) cur.first + tx.amount else cur.first
                val inc = if (tx.type.equals("INCOME", true)) cur.second + tx.amount else cur.second
                map[tag] = Triple(exp, inc, cur.third + 1)
            }
        }
        return map.entries.map { (tag, triple) ->
            HashtagSummary(tag, triple.first, triple.second, triple.third)
        }.sortedByDescending { it.totalExpense + it.totalIncome }
    }

    fun serializeAllocations(allocations: List<AccountAllocation>): String {
        val arr = JSONArray()
        for (a in allocations) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("category", a.category)
            obj.put("allocatedAmount", a.allocatedAmount)
            obj.put("percentage", a.percentage)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseAllocations(json: String?): List<AccountAllocation> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            val list = mutableListOf<AccountAllocation>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AccountAllocation(
                        id = obj.optString("id", "${i + 1}"),
                        category = obj.optString("category", "Bucket"),
                        allocatedAmount = obj.optDouble("allocatedAmount", 0.0),
                        percentage = obj.optDouble("percentage", 0.0)
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun applyAutoSplitToAllocations(account: AccountEntity, incomeAmount: Double): AccountEntity {
        val current = parseAllocations(account.allocationsJson)
        if (current.isEmpty() || incomeAmount <= 0.0) return account
        val updated = current.map { alloc ->
            if (alloc.percentage > 0.0) {
                val add = incomeAmount * (alloc.percentage / 100.0)
                alloc.copy(allocatedAmount = alloc.allocatedAmount + add)
            } else {
                alloc
            }
        }
        return account.copy(allocationsJson = serializeAllocations(updated))
    }

    fun adjustAllocationsForTransaction(
        account: AccountEntity,
        allocationId: String,
        txType: String,
        amount: Double
    ): AccountEntity {
        if (allocationId.isBlank()) return account
        val current = parseAllocations(account.allocationsJson)
        val updated = current.map { alloc ->
            if (alloc.id == allocationId || alloc.category.equals(allocationId, true)) {
                val next = if (txType.equals("INCOME", true)) {
                    alloc.allocatedAmount + amount
                } else {
                    (alloc.allocatedAmount - amount).coerceAtLeast(0.0)
                }
                alloc.copy(allocatedAmount = next)
            } else {
                alloc
            }
        }
        return account.copy(allocationsJson = serializeAllocations(updated))
    }

    fun computeRoundUpSpareChange(amount: Double, roundTo: Int = 10): Double {
        if (roundTo <= 1 || amount <= 0.0) return 0.0
        val rem = amount % roundTo.toDouble()
        return if (rem == 0.0) 0.0 else (roundTo.toDouble() - rem)
    }

    fun simulateDebtPayoff(
        loans: List<LoanEntity>,
        payments: List<LoanPaymentEntity>,
        extraMonthlyPayment: Double,
        isAvalanche: Boolean
    ): DebtPayoffSimulation {
        if (loans.isEmpty()) return DebtPayoffSimulation(0, 0.0, emptyList())
        data class MutableLoan(val name: String, var balance: Double, val minPayment: Double, val monthlyRate: Double)

        val sorted = if (isAvalanche) {
            loans.sortedByDescending { it.interestRate }
        } else {
            loans.sortedBy { it.principal }
        }

        val active = sorted.map { loan ->
            val paid = payments.filter { it.loanId == loan.id }.sumOf { if (it.amount > 0) it.amount else MoneyPaise.toRupees(it.amountPaise) }
            val startBal = ((if (loan.principal > 0) loan.principal else MoneyPaise.toRupees(loan.principalPaise)) - paid).coerceAtLeast(0.0)
            val minPay = (if (loan.monthlyPayment > 0) loan.monthlyPayment else MoneyPaise.toRupees(loan.emiPaise)).coerceAtLeast(100.0)
            MutableLoan(loan.name.ifBlank { loan.lender }, startBal, minPay, (loan.interestRate / 100.0) / 12.0)
        }.filter { it.balance > 0.0 }.toMutableList()

        var months = 0
        var totalInterest = 0.0
        val order = mutableListOf<String>()

        while (active.any { it.balance > 0.01 } && months < 600) {
            months++
            var extraAvailable = extraMonthlyPayment.coerceAtLeast(0.0)
            for (l in active) {
                if (l.balance <= 0.0) continue
                val interest = l.balance * l.monthlyRate
                totalInterest += interest
                l.balance += interest
                val pay = l.minPayment.coerceAtMost(l.balance)
                l.balance -= pay
                if (l.balance <= 0.01 && l.name !in order) {
                    order.add(l.name)
                }
            }
            for (l in active) {
                if (l.balance > 0.01 && extraAvailable > 0.0) {
                    val apply = extraAvailable.coerceAtMost(l.balance)
                    l.balance -= apply
                    extraAvailable -= apply
                    if (l.balance <= 0.01 && l.name !in order) {
                        order.add(l.name)
                    }
                }
            }
        }
        return DebtPayoffSimulation(months.coerceAtLeast(1), totalInterest, order)
    }
}
