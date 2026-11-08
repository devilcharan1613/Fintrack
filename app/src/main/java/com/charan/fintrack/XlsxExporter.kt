package com.charan.fintrack

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a small standards-compliant XLSX workbook without a network connection
 * or third-party runtime dependency. Values are emitted as inline strings or numbers.
 */
internal object XlsxExporter {
    private data class SheetData(val name: String, val rows: List<List<Any?>>)

    fun create(json: String): ByteArray {
        val state = JSONObject(json)
        val sheets = listOf(
            SheetData("Summary", buildSummaryRows(state)),
            SheetData("Accounts", buildRows(state.optJSONArray("accounts"), listOf(
                "Name" to "name", "Type" to "type", "Balance" to "balance",
                "Currency" to "currency", "Institution" to "institution",
                "Account Number" to "accountNumber", "Credit Limit" to "creditLimit",
                "Notes" to "notes", "Created At" to "createdAt"
            ))),
            SheetData("Transactions", buildRows(state.optJSONArray("transactions"), listOf(
                "Date" to "date", "Time" to "time", "Type" to "type",
                "Amount INR" to "amount", "Account ID" to "accountId",
                "Destination Account ID" to "toAccountId", "Category" to "category",
                "Subcategory" to "subcategory", "Person ID" to "personId",
                "Payment Method" to "paymentMethod", "UPI App" to "upiApp",
                "Notes" to "notes"
            ))),
            SheetData("People", buildRows(state.optJSONArray("persons"), listOf(
                "Name" to "name", "Phone" to "phone", "Notes" to "notes"
            ))),
            SheetData("Budgets", buildRows(state.optJSONArray("budgets"), listOf(
                "Name" to "name", "Limit INR" to "amount", "Period" to "period", "Spent INR" to "spent"
            ))),
            SheetData("Savings Goals", buildRows(state.optJSONArray("goals"), listOf(
                "Name" to "name", "Target INR" to "target", "Saved INR" to "saved", "Deadline" to "deadline"
            ))),
            SheetData("Loans", buildRows(state.optJSONArray("loans"), listOf(
                "Name" to "name", "Principal INR" to "principal", "Paid INR" to "paid", "EMI INR" to "emi"
            ))),
            SheetData("Loan Payments", buildRepaymentRows(state.optJSONArray("loans"))),
            SheetData("Categories", buildRows(state.optJSONArray("categories"), listOf(
                "Name" to "name", "Icon" to "icon", "ID" to "id"
            )))
        )
        return zipWorkbook(sheets)
    }

    private fun buildSummaryRows(state: JSONObject): List<List<Any?>> {
        val metadata = state.optJSONObject("exportMetadata")
        val transactions = state.optJSONArray("transactions") ?: JSONArray()
        var income = 0.0
        var expense = 0.0
        for (i in 0 until transactions.length()) {
            val txn = transactions.optJSONObject(i) ?: continue
            when (txn.optString("type")) {
                "income" -> income += txn.optDouble("amount", 0.0)
                "expense" -> expense += txn.optDouble("amount", 0.0)
            }
        }
        return listOf(
            listOf("FinTrack Export Summary", "Value"),
            listOf("Export type", metadata?.optString("type", "full") ?: "full"),
            listOf("From date", metadata?.optString("from", "All records") ?: "All records"),
            listOf("To date", metadata?.optString("to", "All records") ?: "All records"),
            listOf("Generated at", metadata?.optString("generatedAt", java.time.Instant.now().toString()) ?: java.time.Instant.now().toString()),
            listOf("Transactions included", transactions.length()),
            listOf("Income INR", income),
            listOf("Expense INR", expense),
            listOf("Net savings INR", income - expense),
            listOf("Accounts included", state.optJSONArray("accounts")?.length() ?: 0),
            listOf("People included", state.optJSONArray("persons")?.length() ?: 0)
        )
    }

