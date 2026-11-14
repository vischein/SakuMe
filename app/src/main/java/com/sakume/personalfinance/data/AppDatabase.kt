package com.sakume.personalfinance.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, name ASC")
    fun accounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, name ASC")
    suspend fun allAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun account(id: String): AccountEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM accounts")
    suspend fun maxAccountSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAccount(item: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAccounts(items: List<AccountEntity>)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("SELECT * FROM transactions WHERE accountId = :accountId LIMIT 1")
    suspend fun firstTransactionForAccount(accountId: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: String)

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun transactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    suspend fun allTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTransaction(item: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTransactions(items: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun transaction(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transferGroupId = :groupId")
    suspend fun transactionsByGroup(groupId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE investmentAssetId = :assetId")
    suspend fun transactionsForInvestment(assetId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE feeAssetId = :assetId")
    suspend fun feeTransactionsForInvestment(assetId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE transferGroupId = :groupId ORDER BY dateMillis ASC")
    suspend fun allTransactionsByGroup(groupId: String): List<TransactionEntity>

    @Query("SELECT * FROM bills ORDER BY isPaid ASC, dueDateMillis ASC")
    fun bills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills")
    suspend fun allBills(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun bill(id: String): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putBill(item: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putBills(items: List<BillEntity>)

    @Query("SELECT * FROM bills WHERE id = :id OR parentBillId = :id ORDER BY installmentIndex ASC")
    suspend fun billFamily(id: String): List<BillEntity>

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBill(id: String)

    @Query("SELECT * FROM investments ORDER BY archived ASC, name")
    fun investments(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments")
    suspend fun allInvestments(): List<InvestmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putInvestment(item: InvestmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putInvestments(items: List<InvestmentEntity>)

    @Query("SELECT * FROM investment_holdings ORDER BY createdAtMillis ASC")
    fun holdings(): Flow<List<InvestmentHoldingEntity>>

    @Query("SELECT * FROM investment_holdings")
    suspend fun allHoldings(): List<InvestmentHoldingEntity>

    @Query("SELECT * FROM investment_holdings WHERE platformId = :platformId")
    suspend fun holdingsForPlatform(platformId: String): List<InvestmentHoldingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putHolding(item: InvestmentHoldingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putHoldings(items: List<InvestmentHoldingEntity>)

    @Query("DELETE FROM investment_holdings WHERE id = :id")
    suspend fun deleteHolding(id: String)

    @Query("DELETE FROM investment_holdings WHERE platformId = :platformId")
    suspend fun deleteHoldingsForPlatform(platformId: String)

    @Query("DELETE FROM investment_holdings")
    suspend fun clearHoldings()

    @Query("SELECT * FROM investment_contributions WHERE assetId = :assetId")
    suspend fun contributions(assetId: String): List<InvestmentContributionEntity>

    @Query("SELECT * FROM investment_contributions")
    suspend fun allContributions(): List<InvestmentContributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putContribution(item: InvestmentContributionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putContributions(items: List<InvestmentContributionEntity>)

    @Query("DELETE FROM investment_contributions WHERE assetId = :assetId")
    suspend fun deleteContributions(assetId: String)

    @Query("DELETE FROM investment_contributions WHERE id = :id")
    suspend fun deleteContribution(id: String)

    @Query("SELECT * FROM debts ORDER BY isPaid ASC, createdAtMillis DESC")
    fun debts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts")
    suspend fun allDebts(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putDebt(item: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putDebts(items: List<DebtEntity>)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebt(id: String)

    @Query("SELECT * FROM scheduled_investments ORDER BY createdAtMillis DESC")
    fun schedules(): Flow<List<ScheduledInvestmentEntity>>

    @Query("SELECT * FROM scheduled_investments")
    suspend fun allSchedules(): List<ScheduledInvestmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSchedule(item: ScheduledInvestmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSchedules(items: List<ScheduledInvestmentEntity>)

    @Query("DELETE FROM scheduled_investments WHERE id = :id")
    suspend fun deleteSchedule(id: String)

    @Query("DELETE FROM scheduled_investments WHERE assetId = :assetId")
    suspend fun deleteSchedulesForAsset(assetId: String)

    @Query("SELECT * FROM salary_schedules ORDER BY createdAtMillis DESC")
    fun salaries(): Flow<List<SalaryScheduleEntity>>

    @Query("SELECT * FROM salary_schedules")
    suspend fun allSalaries(): List<SalaryScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSalary(item: SalaryScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSalaries(items: List<SalaryScheduleEntity>)

    @Query("DELETE FROM salary_schedules WHERE id = :id")
    suspend fun deleteSalary(id: String)

    @Query("DELETE FROM salary_schedules")
    suspend fun clearSalaries()

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM bills")
    suspend fun clearBills()

    @Query("DELETE FROM investments")
    suspend fun clearInvestments()

    @Query("DELETE FROM investment_contributions")
    suspend fun clearContributions()

    @Query("DELETE FROM debts")
    suspend fun clearDebts()

    @Query("DELETE FROM scheduled_investments")
    suspend fun clearSchedules()
}

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BillEntity::class,
        InvestmentEntity::class,
        InvestmentContributionEntity::class,
        InvestmentHoldingEntity::class,
        DebtEntity::class,
        ScheduledInvestmentEntity::class,
        SalaryScheduleEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS scheduled_investments (
                        id TEXT NOT NULL PRIMARY KEY,
                        assetId TEXT NOT NULL,
                        accountId TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        enabled INTEGER NOT NULL,
                        lastRunMillis INTEGER,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS salary_schedules (
                        id TEXT NOT NULL PRIMARY KEY,
                        accountId TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        frequency TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        lastRunMillis INTEGER,
                        note TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE salary_schedules ADD COLUMN hourOfDay INTEGER NOT NULL DEFAULT 9")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_transferGroupId ON transactions(transferGroupId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_investmentAssetId ON transactions(investmentAssetId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_contributions_assetId ON investment_contributions(assetId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_contributions_accountId ON investment_contributions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_contributions_transactionId ON investment_contributions(transactionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_investments_assetId ON scheduled_investments(assetId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_investments_accountId ON scheduled_investments(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_salary_schedules_accountId ON salary_schedules(accountId)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN paidAccountId TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN paymentTransactionId TEXT")
                db.execSQL("ALTER TABLE bills ADD COLUMN paymentTransactionId TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN openingBalance INTEGER NOT NULL DEFAULT 0")

                db.execSQL("""
                    UPDATE accounts
                    SET openingBalance = balance - COALESCE((
                        SELECT SUM(
                            CASE
                                WHEN t.type IN ('income', 'transfer_in') THEN t.amount
                                WHEN t.type IN ('expense', 'transfer_out') THEN -t.amount
                                ELSE 0
                            END
                        )
                        FROM transactions t
                        WHERE t.accountId = accounts.id
                    ), 0)
                """)
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN paidAmount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN debtId TEXT")
                db.execSQL("UPDATE debts SET paidAmount = amount WHERE isPaid = 1")
                db.execSQL("""
                    UPDATE transactions
                    SET debtId = (SELECT d.id FROM debts d WHERE d.paymentTransactionId = transactions.id)
                    WHERE id IN (SELECT paymentTransactionId FROM debts WHERE paymentTransactionId IS NOT NULL)
                """)
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN feeAssetId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_feeAssetId ON transactions(feeAssetId)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS investment_holdings (
                        id TEXT NOT NULL PRIMARY KEY,
                        platformId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        cost INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_holdings_platformId ON investment_holdings(platformId)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS budgets")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sakume.db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
