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
    val sortOrder: Int = 0
)

@Serializable
@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index("transferGroupId"), Index("investmentAssetId")]
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

    val debtId: String? = null
)

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
    val archived: Boolean = false
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

    val paidAmount: Long = 0
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