    private fun buildRows(array: JSONArray?, columns: List<Pair<String, String>>): List<List<Any?>> {
        val rows = mutableListOf<List<Any?>>()
        rows += columns.map { it.first }
        if (array != null) {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                rows += columns.map { (_, key) ->
                    if (obj.isNull(key)) "" else obj.opt(key)
                }
            }
        }
        return rows
    }

    private fun buildRepaymentRows(loans: JSONArray?): List<List<Any?>> {
        val rows = mutableListOf<List<Any?>>()
        rows += listOf("Loan", "Amount INR", "Date", "Created At")
        if (loans != null) {
            for (loanIndex in 0 until loans.length()) {
                val loan = loans.optJSONObject(loanIndex) ?: continue
                val name = loan.optString("name", "Loan")
                val repayments = loan.optJSONArray("repayments") ?: continue
                for (repaymentIndex in 0 until repayments.length()) {
                    val repayment = repayments.optJSONObject(repaymentIndex) ?: continue
                    rows += listOf(
                        name,
                        if (repayment.isNull("amount")) "" else repayment.opt("amount"),
                        repayment.optString("date", ""),
                        repayment.optString("createdAt", "")
                    )
                }
            }
        }
        return rows
    }

    private fun zipWorkbook(sheets: List<SheetData>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            add(zip, "[Content_Types].xml", contentTypes(sheets.size))
            add(zip, "_rels/.rels", rootRelationships())
            add(zip, "xl/workbook.xml", workbook(sheets))
            add(zip, "xl/_rels/workbook.xml.rels", workbookRelationships(sheets.size))
            add(zip, "xl/styles.xml", styles())
            sheets.forEachIndexed { index, sheet ->
                add(zip, "xl/worksheets/sheet${index + 1}.xml", worksheet(sheet.rows))
            }
        }
        return out.toByteArray()
    }

    private fun add(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypes(count: Int): String {
        val overrides = (1..count).joinToString("") {
            """<Override PartName="/xl/worksheets/sheet$it.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$overrides
</Types>"""
    }

    private fun rootRelationships(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook(sheets: List<SheetData>): String {
        val entries = sheets.mapIndexed { index, sheet ->
            """<sheet name="${xml(sheet.name)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>$entries</sheets>
</workbook>"""
    }

    private fun workbookRelationships(count: Int): String {
        val sheets = (1..count).joinToString("") {
            """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$it.xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$sheets
<Relationship Id="rId${count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun styles(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/><name val="Aptos"/></font><font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Aptos Display"/></font></fonts>
<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF073746"/><bgColor indexed="64"/></patternFill></fill></fills>
<borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"><color rgb="FFD5E5EA"/></left><right style="thin"><color rgb="FFD5E5EA"/></right><top style="thin"><color rgb="FFD5E5EA"/></top><bottom style="thin"><color rgb="FFD5E5EA"/></bottom><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/><xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/></cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

    private fun worksheet(rows: List<List<Any?>>): String {
        val body = rows.mapIndexed { rowIndex, row ->
            val cells = row.mapIndexed { columnIndex, value ->
                cell(columnName(columnIndex + 1) + (rowIndex + 1), value, if (rowIndex == 0) 1 else 0)
            }.joinToString("")
            """<row r="${rowIndex + 1}">$cells</row>"""
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
<sheetData>$body</sheetData>
${if (rows.size > 1 && rows.firstOrNull()?.isNotEmpty() == true) "<autoFilter ref=\"A1:${columnName(rows.maxOfOrNull { it.size } ?: 1)}${rows.size}\"/>" else ""}
</worksheet>"""
    }

    private fun cell(reference: String, value: Any?, style: Int = 0): String {
        if (value == null || value == JSONObject.NULL) {
            return """<c r="$reference" s="$style" t="inlineStr"><is><t></t></is></c>"""
        }
        if (value is Number && value.toDouble().isFinite()) {
            return """<c r="$reference" s="$style"><v>$value</v></c>"""
        }
        if (value is Boolean) {
            return """<c r="$reference" s="$style" t="b"><v>${if (value) 1 else 0}</v></c>"""
        }
        return """<c r="$reference" s="$style" t="inlineStr"><is><t xml:space="preserve">${xml(value.toString())}</t></is></c>"""
    }

    private fun columnName(number: Int): String {
        var n = number
        val result = StringBuilder()
        while (n > 0) {
            val remainder = (n - 1) % 26
            result.append(('A'.code + remainder).toChar())
            n = (n - 1) / 26
        }
        return result.reverse().toString()
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
