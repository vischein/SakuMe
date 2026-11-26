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
class AccountCreditMigrationTest {
    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(16) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE accounts (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL, " +
                        "balance INTEGER NOT NULL, openingBalance INTEGER NOT NULL, sortOrder INTEGER NOT NULL)"
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

    @Test fun migrationAddsNullableCreditLimitAndKeepsExistingAccounts() {
        db.execSQL("INSERT INTO accounts VALUES ('a1', 'Cash', 'Cash', 70000, 100000, 0)")
        db.execSQL("INSERT INTO accounts VALUES ('a2', 'BCA', 'Bank', 250000, 250000, 1)")

        AppDatabase.MIGRATION_16_17.migrate(db)

        db.query("SELECT id, name, type, balance, openingBalance, sortOrder, creditLimit FROM accounts ORDER BY sortOrder").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("a1", c.getString(0))
            assertEquals(70_000L, c.getLong(3))
            assertEquals(100_000L, c.getLong(4))
            assertTrue(c.isNull(6))
            assertTrue(c.moveToNext())
            assertEquals("Bank", c.getString(2))
            assertEquals(250_000L, c.getLong(3))
            assertTrue(c.isNull(6))
        }
    }

    @Test fun migratedColumnHasRoomCompatibleDefinition() {
        AppDatabase.MIGRATION_16_17.migrate(db)
        db.query("PRAGMA table_info(accounts)").use { c ->
            var found = false
            while (c.moveToNext()) {
                if (c.getString(1) == "creditLimit") {
                    found = true
                    assertEquals("INTEGER", c.getString(2).uppercase())
                    assertEquals(0, c.getInt(3))
                    assertTrue(c.isNull(4))
                }
            }
            assertTrue(found)
        }
    }

    @Test fun migratedColumnAcceptsCreditAccounts() {
        AppDatabase.MIGRATION_16_17.migrate(db)
        db.execSQL("INSERT INTO accounts VALUES ('c1', 'PayLater', 'Kredit', -120000, -120000, 0, 500000)")
        db.query("SELECT creditLimit, balance FROM accounts WHERE id = 'c1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(500_000L, c.getLong(0))
            assertEquals(-120_000L, c.getLong(1))
        }
    }
}
