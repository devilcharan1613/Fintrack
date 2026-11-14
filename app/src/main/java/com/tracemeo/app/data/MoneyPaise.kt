package com.tracemeo.app.data

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyPaise {
    fun toPaise(rupees: Double): Long {
        return (BigDecimal.valueOf(rupees).multiply(BigDecimal(100))).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    fun toRupees(paise: Long): Double {
        return BigDecimal.valueOf(paise).divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toDouble()
    }

    fun fromPaise(paise: Long): Double = toRupees(paise)

    fun formatRupees(paise: Long, symbol: String = "₹"): String {
        val r = toRupees(paise)
        return "$symbol%,.2f".format(r)
    }

    fun formatCurrency(amount: Double, symbol: String = "₹", stealthMode: Boolean = false, privacyMask: Boolean = false): String {
        if (privacyMask) return "$symbol••••••"
        val effective = if (stealthMode) {
            val hash = kotlin.math.abs(amount.hashCode() % 155) + 25
            hash.toDouble()
        } else {
            amount
        }
        return if (effective % 1.0 == 0.0) {
            "$symbol%,.0f".format(effective)
        } else {
            "$symbol%,.2f".format(effective)
        }
    }

    fun parsePaise(raw: String): Long {
        val cleaned = raw.replace(Regex("[^0-9.-]"), "").trim()
        val num = cleaned.toDoubleOrNull() ?: 0.0
        return toPaise(num)
    }
}
