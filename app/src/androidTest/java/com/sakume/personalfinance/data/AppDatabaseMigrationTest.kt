package com.sakume.personalfinance.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
}
