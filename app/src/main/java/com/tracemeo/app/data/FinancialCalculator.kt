package com.tracemeo.app.data

import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TransactionEntity
import kotlin.math.pow

object FinancialCalculator {
    fun balance(account: AccountEntity, transactions: List<TransactionEntity>): Double {
        var bal = if (account.balance != 0.0) account.balance else MoneyPaise.toRupees(account.balancePaise)
        for (tx in transactions) {
            if (tx.accountId == account.id) {
                when (tx.type.uppercase()) {
                    "INCOME", "TRANSFER_IN", "BORROW" -> bal += tx.amount
                    "EXPENSE", "TRANSFER_OUT", "LEND", "TRANSFER" -> bal -= tx.amount
                }
            }
        }
        return bal
    }

    fun netCashFlow(income: Double, expense: Double): Double = income - expense

    fun netCashFlow(transactions: List<TransactionEntity>): Double {
        var net = 0.0
        for (tx in transactions) {
            when (tx.type.uppercase()) {
                "INCOME", "TRANSFER_IN", "BORROW" -> net += tx.amount
                "EXPENSE", "TRANSFER_OUT", "LEND", "TRANSFER" -> net -= tx.amount
            }
        }
        return net
    }

    fun categoryTotal(
        transactions: List<TransactionEntity>,
        category: String,
        startMillis: Long = Long.MIN_VALUE,
        endMillis: Long = Long.MAX_VALUE
    ): Double {
        return transactions.filter {
            it.type.equals("EXPENSE", ignoreCase = true) &&
                it.category.equals(category, ignoreCase = true) &&
                it.timestamp in startMillis..endMillis
        }.sumOf { it.amount }
    }

    fun monthlyEmi(principal: Double, annualInterestRate: Double, months: Int): Double {
        if (months <= 0) return 0.0
        if (annualInterestRate <= 0.0) return principal / months
        val r = (annualInterestRate / 12.0) / 100.0
        val factor = (1.0 + r).pow(months.toDouble())
        return (principal * r * factor) / (factor - 1.0)
    }

    fun progress(current: Double, target: Double): Double {
        if (target <= 0.0) return 0.0
        return (current / target).coerceIn(0.0, 1.0)
    }

    fun evaluateMathExpression(expr: String): Double? {
        val cleaned = expr.replace("₹", "").replace(",", "").replace(" ", "").trim()
        if (cleaned.isEmpty()) return null
        return runCatching { SimpleMathParser(cleaned).parse() }.getOrNull()
    }

    private class SimpleMathParser(private val str: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw IllegalArgumentException("Unexpected char")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val denom = parseFactor()
                        x = if (denom == 0.0) 0.0 else x / denom
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Unexpected character")
            }
            return x
        }
    }
}
