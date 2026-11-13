package com.sakume.personalfinance.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sakume.personalfinance.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FinanceRepository(AppDatabase.get(app))
    val accounts = repo.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repo.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bills = repo.bills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val investments = repo.investments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val holdings = repo.holdings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts = repo.debts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val schedules = repo.schedules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val salaries = repo.salaries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                repo.cleanupCategoryLabels()
            } catch (_: Exception) {
            }
        }
    }

    fun run(action: suspend FinanceRepository.() -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.action()
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun addAccount(name: String, type: String, balance: Long, error: (String) -> Unit) =
        run({ addAccount(name, type, balance) }, error)
        
    fun renameAccount(accountId: String, newName: String, error: (String) -> Unit) =
        run({ renameAccount(accountId, newName) }, error)    
        
    fun deleteAccount(accountId: String, error: (String) -> Unit) =
        run({ deleteAccount(accountId) }, error)

    fun reorderAccounts(orderedIds: List<String>) =
        run({ reorderAccounts(orderedIds) })

    fun recalculateBalances(onDone: () -> Unit = {}, error: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.recalculateBalances()
                onDone()
            } catch (e: Exception) {
                error(e.message ?: "Gagal menghitung ulang saldo")
            }
        }
    }

    fun deleteTransaction(id: String, error: (String) -> Unit = {}) = run({ deleteTransaction(id) }, error)

    fun deleteTransactions(ids: List<String>, error: (String) -> Unit = {}) =
        run({ deleteTransactions(ids) }, error)

    fun addTransaction(
        type: String,
        amount: Long,
        category: String,
        accountId: String,
        date: Long,
        note: String,
        error: (String) -> Unit
    ) = run({ addTransaction(type, amount, category, accountId, date, note) }, error)

    fun transfer(from: String, to: String, amount: Long, note: String = "", error: (String) -> Unit) =
        run({ transfer(from, to, amount, note) }, error)

    fun addBill(provider: String, item: String, amount: Long, due: Long, tenor: Int, error: (String) -> Unit) =
        run({ addBill(provider, item, amount, due, tenor) }, error)

    fun payBill(bill: BillEntity, accountId: String, error: (String) -> Unit) =
        run({ payBill(bill.id, accountId) }, error)

    fun payBills(list: List<BillEntity>, accountId: String, error: (String) -> Unit) =
        run({ payBills(list, accountId) }, error)

    fun unpayBill(bill: BillEntity, error: (String) -> Unit = {}) =
        run({ unpayBill(bill.id) }, error)

    fun deleteBill(bill: BillEntity) = run({ deleteBill(bill.id) })

    fun addInvestment(name: String, target: Long?) = run({ addInvestment(name, target) })

    fun updateInvestment(id: String, name: String, target: Long?, error: (String) -> Unit = {}) =
        run({ updateInvestment(id, name, target) }, error)

    fun contribute(asset: String, account: String, amount: Long, adminFee: Long = 0L, error: (String) -> Unit) =
        run({ contribute(asset, account, amount, adminFee) }, error)

    fun withdraw(asset: String, account: String, amount: Long, adminFee: Long = 0L, error: (String) -> Unit) =
        run({ withdraw(asset, account, amount, adminFee) }, error)

    fun deleteInvestment(id: String) = run({ deleteInvestment(id) })

    fun addHolding(platformId: String, name: String, type: String, cost: Long, error: (String) -> Unit) =
        run({ addHolding(platformId, name, type, cost) }, error)

    fun deleteHolding(id: String) = run({ deleteHolding(id) })

    fun addSchedule(assetId: String, accountId: String, amount: Long, dayOfWeek: Int, error: (String) -> Unit) =
        run({ addSchedule(assetId, accountId, amount, dayOfWeek) }, error)

    fun setScheduleEnabled(id: String, enabled: Boolean) = run({ setScheduleEnabled(id, enabled) })

    fun deleteSchedule(id: String) = run({ deleteSchedule(id) })

    fun processDueSchedules(onDone: (FinanceRepository.ScheduleRunResult) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val days = AppPrefs.maxCatchupDays(getApplication())
                onDone(repo.processDueSchedules(maxCatchupDays = days))
            } catch (_: Exception) {
            }
        }
    }

    fun withdrawCash(fromAccountId: String, toCashAccountId: String, amount: Long, note: String = "Tarik tunai", error: (String) -> Unit) =
        run({ transfer(fromAccountId, toCashAccountId, amount, note) }, error)

    fun depositCash(fromCashAccountId: String, toAccountId: String, amount: Long, note: String = "Setor tunai", error: (String) -> Unit) =
        run({ transfer(fromCashAccountId, toAccountId, amount, note) }, error)

    fun addSalary(accountId: String, amount: Long, frequency: String, note: String, hourOfDay: Int, error: (String) -> Unit) =
        run({ addSalary(accountId, amount, frequency, note, hourOfDay) }, error)

    fun setSalaryEnabled(id: String, enabled: Boolean) = run({ setSalaryEnabled(id, enabled) })

    fun updateSalary(id: String, accountId: String, amount: Long, frequency: String, note: String, hourOfDay: Int, error: (String) -> Unit) =
        run({ updateSalary(id, accountId, amount, frequency, note, hourOfDay) }, error)

    fun deleteSalary(id: String) = run({ deleteSalary(id) })

    fun addDebt(type: String, person: String, desc: String, amount: Long, error: (String) -> Unit) =
        run({ addDebt(type, person, desc, amount) }, error)

    fun payDebt(debtId: String, accountId: String, amount: Long, error: (String) -> Unit = {}) =
        run({ payDebt(debtId, accountId, amount) }, error)

    fun markDebtPaid(debt: DebtEntity, accountId: String, error: (String) -> Unit = {}) =
        run({ markDebtPaid(debt, accountId) }, error)

    fun markDebtUnpaid(debt: DebtEntity, error: (String) -> Unit = {}) =
        run({ markDebtUnpaid(debt) }, error)

    suspend fun exportJson() = BackupManager.buildFullJson(getApplication())

    /**
     * Impor / restore. Sebelum data diganti, kondisi saat ini disimpan otomatis ke daftar backup
     * (tipe "Sebelum impor") sehingga restore bisa dibatalkan. Pengaturan di file ikut dipulihkan.
     */
    fun importJson(json: String, onDone: (String) -> Unit = {}, error: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                BackupManager.savePreImport(app, repo.exportJson())
                val imported = repo.importJson(json)
                BackupManager.applySettings(app, imported.settings)
                onDone("Kondisi sebelum impor tersimpan di daftar backup (\"Sebelum impor\").")
            } catch (e: Exception) {
                error(e.message ?: "Gagal mengimpor backup")
            }
        }
    }

    /** Tombol "Backup sekarang": selalu menulis file, tanpa melihat saklar otomatis. */
    fun backupNow(onDone: (BackupManager.Outcome) -> Unit) {
        viewModelScope.launch { onDone(BackupManager.runBackup(getApplication(), force = true, source = "manual")) }
    }
}
