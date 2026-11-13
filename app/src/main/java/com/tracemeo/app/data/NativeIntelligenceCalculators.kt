package com.tracemeo.app.data

import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.BudgetEntity
import com.tracemeo.app.data.local.GoalEntity
import com.tracemeo.app.data.local.LoanEntity
import com.tracemeo.app.data.local.LoanPaymentEntity
import com.tracemeo.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UpiSmsDraft(
    val type: String,
    val amount: Double,
    val note: String,
    val paymentMethod: String,
    val upiApp: String
)

data class MonthlyPaceSummary(
    val currentMonthExpense: Double,
    val monthlyBudgetTotal: Double,
    val paceRatio: Float,
    val expectedDayRatio: Float,
    val statusLabel: String,
    val daysLeft: Int,
    val safePerDay: Double
)

data class MonthlyBucket(
    val label: String,
    val yearMonth: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

data class FinancialXRayMetric(
    val label: String,
    val valueText: String,
    val scorePct: Int,
    val hint: String
)

data class FinancialXRayReport(
    val overallScore: Int,
    val gradeLabel: String,
    val netWorth: Double,
    val liquid: Double,
    val totalDebt: Double,
    val monthlySubs: Double,
    val unpaidFees: Double,
    val metrics: List<FinancialXRayMetric>,
    val observations: List<String>
)

data class NeedsWantsSavingsSummary(
    val needs: Double,
    val wants: Double,
    val savings: Double,
    val needsPct: Int,
    val wantsPct: Int,
    val savingsPct: Int
)

object NativeIntelligenceCalculators {

    fun parseUpiSmsToDraft(rawText: String): UpiSmsDraft? {
        val text = rawText.trim()
        if (text.length < 6) return null
        val lower = text.lowercase(Locale.US)
        val hasMoneyCue = listOf("debited", "credited", "paid", "spent", "received", "upi", "rs", "inr", "₹", "$", "€", "£", "vpa", "a/c")
            .any { it in lower }
        if (!hasMoneyCue) return null

        val amountRegex = Regex("""(?:rs\.?|inr|₹|\$|€|£)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val fallbackNumRegex = Regex("""\b([0-9]{1,7}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)\b""")
        val matchedAmtStr = (amountRegex.find(text)?.groupValues?.getOrNull(1)
            ?: fallbackNumRegex.find(text)?.groupValues?.getOrNull(1))
            ?.replace(",", "")
        val amount = matchedAmtStr?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null

        val isIncome = listOf("credited", "received", "refund", "cashback", "deposited").any { it in lower } &&
            !listOf("debited", "paid to", "spent").any { it in lower }
        val type = if (isIncome) "INCOME" else "EXPENSE"

        val upiApp = when {
            "gpay" in lower || "google pay" in lower -> "GPay"
            "phonepe" in lower -> "PhonePe"
            "paytm" in lower -> "Paytm"
            "bhim" in lower -> "BHIM"
            "upi" in lower -> "Other UPI"
            else -> ""
        }
        val paymentMethod = when {
            upiApp.isNotBlank() || "upi" in lower -> "UPI"
            "card" in lower || "pos" in lower -> "Card"
            else -> "Bank Transfer"
        }
        val merchantMatch = Regex("""(?:to|at|from|info[:\s]+)\s*([A-Za-z0-9 &._-]{3,28})""", RegexOption.IGNORE_CASE).find(text)
        val merchantHint = merchantMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val note = if (merchantHint.isNotBlank()) "SMS: $merchantHint" else text.take(80)

        return UpiSmsDraft(
            type = type,
            amount = amount,
            note = note,
            paymentMethod = paymentMethod,
            upiApp = upiApp
        )
    }

    fun computeMonthlyPace(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthlyPaceSummary {
        val curMonthKey = NativeLedgerCalculator.monthKey(nowMillis)
        val monthExpense = transactions
            .filter { it.type.equals("EXPENSE", true) && NativeLedgerCalculator.monthKey(it.timestamp) == curMonthKey }
            .sumOf { it.amount }
        val monthBudget = budgets
            .filter { it.month == curMonthKey }
            .sumOf { it.limitAmount }
            .takeIf { it > 0.0 }
            ?: transactions
                .filter { it.type.equals("INCOME", true) && NativeLedgerCalculator.monthKey(it.timestamp) == curMonthKey }
                .sumOf { it.amount }
                .takeIf { it > 0.0 }
            ?: NativeLedgerCalculator.totalBalance(accounts, transactions).coerceAtLeast(10000.0)

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val daysLeft = (maxDays - day + 1).coerceAtLeast(1)
        val expectedDayRatio = (day.toFloat() / maxDays.toFloat()).coerceIn(0.05f, 1f)
        val paceRatio = (monthExpense / monthBudget.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.5f)
        val remaining = (monthBudget - monthExpense).coerceAtLeast(0.0)
        val safePerDay = remaining / daysLeft

        val status = when {
            paceRatio > expectedDayRatio + 0.18f || paceRatio >= 0.95f -> "High Pace"
            paceRatio > expectedDayRatio + 0.05f -> "Caution"
            else -> "On Track"
        }

        return MonthlyPaceSummary(
            currentMonthExpense = monthExpense,
            monthlyBudgetTotal = monthBudget,
            paceRatio = paceRatio.coerceIn(0f, 1f),
            expectedDayRatio = expectedDayRatio,
            statusLabel = status,
            daysLeft = daysLeft,
            safePerDay = safePerDay
        )
    }

    fun buildMonthlyReportBuckets(
        transactions: List<TransactionEntity>,
        monthCount: Int = 6,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MonthlyBucket> {
        val labelFmt = SimpleDateFormat("MMM", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -(monthCount - 1))
        }
        return buildList {
            repeat(monthCount) {
                val ym = NativeLedgerCalculator.monthKey(cal.timeInMillis)
                val lbl = labelFmt.format(Date(cal.timeInMillis))
                val inc = transactions.filter {
                    NativeLedgerCalculator.monthKey(it.timestamp) == ym &&
                        it.type in setOf("INCOME", "BORROW")
                }.sumOf { it.amount }
                val exp = transactions.filter {
                    NativeLedgerCalculator.monthKey(it.timestamp) == ym &&
                        it.type in setOf("EXPENSE", "LEND")
                }.sumOf { it.amount }
                add(MonthlyBucket(label = lbl, yearMonth = ym, income = inc, expense = exp, net = inc - exp))
                cal.add(Calendar.MONTH, 1)
            }
        }
    }

    fun compute503020Breakdown(transactions: List<TransactionEntity>): NeedsWantsSavingsSummary {
        val needsCats = setOf("food", "groceries", "bills", "rent", "utilities", "health", "medical", "education", "fuel", "travel", "loan emi")
        val savingsCats = setOf("savings", "savings goal", "investment", "sip", "ppf", "rd", "fd")
        val expenses = transactions.filter { it.type.equals("EXPENSE", true) }
        var needs = 0.0
        var wants = 0.0
        var savings = 0.0
        expenses.forEach { tx ->
            val cat = tx.category.lowercase(Locale.US)
            when {
                savingsCats.any { it in cat } -> savings += tx.amount
                needsCats.any { it in cat } -> needs += tx.amount
                else -> wants += tx.amount
            }
        }
        val income = transactions.filter { it.type.equals("INCOME", true) }.sumOf { it.amount }
        val expenseSum = needs + wants + savings
        if (income > expenseSum) {
            savings += (income - expenseSum)
        }
        val denom = (needs + wants + savings).coerceAtLeast(1.0)
        return NeedsWantsSavingsSummary(
            needs = needs,
            wants = wants,
            savings = savings,
            needsPct = ((needs / denom) * 100.0).toInt().coerceIn(0, 100),
            wantsPct = ((wants / denom) * 100.0).toInt().coerceIn(0, 100),
            savingsPct = ((savings / denom) * 100.0).toInt().coerceIn(0, 100)
        )
    }

    fun computeFinancialXRay(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        goals: List<GoalEntity>,
        loans: List<LoanEntity>,
        loanPayments: List<LoanPaymentEntity>,
        subscriptions: List<SubscriptionItem>,
        academicEvents: List<AcademicEventItem>,
        currencySymbol: String = MoneyPaise.activeCurrencySymbol
    ): FinancialXRayReport {
        val runway = NativeLedgerCalculator.computeEmergencyRunwayMonths(accounts, transactions)
        val totalDebt = loans.sumOf { MoneyPaise.toRupees(NativeLedgerCalculator.loanRemainingPaise(it, loanPayments)) }
        val liquid = accounts.filter { !it.isArchived && !it.accountType.contains("Credit", true) }
            .sumOf { NativeLedgerCalculator.accountBalance(it, transactions) }
        val netWorth = liquid - totalDebt
        val income = NativeLedgerCalculator.totalIncome(transactions)
        val expense = NativeLedgerCalculator.totalExpense(transactions)
        val savingsRate = if (income > 0.0) ((income - expense) / income) * 100.0 else 0.0
        val monthlySubs = subscriptions.sumOf { if (it.billingCycle.equals("Yearly", true)) it.amount / 12.0 else it.amount }
        val unpaidFees = academicEvents.filter { !it.isPaid }.sumOf { it.feeAmount }
        val dtiRatio = if (income > 0.0) (totalDebt / income) * 100.0 else if (totalDebt > 0.0) 100.0 else 0.0
        val ccCards = accounts.filter { it.accountType.contains("Credit", true) && it.creditLimit > 0.0 }
        val maxCcUtil = ccCards.maxOfOrNull { NativeLedgerCalculator.creditCardUtilizationPct(it, transactions) } ?: 0.0

        val curMonth = NativeLedgerCalculator.monthKey(System.currentTimeMillis())
        val curBudgets = budgets.filter { it.month == curMonth && it.limitAmount > 0.0 }
        val budgetsWithinLimit = curBudgets.count { b ->
            val spent = transactions.filter {
                it.type.equals("EXPENSE", true) &&
                    it.category.equals(b.category, true) &&
                    NativeLedgerCalculator.monthKey(it.timestamp) == curMonth
            }.sumOf { it.amount }
            spent <= b.limitAmount
        }
        val budgetAdherence = if (curBudgets.isEmpty()) 85 else ((budgetsWithinLimit * 100.0) / curBudgets.size).toInt().coerceIn(0, 100)
        val avgMonthlyIncome = (income / 3.0).coerceAtLeast(1.0)
        val subBurnPct = if (income > 0.0) ((monthlySubs / avgMonthlyIncome) * 100.0).coerceIn(0.0, 100.0) else 0.0
        val streak = NativeLedgerCalculator.computeNoSpendStreakDays(transactions)
        val allocatedSum = accounts.filter { !it.isArchived }.sumOf { acc ->
            NativeLedgerCalculator.parseAllocations(acc.allocationsJson).sumOf { it.allocatedAmount }
        }
        val allocRate = if (liquid > 0.0) ((allocatedSum / liquid) * 100.0).coerceIn(0.0, 100.0) else 0.0
        val goalCount = goals.size
        val activeMonthsCount = transactions.map { NativeLedgerCalculator.monthKey(it.timestamp) }.distinct().size

        val m1 = FinancialXRayMetric("Savings Rate", "${savingsRate.toInt()}%", savingsRate.toInt().coerceIn(0, 100), if (savingsRate >= 20) "Healthy 20%+ cushion" else "Aim for 20%+ savings")
        val m2 = FinancialXRayMetric("Emergency Runway", "${String.format(Locale.US, "%.1f", runway)} mo", ((runway / 6.0) * 100).toInt().coerceIn(0, 100), "Target: 6+ months")
        val m3 = FinancialXRayMetric("Debt-to-Income", "${dtiRatio.toInt()}%", (100 - dtiRatio.toInt()).coerceIn(0, 100), if (totalDebt == 0.0) "Debt-free balance sheet" else "Keep total debt < 35%")
        val m4 = FinancialXRayMetric("Credit Card Util", "${maxCcUtil.toInt()}%", (100 - maxCcUtil.toInt()).coerceIn(0, 100), if (maxCcUtil <= 30.0) "Optimal (<30% threshold)" else "High (>30% alert)")
        val m5 = FinancialXRayMetric("Budget Adherence", "$budgetAdherence%", budgetAdherence, if (curBudgets.isEmpty()) "Add monthly budgets" else "$budgetsWithinLimit/${curBudgets.size} within limit")
        val m6 = FinancialXRayMetric("Subscription Burn", "${subBurnPct.toInt()}%", (100 - (subBurnPct * 2).toInt()).coerceIn(0, 100), "${MoneyPaise.formatCurrency(monthlySubs, false, currencySymbol)}/mo recurring")
        val m7 = FinancialXRayMetric("Liquid Reserves", MoneyPaise.formatCurrency(liquid, false, currencySymbol), if (liquid > 0) 90 else 20, "Available non-credit cash")
        val m8 = FinancialXRayMetric("Goal Coverage", "$goalCount active", (goalCount * 25).coerceIn(20, 100), "Dedicated savings goals")
        val m9 = FinancialXRayMetric("No-Spend Momentum", "${streak}d streak", (streak * 14).coerceIn(10, 100), "Consecutive zero-spend days")
        val m10 = FinancialXRayMetric("Pocket Allocation", "${allocRate.toInt()}%", allocRate.toInt().coerceIn(15, 100), "$activeMonthsCount mo ledger history")

        val metrics = listOf(m1, m2, m3, m4, m5, m6, m7, m8, m9, m10)
        val overall = (metrics.sumOf { it.scorePct } / metrics.size).coerceIn(0, 100)
        val grade = when {
            overall >= 85 -> "A+ Fortress"
            overall >= 70 -> "A Healthy"
            overall >= 55 -> "B Balanced"
            overall >= 40 -> "C Tight"
            else -> "D Critical"
        }
        val observations = buildList {
            if (maxCcUtil > 30.0) add("Credit card utilization (${maxCcUtil.toInt()}%) exceeds the 30% healthy ceiling.")
            if (unpaidFees > 0.0) add("Pending academic/fee obligations total ${MoneyPaise.formatCurrency(unpaidFees, false, currencySymbol)}.")
            if (runway < 3.0 && expense > 0.0) add("Emergency runway is below 3 months — allocate spare income into a Reserve pocket.")
            if (savingsRate >= 20.0) add("Strong ${savingsRate.toInt()}% savings rate puts you ahead of the 50/30/20 benchmark.")
            if (isEmpty()) add("All 10 financial vitals are in balanced territory. Keep logging daily for sharper forecasting.")
        }

        return FinancialXRayReport(
            overallScore = overall,
            gradeLabel = grade,
            netWorth = netWorth,
            liquid = liquid,
            totalDebt = totalDebt,
            monthlySubs = monthlySubs,
            unpaidFees = unpaidFees,
            metrics = metrics,
            observations = observations
        )
    }
}
