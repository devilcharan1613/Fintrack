package com.tracemeo.app.data

import com.tracemeo.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Locale

object CsvParser {
    fun exportTransactionsToCsv(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("id,accountId,type,category,subcategory,amount,paymentMethod,upiApp,timestamp,note,hashtags\n")
        for (tx in transactions) {
            val safeNote = tx.note.replace("\"", "\"\"")
            val safeTags = tx.hashtags.replace("\"", "\"\"")
            sb.append("${tx.id},${tx.accountId},${tx.type},\"${tx.category}\",\"${tx.subcategory}\",${tx.amount},${tx.paymentMethod},${tx.upiApp},${tx.timestamp},\"$safeNote\",\"$safeTags\"\n")
        }
        return sb.toString()
    }

    fun parseCsvTransactions(csvText: String, defaultAccountId: Long = 1L): List<TransactionEntity> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size <= 1) return emptyList()
        val results = mutableListOf<TransactionEntity>()
        for (i in 1 until lines.size) {
            val cols = parseCsvRow(lines[i])
            if (cols.size < 4) continue
            val type = cols.getOrNull(2)?.uppercase() ?: "EXPENSE"
            val category = cols.getOrNull(3)?.ifBlank { "Other" } ?: "Other"
            val amount = cols.getOrNull(5)?.toDoubleOrNull()
                ?: cols.getOrNull(1)?.toDoubleOrNull()
                ?: 0.0
            val note = cols.getOrNull(9) ?: cols.getOrNull(4) ?: ""
            if (amount > 0.0) {
                results.add(
                    TransactionEntity(
                        accountId = defaultAccountId,
                        type = if (type in setOf("INCOME", "EXPENSE", "TRANSFER", "BORROW", "LEND")) type else "EXPENSE",
                        amount = amount,
                        category = category,
                        note = note
                    )
                )
            }
        }
        return results
    }

    private fun parseCsvRow(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    out.add(cur.toString().trim())
                    cur.clear()
                }
                else -> cur.append(ch)
            }
        }
        out.add(cur.toString().trim())
        return out
    }
}
