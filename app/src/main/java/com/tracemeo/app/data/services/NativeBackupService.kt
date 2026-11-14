package com.tracemeo.app.data.services

import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.NativeTracmioUiState
import com.tracemeo.app.data.security.TracmioSecurityEngine
import com.tracemeo.app.ui.nativeapp.LiquidGlassSettings
import org.json.JSONArray
import org.json.JSONObject

object NativeBackupService {
    fun buildFullBackupJson(
        state: NativeTracmioUiState,
        settings: LiquidGlassSettings
    ): String {
        val root = JSONObject()
        root.put("app", "Tracmio")
        root.put("versionName", "16.13")
        root.put("versionCode", 30)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val prefsObj = JSONObject().apply {
            put("userName", settings.userName)
            put("currencySymbol", settings.currencySymbol)
            put("themePreset", settings.themePreset)
            put("glassUiStyle", settings.glassUiStyle)
            put("wallpaperPreset", settings.wallpaperPreset)
            put("backgroundVisibility", settings.backgroundVisibility.toDouble())
            put("glowIntensity", settings.glowIntensity.toDouble())
            put("uiScale", settings.uiScale.toDouble())
            put("roundUpEnabled", settings.roundUpEnabled)
        }
        root.put("appPreferences", prefsObj)

        val accountsArr = JSONArray()
        state.accounts.forEach { acc ->
            accountsArr.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("accountType", acc.accountType)
                    put("balance", if (acc.balance != 0.0) acc.balance else MoneyPaise.toRupees(acc.balancePaise))
                    put("creditLimit", acc.creditLimit)
                    put("statementDay", acc.statementDay)
                    put("dueDay", acc.dueDay)
                    put("allocationsJson", acc.allocationsJson)
                    put("allocationsCount", NativeLedgerCalculator.parseAllocations(acc.allocationsJson).size)
                }
            )
        }
        root.put("accounts", accountsArr)

        val txArr = JSONArray()
        state.transactions.forEach { tx ->
            txArr.put(
                JSONObject().apply {
                    put("id", tx.id)
                    put("accountId", tx.accountId)
                    put("type", tx.type)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("subcategory", tx.subcategory)
                    put("paymentMethod", tx.paymentMethod)
                    put("upiApp", tx.upiApp)
                    put("note", tx.note)
                    put("hashtags", tx.hashtags)
                    put("warrantyDate", tx.warrantyDate)
                    put("allocationId", tx.allocationId)
                    put("timestamp", tx.timestamp)
                }
            )
        }
        root.put("transactions", txArr)

        val budgetsArr = JSONArray()
        state.budgets.forEach { b ->
            budgetsArr.put(JSONObject().apply {
                put("category", b.category)
                put("monthlyLimit", if (b.monthlyLimit > 0.0) b.monthlyLimit else MoneyPaise.toRupees(b.limitPaise))
            })
        }
        root.put("budgets", budgetsArr)

        val goalsArr = JSONArray()
        state.goals.forEach { g ->
            goalsArr.put(JSONObject().apply {
                put("title", g.title.ifBlank { g.name })
                put("targetAmount", if (g.targetAmount > 0.0) g.targetAmount else MoneyPaise.toRupees(g.targetPaise))
                put("currentAmount", if (g.currentAmount > 0.0) g.currentAmount else MoneyPaise.toRupees(g.currentPaise))
                put("deadline", g.deadline)
            })
        }
        root.put("goals", goalsArr)

        val loansArr = JSONArray()
        state.loans.forEach { l ->
            loansArr.put(JSONObject().apply {
                put("name", l.name.ifBlank { l.lender })
                put("principal", if (l.principal > 0.0) l.principal else MoneyPaise.toRupees(l.principalPaise))
                put("interestRate", l.interestRate)
                put("monthlyPayment", if (l.monthlyPayment > 0.0) l.monthlyPayment else MoneyPaise.toRupees(l.emiPaise))
            })
        }
        root.put("loans", loansArr)

        val trackersArr = JSONArray()
        state.customTrackers.forEach { t ->
            trackersArr.put(JSONObject().apply {
                put("title", t.title)
                put("trackerType", t.trackerType)
                put("targetValue", t.targetValue)
                put("currentValue", t.currentValue)
            })
        }
        root.put("customTrackers", trackersArr)

        return root.toString(2)
    }

    fun exportEncryptedBackupJson(
        state: NativeTracmioUiState,
        settings: LiquidGlassSettings,
        password: CharArray
    ): String {
        val plain = buildFullBackupJson(state, settings)
        return TracmioSecurityEngine.encryptBackup(plain, password)
    }
}
