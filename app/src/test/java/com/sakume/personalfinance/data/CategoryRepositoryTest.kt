package com.sakume.personalfinance.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CategoryRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepository
    private lateinit var cashId: String

    @Before fun setUp() {
        kotlinx.coroutines.runBlocking {
            db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java
            ).allowMainThreadQueries().addCallback(AppDatabase.SEED_ON_CREATE).build()
            repo = FinanceRepository(db)
            repo.addAccount("Cash", "Cash", 500_000L)
            cashId = db.dao().allAccounts().single().id
        }
    }

    @After fun tearDown() { if (::db.isInitialized) db.close() }

    private val oldBackup = """
        {"accounts":[{"id":"a1","name":"Cash","type":"Cash","balance":100000,"openingBalance":100000}],
         "transactions":[
          {"id":"t1","type":"expense","amount":25000,"category":"Makan siang","accountId":"a1","dateMillis":1700000000000,"note":"Makan siang"},
          {"id":"t2","type":"expense","amount":10000,"category":"Bensin","accountId":"a1","dateMillis":1700000001000,"note":""},
          {"id":"t3","type":"income","amount":500000,"category":"Gaji","accountId":"a1","dateMillis":1700000002000,"note":"Gaji"},
          {"id":"t4","type":"expense","amount":5000,"category":"Beli sesuatu","accountId":"a1","dateMillis":1700000003000,"note":"Beli sesuatu"},
          {"id":"t5","type":"expense","amount":7000,"category":"Utang · Budi","accountId":"a1","dateMillis":1700000004000,"note":"Cicilan utang","debtId":"d1"}],
         "bills":[],"investments":[],"contributions":[],
         "debts":[{"id":"d1","type":"utang","person":"Budi","description":"","amount":7000,"isPaid":true,"createdAtMillis":1700000000000,"paidAmount":7000}],
         "meta":{"format":2,"createdAtMillis":1700000000000,"appVersion":"1.3.0","source":"manual"}}
    """.trimIndent()

    @Test fun formatVersionIsThreeAndNewBackupsCarryCategories() = runTest {
        assertEquals(3, BackupCodec.FORMAT_VERSION)
        repo.addTransaction("expense", 1_000, CategoryIds.MAKAN, cashId, 1_700_000_000_000, "")
        val decoded = BackupCodec.decode(repo.exportJson())
        assertEquals(DefaultCategories.all.size, decoded.categories.size)
        assertEquals(CategoryIds.MAKAN, decoded.transactions.single().categoryId)
    }

    @Test fun restoringOldBackupWithoutCategoriesMapsWithTheSameRulesAsTheMigration() = runTest {
        repo.importJson(oldBackup)
        val byId = db.dao().allTransactions().associateBy { it.id }
        assertEquals(CategoryIds.MAKAN, byId.getValue("t1").categoryId)
        assertEquals(CategoryIds.BBM, byId.getValue("t2").categoryId)
        assertEquals("Bensin", byId.getValue("t2").note)
        assertEquals(CategoryIds.GAJI, byId.getValue("t3").categoryId)
        assertEquals(CategoryIds.LAINNYA, byId.getValue("t4").categoryId)
        assertEquals("Beli sesuatu", byId.getValue("t4").note)
        assertEquals(CategoryIds.UTANG, byId.getValue("t5").categoryId)
        assertEquals(DefaultCategories.all.map { it.id }.toSet(), db.dao().allCategories().map { it.id }.toSet())
        assertEquals(5, byId.size)
    }

    @Test fun restoringOldBackupKeepsEveryAmountAndAccount() = runTest {
        repo.importJson(oldBackup)
        assertEquals(547_000L, db.dao().allTransactions().sumOf { it.amount })
        assertEquals(listOf("a1"), db.dao().allAccounts().map { it.id })
    }

    @Test fun newBackupRoundTripKeepsCustomCategoriesAndAssignments() = runTest {
        val custom = repo.addCategory("Kopi kantor", CATEGORY_KIND_EXPENSE, "cafe", 0xFF3B82F6)
        repo.addTransaction("expense", 4_000, custom.id, cashId, 1_700_000_000_000, "Latte")
        val before = repo.snapshot()
        repo.importJson(BackupCodec.encode(before))
        assertEquals(before, repo.snapshot())
    }

    @Test fun backupReferencingUnknownCategoryIsRejectedAndNothingChanges() = runTest {
        val before = repo.snapshot()
        val bad = before.copy(
            transactions = listOf(TransactionEntity("x", "expense", 1_000, "", cashId, 1L, "", categoryId = "tidak-ada"))
        )
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.importJson(BackupCodec.encode(bad)) }
        }
        assertEquals(before, repo.snapshot())
    }

    @Test fun backupMissingSystemCategoriesGetThemAdded() = runTest {
        val data = repo.snapshot().copy(categories = listOf(CategoryEntity("custom", "Kustom", CATEGORY_KIND_EXPENSE)))
        repo.importJson(BackupCodec.encode(data))
        val ids = db.dao().allCategories().map { it.id }.toSet()
        assertTrue(DefaultCategories.systemIds.all { it in ids })
        assertTrue("custom" in ids)
    }

    @Test fun addTransactionRequiresExistingCategoryOfMatchingKind() = runTest {
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repo.addTransaction("expense", 1_000, "tidak-ada", cashId, 1L, "") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.addTransaction("expense", 1_000, CategoryIds.GAJI, cashId, 1L, "") }
        }
        repo.addTransaction("expense", 1_000, CategoryIds.MAKAN, cashId, 1L, "")
        assertEquals(CategoryIds.MAKAN, db.dao().allTransactions().single().categoryId)
    }

    @Test fun noteIsOptionalAndSeparateFromCategory() = runTest {
        repo.addTransaction("expense", 1_000, CategoryIds.MAKAN, cashId, 1L, "")
        val tx = db.dao().allTransactions().single()
        assertEquals("", tx.note)
        assertEquals(CategoryIds.MAKAN, tx.categoryId)
    }

    @Test fun systemCategoriesCannotBeDeletedOrRenamed() = runTest {
        DefaultCategories.systemIds.forEach { id ->
            assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repo.deleteCategory(id) } }
        }
        repo.updateCategory(CategoryIds.INVESTASI, "Nama baru", "cash", 0xFF000000)
        val after = db.dao().category(CategoryIds.INVESTASI)!!
        assertEquals("Investasi", after.name)
        assertEquals(0xFF000000, after.color)
    }

    @Test fun deletingCustomCategoryMovesItsTransactionsToLainnya() = runTest {
        val custom = repo.addCategory("Hobi", CATEGORY_KIND_EXPENSE, "movie", 0xFF8B5CF6)
        repo.addTransaction("expense", 1_000, custom.id, cashId, 1L, "a")
        repo.addTransaction("expense", 2_000, custom.id, cashId, 2L, "b")
        assertEquals(2, repo.deleteCategory(custom.id))
        assertEquals(null, db.dao().category(custom.id))
        assertTrue(db.dao().allTransactions().all { it.categoryId == CategoryIds.LAINNYA })
        assertEquals(2, db.dao().allTransactions().size)
    }

    @Test fun categoryNamesAreUniqueIgnoringCase() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.addCategory("makan & MINUM", CATEGORY_KIND_EXPENSE, "cafe", 1L) }
        }
        val a = repo.addCategory("Hobi", CATEGORY_KIND_EXPENSE, "movie", 1L)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.updateCategory(a.id, "Transport", "movie", 1L) }
        }
        assertFalse(a.isSystem)
    }

    @Test fun systemGeneratedTransactionsUseSystemCategories() = runTest {
        repo.addAccount("Bank", "Bank", 100_000)
        val bank = db.dao().allAccounts().first { it.name == "Bank" }
        repo.transfer(cashId, bank.id, 10_000)
        repo.addInvestment("Bibit", null)
        val asset = db.dao().allInvestments().single()
        repo.contribute(asset.id, cashId, 5_000, adminFee = 500)
        val all = db.dao().allTransactions()
        assertEquals(2, all.count { it.categoryId == CategoryIds.TRANSFER })
        assertEquals(1, all.count { it.categoryId == CategoryIds.INVESTASI })
        assertEquals(1, all.count { it.categoryId == CategoryIds.ADMIN_BANK })
        repo.addGoal("Liburan", 1_000_000, null, "Liburan")
        val goal = db.dao().allInvestments().first { it.isGoal }
        repo.contribute(goal.id, cashId, 3_000)
        assertEquals(1, db.dao().allTransactions().count { it.categoryId == CategoryIds.TABUNGAN })
    }
}
