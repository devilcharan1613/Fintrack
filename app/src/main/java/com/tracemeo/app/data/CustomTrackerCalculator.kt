package com.tracemeo.app.data

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

object CustomTrackerCalculator {

    data class LinkedTransactionInput(
        val transactionUid: String,
        val type: String, // INCOME, EXPENSE, TRANSFER, REFUND, BORROW, LEND
        val amount: Double,
        val category: String,
        val date: String,
        val paymentMethod: String = "Cash",
        val accountName: String = "",
        val note: String = "",
        val isRefund: Boolean = false,
        val paidByPersonUid: String = "",
        val participantShares: Map<String, Double> = emptyMap() // personUid -> share amount
    )

    data class TrackerOverviewSummary(
        val totalIncome: Double,
        val totalExpenses: Double,
        val totalRefunds: Double,
        val totalTransfers: Double,
        val netExpenses: Double,
        val netCashFlow: Double,
        val budget: Double,
        val remainingBudget: Double,
        val budgetUtilizationPercent: Double,
        val transactionCount: Int,
        val categoryBreakdown: Map<String, Double>,
        val dailySpending: Map<String, Double>,
        val paymentMethodBreakdown: Map<String, Double>
    )

    data class PersonBalanceSummary(
        val personUid: String,
        val totalPaid: Double,
        val totalShareOwed: Double,
        val settlementsPaid: Double,
        val settlementsReceived: Double,
        val netBalance: Double, // Positive = receivable, Negative = owes others
        val amountOwed: Double,
        val amountReceivable: Double
    )

    data class SettlementInput(
        val fromPersonUid: String,
        val toPersonUid: String,
        val amount: Double
    )

    fun roundCurrency(value: Double): Double {
        if (!value.isFinite()) return 0.0
        return (value * 100.0).roundToLong() / 100.0
    }

    /**
     * Computes complete tracker overview metrics from linked transactions.
     * Deduplicates by transactionUid within a single tracker so duplicate links cannot inflate totals.
     * Note: Settlements are tracked separately and NEVER counted as additional income or expenses.
     */
    fun calculateOverview(
        transactions: List<LinkedTransactionInput>,
        budgetAmount: Double
    ): TrackerOverviewSummary {
        val uniqueTxns = transactions.distinctBy { it.transactionUid }
        var income = 0.0
        var expenses = 0.0
        var refunds = 0.0
        var transfers = 0.0

        val categoryMap = linkedMapOf<String, Double>()
        val dailyMap = linkedMapOf<String, Double>()
        val methodMap = linkedMapOf<String, Double>()

        for (tx in uniqueTxns) {
            val amt = abs(tx.amount)
            val normType = tx.type.trim().uppercase()
            when {
                tx.isRefund || normType == "REFUND" -> {
                    refunds += amt
                    val cat = tx.category.ifBlank { "Other" }
                    categoryMap[cat] = (categoryMap[cat] ?: 0.0) - amt
                }
                normType == "INCOME" || normType == "BORROW" -> {
                    income += amt
                }
                normType == "TRANSFER" || normType == "TRANSFER_IN" || normType == "TRANSFER_OUT" -> {
                    transfers += amt
                }
                else -> {
                    // EXPENSE or LEND
                    expenses += amt
                    val cat = tx.category.ifBlank { "Other" }
                    categoryMap[cat] = (categoryMap[cat] ?: 0.0) + amt
                    if (tx.date.isNotBlank()) {
                        dailyMap[tx.date] = (dailyMap[tx.date] ?: 0.0) + amt
                    }
                    val method = tx.paymentMethod.ifBlank { "Cash" }
                    methodMap[method] = (methodMap[method] ?: 0.0) + amt
                }
            }
        }

        val netExpenses = roundCurrency(max(0.0, expenses - refunds))
        val netCashFlow = roundCurrency(income - netExpenses)
        val cleanBudget = roundCurrency(max(0.0, budgetAmount))
        val remainingBudget = if (cleanBudget > 0.0) roundCurrency(cleanBudget - netExpenses) else 0.0
        val utilization = if (cleanBudget > 0.0) {
            roundCurrency((netExpenses / cleanBudget) * 100.0)
        } else {
            0.0
        }

        return TrackerOverviewSummary(
            totalIncome = roundCurrency(income),
            totalExpenses = roundCurrency(expenses),
            totalRefunds = roundCurrency(refunds),
            totalTransfers = roundCurrency(transfers),
            netExpenses = netExpenses,
            netCashFlow = netCashFlow,
            budget = cleanBudget,
            remainingBudget = remainingBudget,
            budgetUtilizationPercent = utilization,
            transactionCount = uniqueTxns.size,
            categoryBreakdown = categoryMap.mapValues { roundCurrency(max(0.0, it.value)) }.filterValues { it > 0.0 },
            dailySpending = dailyMap.mapValues { roundCurrency(it.value) },
            paymentMethodBreakdown = methodMap.mapValues { roundCurrency(it.value) }
        )
    }

