package com.tracmio.app.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyPaise {
    fun toPaise(amount: Double): Long =
        if (!amount.isFinite()) 0L
        else BigDecimal.valueOf(amount).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()

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

    fun formatRupees(paise: Long, masked: Boolean = false): String {
        if (masked) return "₹••••••"
        val rupees = toRupees(paise)
        return formatCurrency(rupees, masked = false)
    }

    fun formatCurrency(amount: Double, masked: Boolean = false): String {
        if (masked) return "₹••••••"
        val sign = if (amount < 0) "-" else ""
        val absVal = kotlin.math.abs(amount)
        val nf = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = if (absVal % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "${sign}₹${nf.format(absVal)}"
    }
}
