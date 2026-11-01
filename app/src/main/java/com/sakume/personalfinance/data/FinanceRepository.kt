package com.sakume.personalfinance.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class FinanceRepository(private val db: AppDatabase) {
    private val dao = db.dao()
    val accounts: Flow<List<AccountEntity>> = dao.accounts()
    val transactions: Flow<List<TransactionEntity>> = dao.transactions()
    val bills: Flow<List<BillEntity>> = dao.bills()
    val investments: Flow<List<InvestmentEntity>> = dao.investments()
    val debts: Flow<List<DebtEntity>> = dao.debts()
    val schedules: Flow<List<ScheduledInvestmentEntity>> = dao.schedules()
    val salaries: Flow<List<SalaryScheduleEntity>> = dao.salaries()
    private fun id() = UUID.randomUUID().toString()

    // ── Accounts ──────────────────────────────────────────────────────────

    suspend fun addAccount(name: String, type: String, opening: Long) = db.withTransaction {
        val order = dao.maxAccountSortOrder() + 1
        dao.putAccount(AccountEntity(id(), name.trim(), type, opening, openingBalance = opening, sortOrder = order))
    }

    suspend fun deleteAccount(accountId: String) = db.withTransaction {
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        require(account.balance == 0L) {
            "Saldo masih ${account.balance}. Transfer atau kosongkan dulu sebelum menghapus."
        }
        // Jangan pernah menghapus histori rekening secara sepihak karena transaksi
        // transfer/debt/investment dapat mempunyai pasangan atau state di entitas lain.
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
        orderedIds.forEachIndexed { index, id ->
            val acc = all[id] ?: return@forEachIndexed
            if (acc.sortOrder != index) {
                dao.putAccount(acc.copy(sortOrder = index))
            }
        }
    }

    /**
     * Hitung ulang semua saldo rekening dari transaksi yang ada.
     * Berguna setelah import / recovery data yang mungkin inkonsisten.
     */
    suspend fun recalculateBalances() = db.withTransaction {
        val allTx = dao.allTransactions()
        val accounts = dao.allAccounts()
        // Saldo harus dimulai dari openingBalance, bukan nol. Ini menjaga saldo awal
        // rekening tetap utuh ketika user menjalankan recovery/recalculate.
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
    }

    // ── Transactions ──────────────────────────────────────────────────────

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
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        if (type == "expense") require(account.balance >= amount) { "Saldo rekening tidak cukup" }
        dao.putAccount(account.copy(balance = account.balance + if (type == "income") amount else -amount))
        dao.putTransaction(TransactionEntity(id(), type, amount, category, accountId, date, note))
    }

    suspend fun deleteTransactions(txIds: List<String>) {
        // Proses satu per satu dalam transaksi terpisah agar partial success aman
        txIds.distinct().forEach { deleteTransaction(it) }
    }

    suspend fun deleteTransaction(txId: String) = db.withTransaction {
        val tx = dao.transaction(txId) ?: return@withTransaction
        val group = tx.transferGroupId
        val toReverse = if (group != null) dao.transactionsByGroup(group) else listOf(tx)
        for (item in toReverse) {
            val account = dao.account(item.accountId)
            if (account != null) {
                val newBal = when (item.type) {
                    "income", "transfer_in" -> account.balance - item.amount
                    "expense", "transfer_out" -> account.balance + item.amount
                    else -> account.balance
                }
                dao.putAccount(account.copy(balance = newBal))
            }

            // Payment transaction adalah sumber kebenaran untuk state bill/debt.
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
            dao.allDebts().firstOrNull { it.paymentTransactionId == item.id }?.let { debt ->
                dao.putDebt(
                    debt.copy(
                        isPaid = false,
                        paidAtMillis = null,
                        paidAccountId = null,
                        paymentTransactionId = null
                    )
                )
            }

            if (item.investmentAssetId != null && item.type == "expense") {
                val asset = dao.allInvestments().firstOrNull { it.id == item.investmentAssetId }
                if (asset != null && !asset.archived) {
                    dao.putInvestment(asset.copy(value = (asset.value - item.amount).coerceAtLeast(0)))
                }
                dao.allContributions().filter { it.transactionId == item.id }
                    .forEach { dao.deleteContribution(it.id) }
            }
            if (item.investmentAssetId != null && item.type == "income") {
                val asset = dao.allInvestments().firstOrNull { it.id == item.investmentAssetId }
                if (asset != null && !asset.archived) {
                    dao.putInvestment(asset.copy(value = asset.value + item.amount))
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

    // ── Bills ─────────────────────────────────────────────────────────────

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
                "Tagihan • ${bill.provider}", accountId, now, bill.itemName
            )
        )
    }

    suspend fun payBills(billsToPay: List<BillEntity>, accountId: String) = db.withTransaction {
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        val unpaid = billsToPay.filterNot { it.isPaid }
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
                    "Tagihan • ${it.provider}", accountId, now, it.itemName
                )
            )
        }
    }

    /** Batalkan pembayaran tagihan tanpa double-reverse saldo. */
    suspend fun unpayBill(billId: String) = db.withTransaction {
        val bill = dao.bill(billId) ?: error("Tagihan tidak ditemukan")
        require(bill.isPaid) { "Tagihan belum lunas" }
        val txId = bill.paymentTransactionId
        if (txId != null && dao.transaction(txId) != null) {
            // deleteTransaction() sudah membalik saldo dan mereset bill berdasarkan paymentTransactionId.
            deleteTransaction(txId)
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
        // Reverse semua pembayaran melalui deleteTransaction agar saldo dan state bill
        // dipulihkan tepat sekali. Setelah itu hapus seluruh family cicilan.
        family.filter { it.isPaid }.forEach { bill ->
            val txId = bill.paymentTransactionId
            if (txId != null && dao.transaction(txId) != null) {
                deleteTransaction(txId)
            } else {
                val account = bill.paidAccountId?.let { dao.account(it) }
                if (account != null) dao.putAccount(account.copy(balance = account.balance + bill.amount))
            }
        }
        family.forEach { dao.deleteBill(it.id) }
    }

    // ── Investments ───────────────────────────────────────────────────────

    suspend fun addInvestment(name: String, target: Long?) {
        dao.putInvestment(InvestmentEntity(id(), name.trim(), target))
    }

    suspend fun updateInvestment(id: String, name: String, target: Long?) {
        val asset = dao.allInvestments().firstOrNull { it.id == id }
            ?: error("Aset tidak ditemukan")
        require(name.isNotBlank()) { "Nama aset wajib diisi" }
        dao.putInvestment(asset.copy(name = name.trim(), target = target?.takeIf { it > 0 }))
    }

    suspend fun contribute(assetId: String, accountId: String, amount: Long) = db.withTransaction {
        require(amount > 0) { "Nominal harus lebih dari nol" }
        val asset = dao.allInvestments().firstOrNull { it.id == assetId && !it.archived }
            ?: error("Aset tidak ditemukan")
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        require(account.balance >= amount) { "Saldo rekening tidak cukup" }
        val txId = id()
        val now = System.currentTimeMillis()
        dao.putAccount(account.copy(balance = account.balance - amount))
        dao.putInvestment(asset.copy(value = asset.value + amount))
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

    suspend fun withdraw(assetId: String, accountId: String, amount: Long) = db.withTransaction {
        require(amount > 0) { "Nominal harus lebih dari nol" }
        val asset = dao.allInvestments().firstOrNull { it.id == assetId && !it.archived }
            ?: error("Aset tidak ditemukan")
        require(asset.value >= amount) { "Nilai investasi tidak cukup (tersedia ${asset.value})" }
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
        // Kurangi kontribusi (FIFO) agar refund saat hapus tetap akurat
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
    }

    /**
     * Hapus/arsip investasi:
     * 1. Refund kontribusi ke rekening asal
     * 2. Hapus transaksi expense kontribusi terkait (supaya history bersih)
     * 3. Hapus jadwal
     * 4. Archive asset (value = 0)
     */
    suspend fun deleteInvestment(assetId: String) = db.withTransaction {
        val asset = dao.allInvestments().firstOrNull { it.id == assetId } ?: return@withTransaction
        // Refund kontribusi
        dao.contributions(assetId).groupBy { it.accountId }.forEach { (accountId, contributions) ->
            val total = contributions.sumOf { it.amount }
            val account = dao.account(accountId)
            if (account != null) {
                dao.putAccount(account.copy(balance = account.balance + total))
            }
        }
        // Hapus transaksi expense yang berasal dari kontribusi (bukan withdraw)
        dao.transactionsForInvestment(assetId)
            .filter { it.type == "expense" }
            .forEach { dao.deleteTransaction(it.id) }
        dao.deleteContributions(assetId)
        dao.deleteSchedulesForAsset(assetId)
        dao.putInvestment(asset.copy(value = 0, archived = true))
    }

    // ── Schedules ─────────────────────────────────────────────────────────

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

    data class ScheduleRunResult(
        val investmentRuns: List<String>,
        val salaryRuns: List<String>,
        val failures: List<String> = emptyList()
    )

    /**
     * @param maxCatchupDays jangan proses occurrence lebih lama dari N hari ke belakang
     *        (mencegah ratusan transaksi retroaktif jika app tidak dibuka berbulan-bulan).
     */
    suspend fun processDueSchedules(
        now: Long = System.currentTimeMillis(),
        maxCatchupDays: Int = 30
    ): ScheduleRunResult {
        val investmentRuns = mutableListOf<String>()
        val salaryRuns = mutableListOf<String>()
        val failures = mutableListOf<String>()
        val earliest = now - maxCatchupDays.coerceIn(1, 365) * 86_400_000L
        // Kejar setiap jadwal mingguan yang terlewat. lastRunMillis menyimpan tanggal
        // jadwal yang benar-benar diproses, bukan waktu aplikasi kebetulan dibuka.
        for (initial in dao.allSchedules()) {
            if (!initial.enabled) continue
            val asset = dao.allInvestments().firstOrNull { it.id == initial.assetId }
            var schedule = initial
            var dueAt = nextWeeklyDue(schedule, now)
            while (dueAt != null && dueAt <= now) {
                val occurrenceAt = dueAt
                if (occurrenceAt < earliest) {
                    // Loncat ke batas catch-up: tandai lastRun agar tidak mengulang masa lalu jauh.
                    dao.putSchedule(schedule.copy(lastRunMillis = earliest))
                    schedule = schedule.copy(lastRunMillis = earliest)
                    dueAt = nextWeeklyDue(schedule, now)
                    continue
                }
                try {
                    db.withTransaction {
                        // Baca ulang di dalam transaksi agar worker dan pembukaan app
                        // tidak membuat transaksi yang sama dua kali.
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
                    // Cek ulang state: bila transaksi ini sudah diproses worker lain,
                    // jangan laporkan sebagai transaksi baru.
                    val updated = dao.allSchedules().firstOrNull { it.id == schedule.id }
                    if (updated != null && updated.lastRunMillis == occurrenceAt) {
                        investmentRuns += "${asset?.name ?: "Investasi"} · Rp${schedule.amount}"
                        schedule = updated
                    } else {
                        schedule = updated ?: schedule
                    }
                } catch (e: Exception) {
                    failures += "${asset?.name ?: "Investasi"}: ${e.message ?: "gagal diproses"}"
                    // Jangan mengulang loop tanpa akhir bila saldo kurang atau data rusak.
                    break
                }
                dueAt = nextWeeklyDue(schedule, now)
            }
        }

        // Gaji juga mengejar semua periode jatuh tempo (harian, 14 harian, bulanan).
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
                        salaryRuns += "${schedule.note.ifBlank { "Gaji" }} → ${account?.name ?: "Rekening"} · Rp${schedule.amount}"
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

    /** Menghasilkan tanggal jatuh tempo mingguan berikutnya setelah run terakhir. */
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
        // Jadwal baru boleh dijalankan pada hari pembuatannya, walau dibuat setelah tengah malam.
        if (s.lastRunMillis == null && candidate.get(Calendar.YEAR) == Calendar.getInstance().apply { timeInMillis = anchor }.get(Calendar.YEAR) &&
            candidate.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().apply { timeInMillis = anchor }.get(Calendar.DAY_OF_YEAR)) {
            return maxOf(anchor, candidate.timeInMillis).takeIf { it <= now }
        }
        return candidate.timeInMillis.takeIf { it <= now }
    }

    /** Tanggal gaji berikutnya berdasarkan anchor jadwal, bukan waktu app dijalankan. */
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
            // Jika jadwal baru dibuat setelah jam gajian hari ini, mulai dari periode berikutnya.
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

    // ── Salary ────────────────────────────────────────────────────────────

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

    // ── Debts ─────────────────────────────────────────────────────────────

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

    /**
     * Tandai piutang/utang lunas dan update saldo rekening.
     * - Piutang (orang bayar ke kita) → income, saldo naik
     * - Utang (kita bayar ke orang) → expense, saldo turun
     * Menyimpan paymentTransactionId agar unpay akurat.
     */
    suspend fun markDebtPaid(debt: DebtEntity, accountId: String) = db.withTransaction {
        require(!debt.isPaid) { "Sudah ditandai lunas" }
        val account = dao.account(accountId) ?: error("Rekening tidak ditemukan")
        val now = System.currentTimeMillis()
        val txId = id()
        if (debt.type == "utang") {
            require(account.balance >= debt.amount) { "Saldo rekening tidak cukup" }
            dao.putAccount(account.copy(balance = account.balance - debt.amount))
            dao.putTransaction(
                TransactionEntity(
                    txId, "expense", debt.amount,
                    "Utang · ${debt.person}",
                    accountId, now,
                    debt.description.ifBlank { "Pelunasan utang" }
                )
            )
        } else {
            dao.putAccount(account.copy(balance = account.balance + debt.amount))
            dao.putTransaction(
                TransactionEntity(
                    txId, "income", debt.amount,
                    "Piutang · ${debt.person}",
                    accountId, now,
                    debt.description.ifBlank { "Pelunasan piutang" }
                )
            )
        }
        dao.putDebt(
            debt.copy(
                isPaid = true,
                paidAtMillis = now,
                paidAccountId = accountId,
                paymentTransactionId = txId
            )
        )
    }

    suspend fun markDebtUnpaid(debt: DebtEntity) = db.withTransaction {
        require(debt.isPaid) { "Belum lunas" }
        val txId = debt.paymentTransactionId
        if (txId != null && dao.transaction(txId) != null) {
            deleteTransaction(txId)
        } else {
            val account = debt.paidAccountId?.let { dao.account(it) }
            if (account != null) {
                val restored = if (debt.type == "piutang") account.balance - debt.amount else account.balance + debt.amount
                dao.putAccount(account.copy(balance = restored))
            }
            dao.putDebt(debt.copy(isPaid = false, paidAtMillis = null, paidAccountId = null, paymentTransactionId = null))
        }
    }

    suspend fun deleteDebt(debtId: String) = db.withTransaction {
        val debt = dao.allDebts().firstOrNull { it.id == debtId } ?: return@withTransaction
        val txId = debt.paymentTransactionId
        if (debt.isPaid && txId != null && dao.transaction(txId) != null) {
            deleteTransaction(txId)
        } else if (debt.isPaid) {
            val account = debt.paidAccountId?.let { dao.account(it) }
            if (account != null) {
                val restored = if (debt.type == "piutang") account.balance - debt.amount else account.balance + debt.amount
                dao.putAccount(account.copy(balance = restored))
            }
        }
        dao.deleteDebt(debtId)
    }

    // ── Backup ────────────────────────────────────────────────────────────

    suspend fun exportJson(): String {
        val data = BackupData(
            dao.allAccounts(), dao.allTransactions(), dao.allBills(), dao.allInvestments(),
            dao.allContributions(), dao.allDebts(), dao.allSchedules(), dao.allSalaries()
        )
        return BackupCodec.encode(data)
    }

    suspend fun importJson(json: String) = db.withTransaction {
        val data = if (BackupCodec.isLegacySakuMe(json)) {
            BackupCodec.decodeLegacySakuMe(json)
        } else {
            BackupCodec.decode(json)
        }
        validateBackup(data)
        dao.clearAccounts()
        dao.clearTransactions()
        dao.clearBills()
        dao.clearInvestments()
        dao.clearContributions()
        dao.clearDebts()
        dao.clearSchedules()
        dao.clearSalaries()
        dao.putAccounts(data.accounts)
        dao.putTransactions(data.transactions)
        dao.putBills(data.bills)
        dao.putInvestments(data.investments)
        dao.putContributions(data.contributions)
        dao.putDebts(data.debts)
        dao.putSchedules(data.schedules)
        dao.putSalaries(data.salaries)
    }
    private fun validateBackup(data: BackupData) {
        val accountIds = data.accounts.map { it.id }.toSet()
        require(accountIds.size == data.accounts.size) { "Backup memiliki ID rekening duplikat" }
        require(data.transactions.all { it.accountId in accountIds }) { "Backup memiliki transaksi dengan rekening yang tidak ada" }
        val groupedTransfers = data.transactions.filter { it.transferGroupId != null }.groupBy { it.transferGroupId }
        require(groupedTransfers.values.all { group ->
            group.size == 2 && group.count { it.type == "transfer_out" } == 1 && group.count { it.type == "transfer_in" } == 1 &&
                group[0].amount == group[1].amount
        }) { "Backup memiliki transfer yang tidak berpasangan" }
        require(data.contributions.all { it.accountId in accountIds }) { "Backup memiliki kontribusi dengan rekening yang tidak ada" }
        val assetIds = data.investments.map { it.id }.toSet()
        require(assetIds.size == data.investments.size) { "Backup memiliki ID investasi duplikat" }
        require(data.contributions.all { it.assetId in assetIds }) { "Backup memiliki kontribusi dengan investasi yang tidak ada" }
        require(data.transactions.all { it.investmentAssetId == null || it.investmentAssetId in assetIds }) { "Backup memiliki transaksi investasi yang tidak valid" }
        require(data.schedules.all { it.accountId in accountIds && it.assetId in assetIds }) { "Backup memiliki jadwal investasi yang tidak valid" }
        require(data.salaries.all { it.accountId in accountIds }) { "Backup memiliki jadwal gaji dengan rekening yang tidak ada" }
        require(data.bills.all {
            it.amount > 0 && (it.paidAccountId == null || it.paidAccountId in accountIds)
        }) { "Backup memiliki tagihan dengan nominal/rekening pembayaran tidak valid" }
        val transactionIds = data.transactions.map { it.id }.toSet()
        require(data.bills.all { it.paymentTransactionId == null || it.paymentTransactionId in transactionIds }) { "Backup memiliki referensi pembayaran tagihan yang tidak valid" }
        require(data.debts.all {
            it.amount > 0 && (it.type == "utang" || it.type == "piutang") &&
                (it.paidAccountId == null || it.paidAccountId in accountIds)
        }) { "Backup memiliki utang/piutang tidak valid" }
        require(data.debts.all { it.paymentTransactionId == null || it.paymentTransactionId in transactionIds }) { "Backup memiliki referensi pembayaran utang/piutang yang tidak valid" }
    }

}
