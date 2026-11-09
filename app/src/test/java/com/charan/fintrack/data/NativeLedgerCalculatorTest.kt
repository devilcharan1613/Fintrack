package com.charan.fintrack.data

import com.charan.fintrack.data.local.AccountEntity
import com.charan.fintrack.data.local.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NativeLedgerCalculatorTest {
    @Test fun openingBalanceAndExpenseProduceExactBalance() {
        val account = AccountEntity(id = 1, name = "Cash", accountType = "Wallet", openingBalancePaise = 10_000)
        val rows = listOf(
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 25.0, amountPaise = 2_500, category = "Food"),
            TransactionEntity(accountId = 1, type = "INCOME", amount = 5.0, amountPaise = 500, category = "Gift")
        )
        assertEquals(8_000L, NativeLedgerCalculator.accountBalancePaise(account, rows))
    }

    @Test fun transferPairDoesNotChangeCombinedBalanceOrIncomeAndExpense() {
        val a = AccountEntity(id = 1, name = "A", accountType = "Bank", openingBalancePaise = 10_000)
        val b = AccountEntity(id = 2, name = "B", accountType = "Wallet", openingBalancePaise = 0)
        val rows = listOf(
            TransactionEntity(accountId = 1, type = "TRANSFER_OUT", amount = 30.0, amountPaise = 3_000, category = "Transfer", transferId = "x"),
            TransactionEntity(accountId = 2, type = "TRANSFER_IN", amount = 30.0, amountPaise = 3_000, category = "Transfer", transferId = "x")
        )
        assertEquals(10_000L, NativeLedgerCalculator.totalBalancePaise(listOf(a, b), rows))
        assertEquals(0L, NativeLedgerCalculator.incomePaise(rows))
        assertEquals(0L, NativeLedgerCalculator.expensePaise(rows))
    }

    @Test fun borrowedAndLentAmountsAreReconciledInNetWorth() {
        val account = AccountEntity(id = 3, name = "Cash", accountType = "Wallet", openingBalancePaise = 10_000)
        val rows = listOf(
            TransactionEntity(accountId = 3, type = "BORROW", amount = 5.0, amountPaise = 500, category = "Borrow"),
            TransactionEntity(accountId = 3, type = "LEND", amount = 2.0, amountPaise = 200, category = "Lend")
        )
        assertEquals(10_000L, NativeLedgerCalculator.netWorthPaise(listOf(account), rows))
    }

    @Test fun amountArithmeticDetectsOverflow() {
        val account = AccountEntity(id = 1, name = "Cash", accountType = "Wallet", openingBalancePaise = Long.MAX_VALUE)
        val rows = listOf(TransactionEntity(accountId = 1, type = "INCOME", amount = 1.0, amountPaise = 1, category = "Other"))
        assertThrows(ArithmeticException::class.java) {
            NativeLedgerCalculator.accountBalancePaise(account, rows)
        }
    }
}
