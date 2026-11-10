package com.charan.fintrack.data.backup

import androidx.room.withTransaction
import com.charan.fintrack.data.MoneyPaise
import com.charan.fintrack.data.NativeLedgerCalculator
import com.charan.fintrack.data.local.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class BackupMergeReport(
    val accountsAdded: Int = 0,
    val transactionsAdded: Int = 0,
    val peopleAdded: Int = 0,
    val categoriesAdded: Int = 0,
    val budgetsAdded: Int = 0,
    val goalsAdded: Int = 0,
    val contributionsAdded: Int = 0,
    val loansAdded: Int = 0,
    val repaymentsAdded: Int = 0,
    val duplicatesSkipped: Int = 0
) {
    val totalAdded: Int
        get() = accountsAdded + transactionsAdded + peopleAdded + categoriesAdded +
            budgetsAdded + goalsAdded + contributionsAdded + loansAdded + repaymentsAdded
}

/**
 * Local JSON backup codec and merge importer.
 *
 * - Does not clear or replace existing rows.
 * - Uses stable UIDs and IGNORE semantics to make repeated imports idempotent.
 * - Imports all records in one Room transaction.
 * - Converts legacy HTML account current-balances back into opening balances by
 *   reversing the original transactions before those transactions become ledger rows.
 * - Loan repayments and goal contributions remain tracking-only records.
 */
class NativeBackupService(private val database: FinTrackDatabase) {
    private data class ExportSnapshot(
        val accounts: List<AccountEntity>,
        val transactions: List<TransactionEntity>,
        val people: List<PersonEntity>,
        val categories: List<CategoryEntity>,
        val budgets: List<BudgetEntity>,
        val goals: List<GoalEntity>,
        val contributions: List<GoalContributionEntity>,
        val loans: List<LoanEntity>,
        val payments: List<LoanPaymentEntity>
    )

    private val accounts = database.accountDao()
    private val finance = database.financeDao()

    companion object {
        const val FORMAT = "FinTrackNativeBackup"
        const val SCHEMA_VERSION = 2
    }

