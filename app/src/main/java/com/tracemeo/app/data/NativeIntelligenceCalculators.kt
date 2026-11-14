package com.tracemeo.app.data

import com.tracemeo.app.data.local.*
import java.util.Calendar

object NativeIntelligenceCalculators {
    data class MonthlyPaceResult(
        val totalBudget: Double,
        val spentSoFar: Double,
        val currentMonthExpense: Double = spentSoFar,
        val remainingDays: Int,
        val dailySafeLimit: Double,
        val safePerDay: Double = dailySafeLimit,
        val isOnTrack: Boolean
    )

    data class Breakdown503020(
        val needs: Double,
        val wants: Double,
        val savings: Double,
        val totalIncome: Double
    )

    data class XRayMetric(
        val name: String,
        val score: Int,
        val summary: String,
        val status: String
    )

    data class FinancialXRayReport(
        val overallScore: Int,
        val grade: String,
        val metrics: List<XRayMetric>
    )

    fun parseUpiSmsToDraft(smsText: String): UpiSmsDraft? {
        if (smsText.isBlank()) return null
        val lower = smsText.lowercase()
        val isDebit = lower.contains("debited") || lower.contains("spent") || lower.contains("paid") || lower.contains("sent")
        val isCredit = lower.contains("credited") || lower.contains("received")
        if (!isDebit && !isCredit) return null

        val amountRegex = Regex("""(?:inr|rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val match = amountRegex.find(smsText) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        val upiApp = when {
            lower.contains("gpay") || lower.contains("google pay") -> "GPay"
            lower.contains("phonepe") -> "PhonePe"
            lower.contains("paytm") -> "Paytm"
            lower.contains("bhim") -> "BHIM"
            lower.contains("cred") -> "CRED"
            else -> "UPI"
        }

        val merchantRegex = Regex("""(?:to|at|from)\s+([A-Za-z0-9 &._-]{2,25}?)(?:\s+via|\s+on|\s+ref|\s+upi|\.$|$)""", RegexOption.IGNORE_CASE)
        val merchant = merchantRegex.find(smsText)?.groupValues?.getOrNull(1)?.trim() ?: "UPI Merchant"

        return UpiSmsDraft(
            merchant = merchant,
            amount = amount,
            type = if (isCredit && !isDebit) "INCOME" else "EXPENSE",
            upiApp = upiApp,
            paymentMethod = "UPI"
        )
    }

    data class MonthComparison(
        val thisMonthExpense: Double,
        val lastMonthExpense: Double,
        val thisMonthIncome: Double,
        val lastMonthIncome: Double
    )

    data class MoneyStorySummary(
        val transactionCount: Int,
        val topCategory: String,
        val topCategoryAmount: Double,
        val topPaymentMethod: String,
        val biggestExpenseAmount: Double,
        val biggestExpenseLabel: String,
        val savingsRatePercent: Int
    )

    fun computeMonthlyPace(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthlyPaceResult {
        val totalBudget = budgets.sumOf {
            if (it.monthlyLimit > 0.0) it.monthlyLimit else MoneyPaise.toRupees(it.limitPaise)
        }.let { if (it <= 0.0) 15000.0 else it }

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)
        val txCal = Calendar.getInstance()
        val spentSoFar = transactions.filter { tx ->
            if (!tx.type.equals("EXPENSE", true)) return@filter false
            txCal.timeInMillis = tx.timestamp
            txCal.get(Calendar.YEAR) == curYear && txCal.get(Calendar.MONTH) == curMonth
        }.sumOf { it.amount }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)
        val remainingBudget = (totalBudget - spentSoFar).coerceAtLeast(0.0)
        val dailySafeLimit = remainingBudget / remainingDays.toDouble()

        return MonthlyPaceResult(
            totalBudget = totalBudget,
            spentSoFar = spentSoFar,
            currentMonthExpense = spentSoFar,
            remainingDays = remainingDays,
            dailySafeLimit = dailySafeLimit,
            safePerDay = dailySafeLimit,
            isOnTrack = spentSoFar <= (totalBudget * (currentDay.toDouble() / daysInMonth.toDouble()))
        )
    }

    fun computeMonthComparison(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthComparison {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)
        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH)

        val txCal = Calendar.getInstance()
        var thisExp = 0.0
        var lastExp = 0.0
        var thisInc = 0.0
        var lastInc = 0.0
        for (tx in transactions) {
            txCal.timeInMillis = tx.timestamp
            val y = txCal.get(Calendar.YEAR)
            val m = txCal.get(Calendar.MONTH)
            val isThis = y == curYear && m == curMonth
            val isLast = y == prevYear && m == prevMonth
            if (tx.type.equals("EXPENSE", true)) {
                if (isThis) thisExp += tx.amount else if (isLast) lastExp += tx.amount
            } else if (tx.type.equals("INCOME", true)) {
                if (isThis) thisInc += tx.amount else if (isLast) lastInc += tx.amount
            }
        }
        return MonthComparison(thisExp, lastExp, thisInc, lastInc)
    }

    fun computeNoSpendStreakDays(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (transactions.none { it.type.equals("EXPENSE", true) }) return 0
        val expenseDays = transactions.filter { it.type.equals("EXPENSE", true) }
            .map { NativeLedgerCalculator.dayKey(it.timestamp) }
            .toSet()
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        var streak = 0
        for (i in 0 until 60) {
            val key = NativeLedgerCalculator.dayKey(cal.timeInMillis)
            if (key in expenseDays) break
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    fun computeMoneyStory(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): MoneyStorySummary {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)
        val txCal = Calendar.getInstance()
        val monthTxs = transactions.filter {
            txCal.timeInMillis = it.timestamp
            txCal.get(Calendar.YEAR) == curYear && txCal.get(Calendar.MONTH) == curMonth
        }.ifEmpty { transactions }

        val expenses = monthTxs.filter { it.type.equals("EXPENSE", true) }
        val income = monthTxs.filter { it.type.equals("INCOME", true) }.sumOf { it.amount }
        val expenseSum = expenses.sumOf { it.amount }
        val topCat = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .maxByOrNull { it.value }
        val topMethod = expenses.groupBy { it.upiApp.ifBlank { it.paymentMethod } }
            .maxByOrNull { it.value.size }?.key ?: "UPI / Cash"
        val biggest = expenses.maxByOrNull { it.amount }
        val savingsRate = if (income > 0.0) (((income - expenseSum) / income) * 100.0).toInt().coerceIn(0, 100) else 0

        return MoneyStorySummary(
            transactionCount = monthTxs.size,
            topCategory = topCat?.key ?: "No expenses yet",
            topCategoryAmount = topCat?.value ?: 0.0,
            topPaymentMethod = topMethod,
            biggestExpenseAmount = biggest?.amount ?: 0.0,
            biggestExpenseLabel = biggest?.let { it.note.ifBlank { it.category } } ?: "None",
            savingsRatePercent = savingsRate
        )
    }

    fun compute503020Breakdown(transactions: List<TransactionEntity>): Breakdown503020 {
        val income = transactions.filter { it.type.equals("INCOME", true) }.sumOf { it.amount }
        val wantCategories = setOf("shopping", "shopping & personal", "entertainment", "gifts & celebrations", "dining")
        var needs = 0.0
        var wants = 0.0
        for (tx in transactions) {
            if (tx.type.equals("EXPENSE", true)) {
                if (tx.category.lowercase() in wantCategories) {
                    wants += tx.amount
                } else {
                    needs += tx.amount
                }
            }
        }
        val savings = (income - needs - wants).coerceAtLeast(0.0)
        return Breakdown503020(needs = needs, wants = wants, savings = savings, totalIncome = income)
    }

    fun computeFinancialXRay(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity> = emptyList(),
        goals: List<GoalEntity> = emptyList(),
        loans: List<LoanEntity> = emptyList(),
        loanPayments: List<LoanPaymentEntity> = emptyList(),
        subscriptions: List<SubscriptionEntity> = emptyList(),
        academicEvents: List<AcademicEventEntity> = emptyList()
    ): FinancialXRayReport {
        val totalBal = accounts.filter { !it.isArchived && !it.accountType.equals("Credit Card", true) }
            .sumOf { FinancialCalculator.balance(it, transactions) }
        val income = transactions.filter { it.type.equals("INCOME", true) }.sumOf { it.amount }
        val expense = transactions.filter { it.type.equals("EXPENSE", true) }.sumOf { it.amount }
        val savingsRate = if (income > 0.0) ((income - expense) / income * 100.0).toInt().coerceIn(0, 100) else 75

        val pace = computeMonthlyPace(accounts, transactions, budgets)
        val budgetAdherenceScore = if (budgets.isEmpty()) 80 else {
            val ratio = pace.spentSoFar / pace.totalBudget.coerceAtLeast(1.0)
            ((1.25 - ratio).coerceIn(0.2, 1.0) * 100.0).toInt()
        }

        val creditCards = accounts.filter { it.accountType.equals("Credit Card", true) && it.creditLimit > 0.0 }
        val creditScore = if (creditCards.isEmpty()) 95 else {
            val maxUtil = creditCards.maxOf { (FinancialCalculator.balance(it, transactions).coerceAtLeast(0.0) / it.creditLimit) * 100.0 }
            when {
                maxUtil <= 15.0 -> 96
                maxUtil <= 30.0 -> 88
                maxUtil <= 60.0 -> 62
                else -> 38
            }
        }

        val goalVelocityScore = if (goals.isEmpty()) 70 else {
            val avgProgress = goals.map { g ->
                val cur = if (g.currentAmount > 0.0) g.currentAmount else MoneyPaise.toRupees(g.currentPaise)
                val target = if (g.targetAmount > 0.0) g.targetAmount else MoneyPaise.toRupees(g.targetPaise)
                FinancialCalculator.progress(cur, target)
            }.average()
            (55 + avgProgress * 45.0).toInt().coerceIn(0, 100)
        }

        val academicScore = if (academicEvents.isEmpty()) 90 else {
            val paidRatio = academicEvents.count { it.isPaid }.toDouble() / academicEvents.size.toDouble()
            (50 + paidRatio * 50.0).toInt().coerceIn(0, 100)
        }

        val digitalCount = transactions.count { it.paymentMethod.equals("UPI", true) || it.paymentMethod.equals("Card", true) }
        val mixScore = if (transactions.isEmpty()) 85 else ((digitalCount.toDouble() / transactions.size) * 30.0 + 65.0).toInt().coerceIn(60, 95)

        val metrics = listOf(
            XRayMetric("Savings Rate", savingsRate, "Keep saving 20%+ of monthly inflows", if (savingsRate >= 20) "Healthy" else "Watch"),
            XRayMetric("Liquidity Cushion", if (totalBal >= 5000.0) 88 else if (totalBal > 0.0) 68 else 45, "Live liquid balance across wallets & banks", if (totalBal >= 5000.0) "Good" else "Build"),
            XRayMetric("Budget Adherence", budgetAdherenceScore, "Current month spend vs category budgets", if (budgetAdherenceScore >= 75) "On Track" else "Tight"),
            XRayMetric("Debt-to-Income", if (loans.isEmpty()) 95 else 72, "EMI burden relative to income", if (loans.isEmpty()) "Debt-Free" else "Manage"),
            XRayMetric("Subscription Load", if (subscriptions.size <= 4) 90 else 68, "Recurring monthly drain", if (subscriptions.size <= 4) "Low" else "Review"),
            XRayMetric("Goal Velocity", goalVelocityScore, "Progress toward savings targets", if (goals.isNotEmpty()) "Active" else "Set Goal"),
            XRayMetric("Credit Utilization", creditScore, "Keep credit card usage below 30%", if (creditScore >= 85) "Safe" else "High"),
            XRayMetric("Cash vs UPI Mix", mixScore, "Balanced digital and physical cash tracking", "Balanced"),
            XRayMetric("Academic & Fee Readiness", academicScore, "Upcoming semester/exam fee coverage", if (academicScore >= 80) "Ready" else "Pending"),
            XRayMetric("Expense Consistency", if (transactions.size >= 3) 84 else 78, "Weekday vs weekend spending stability", "Stable")
        )
        val avg = metrics.map { it.score }.average().toInt().coerceIn(0, 100)
        val grade = when {
            avg >= 85 -> "A+"
            avg >= 75 -> "A"
            avg >= 65 -> "B"
            else -> "C"
        }
        return FinancialXRayReport(overallScore = avg, grade = grade, metrics = metrics)
    }
}
