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
                    put("stableUid", tx.stableUid)
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
                    put("transferAccountId", tx.transferAccountId ?: JSONObject.NULL)
                    put("transferId", tx.transferId ?: JSONObject.NULL)
                    put("personId", tx.personId ?: JSONObject.NULL)
                    put("timestamp", tx.timestamp)
                }
            )
        }
        root.put("transactions", txArr)

        val budgetsArr = JSONArray()
        state.budgets.forEach { b ->
            budgetsArr.put(JSONObject().apply {
                put("id", b.id)
                put("category", b.category)
                put("monthlyLimit", if (b.monthlyLimit > 0.0) b.monthlyLimit else MoneyPaise.toRupees(b.limitPaise))
                put("month", b.month)
            })
        }
        root.put("budgets", budgetsArr)

        val goalsArr = JSONArray()
        state.goals.forEach { g ->
            goalsArr.put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title.ifBlank { g.name })
                put("targetAmount", if (g.targetAmount > 0.0) g.targetAmount else MoneyPaise.toRupees(g.targetPaise))
                put("currentAmount", if (g.currentAmount > 0.0) g.currentAmount else MoneyPaise.toRupees(g.currentPaise))
                put("deadline", g.deadline)
                put("icon", g.icon)
                put("colorHex", g.colorHex)
            })
        }
        root.put("goals", goalsArr)

        val goalContribArr = JSONArray()
        state.goalContributions.forEach { c ->
            goalContribArr.put(JSONObject().apply {
                put("goalId", c.goalId)
                put("amount", c.amount)
                put("note", c.note)
                put("timestamp", c.timestamp)
            })
        }
        root.put("goalContributions", goalContribArr)

        val loansArr = JSONArray()
        state.loans.forEach { l ->
            loansArr.put(JSONObject().apply {
                put("id", l.id)
                put("name", l.name.ifBlank { l.lender })
                put("principal", if (l.principal > 0.0) l.principal else MoneyPaise.toRupees(l.principalPaise))
                put("interestRate", l.interestRate)
                put("tenureMonths", l.tenureMonths)
                put("monthlyPayment", if (l.monthlyPayment > 0.0) l.monthlyPayment else MoneyPaise.toRupees(l.emiPaise))
                put("isClosed", l.isClosed)
            })
        }
        root.put("loans", loansArr)

        val loanPayArr = JSONArray()
        state.loanPayments.forEach { lp ->
            loanPayArr.put(JSONObject().apply {
                put("loanId", lp.loanId)
                put("amount", if (lp.amount > 0.0) lp.amount else MoneyPaise.toRupees(lp.amountPaise))
                put("note", lp.note)
                put("timestamp", lp.timestamp)
            })
        }
        root.put("loanPayments", loanPayArr)

        val personsArr = JSONArray()
        state.persons.forEach { p ->
            personsArr.put(JSONObject().apply {
                put("name", p.name)
                put("phone", p.phone)
                put("upiId", p.upiId)
                put("netBalance", p.netBalance)
                put("notes", p.notes)
            })
        }
        root.put("persons", personsArr)

        val folderArr = JSONArray()
        state.trackerFolders.forEach { f ->
            folderArr.put(JSONObject().apply {
                put("id", f.id)
                put("folderName", f.folderName)
                put("icon", f.icon)
                put("colorHex", f.colorHex)
                put("sortOrder", f.sortOrder)
            })
        }
        root.put("trackerFolders", folderArr)

        val trackersArr = JSONArray()
        state.customTrackers.forEach { t ->
            trackersArr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("trackerType", t.trackerType)
                put("targetValue", t.targetValue)
                put("currentValue", t.currentValue)
                put("unit", t.unit)
                put("icon", t.icon)
                put("colorHex", t.colorHex)
                put("folderId", t.folderId)
                put("isPinned", t.isPinned)
            })
        }
        root.put("customTrackers", trackersArr)

        val trackerEntriesArr = JSONArray()
        state.trackerEntries.forEach { e ->
            trackerEntriesArr.put(JSONObject().apply {
                put("trackerId", e.trackerId)
                put("deltaValue", e.deltaValue)
                put("note", e.note)
                put("timestamp", e.timestamp)
            })
        }
        root.put("trackerEntries", trackerEntriesArr)

        val subsArr = JSONArray()
        state.subscriptions.forEach { s ->
            subsArr.put(JSONObject().apply {
                put("name", s.name)
                put("amount", s.amount)
                put("billingCycle", s.billingCycle)
                put("nextDueDateMillis", s.nextDueDateMillis)
                put("category", s.category)
                put("isSimOrWifi", s.isSimOrWifi)
                put("validityDays", s.validityDays)
                put("isActive", s.isActive)
            })
        }
        root.put("subscriptions", subsArr)

        val acadArr = JSONArray()
        state.academicEvents.forEach { ev ->
            acadArr.put(JSONObject().apply {
                put("title", ev.title)
                put("feeAmount", ev.feeAmount)
                put("paidAmount", ev.paidAmount)
                put("dueDate", ev.dueDate)
                put("semester", ev.semester)
                put("isPaid", ev.isPaid)
            })
        }
        root.put("academicEvents", acadArr)

        val remArr = JSONArray()
        state.reminders.forEach { r ->
            remArr.put(JSONObject().apply {
                put("title", r.title)
                put("amount", r.amount)
                put("dueDate", r.dueDate)
                put("timeHour", r.timeHour)
                put("timeMinute", r.timeMinute)
                put("isCompleted", r.isCompleted)
                put("isRecurring", r.isRecurring)
            })
        }
        root.put("reminders", remArr)

        val khataArr = JSONArray()
        state.khataEntries.forEach { k ->
            khataArr.put(JSONObject().apply {
                put("shopOrFriend", k.shopOrFriend)
                put("itemDescription", k.itemDescription)
                put("amount", k.amount)
                put("isSettled", k.isSettled)
                put("timestamp", k.timestamp)
            })
        }
        root.put("khataEntries", khataArr)

        val wishlistArr = JSONArray()
        state.wishlistCooldowns.forEach { w ->
            wishlistArr.put(JSONObject().apply {
                put("itemName", w.itemName)
                put("price", w.price)
                put("createdAtMillis", w.createdAtMillis)
                put("status", w.status)
            })
        }
        root.put("wishlistCooldowns", wishlistArr)

        val cashbackArr = JSONArray()
        state.cashbackRewards.forEach { c ->
            cashbackArr.put(JSONObject().apply {
                put("sourceApp", c.sourceApp)
                put("amount", c.amount)
                put("note", c.note)
                put("timestamp", c.timestamp)
            })
        }
        root.put("cashbackRewards", cashbackArr)

        val ppuArr = JSONArray()
        state.pricePerUseItems.forEach { p ->
            ppuArr.put(JSONObject().apply {
                put("itemName", p.itemName)
                put("purchasePrice", p.purchasePrice)
                put("usageCount", p.usageCount)
                put("targetUses", p.targetUses)
                put("createdAtMillis", p.createdAtMillis)
            })
        }
        root.put("pricePerUseItems", ppuArr)

        val chipsArr = JSONArray()
        state.quickSpendChips.forEach { c ->
            chipsArr.put(JSONObject().apply {
                put("id", c.id)
                put("emoji", c.emoji)
                put("title", c.title)
                put("amount", c.amount)
                put("category", c.category)
            })
        }
        root.put("quickSpendChips", chipsArr)

        val refundsArr = JSONArray()
        state.pendingRefunds.forEach { r ->
            refundsArr.put(JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("source", r.source)
                put("amount", r.amount)
                put("expectedDate", r.expectedDate)
                put("isReceived", r.isReceived)
            })
        }
        root.put("pendingRefunds", refundsArr)

        val pinnedArr = JSONArray()
        state.pinnedToolIds.forEach { pinnedArr.put(it) }
        root.put("pinnedToolIds", pinnedArr)

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