    /**
     * Multiple Tracker Accounting Rule:
     * Each linked tracker displays the full transaction amount, while global FinTrack reports
     * deduplicate by original transactionUid so a ₹1,000 expense assigned to 3 trackers
     * counts as ₹1,000 in each tracker and ₹1,000 (once) in global expenses.
     */
    fun calculateGlobalDeduplicatedExpense(
        trackerAssignments: Map<String, List<LinkedTransactionInput>>
    ): Double {
        val uniqueGlobal = linkedMapOf<String, LinkedTransactionInput>()
        trackerAssignments.values.flatten().forEach { tx ->
            uniqueGlobal.putIfAbsent(tx.transactionUid, tx)
        }
        return calculateOverview(uniqueGlobal.values.toList(), 0.0).netExpenses
    }

    /**
     * Calculates equal split shares for a given amount across participant UIDs.
     */
    fun calculateEqualSplit(amount: Double, participantUids: List<String>): Map<String, Double> {
        val cleanParticipants = participantUids.distinct().filter { it.isNotBlank() }
        if (cleanParticipants.isEmpty() || amount <= 0.0) return emptyMap()
        val totalPaise = (amount * 100.0).roundToLong()
        val basePaise = totalPaise / cleanParticipants.size
        val remainder = (totalPaise % cleanParticipants.size).toInt()
        val result = linkedMapOf<String, Double>()
        cleanParticipants.forEachIndexed { index, uid ->
            val sharePaise = basePaise + if (index < remainder) 1L else 0L
            result[uid] = sharePaise / 100.0
        }
        return result
    }

    /**
     * Computes person-wise paid, share owed, settlements, and net receivable/owed balances.
     * Settlements adjust person balances without adding to tracker income or expense totals.
     */
    fun calculatePersonBalances(
        personUids: List<String>,
        transactions: List<LinkedTransactionInput>,
        settlements: List<SettlementInput>
    ): Map<String, PersonBalanceSummary> {
        val paidMap = mutableMapOf<String, Double>()
        val shareMap = mutableMapOf<String, Double>()
        val settledOutMap = mutableMapOf<String, Double>()
        val settledInMap = mutableMapOf<String, Double>()

        personUids.forEach { uid ->
            paidMap[uid] = 0.0
            shareMap[uid] = 0.0
            settledOutMap[uid] = 0.0
            settledInMap[uid] = 0.0
        }

        for (tx in transactions.distinctBy { it.transactionUid }) {
            val normType = tx.type.trim().uppercase()
            if (normType != "EXPENSE" && normType != "LEND") continue
            val amt = abs(tx.amount)
            if (tx.paidByPersonUid.isNotBlank()) {
                paidMap[tx.paidByPersonUid] = (paidMap[tx.paidByPersonUid] ?: 0.0) + amt
            }
            val shares = if (tx.participantShares.isNotEmpty()) {
                tx.participantShares
            } else if (personUids.isNotEmpty()) {
                calculateEqualSplit(amt, personUids)
            } else {
                emptyMap()
            }
            shares.forEach { (uid, share) ->
                shareMap[uid] = (shareMap[uid] ?: 0.0) + share
            }
        }

        for (s in settlements) {
            val amt = max(0.0, s.amount)
            settledOutMap[s.fromPersonUid] = (settledOutMap[s.fromPersonUid] ?: 0.0) + amt
            settledInMap[s.toPersonUid] = (settledInMap[s.toPersonUid] ?: 0.0) + amt
        }

        val allUids = (personUids + paidMap.keys + shareMap.keys + settledOutMap.keys + settledInMap.keys).distinct()
        return allUids.associateWith { uid ->
            val paid = roundCurrency(paidMap[uid] ?: 0.0)
            val share = roundCurrency(shareMap[uid] ?: 0.0)
            val sOut = roundCurrency(settledOutMap[uid] ?: 0.0)
            val sIn = roundCurrency(settledInMap[uid] ?: 0.0)
            // Net balance: (paid - share) + (settlementsPaid - settlementsReceived)
            val net = roundCurrency((paid - share) + (sOut - sIn))
            PersonBalanceSummary(
                personUid = uid,
                totalPaid = paid,
                totalShareOwed = share,
                settlementsPaid = sOut,
                settlementsReceived = sIn,
                netBalance = net,
                amountOwed = if (net < 0.0) roundCurrency(-net) else 0.0,
                amountReceivable = if (net > 0.0) net else 0.0
            )
        }
    }

