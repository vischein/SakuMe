package com.sakume.personalfinance.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

val HOLDING_TYPES = listOf("Crypto", "ETF", "Saham")

class FinanceRepository(private val db: AppDatabase) {
    private val dao = db.dao()
    val accounts: Flow<List<AccountEntity>> = dao.accounts()
    val transactions: Flow<List<TransactionEntity>> = dao.transactions()
    val bills: Flow<List<BillEntity>> = dao.bills()
    val investments: Flow<List<InvestmentEntity>> = dao.investments()
    val holdings: Flow<List<InvestmentHoldingEntity>> = dao.holdings()
    val debts: Flow<List<DebtEntity>> = dao.debts()
    val schedules: Flow<List<ScheduledInvestmentEntity>> = dao.schedules()
    val salaries: Flow<List<SalaryScheduleEntity>> = dao.salaries()
    private fun id() = UUID.randomUUID().toString()

    private fun rupiahText(v: Long): String =
        "Rp" + java.text.NumberFormat.getInstance(java.util.Locale("id", "ID")).format(v)

    suspend fun addAccount(name: String, type: String, opening: Long) = db.withTransaction {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Nama rekening wajib diisi" }
        require(opening >= 0L) { "Saldo awal tidak boleh negatif" }
        require(dao.allAccounts().none { it.name.equals(cleanName, ignoreCase = true) }) {
            "Nama rekening sudah digunakan"
        }
        val order = dao.maxAccountSortOrder() + 1
        dao.putAccount(AccountEntity(id(), cleanName, type, opening, openingBalance = opening, sortOrder = order))
    }
    
    suspend fun renameAccount(accountId: String, newName: String) = db.withTransaction {
    val cleanName = newName.trim()
         require(cleanName.isNotBlank()) { "Nama rekening wajib diisi" }
    val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
         require(dao.allAccounts().none { it.id != accountId && it.name.equals(cleanName, ignoreCase = true) }) {
             "Nama rekening sudah digunakan"
         }
         if (account.name != cleanName) dao.putAccount(account.copy(name = cleanName))
    }

    suspend fun deleteAccount(accountId: String) = db.withTransaction {
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        require(account.balance == 0L) {
            "Saldo masih ${account.balance}. Transfer atau kosongkan dulu sebelum menghapus."
        }

        val hasTransactions = dao.firstTransactionForAccount(accountId) != null
        require(!hasTransactions) {
            "Rekening masih memiliki histori transaksi. Hapus atau pindahkan histori terkait terlebih dahulu."
        }
        val activeContribs = dao.allContributions().any { it.accountId == accountId }
        require(!activeContribs) {
            "Masih ada kontribusi investasi dari rekening ini. Tarik dulu atau hapus asetnya."
        }
        val paidBill = dao.allBills().any { it.paidAccountId == accountId }
        require(!paidBill) { "Rekening masih terhubung ke pembayaran tagihan." }
        val paidDebt = dao.allDebts().any { it.paidAccountId == accountId }
        require(!paidDebt) { "Rekening masih terhubung ke pelunasan utang/piutang." }
        dao.deleteAccount(accountId)
    }

    suspend fun reorderAccounts(orderedIds: List<String>) = db.withTransaction {
        val all = dao.allAccounts().associateBy { it.id }
        val normalized = orderedIds.filter { it in all }.distinct()
        require(normalized.size == all.size) { "Urutan rekening tidak lengkap" }
        normalized.forEachIndexed { index, id ->
            val acc = all.getValue(id)
            if (acc.sortOrder != index) dao.putAccount(acc.copy(sortOrder = index))
        }
    }

    suspend fun recalculateBalances() = db.withTransaction {
        val allTx = dao.allTransactions()
        val accounts = dao.allAccounts()

        val balances = accounts.associate { it.id to it.openingBalance }.toMutableMap()
        for (tx in allTx.sortedBy { it.dateMillis }) {
            val delta = when (tx.type) {
                "income", "transfer_in" -> tx.amount
                "expense", "transfer_out" -> -tx.amount
                else -> 0L
            }
            if (balances.containsKey(tx.accountId)) {
                balances[tx.accountId] = (balances[tx.accountId] ?: 0L) + delta
            }
        }
        accounts.forEach { acc ->
            val computed = balances[acc.id] ?: acc.openingBalance
            if (acc.balance != computed) dao.putAccount(acc.copy(balance = computed))
        }
        cleanupCategoryLabelsInternal()
    }

    suspend fun cleanupCategoryLabels() = db.withTransaction {
        cleanupCategoryLabelsInternal()
    }

    private suspend fun cleanupCategoryLabelsInternal() {
        dao.allTransactions().forEach { tx ->
            val cleaned = sanitizeCategoryStored(tx.category)
            if (cleaned != tx.category) {
                dao.putTransaction(tx.copy(category = cleaned))
            }
        }
    }

