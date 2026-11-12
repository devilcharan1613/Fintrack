package com.tracmio.app.data.repository

import androidx.room.withTransaction
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.TransactionDateTime
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.BudgetEntity
import com.tracmio.app.data.local.CategoryEntity
import com.tracmio.app.data.local.CustomTrackerEntity
import com.tracmio.app.data.local.GoalContributionEntity
import com.tracmio.app.data.local.GoalEntity
import com.tracmio.app.data.local.LoanEntity
import com.tracmio.app.data.local.LoanPaymentEntity
import com.tracmio.app.data.local.PersonEntity
import com.tracmio.app.data.local.TracmioDatabase
import com.tracmio.app.data.local.TrackerPersonEntity
import com.tracmio.app.data.local.TrackerSettlementEntity
import com.tracmio.app.data.local.TrackerTransactionCrossRefEntity
import com.tracmio.app.data.local.TransactionEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NativeBackupService(private val database: TracmioDatabase) {
    private val accounts = database.accountDao()
    private val finance = database.financeDao()
    private val trackersDao = database.customTrackerDao()

    suspend fun exportJson(): String {
        val accountRows = accounts.getAllAccountsIncludingArchived().first()
        val transactionRows = finance.transactions().first()
        val budgetRows = finance.budgets().first()
        val goalRows = finance.goals().first()
        val goalContribRows = finance.allGoalContributions().first()
        val loanRows = finance.loans().first()
        val loanPaymentRows = finance.allLoanPayments().first()
        val categoryRows = finance.categories().first()
        val personRows = finance.persons().first()
        val trackerRows = trackersDao.getAllTrackersSync()
        val allLinks = trackersDao.getAllTrackerLinks()

        val root = JSONObject()
        root.put("app", "Tracmio")
        root.put("version", 4)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("accounts", JSONArray().apply {
            accountRows.forEach { acc ->
                put(JSONObject().apply {
                    put("id", acc.id)
                    put("stableUid", acc.stableUid)
                    put("name", acc.name)
                    put("type", acc.accountType)
                    put("balance", acc.balance)
                    put("balancePaise", acc.balancePaise)
                    put("currency", acc.currency)
                    put("institution", acc.institution)
                    put("accountNumber", acc.accountNumber)
                    put("notes", acc.notes)
                    put("isArchived", acc.isArchived)
                    put("createdAt", acc.createdAt)
                    put("updatedAt", acc.updatedAt)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            transactionRows.forEach { tx ->
                put(JSONObject().apply {
                    put("id", tx.id)
                    put("stableUid", tx.stableUid)
                    put("accountId", tx.accountId)
                    put("type", tx.type)
                    put("amount", tx.amount)
                    put("amountPaise", tx.amountPaise)
                    put("category", tx.category)
                    put("subcategory", tx.subcategory)
                    put("note", tx.note)
                    put("timestamp", tx.timestamp)
                    put("date", TransactionDateTime.formatDateInput(tx.timestamp))
                    put("time", TransactionDateTime.formatTimeInput(tx.timestamp))
                    put("personId", tx.personId ?: JSONObject.NULL)
                    put("transferAccountId", tx.transferAccountId ?: JSONObject.NULL)
                    put("transferId", tx.transferId ?: JSONObject.NULL)
                    put("paymentMethod", tx.paymentMethod)
                    put("upiApp", tx.upiApp)
                })
            }
        })

        root.put("budgets", JSONArray().apply {
            budgetRows.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("stableUid", b.stableUid)
                    put("category", b.category)
                    put("limit", b.limitAmount)
                    put("limitAmountPaise", b.limitAmountPaise)
                    put("month", b.month)
                })
            }
        })

        root.put("goals", JSONArray().apply {
            goalRows.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("stableUid", g.stableUid)
                    put("name", g.name)
                    put("target", g.targetAmount)
                    put("targetAmountPaise", g.targetAmountPaise)
                    put("saved", g.savedAmount)
                    put("savedAmountPaise", g.savedAmountPaise)
                    put("deadline", g.deadline ?: JSONObject.NULL)
                })
            }
        })

        root.put("goalContributions", JSONArray().apply {
            goalContribRows.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("stableUid", c.stableUid)
                    put("goalId", c.goalId)
                    put("accountId", c.accountId ?: JSONObject.NULL)
                    put("transactionId", c.transactionId ?: JSONObject.NULL)
                    put("amount", c.amount)
                    put("amountPaise", c.amountPaise)
                    put("timestamp", c.timestamp)
                    put("note", c.note)
                })
            }
        })

        root.put("loans", JSONArray().apply {
            loanRows.forEach { l ->
                put(JSONObject().apply {
                    put("id", l.id)
                    put("stableUid", l.stableUid)
                    put("name", l.name)
                    put("principal", l.principal)
                    put("principalPaise", l.principalPaise)
                    put("paid", l.paidAmount)
                    put("paidAmountPaise", l.paidAmountPaise)
                    put("monthly", l.monthlyPayment)
                    put("monthlyPaymentPaise", l.monthlyPaymentPaise)
                    put("interest", l.interestRate)
                    put("dueDay", l.dueDay)
                })
            }
        })

        root.put("loanPayments", JSONArray().apply {
            loanPaymentRows.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("stableUid", p.stableUid)
                    put("loanId", p.loanId)
                    put("accountId", p.accountId ?: JSONObject.NULL)
                    put("transactionId", p.transactionId ?: JSONObject.NULL)
                    put("amount", p.amount)
                    put("amountPaise", p.amountPaise)
                    put("timestamp", p.timestamp)
                    put("note", p.note)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            categoryRows.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("stableUid", c.stableUid)
                    put("name", c.name)
                    put("type", c.type)
                    put("parentId", c.parentId ?: JSONObject.NULL)
                    put("icon", c.icon)
                    put("system", c.isSystem)
                })
            }
        })

        root.put("persons", JSONArray().apply {
            personRows.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("stableUid", p.stableUid)
                    put("name", p.name)
                    put("phone", p.phone)
                    put("notes", p.notes)
                })
            }
        })

        root.put("customTrackers", JSONArray().apply {
            for (t in trackerRows) {
                val tLinks = allLinks.filter { it.trackerUid == t.stableUid }
                val tPeople = trackersDao.getTrackerPeople(t.stableUid)
                val tSettlements = trackersDao.getTrackerSettlements(t.stableUid)
                put(JSONObject().apply {
                    put("stableUid", t.stableUid)
                    put("name", t.name)
                    put("trackerType", t.trackerType)
                    put("customTypeName", t.customTypeName)
                    put("description", t.description)
                    put("icon", t.icon)
                    put("themeColor", t.themeColor)
                    put("startDate", t.startDate)
                    put("endDate", t.endDate)
                    put("budgetAmount", t.budgetAmount)
                    put("budgetMode", t.budgetMode)
                    put("currency", t.currency)
                    put("notes", t.notes)
                    put("status", t.status)
                    put("isArchived", t.isArchived)
                    put("transactionLinks", JSONArray().apply {
                        tLinks.forEach { lk ->
                            put(JSONObject().apply {
                                put("transactionUid", lk.transactionUid)
                                put("isRefund", lk.isRefund)
                                put("paidByPersonUid", lk.paidByPersonUid)
                                put("splitMode", lk.splitMode)
                            })
                        }
                    })
                    put("people", JSONArray().apply {
                        tPeople.forEach { p ->
                            put(JSONObject().apply {
                                put("personUid", p.personUid)
                                put("name", p.name)
                                put("phone", p.phone)
                                put("notes", p.notes)
                            })
                        }
                    })
                    put("settlements", JSONArray().apply {
                        tSettlements.forEach { s ->
                            put(JSONObject().apply {
                                put("settlementUid", s.settlementUid)
                                put("fromPersonUid", s.fromPersonUid)
                                put("toPersonUid", s.toPersonUid)
                                put("amount", s.amount)
                                put("isFullSettlement", s.isFullSettlement)
                                put("date", s.date)
                                put("note", s.note)
                            })
                        }
                    })
                })
            }
        })

        return root.toString()
    }

    suspend fun restoreJson(json: String) {
        val parsed = runCatching { JSONObject(json) }.getOrElse {
            throw IllegalArgumentException("Invalid Tracmio backup JSON")
        }
        val root = if (parsed.has("data") && parsed.optJSONObject("data") != null) {
            parsed.getJSONObject("data")
        } else {
            parsed
        }
        require(root.has("accounts") || root.has("transactions")) {
            "Invalid Tracmio backup"
        }

        val accountIdMap = mutableMapOf<String, Long>()
        val personIdMap = mutableMapOf<String, Long>()
        val goalIdMap = mutableMapOf<String, Long>()
        val loanIdMap = mutableMapOf<String, Long>()
        val txIdMap = mutableMapOf<String, Long>()

        database.withTransaction {
            finance.deleteAllGoalContributions()
            finance.deleteAllLoanPayments()
            finance.deleteAllTransactions()
            finance.deleteAllBudgets()
            finance.deleteAllGoals()
            finance.deleteAllLoans()
            finance.deleteAllCategories()
            finance.deleteAllPersons()
            accounts.deleteAll()

            val personsArray = root.optJSONArray("persons") ?: root.optJSONArray("people") ?: JSONArray()
            for (i in 0 until personsArray.length()) {
                val obj = personsArray.optJSONObject(i) ?: continue
                val oldKey = obj.opt("id")?.toString().orEmpty()
                val newId = finance.insertPerson(
                    PersonEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        name = obj.optString("name", "Person"),
                        phone = obj.optString("phone", ""),
                        notes = obj.optString("notes", obj.optString("note", ""))
                    )
                )
                if (oldKey.isNotEmpty()) personIdMap[oldKey] = newId
            }

            val accountsArray = root.optJSONArray("accounts") ?: JSONArray()
            for (i in 0 until accountsArray.length()) {
                val obj = accountsArray.optJSONObject(i) ?: continue
                val oldKey = obj.opt("id")?.toString().orEmpty()
                val paise = if (obj.has("balancePaise")) {
                    obj.optLong("balancePaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("balance", 0.0))
                }
                val newId = accounts.insertAccount(
                    AccountEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        name = obj.optString("name", "Account"),
                        accountType = obj.optString("type", obj.optString("accountType", "Bank")),
                        balance = MoneyPaise.toRupees(paise),
                        balancePaise = paise,
                        currency = obj.optString("currency").ifBlank { "INR" },
                        institution = obj.optString("institution", ""),
                        accountNumber = obj.optString("accountNumber", ""),
                        notes = obj.optString("notes", ""),
                        isArchived = obj.optBoolean("isArchived", false)
                    )
                )
                if (oldKey.isNotEmpty()) accountIdMap[oldKey] = newId
            }

            val defaultAccountId = accountIdMap.values.firstOrNull() ?: accounts.insertAccount(
                AccountEntity(name = "Main Wallet", accountType = "Cash", balance = 0.0, balancePaise = 0L)
            )

            val transactionsArray = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until transactionsArray.length()) {
                val obj = transactionsArray.optJSONObject(i) ?: continue
                val oldIdKey = obj.opt("id")?.toString().orEmpty()
                val rawAccKey = obj.opt("accountId")?.toString().orEmpty()
                val accountId = accountIdMap[rawAccKey] ?: defaultAccountId
                val rawTransferAccKey = obj.opt("transferAccountId")?.toString().orEmpty()
                val transferAccId = accountIdMap[rawTransferAccKey]
                val rawPersonKey = obj.opt("personId")?.toString().orEmpty()
                val personId = personIdMap[rawPersonKey]

                val paise = if (obj.has("amountPaise")) {
                    obj.optLong("amountPaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("amount", 0.0))
                }
                val rawType = obj.optString("type", "EXPENSE").uppercase()
                val normalizedType = when (rawType) {
                    "TRANSFER" -> if (transferAccId != null) "TRANSFER_OUT" else "EXPENSE"
                    else -> rawType
                }
                val timestamp = if (obj.has("timestamp") && obj.optLong("timestamp", 0L) > 0L) {
                    obj.optLong("timestamp")
                } else {
                    TransactionDateTime.parseDateAndTime(
                        obj.optString("date", ""),
                        obj.optString("time", "12:00")
                    )
                }

                val newTxId = finance.insertTransaction(
                    TransactionEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        accountId = accountId,
                        type = normalizedType,
                        amount = MoneyPaise.toRupees(paise),
                        amountPaise = paise,
                        category = obj.optString("category").ifBlank { "General" },
                        subcategory = obj.optString("subcategory", ""),
                        note = obj.optString("note", obj.optString("notes", "")),
                        timestamp = timestamp,
                        personId = personId,
                        transferAccountId = transferAccId,
                        transferId = obj.optString("transferId").takeIf { it.isNotBlank() && it != "null" },
                        paymentMethod = obj.optString("paymentMethod", obj.optString("method", "Cash")).ifBlank { "Cash" },
                        upiApp = obj.optString("upiApp", "")
                    )
                )
                if (oldIdKey.isNotEmpty()) txIdMap[oldIdKey] = newTxId
            }

            val budgetsArray = root.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until budgetsArray.length()) {
                val obj = budgetsArray.optJSONObject(i) ?: continue
                val paise = if (obj.has("limitAmountPaise")) {
                    obj.optLong("limitAmountPaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("limit", obj.optDouble("limitAmount", 0.0)))
                }
                finance.insertBudget(
                    BudgetEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        category = obj.optString("category", "General"),
                        limitAmount = MoneyPaise.toRupees(paise),
                        limitAmountPaise = paise,
                        month = obj.optString("month", "")
                    )
                )
            }

            val goalsArray = root.optJSONArray("goals") ?: JSONArray()
            for (i in 0 until goalsArray.length()) {
                val obj = goalsArray.optJSONObject(i) ?: continue
                val oldKey = obj.opt("id")?.toString().orEmpty()
                val targetPaise = if (obj.has("targetAmountPaise")) {
                    obj.optLong("targetAmountPaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("target", obj.optDouble("targetAmount", 0.0)))
                }
                val savedPaise = if (obj.has("savedAmountPaise")) {
                    obj.optLong("savedAmountPaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("saved", obj.optDouble("savedAmount", 0.0)))
                }
                val newGoalId = finance.insertGoal(
                    GoalEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        name = obj.optString("name", "Goal"),
                        targetAmount = MoneyPaise.toRupees(targetPaise),
                        targetAmountPaise = targetPaise,
                        savedAmount = MoneyPaise.toRupees(savedPaise),
                        savedAmountPaise = savedPaise,
                        deadline = obj.optLong("deadline", 0L).takeIf { it > 0L }
                    )
                )
                if (oldKey.isNotEmpty()) goalIdMap[oldKey] = newGoalId
            }

            val goalContribsArray = root.optJSONArray("goalContributions") ?: JSONArray()
            for (i in 0 until goalContribsArray.length()) {
                val obj = goalContribsArray.optJSONObject(i) ?: continue
                val mappedGoalId = goalIdMap[obj.opt("goalId")?.toString().orEmpty()] ?: continue
                val mappedAccountId = accountIdMap[obj.opt("accountId")?.toString().orEmpty()]
                val mappedTxId = txIdMap[obj.opt("transactionId")?.toString().orEmpty()]
                val paise = if (obj.has("amountPaise")) {
                    obj.optLong("amountPaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("amount", 0.0))
                }
                finance.insertGoalContribution(
                    GoalContributionEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        goalId = mappedGoalId,
                        accountId = mappedAccountId,
                        transactionId = mappedTxId,
                        amount = MoneyPaise.toRupees(paise),
                        amountPaise = paise,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        note = obj.optString("note", "")
                    )
                )
            }

            val loansArray = root.optJSONArray("loans") ?: JSONArray()
            for (i in 0 until loansArray.length()) {
                val obj = loansArray.optJSONObject(i) ?: continue
                val oldKey = obj.opt("id")?.toString().orEmpty()
                val principalPaise = if (obj.has("principalPaise")) {
                    obj.optLong("principalPaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("principal", 0.0))
                }
                val paidPaise = if (obj.has("paidAmountPaise")) {
                    obj.optLong("paidAmountPaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("paid", obj.optDouble("paidAmount", 0.0)))
                }
                val monthlyPaise = if (obj.has("monthlyPaymentPaise")) {
                    obj.optLong("monthlyPaymentPaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("monthly", obj.optDouble("emi", 0.0)))
                }
                val newLoanId = finance.insertLoan(
                    LoanEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        name = obj.optString("name", "Loan"),
                        principal = MoneyPaise.toRupees(principalPaise),
                        principalPaise = principalPaise,
                        paidAmount = MoneyPaise.toRupees(paidPaise),
                        paidAmountPaise = paidPaise,
                        monthlyPayment = MoneyPaise.toRupees(monthlyPaise),
                        monthlyPaymentPaise = monthlyPaise,
                        interestRate = obj.optDouble("interest", obj.optDouble("rate", 0.0)),
                        dueDay = obj.optInt("dueDay", 5)
                    )
                )
                if (oldKey.isNotEmpty()) loanIdMap[oldKey] = newLoanId
            }

            val loanPaymentsArray = root.optJSONArray("loanPayments") ?: JSONArray()
            for (i in 0 until loanPaymentsArray.length()) {
                val obj = loanPaymentsArray.optJSONObject(i) ?: continue
                val mappedLoanId = loanIdMap[obj.opt("loanId")?.toString().orEmpty()] ?: continue
                val mappedAccountId = accountIdMap[obj.opt("accountId")?.toString().orEmpty()]
                val mappedTxId = txIdMap[obj.opt("transactionId")?.toString().orEmpty()]
                val paise = if (obj.has("amountPaise")) {
                    obj.optLong("amountPaise")
                } else {
                    MoneyPaise.toPaise(obj.optDouble("amount", 0.0))
                }
                finance.insertLoanPayment(
                    LoanPaymentEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        loanId = mappedLoanId,
                        accountId = mappedAccountId,
                        transactionId = mappedTxId,
                        amount = MoneyPaise.toRupees(paise),
                        amountPaise = paise,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        note = obj.optString("note", "")
                    )
                )
            }

            val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until categoriesArray.length()) {
                val obj = categoriesArray.optJSONObject(i) ?: continue
                finance.insertCategory(
                    CategoryEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        name = obj.optString("name", "Category"),
                        type = obj.optString("type", "EXPENSE").uppercase(),
                        icon = obj.optString("icon", ""),
                        isSystem = obj.optBoolean("system", obj.optBoolean("isSystem", false))
                    )
                )
            }

            val trackersArr = root.optJSONArray("customTrackers") ?: root.optJSONArray("trackers") ?: JSONArray()
            for (i in 0 until trackersArr.length()) {
                val t = trackersArr.optJSONObject(i) ?: continue
                val tUid = t.optString("stableUid", t.optString("id", "")).ifBlank { UUID.randomUUID().toString() }
                trackersDao.upsertTracker(
                    CustomTrackerEntity(
                        stableUid = tUid,
                        name = t.optString("name", "Tracker"),
                        trackerType = t.optString("trackerType", t.optString("type", "Custom")),
                        customTypeName = t.optString("customTypeName", ""),
                        description = t.optString("description", ""),
                        icon = t.optString("icon", "🎯"),
                        themeColor = t.optString("themeColor", "#00D6A3"),
                        startDate = t.optString("startDate", ""),
                        endDate = t.optString("endDate", ""),
                        budgetAmount = t.optDouble("budgetAmount", t.optDouble("budget", 0.0)),
                        budgetMode = t.optString("budgetMode", "FIXED"),
                        currency = t.optString("currency", "₹"),
                        notes = t.optString("notes", ""),
                        status = t.optString("status", "ACTIVE"),
                        isArchived = t.optBoolean("isArchived", false)
                    )
                )
                val linksArr = t.optJSONArray("transactionLinks") ?: JSONArray()
                for (j in 0 until linksArr.length()) {
                    val lk = linksArr.optJSONObject(j) ?: continue
                    val txUid = lk.optString("transactionUid", lk.optString("transactionId", ""))
                    if (txUid.isNotBlank()) {
                        trackersDao.linkTransactionToTracker(
                            TrackerTransactionCrossRefEntity(
                                trackerUid = tUid,
                                transactionUid = txUid,
                                isRefund = lk.optBoolean("isRefund", false),
                                paidByPersonUid = lk.optString("paidByPersonUid", ""),
                                splitMode = lk.optString("splitMode", "EQUAL")
                            )
                        )
                    }
                }
                val peopleArr = t.optJSONArray("people") ?: JSONArray()
                for (j in 0 until peopleArr.length()) {
                    val p = peopleArr.optJSONObject(j) ?: continue
                    val pUid = p.optString("personUid", p.optString("id", "")).ifBlank { "$tUid-p-$j" }
                    trackersDao.upsertTrackerPerson(
                        TrackerPersonEntity(
                            personUid = pUid,
                            trackerUid = tUid,
                            name = p.optString("name", "Member"),
                            phone = p.optString("phone", ""),
                            notes = p.optString("notes", "")
                        )
                    )
                }
                val settlementsArr = t.optJSONArray("settlements") ?: JSONArray()
                for (j in 0 until settlementsArr.length()) {
                    val s = settlementsArr.optJSONObject(j) ?: continue
                    val sUid = s.optString("settlementUid", s.optString("id", "")).ifBlank { "$tUid-s-$j" }
                    trackersDao.upsertTrackerSettlement(
                        TrackerSettlementEntity(
                            settlementUid = sUid,
                            trackerUid = tUid,
                            fromPersonUid = s.optString("fromPersonUid", ""),
                            toPersonUid = s.optString("toPersonUid", ""),
                            amount = s.optDouble("amount", 0.0),
                            isFullSettlement = s.optBoolean("isFullSettlement", false),
                            date = s.optString("date", ""),
                            note = s.optString("note", "")
                        )
                    )
                }
            }
        }
    }
}