    suspend fun exportJson(): String {
        // Read related finance tables in one Room transaction so a concurrent
        // edit cannot produce a backup with mismatched account/transaction state.
        val snapshot = database.withTransaction {
            ExportSnapshot(
                accounts = accounts.allAccounts(),
                transactions = finance.allTransactions(),
                people = finance.allPersons(),
                categories = finance.allCategories(),
                budgets = finance.allBudgets(),
                goals = finance.allGoals(),
                contributions = finance.allGoalContributions(),
                loans = finance.allLoans(),
                payments = finance.allLoanPayments()
            )
        }
        val accountRows = snapshot.accounts
        val transactionRows = snapshot.transactions
        val peopleRows = snapshot.people
        val categoryRows = snapshot.categories
        val budgetRows = snapshot.budgets
        val goalRows = snapshot.goals
        val contributionRows = snapshot.contributions
        val loanRows = snapshot.loans
        val paymentRows = snapshot.payments

        val root = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("appVersion", "2.5.2")
            .put("createdAt", System.currentTimeMillis())

        root.put("accounts", JSONArray().apply {
            accountRows.forEach { row ->
                put(JSONObject()
                    .put("id", row.id)
                    .put("stableUid", row.stableUid)
                    .put("name", row.name)
                    .put("accountType", row.accountType)
                    .put("type", row.accountType)
                    .put("openingBalancePaise", row.openingBalancePaise)
                    .put("balance", MoneyPaise.fromSignedPaise(
                        com.charan.fintrack.data.NativeLedgerCalculator.accountBalancePaise(row, transactionRows)
                    ).toRupeesDecimal().toDouble())
                    .put("creditLimitPaise", row.creditLimitPaise)
                    .put("creditLimit", MoneyPaise.fromSignedPaise(row.creditLimitPaise).toRupeesDecimal().toDouble())
                    .put("currency", row.currency)
                    .put("institution", row.institution)
                    .put("accountNumber", row.accountNumber)
                    .put("notes", row.notes)
                    .put("isArchived", row.isArchived)
                    .put("createdAt", row.createdAt)
                    .put("updatedAt", row.updatedAt))
            }
        })
        root.put("transactions", JSONArray().apply {
            transactionRows.forEach { row ->
                put(JSONObject()
                    .put("id", row.id)
                    .put("stableUid", row.stableUid)
                    .put("accountId", row.accountId)
                    .put("type", row.type)
                    .put("amountPaise", row.amountPaise)
                    .put("amount", MoneyPaise.fromSignedPaise(row.amountPaise).toRupeesDecimal().toDouble())
                    .put("category", row.category)
                    .put("subcategory", row.subcategory)
                    .put("note", row.note)
                    .put("notes", row.note)
                    .put("timestamp", row.timestamp)
                    .put("date", localDate(row.timestamp))
                    .put("time", SimpleDateFormat("HH:mm", Locale.US).format(Date(row.timestamp)))
                    .put("personId", row.personId ?: JSONObject.NULL)
                    .put("transferAccountId", row.transferAccountId ?: JSONObject.NULL)
                    .put("toAccountId", row.transferAccountId ?: JSONObject.NULL)
                    .put("transferId", row.transferId ?: JSONObject.NULL)
                    .put("paymentMethod", row.paymentMethod)
                    .put("upiApp", row.upiApp))
            }
        })
        val peopleJson = JSONArray().apply {
            peopleRows.forEach { row ->
                put(JSONObject()
                    .put("id", row.id).put("stableUid", row.stableUid)
                    .put("name", row.name).put("phone", row.phone).put("notes", row.notes))
            }
        }
        root.put("people", peopleJson)
        root.put("persons", peopleJson)
        root.put("categories", JSONArray().apply {
            categoryRows.forEach { row ->
                put(JSONObject()
                    .put("id", row.id).put("stableUid", row.stableUid)
                    .put("name", row.name).put("type", row.type)
                    .put("parentId", row.parentId ?: JSONObject.NULL)
                    .put("icon", row.icon).put("isSystem", row.isSystem)
                    .put("subcategoriesJson", row.subcategoriesJson))
            }
        })
        root.put("budgets", JSONArray().apply {
            budgetRows.forEach { row ->
                put(JSONObject()
                    .put("id", row.id).put("stableUid", row.stableUid)
                    .put("category", row.category).put("name", row.category)
                    .put("limitAmountPaise", row.limitAmountPaise)
                    .put("amount", MoneyPaise.fromSignedPaise(row.limitAmountPaise).toRupeesDecimal().toDouble())
                    .put(
                        "spent",
                        MoneyPaise.fromSignedPaise(
                            budgetSpentPaise(row, transactionRows)
                        ).toRupeesDecimal().toDouble()
                    )
                    .put("month", row.month).put("period", row.period))
            }
        })
        root.put("goals", JSONArray().apply {
            goalRows.forEach { row ->
                put(JSONObject()
                    .put("id", row.id).put("stableUid", row.stableUid)
                    .put("name", row.name).put("targetAmountPaise", row.targetAmountPaise)
                    .put("target", MoneyPaise.fromSignedPaise(row.targetAmountPaise).toRupeesDecimal().toDouble())
                    .put("savedAmountPaise", row.savedAmountPaise)
                    .put("saved", MoneyPaise.fromSignedPaise(row.savedAmountPaise).toRupeesDecimal().toDouble())
                    .put("deadline", row.deadline ?: JSONObject.NULL))
            }
        })
        root.put("goalContributions", JSONArray().apply {
            contributionRows.forEach { row ->
                put(JSONObject()
                    .put("id", row.id).put("stableUid", row.stableUid)
                    .put("goalId", row.goalId).put("amountPaise", row.amountPaise)
                    .put("timestamp", row.timestamp).put("note", row.note))
            }
        })
        root.put("loans", JSONArray().apply {
            loanRows.forEach { row ->
                put(JSONObject()
                    .put("id", row.id).put("stableUid", row.stableUid)
                    .put("name", row.name).put("principalPaise", row.principalPaise)
                    .put("principal", MoneyPaise.fromSignedPaise(row.principalPaise).toRupeesDecimal().toDouble())
                    .put("paidAmountPaise", row.paidAmountPaise)
                    .put("paid", MoneyPaise.fromSignedPaise(row.paidAmountPaise).toRupeesDecimal().toDouble())
                    .put("monthlyPaymentPaise", row.monthlyPaymentPaise)
                    .put("monthlyPayment", MoneyPaise.fromSignedPaise(row.monthlyPaymentPaise).toRupeesDecimal().toDouble())
                    .put("emi", MoneyPaise.fromSignedPaise(row.monthlyPaymentPaise).toRupeesDecimal().toDouble())
                    .put("interestRate", row.interestRate)
                    .put("repayments", JSONArray().apply {
                        paymentRows.filter { it.loanId == row.id }.forEach { payment ->
                            put(JSONObject()
                                .put("stableUid", payment.stableUid)
                                .put("amountPaise", payment.amountPaise)
                                .put("amount", MoneyPaise.fromSignedPaise(payment.amountPaise).toRupeesDecimal().toDouble())
                                .put("timestamp", payment.timestamp)
                                .put("date", localDate(payment.timestamp))
                                .put("createdAt", payment.timestamp)
                                .put("note", payment.note))
                        }
                    }))
            }
        })
        root.put("loanPayments", JSONArray().apply {
            paymentRows.forEach { row ->
                put(JSONObject()
                    .put("id", row.id).put("stableUid", row.stableUid)
                    .put("loanId", row.loanId).put("amountPaise", row.amountPaise)
                    .put("timestamp", row.timestamp).put("note", row.note))
            }
        })
        return root.toString(2)
    }

    suspend fun exportCsv(): String {
        val rows = finance.allTransactions()
            .filterNot { it.type.equals("TRANSFER_IN", true) }
            .sortedByDescending { it.timestamp }
        val header = listOf(
            "Date", "Type", "Amount INR", "Account ID", "Destination Account ID",
            "Category", "Subcategory", "Payment Method", "UPI App", "Note"
        )
        return buildString {
            appendLine(header.joinToString(",") { csv(it) })
            rows.forEach { row ->
                appendLine(
                    listOf(
                        localDate(row.timestamp),
                        if (row.type.equals("TRANSFER_OUT", true)) "TRANSFER" else row.type,
                        MoneyPaise.fromSignedPaise(row.amountPaise).toRupeesDecimal().toPlainString(),
                        row.accountId.toString(),
                        row.transferAccountId?.toString().orEmpty(),
                        row.category,
                        row.subcategory,
                        row.paymentMethod,
                        row.upiApp,
                        row.note
                    ).joinToString(",") { csv(it) }
                )
            }
        }
    }

