package com.tracemeo.app.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyPaise {
    @Volatile
    var activeCurrencySymbol: String = "₹"

    fun toPaise(amount: Double): Long =
        if (!amount.isFinite()) 0L
        else BigDecimal.valueOf(amount).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()

    fun parsePaise(raw: String): Long? {
        val cleaned = raw.trim()
            .replace(",", "")
            .removePrefix("₹")
            .removePrefix("$")
            .removePrefix("€")
            .removePrefix("£")
            .trim()
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

    fun formatRupees(paise: Long, masked: Boolean = false, symbol: String = activeCurrencySymbol): String {
        if (masked) return "${symbol}••••••"
        return formatCurrency(toRupees(paise), masked = false, symbol = symbol)
    }

    fun formatCurrency(amount: Double, masked: Boolean = false, symbol: String = activeCurrencySymbol): String {
        if (masked) return "${symbol}••••••"
        val sign = if (amount < 0) "-" else ""
        val absVal = kotlin.math.abs(amount)
        val locale = if (symbol == "₹") Locale("en", "IN") else Locale.US
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = if (absVal % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "${sign}${symbol}${nf.format(absVal)}"
    }
}
