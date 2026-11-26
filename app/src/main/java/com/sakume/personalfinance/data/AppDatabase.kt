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
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

internal const val DB_FILE_NAME = "sakume.db"

class MigrationFailedException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

internal enum class DbFileKind { MISSING, TOO_SHORT, PLAIN_SQLITE, OTHER, UNREADABLE }

private val SQLITE_PLAIN_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

/**
 * Menilai file database dari 16 byte pertamanya. Hanya PLAIN_SQLITE yang boleh dimigrasi.
 * File SQLCipher (terenkripsi) tidak pernah diawali header ini; OTHER = bukan SQLite polos, jangan disentuh.
 */
internal fun classifyDbFile(file: File): DbFileKind {
    if (!file.exists()) return DbFileKind.MISSING
    return try {
        file.inputStream().use { input ->
            val buf = ByteArray(SQLITE_PLAIN_MAGIC.size)
            var n = 0
            while (n < buf.size) {
                val r = input.read(buf, n, buf.size - n)
                if (r <= 0) break
                n += r
            }
            when {
                n < buf.size -> DbFileKind.TOO_SHORT
                buf.contentEquals(SQLITE_PLAIN_MAGIC) -> DbFileKind.PLAIN_SQLITE
                else -> DbFileKind.OTHER
            }
        }
    } catch (_: Exception) {
        DbFileKind.UNREADABLE
    }
}


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

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun categories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun allCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun category(id: String): CategoryEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories WHERE id != 'sys_lainnya'")
    suspend fun maxCategorySortOrder(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: String): Int

    @Query("UPDATE transactions SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun reassignCategory(fromId: String, toId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCategory(item: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCategories(items: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

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
        SalaryScheduleEntity::class,
        CategoryEntity::class
    ],
    version = 17,
    exportSchema = true
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

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN dueDateMillis INTEGER")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE investments ADD COLUMN kind TEXT NOT NULL DEFAULT 'platform'")
                db.execSQL("ALTER TABLE investments ADD COLUMN deadlineMillis INTEGER")
                db.execSQL("ALTER TABLE investments ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            }
        }

        internal fun seedCategories(db: SupportSQLiteDatabase) {
            DefaultCategories.all.forEach { c ->
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (id, name, kind, icon, color, sortOrder, isSystem) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(c.id, c.name, c.kind, c.icon, c.color, c.sortOrder, if (c.isSystem) 1 else 0)
                )
            }
        }

        internal fun backfillTransactionCategories(db: SupportSQLiteDatabase) {
            val goalIds = HashSet<String>()
            db.query("SELECT id FROM investments WHERE kind = 'goal'").use { c ->
                while (c.moveToNext()) goalIds += c.getString(0)
            }
            class Row(val id: String, val m: LegacyMapping)
            val rows = ArrayList<Row>()
            db.query(
                "SELECT id, type, category, note, debtId, transferGroupId, investmentAssetId, feeAssetId FROM transactions"
            ).use { c ->
                while (c.moveToNext()) {
                    rows += Row(
                        c.getString(0),
                        LegacyCategoryMapper.map(
                            type = c.getString(1),
                            category = c.getString(2) ?: "",
                            note = c.getString(3) ?: "",
                            debtId = if (c.isNull(4)) null else c.getString(4),
                            transferGroupId = if (c.isNull(5)) null else c.getString(5),
                            investmentAssetId = if (c.isNull(6)) null else c.getString(6),
                            feeAssetId = if (c.isNull(7)) null else c.getString(7),
                            goalAssetIds = goalIds
                        )
                    )
                }
            }
            rows.forEach { r ->
                db.execSQL(
                    "UPDATE transactions SET categoryId = ?, note = ? WHERE id = ?",
                    arrayOf<Any?>(r.m.categoryId, r.m.note, r.id)
                )
            }
        }

        internal val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        color INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        isSystem INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                seedCategories(db)
                db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
                backfillTransactionCategories(db)
            }
        }

        internal val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimit INTEGER")
            }
        }

        internal val SEED_ON_CREATE = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedCategories(db)
            }
        }

        private val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
            MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
            MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17
        )

        /**
         * Memblokir (Keystore, migrasi, buka database). JANGAN dipanggil dari main thread:
         * UI memakai [open] lewat FinanceViewModel; worker background boleh memanggil ini langsung.
         */
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: openEncrypted(context.applicationContext).also { INSTANCE = it }
            }

        /**
         * Seperti [get], tetapi juga memaksa database benar-benar terbuka sekarang supaya kegagalan
         * (kunci, file rusak, migrasi skema) muncul di sini, bukan nanti di tengah query. Panggil di thread IO.
         * Bila gagal, instance dibuang agar pemanggilan berikutnya (Coba lagi) mulai dari awal.
         */
        fun open(context: Context): AppDatabase {
            val db = get(context)
            try {
                db.openHelper.writableDatabase
            } catch (e: Throwable) {
                synchronized(this) { if (INSTANCE === db) INSTANCE = null }
                try {
                    db.close()
                } catch (_: Exception) {
                }
                throw e
            }
            return db
        }

        private fun openEncrypted(context: Context): AppDatabase {
            
            System.loadLibrary("sqlcipher")

            val passphrase = SecureStore.sqlCipherPassphrase(context)
            val factory = SupportOpenHelperFactory(passphrase)

            
            if (!SecureStore.isDbMigrated(context)) {
                migratePlainToEncryptedIfNeeded(context, passphrase)
            }

            return Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE_NAME)
                .openHelperFactory(factory)
                .addMigrations(*ALL_MIGRATIONS)
                .addCallback(SEED_ON_CREATE)
                .build()
        }

        










        private fun migratePlainToEncryptedIfNeeded(context: Context, passphrase: ByteArray) {
            val dbFile = context.getDatabasePath(DB_FILE_NAME)
            when (classifyDbFile(dbFile)) {
                DbFileKind.MISSING -> {
                    SecureStore.setDbMigrated(context, true)
                    return
                }
                DbFileKind.OTHER -> {
                    // Bukan SQLite polos (diawali header lain) = terenkripsi. Tidak ada yang perlu dimigrasi dan
                    // file TIDAK disentuh. Kalau ternyata tidak bisa dibuka (kunci salah, rusak), itu dilaporkan
                    // oleh pembukaan database biasa, bukan diselesaikan dengan memindahkan file.
                    SecureStore.setDbMigrated(context, true)
                    return
                }
                DbFileKind.TOO_SHORT, DbFileKind.UNREADABLE -> {
                    android.util.Log.w("SakuMe", "sakume.db tidak bisa diperiksa headernya; tidak disentuh")
                    return
                }
                DbFileKind.PLAIN_SQLITE -> Unit
            }

            // Header benar-benar SQLite polos: database lama sebelum SQLCipher. Pindahkan, baca, impor ulang.
            val plainName = "sakume_plain_migrate.db"
            val plainFile = context.getDatabasePath(plainName)
            var moved = false
            var plainDeleted = false
            var plain: AppDatabase? = null
            try {
                renameDbFiles(dbFile, plainFile)
                moved = true

                plain = Room.databaseBuilder(context, AppDatabase::class.java, plainName)
                    .addMigrations(*ALL_MIGRATIONS)
                    .addCallback(SEED_ON_CREATE)
                    .build()
                val repo = FinanceRepository(plain)
                val json = runBlocking {
                    val data = repo.snapshot()
                    BackupCodec.encode(
                        data.copy(
                            meta = BackupMeta(
                                createdAtMillis = System.currentTimeMillis(),
                                appVersion = try {
                                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                                } catch (_: Exception) { "" },
                                source = "sqlcipher-migrate"
                            )
                        )
                    )
                }
                plain.close()
                plain = null

                val safetyDir = File(context.filesDir, "backups").also { if (!it.exists()) it.mkdirs() }
                val safetyFile = File(
                    safetyDir,
                    "sakume_sebelum_impor_sqlcipher_${System.currentTimeMillis()}.json"
                )
                safetyFile.writeText(json)

                deleteDbFiles(plainFile)
                plainDeleted = true
                SecureStore.setDbMigrated(context, true)

                try {
                    val encrypted = Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE_NAME)
                        .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf()))
                        .addMigrations(*ALL_MIGRATIONS)
                        .addCallback(SEED_ON_CREATE)
                        .build()
                    runBlocking {
                        FinanceRepository(encrypted).importJson(json)
                    }
                    encrypted.close()
                } catch (reimport: Exception) {
                    android.util.Log.e(
                        "SakuMe",
                        "SQLCipher re-import failed; safety backup at ${safetyFile.name}: ${reimport.message}",
                        reimport
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SakuMe", "SQLCipher migration failed: ${e.message}", e)
                try {
                    plain?.close()
                } catch (_: Exception) {
                }
                // Gagal sebelum database lama dihapus: kembalikan ke nama semula supaya datanya tetap utuh.
                if (moved && !plainDeleted && !dbFile.exists()) {
                    renameDbFiles(plainFile, dbFile)
                }
                val detail = e.message ?: e.javaClass.simpleName
                throw MigrationFailedException(
                    if (plainDeleted)
                        "Database lama sudah dipindah ke versi terenkripsi, tetapi langkah akhirnya gagal. " +
                            "Salinan datamu ada di daftar Backup & Restore (\"Sebelum impor\"). ($detail)"
                    else
                        "Database lama gagal dimigrasi ke versi terenkripsi. Data kamu tidak dihapus dan file " +
                            "database dikembalikan seperti semula. Coba tutup lalu buka SakuMe lagi. ($detail)",
                    e
                )
            }
        }

        private fun renameDbFiles(from: File, to: File) {
            if (!from.exists()) return
            from.renameTo(to)
            File(from.path + "-wal").takeIf { it.exists() }?.renameTo(File(to.path + "-wal"))
            File(from.path + "-shm").takeIf { it.exists() }?.renameTo(File(to.path + "-shm"))
            File(from.path + "-journal").takeIf { it.exists() }?.renameTo(File(to.path + "-journal"))
        }

        private fun deleteDbFiles(base: File) {
            base.delete()
            File(base.path + "-wal").delete()
            File(base.path + "-shm").delete()
            File(base.path + "-journal").delete()
        }
    }
}
