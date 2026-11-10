package com.charan.fintrack.ui.nativeapp

import com.charan.fintrack.data.local.TransactionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionAccountFilterTest {
    @Test
    fun transferMatchesBothSourceAndDestinationAccountsWhenIncomingRowsAreHidden() {
        val transfer = TransactionEntity(
            id = 1,
            accountId = 10,
            type = "TRANSFER_OUT",
            amount = 25.0,
            amountPaise = 2_500,
            category = "Transfer",
            transferAccountId = 20,
            transferId = "pair-1"
        )

        assertTrue(transactionMatchesAccount(transfer, 10))
        assertTrue(transactionMatchesAccount(transfer, 20))
        assertFalse(transactionMatchesAccount(transfer, 30))
        assertTrue(transactionMatchesAccount(transfer, null))
    }

    @Test
    fun regularTransactionOnlyMatchesItsOwnAccount() {
        val expense = TransactionEntity(
            id = 2,
            accountId = 10,
            type = "EXPENSE",
            amount = 5.0,
            amountPaise = 500,
            category = "Food"
        )

        assertTrue(transactionMatchesAccount(expense, 10))
        assertFalse(transactionMatchesAccount(expense, 20))
    }
}
