package com.charan.fintrack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionDateTimeTest {
    @Test
    fun parsesValidLocalDateAndTime() {
        val actual = parseTransactionDateTime("2026-11-10", "07:18")
        val expected = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            .parse("2026-11-10 07:18")!!.time
        assertEquals(expected, actual)
    }

    @Test
    fun rejectsImpossibleDatesAndTrailingCharacters() {
        assertThrows(IllegalArgumentException::class.java) {
            parseTransactionDateTime("2026-02-31", "12:00")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseTransactionDateTime("2026-11-10garbage", "12:00")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseTransactionDateTime("2026-11-10", "12:00garbage")
        }
    }
}
