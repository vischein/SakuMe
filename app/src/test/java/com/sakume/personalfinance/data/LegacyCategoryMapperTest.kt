package com.sakume.personalfinance.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyCategoryMapperTest {
    private fun map(
        type: String = "expense",
        category: String,
        note: String = category,
        debtId: String? = null,
        group: String? = null,
        inv: String? = null,
        fee: String? = null,
        goals: Set<String> = emptySet()
    ) = LegacyCategoryMapper.map(type, category, note, debtId, group, inv, fee, goals)

    @Test fun textRulesKeepTheSameLabelsTheOldAnalyticsScreenProduced() {
        assertEquals("Makan & minum", LegacyCategoryMapper.legacyLabel("Kopi susu"))
        assertEquals("Utilitas", LegacyCategoryMapper.legacyLabel("Listrik PLN"))
        assertEquals("BBM", LegacyCategoryMapper.legacyLabel("Pertalite"))
        assertEquals("Lainnya", LegacyCategoryMapper.legacyLabel("   "))
        assertEquals("Beli", LegacyCategoryMapper.legacyLabel("Beli sesuatu"))
    }

    @Test fun everyLabelTheOldRulesCanProduceMapsToAnExistingDefaultCategory() {
        val samples = listOf(
            "rokok", "bensin", "investasi", "tagihan", "admin", "makan", "ojek", "belanja", "gaji",
            "netflix", "obat", "pulsa", "kos", "pln", "tarik tunai"
        )
        samples.forEach { text ->
            val kind = if (text == "gaji") "income" else "expense"
            val id = map(type = kind, category = text).categoryId
            assertTrue("$text -> $id", DefaultCategories.byId(id) != null)
            assertTrue("$text should not fall into Lainnya", id != CategoryIds.LAINNYA)
        }
    }

    @Test fun unmappedTextFallsIntoLainnyaAndKeepsOriginalTextInNote() {
        val m = map(category = "Beli sesuatu", note = "")
        assertEquals(CategoryIds.LAINNYA, m.categoryId)
        assertEquals("Beli sesuatu", m.note)
    }

    @Test fun existingNoteIsNeverOverwritten() {
        val m = map(category = "Tagihan SPayLater", note = "iPhone")
        assertEquals(CategoryIds.TAGIHAN, m.categoryId)
        assertEquals("iPhone", m.note)
    }

    @Test fun placeholderCategoryDoesNotBecomeANote() {
        assertEquals("", map(category = "Pengeluaran", note = "").note)
        assertEquals("", map(type = "income", category = "Pemasukan", note = "").note)
    }

    @Test fun categoryNameEqualToMappedCategoryIsNotCopiedIntoNote() {
        assertEquals("", map(type = "income", category = "Gaji", note = "").note)
    }

    @Test fun structuralLinksWinOverText() {
        assertEquals(CategoryIds.UTANG, map(category = "Makan", debtId = "d").categoryId)
        assertEquals(CategoryIds.TRANSFER, map(type = "transfer_out", category = "Makan", group = "g").categoryId)
        assertEquals(CategoryIds.ADMIN_BANK, map(category = "Investasi", fee = "p").categoryId)
        assertEquals(CategoryIds.TABUNGAN, map(category = "Investasi", inv = "goal", goals = setOf("goal")).categoryId)
        assertEquals(CategoryIds.INVESTASI, map(category = "Investasi", inv = "plat", goals = setOf("goal")).categoryId)
    }

    @Test fun kindMismatchFallsBackToLainnya() {
        assertEquals(CategoryIds.LAINNYA, map(type = "expense", category = "Gaji karyawan").categoryId)
        assertEquals(CategoryIds.LAINNYA, map(type = "income", category = "Makan siang").categoryId)
    }

    @Test fun defaultsContainTheEightSystemCategoriesAndUniqueNamesAndIds() {
        assertEquals(
            setOf("Investasi", "Tabungan", "Gaji", "Transfer", "Tagihan", "Utang", "Admin bank", "Lainnya"),
            DefaultCategories.all.filter { it.isSystem }.map { it.name }.toSet()
        )
        assertEquals(DefaultCategories.all.size, DefaultCategories.all.map { it.id }.toSet().size)
        assertEquals(DefaultCategories.all.size, DefaultCategories.all.map { it.name.lowercase() }.toSet().size)
    }

    @Test fun backfillIsIdempotentAndSeedsMissingSystemCategories() {
        val tx = TransactionEntity("t", "expense", 1000, "Makan", "a", 1L, "Makan")
        val data = BackupData(
            accounts = listOf(AccountEntity("a", "Cash")), transactions = listOf(tx), bills = emptyList(),
            investments = emptyList(), contributions = emptyList(), debts = emptyList(),
            categories = listOf(CategoryEntity("custom", "Kustom", CATEGORY_KIND_EXPENSE))
        )
        val once = CategoryBackfill.apply(data)
        assertTrue(DefaultCategories.systemIds.all { id -> once.categories.any { it.id == id } })
        assertTrue(once.categories.any { it.id == "custom" })
        assertEquals(once, CategoryBackfill.apply(once))
    }
}
