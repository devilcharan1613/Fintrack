package com.charan.fintrack

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds local-only transaction exports. The input is a snapshot prepared from the
 * currently selected date range in the WebView; this class never reads or mutates
 * the financial database.
 */
object TransactionReportExporter {
    fun createPdf(data: JSONObject): ByteArray {
        val document = PdfDocument()
        val width = 595
        val height = 842
        val margin = 40f
        val contentWidth = width - margin * 2
        val ink = 0xff17212b.toInt()
        val muted = 0xff687583.toInt()
        val teal = 0xff009f82.toInt()
        val pale = 0xffeaf7f4.toInt()
        val line = 0xffd9e2e7.toInt()
        var pageNumber = 0
        var page = document.startPage(PdfDocument.PageInfo.Builder(width, height, ++pageNumber).create())
        var canvas = page.canvas
        var y = 48f

        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 9.5f; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 8f }
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 10f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 22f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = teal; style = Paint.Style.FILL }
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 0.8f }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pale; style = Paint.Style.FILL }
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xffffffff.toInt(); textSize = 8f; typeface = Typeface.DEFAULT_BOLD }

        fun footer() {
            canvas.drawLine(margin, 805f, width - margin, 805f, rule)
            canvas.drawText("Generated locally by FinTrack  •  Personal financial record", margin, 820f, small)
            val pageText = "Page $pageNumber"
            canvas.drawText(pageText, width - margin - small.measureText(pageText), 820f, small)
        }
        fun nextPage() {
            footer()
            document.finishPage(page)
            page = document.startPage(PdfDocument.PageInfo.Builder(width, height, ++pageNumber).create())
            canvas = page.canvas
            y = 48f
            canvas.drawText("FinTrack Transaction Statement  •  Continued", margin, y, bold)
            y += 24f
        }
        fun ensure(space: Float) { if (y + space > 785f) nextPage() }
        fun heading(label: String) {
            ensure(28f)
            y += 8f
            canvas.drawText(label, margin, y, bold)
            y += 7f
            canvas.drawLine(margin, y, width - margin, y, rule)
            y += 14f
        }
        fun value(value: Double): String = (data.optString("currency", "₹")) +
            String.format(Locale.US, "%,.2f", value)
        fun clipped(text: String, paint: Paint, maxWidth: Float): String {
            var out = text.replace("\n", " ").replace("\r", " ")
            while (out.isNotEmpty() && paint.measureText(out) > maxWidth) out = out.dropLast(1)
            return if (out.length < text.length && out.length > 2) out.dropLast(1) + "…" else out
        }
        fun table(headers: List<String>, rows: List<List<String>>, widths: List<Float>) {
            val rowH = 22f
            ensure(rowH + 8f)
            var x = margin
            canvas.drawRoundRect(RectF(margin, y - 13f, width - margin, y + 8f), 4f, 4f, fill)
            headers.forEachIndexed { index, header ->
                val p = Paint(bold).apply { textSize = 7.5f }
                canvas.drawText(clipped(header, p, widths[index] - 8f), x + 4f, y, p)
                x += widths[index]
            }
            y += 17f
            rows.forEach { row ->
                ensure(rowH)
                x = margin
                row.forEachIndexed { index, cell ->
                    val p = Paint(body).apply { textSize = 7.3f }
                    canvas.drawText(clipped(cell, p, widths[index] - 8f), x + 4f, y, p)
                    x += widths[index]
                }
                canvas.drawLine(margin, y + 5f, width - margin, y + 5f, rule)
                y += rowH
            }
            y += 5f
        }

        val summary = data.optJSONObject("summary") ?: JSONObject()
        val accounts = data.optJSONArray("accounts") ?: JSONArray()
        val categories = data.optJSONArray("categories") ?: JSONArray()
        val transactions = data.optJSONArray("transactions") ?: JSONArray()
        val chart = data.optJSONArray("chart") ?: JSONArray()

        canvas.drawText("FinTrack", margin, y, Paint(titlePaint).apply { color = teal; textSize = 12f })
        y += 28f
        canvas.drawText(data.optString("title", "Transaction Statement"), margin, y, titlePaint)
        y += 20f
        canvas.drawText("Period: ${data.optString("startDate")} to ${data.optString("endDate")}", margin, y, body)
        y += 14f
        canvas.drawText("Generated: ${data.optString("generatedAt")}", margin, y, small)
        y += 18f
        canvas.drawLine(margin, y, width - margin, y, accent)
        y += 20f

        heading("FINANCIAL SUMMARY")
        val cards = listOf(
            "Income" to summary.optDouble("income"),
            "Expenses" to summary.optDouble("expense"),
            "Net savings" to summary.optDouble("netSavings"),
            "Transactions" to summary.optDouble("transactionCount").toDouble()
        )
        val cardW = (contentWidth - 18f) / 2f
        cards.forEachIndexed { index, item ->
            val col = index % 2
            val row = index / 2
            val left = margin + col * (cardW + 18f)
            val top = y + row * 42f
            canvas.drawRoundRect(RectF(left, top - 15f, left + cardW, top + 18f), 6f, 6f, fill)
            canvas.drawText(item.first, left + 9f, top - 2f, small)
            canvas.drawText(if (item.first == "Transactions") item.second.toInt().toString() else value(item.second), left + 9f, top + 12f, bold)
        }
        y += 92f
        ensure(18f)
        canvas.drawText("Current account balance: " + value(summary.optDouble("currentBalance")), margin, y, bold)
        y += 18f

        heading("INCOME VS EXPENSE CHART")
        if (chart.length() == 0) {
            canvas.drawText("No monthly income or expense activity in this period.", margin, y, small); y += 18f
        } else {
            val shown = (maxOf(0, chart.length() - 12) until chart.length()).map { chart.getJSONObject(it) }
            val maxValue = shown.maxOfOrNull { maxOf(it.optDouble("income"), it.optDouble("expense")) }?.coerceAtLeast(1.0) ?: 1.0
            shown.forEach { item ->
                ensure(24f)
                val label = item.optString("month")
                canvas.drawText(label, margin, y, small)
                val startX = margin + 65f
                val maxBar = contentWidth - 145f
                val incW = (item.optDouble("income") / maxValue * maxBar).toFloat()
                val expW = (item.optDouble("expense") / maxValue * maxBar).toFloat()
                canvas.drawRect(startX, y - 8f, startX + incW, y - 2f, accent)
                canvas.drawRect(startX, y + 1f, startX + expW, y + 7f, Paint(accent).apply { color = 0xffe45b63.toInt() })
                canvas.drawText("I " + value(item.optDouble("income")), startX + maxBar + 6f, y - 2f, small)
                canvas.drawText("E " + value(item.optDouble("expense")), startX + maxBar + 6f, y + 8f, small)
                y += 23f
            }
        }

        heading("CATEGORY BREAKDOWN")
        val topCats = (0 until categories.length()).map { categories.getJSONObject(it) }
            .sortedByDescending { it.optDouble("expense") }.take(8)
        if (topCats.isEmpty()) {
            canvas.drawText("No categorized income or expense activity.", margin, y, small); y += 18f
        } else {
            val maxValue = topCats.maxOfOrNull { it.optDouble("expense") }?.coerceAtLeast(1.0) ?: 1.0
            topCats.forEach { item ->
                ensure(22f)
                canvas.drawText(clipped(item.optString("name"), body, 105f), margin, y, body)
                val barX = margin + 110f
                val barW = (item.optDouble("expense") / maxValue * 220f).toFloat()
                canvas.drawRoundRect(RectF(barX, y - 9f, barX + barW, y - 2f), 3f, 3f, accent)
                canvas.drawText(value(item.optDouble("expense")), barX + 228f, y - 2f, small)
                y += 20f
            }
        }

        heading("ACCOUNT BREAKDOWN")
        val accountRows = (0 until accounts.length()).map { accounts.getJSONObject(it) }.map {
            listOf(it.optString("name"), value(it.optDouble("balance")), value(it.optDouble("income")), value(it.optDouble("expense")),
                value(it.optDouble("transferIn") + it.optDouble("transferOut")), value(it.optDouble("netFlow")))
        }
        table(listOf("Account", "Balance", "Income", "Expense", "Transfers", "Net flow"), accountRows, listOf(105f, 82f, 78f, 78f, 82f, 90f))

        heading("CATEGORY TOTALS")
        val categoryRows = (0 until categories.length()).map { categories.getJSONObject(it) }.map {
            listOf(it.optString("name"), value(it.optDouble("income")), value(it.optDouble("expense")), it.optInt("count").toString())
        }
        table(listOf("Category", "Income", "Expense", "Count"), categoryRows, listOf(190f, 105f, 105f, 115f))

        heading("TRANSACTION LEDGER")
        val ledgerRows = (0 until transactions.length()).map { transactions.getJSONObject(it) }.map {
            val type = it.optString("type").replaceFirstChar { ch -> ch.uppercase() }
            val detail = listOf(it.optString("category"), it.optString("account"), it.optString("toAccount"), it.optString("notes"))
                .filter { s -> s.isNotBlank() && s != "—" }.joinToString(" · ")
            listOf(it.optString("date"), type, it.optString("account"), it.optString("category"),
                value(it.optDouble("amount")), detail)
        }
        table(listOf("Date", "Type", "Account", "Category", "Amount", "Details"), ledgerRows,
            listOf(65f, 58f, 90f, 100f, 82f, 120f))

        footer()
        document.finishPage(page)
        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    fun createDocx(data: JSONObject): ByteArray {
        val summary = data.optJSONObject("summary") ?: JSONObject()
        val accounts = data.optJSONArray("accounts") ?: JSONArray()
        val categories = data.optJSONArray("categories") ?: JSONArray()
        val transactions = data.optJSONArray("transactions") ?: JSONArray()
        val chart = data.optJSONArray("chart") ?: JSONArray()
        val currency = data.optString("currency", "₹")
        fun money(v: Double) = currency + String.format(Locale.US, "%,.2f", v)
        fun esc(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")
        fun p(text: String, style: String = "Normal"): String =
            "<w:p><w:pPr><w:pStyle w:val=\"$style\"/></w:pPr><w:r><w:t xml:space=\"preserve\">${esc(text)}</w:t></w:r></w:p>"
        fun table(headers: List<String>, rows: List<List<String>>): String {
            val all = listOf(headers) + rows
            val borders = "<w:tblBorders><w:top w:val=\"single\" w:sz=\"5\" w:color=\"D5E0E5\"/><w:left w:val=\"single\" w:sz=\"5\" w:color=\"D5E0E5\"/><w:bottom w:val=\"single\" w:sz=\"5\" w:color=\"D5E0E5\"/><w:right w:val=\"single\" w:sz=\"5\" w:color=\"D5E0E5\"/><w:insideH w:val=\"single\" w:sz=\"4\" w:color=\"E4EAEE\"/><w:insideV w:val=\"single\" w:sz=\"4\" w:color=\"E4EAEE\"/></w:tblBorders>"
            val xmlRows = all.joinToString("") { row ->
                "<w:tr>" + row.joinToString("") { cell ->
                    "<w:tc><w:tcPr><w:tcW w:w=\"2200\" w:type=\"dxa\"/></w:tcPr><w:p><w:r><w:t xml:space=\"preserve\">${esc(cell)}</w:t></w:r></w:p></w:tc>"
                } + "</w:tr>"
            }
            return "<w:tbl><w:tblPr><w:tblW w:w=\"0\" w:type=\"auto\"/>$borders</w:tblPr>$xmlRows</w:tbl>"
        }
        val body = StringBuilder()
        body.append(p("FinTrack", "Subtitle"))
        body.append(p(data.optString("title", "Transaction Statement"), "Title"))
        body.append(p("Period: ${data.optString("startDate")} to ${data.optString("endDate")}"))
        body.append(p("Generated: ${data.optString("generatedAt")}"))
        body.append(p("Financial Summary", "Heading1"))
        body.append(table(listOf("Metric", "Value"), listOf(
            listOf("Income", money(summary.optDouble("income"))),
            listOf("Expenses", money(summary.optDouble("expense"))),
            listOf("Net savings", money(summary.optDouble("netSavings"))),
            listOf("Current account balance", money(summary.optDouble("currentBalance"))),
            listOf("Transactions", summary.optInt("transactionCount").toString())
        )))
        body.append(p("Charts and Trends", "Heading1"))
        val chartRows = (0 until chart.length()).map { chart.getJSONObject(it) }.takeLast(12).map {
            listOf(it.optString("month"), "Income " + money(it.optDouble("income")), "Expense " + money(it.optDouble("expense")))
        }
        body.append(table(listOf("Month", "Income", "Expense"), chartRows))
        body.append(p("Category Breakdown", "Heading1"))
        val catRows = (0 until categories.length()).map { categories.getJSONObject(it) }.map {
            val bar = "■".repeat((it.optDouble("expense") / maxOf(1.0, categories.optJSONObject(0)?.optDouble("expense") ?: 1.0) * 16).toInt().coerceIn(0, 16))
            listOf(it.optString("name"), money(it.optDouble("income")), money(it.optDouble("expense")), bar, it.optInt("count").toString())
        }
        body.append(table(listOf("Category", "Income", "Expense", "Chart", "Count"), catRows))
        body.append(p("Account Breakdown", "Heading1"))
        val accountRows = (0 until accounts.length()).map { accounts.getJSONObject(it) }.map {
            listOf(it.optString("name"), it.optString("type"), money(it.optDouble("balance")), money(it.optDouble("income")), money(it.optDouble("expense")),
                money(it.optDouble("transferIn") + it.optDouble("transferOut")), money(it.optDouble("netFlow")))
        }
        body.append(table(listOf("Account", "Type", "Balance", "Income", "Expense", "Transfers", "Net flow"), accountRows))
        body.append(p("Complete Transaction Ledger", "Heading1"))
        val txnRows = (0 until transactions.length()).map { transactions.getJSONObject(it) }.map {
            listOf(it.optString("date"), it.optString("type"), it.optString("account"), it.optString("category"),
                money(it.optDouble("amount")), listOf(it.optString("toAccount"), it.optString("notes"), it.optString("person")).filter { s -> s.isNotBlank() }.joinToString(" · "))
        }
        body.append(table(listOf("Date", "Type", "Account", "Category", "Amount", "Notes / Person"), txnRows))
        body.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"720\" w:right=\"720\" w:bottom=\"720\" w:left=\"720\"/></w:sectPr>")

        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$body</w:body></w:document>"""
        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:rFonts w:ascii="Aptos" w:hAnsi="Aptos"/><w:sz w:val="20"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:rPr><w:b/><w:color w:val="009F82"/><w:sz w:val="34"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Subtitle"><w:name w:val="Subtitle"/><w:rPr><w:color w:val="009F82"/><w:sz w:val="24"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:rPr><w:b/><w:color w:val="009F82"/><w:sz w:val="26"/></w:rPr></w:style>
</w:styles>"""
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""
        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
        val docRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, value: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("[Content_Types].xml", contentTypes)
            entry("_rels/.rels", rels)
            entry("word/document.xml", documentXml)
            entry("word/styles.xml", stylesXml)
            entry("word/_rels/document.xml.rels", docRels)
        }
        return output.toByteArray()
    }
}
