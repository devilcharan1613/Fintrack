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

        assertNotNull(entries["[Content_Types].xml"])
        assertTrue(entries["xl/workbook.xml"].orEmpty().contains("Summary"))
        assertTrue(entries["xl/workbook.xml"].orEmpty().contains("Transactions"))
        assertTrue(entries["xl/worksheets/sheet1.xml"].orEmpty().contains("FinTrack Export Summary"))
        assertTrue(entries["xl/worksheets/sheet1.xml"].orEmpty().contains("150.0"))
        assertTrue(entries["xl/worksheets/sheet1.xml"].orEmpty().contains("Generated at"))
        assertTrue(entries["xl/worksheets/sheet1.xml"].orEmpty().contains("Income INR"))
        assertTrue(entries["xl/worksheets/sheet2.xml"].orEmpty().contains("Daily Cash"))
        assertTrue(entries["xl/worksheets/sheet3.xml"].orEmpty().contains("expense"))
        assertTrue(entries["xl/worksheets/sheet5.xml"].orEmpty().contains("4000.0"))
        assertTrue(entries["xl/worksheets/sheet7.xml"].orEmpty().contains("Education Loan"))
        assertTrue(entries["xl/worksheets/sheet8.xml"].orEmpty().contains("2026-11-08"))
        assertTrue(entries["xl/worksheets/sheet3.xml"].orEmpty().contains("state=\"frozen\""))
        assertTrue(entries["xl/worksheets/sheet3.xml"].orEmpty().contains("<autoFilter"))

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
}
