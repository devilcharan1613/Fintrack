package com.charan.fintrack

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class TransactionReportExporterTest {
    private fun sampleReport() = JSONObject(
        """{
          "title":"FinTrack Transaction Statement",
          "generatedAt":"07 Nov 2026, 10:00",
          "startDate":"2026-11-01",
          "endDate":"2026-11-07",
          "currency":"₹",
          "summary":{"income":12000,"expense":3500,"netSavings":8500,"currentBalance":24500,"transactionCount":2},
          "chart":[{"month":"2026-11","income":12000,"expense":3500}],
          "accounts":[{"name":"Main Bank","type":"bank","balance":24500,"income":12000,"expense":3500,"transferIn":0,"transferOut":0,"netFlow":8500}],
          "categories":[{"name":"Food","income":0,"expense":3500,"count":1}],
          "transactions":[
            {"date":"2026-11-07","type":"income","amount":12000,"account":"Main Bank","category":"Salary","notes":"Monthly pay"},
            {"date":"2026-11-06","type":"expense","amount":3500,"account":"Main Bank","category":"Food","notes":"Groceries"}
          ]
        }"""
    )

    @Test
    fun pdfExportProducesPdfDocument() {
        val bytes = TransactionReportExporter.createPdf(sampleReport())
        assertTrue(bytes.size > 500)
        assertEquals("%PDF-", String(bytes.copyOfRange(0, 5), Charsets.US_ASCII))
    }

    @Test
    fun docxExportContainsRequiredOfficePartsAndReportSections() {
        val bytes = TransactionReportExporter.createDocx(sampleReport())
        assertTrue(bytes.size > 500)
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.endsWith(".xml") || entry.name.endsWith(".rels")) {
                    entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
            }
        }
        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("word/document.xml"))
        assertTrue(entries.containsKey("word/_rels/document.xml.rels"))
        assertTrue(entries["word/_rels/document.xml.rels"]?.contains("relationships/styles") == true)
        val document = entries["word/document.xml"] ?: ""
        listOf("Financial Summary", "Charts and Trends", "Category Breakdown",
            "Account Breakdown", "Complete Transaction Ledger", "Main Bank", "Groceries")
            .forEach { expected -> assertTrue("Missing $expected", document.contains(expected)) }
    }
}
