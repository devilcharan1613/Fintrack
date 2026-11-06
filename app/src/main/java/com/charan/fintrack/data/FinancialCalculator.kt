package com.charan.fintrack.data

import com.charan.fintrack.data.local.AccountEntity
import com.charan.fintrack.data.local.TransactionEntity

object FinancialCalculator {
    private val positiveTypes = setOf("INCOME", "TRANSFER_IN", "BORROW")

    fun balance(account: AccountEntity, transactions: List<TransactionEntity>): Double =
        account.balance + transactions.filter { it.accountId == account.id }.sumOf {
            if (it.type in positiveTypes) it.amount else -it.amount
        }

    fun netCashFlow(income: Double, expense: Double): Double = income - expense

    fun netCashFlow(transactions: List<TransactionEntity>): Double =
        transactions.sumOf { if (it.type in positiveTypes) it.amount else -it.amount }

    fun categoryTotal(
        transactions: List<TransactionEntity>,
        category: String,
        from: Long? = null,
        to: Long? = null
    ): Double = transactions
        .asSequence()
        .filter { it.type in setOf("EXPENSE", "LEND") && it.category.equals(category, ignoreCase = true) }
        .filter { from == null || it.timestamp >= from }
        .filter { to == null || it.timestamp < to }
        .sumOf { it.amount }

    fun progress(current: Double, target: Double): Double =
        if (target <= 0) 0.0 else (current / target).coerceIn(0.0, 1.0)

    fun monthlyEmi(principal: Double, annualRatePercent: Double, months: Int): Double {
        require(principal >= 0 && annualRatePercent >= 0 && months > 0)
        if (annualRatePercent == 0.0) return principal / months
        val monthlyRate = annualRatePercent / 1200.0
        val factor = Math.pow(1.0 + monthlyRate, months.toDouble())
        return principal * monthlyRate * factor / (factor - 1.0)
    }
}
