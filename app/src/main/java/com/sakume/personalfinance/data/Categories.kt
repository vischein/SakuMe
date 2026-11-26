package com.sakume.personalfinance.data

object CategoryIds {
    const val INVESTASI = "sys_investasi"
    const val TABUNGAN = "sys_tabungan"
    const val GAJI = "sys_gaji"
    const val TRANSFER = "sys_transfer"
    const val TAGIHAN = "sys_tagihan"
    const val UTANG = "sys_utang"
    const val ADMIN_BANK = "sys_admin"
    const val LAINNYA = "sys_lainnya"

    const val MAKAN = "def_makan"
    const val TRANSPORT = "def_transport"
    const val BBM = "def_bbm"
    const val BELANJA = "def_belanja"
    const val ROKOK = "def_rokok"
    const val HIBURAN = "def_hiburan"
    const val KESEHATAN = "def_kesehatan"
    const val PULSA = "def_pulsa"
    const val SEWA = "def_sewa"
    const val UTILITAS = "def_utilitas"
    const val PENDIDIKAN = "def_pendidikan"
    const val TUNAI = "def_tunai"
    const val BONUS = "def_bonus"
    const val HADIAH = "def_hadiah"
}

object DefaultCategories {
    private fun c(id: String, name: String, kind: String, icon: String, color: Long, order: Int, system: Boolean = false) =
        CategoryEntity(id, name, kind, icon, color, order, system)

    val all: List<CategoryEntity> = listOf(
        c(CategoryIds.MAKAN, "Makan & minum", CATEGORY_KIND_EXPENSE, "restaurant", 0xFFE8A838, 0),
        c(CategoryIds.TRANSPORT, "Transport", CATEGORY_KIND_EXPENSE, "car", 0xFF3B82F6, 1),
        c(CategoryIds.BBM, "BBM", CATEGORY_KIND_EXPENSE, "fuel", 0xFFF97316, 2),
        c(CategoryIds.BELANJA, "Belanja", CATEGORY_KIND_EXPENSE, "cart", 0xFFEC4899, 3),
        c(CategoryIds.ROKOK, "Rokok", CATEGORY_KIND_EXPENSE, "smoke", 0xFF78716C, 4),
        c(CategoryIds.HIBURAN, "Hiburan", CATEGORY_KIND_EXPENSE, "movie", 0xFF8B5CF6, 5),
        c(CategoryIds.KESEHATAN, "Kesehatan", CATEGORY_KIND_EXPENSE, "medical", 0xFFEF4444, 6),
        c(CategoryIds.PULSA, "Pulsa & data", CATEGORY_KIND_EXPENSE, "phone", 0xFF06B6D4, 7),
        c(CategoryIds.SEWA, "Sewa / kos", CATEGORY_KIND_EXPENSE, "home", 0xFF14B8A6, 8),
        c(CategoryIds.UTILITAS, "Utilitas", CATEGORY_KIND_EXPENSE, "bolt", 0xFFEAB308, 9),
        c(CategoryIds.PENDIDIKAN, "Pendidikan", CATEGORY_KIND_EXPENSE, "school", 0xFF6366F1, 10),
        c(CategoryIds.TUNAI, "Tunai", CATEGORY_KIND_EXPENSE, "cash", 0xFF22C55E, 11),
        c(CategoryIds.BONUS, "Bonus", CATEGORY_KIND_INCOME, "gift", 0xFF10B981, 12),
        c(CategoryIds.HADIAH, "Hadiah", CATEGORY_KIND_INCOME, "gift", 0xFFF472B6, 13),
        c(CategoryIds.GAJI, "Gaji", CATEGORY_KIND_INCOME, "work", 0xFF16A34A, 14, system = true),
        c(CategoryIds.TAGIHAN, "Tagihan", CATEGORY_KIND_EXPENSE, "receipt", 0xFFDC2626, 15, system = true),
        c(CategoryIds.ADMIN_BANK, "Admin bank", CATEGORY_KIND_EXPENSE, "bank", 0xFF64748B, 16, system = true),
        c(CategoryIds.INVESTASI, "Investasi", CATEGORY_KIND_BOTH, "trend", 0xFF0EA5E9, 17, system = true),
        c(CategoryIds.TABUNGAN, "Tabungan", CATEGORY_KIND_BOTH, "savings", 0xFF84CC16, 18, system = true),
        c(CategoryIds.UTANG, "Utang", CATEGORY_KIND_BOTH, "debt", 0xFFA855F7, 19, system = true),
        c(CategoryIds.TRANSFER, "Transfer", CATEGORY_KIND_TRANSFER, "swap", 0xFFE8A838, 20, system = true),
        c(CategoryIds.LAINNYA, "Lainnya", CATEGORY_KIND_BOTH, "category", 0xFF6B7280, 99, system = true)
    )

    val systemIds: Set<String> = all.filter { it.isSystem }.map { it.id }.toSet()
    private val byId = all.associateBy { it.id }
    fun byId(id: String): CategoryEntity? = byId[id]
}

data class LegacyMapping(val categoryId: String, val note: String)

