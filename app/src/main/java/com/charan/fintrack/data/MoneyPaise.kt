package com.charan.fintrack.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Exact INR amount represented as signed 64-bit paise.
 *
 * This is the migration-safe money primitive. Room entities must be migrated to Long
 * columns before replacing their legacy Double fields; do not change a persisted
 * Room entity in isolation because that requires a schema migration.
 */
@JvmInline
value class MoneyPaise(val paise: Long) : Comparable<MoneyPaise> {
    operator fun plus(other: MoneyPaise): MoneyPaise =
        MoneyPaise(Math.addExact(paise, other.paise))

    operator fun minus(other: MoneyPaise): MoneyPaise =
        MoneyPaise(Math.subtractExact(paise, other.paise))

    fun toRupeesDecimal(): BigDecimal = BigDecimal.valueOf(paise, 2)

    fun formatInr(locale: Locale = Locale("en", "IN")): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = java.util.Currency.getInstance("INR")
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(toRupeesDecimal())

    override fun compareTo(other: MoneyPaise): Int = paise.compareTo(other.paise)

    companion object {
        val ZERO = MoneyPaise(0)

        /** Parse a rupee amount without passing through binary floating point. */
        fun fromRupees(value: String): MoneyPaise {
            val decimal = BigDecimal(value.trim())
            require(decimal.signum() >= 0) { "Money amount cannot be negative" }
            return MoneyPaise(
                decimal.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
            )
        }

        /**
         * Convert an existing legacy Double rupee value. BigDecimal.valueOf uses the
         * canonical decimal representation of the Double. More than two decimal
         * places are rounded HALF_UP to the nearest paise.
         */
        fun fromLegacyRupees(value: Double): MoneyPaise {
            require(value.isFinite() && value >= 0.0) { "Legacy money amount must be finite and non-negative" }
            val paise = BigDecimal.valueOf(value)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
            return MoneyPaise(paise)
        }

        /** HALF_UP conversion for signed opening balances, including credit balances. */
        fun fromLegacySignedRupees(value: Double): MoneyPaise {
            require(value.isFinite()) { "Legacy balance must be finite" }
            val paise = BigDecimal.valueOf(value)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
            return MoneyPaise(paise)
        }

        /** Signed factory for internal ledger deltas and balance adjustments. */
        fun fromSignedPaise(paise: Long): MoneyPaise = MoneyPaise(paise)
    }
}
