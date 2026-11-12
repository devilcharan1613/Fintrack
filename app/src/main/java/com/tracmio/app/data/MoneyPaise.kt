package com.tracmio.app.data

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyPaise {
    fun toPaise(amount: Double): Long =
        BigDecimal.valueOf(amount).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()

    fun parsePaise(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "").removePrefix("₹").trim()
        if (cleaned.isEmpty()) return null
        return runCatching {
            val decimal = BigDecimal(cleaned)
            decimal.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }.getOrNull()
    }

    fun toRupees(paise: Long): Double = paise / 100.0

    fun formatInput(paise: Long): String {
        val bd = BigDecimal.valueOf(paise, 2).stripTrailingZeros()
        return bd.toPlainString()
    }
}
