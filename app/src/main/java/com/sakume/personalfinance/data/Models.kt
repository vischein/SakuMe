package com.sakume.personalfinance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String = "Cash",
    val balance: Long = 0,

    val openingBalance: Long = 0,
    val sortOrder: Int = 0,
    val creditLimit: Long? = null
) {
    val isCredit: Boolean get() = type.equals(ACCOUNT_TYPE_CREDIT, ignoreCase = true)

    val owed: Long get() = if (isCredit) (-balance).coerceAtLeast(0L) else 0L

    val availableCredit: Long?
        get() {
            val limit = creditLimit
            return if (isCredit && limit != null) (limit - owed).coerceAtLeast(0L) else null
        }

    fun canHold(newBalance: Long): Boolean {
        if (!isCredit) return newBalance >= 0L
        val limit = creditLimit ?: return true
        return newBalance >= -limit
    }
}

const val ACCOUNT_TYPE_CREDIT = "Kredit"
val ACCOUNT_TYPES = listOf("Cash", "E-Wallet", "Bank", ACCOUNT_TYPE_CREDIT)

fun accountTypeLabel(type: String): String =
    if (type.equals(ACCOUNT_TYPE_CREDIT, ignoreCase = true)) "Kredit/PayLater" else type

fun List<AccountEntity>.netWorth(): Long = sumOf { it.balance }

fun List<AccountEntity>.creditOwedTotal(): Long = sumOf { it.owed }

@Serializable
@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index("transferGroupId"), Index("investmentAssetId"), Index("feeAssetId"), Index("categoryId")]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: Long,
    val category: String,
    val accountId: String,
    val dateMillis: Long,
    val note: String = "",
    val investmentAssetId: String? = null,
    val transferGroupId: String? = null,

    val debtId: String? = null,

    val feeAssetId: String? = null,

    val categoryId: String = ""
)

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val icon: String = "category",
    val color: Long = 0xFF6B7280,
    val sortOrder: Int = 0,
    val isSystem: Boolean = false
) {
    fun accepts(txType: String): Boolean = when (txType) {
        "income" -> kind == CATEGORY_KIND_INCOME || kind == CATEGORY_KIND_BOTH
        "expense" -> kind == CATEGORY_KIND_EXPENSE || kind == CATEGORY_KIND_BOTH
        "transfer_in", "transfer_out" -> kind == CATEGORY_KIND_TRANSFER
        else -> false
    }
}

const val CATEGORY_KIND_EXPENSE = "expense"
const val CATEGORY_KIND_INCOME = "income"
const val CATEGORY_KIND_BOTH = "both"
const val CATEGORY_KIND_TRANSFER = "transfer"

@Serializable
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val itemName: String,
    val amount: Long,
    val dueDateMillis: Long,
    val tenorMonths: Int = 1,
    val installmentIndex: Int = 1,
    val isPaid: Boolean = false,
    val paidAccountId: String? = null,
    val parentBillId: String? = null,
    val paidAtMillis: Long? = null,

    val paymentTransactionId: String? = null
)

@Serializable
@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val target: Long? = null,
    val value: Long = 0,
    val archived: Boolean = false,

    val kind: String = KIND_PLATFORM,

    val deadlineMillis: Long? = null,

    val category: String = ""
) {
    val isGoal: Boolean get() = kind == KIND_GOAL
}

const val KIND_PLATFORM = "platform"
const val KIND_GOAL = "goal"
val GOAL_CATEGORIES = listOf("Darurat", "Liburan", "Gadget", "Kendaraan", "Rumah", "Pendidikan", "Lainnya")

@Serializable
@Entity(
    tableName = "investment_holdings",
    indices = [Index("platformId")]
)
data class InvestmentHoldingEntity(
    @PrimaryKey val id: String,
    val platformId: String,
    val name: String,
    val type: String,
    val cost: Long,
    val createdAtMillis: Long
)

@Serializable
@Entity(
    tableName = "investment_contributions",
    indices = [Index("assetId"), Index("accountId"), Index("transactionId")]
)
data class InvestmentContributionEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val accountId: String,
    val amount: Long,
    val transactionId: String,
    val createdAtMillis: Long
)

@Serializable
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String,
    val type: String,
    val person: String,
    val description: String,
    val amount: Long,
    val isPaid: Boolean = false,
    val createdAtMillis: Long,
    val paidAtMillis: Long? = null,

    val paidAccountId: String? = null,

    val paymentTransactionId: String? = null,

    val paidAmount: Long = 0,

    val dueDateMillis: Long? = null
) {
    val remaining: Long get() = (amount - paidAmount).coerceAtLeast(0L)
}

@Serializable
@Entity(
    tableName = "scheduled_investments",
    indices = [Index("assetId"), Index("accountId")]
)
data class ScheduledInvestmentEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val accountId: String,
    val amount: Long,

    val dayOfWeek: Int,
    val enabled: Boolean = true,
    val lastRunMillis: Long? = null,
    val createdAtMillis: Long = 0
)

@Serializable
@Entity(
    tableName = "salary_schedules",
    indices = [Index("accountId")]
)
data class SalaryScheduleEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amount: Long,

    val frequency: String,
    val enabled: Boolean = true,
    val lastRunMillis: Long? = null,
    val note: String = "Gaji",
    val createdAtMillis: Long = 0,

    val hourOfDay: Int = 9
)
