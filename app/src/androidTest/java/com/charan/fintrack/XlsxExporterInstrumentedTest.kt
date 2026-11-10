package com.charan.fintrack

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class XlsxExporterInstrumentedTest {
    @Test
    fun exportCreatesFormattedWorkbookWithSummaryAndFinanceSheets() {
        val state = JSONObject()
            .put("accounts", JSONArray().put(JSONObject().put("name", "Daily Cash").put("balance", 1250.0)))
            .put("transactions", JSONArray().put(JSONObject().put("type", "expense").put("amount", 150.0).put("category", "Food")))
            .put("persons", JSONArray())
            .put("budgets", JSONArray().put(JSONObject().put("name", "Food").put("amount", 4000.0).put("spent", 150.0)))
            .put("goals", JSONArray())
            .put("loans", JSONArray().put(
                JSONObject().put("name", "Education Loan").put("principal", 12000.0).put("paid", 1000.0)
                    .put("repayments", JSONArray().put(JSONObject().put("amount", 1000.0).put("date", "2026-11-08")))
            ))
            .put("categories", JSONArray())

        val output = XlsxExporter.create(state.toString())
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }

        assertNotNull("Missing XLSX content types", entries["[Content_Types].xml"])
        assertTrue("Missing 'Summary' in xl/workbook.xml: "+entries["xl/workbook.xml"].orEmpty(), entries["xl/workbook.xml"].orEmpty().contains("Summary"))
        assertTrue("Missing 'Transactions' in xl/workbook.xml: "+entries["xl/workbook.xml"].orEmpty(), entries["xl/workbook.xml"].orEmpty().contains("Transactions"))
        assertTrue("Missing 'FinTrack Export Summary' in xl/worksheets/sheet1.xml: "+entries["xl/worksheets/sheet1.xml"].orEmpty(), entries["xl/worksheets/sheet1.xml"].orEmpty().contains("FinTrack Export Summary"))
        assertTrue("Missing '150.0' in xl/worksheets/sheet1.xml: "+entries["xl/worksheets/sheet1.xml"].orEmpty(), entries["xl/worksheets/sheet1.xml"].orEmpty().contains("150.00"))
        assertTrue("Missing 'Generated at' in xl/worksheets/sheet1.xml: "+entries["xl/worksheets/sheet1.xml"].orEmpty(), entries["xl/worksheets/sheet1.xml"].orEmpty().contains("Generated at"))
        assertTrue("Missing 'Income INR' in xl/worksheets/sheet1.xml: "+entries["xl/worksheets/sheet1.xml"].orEmpty(), entries["xl/worksheets/sheet1.xml"].orEmpty().contains("Income INR"))
        assertTrue("Missing 'Daily Cash' in xl/worksheets/sheet2.xml: "+entries["xl/worksheets/sheet2.xml"].orEmpty(), entries["xl/worksheets/sheet2.xml"].orEmpty().contains("Daily Cash"))
        assertTrue("Missing 'expense' in xl/worksheets/sheet3.xml: "+entries["xl/worksheets/sheet3.xml"].orEmpty(), entries["xl/worksheets/sheet3.xml"].orEmpty().contains("expense"))
        assertTrue(
            "Missing numeric budget limit 4000 in xl/worksheets/sheet5.xml: " + entries["xl/worksheets/sheet5.xml"].orEmpty(),
            Regex("""<c r="B2"[^>]*><v>4000(?:\.0+)?</v></c>""").containsMatchIn(entries["xl/worksheets/sheet5.xml"].orEmpty())
        )
        assertTrue("Missing 'Education Loan' in xl/worksheets/sheet7.xml: "+entries["xl/worksheets/sheet7.xml"].orEmpty(), entries["xl/worksheets/sheet7.xml"].orEmpty().contains("Education Loan"))
        assertTrue("Missing '2026-11-08' in xl/worksheets/sheet8.xml: "+entries["xl/worksheets/sheet8.xml"].orEmpty(), entries["xl/worksheets/sheet8.xml"].orEmpty().contains("2026-11-08"))
        assertTrue(entries["xl/worksheets/sheet3.xml"].orEmpty().contains("state=\"frozen\""))
        assertTrue("Missing '<autoFilter' in xl/worksheets/sheet3.xml: "+entries["xl/worksheets/sheet3.xml"].orEmpty(), entries["xl/worksheets/sheet3.xml"].orEmpty().contains("<autoFilter"))

        val unsafeState = JSONObject()
            .put("accounts", JSONArray())
            .put("transactions", JSONArray().put(
                JSONObject().put("type", "expense").put("amount", 10.0)
                    .put("notes", "line\u0001 break & <tag>")
            ))
        val sanitized = XlsxExporter.create(unsafeState.toString())
        ZipInputStream(ByteArrayInputStream(sanitized)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val xml = zip.readBytes().toString(Charsets.UTF_8)
                assertTrue("Invalid XML control character leaked into "+entry.name, !xml.contains('\u0001'))
                zip.closeEntry()
            }
        }
    }

    @Test
    fun summaryTotalsUseExactPaiseInsteadOfFloatingPointAddition() {
        val state = JSONObject()
            .put("accounts", JSONArray())
            .put("transactions", JSONArray()
                .put(JSONObject().put("type", "income").put("amount", 0.10).put("amountPaise", 10))
                .put(JSONObject().put("type", "income").put("amount", 0.20).put("amountPaise", 20))
                .put(JSONObject().put("type", "expense").put("amount", 0.01).put("amountPaise", 1))
            )
        val output = XlsxExporter.create(state.toString())
        var summary = ""
        ZipInputStream(ByteArrayInputStream(output)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "xl/worksheets/sheet1.xml") {
                    summary = zip.readBytes().toString(Charsets.UTF_8)
                    break
                }
                zip.closeEntry()
            }
        }
        assertTrue("Income total should be exactly ₹0.30: $summary", summary.contains("<v>0.30</v>"))
        assertTrue("Expense total should be exactly ₹0.01: $summary", summary.contains("<v>0.01</v>"))
        assertTrue("Net savings should be exactly ₹0.29: $summary", summary.contains("<v>0.29</v>"))
    }

    @Test
    fun transferPairIsExportedAsOneLogicalTransaction() {
        val state = JSONObject()
            .put("accounts", JSONArray())
            .put("transactions", JSONArray()
                .put(JSONObject().put("type", "TRANSFER_OUT").put("amount", 10.0)
                    .put("accountId", 1).put("transferAccountId", 2).put("toAccountId", 2))
                .put(JSONObject().put("type", "TRANSFER_IN").put("amount", 10.0)
                    .put("accountId", 2).put("transferAccountId", 1).put("toAccountId", 1))
            )
        val output = XlsxExporter.create(state.toString())
        var transactionSheet = ""
        ZipInputStream(ByteArrayInputStream(output)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "xl/worksheets/sheet3.xml") {
                    transactionSheet = zip.readBytes().toString(Charsets.UTF_8)
                    break
                }
                zip.closeEntry()
            }
        }
        assertTrue(transactionSheet.contains("TRANSFER"))
        assertTrue(!transactionSheet.contains("TRANSFER_IN"))
    }
}
