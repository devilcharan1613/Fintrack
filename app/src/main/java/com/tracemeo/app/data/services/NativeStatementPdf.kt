package com.tracemeo.app.data.services

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TransactionEntity
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NativeStatementPdf {
    fun buildStatementPdfBytes(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        currencySymbol: String = "₹"
    ): ByteArray {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 150, 136)
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
        }

        var y = 48f
        canvas.drawText("TRACMIO · OFFLINE FINANCE MANAGER (v16.13)", 40f, y, titlePaint)
        y += 22f
        val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        canvas.drawText("Generated: ${fmt.format(Date())}", 40f, y, bodyPaint)
        y += 28f

        val totalBal = accounts.sumOf { com.tracemeo.app.data.FinancialCalculator.balance(it, transactions) }
        canvas.drawText("Total Net Balance Across ${accounts.size} Accounts: $currencySymbol${"%,.2f".format(totalBal)}", 40f, y, titlePaint)
        y += 26f

        canvas.drawText("Recent Transactions (${transactions.size} total):", 40f, y, bodyPaint)
        y += 18f

        transactions.take(28).forEach { tx ->
            val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(tx.timestamp))
            val line = "$dateStr  |  ${tx.type.padEnd(8)}  |  ${tx.category.take(18).padEnd(18)}  |  $currencySymbol${"%,.2f".format(tx.amount)}  |  ${tx.note.take(24)}"
            canvas.drawText(line, 40f, y, bodyPaint)
            y += 16f
        }

        doc.finishPage(page)
        val bos = ByteArrayOutputStream()
        doc.writeTo(bos)
        doc.close()
        return bos.toByteArray()
    }
}