    private fun sanitizeCategoryStored(raw: String): String =
        raw
            .replace(" • ", " ")
            .replace('•', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

    suspend fun addTransaction(
        type: String,
        amount: Long,
        category: String,
        accountId: String,
        date: Long,
        note: String
    ) = db.withTransaction {
        require(amount > 0) { "Nominal harus lebih dari nol" }
        require(type == "income" || type == "expense") { "Tipe transaksi tidak valid" }
        require(category.isNotBlank()) { "Kategori transaksi wajib diisi" }
        require(date > 0L) { "Tanggal transaksi tidak valid" }
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        if (type == "expense") require(account.balance >= amount) { "Saldo rekening tidak cukup" }
        dao.putAccount(account.copy(balance = account.balance + if (type == "income") amount else -amount))
        dao.putTransaction(TransactionEntity(id(), type, amount, category, accountId, date, note))
    }

    suspend fun deleteTransactions(txIds: List<String>) = db.withTransaction {
        val found = mutableListOf<TransactionEntity>()
        for (id in txIds.filter { it.isNotBlank() }.distinct()) {
            dao.transaction(id)?.let { found += it }
        }
        val ordered = found
            .sortedBy { if (it.type == "income" || it.type == "transfer_in") 1 else 0 }
            .map { it.id }
        for (id in ordered) deleteTransactionInternal(id)
    }

    suspend fun deleteTransaction(txId: String) = db.withTransaction {
        deleteTransactionInternal(txId)
    }

    private suspend fun deleteTransactionInternal(txId: String) {
        val tx = dao.transaction(txId) ?: return
        val group = tx.transferGroupId
        val items = if (group != null) {
            dao.transactionsByGroup(group)
                .filter { it.transferGroupId == group }
                .distinctBy { it.id }
        } else {
            listOf(tx)
        }

        items.forEach { item ->
            val account = dao.account(item.accountId)
            if (account != null) {
                val newBalance = when (item.type) {
                    "income", "transfer_in" -> account.balance - item.amount
                    "expense", "transfer_out" -> account.balance + item.amount
                    else -> account.balance
                }
                // Menghapus pemasukan / transfer masuk MENGURANGI saldo. Kalau uangnya sudah
                // terpakai, saldo akan minus -> tolak dengan pesan yang jelas.
                require(newBalance >= 0L) {
                    "${if (item.type == "transfer_in") "Transfer masuk" else "Pemasukan"} ${rupiahText(item.amount)} " +
                        "tidak bisa dihapus: saldo ${account.name} " +
                        "hanya ${rupiahText(account.balance)} (uangnya sudah terpakai). " +
                        "Hapus dulu transaksi pengeluaran setelahnya atau tambah saldo."
                }
                dao.putAccount(account.copy(balance = newBalance))
            }

            dao.allBills().firstOrNull { it.paymentTransactionId == item.id }?.let { bill ->
                dao.putBill(
                    bill.copy(
                        isPaid = false,
                        paidAccountId = null,
                        paidAtMillis = null,
                        paymentTransactionId = null
                    )
                )
            }
            val linkedDebt = item.debtId?.let { dId -> dao.allDebts().firstOrNull { it.id == dId } }
                ?: dao.allDebts().firstOrNull { it.paymentTransactionId == item.id }
            if (linkedDebt != null) {
                val lastOther = dao.allTransactions()
                    .filter { it.debtId == linkedDebt.id && it.id != item.id }
                    .maxByOrNull { it.dateMillis }
                dao.putDebt(
                    linkedDebt.copy(
                        paidAmount = (linkedDebt.paidAmount - item.amount).coerceAtLeast(0L),
                        isPaid = false,
                        paidAtMillis = null,
                        paidAccountId = lastOther?.accountId,
                        paymentTransactionId = lastOther?.id
                    )
                )
            }

            val assetId = item.investmentAssetId
            if (assetId != null) {
                val asset = dao.allInvestments().firstOrNull { it.id == assetId }
                when (item.type) {
                    "expense" -> {
                        if (asset != null && !asset.archived) {
                            require(asset.value >= item.amount) { "Nilai investasi tidak konsisten" }
                            val held = dao.holdingsForPlatform(asset.id).sumOf { it.cost }
                            require(asset.value - item.amount >= held) { "Saldo platform sudah dipakai untuk aset. Hapus aset terlebih dahulu" }
                            dao.putInvestment(asset.copy(value = asset.value - item.amount))
                        }
                        dao.allContributions().filter { it.transactionId == item.id }.forEach { contribution ->
                            dao.deleteContribution(contribution.id)
                        }
                    }
                    "income" -> {
                        if (asset != null && !asset.archived) {
                            dao.putInvestment(asset.copy(value = asset.value + item.amount))
                            val existing = dao.allContributions().firstOrNull { it.transactionId == item.id }
                            if (existing == null) {
                                dao.putContribution(
                                    InvestmentContributionEntity(
                                        id = id(),
                                        assetId = asset.id,
                                        accountId = item.accountId,
                                        amount = item.amount,
                                        transactionId = item.id,
                                        createdAtMillis = item.dateMillis
                                    )
                                )
                            }
                        }
                    }
                }
            }

            dao.deleteTransaction(item.id)
        }
    }

    suspend fun transfer(fromId: String, toId: String, amount: Long, note: String = "") = db.withTransaction {
        require(fromId != toId && amount > 0) { "Rekening atau nominal tidak valid" }
        val from = dao.account(fromId) ?: error("Rekening asal tidak ditemukan")
        val to = dao.account(toId) ?: error("Rekening tujuan tidak ditemukan")
        require(from.balance >= amount) { "Saldo tidak cukup" }
        dao.putAccount(from.copy(balance = from.balance - amount))
        dao.putAccount(to.copy(balance = to.balance + amount))
        val group = id()
        val now = System.currentTimeMillis()
        dao.putTransaction(
            TransactionEntity(id(), "transfer_out", amount, "Transfer", fromId, now, note, transferGroupId = group)
        )
        dao.putTransaction(
            TransactionEntity(id(), "transfer_in", amount, "Transfer", toId, now, note, transferGroupId = group)
        )
    }

    suspend fun addBill(provider: String, item: String, total: Long, dueDate: Long, tenor: Int) =
        db.withTransaction {
            require(total > 0 && tenor in listOf(1, 3, 6, 9, 12)) { "Nominal atau tenor tidak valid" }
            val parent = id()
            val base = Calendar.getInstance().apply { timeInMillis = dueDate }
            if (provider.equals("SPayLater", true)) base.set(Calendar.DAY_OF_MONTH, 1)
            val each = total / tenor
            repeat(tenor) { i ->
                val cal = base.clone() as Calendar
                cal.add(Calendar.MONTH, i)
                if (provider.equals("SPayLater", true)) cal.set(Calendar.DAY_OF_MONTH, 1)
                val amount = if (i == tenor - 1) total - each * (tenor - 1) else each
                dao.putBill(
                    BillEntity(
                        id(),
                        provider.trim(),
                        item.trim(),
                        amount,
                        cal.timeInMillis,
                        tenor,
                        i + 1,
                        parentBillId = if (tenor > 1) parent else null
                    )
                )
            }
        }

    suspend fun payBill(billId: String, accountId: String) = db.withTransaction {
        val bill = dao.bill(billId) ?: error("Tagihan tidak ditemukan")
        require(!bill.isPaid) { "Tagihan sudah lunas" }
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        require(account.balance >= bill.amount) { "Saldo rekening tidak cukup" }
        val txId = id()
        val now = System.currentTimeMillis()
        dao.putAccount(account.copy(balance = account.balance - bill.amount))
        dao.putBill(
            bill.copy(
                isPaid = true,
                paidAccountId = accountId,
                paidAtMillis = now,
                paymentTransactionId = txId
            )
        )
        dao.putTransaction(
            TransactionEntity(
                txId, "expense", bill.amount,
                "Tagihan ${bill.provider}", accountId, now, bill.itemName
            )
        )
    }

    suspend fun payBills(billsToPay: List<BillEntity>, accountId: String) = db.withTransaction {
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        val requestedIds = billsToPay.map { it.id }.toSet()
        val unpaid = dao.allBills().filter { it.id in requestedIds && !it.isPaid }
        val total = unpaid.sumOf { it.amount }
        require(total > 0) { "Tidak ada tagihan aktif" }
        require(account.balance >= total) { "Saldo rekening tidak cukup" }
        dao.putAccount(account.copy(balance = account.balance - total))
        val now = System.currentTimeMillis()
        unpaid.forEach {
            val txId = id()
            dao.putBill(
                it.copy(
                    isPaid = true,
                    paidAccountId = accountId,
                    paidAtMillis = now,
                    paymentTransactionId = txId
                )
            )
            dao.putTransaction(
                TransactionEntity(
                    txId, "expense", it.amount,
                    "Tagihan ${it.provider}", accountId, now, it.itemName
                )
            )
        }
    }

    suspend fun unpayBill(billId: String) = db.withTransaction {
        val bill = dao.bill(billId) ?: error("Tagihan tidak ditemukan")
        require(bill.isPaid) { "Tagihan belum lunas" }
        val txId = bill.paymentTransactionId
        if (txId != null && dao.transaction(txId) != null) {

            deleteTransactionInternal(txId)
        } else {
            val accountId = bill.paidAccountId
            val account = accountId?.let { dao.account(it) }
            if (account != null) dao.putAccount(account.copy(balance = account.balance + bill.amount))
            dao.putBill(bill.copy(isPaid = false, paidAccountId = null, paidAtMillis = null, paymentTransactionId = null))
        }
    }

    suspend fun deleteBill(billId: String) = db.withTransaction {
        val family = dao.billFamily(billId)
        if (family.isEmpty()) return@withTransaction

        family.filter { it.isPaid }.forEach { bill ->
            val txId = bill.paymentTransactionId
            if (txId != null && dao.transaction(txId) != null) {
                deleteTransactionInternal(txId)
            } else {
                val account = bill.paidAccountId?.let { dao.account(it) }
                if (account != null) dao.putAccount(account.copy(balance = account.balance + bill.amount))
            }
        }
        family.forEach { dao.deleteBill(it.id) }
    }

    suspend fun addInvestment(name: String, target: Long?) = db.withTransaction {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Nama aset wajib diisi" }
        require(target == null || target > 0L) { "Target tidak valid" }
        dao.putInvestment(InvestmentEntity(id(), cleanName, target))
    }

    suspend fun updateInvestment(id: String, name: String, target: Long?) {
        val asset = dao.allInvestments().firstOrNull { it.id == id }
            ?: error("Aset tidak ditemukan")
        require(name.isNotBlank()) { "Nama aset wajib diisi" }
        dao.putInvestment(asset.copy(name = name.trim(), target = target?.takeIf { it > 0 }))
    }

    suspend fun contribute(assetId: String, accountId: String, amount: Long, adminFee: Long = 0L) = db.withTransaction {
        require(amount > 0) { "Nominal harus lebih dari nol" }
        require(adminFee >= 0) { "Biaya admin tidak valid" }
        val asset = dao.allInvestments().firstOrNull { it.id == assetId && !it.archived }
            ?: error("Aset tidak ditemukan")
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        require(account.balance >= amount + adminFee) { "Saldo rekening tidak cukup" }
        val txId = id()
        val now = System.currentTimeMillis()
        dao.putAccount(account.copy(balance = account.balance - amount - adminFee))
        dao.putInvestment(asset.copy(value = asset.value + amount))
        if (adminFee > 0) {
            dao.putTransaction(
                TransactionEntity(
                    id(), "expense", adminFee, "Admin bank", accountId, now,
                    "Biaya admin setor ke ${asset.name}", feeAssetId = assetId
                )
            )
        }
        dao.putTransaction(
            TransactionEntity(
                txId, "expense", amount, "Investasi", accountId, now,
                "Setor ke ${asset.name}", assetId
            )
        )
        dao.putContribution(
            InvestmentContributionEntity(id(), assetId, accountId, amount, txId, now)
        )
    }

    suspend fun addHolding(platformId: String, name: String, type: String, cost: Long) = db.withTransaction {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Nama aset wajib diisi" }
        require(type in HOLDING_TYPES) { "Jenis aset tidak valid" }
        require(cost > 0) { "Nominal harus lebih dari nol" }
        val platform = dao.allInvestments().firstOrNull { it.id == platformId && !it.archived }
            ?: error("Platform tidak ditemukan")
        val available = platform.value - dao.holdingsForPlatform(platformId).sumOf { it.cost }
        require(cost <= available) { "Saldo platform tidak cukup (tersedia $available)" }
        dao.putHolding(InvestmentHoldingEntity(id(), platformId, cleanName, type, cost, System.currentTimeMillis()))
    }

    suspend fun deleteHolding(holdingId: String) = db.withTransaction {
        dao.deleteHolding(holdingId)
    }

    suspend fun withdraw(assetId: String, accountId: String, amount: Long, adminFee: Long = 0L) = db.withTransaction {
        require(amount > 0) { "Nominal harus lebih dari nol" }
        require(adminFee >= 0) { "Biaya admin tidak valid" }
        require(adminFee <= amount) { "Biaya admin melebihi nominal tarik" }
        val asset = dao.allInvestments().firstOrNull { it.id == assetId && !it.archived }
            ?: error("Aset tidak ditemukan")
        val available = asset.value - dao.holdingsForPlatform(assetId).sumOf { it.cost }
        require(available >= amount) { "Saldo platform tidak cukup (tersedia $available)" }
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        val now = System.currentTimeMillis()
        dao.putAccount(account.copy(balance = account.balance + amount))
        dao.putInvestment(asset.copy(value = asset.value - amount))
        dao.putTransaction(
            TransactionEntity(
                id(), "income", amount, "Investasi", accountId, now,
                "Tarik dari ${asset.name}", assetId
            )
        )
        if (adminFee > 0) {
            dao.putAccount(account.copy(balance = account.balance + amount - adminFee))
            dao.putTransaction(
                TransactionEntity(
                    id(), "expense", adminFee, "Admin bank", accountId, now,
                    "Biaya admin tarik dari ${asset.name}", feeAssetId = assetId
                )
            )
        }

        var left = amount
        dao.contributions(assetId).sortedBy { it.createdAtMillis }.forEach { c ->
            if (left <= 0) return@forEach
            if (c.amount <= left) {
                left -= c.amount
                dao.deleteContribution(c.id)
            } else {
                dao.putContribution(c.copy(amount = c.amount - left))
                left = 0
            }
        }
        require(left == 0L) { "Data kontribusi investasi tidak konsisten" }
    }

    suspend fun deleteInvestment(assetId: String) = db.withTransaction {
        val asset = dao.allInvestments().firstOrNull { it.id == assetId } ?: return@withTransaction
        val contributions = dao.contributions(assetId)
        contributions.groupBy { it.accountId }.forEach { (accountId, lots) ->
            val total = lots.sumOf { it.amount }
            if (total > 0) {
                val account = dao.account(accountId) ?: error("Rekening kontribusi tidak ditemukan")
                dao.putAccount(account.copy(balance = account.balance + total))
            }
        }

        dao.feeTransactionsForInvestment(assetId).groupBy { it.accountId }.forEach { (accountId, fees) ->
            val account = dao.account(accountId) ?: error("Rekening biaya admin tidak ditemukan")
            dao.putAccount(account.copy(balance = account.balance + fees.sumOf { it.amount }))
            fees.forEach { dao.deleteTransaction(it.id) }
        }

        dao.transactionsForInvestment(assetId).forEach { transaction ->
            dao.deleteTransaction(transaction.id)
        }
        dao.deleteContributions(assetId)
        dao.deleteHoldingsForPlatform(assetId)
        dao.deleteSchedulesForAsset(assetId)
        dao.putInvestment(asset.copy(value = 0L, archived = true))
    }

    suspend fun addSchedule(assetId: String, accountId: String, amount: Long, dayOfWeek: Int) {
        require(amount > 0) { "Nominal harus lebih dari nol" }
        require(dayOfWeek in 1..7) { "Hari tidak valid" }
        val asset = dao.allInvestments().firstOrNull { it.id == assetId && !it.archived }
            ?: error("Aset tidak ditemukan")
        dao.account(accountId) ?: error("Rekening tidak ditemukan")
        dao.putSchedule(
            ScheduledInvestmentEntity(
                id = id(),
                assetId = assetId,
                accountId = accountId,
                amount = amount,
                dayOfWeek = dayOfWeek,
                enabled = true,
                lastRunMillis = null,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun setScheduleEnabled(scheduleId: String, enabled: Boolean) {
        val s = dao.allSchedules().firstOrNull { it.id == scheduleId } ?: return
        dao.putSchedule(s.copy(enabled = enabled))
    }

    suspend fun deleteSchedule(scheduleId: String) {
        dao.deleteSchedule(scheduleId)
    }

    data class SalaryRun(
        val note: String,
        val accountName: String,
        val amount: Long,
        val balanceAfter: Long,
        val frequency: String
    )

    data class InvestmentRun(
        val assetName: String,
        val accountName: String,
        val amount: Long
    )

    data class ScheduleRunResult(
        val investmentRuns: List<InvestmentRun>,
        val salaryRuns: List<SalaryRun>,
        val failures: List<String> = emptyList()
    )

    suspend fun processDueSchedules(
        now: Long = System.currentTimeMillis(),
        maxCatchupDays: Int = 30
    ): ScheduleRunResult {
        val investmentRuns = mutableListOf<InvestmentRun>()
        val salaryRuns = mutableListOf<SalaryRun>()
        val failures = mutableListOf<String>()
        val earliest = now - maxCatchupDays.coerceIn(1, 365) * 86_400_000L

        for (initial in dao.allSchedules()) {
            if (!initial.enabled) continue
            val asset = dao.allInvestments().firstOrNull { it.id == initial.assetId }
            var schedule = initial
            var dueAt = nextWeeklyDue(schedule, now)
            while (dueAt != null && dueAt <= now) {
                val occurrenceAt = dueAt
                if (occurrenceAt < earliest) {

                    dao.putSchedule(schedule.copy(lastRunMillis = earliest))
                    schedule = schedule.copy(lastRunMillis = earliest)
                    dueAt = nextWeeklyDue(schedule, now)
                    continue
                }
                try {
                    db.withTransaction {

                        val current = dao.allSchedules().firstOrNull { it.id == schedule.id }
                            ?: return@withTransaction
                        if (!current.enabled || (current.lastRunMillis ?: Long.MIN_VALUE) >= occurrenceAt) {
                            return@withTransaction
                        }
                        val currentAsset = dao.allInvestments().firstOrNull {
                            it.id == current.assetId && !it.archived
                        } ?: error("Aset tidak ditemukan")
                        val account = dao.account(current.accountId) ?: error("Rekening tidak ditemukan")
                        require(account.balance >= current.amount) { "Saldo rekening tidak cukup" }
                        val txId = id()
                        dao.putAccount(account.copy(balance = account.balance - current.amount))
                        dao.putInvestment(currentAsset.copy(value = currentAsset.value + current.amount))
                        dao.putTransaction(
                            TransactionEntity(txId, "expense", current.amount, "Investasi", current.accountId,
                                occurrenceAt, "Setor terjadwal ke ${currentAsset.name}", current.assetId)
                        )
                        dao.putContribution(
                            InvestmentContributionEntity(id(), current.assetId, current.accountId,
                                current.amount, txId, occurrenceAt)
                        )
                        dao.putSchedule(current.copy(lastRunMillis = occurrenceAt))
                    }

                    val updated = dao.allSchedules().firstOrNull { it.id == schedule.id }
                    if (updated != null && updated.lastRunMillis == occurrenceAt) {
                        val from = dao.account(schedule.accountId)
                        investmentRuns += InvestmentRun(asset?.name ?: "Investasi", from?.name ?: "Rekening", schedule.amount)
                        schedule = updated
                    } else {
                        schedule = updated ?: schedule
                    }
                } catch (e: Exception) {
                    failures += "${asset?.name ?: "Investasi"}: ${e.message ?: "gagal diproses"}"

                    break
                }
                dueAt = nextWeeklyDue(schedule, now)
            }
        }

        for (initial in dao.allSalaries()) {
            if (!initial.enabled) continue
            var schedule = initial
            var dueAt = nextSalaryDue(schedule, now)
            while (dueAt != null && dueAt <= now) {
                val occurrenceAt = dueAt
                if (occurrenceAt < earliest) {
                    dao.putSalary(schedule.copy(lastRunMillis = earliest))
                    schedule = schedule.copy(lastRunMillis = earliest)
                    dueAt = nextSalaryDue(schedule, now)
                    continue
                }
                try {
                    var inserted = false
                    db.withTransaction {
                        val current = dao.allSalaries().firstOrNull { it.id == schedule.id }
                            ?: return@withTransaction
                        if (!current.enabled || (current.lastRunMillis ?: Long.MIN_VALUE) >= occurrenceAt) {
                            return@withTransaction
                        }
                        val account = dao.account(current.accountId)
                            ?: error("Rekening tidak ditemukan")
                        dao.putAccount(account.copy(balance = account.balance + current.amount))
                        dao.putTransaction(
                            TransactionEntity(id(), "income", current.amount, "Gaji", current.accountId,
                                occurrenceAt, current.note.ifBlank { "Gaji" })
                        )
                        dao.putSalary(current.copy(lastRunMillis = occurrenceAt))
                        inserted = true
                    }
                    val updated = dao.allSalaries().firstOrNull { it.id == schedule.id }
                    if (inserted) {
                        val account = dao.account(schedule.accountId)
                        salaryRuns += SalaryRun(
                            note = schedule.note.ifBlank { "Gaji" },
                            accountName = account?.name ?: "Rekening",
                            amount = schedule.amount,
                            balanceAfter = account?.balance ?: 0L,
                            frequency = schedule.frequency
                        )
                    }
                    if (updated == null) break
                    schedule = updated
                } catch (e: Exception) {
                    failures += "${schedule.note.ifBlank { "Gaji" }}: ${e.message ?: "gagal diproses"}"
                    break
                }
                dueAt = nextSalaryDue(schedule, now)
            }
        }
        return ScheduleRunResult(investmentRuns, salaryRuns, failures)
    }

    private fun nextWeeklyDue(s: ScheduledInvestmentEntity, now: Long): Long? {
        val anchor = s.lastRunMillis ?: s.createdAtMillis.takeIf { it > 0 } ?: now
        val candidate = Calendar.getInstance().apply {
            timeInMillis = anchor
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (s.lastRunMillis != null) add(Calendar.DAY_OF_YEAR, 1)
            while (get(Calendar.DAY_OF_WEEK) != s.dayOfWeek) add(Calendar.DAY_OF_YEAR, 1)
        }

        if (s.lastRunMillis == null && candidate.get(Calendar.YEAR) == Calendar.getInstance().apply { timeInMillis = anchor }.get(Calendar.YEAR) &&
            candidate.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().apply { timeInMillis = anchor }.get(Calendar.DAY_OF_YEAR)) {
            return maxOf(anchor, candidate.timeInMillis).takeIf { it <= now }
        }
        return candidate.timeInMillis.takeIf { it <= now }
    }

    private fun nextSalaryDue(s: SalaryScheduleEntity, now: Long): Long? {
        val anchor = s.lastRunMillis ?: s.createdAtMillis.takeIf { it > 0 } ?: now
        val due = Calendar.getInstance().apply {
            timeInMillis = anchor
            set(Calendar.HOUR_OF_DAY, s.hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (s.lastRunMillis != null) {
            when (s.frequency) {
                "daily" -> due.add(Calendar.DAY_OF_YEAR, 1)
                "biweekly" -> due.add(Calendar.DAY_OF_YEAR, 14)
                "monthly" -> {
                    val targetDay = Calendar.getInstance().apply { timeInMillis = s.createdAtMillis }.get(Calendar.DAY_OF_MONTH)
                    due.set(Calendar.DAY_OF_MONTH, 1)
                    due.add(Calendar.MONTH, 1)
                    due.set(Calendar.DAY_OF_MONTH, minOf(targetDay, due.getActualMaximum(Calendar.DAY_OF_MONTH)))
                }
                else -> return null
            }
        } else if (due.timeInMillis < anchor) {

            when (s.frequency) {
                "daily" -> due.add(Calendar.DAY_OF_YEAR, 1)
                "biweekly" -> due.add(Calendar.DAY_OF_YEAR, 14)
                "monthly" -> {
                    val targetDay = Calendar.getInstance().apply { timeInMillis = s.createdAtMillis }.get(Calendar.DAY_OF_MONTH)
                    due.set(Calendar.DAY_OF_MONTH, 1)
                    due.add(Calendar.MONTH, 1)
                    due.set(Calendar.DAY_OF_MONTH, minOf(targetDay, due.getActualMaximum(Calendar.DAY_OF_MONTH)))
                }
                else -> return null
            }
        }
        return due.timeInMillis.takeIf { it <= now }
    }

    suspend fun addSalary(
        accountId: String,
        amount: Long,
        frequency: String,
        note: String = "Gaji",
        hourOfDay: Int = 9
    ) {
        require(amount > 0) { "Nominal harus lebih dari nol" }
        require(frequency in listOf("daily", "biweekly", "monthly")) { "Frekuensi tidak valid" }
        require(hourOfDay in 0..23) { "Jam tidak valid" }
        dao.account(accountId) ?: error("Rekening tidak ditemukan")
        dao.putSalary(
            SalaryScheduleEntity(
                id = id(),
                accountId = accountId,
                amount = amount,
                frequency = frequency,
                enabled = true,
                lastRunMillis = null,
                note = note.trim().ifBlank { "Gaji" },
                createdAtMillis = System.currentTimeMillis(),
                hourOfDay = hourOfDay
            )
        )
    }

    suspend fun setSalaryEnabled(id: String, enabled: Boolean) {
        val s = dao.allSalaries().firstOrNull { it.id == id } ?: return
        dao.putSalary(s.copy(enabled = enabled))
    }

    suspend fun updateSalary(
        id: String,
        accountId: String,
        amount: Long,
        frequency: String,
        note: String,
        hourOfDay: Int = 9
    ) {
        require(amount > 0) { "Nominal harus lebih dari nol" }
        require(frequency in listOf("daily", "biweekly", "monthly"))
        require(hourOfDay in 0..23) { "Jam tidak valid" }
        val s = dao.allSalaries().firstOrNull { it.id == id } ?: error("Jadwal gaji tidak ditemukan")
        dao.account(accountId) ?: error("Rekening tidak ditemukan")
        dao.putSalary(
            s.copy(
                accountId = accountId,
                amount = amount,
                frequency = frequency,
                note = note.trim().ifBlank { "Gaji" },
                hourOfDay = hourOfDay
            )
        )
    }

    suspend fun deleteSalary(id: String) {
        dao.deleteSalary(id)
    }

    suspend fun addDebt(type: String, person: String, description: String, amount: Long) {
        require(type == "piutang" || type == "utang")
        require(amount > 0)
        dao.putDebt(
            DebtEntity(
                id(), type, person.trim(), description.trim(), amount,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun payDebt(debtId: String, accountId: String, amount: Long) = db.withTransaction {
        val debt = dao.allDebts().firstOrNull { it.id == debtId } ?: error("Data tidak ditemukan")
        require(!debt.isPaid && debt.remaining > 0L) { "Sudah ditandai lunas" }
        require(amount > 0L) { "Nominal harus lebih dari nol" }
        require(amount <= debt.remaining) { "Nominal melebihi sisa yang harus dibayar" }
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        val now = System.currentTimeMillis()
        val txId = id()
        val newPaid = debt.paidAmount + amount
        val settled = newPaid >= debt.amount
        val isUtang = debt.type == "utang"
        val label = when {
            settled && debt.paidAmount == 0L -> if (isUtang) "Pelunasan utang" else "Pelunasan piutang"
            settled -> if (isUtang) "Pelunasan utang (cicilan terakhir)" else "Pelunasan piutang (cicilan terakhir)"
            else -> if (isUtang) "Cicilan utang" else "Cicilan piutang"
        }
        val note = if (debt.description.isBlank()) label else "$label · ${debt.description}"
        if (isUtang) {
            require(account.balance >= amount) { "Saldo rekening tidak cukup" }
            dao.putAccount(account.copy(balance = account.balance - amount))
        } else {
            dao.putAccount(account.copy(balance = account.balance + amount))
        }
        dao.putTransaction(
            TransactionEntity(
                txId, if (isUtang) "expense" else "income", amount,
                (if (isUtang) "Utang" else "Piutang") + " · ${debt.person}",
                accountId, now, note, debtId = debt.id
            )
        )
        dao.putDebt(
            debt.copy(
                paidAmount = newPaid,
                isPaid = settled,
                paidAtMillis = if (settled) now else null,
                paidAccountId = accountId,
                paymentTransactionId = txId
            )
        )
    }

    suspend fun markDebtPaid(debt: DebtEntity, accountId: String) =
        payDebt(debt.id, accountId, debt.remaining)

    suspend fun markDebtUnpaid(debt: DebtEntity) = db.withTransaction {
        val current = dao.allDebts().firstOrNull { it.id == debt.id } ?: return@withTransaction
        require(current.paidAmount > 0L) { "Belum ada pembayaran" }
        val last = dao.allTransactions().filter { it.debtId == current.id }.maxByOrNull { it.dateMillis }
        if (last != null) {
            deleteTransactionInternal(last.id)
        } else {
            val account = current.paidAccountId?.let { dao.account(it) }
            if (account != null) {
                val restored = if (current.type == "piutang") account.balance - current.paidAmount else account.balance + current.paidAmount
                dao.putAccount(account.copy(balance = restored))
            }
            dao.putDebt(current.copy(paidAmount = 0L, isPaid = false, paidAtMillis = null, paidAccountId = null, paymentTransactionId = null))
        }
    }

    suspend fun deleteDebt(debtId: String) = db.withTransaction {
        val debt = dao.allDebts().firstOrNull { it.id == debtId } ?: return@withTransaction
        val payments = dao.allTransactions().filter { it.debtId == debtId }
        if (payments.isNotEmpty()) {
            payments.forEach { deleteTransactionInternal(it.id) }
        } else if (debt.paidAmount > 0L) {
            val account = debt.paidAccountId?.let { dao.account(it) }
            if (account != null) {
                val restored = if (debt.type == "piutang") account.balance - debt.paidAmount else account.balance + debt.paidAmount
                dao.putAccount(account.copy(balance = restored))
            }
        }
        dao.deleteDebt(debtId)
    }

    suspend fun snapshot(): BackupData = db.withTransaction {
        BackupData(
            accounts = dao.allAccounts(),
            transactions = dao.allTransactions(),
            bills = dao.allBills(),
            investments = dao.allInvestments(),
            contributions = dao.allContributions(),
            debts = dao.allDebts(),
            schedules = dao.allSchedules(),
            salaries = dao.allSalaries(),
            holdings = dao.allHoldings()
        )
    }

    suspend fun exportJson(): String = BackupCodec.encode(snapshot())

    suspend fun importJson(json: String): BackupData = db.withTransaction {
        val decoded = if (BackupCodec.isLegacySakuMe(json)) {
            BackupCodec.decodeLegacySakuMe(json)
        } else {
            BackupCodec.decode(json)
        }
        val data = normalizeBills(normalizeDebts(decoded))
        validateBackup(data)
        dao.clearAccounts()
        dao.clearTransactions()
        dao.clearBills()
        dao.clearInvestments()
        dao.clearContributions()
        dao.clearHoldings()
        dao.clearDebts()
        dao.clearSchedules()
        dao.clearSalaries()
        dao.putAccounts(data.accounts)
        dao.putTransactions(data.transactions)
        dao.putBills(data.bills)
        dao.putInvestments(data.investments)
        dao.putContributions(data.contributions)
        dao.putHoldings(data.holdings)
        dao.putDebts(data.debts)
        dao.putSchedules(data.schedules)
        dao.putSalaries(data.salaries)
        data
    }
    private fun normalizeDebts(data: BackupData): BackupData {
        val txs = data.transactions.toMutableList()
        val debts = data.debts.map { d ->
            if (d.isPaid && d.paidAmount == 0L) {
                val idx = txs.indexOfFirst { it.id == d.paymentTransactionId && it.debtId == null }
                if (idx >= 0) txs[idx] = txs[idx].copy(debtId = d.id)
                d.copy(paidAmount = d.amount)
            } else d
        }
        return data.copy(transactions = txs, debts = debts)
    }

    private fun normalizeBills(data: BackupData): BackupData {
        val accountIds = data.accounts.map { it.id }.toSet()
        val txById = data.transactions.associateBy { it.id }
        val claimed = HashSet<String>()
        data.debts.mapNotNullTo(claimed) { it.paymentTransactionId }
        data.bills.forEach { b ->
            val tx = b.paymentTransactionId?.let { txById[it] }
            if (b.isPaid && tx != null && tx.type == "expense" && tx.amount == b.amount) claimed += tx.id
        }

        val bills = data.bills.map { bill ->
            if (!bill.isPaid) {
                return@map bill.copy(paidAccountId = null, paidAtMillis = null, paymentTransactionId = null)
            }
            val accId = bill.paidAccountId?.takeIf { it in accountIds }
            val linked = bill.paymentTransactionId?.let { txById[it] }
                ?.takeIf { it.type == "expense" && it.amount == bill.amount && it.debtId == null }

            val tx = linked ?: data.transactions
                .asSequence()
                .filter {
                    it.type == "expense" && it.amount == bill.amount && it.debtId == null &&
                        it.transferGroupId == null && it.id !in claimed &&
                        it.category == "Tagihan ${bill.provider}" &&
                        (accId == null || it.accountId == accId)
                }
                .minByOrNull { Math.abs(it.dateMillis - (bill.paidAtMillis ?: bill.dueDateMillis)) }
                ?.also { claimed += it.id }

            bill.copy(
                paidAccountId = tx?.accountId ?: accId,
                paidAtMillis = bill.paidAtMillis ?: tx?.dateMillis ?: bill.dueDateMillis,
                paymentTransactionId = tx?.id
            )
        }
        return data.copy(bills = bills)
    }

    private fun validateBackup(data: BackupData) {
        val accountIds = data.accounts.map { it.id }.toSet()
        val assetIds = data.investments.map { it.id }.toSet()
        val transactionIds = data.transactions.map { it.id }.toSet()
        require(accountIds.size == data.accounts.size) { "Backup memiliki ID rekening duplikat" }
        require(assetIds.size == data.investments.size) { "Backup memiliki ID investasi duplikat" }
        require(transactionIds.size == data.transactions.size) { "Backup memiliki ID transaksi duplikat" }
        require(data.accounts.all { it.name.isNotBlank() && it.balance >= 0L }) { "Backup memiliki rekening tidak valid" }
        require(data.transactions.all {
            it.amount > 0L && it.type in setOf("income", "expense", "transfer_in", "transfer_out") &&
                it.accountId in accountIds && it.dateMillis > 0L
        }) { "Backup memiliki transaksi tidak valid" }
        val groupedTransfers = data.transactions.filter { it.transferGroupId != null }.groupBy { it.transferGroupId }
        require(groupedTransfers.values.all { group ->
            group.size == 2 &&
                group.count { it.type == "transfer_out" } == 1 &&
                group.count { it.type == "transfer_in" } == 1 &&
                group.map { it.amount }.distinct().size == 1
        }) { "Backup memiliki transfer yang tidak berpasangan" }
        require(data.contributions.all {
            it.amount > 0L && it.accountId in accountIds && it.assetId in assetIds && it.transactionId.isNotBlank()
        }) { "Backup memiliki kontribusi investasi yang tidak valid" }
        require(data.holdings.all {
            it.cost > 0L && it.platformId in assetIds && it.type in HOLDING_TYPES && it.name.isNotBlank()
        }) { "Backup memiliki aset investasi yang tidak valid" }
        require(data.transactions.all { it.investmentAssetId == null || it.investmentAssetId in assetIds }) { "Backup memiliki transaksi investasi yang tidak valid" }
        require(data.schedules.all {
            it.amount > 0L && it.accountId in accountIds && it.assetId in assetIds && it.dayOfWeek in 1..7
        }) { "Backup memiliki jadwal investasi yang tidak valid" }
        require(data.salaries.all {
            it.amount > 0L && it.accountId in accountIds && it.frequency in setOf("daily", "biweekly", "monthly") && it.hourOfDay in 0..23
        }) { "Backup memiliki jadwal gaji yang tidak valid" }
        require(data.bills.all {
            it.amount > 0L && it.tenorMonths > 0 && it.installmentIndex in 1..it.tenorMonths &&
                (it.paidAccountId == null || it.paidAccountId in accountIds)
        }) { "Backup memiliki tagihan dengan nominal/rekening pembayaran tidak valid" }
        require(data.bills.all { bill ->
            if (!bill.isPaid) {
                bill.paidAccountId == null && bill.paidAtMillis == null && bill.paymentTransactionId == null
            } else {
                val txId = bill.paymentTransactionId
                val tx = txId?.let { id -> data.transactions.firstOrNull { it.id == id } }
                bill.paidAtMillis != null &&
                    (txId == null || (tx != null && tx.type == "expense" && tx.amount == bill.amount &&
                        (bill.paidAccountId == null || tx.accountId == bill.paidAccountId)))
            }
        }) { "Backup memiliki status pembayaran tagihan yang tidak konsisten" }
        val debtIds = data.debts.map { it.id }.toSet()
        require(debtIds.size == data.debts.size) { "Backup memiliki ID utang/piutang duplikat" }
        require(data.transactions.all { it.debtId == null || it.debtId in debtIds }) {
            "Backup memiliki transaksi yang terhubung ke utang/piutang tidak dikenal"
        }
        require(data.debts.all {
            it.amount > 0L && it.type in setOf("utang", "piutang") &&
                it.paidAmount in 0L..it.amount && it.isPaid == (it.paidAmount == it.amount) &&
                (it.paidAccountId == null || it.paidAccountId in accountIds)
        }) { "Backup memiliki utang/piutang tidak valid" }
        require(data.debts.all { debt ->
            val expectedType = if (debt.type == "utang") "expense" else "income"
            val payments = data.transactions.filter { it.debtId == debt.id }
            payments.all { it.type == expectedType } && payments.sumOf { it.amount } <= debt.paidAmount
        }) { "Backup memiliki status pembayaran utang/piutang yang tidak konsisten" }
    }

}