    suspend fun mergeJson(json: String): BackupMergeReport {
        require(json.isNotBlank()) { "Backup file is empty" }
        val root = JSONObject(json)
        val accountArray = root.optJSONArray("accounts")
            ?: throw IllegalArgumentException("Backup is missing accounts.")
        val transactionArray = root.optJSONArray("transactions")
            ?: throw IllegalArgumentException("Backup is missing transactions.")
        val nativeFormat = root.optString("format") == FORMAT
        if (nativeFormat) {
            // Never partially import an unsupported future backup schema. Older positive
            // schema versions remain eligible for backward-compatible default handling.
            val schemaVersion = root.optInt("schemaVersion", -1)
            require(schemaVersion in 1..SCHEMA_VERSION) {
                "Unsupported native backup schema version: $schemaVersion (supported: 1-$SCHEMA_VERSION)."
            }
        }

        val personArray = root.optJSONArray(if (nativeFormat) "people" else "persons") ?: JSONArray()
        val categoryArray = root.optJSONArray("categories") ?: JSONArray()
        val budgetArray = root.optJSONArray("budgets") ?: JSONArray()
        val goalArray = root.optJSONArray("goals") ?: JSONArray()
        val contributionArray = root.optJSONArray("goalContributions") ?: JSONArray()
        val loanArray = root.optJSONArray("loans") ?: JSONArray()
        val paymentArray = root.optJSONArray("loanPayments") ?: JSONArray()

        validateDuplicateSourceIds(accountArray, "account")
        validateDuplicateSourceIds(transactionArray, "transaction")
        validateDuplicateSourceIds(personArray, "person")
        validateDuplicateSourceIds(categoryArray, "category")
        validateDuplicateSourceIds(budgetArray, "budget")
        validateDuplicateSourceIds(goalArray, "goal")
        validateDuplicateSourceIds(loanArray, "loan")
        if (nativeFormat) validateNativeTransferPairs(transactionArray)

        // The legacy WebView stores current account balance and mutates it on each
        // transaction. Native accounts store opening balance and derive current balance.
        val legacyDeltas = if (nativeFormat) emptyMap() else legacyAccountDeltas(transactionArray)

        var accountsAdded = 0
        var transactionsAdded = 0
        var peopleAdded = 0
        var categoriesAdded = 0
        var budgetsAdded = 0
        var goalsAdded = 0
        var contributionsAdded = 0
        var loansAdded = 0
        var repaymentsAdded = 0
        var duplicatesSkipped = 0

        database.withTransaction {
            val accountIdMap = mutableMapOf<String, Long>()
            val personIdMap = mutableMapOf<String, Long>()
            val categoryIdMap = mutableMapOf<String, Long>()
            val categoryNameMap = mutableMapOf<String, String>()
            val goalIdMap = mutableMapOf<String, Long>()
            val loanIdMap = mutableMapOf<String, Long>()

            // 1) Accounts. For legacy HTML backups, back-calculate the opening balance.
            for (index in 0 until accountArray.length()) {
                val item = accountArray.getJSONObject(index)
                val sourceId = requireSourceId(item, "account")
                val uid = stableUid(item, "account", sourceId)
                val existing = accounts.accountByStableUid(uid)
                if (existing != null) {
                    accountIdMap[sourceId] = existing.id
                    duplicatesSkipped++
                    continue
                }
                val openingPaise = if (nativeFormat && !item.isNull("openingBalancePaise")) {
                    item.getLong("openingBalancePaise")
                } else if (nativeFormat) {
                    legacyMoney(item.opt("balance"), "account balance", signed = true)
                } else {
                    val current = legacyMoney(item.opt("balance"), "account balance", signed = true)
                    Math.subtractExact(current, legacyDeltas[sourceId] ?: 0L)
                }
                val creditLimitPaise = if (!item.isNull("creditLimitPaise")) item.getLong("creditLimitPaise")
                    else legacyMoney(item.opt("creditLimit") ?: 0, "credit limit")
                require(creditLimitPaise >= 0L) { "Credit limit cannot be negative." }
                val opening = MoneyPaise.fromSignedPaise(openingPaise)
                val credit = MoneyPaise.fromSignedPaise(creditLimitPaise)
                val id = accounts.insertAccount(
                    AccountEntity(
                        name = requiredText(item, "name", "account name"),
                        accountType = text(item, "accountType", "type").ifBlank { "wallet" },
                        balance = opening.toRupeesDecimal().toDouble(),
                        currency = text(item, "currency").ifBlank { "INR" },
                        institution = text(item, "institution"),
                        accountNumber = text(item, "accountNumber"),
                        notes = text(item, "notes"),
                        isArchived = item.optBoolean("isArchived", false),
                        createdAt = timestamp(item.opt("createdAt")),
                        updatedAt = timestamp(item.opt("updatedAt")),
                        stableUid = uid,
                        openingBalancePaise = openingPaise,
                        creditLimit = credit.toRupeesDecimal().toDouble(),
                        creditLimitPaise = creditLimitPaise
                    )
                )
                accountIdMap[sourceId] = id
                accountsAdded++
            }

            // 2) People.
            for (index in 0 until personArray.length()) {
                val item = personArray.getJSONObject(index)
                val sourceId = requireSourceId(item, "person")
                val uid = stableUid(item, "person", sourceId)
                val existing = finance.personByStableUid(uid)
                if (existing != null) {
                    personIdMap[sourceId] = existing.id
                    duplicatesSkipped++
                    continue
                }
                val id = finance.insertPerson(
                    PersonEntity(
                        name = requiredText(item, "name", "person name"),
                        phone = text(item, "phone"),
                        notes = text(item, "notes", "note"),
                        stableUid = uid
                    )
                )
                personIdMap[sourceId] = id
                peopleAdded++
            }

            // 3) Categories. Persist subcategories as JSON so they survive a round trip.
            val pendingCategoryParents = mutableListOf<Pair<Long, String>>()
            for (index in 0 until categoryArray.length()) {
                val item = categoryArray.getJSONObject(index)
                val sourceId = requireSourceId(item, "category")
                val uid = stableUid(item, "category", sourceId)
                val existing = finance.categoryByStableUid(uid)
                if (existing != null) {
                    categoryIdMap[sourceId] = existing.id
                    categoryNameMap[sourceId] = existing.name
                    duplicatesSkipped++
                    continue
                }
                val subcategories = item.optJSONArray("subcategories")
                    ?: item.optString("subcategoriesJson").takeIf { it.isNotBlank() }?.let { JSONArray(it) }
                    ?: JSONArray()
                val newId = finance.insertCategory(
                    CategoryEntity(
                        name = requiredText(item, "name", "category name"),
                        type = text(item, "type").ifBlank { "EXPENSE" }.uppercase(),
                        parentId = null,
                        icon = text(item, "icon").ifBlank { "•" },
                        isSystem = item.optBoolean("isSystem", false),
                        stableUid = uid,
                        subcategoriesJson = subcategories.toString()
                    )
                )
                categoryIdMap[sourceId] = newId
                categoryNameMap[sourceId] = item.getString("name")
                if (!item.isNull("parentId")) pendingCategoryParents += newId to item.get("parentId").toString()
                categoriesAdded++
            }
            pendingCategoryParents.forEach { (id, oldParent) ->
                val parentId = categoryIdMap[oldParent] ?: oldParent.toLongOrNull()
                if (parentId != null) {
                    val row = finance.allCategories().firstOrNull { it.id == id }
                    if (row != null) finance.updateCategory(row.copy(parentId = parentId))
                }
            }

            // 4) Budgets, goals and loans before their history records.
            for (index in 0 until budgetArray.length()) {
                val item = budgetArray.getJSONObject(index)
                val sourceId = requireSourceId(item, "budget")
                val uid = stableUid(item, "budget", sourceId)
                if (finance.budgetByStableUid(uid) != null) {
                    duplicatesSkipped++
                    continue
                }
                val limit = paise(item, "limitAmountPaise", "limitAmount", "amount", "limit")
                require(limit > 0L) { "Budget limit must be positive." }
                finance.insertBudget(
                    BudgetEntity(
                        category = text(item, "category", "name").ifBlank { "Other" },
                        limitAmount = MoneyPaise.fromSignedPaise(limit).toRupeesDecimal().toDouble(),
                        month = text(item, "month").ifBlank { localMonth(System.currentTimeMillis()) },
                        stableUid = uid,
                        limitAmountPaise = limit,
                        period = text(item, "period").ifBlank { "Monthly" }.replaceFirstChar { it.uppercase() }
                    )
                )
                budgetsAdded++
            }

            for (index in 0 until goalArray.length()) {
                val item = goalArray.getJSONObject(index)
                val sourceId = requireSourceId(item, "goal")
                val uid = stableUid(item, "goal", sourceId)
                val existing = finance.goalByStableUid(uid)
                if (existing != null) {
                    goalIdMap[sourceId] = existing.id
                    duplicatesSkipped++
                    continue
                }
                val target = paise(item, "targetAmountPaise", "targetAmount", "target")
                val saved = paise(item, "savedAmountPaise", "savedAmount", "saved", defaultValue = 0L)
                require(target > 0L && saved in 0..target) { "Invalid savings goal amounts." }
                val id = finance.insertGoal(
                    GoalEntity(
                        name = requiredText(item, "name", "goal name"),
                        targetAmount = MoneyPaise.fromSignedPaise(target).toRupeesDecimal().toDouble(),
                        savedAmount = MoneyPaise.fromSignedPaise(saved).toRupeesDecimal().toDouble(),
                        deadline = nullableTimestamp(item.opt("deadline")),
                        stableUid = uid,
                        targetAmountPaise = target,
                        savedAmountPaise = saved
                    )
                )
                goalIdMap[sourceId] = id
                goalsAdded++
            }

            for (index in 0 until loanArray.length()) {
                val item = loanArray.getJSONObject(index)
                val sourceId = requireSourceId(item, "loan")
                val uid = stableUid(item, "loan", sourceId)
                val existing = finance.loanByStableUid(uid)
                if (existing != null) {
                    loanIdMap[sourceId] = existing.id
                    duplicatesSkipped++
                    continue
                }
                val principal = paise(item, "principalPaise", "principal")
                val paid = paise(item, "paidAmountPaise", "paidAmount", "paid", defaultValue = 0L)
                val monthly = paise(item, "monthlyPaymentPaise", "monthlyPayment", "emi", defaultValue = 0L)
                require(principal > 0L && paid in 0..principal && monthly >= 0L) { "Invalid loan amounts." }
                val id = finance.insertLoan(
                    LoanEntity(
                        name = requiredText(item, "name", "loan name"),
                        principal = MoneyPaise.fromSignedPaise(principal).toRupeesDecimal().toDouble(),
                        paidAmount = MoneyPaise.fromSignedPaise(paid).toRupeesDecimal().toDouble(),
                        monthlyPayment = MoneyPaise.fromSignedPaise(monthly).toRupeesDecimal().toDouble(),
                        interestRate = item.optDouble("interestRate", 0.0),
                        stableUid = uid,
                        principalPaise = principal,
                        paidAmountPaise = paid,
                        monthlyPaymentPaise = monthly
                    )
                )
                loanIdMap[sourceId] = id
                loansAdded++
            }

            // 5) Transactions and paired transfer records.
            for (index in 0 until transactionArray.length()) {
                val item = transactionArray.getJSONObject(index)
                val sourceId = requireSourceId(item, "transaction")
                val rawType = requiredText(item, "type", "transaction type").uppercase()
                val amount = paise(item, "amountPaise", "amount")
                require(amount > 0L) { "Transaction amounts must be positive." }
                val oldAccountId = requireReference(item, "accountId", "transaction account")
                val accountId = accountIdMap[oldAccountId]
                    ?: throw IllegalArgumentException("Transaction references an account not included in the backup.")
                val oldPersonId = if (item.isNull("personId") || !item.has("personId")) null else item.get("personId").toString()
                val personId = if (rawType == "BORROW" || rawType == "LEND") {
                    oldPersonId?.let {
                        personIdMap[it] ?: throw IllegalArgumentException("Transaction references a missing person.")
                    }
                } else null
                val categoryId = text(item, "category")
                val categoryName = categoryNameMap[categoryId] ?: categoryId.ifBlank { "Other" }
                val note = text(item, "note", "notes")
                val date = timestampFromTransaction(item)

                if (rawType == "TRANSFER") {
                    val oldDestination = requireReference(item, "toAccountId", "transfer destination")
                    val destination = accountIdMap[oldDestination]
                        ?: throw IllegalArgumentException("Transfer references a destination account not included in the backup.")
                    require(destination != accountId) { "Transfer source and destination must differ." }
                    val transferUid = "legacy-transfer:$sourceId"
                    val outUid = "legacy:transaction:$sourceId:out"
                    val inUid = "legacy:transaction:$sourceId:in"
                    if (finance.transactionByStableUid(outUid) == null) {
                        finance.insertTransaction(
                            TransactionEntity(
                                accountId = accountId, type = "TRANSFER_OUT",
                                amount = MoneyPaise.fromSignedPaise(amount).toRupeesDecimal().toDouble(),
                                amountPaise = amount, category = "Transfer", note = note,
                                timestamp = date, transferAccountId = destination, transferId = transferUid,
                                stableUid = outUid
                            )
                        )
                        transactionsAdded++
                    } else duplicatesSkipped++
                    if (finance.transactionByStableUid(inUid) == null) {
                        finance.insertTransaction(
                            TransactionEntity(
                                accountId = destination, type = "TRANSFER_IN",
                                amount = MoneyPaise.fromSignedPaise(amount).toRupeesDecimal().toDouble(),
                                amountPaise = amount, category = "Transfer", note = note,
                                timestamp = date, transferAccountId = accountId, transferId = transferUid,
                                stableUid = inUid
                            )
                        )
                        transactionsAdded++
                    } else duplicatesSkipped++
                } else {
                    val type = when (rawType) {
                        "INCOME" -> "INCOME"
                        "EXPENSE" -> "EXPENSE"
                        "BORROW" -> "BORROW"
                        "LEND" -> "LEND"
                        "TRANSFER_OUT", "TRANSFER_IN" -> rawType
                        else -> throw IllegalArgumentException("Unsupported transaction type: $rawType")
                    }
                    if (type == "BORROW" || type == "LEND") {
                        require(personId != null) { "Borrowed/lent transaction is missing its person." }
                    }
                    val uid = stableUid(item, "transaction", sourceId)
                    if (finance.transactionByStableUid(uid) != null) {
                        duplicatesSkipped++
                        continue
                    }
                    val transferAccount = if (type.startsWith("TRANSFER")) {
                        val oldTransferAccount = requireReference(item, "transferAccountId", "transfer")
                        accountIdMap[oldTransferAccount]
                            ?: throw IllegalArgumentException("Transfer references an account not included in the backup.")
                    } else null
                    val transferUid = if (type.startsWith("TRANSFER")) {
                        requiredText(item, "transferId", "transfer ID")
                    } else null
                    finance.insertTransaction(
                        TransactionEntity(
                            accountId = accountId,
                            type = type,
                            amount = MoneyPaise.fromSignedPaise(amount).toRupeesDecimal().toDouble(),
                            amountPaise = amount,
                            category = if (type.startsWith("TRANSFER")) "Transfer" else categoryName,
                            note = note,
                            timestamp = date,
                            transferAccountId = transferAccount,
                            transferId = transferUid,
                            subcategory = text(item, "subcategory"),
                            personId = personId,
                            stableUid = uid,
                            paymentMethod = text(item, "paymentMethod"),
                            upiApp = text(item, "upiApp")
                        )
                    )
                    transactionsAdded++
                }
            }

            // 6) Preserve individual savings contributions, including old nested arrays.
            val seenContributionSources = mutableSetOf<String>()
            for (goalIndex in 0 until goalArray.length()) {
                val goalObject = goalArray.getJSONObject(goalIndex)
                val oldGoalId = requireSourceId(goalObject, "goal")
                val localGoalId = goalIdMap[oldGoalId]
                    ?: finance.goalByStableUid(stableUid(goalObject, "goal", oldGoalId))?.id
                    ?: continue
                val items = goalObject.optJSONArray("contributions") ?: JSONArray()
                for (itemIndex in 0 until items.length()) {
                    val contribution = items.getJSONObject(itemIndex)
                    val sourceKey = "$oldGoalId:$itemIndex"
                    if (!seenContributionSources.add(sourceKey)) continue
                    val uid = stableUid(contribution, "goal_contribution", "$oldGoalId:$itemIndex")
                    if (finance.goalContributionByStableUid(uid) != null) {
                        duplicatesSkipped++
                        continue
                    }
                    val amount = paise(contribution, "amountPaise", "amount")
                    require(amount > 0L) { "Savings contribution must be positive." }
                    finance.insertGoalContribution(
                        GoalContributionEntity(
                            goalId = localGoalId,
                            amount = MoneyPaise.fromSignedPaise(amount).toRupeesDecimal().toDouble(),
                            amountPaise = amount,
                            timestamp = timestampFromTransaction(contribution),
                            note = text(contribution, "note"),
                            stableUid = uid
                        )
                    )
                    contributionsAdded++
                }
            }
            for (index in 0 until contributionArray.length()) {
                val item = contributionArray.getJSONObject(index)
                val oldGoal = requireReference(item, "goalId", "goal contribution")
                val localGoalId = goalIdMap[oldGoal]
                    ?: throw IllegalArgumentException("Contribution references a missing goal.")
                val sourceId = text(item, "id").ifBlank { "$oldGoal:$index" }
                val uid = stableUid(item, "goal_contribution", sourceId)
                if (finance.goalContributionByStableUid(uid) != null) {
                    duplicatesSkipped++
                    continue
                }
                val amount = paise(item, "amountPaise", "amount")
                require(amount > 0L) { "Savings contribution must be positive." }
                finance.insertGoalContribution(
                    GoalContributionEntity(
                        goalId = localGoalId,
                        amount = MoneyPaise.fromSignedPaise(amount).toRupeesDecimal().toDouble(),
                        amountPaise = amount,
                        timestamp = timestampFromTransaction(item),
                        note = text(item, "note"),
                        stableUid = uid
                    )
                )
                contributionsAdded++
            }

            // 7) Preserve repayment history without creating cash-ledger transactions.
            for (loanIndex in 0 until loanArray.length()) {
                val loanObject = loanArray.getJSONObject(loanIndex)
                val oldLoanId = requireSourceId(loanObject, "loan")
                val localLoanId = loanIdMap[oldLoanId]
                    ?: finance.loanByStableUid(stableUid(loanObject, "loan", oldLoanId))?.id
                    ?: continue
                val repayments = loanObject.optJSONArray("repayments") ?: JSONArray()
                for (paymentIndex in 0 until repayments.length()) {
                    val payment = repayments.getJSONObject(paymentIndex)
                    val uid = stableUid(payment, "loan_payment", "$oldLoanId:$paymentIndex")
                    if (finance.loanPaymentByStableUid(uid) != null) {
                        duplicatesSkipped++
                        continue
                    }
                    val amount = paise(payment, "amountPaise", "amount")
                    require(amount > 0L) { "Loan repayment must be positive." }
                    finance.insertLoanPayment(
                        LoanPaymentEntity(
                            loanId = localLoanId,
                            amount = MoneyPaise.fromSignedPaise(amount).toRupeesDecimal().toDouble(),
                            amountPaise = amount,
                            timestamp = timestampFromTransaction(payment),
                            note = text(payment, "note"),
                            stableUid = uid
                        )
                    )
                    repaymentsAdded++
                }
            }
            for (index in 0 until paymentArray.length()) {
                val item = paymentArray.getJSONObject(index)
                val oldLoan = requireReference(item, "loanId", "loan payment")
                val localLoanId = loanIdMap[oldLoan]
                    ?: throw IllegalArgumentException("Repayment references a missing loan.")
                val sourceId = text(item, "id").ifBlank { "$oldLoan:$index" }
                val uid = stableUid(item, "loan_payment", sourceId)
                if (finance.loanPaymentByStableUid(uid) != null) {
                    duplicatesSkipped++
                    continue
                }
                val amount = paise(item, "amountPaise", "amount")
                require(amount > 0L) { "Loan repayment must be positive." }
                finance.insertLoanPayment(
                    LoanPaymentEntity(
                        loanId = localLoanId,
                        amount = MoneyPaise.fromSignedPaise(amount).toRupeesDecimal().toDouble(),
                        amountPaise = amount,
                        timestamp = timestampFromTransaction(item),
                        note = text(item, "note"),
                        stableUid = uid
                    )
                )
                repaymentsAdded++
            }
        }

        return BackupMergeReport(
            accountsAdded, transactionsAdded, peopleAdded, categoriesAdded,
            budgetsAdded, goalsAdded, contributionsAdded, loansAdded, repaymentsAdded,
            duplicatesSkipped
        )
    }