    /**
     * Evaluates budget threshold alerts (50%, 75%, 90%, 100%, custom %) and returns newly crossed
     * thresholds that have not yet fired, plus active thresholds after any refund/adjustment.
     */
    fun evaluateBudgetThresholds(
        netExpenses: Double,
        budgetAmount: Double,
        enabledThresholds: List<Int>,
        alreadyFiredThresholds: Set<Int>
    ): Pair<List<Int>, Set<Int>> {
        if (budgetAmount <= 0.0) return emptyList<Int>() to emptySet()
        val utilization = (netExpenses / budgetAmount) * 100.0
        val sortedThresholds = enabledThresholds.filter { it > 0 }.distinct().sorted()
        val currentlyActive = sortedThresholds.filter { utilization >= it }.toSet()
        val newlyCrossed = sortedThresholds.filter { utilization >= it && it !in alreadyFiredThresholds }
        return newlyCrossed to currentlyActive
    }

    /**
     * Computes next occurrence ISO date (YYYY-MM-DD) for recurring trackers without duplicates.
     */
    fun nextRecurrenceDate(
        currentStartDateIso: String,
        frequency: String,
        customIntervalDays: Int = 30
    ): String {
        val base = runCatching { LocalDate.parse(currentStartDateIso) }.getOrElse { LocalDate.now() }
        val next = when (frequency.trim().uppercase()) {
            "DAILY" -> base.plusDays(1)
            "WEEKLY" -> base.plusWeeks(1)
            "MONTHLY" -> base.plusMonths(1)
            "YEARLY" -> base.plusYears(1)
            "CUSTOM" -> base.plusDays(max(1, customIntervalDays).toLong())
            else -> base
        }
        return next.toString()
    }

    /**
     * Validates custom field values according to fieldType.
     */
    fun validateCustomFieldValue(
        fieldType: String,
        rawValue: String,
        allowedOptions: List<String> = emptyList()
    ): Boolean {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return true
        return when (fieldType.trim().uppercase()) {
            "TEXT" -> true
            "NUMBER" -> trimmed.toDoubleOrNull()?.isFinite() == true
            "DATE" -> runCatching { LocalDate.parse(trimmed) }.isSuccess
            "CHECKBOX" -> trimmed.lowercase() in setOf("true", "false", "1", "0", "yes", "no")
            "DROPDOWN" -> allowedOptions.isEmpty() || trimmed in allowedOptions
            "MULTI_SELECT" -> {
                if (allowedOptions.isEmpty()) true
                else trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }.all { it in allowedOptions }
            }
            else -> false
        }
    }

    /**
     * Rule-based offline suggestion engine: scores unlinked transactions based on
     * tracker date window, tracker keywords/name, category affinity, and merchant/note match.
     */
    fun suggestTransactionsForTracker(
        trackerName: String,
        trackerType: String,
        startDateIso: String,
        endDateIso: String,
        alreadyLinkedUids: Set<String>,
        dismissedUids: Set<String>,
        candidateTransactions: List<LinkedTransactionInput>
    ): List<LinkedTransactionInput> {
        val keywords = (trackerName.lowercase().split(Regex("\\s+")) + trackerType.lowercase().split(Regex("\\s+")))
            .map { it.trim() }
            .filter { it.length >= 3 }
            .toSet()

        return candidateTransactions.filter { tx ->
            if (tx.transactionUid in alreadyLinkedUids || tx.transactionUid in dismissedUids) return@filter false
            var score = 0
            if (startDateIso.isNotBlank() && endDateIso.isNotBlank() && tx.date.isNotBlank()) {
                if (tx.date >= startDateIso && tx.date <= endDateIso) score += 2
            }
            val textBlob = "${tx.note} ${tx.category}".lowercase()
            if (keywords.any { kw -> textBlob.contains(kw) }) {
                score += 2
            }
            score >= 2
        }
    }
}
