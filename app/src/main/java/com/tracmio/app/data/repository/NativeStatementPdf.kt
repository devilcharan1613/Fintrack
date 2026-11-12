package com.tracmio.app.data.repository

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.tracmio.app.data.TransactionDateTime
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.TransactionEntity
import java.io.OutputStream
import java.util.Locale

object NativeStatementPdf {
    fun write(
        outputStream: OutputStream,
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        periodLabel: String = "All Time"
    ) {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 150, 115)
            textSize = 16f
            isFakeBoldText = true
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 28, 40)
            textSize = 11f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 48, 60)
            textSize = 9.5f
        }
        val accountMap = accounts.associateBy({ it.id }, { it.name })

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = doc.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = 44f

        canvas.drawText("TRACMIO · OFFLINE FINANCE MANAGER", 36f, y, titlePaint)
        y += 20f
        canvas.drawText("Transaction Statement ($periodLabel)", 36f, y, headerPaint)
        y += 24f

        canvas.drawText("Date", 36f, y, headerPaint)
        canvas.drawText("Type", 110f, y, headerPaint)
        canvas.drawText("Category", 180f, y, headerPaint)
        canvas.drawText("Account", 300f, y, headerPaint)
        canvas.drawText("Method", 400f, y, headerPaint)
        canvas.drawText("Amount", 490f, y, headerPaint)
        y += 16f

        transactions.forEach { tx ->
            if (y > pageHeight - 48f) {
                doc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = doc.startPage(pageInfo)
                canvas = page.canvas
                y = 44f
            }
            val dateStr = TransactionDateTime.formatDateInput(tx.timestamp)
            val accName = accountMap[tx.accountId] ?: "#${tx.accountId}"
            val method = if (tx.upiApp.isNotBlank()) "${tx.paymentMethod} (${tx.upiApp})" else tx.paymentMethod
            val amtStr = String.format(Locale.US, "₹%.2f", tx.amount)

            canvas.drawText(dateStr.take(10), 36f, y, bodyPaint)
            canvas.drawText(tx.type.take(12), 110f, y, bodyPaint)
            canvas.drawText(tx.category.take(18), 180f, y, bodyPaint)
            canvas.drawText(accName.take(15), 300f, y, bodyPaint)
            canvas.drawText(method.take(14), 400f, y, bodyPaint)
            canvas.drawText(amtStr, 490f, y, bodyPaint)
            y += 15f
        }

        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }
}
