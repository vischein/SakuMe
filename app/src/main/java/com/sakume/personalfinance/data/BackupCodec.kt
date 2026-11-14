package com.sakume.personalfinance.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.UUID

@Serializable data class BackupData(
    val accounts: List<AccountEntity>,
    val transactions: List<TransactionEntity>,
    val bills: List<BillEntity>,
    val investments: List<InvestmentEntity>,
    val contributions: List<InvestmentContributionEntity>,
    val debts: List<DebtEntity>,
    val schedules: List<ScheduledInvestmentEntity> = emptyList(),
    val salaries: List<SalaryScheduleEntity> = emptyList(),
    val holdings: List<InvestmentHoldingEntity> = emptyList(),
    val meta: BackupMeta? = null,
    val settings: BackupSettings? = null
)

@Serializable data class BackupMeta(
    val format: Int = BackupCodec.FORMAT_VERSION,
    val createdAtMillis: Long = 0L,
    val appVersion: String = "",
    val source: String = "manual"
)

@Serializable data class BackupSettings(
    val trailingZeros: Int? = null,
    val notificationsEnabled: Boolean? = null,
    val maxCatchupDays: Int? = null,
    val billReminderEnabled: Boolean? = null,
    val billReminderHour: Int? = null,
    val billReminderEarlyDays: Int? = null,
    val autoBackupEnabled: Boolean? = null,
    val autoBackupKeep: Int? = null
)
object BackupCodec {
    const val FORMAT_VERSION = 2
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(data: BackupData): String = json.encodeToString(data)
    fun decode(text: String): BackupData = json.decodeFromString(text)

    fun isLegacySakuMe(text: String): Boolean = try {
        val obj = Json.parseToJsonElement(text).jsonObject
        obj.containsKey("appId") || obj.containsKey("savingGoals")
    } catch (e: Exception) { false }

    fun decodeLegacySakuMe(text: String): BackupData {
        val root = Json.parseToJsonElement(text).jsonObject
        fun arr(key: String) = root[key]?.jsonArray.orEmpty()

        val accountIdMap = HashMap<Long, String>()
        val investmentIdMap = HashMap<Long, String>()

        fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.content ?: ""
        fun JsonObject.longOf(key: String): Long = this[key]?.jsonPrimitive?.longOrNull ?: 0L
        fun JsonObject.longOrNull(key: String): Long? = this[key]?.let { if (it is JsonNull) null else it.jsonPrimitive.longOrNull }
        fun JsonObject.boolOf(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: false

        val accounts = arr("accounts").map { it.jsonObject }.map { a ->
            val newId = UUID.randomUUID().toString()
            accountIdMap[a.longOf("id")] = newId
            AccountEntity(id = newId, name = a.str("name"), type = a.str("type"), balance = a.longOf("balance"), openingBalance = a.longOf("balance"))
        }

        val savingGoals = arr("savingGoals").map { it.jsonObject }.map { g ->
            val newId = UUID.randomUUID().toString()
            investmentIdMap[g.longOf("id")] = newId
            InvestmentEntity(
                id = newId, name = g.str("name"),
                target = g.longOrNull("targetAmount")?.takeIf { it > 0 },
                value = g.longOf("currentAmount"),
                archived = g.boolOf("isArchived")
            )
        }

        val transactions = mutableListOf<TransactionEntity>()
        arr("transactions").map { it.jsonObject }.forEach { t ->
            val oldType = t.str("type")
            val fromAcc = accountIdMap[t.longOf("accountId")] ?: return@forEach
            val amount = t.longOf("amount")
            val category = t.str("category")
            val note = t.str("note")
            val date = t.longOf("date")
            val savingGoalId = t.longOrNull("savingGoalId")?.let { investmentIdMap[it] }
            when (oldType) {
                "TRANSFER" -> {
                    val toAcc = t.longOrNull("toAccountId")?.let { accountIdMap[it] }
                    if (toAcc != null) {
                        val group = UUID.randomUUID().toString()
                        transactions += TransactionEntity(UUID.randomUUID().toString(), "transfer_out", amount, category, fromAcc, date, note, transferGroupId = group)
                        transactions += TransactionEntity(UUID.randomUUID().toString(), "transfer_in", amount, category, toAcc, date, note, transferGroupId = group)
                    }
                }
                "INCOME" -> transactions += TransactionEntity(UUID.randomUUID().toString(), "income", amount, category, fromAcc, date, note, investmentAssetId = savingGoalId)
                "EXPENSE" -> transactions += TransactionEntity(UUID.randomUUID().toString(), "expense", amount, category, fromAcc, date, note, investmentAssetId = savingGoalId)
            }
        }

        val billParentMap = HashMap<Long, String>()
        val bills = arr("bills").map { it.jsonObject }.map { b ->
            val tenor = b.longOf("tenorMonths").toInt().takeIf { it > 0 } ?: 1
            val parentId = if (tenor > 1) billParentMap.getOrPut(b.longOf("id")) { UUID.randomUUID().toString() } else null
            BillEntity(
                id = UUID.randomUUID().toString(), provider = b.str("provider"), itemName = b.str("itemName"),
                amount = b.longOf("amount"), dueDateMillis = b.longOf("dueDate"),
                tenorMonths = tenor, installmentIndex = b.longOf("installmentIndex").toInt().takeIf { it > 0 } ?: 1,
                isPaid = b.boolOf("isPaid"),
                paidAccountId = b.longOrNull("paidAccountId")?.let { accountIdMap[it] },
                parentBillId = parentId,
                paidAtMillis = if (b.boolOf("isPaid")) b.longOrNull("updatedAt") else null
            )
        }

        val debts = arr("debts").map { it.jsonObject }.map { d ->
            DebtEntity(
                id = UUID.randomUUID().toString(),
                type = if (d.boolOf("isBorrowed")) "utang" else "piutang",
                person = d.str("personName"), description = d.str("note"), amount = d.longOf("amount"),
                isPaid = d.boolOf("isPaid"),
                createdAtMillis = d.longOf("lentDate"),
                paidAtMillis = d.longOrNull("paidDate")
            )
        }

        val netByAccount = transactions.groupBy { it.accountId }.mapValues { (_, items) ->
            items.sumOf { tx ->
                when (tx.type) {
                    "income", "transfer_in" -> tx.amount
                    "expense", "transfer_out" -> -tx.amount
                    else -> 0L
                }
            }
        }
        val normalizedAccounts = accounts.map {
            it.copy(openingBalance = it.balance - (netByAccount[it.id] ?: 0L))
        }
        return BackupData(normalizedAccounts, transactions, bills, savingGoals, emptyList(), debts)
    }
}
