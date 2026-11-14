package com.tracmio.app.data.repository

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.NativeLedgerCalculator
import com.tracmio.app.data.TransactionDateTime
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.TransactionEntity
import java.io.FileOutputStream

object NativeStatementPdf {

    fun printReceipt(
        context: Context,
        tx: TransactionEntity,
        accountName: String
    ) {
        val lines = buildList {
            add("VERIFIED OFFLINE RECEIPT SLIP" to true)
            add("Type: ${tx.type}" to false)
            add("Amount: ${MoneyPaise.formatCurrency(tx.amount)}" to true)
            add("Category: ${tx.category}" + (if (tx.subcategory.isNotBlank()) " • ${tx.subcategory}" else "") to false)
            add("Account / Wallet: $accountName" to false)
            add("Method: ${tx.paymentMethod}" + (if (tx.upiApp.isNotBlank()) " (${tx.upiApp})" else "") to false)
            add("Date & Time: ${TransactionDateTime.formatDateInput(tx.timestamp)} ${TransactionDateTime.formatTimeInput(tx.timestamp)}" to false)
            if (tx.warrantyDate.isNotBlank()) add("Warranty / Deadline: ${tx.warrantyDate}" to false)
            if (tx.note.isNotBlank()) add("Note: ${tx.note}" to false)
            add("Receipt Ref: #TRC-${tx.id}" to false)
        }
        printLines(context, "Tracmio_Receipt_${tx.id}", "Tracmio Digital Receipt", lines)
    }

    fun printLedgerStatement(
        context: Context,
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ) {
        val totalBal = NativeLedgerCalculator.totalBalance(accounts, transactions)
        val totalInc = NativeLedgerCalculator.totalIncome(transactions)
        val totalExp = NativeLedgerCalculator.totalExpense(transactions)
        val lines = buildList {
            add("OFFLINE FINANCIAL STATEMENT · v16.13" to true)
            add("Total Net Worth: ${MoneyPaise.formatCurrency(totalBal)}" to true)
            add("Total Income: ${MoneyPaise.formatCurrency(totalInc)}   |   Total Expense: ${MoneyPaise.formatCurrency(totalExp)}" to false)
            add("------------------------------------------------------------" to false)
            add("ACCOUNTS (${accounts.size})" to true)
            accounts.forEach { acc ->
                val bal = NativeLedgerCalculator.accountBalance(acc, transactions)
                add("• ${acc.name} (${acc.accountType}): ${MoneyPaise.formatCurrency(bal)}" to false)
            }
            add("------------------------------------------------------------" to false)
            add("RECENT TRANSACTIONS (${transactions.size})" to true)
            transactions.take(60).forEach { tx ->
                val date = TransactionDateTime.formatDateInput(tx.timestamp)
                val sign = if (tx.type in setOf("INCOME", "BORROW", "TRANSFER_IN")) "+" else "-"
                add("$date | ${tx.category} | $sign${MoneyPaise.formatCurrency(tx.amount)} | ${tx.note.take(40)}" to false)
            }
        }
        printLines(context, "Tracmio_Statement", "Tracmio Ledger Statement", lines)
    }

    fun printCustomReport(
        context: Context,
        jobName: String,
        title: String,
        lines: List<Pair<String, Boolean>>
    ) {
        printLines(context, jobName, title, lines)
    }

    private fun printLines(
        context: Context,
        jobName: String,
        headerTitle: String,
        lines: List<Pair<String, Boolean>>
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val pageCount = ((lines.size / 36) + 1).coerceAtLeast(1)
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pageCount)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pdfDoc = PdfDocument()
                val titlePaint = Paint().apply {
                    color = Color.parseColor("#007A66")
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val boldPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val bodyPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 10.5f
                    isAntiAlias = true
                }

                val perPage = 36
                val chunks = lines.chunked(perPage).ifEmpty { listOf(emptyList()) }
                chunks.forEachIndexed { pageIdx, chunk ->
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIdx + 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    canvas.drawText(headerTitle, 40f, 52f, titlePaint)
                    var y = 84f
                    chunk.forEach { (text, isBold) ->
                        canvas.drawText(text, 40f, y, if (isBold) boldPaint else bodyPaint)
                        y += 19f
                    }
                    canvas.drawText("Generated 100% offline by Tracmio v16.13 · Page ${pageIdx + 1}", 40f, 810f, bodyPaint)
                    pdfDoc.finishPage(page)
                }

                runCatching {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }.onFailure { err ->
                    callback.onWriteFailed(err.message)
                }.also {
                    pdfDoc.close()
                }
            }
        }
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }
}