    /**
     * Native backups store transfers as a matched TRANSFER_OUT / TRANSFER_IN pair.
     * Reject incomplete or inconsistent pairs before writing anything to Room:
     * importing only one side would silently change the user total balance.
     */
    private fun validateNativeTransferPairs(transactions: JSONArray) {
        val groups = mutableMapOf<String, MutableList<JSONObject>>()
        for (index in 0 until transactions.length()) {
            val item = transactions.getJSONObject(index)
            val type = requiredText(item, "type", "transaction type").uppercase()
            if (type != "TRANSFER_OUT" && type != "TRANSFER_IN") continue
            val transferId = requiredText(item, "transferId", "transfer ID")
            groups.getOrPut(transferId) { mutableListOf() }.add(item)
        }

        groups.forEach { (transferId, pair) ->
            require(pair.size == 2) {
                "Transfer $transferId is incomplete. A backup must contain both transfer records."
            }
            val outgoing = pair.singleOrNull { text(it, "type").equals("TRANSFER_OUT", true) }
                ?: throw IllegalArgumentException("Transfer $transferId must contain one outgoing record.")
            val incoming = pair.singleOrNull { text(it, "type").equals("TRANSFER_IN", true) }
                ?: throw IllegalArgumentException("Transfer $transferId must contain one incoming record.")
            val outAmount = paise(outgoing, "amountPaise", "amount")
            val inAmount = paise(incoming, "amountPaise", "amount")
            require(outAmount > 0L && outAmount == inAmount) {
                "Transfer $transferId has mismatched or invalid amounts."
            }
            val outAccount = requireReference(outgoing, "accountId", "transfer")
            val outDestination = requireReference(outgoing, "transferAccountId", "transfer")
            val inAccount = requireReference(incoming, "accountId", "transfer")
            val inDestination = requireReference(incoming, "transferAccountId", "transfer")
            require(outAccount == inDestination && outDestination == inAccount && outAccount != outDestination) {
                "Transfer $transferId has inconsistent source and destination accounts."
            }
        }
    }