object LegacyCategoryMapper {
    private val labelToId = mapOf(
        "Rokok" to CategoryIds.ROKOK,
        "BBM" to CategoryIds.BBM,
        "Investasi" to CategoryIds.INVESTASI,
        "Tagihan" to CategoryIds.TAGIHAN,
        "Admin bank" to CategoryIds.ADMIN_BANK,
        "Makan & minum" to CategoryIds.MAKAN,
        "Transport" to CategoryIds.TRANSPORT,
        "Belanja" to CategoryIds.BELANJA,
        "Gaji" to CategoryIds.GAJI,
        "Hiburan" to CategoryIds.HIBURAN,
        "Kesehatan" to CategoryIds.KESEHATAN,
        "Pulsa & data" to CategoryIds.PULSA,
        "Sewa / kos" to CategoryIds.SEWA,
        "Utilitas" to CategoryIds.UTILITAS,
        "Tunai" to CategoryIds.TUNAI
    )

    fun sanitize(raw: String): String =
        raw.replace(" • ", " ").replace('•', ' ').replace(Regex("\\s+"), " ").trim()

    fun legacyLabel(raw: String): String {
        val original = raw.trim().ifBlank { return "Lainnya" }
        val lower = original.lowercase()
            .replace('•', ' ')
            .replace('-', ' ')
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        val rules = listOf(
            listOf("rokok", "cigarette", "vape") to "Rokok",
            listOf("bensin", "pertamax", "pertalite", "solar", "bbm", "spbu") to "BBM",
            listOf("investasi", "saham", "reksadana", "crypto", "bitcoin") to "Investasi",
            listOf("tagihan", "kredivo", "spinjam", "shopee paylater", "spaylater", "cicilan") to "Tagihan",
            listOf("admin", "biaya admin", "transfer fee") to "Admin bank",
            listOf("makan", "kopi", "tea", "teh", "nasi", "resto", "warung", "go food", "gofood", "grabfood") to "Makan & minum",
            listOf("transport", "ojek", "grab", "gojek", "parkir", "tol") to "Transport",
            listOf("belanja", "supermarket", "minimarket", "alfamart", "indomaret") to "Belanja",
            listOf("gaji", "salary", "upah") to "Gaji",
            listOf("hiburan", "game", "netflix", "spotify", "youtube") to "Hiburan",
            listOf("kesehatan", "obat", "dokter", "apotek") to "Kesehatan",
            listOf("pulsa", "kuota", "internet", "paket data") to "Pulsa & data",
            listOf("sewa", "kontrakan", "kos", "kost") to "Sewa / kos",
            listOf("listrik", "air", "pdam", "pln") to "Utilitas",
            listOf("tarik tunai", "setor tunai") to "Tunai"
        )
        for ((keys, label) in rules) {
            if (keys.any { lower.contains(it) }) return label
        }
        val first = lower.split(" ").firstOrNull { it.length >= 3 } ?: lower
        return first.replaceFirstChar { it.uppercase() }
    }

    fun map(
        type: String,
        category: String,
        note: String,
        debtId: String?,
        transferGroupId: String?,
        investmentAssetId: String?,
        feeAssetId: String?,
        goalAssetIds: Set<String>
    ): LegacyMapping {
        val structural = when {
            transferGroupId != null || type == "transfer_in" || type == "transfer_out" -> CategoryIds.TRANSFER
            debtId != null -> CategoryIds.UTANG
            feeAssetId != null -> CategoryIds.ADMIN_BANK
            investmentAssetId != null ->
                if (investmentAssetId in goalAssetIds) CategoryIds.TABUNGAN else CategoryIds.INVESTASI
            else -> null
        }
        val textual = labelToId[legacyLabel(category.ifBlank { note })] ?: CategoryIds.LAINNYA
        var id = structural ?: textual
        val chosen = DefaultCategories.byId(id)
        if (chosen == null || !chosen.accepts(type)) id = CategoryIds.LAINNYA
        if (type == "transfer_in" || type == "transfer_out") id = CategoryIds.TRANSFER
        return LegacyMapping(id, preservedNote(category, note, DefaultCategories.byId(id)?.name.orEmpty()))
    }

    private fun preservedNote(category: String, note: String, mappedName: String): String {
        if (note.isNotBlank()) return note
        val text = sanitize(category)
        return if (text.isBlank() || text == "Pemasukan" || text == "Pengeluaran" || text.equals(mappedName, true)) "" else text
    }
}

object CategoryBackfill {
    fun apply(data: BackupData): BackupData {
        val base = data.categories.ifEmpty { DefaultCategories.all }
        val present = base.map { it.id }.toSet()
        val categories = (base + DefaultCategories.all.filter { it.isSystem && it.id !in present })
            .map { if (it.id in DefaultCategories.systemIds && !it.isSystem) it.copy(isSystem = true) else it }
        val goalIds = data.investments.filter { it.kind == KIND_GOAL }.map { it.id }.toSet()
        val transactions = data.transactions.map { tx ->
            if (tx.categoryId.isNotBlank()) tx
            else {
                val m = LegacyCategoryMapper.map(
                    tx.type, tx.category, tx.note, tx.debtId, tx.transferGroupId,
                    tx.investmentAssetId, tx.feeAssetId, goalIds
                )
                tx.copy(categoryId = m.categoryId, note = m.note)
            }
        }
        return data.copy(categories = categories, transactions = transactions)
    }
}
