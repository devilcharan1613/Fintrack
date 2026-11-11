package com.charan.tracmio.data

import com.charan.tracmio.data.local.AccountEntity
import com.charan.tracmio.data.local.GoalContributionEntity
import com.charan.tracmio.data.local.GoalEntity
import com.charan.tracmio.data.local.LoanEntity
import com.charan.tracmio.data.local.LoanPaymentEntity
import com.charan.tracmio.data.local.PersonEntity
import com.charan.tracmio.data.local.TransactionEntity

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
}
