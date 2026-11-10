package com.charan.fintrack.ui.nativeapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDateRangeTest {
    @Test
    fun emptyAndOneSidedRangesAreValid() {
        assertTrue(transactionDateRangeIsValid("", "", null, null))
        assertTrue(transactionDateRangeIsValid("2026-11-01", "", 100L, null))
        assertTrue(transactionDateRangeIsValid("", "2026-11-10", null, 200L))
    }

    @Test
    fun invalidDateInputDoesNotDisableTheFilter() {
        assertFalse(transactionDateRangeIsValid("2026-02-31", "", null, null))
        assertFalse(transactionDateRangeIsValid("", "not-a-date", null, null))
    }

    @Test
    fun reversedDateRangeIsRejected() {
        assertFalse(transactionDateRangeIsValid("2026-11-10", "2026-11-01", 200L, 100L))
        assertTrue(transactionDateRangeIsValid("2026-11-01", "2026-11-01", 100L, 150L))
    }
}