    /**
     * Match the budget progress window used by the native Budgets screen so XLSX
     * exports show the same calculated spend instead of an empty derived column.
     */
    private fun budgetSpentPaise(budget: BudgetEntity, transactions: List<TransactionEntity>): Long {
        val (start, end) = budgetWindow(budget)
        return NativeLedgerCalculator.categoryExpensePaise(
            transactions, budget.category, start, end
        )
    }

    private fun budgetWindow(budget: BudgetEntity): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        when (budget.period.lowercase(Locale.ROOT)) {
            "daily" -> {
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_YEAR, 1)
            }
            "weekly" -> {
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
                val dayFromMonday = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7
                start.add(Calendar.DAY_OF_YEAR, -dayFromMonday)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_YEAR, 7)
            }
            "yearly" -> {
                start.set(Calendar.DAY_OF_YEAR, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.YEAR, 1)
            }
            else -> {
                val parts = budget.month.split("-")
                val year = parts.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.YEAR)
                val month = (parts.getOrNull(1)?.toIntOrNull()?.minus(1))
                    ?.takeIf { it in 0..11 } ?: now.get(Calendar.MONTH)
                start.set(year, month, 1, 0, 0, 0)
                start.set(Calendar.MILLISECOND, 0)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.MONTH, 1)
            }
        }
        return start.timeInMillis to end.timeInMillis
    }

    private fun legacyAccountDeltas(transactions: JSONArray): Map<String, Long> {
        val deltas = mutableMapOf<String, Long>()
        fun add(id: String, amount: Long) {
            deltas[id] = Math.addExact(deltas[id] ?: 0L, amount)
        }
        for (index in 0 until transactions.length()) {
            val item = transactions.getJSONObject(index)
            val type = requiredText(item, "type", "transaction type").uppercase()
            val amount = paise(item, "amountPaise", "amount")
            val accountId = requireReference(item, "accountId", "transaction")
            when (type) {
                "INCOME", "BORROW" -> add(accountId, amount)
                "EXPENSE", "LEND" -> add(accountId, Math.negateExact(amount))
                "TRANSFER" -> {
                    add(accountId, Math.negateExact(amount))
                    add(requireReference(item, "toAccountId", "transfer"), amount)
                }
                "TRANSFER_OUT" -> add(accountId, Math.negateExact(amount))
                "TRANSFER_IN" -> add(accountId, amount)
                else -> throw IllegalArgumentException("Unsupported transaction type: $type")
            }
        }
        return deltas
    }

    private fun validateDuplicateSourceIds(array: JSONArray, label: String) {
        val seen = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = requireSourceId(item, label)
            require(seen.add(id)) { "Backup contains duplicate $label ID: $id" }
        }
    }

    private fun requireSourceId(item: JSONObject, label: String): String {
        val id = text(item, "id", "stableUid")
        require(id.isNotBlank()) { "Backup contains a $label without an ID." }
        return id
    }

    private fun requireReference(item: JSONObject, key: String, label: String): String {
        require(item.has(key) && !item.isNull(key)) { "$label is missing $key." }
        val id = item.get(key).toString()
        require(id.isNotBlank() && id != "0") { "$label has an invalid $key." }
        return id
    }

    private fun stableUid(item: JSONObject, type: String, sourceId: String): String =
        text(item, "stableUid").ifBlank { "legacy:$type:$sourceId" }

    private fun requiredText(item: JSONObject, key: String, label: String): String =
        text(item, key).trim().also { require(it.isNotEmpty()) { "Backup is missing $label." } }

    private fun text(item: JSONObject, vararg keys: String): String {
        for (key in keys) {
            if (!item.has(key) || item.isNull(key)) continue
            val value = item.opt(key)
            if (value != null && value != JSONObject.NULL) return value.toString()
        }
        return ""
    }

    private fun paise(
        item: JSONObject,
        paiseKey: String,
        vararg rupeeKeys: String,
        defaultValue: Long? = null
    ): Long {
        if (item.has(paiseKey) && !item.isNull(paiseKey)) {
            val value = item.get(paiseKey)
            return try {
                BigDecimal(value.toString()).longValueExact()
            } catch (_: NumberFormatException) {
                throw IllegalArgumentException("Invalid $paiseKey value. Paise must be a whole number.")
            } catch (_: ArithmeticException) {
                throw IllegalArgumentException("Invalid $paiseKey value. Paise must be a whole number within the supported range.")
            }
        }
        for (key in rupeeKeys) {
            if (item.has(key) && !item.isNull(key)) return legacyMoney(item.opt(key), key)
        }
        return defaultValue ?: throw IllegalArgumentException("Backup is missing $paiseKey.")
    }

    private fun legacyMoney(value: Any?, label: String, signed: Boolean = false): Long {
        require(value != null && value != JSONObject.NULL) { "Backup is missing $label." }
        val decimal = try {
            BigDecimal(value.toString())
        } catch (_: NumberFormatException) {
            throw IllegalArgumentException("Invalid $label value.")
        }
        if (!signed) require(decimal.signum() >= 0) { "$label cannot be negative." }
        return decimal.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }

    private fun timestamp(value: Any?): Long {
        if (value == null || value == JSONObject.NULL) return System.currentTimeMillis()
        if (value is Number) {
            return try { BigDecimal(value.toString()).longValueExact() }
            catch (_: Exception) { throw IllegalArgumentException("Backup contains an invalid timestamp.") }
        }
        val raw = value.toString().trim()
        if (raw.isBlank()) return System.currentTimeMillis()
        raw.toLongOrNull()?.let { return it }
        val isoFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )
        for (pattern in isoFormats) {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                if (pattern.endsWith("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
            }
            val position = ParsePosition(0)
            val parsed = format.parse(raw, position)
            if (parsed != null && position.index == raw.length) return parsed.time
        }
        throw IllegalArgumentException("Backup contains an invalid timestamp: $raw")
    }

    private fun nullableTimestamp(value: Any?): Long? =
        if (value == null || value == JSONObject.NULL || value.toString().isBlank()) null else timestamp(value)

    private fun timestampFromTransaction(item: JSONObject): Long {
        // Native backups include the original epoch timestamp. Prefer it over the
        // display date/time strings so restores preserve seconds, milliseconds and
        // the source timezone rather than interpreting the same local time again.
        val exactTimestamp = item.opt("timestamp")
        if (exactTimestamp != null && exactTimestamp != JSONObject.NULL) {
            return timestamp(exactTimestamp)
        }
        val date = text(item, "date")
        val time = text(item, "time").ifBlank { "12:00" }
        if (date.isNotBlank()) {
            val input = "$date $time"
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
            val position = ParsePosition(0)
            val parsed = format.parse(input, position)
            if (parsed != null && position.index == input.length) return parsed.time
            throw IllegalArgumentException("Backup contains an invalid transaction date or time: $input")
        }
        return timestamp(item.opt("timestamp") ?: item.opt("createdAt"))
    }

    private fun localDate(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

    private fun localMonth(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(timestamp))

    private fun csv(value: String): String {
        // Prevent spreadsheet applications from evaluating user-controlled text as formulas.
        val firstMeaningful = value.trimStart().firstOrNull()
        val beginsWithFormulaMarker = firstMeaningful != null &&
            (firstMeaningful == '=' || firstMeaningful == '+' ||
                firstMeaningful == '-' || firstMeaningful == '@')
        val safe = if (beginsWithFormulaMarker) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
