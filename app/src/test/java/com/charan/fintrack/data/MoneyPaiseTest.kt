package com.charan.fintrack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyPaiseTest {
    @Test fun parsesRupeesExactlyIntoPaise() {
        assertEquals(12345L, MoneyPaise.fromRupees("123.45").paise)
        assertEquals(0L, MoneyPaise.fromRupees("0.00").paise)
        assertEquals(1L, MoneyPaise.fromRupees("0.01").paise)
    }

    @Test fun rejectsSubPaisePrecisionInsteadOfSilentlyRoundingUserInput() {
        assertThrows(ArithmeticException::class.java) {
            MoneyPaise.fromRupees("1.001")
        }
    }

    @Test fun convertsLegacyDoubleWithDocumentedHalfUpRule() {
        assertEquals(101L, MoneyPaise.fromLegacyRupees(1.005).paise)
        assertEquals(100L, MoneyPaise.fromLegacyRupees(1.004).paise)
    }

    @Test fun exactArithmeticDetectsOverflow() {
        assertEquals(300L, (MoneyPaise(100) + MoneyPaise(200)).paise)
        assertThrows(ArithmeticException::class.java) {
            MoneyPaise(Long.MAX_VALUE) + MoneyPaise(1)
        }
    }

    @Test fun rupeeDecimalRoundTripsWithoutFloatingPoint() {
        assertEquals("987654321.09", MoneyPaise(98_765_432_109L).toRupeesDecimal().toPlainString())
    }

    @Test fun rejectsNegativeOrNonFiniteLegacyAmounts() {
        assertThrows(IllegalArgumentException::class.java) { MoneyPaise.fromLegacyRupees(-1.0) }
        assertThrows(IllegalArgumentException::class.java) { MoneyPaise.fromLegacyRupees(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { MoneyPaise.fromLegacyRupees(Double.POSITIVE_INFINITY) }
    }
}
