package com.sakume.personalfinance.data

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class AppDatabaseConfigurationTest {
    @Test fun databaseBuilderNeverEnablesDestructiveMigrationFallback() {
        val source = File("src/main/java/com/sakume/personalfinance/data/AppDatabase.kt")
        assertFalse("AppDatabase must use explicit migrations, not destructive fallback", source.readText().contains("fallbackToDestructiveMigration"))
    }
}
