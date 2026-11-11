package com.charan.tracmio.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxExporter {

    private data class SheetSpec(
        val name: String,
        val headers: List<String>,
        val rows: List<List<Any?>>
    )

    fun create(json: String): ByteArray {
        val root = runCatching { JSONObject(json) }.getOrDefault(JSONObject())
        val sheets = buildSheets(root)
        return buildWorkbookBytes(sheets)
    }

    private fun buildSheets(root: JSONObject): List<SheetSpec> {
        val accounts = root.optJSONArray("accounts") ?: JSONArray()
        val transactions = root.optJSONArray("transactions") ?: JSONArray()
        val budgets = root.optJSONArray("budgets") ?: JSONArray()
        val goals = root.optJSONArray("goals") ?: JSONArray()
        val loans = root.optJSONArray("loans") ?: JSONArray()
        val persons = root.optJSONArray("persons") ?: JSONArray()

        val accountNames = mutableMapOf<String, String>()
        for (i in 0 until accounts.length()) {
            val acc = accounts.optJSONObject(i) ?: continue
            accountNames[acc.opt("id")?.toString().orEmpty()] = acc.optString("name", "Account")
        }

        val personNames = mutableMapOf<String, String>()
        for (i in 0 until persons.length()) {
            val p = persons.optJSONObject(i) ?: continue
            personNames[p.opt("id")?.toString().orEmpty()] = p.optString("name", "")
        }

        var totalIncome = 0.0
        var totalExpense = 0.0
        val txRows = mutableListOf<List<Any?>>()
        for (i in 0 until transactions.length()) {
            val tx = transactions.optJSONObject(i) ?: continue
            val type = tx.optString("type", "expense").lowercase()
            val amount = tx.optDouble("amount", 0.0)
            if (type == "income" || type == "borrow") totalIncome += amount
            if (type == "expense" || type == "lend") totalExpense += amount
            val accId = tx.opt("accountId")?.toString().orEmpty()
            val accName = tx.optString("accountName").ifBlank { accountNames[accId] ?: accId }
            val personId = tx.opt("personId")?.toString().orEmpty()
            val personName = tx.optString("personName").ifBlank { personNames[personId] ?: "" }
            txRows += listOf(
                tx.optString("date", ""),
                tx.optString("time", ""),
                type.uppercase(),
                tx.optString("category", ""),
                tx.optString("subcategory", ""),
                accName,
                tx.optString("paymentMethod", tx.optString("method", "")),
                tx.optString("upiApp", ""),
                personName,
                amount,
                tx.optString("note", tx.optString("notes", ""))
            )
        }

        val accRows = mutableListOf<List<Any?>>()
        for (i in 0 until accounts.length()) {
            val acc = accounts.optJSONObject(i) ?: continue
            accRows += listOf(
                acc.optString("name", ""),
                acc.optString("type", acc.optString("accountType", "")),
                acc.optDouble("balance", 0.0),
                acc.optString("currency", "INR"),
                acc.optString("institution", "")
            )
        }

        val budgetRows = mutableListOf<List<Any?>>()
        for (i in 0 until budgets.length()) {
            val b = budgets.optJSONObject(i) ?: continue
            budgetRows += listOf(
                b.optString("category", ""),
                b.optDouble("limit", b.optDouble("limitAmount", 0.0)),
                b.optString("month", "")
            )
        }

        val goalRows = mutableListOf<List<Any?>>()
        for (i in 0 until goals.length()) {
            val g = goals.optJSONObject(i) ?: continue
            goalRows += listOf(
                g.optString("name", ""),
                g.optDouble("target", g.optDouble("targetAmount", 0.0)),
                g.optDouble("saved", g.optDouble("savedAmount", 0.0)),
                g.optString("deadline", "")
            )
        }

        val loanRows = mutableListOf<List<Any?>>()
        for (i in 0 until loans.length()) {
            val l = loans.optJSONObject(i) ?: continue
            loanRows += listOf(
                l.optString("name", ""),
                l.optDouble("principal", 0.0),
                l.optDouble("paid", l.optDouble("paidAmount", 0.0)),
                l.optDouble("emi", l.optDouble("monthlyPayment", 0.0)),
                l.optDouble("rate", l.optDouble("interestRate", 0.0))
            )
        }

        val summaryRows = listOf(
            listOf("Application", "Tracmio — Finance & Expenses Tracker (v2.5.2)"),
            listOf("Total Transactions", transactions.length()),
            listOf("Total Accounts", accounts.length()),
            listOf("Total Inflow (Income + Borrow)", totalIncome),
            listOf("Total Outflow (Expense + Lend)", totalExpense),
            listOf("Net Cash Flow", totalIncome - totalExpense)
        )

        return listOf(
            SheetSpec("Summary", listOf("Metric", "Value"), summaryRows),
            SheetSpec(
                "Transactions",
                listOf("Date", "Time", "Type", "Category", "Subcategory", "Account", "Payment Method", "UPI App", "Person", "Amount", "Notes"),
                txRows
            ),
            SheetSpec("Accounts", listOf("Name", "Type", "Balance", "Currency", "Institution"), accRows),
            SheetSpec("Budgets", listOf("Category", "Limit", "Month"), budgetRows),
            SheetSpec("Goals", listOf("Goal Name", "Target Amount", "Saved Amount", "Deadline"), goalRows),
            SheetSpec("Loans", listOf("Loan Name", "Principal", "Paid Amount", "Monthly EMI", "Interest Rate (%)"), loanRows)
        )
    }

    private fun buildWorkbookBytes(sheets: List<SheetSpec>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            writeEntry(zip, "[Content_Types].xml", contentTypesXml(sheets.size))
            writeEntry(zip, "_rels/.rels", rootRelsXml())
            writeEntry(zip, "xl/workbook.xml", workbookXml(sheets))
            writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml(sheets.size))
            writeEntry(zip, "xl/styles.xml", stylesXml())
            sheets.forEachIndexed { idx, sheet ->
                writeEntry(zip, "xl/worksheets/sheet${idx + 1}.xml", worksheetXml(sheet))
            }
        }
        return out.toByteArray()
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        val entry = ZipEntry(name)
        zip.putNextEntry(entry)
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypesXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("""</Types>""")
    }

    private fun rootRelsXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            """</Relationships>"""

    private fun workbookXml(sheets: List<SheetSpec>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("""<sheets>""")
        sheets.forEachIndexed { idx, sheet ->
            val sheetId = idx + 1
            append("""<sheet name="${escapeXml(sheet.name)}" sheetId="$sheetId" r:id="rId$sheetId"/>""")
        }
        append("""</sheets>""")
        append("""</workbook>""")
    }

    private fun workbookRelsXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("""</Relationships>""")
    }

    private fun stylesXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
            """<fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>""" +
            """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>""" +
            """</styleSheet>"""

    private fun worksheetXml(sheet: SheetSpec): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("""<sheetData>""")
        appendRow(1, sheet.headers, isHeader = true)
        sheet.rows.forEachIndexed { idx, row ->
            appendRow(idx + 2, row, isHeader = false)
        }
        append("""</sheetData>""")
        append("""</worksheet>""")
    }

    private fun StringBuilder.appendRow(rowNumber: Int, values: List<Any?>, isHeader: Boolean) {
        append("""<row r="$rowNumber">""")
        values.forEachIndexed { colIdx, raw ->
            val ref = columnName(colIdx) + rowNumber
            val styleAttr = if (isHeader) """ s="1"""" else ""
            when (raw) {
                is Number -> {
                    val d = raw.toDouble()
                    if (d.isFinite()) {
                        append("""<c r="$ref"$styleAttr><v>$d</v></c>""")
                    } else {
                        append("""<c r="$ref" t="inlineStr"$styleAttr><is><t></t></is></c>""")
                    }
                }
                else -> {
                    val text = escapeXml(raw?.toString().orEmpty())
                    append("""<c r="$ref" t="inlineStr"$styleAttr><is><t>$text</t></is></c>""")
                }
            }
        }
        append("""</row>""")
    }

    private fun columnName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, ('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.toString()
    }

    private fun escapeXml(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (ch.code >= 0x20 || ch == '\n' || ch == '\r' || ch == '\t') append(ch)
            }
        }
    }
}
