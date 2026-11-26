package com.sakume.personalfinance.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun exportedVersionFifteenSchemaCanBeCreatedAndValidated() {
        val databaseName = "sakume-migration-v15"
        helper.createDatabase(databaseName, 15).close()
        helper.runMigrationsAndValidate(databaseName, 15, true).close()
    }

    @Test fun migrationFifteenToSixteenKeepsRealDataAndMapsCategories() {
        val databaseName = "sakume-migration-v15-to-v16"
        helper.createDatabase(databaseName, 15).apply {
            execSQL("INSERT INTO accounts (id, name, type, balance, openingBalance, sortOrder) VALUES ('a1', 'Cash', 'Cash', 70000, 100000, 0)")
            execSQL(
                "INSERT INTO transactions (id, type, amount, category, accountId, dateMillis, note) VALUES " +
                    "('t1', 'expense', 25000, 'Makan siang', 'a1', 1700000000000, 'Makan siang')"
            )
            execSQL(
                "INSERT INTO transactions (id, type, amount, category, accountId, dateMillis, note) VALUES " +
                    "('t2', 'expense', 5000, 'Beli sesuatu', 'a1', 1700000001000, '')"
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate(databaseName, 16, true, AppDatabase.MIGRATION_15_16)
        migrated.query("SELECT id, categoryId, note, category, amount FROM transactions ORDER BY id").use { c ->
            check(c.moveToFirst())
            assertEquals("t1", c.getString(0))
            assertEquals(CategoryIds.MAKAN, c.getString(1))
            assertEquals("Makan siang", c.getString(2))
            check(c.moveToNext())
            assertEquals("t2", c.getString(0))
            assertEquals(CategoryIds.LAINNYA, c.getString(1))
            assertEquals("Beli sesuatu", c.getString(2))
            assertEquals("Beli sesuatu", c.getString(3))
            assertEquals(5000L, c.getLong(4))
        }
        migrated.query("SELECT balance FROM accounts WHERE id = 'a1'").use { c ->
            check(c.moveToFirst())
            assertEquals(70000L, c.getLong(0))
        }
        migrated.close()
    }
}
