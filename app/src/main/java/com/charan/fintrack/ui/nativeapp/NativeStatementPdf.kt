package com.charan.fintrack.ui.nativeapp

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.charan.fintrack.data.MoneyPaise
import com.charan.fintrack.data.local.AccountEntity
import com.charan.fintrack.data.local.TransactionEntity
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds a local A4 transaction statement without WebView or an online service. */
internal object NativeStatementPdf {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 38f
    private const val RIGHT = 557f

    fun create(accounts: List<AccountEntity>, transactions: List<TransactionEntity>): ByteArray {
        val pdf = PdfDocument()
        val rows = transactions
            .filterNot { it.type.equals("TRANSFER_IN", true) }
            .sortedByDescending { it.timestamp }
        val accountNames = accounts.associate { it.id to it.name }
        val income = rows.filter { it.type.equals("INCOME", true) }
            .fold(0L) { total, row -> Math.addExact(total, row.amountPaise) }
        val expense = rows.filter { it.type.equals("EXPENSE", true) }
            .fold(0L) { total, row -> Math.addExact(total, row.amountPaise) }

        val title = paint(19f, true).apply { color = 0xFF073746.toInt() }
        val subtitle = paint(9f, false).apply { color = 0xFF526A76.toInt() }
        val footerSubtitle = paint(7.5f, false).apply {
            color = 0xFF526A76.toInt()
            textAlign = Paint.Align.RIGHT
        }
        val label = paint(8f, true).apply { color = 0xFF43616D.toInt() }
        val body = paint(9f, false).apply { color = 0xFF172F3B.toInt() }
        val amountPaint = paint(9f, true).apply { textAlign = Paint.Align.RIGHT }
        val headerPaint = paint(8f, true).apply { color = 0xFFFFFFFF.toInt() }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD3E1E6.toInt(); strokeWidth = 0.7f }
        val headerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF073746.toInt() }
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF0F7F9.toInt() }

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun startPage() {
            pageNumber++
            page = pdf.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            val canvas = page!!.canvas
            canvas.drawText("FINTRACK • OFFLINE FINANCE MANAGER", LEFT, 34f, label)
            canvas.drawText("Transaction Statement", LEFT, 62f, title)
            canvas.drawText(
                "Generated: " + SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date()),
                LEFT, 80f, subtitle
            )
            canvas.drawLine(LEFT, 94f, RIGHT, 94f, border)
            canvas.drawRoundRect(LEFT, 108f, RIGHT, 162f, 9f, 9f, cardFill)
            canvas.drawText("INCOME", LEFT + 13f, 127f, label)
            canvas.drawText("EXPENSE", LEFT + 186f, 127f, label)
            canvas.drawText("TRANSACTIONS", LEFT + 363f, 127f, label)
            canvas.drawText(money(income), LEFT + 13f, 148f, body)
            canvas.drawText(money(expense), LEFT + 186f, 148f, body)
            canvas.drawText(rows.size.toString(), LEFT + 363f, 148f, body)
            canvas.drawRect(LEFT, 180f, RIGHT, 202f, headerFill)
            canvas.drawText("DATE", LEFT + 7f, 194f, headerPaint)
            canvas.drawText("TYPE / DESCRIPTION", LEFT + 105f, 194f, headerPaint)
            canvas.drawText("ACCOUNT", LEFT + 350f, 194f, headerPaint)
            canvas.drawText("AMOUNT", RIGHT - 7f, 194f, headerPaint.apply { textAlign = Paint.Align.RIGHT })
            y = 218f
        }

        fun finishPage() {
            val current = page ?: return
            current.canvas.drawLine(LEFT, 811f, RIGHT, 811f, border)
            current.canvas.drawText("Private local record • Currency: INR", LEFT, 827f, subtitle)
            current.canvas.drawText("Page $pageNumber", RIGHT - 5f, 827f, footerSubtitle)
            pdf.finishPage(current)
            page = null
        }

        startPage()
        rows.forEachIndexed { index, row ->
            if (y > 780f) {
                finishPage()
                startPage()
            }
            val canvas = page!!.canvas
            val rowColor = if (index % 2 == 0) 0xFFF7FAFB.toInt() else 0xFFFFFFFF.toInt()
            val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rowColor }
            canvas.drawRect(LEFT, y - 12f, RIGHT, y + 24f, rowPaint)
            canvas.drawLine(LEFT, y + 24f, RIGHT, y + 24f, border)
            val date = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(row.timestamp))
            val isTransfer = row.type.equals("TRANSFER_OUT", true)
            val type = if (isTransfer) "Transfer" else row.type.replace('_', ' ').lowercase(Locale.getDefault())
                .replaceFirstChar { it.uppercase() }
            canvas.drawText(date, LEFT + 7f, y + 2f, body)
            canvas.drawText(type.take(15), LEFT + 105f, y - 2f, body.apply { typeface = Typeface.DEFAULT_BOLD })
            canvas.drawText(row.category.ifBlank { row.note }.take(31), LEFT + 105f, y + 12f, subtitle)
            val accountLabel = if (isTransfer) {
                (accountNames[row.accountId] ?: "Account") + " → " +
                    (row.transferAccountId?.let { accountNames[it] } ?: "Account")
            } else accountNames[row.accountId] ?: "Account"
            canvas.drawText(accountLabel.take(27), LEFT + 350f, y + 2f, body)
            val negative = row.type.equals("EXPENSE", true) || row.type.equals("LEND", true)
            val signed = if (isTransfer) "↔ " + money(row.amountPaise)
                else (if (negative) "−" else "+") + money(row.amountPaise)
            amountPaint.color = when {
                isTransfer -> 0xFF1689A1.toInt()
                negative -> 0xFFC74659.toInt()
                else -> 0xFF008B70.toInt()
            }
            canvas.drawText(signed, RIGHT - 7f, y + 2f, amountPaint)
            y += 39f
        }
        finishPage()

        return ByteArrayOutputStream().use { output ->
            pdf.writeTo(output)
            pdf.close()
            output.toByteArray()
        }
    }

    private fun paint(size: Float, bold: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private fun money(paise: Long): String =
        MoneyPaise.fromSignedPaise(paise).formatInr(Locale("en", "IN"))
}
