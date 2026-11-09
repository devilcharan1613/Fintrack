package com.charan.fintrack.ui.nativeapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charan.fintrack.data.local.AccountEntity
import com.charan.fintrack.data.local.TransactionEntity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeStatementPdfInstrumentedTest {
    @Test
    fun createsReadablePdfForEmptyAndPopulatedLedgers() {
        val account = AccountEntity(
            id = 1,
            name = "Primary Bank",
            accountType = "Bank",
            openingBalancePaise = 125_000L
        )
        val transactions = (1..45).map { index ->
            TransactionEntity(
                accountId = 1,
                type = if (index % 2 == 0) "EXPENSE" else "INCOME",
                amount = 125.50,
                amountPaise = 12_550L,
                category = if (index % 2 == 0) "Food" else "Salary",
                note = "PDF export test row $index",
                timestamp = 1_800_000_000_000L + index * 60_000L
            )
        }

        val emptyPdf = NativeStatementPdf.create(listOf(account), emptyList())
        val populatedPdf = NativeStatementPdf.create(listOf(account), transactions)

        assertTrue("Empty-ledger PDF should contain a PDF document", emptyPdf.size > 100)
        assertTrue("Populated multi-page PDF should contain a PDF document", populatedPdf.size > emptyPdf.size)
        assertTrue("PDF output must begin with a PDF signature", populatedPdf.copyOfRange(0, 4).toString(Charsets.US_ASCII) == "%PDF")
    }
}
