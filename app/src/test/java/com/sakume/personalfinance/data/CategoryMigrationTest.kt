package com.sakume.personalfinance.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CategoryMigrationTest {
    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(15) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE transactions (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, amount INTEGER NOT NULL, " +
                        "category TEXT NOT NULL, accountId TEXT NOT NULL, dateMillis INTEGER NOT NULL, note TEXT NOT NULL, " +
                        "investmentAssetId TEXT, transferGroupId TEXT, debtId TEXT, feeAssetId TEXT)"
                )
                db.execSQL(
                    "CREATE TABLE investments (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, target INTEGER, " +
                        "value INTEGER NOT NULL, archived INTEGER NOT NULL, kind TEXT NOT NULL, deadlineMillis INTEGER, category TEXT NOT NULL)"
                )
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext<Context>())
            .name(null).callback(callback).build()
        helper = FrameworkSQLiteOpenHelperFactory().create(config)
        db = helper.writableDatabase
    }

    @After fun tearDown() { helper.close() }

    private fun tx(
        id: String, type: String, category: String, note: String,
        inv: String? = null, group: String? = null, debt: String? = null, fee: String? = null
    ) {
        db.execSQL(
            "INSERT INTO transactions VALUES (?, ?, 1000, ?, 'a', 1700000000000, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(id, type, category, note, inv, group, debt, fee)
        )
    }

    private fun categoryOf(id: String): Pair<String, String> =
        db.query("SELECT categoryId, note FROM transactions WHERE id = '$id'").use { c ->
            c.moveToFirst()
            c.getString(0) to c.getString(1)
        }

    private fun seedLegacyRows() {
        db.execSQL("INSERT INTO investments VALUES ('goal1','Dana darurat',NULL,0,0,'goal',NULL,'Darurat')")
        db.execSQL("INSERT INTO investments VALUES ('plat1','Bibit',NULL,0,0,'platform',NULL,'')")
        tx("t1", "expense", "Makan siang", "Makan siang")
        tx("t2", "expense", "Bensin Pertalite", "")
        tx("t3", "expense", "Pengeluaran", "")
        tx("t4", "income", "Gaji", "Gaji")
        tx("t5", "expense", "Beli sesuatu", "Beli sesuatu")
        tx("t6", "transfer_out", "Transfer", "", group = "g1")
        tx("t7", "transfer_in", "Transfer", "", group = "g1")
        tx("t8", "expense", "Utang · Budi", "Cicilan utang", debt = "d1")
        tx("t9", "expense", "Investasi", "Setor ke Dana darurat", inv = "goal1")
        tx("t10", "expense", "Investasi", "Setor ke Bibit", inv = "plat1")
        tx("t11", "expense", "Admin bank", "Biaya admin", fee = "plat1")
        tx("t12", "expense", "Gaji karyawan", "Gaji karyawan")
        tx("t13", "expense", "Tagihan SPayLater", "iPhone")
        tx("t14", "expense", "Utang · Ani", "", debt = "d2")
    }

    @Test fun migrationSeedsDefaultCategoriesAndKeepsEveryRow() {
        seedLegacyRows()
        AppDatabase.MIGRATION_15_16.migrate(db)

        db.query("SELECT COUNT(*) FROM categories").use { c -> c.moveToFirst(); assertEquals(DefaultCategories.all.size, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM categories WHERE isSystem = 1").use { c -> c.moveToFirst(); assertEquals(8, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM transactions").use { c -> c.moveToFirst(); assertEquals(14, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM transactions WHERE categoryId = ''").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_categoryId'").use {
            assertTrue(it.moveToFirst())
        }
    }

    @Test fun migrationMapsLegacyRowsAndPreservesOriginalText() {
        seedLegacyRows()
        AppDatabase.MIGRATION_15_16.migrate(db)

        assertEquals(CategoryIds.MAKAN to "Makan siang", categoryOf("t1"))
        assertEquals(CategoryIds.BBM to "Bensin Pertalite", categoryOf("t2"))
        assertEquals(CategoryIds.LAINNYA to "", categoryOf("t3"))
        assertEquals(CategoryIds.GAJI to "Gaji", categoryOf("t4"))
        assertEquals(CategoryIds.LAINNYA to "Beli sesuatu", categoryOf("t5"))
        assertEquals(CategoryIds.TRANSFER, categoryOf("t6").first)
        assertEquals(CategoryIds.TRANSFER, categoryOf("t7").first)
        assertEquals(CategoryIds.UTANG to "Cicilan utang", categoryOf("t8"))
        assertEquals(CategoryIds.TABUNGAN, categoryOf("t9").first)
        assertEquals(CategoryIds.INVESTASI, categoryOf("t10").first)
        assertEquals(CategoryIds.ADMIN_BANK, categoryOf("t11").first)
        assertEquals(CategoryIds.LAINNYA to "Gaji karyawan", categoryOf("t12"))
        assertEquals(CategoryIds.TAGIHAN to "iPhone", categoryOf("t13"))
        assertEquals(CategoryIds.UTANG to "Utang · Ani", categoryOf("t14"))
    }

    @Test fun migrationLeavesTheLegacyCategoryColumnUntouched() {
        seedLegacyRows()
        AppDatabase.MIGRATION_15_16.migrate(db)
        db.query("SELECT category FROM transactions WHERE id = 't13'").use { c ->
            c.moveToFirst()
            assertEquals("Tagihan SPayLater", c.getString(0))
        }
    }

    @Test fun migrationWorksOnAnEmptyDatabase() {
        AppDatabase.MIGRATION_15_16.migrate(db)
        db.query("SELECT COUNT(*) FROM categories").use { c -> c.moveToFirst(); assertEquals(DefaultCategories.all.size, c.getInt(0)) }
    }
}
