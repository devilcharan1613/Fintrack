package com.tracemeo.app.data.repository

import androidx.room.withTransaction
import com.tracemeo.app.data.MoneyPaise
import com.tracemeo.app.data.NativeLedgerCalculator
import com.tracemeo.app.data.PreferencesAndThemeStore
import com.tracemeo.app.data.TransactionDateTime
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.BudgetEntity
import com.tracemeo.app.data.local.CashbackEntryEntity
import com.tracemeo.app.data.local.CategoryEntity
import com.tracemeo.app.data.local.CustomTrackerEntity
import com.tracemeo.app.data.local.GoalContributionEntity
import com.tracemeo.app.data.local.GoalEntity
import com.tracemeo.app.data.local.LoanEntity
import com.tracemeo.app.data.local.LoanPaymentEntity
import com.tracemeo.app.data.local.PersonEntity
import com.tracemeo.app.data.local.QuickTemplateEntity
import com.tracemeo.app.data.local.ReminderEntity
import com.tracemeo.app.data.local.TracmioDatabase
import com.tracemeo.app.data.local.TrackerPersonEntity
import com.tracemeo.app.data.local.TrackerSettlementEntity
import com.tracemeo.app.data.local.TrackerTransactionCrossRefEntity
import com.tracemeo.app.data.local.TransactionEntity
import com.tracemeo.app.data.local.ValueItemEntity
import com.tracemeo.app.data.local.WishlistItemEntity
import com.tracemeo.app.data.security.TracmioSecurityEngine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NativeBackupService(
    private val database: TracmioDatabase,
    private val prefsStore: PreferencesAndThemeStore? = null
) {
    private val accounts = database.accountDao()
    private val finance = database.financeDao()
    private val trackersDao = database.customTrackerDao()
    private val features = database.featureDao()

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
        val quickTemplates = features.quickTemplates().first()
        val wishlist = features.wishlistItems().first()
        val cashback = features.cashbackEntries().first()
        val valueItems = features.valueItems().first()
        val reminderRows = features.reminders().first()

        val root = JSONObject()
        root.put("app", "Tracmio")
        root.put("version", "16.13")
        root.put("schemaVersion", 9)
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
                    put("allocationsJson", acc.allocationsJson)
                    put("creditLimit", acc.creditLimit)
                    put("billingCycleDay", acc.billingCycleDay)
                    put("dueDay", acc.dueDay)
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
                    put("hashtags", tx.hashtags)
                    put("timestamp", tx.timestamp)
                    put("date", TransactionDateTime.formatDateInput(tx.timestamp))
                    put("time", TransactionDateTime.formatTimeInput(tx.timestamp))
                    put("personId", tx.personId ?: JSONObject.NULL)
                    put("transferAccountId", tx.transferAccountId ?: JSONObject.NULL)
                    put("transferId", tx.transferId ?: JSONObject.NULL)
                    put("paymentMethod", tx.paymentMethod)
                    put("upiApp", tx.upiApp)
                    put("warrantyDate", tx.warrantyDate)
                    put("attachmentUri", tx.attachmentUri)
                    put("allocationId", tx.allocationId)
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

        root.put("quickTemplates", JSONArray().apply {
            quickTemplates.forEach { qt ->
                put(JSONObject().apply {
                    put("stableUid", qt.stableUid)
                    put("icon", qt.icon)
                    put("label", qt.label)
                    put("amount", qt.amount)
                    put("category", qt.category)
                })
            }
        })

        root.put("wishlist", JSONArray().apply {
            wishlist.forEach { w ->
                put(JSONObject().apply {
                    put("stableUid", w.stableUid)
                    put("name", w.name)
                    put("price", w.price)
                    put("addedAt", w.addedAt)
                    put("unlockedAt", w.unlockedAt)
                    put("status", w.status)
                })
            }
        })

        root.put("cashback", JSONArray().apply {
            cashback.forEach { cb ->
                put(JSONObject().apply {
                    put("stableUid", cb.stableUid)
                    put("source", cb.source)
                    put("amount", cb.amount)
                    put("note", cb.note)
                    put("timestamp", cb.timestamp)
                })
            }
        })

        root.put("valueItems", JSONArray().apply {
            valueItems.forEach { vi ->
                put(JSONObject().apply {
                    put("stableUid", vi.stableUid)
                    put("name", vi.name)
                    put("cost", vi.cost)
                    put("uses", vi.uses)
                    put("createdAt", vi.createdAt)
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

        root.put("reminders", JSONArray().apply {
            reminderRows.forEach { r ->
                put(JSONObject().apply {
                    put("stableUid", r.stableUid)
                    put("title", r.title)
                    put("kind", r.kind)
                    put("hour", r.hour)
                    put("minute", r.minute)
                    put("dayOfMonth", r.dayOfMonth)
                    put("triggerDate", r.triggerDate)
                    put("isEnabled", r.isEnabled)
                    put("note", r.note)
                    put("createdAt", r.createdAt)
                })
            }
        })

        prefsStore?.let { store ->
            root.put("appPreferences", store.exportPreferencesJson())
        }

        return root.toString()
    }

    suspend fun restoreJson(json: String) {
        val parsed = TracmioSecurityEngine.validateBackupJson(json)
        val root = parsed.optJSONObject("data") ?: parsed
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
            features.deleteAllQuickTemplates()
            features.deleteAllWishlistItems()
            features.deleteAllCashbackEntries()
            features.deleteAllValueItems()
            features.deleteAllReminders()
            accounts.deleteAll()

            val personsArray = root.optJSONArray("persons") ?: root.optJSONArray("people") ?: JSONArray()
            for (i in 0 until personsArray.length()) {
                val obj = personsArray.optJSONObject(i) ?: continue
                val oldKey = obj.opt("id")?.toString().orEmpty()
                val newId = finance.insertPerson(
                    PersonEntity(
                        stableUid = TracmioSecurityEngine.sanitizeInput(obj.optString("stableUid"), 64).ifBlank { UUID.randomUUID().toString() },
                        name = TracmioSecurityEngine.sanitizeInput(obj.optString("name", "Person"), 80),
                        phone = TracmioSecurityEngine.sanitizeInput(obj.optString("phone", ""), 30),
                        notes = TracmioSecurityEngine.sanitizeInput(obj.optString("notes", obj.optString("note", "")), 250)
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
                val allocsRaw = obj.optString("allocationsJson", "").ifBlank {
                    obj.optJSONArray("allocations")?.toString() ?: "[]"
                }
                val newId = accounts.insertAccount(
                    AccountEntity(
                        stableUid = TracmioSecurityEngine.sanitizeInput(obj.optString("stableUid"), 64).ifBlank { UUID.randomUUID().toString() },
                        name = TracmioSecurityEngine.sanitizeInput(obj.optString("name", "Account"), 80),
                        accountType = TracmioSecurityEngine.sanitizeInput(obj.optString("type", obj.optString("accountType", "Bank")), 30),
                        balance = MoneyPaise.toRupees(paise),
                        balancePaise = paise,
                        currency = TracmioSecurityEngine.sanitizeInput(obj.optString("currency"), 8).ifBlank { "INR" },
                        institution = TracmioSecurityEngine.sanitizeInput(obj.optString("institution", ""), 80),
                        accountNumber = TracmioSecurityEngine.sanitizeInput(obj.optString("accountNumber", ""), 32),
                        notes = TracmioSecurityEngine.sanitizeInput(obj.optString("notes", ""), 250),
                        allocationsJson = allocsRaw,
                        creditLimit = obj.optDouble("creditLimit", 0.0).coerceAtLeast(0.0),
                        billingCycleDay = obj.optInt("billingCycleDay", 1).coerceIn(1, 28),
                        dueDay = obj.optInt("dueDay", 15).coerceIn(1, 28),
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
                    "INCOME", "EXPENSE", "TRANSFER_OUT", "TRANSFER_IN", "BORROW", "LEND" -> rawType
                    else -> "EXPENSE"
                }
                val timestamp = if (obj.has("timestamp") && obj.optLong("timestamp", 0L) > 0L) {
                    obj.optLong("timestamp")
                } else {
                    TransactionDateTime.parseDateAndTime(
                        obj.optString("date", ""),
                        obj.optString("time", "12:00")
                    )
                }

                val cleanNote = TracmioSecurityEngine.sanitizeInput(obj.optString("note", obj.optString("notes", "")), 400)
                val rawTags = TracmioSecurityEngine.sanitizeInput(obj.optString("hashtags", ""), 200)
                val newTxId = finance.insertTransaction(
                    TransactionEntity(
                        stableUid = TracmioSecurityEngine.sanitizeInput(obj.optString("stableUid"), 64).ifBlank { UUID.randomUUID().toString() },
                        accountId = accountId,
                        type = normalizedType,
                        amount = MoneyPaise.toRupees(paise),
                        amountPaise = paise,
                        category = TracmioSecurityEngine.sanitizeInput(obj.optString("category"), 60).ifBlank { "Other" },
                        subcategory = TracmioSecurityEngine.sanitizeInput(obj.optString("subcategory", ""), 60),
                        note = cleanNote,
                        hashtags = NativeLedgerCalculator.formatHashtags(cleanNote, rawTags),
                        timestamp = timestamp,
                        personId = personId,
                        transferAccountId = transferAccId,
                        transferId = obj.optString("transferId").takeIf { it.isNotBlank() && it != "null" },
                        paymentMethod = TracmioSecurityEngine.sanitizeInput(obj.optString("paymentMethod", obj.optString("method", "Cash")), 30).ifBlank { "Cash" },
                        upiApp = TracmioSecurityEngine.sanitizeInput(obj.optString("upiApp", ""), 30),
                        warrantyDate = TracmioSecurityEngine.sanitizeInput(obj.optString("warrantyDate", ""), 20),
                        attachmentUri = TracmioSecurityEngine.sanitizeInput(obj.optString("attachmentUri", ""), 300),
                        allocationId = TracmioSecurityEngine.sanitizeInput(obj.optString("allocationId", ""), 64)
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
                        category = TracmioSecurityEngine.sanitizeInput(obj.optString("category", "Other"), 60),
                        limitAmount = MoneyPaise.toRupees(paise),
                        limitAmountPaise = paise,
                        month = TracmioSecurityEngine.sanitizeInput(obj.optString("month", ""), 16)
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
                        name = TracmioSecurityEngine.sanitizeInput(obj.optString("name", "Goal"), 80),
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
                        note = TracmioSecurityEngine.sanitizeInput(obj.optString("note", ""), 200)
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
                        name = TracmioSecurityEngine.sanitizeInput(obj.optString("name", "Loan"), 80),
                        principal = MoneyPaise.toRupees(principalPaise),
                        principalPaise = principalPaise,
                        paidAmount = MoneyPaise.toRupees(paidPaise),
                        paidAmountPaise = paidPaise,
                        monthlyPayment = MoneyPaise.toRupees(monthlyPaise),
                        monthlyPaymentPaise = monthlyPaise,
                        interestRate = obj.optDouble("interest", obj.optDouble("rate", 0.0)).coerceAtLeast(0.0),
                        dueDay = obj.optInt("dueDay", 5).coerceIn(1, 28)
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
                        note = TracmioSecurityEngine.sanitizeInput(obj.optString("note", ""), 200)
                    )
                )
            }

            val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until categoriesArray.length()) {
                val obj = categoriesArray.optJSONObject(i) ?: continue
                finance.insertCategory(
                    CategoryEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        name = TracmioSecurityEngine.sanitizeInput(obj.optString("name", "Category"), 50),
                        type = obj.optString("type", "EXPENSE").uppercase(),
                        icon = TracmioSecurityEngine.sanitizeInput(obj.optString("icon", ""), 8),
                        isSystem = obj.optBoolean("system", obj.optBoolean("isSystem", false))
                    )
                )
            }

            val qtArr = root.optJSONArray("quickTemplates") ?: JSONArray()
            for (i in 0 until qtArr.length()) {
                val obj = qtArr.optJSONObject(i) ?: continue
                features.insertQuickTemplate(
                    QuickTemplateEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        icon = TracmioSecurityEngine.sanitizeInput(obj.optString("icon", "⚡"), 8),
                        label = TracmioSecurityEngine.sanitizeInput(obj.optString("label", "Quick Spend"), 40),
                        amount = obj.optDouble("amount", 0.0).coerceAtLeast(1.0),
                        category = TracmioSecurityEngine.sanitizeInput(obj.optString("category", "Food"), 40)
                    )
                )
            }

            val wishArr = root.optJSONArray("wishlist") ?: JSONArray()
            for (i in 0 until wishArr.length()) {
                val obj = wishArr.optJSONObject(i) ?: continue
                features.insertWishlistItem(
                    WishlistItemEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        name = TracmioSecurityEngine.sanitizeInput(obj.optString("name", "Item"), 80),
                        price = obj.optDouble("price", 0.0).coerceAtLeast(0.0),
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis()),
                        unlockedAt = obj.optLong("unlockedAt", System.currentTimeMillis() + 86_400_000L),
                        status = TracmioSecurityEngine.sanitizeInput(obj.optString("status", "COOLING"), 20)
                    )
                )
            }

            val cbArr = root.optJSONArray("cashback") ?: JSONArray()
            for (i in 0 until cbArr.length()) {
                val obj = cbArr.optJSONObject(i) ?: continue
                features.insertCashbackEntry(
                    CashbackEntryEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        source = TracmioSecurityEngine.sanitizeInput(obj.optString("source", "Reward"), 60),
                        amount = obj.optDouble("amount", 0.0).coerceAtLeast(0.0),
                        note = TracmioSecurityEngine.sanitizeInput(obj.optString("note", ""), 160),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            val viArr = root.optJSONArray("valueItems") ?: JSONArray()
            for (i in 0 until viArr.length()) {
                val obj = viArr.optJSONObject(i) ?: continue
                features.insertValueItem(
                    ValueItemEntity(
                        stableUid = obj.optString("stableUid").ifBlank { UUID.randomUUID().toString() },
                        name = TracmioSecurityEngine.sanitizeInput(obj.optString("name", "Purchase"), 80),
                        cost = obj.optDouble("cost", 0.0).coerceAtLeast(0.0),
                        uses = obj.optInt("uses", 1).coerceAtLeast(1),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val trackersArr = root.optJSONArray("customTrackers") ?: root.optJSONArray("trackers") ?: JSONArray()
            for (i in 0 until trackersArr.length()) {
                val t = trackersArr.optJSONObject(i) ?: continue
                val tUid = TracmioSecurityEngine.sanitizeInput(t.optString("stableUid", t.optString("id", "")), 64).ifBlank { UUID.randomUUID().toString() }
                trackersDao.upsertTracker(
                    CustomTrackerEntity(
                        stableUid = tUid,
                        name = TracmioSecurityEngine.sanitizeInput(t.optString("name", "Tracker"), 80),
                        trackerType = TracmioSecurityEngine.sanitizeInput(t.optString("trackerType", t.optString("type", "Custom")), 30),
                        customTypeName = TracmioSecurityEngine.sanitizeInput(t.optString("customTypeName", ""), 40),
                        description = TracmioSecurityEngine.sanitizeInput(t.optString("description", ""), 200),
                        icon = TracmioSecurityEngine.sanitizeInput(t.optString("icon", "🎯"), 8),
                        themeColor = TracmioSecurityEngine.sanitizeInput(t.optString("themeColor", "#00D6A3"), 16),
                        startDate = TracmioSecurityEngine.sanitizeInput(t.optString("startDate", ""), 20),
                        endDate = TracmioSecurityEngine.sanitizeInput(t.optString("endDate", ""), 20),
                        budgetAmount = t.optDouble("budgetAmount", t.optDouble("budget", 0.0)).coerceAtLeast(0.0),
                        budgetMode = TracmioSecurityEngine.sanitizeInput(t.optString("budgetMode", "FIXED"), 20),
                        currency = TracmioSecurityEngine.sanitizeInput(t.optString("currency", "₹"), 8),
                        notes = TracmioSecurityEngine.sanitizeInput(t.optString("notes", ""), 250),
                        status = TracmioSecurityEngine.sanitizeInput(t.optString("status", "ACTIVE"), 20),
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
                            name = TracmioSecurityEngine.sanitizeInput(p.optString("name", "Member"), 80),
                            phone = TracmioSecurityEngine.sanitizeInput(p.optString("phone", ""), 30),
                            notes = TracmioSecurityEngine.sanitizeInput(p.optString("notes", ""), 200)
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
                            amount = s.optDouble("amount", 0.0).coerceAtLeast(0.0),
                            isFullSettlement = s.optBoolean("isFullSettlement", false),
                            date = s.optString("date", ""),
                            note = TracmioSecurityEngine.sanitizeInput(s.optString("note", ""), 200)
                        )
                    )
                }
            }
            val remArr = root.optJSONArray("reminders") ?: JSONArray()
            for (i in 0 until remArr.length()) {
                val obj = remArr.optJSONObject(i) ?: continue
                features.insertReminder(
                    ReminderEntity(
                        stableUid = TracmioSecurityEngine.sanitizeInput(obj.optString("stableUid"), 64).ifBlank { UUID.randomUUID().toString() },
                        title = TracmioSecurityEngine.sanitizeInput(obj.optString("title", "Reminder"), 80),
                        kind = TracmioSecurityEngine.sanitizeInput(obj.optString("kind", "CUSTOM"), 24),
                        hour = obj.optInt("hour", 20).coerceIn(0, 23),
                        minute = obj.optInt("minute", 0).coerceIn(0, 59),
                        dayOfMonth = obj.optInt("dayOfMonth", 1).coerceIn(1, 28),
                        triggerDate = TracmioSecurityEngine.sanitizeInput(obj.optString("triggerDate", ""), 20),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        note = TracmioSecurityEngine.sanitizeInput(obj.optString("note", ""), 200),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        root.optJSONObject("appPreferences")?.let { prefObj ->
            prefsStore?.importPreferencesJson(prefObj)
        }
        prefsStore?.let { store ->
            FinanceRepository(database, store).syncSettingsToRoom(store.settings.value)
        }
    }
}
