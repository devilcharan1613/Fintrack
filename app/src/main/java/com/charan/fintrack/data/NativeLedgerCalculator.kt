package com.charan.fintrack.data

import com.charan.fintrack.data.local.AccountEntity
import com.charan.fintrack.data.local.TransactionEntity

/**
 * Exact ledger arithmetic. The opening balance is the only account baseline;
 * every later balance change comes from a transaction row.
 */
object NativeLedgerCalculator {
    fun signedDeltaPaise(transaction: TransactionEntity): Long = when (transaction.type.uppercase()) {
        "INCOME", "BORROW", "TRANSFER_IN" -> transaction.amountPaise
        "EXPENSE", "LEND", "TRANSFER_OUT" -> Math.negateExact(transaction.amountPaise)
        else -> 0L
    }

    fun accountBalancePaise(account: AccountEntity, transactions: List<TransactionEntity>): Long {
        var result = account.openingBalancePaise
        transactions.asSequence().filter { it.accountId == account.id }.forEach {
            result = Math.addExact(result, signedDeltaPaise(it))
        }
        return result
    }

    fun totalBalancePaise(accounts: List<AccountEntity>, transactions: List<TransactionEntity>): Long =
        accounts.fold(0L) { total, account ->
            Math.addExact(total, accountBalancePaise(account, transactions))
        }

    fun incomePaise(transactions: List<TransactionEntity>): Long =
        sum(transactions) { it.type.equals("INCOME", true) }

    fun expensePaise(transactions: List<TransactionEntity>): Long =
        sum(transactions) { it.type.equals("EXPENSE", true) }

    fun borrowedPaise(transactions: List<TransactionEntity>): Long =
        sum(transactions) { it.type.equals("BORROW", true) }

    fun lentPaise(transactions: List<TransactionEntity>): Long =
        sum(transactions) { it.type.equals("LEND", true) }

    fun netWorthPaise(accounts: List<AccountEntity>, transactions: List<TransactionEntity>): Long {
        val cashAndAccounts = totalBalancePaise(accounts, transactions)
        return Math.addExact(
            Math.subtractExact(cashAndAccounts, borrowedPaise(transactions)),
            lentPaise(transactions)
        )
    }

    fun categoryExpensePaise(
        transactions: List<TransactionEntity>,
        category: String,
        fromInclusive: Long? = null,
        toExclusive: Long? = null
    ): Long = transactions.asSequence()
        .filter { it.type.equals("EXPENSE", true) }
        .filter { it.category.equals(category, ignoreCase = true) }
        .filter { fromInclusive == null || it.timestamp >= fromInclusive }
        .filter { toExclusive == null || it.timestamp < toExclusive }
        .fold(0L) { total, row -> Math.addExact(total, row.amountPaise) }

    private fun sum(
        transactions: List<TransactionEntity>,
        predicate: (TransactionEntity) -> Boolean
    ): Long = transactions.asSequence().filter(predicate)
        .fold(0L) { total, row -> Math.addExact(total, row.amountPaise) }
}
