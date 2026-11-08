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
    fun exportCreatesWorkbookWithFinanceSheetsAndValues() {
        val state = JSONObject()
            .put("accounts", JSONArray().put(JSONObject().put("name", "Daily Cash").put("balance", 1250.0)))
            .put("transactions", JSONArray().put(JSONObject().put("type", "expense").put("amount", 150.0).put("category", "Food")))
            .put("persons", JSONArray())
            .put("budgets", JSONArray().put(JSONObject().put("name", "Food").put("amount", 4000.0).put("spent", 150.0)))
            .put("goals", JSONArray())
            .put("loans", JSONArray())
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
        assertTrue(entries["xl/workbook.xml"].orEmpty().contains("Transactions"))
        assertTrue(entries["xl/worksheets/sheet1.xml"].orEmpty().contains("Daily Cash"))
        assertTrue(entries["xl/worksheets/sheet2.xml"].orEmpty().contains("expense"))
        assertTrue(entries["xl/worksheets/sheet4.xml"].orEmpty().contains("4000.0"))
    }
}
