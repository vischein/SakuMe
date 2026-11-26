package com.sakume.personalfinance.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sakume.personalfinance.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Status pembukaan database (berat: Keystore, migrasi, SQLCipher), dikerjakan di luar main thread. */
sealed interface DbState {
    data object Loading : DbState
    data object Ready : DbState
    data class Failed(val message: String) : DbState
}

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val _dbState = MutableStateFlow<DbState>(DbState.Loading)
    val dbState: StateFlow<DbState> = _dbState

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors: SharedFlow<String> = _errors

    private val repoFlow = MutableStateFlow<FinanceRepository?>(null)

    private fun report(message: String) {
        _errors.tryEmit(message)
    }

    /** Hanya dipakai setelah [dbState] = Ready (UI baru ditampilkan saat itu). */
    private val repo: FinanceRepository
        get() = repoFlow.value ?: throw IllegalStateException("Database belum siap")

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> listFrom(source: (FinanceRepository) -> Flow<List<T>>): StateFlow<List<T>> =
        repoFlow
            .flatMapLatest { r -> if (r == null) flowOf(emptyList()) else source(r) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts = listFrom { it.accounts }
    val transactions = listFrom { it.transactions }
    val bills = listFrom { it.bills }
    val investments = listFrom { it.investments }
    val holdings = listFrom { it.holdings }
    val debts = listFrom { it.debts }
    val schedules = listFrom { it.schedules }
    val salaries = listFrom { it.salaries }
    val categories = listFrom { it.categories }

    private var openJob: Job? = null

    init {
        openDatabase()
    }

    /** Membuka database di thread IO. Aman dipanggil ulang ("Coba lagi") setelah gagal. */
    fun openDatabase() {
        if (openJob?.isActive == true || _dbState.value == DbState.Ready) return
        _dbState.value = DbState.Loading
        openJob = viewModelScope.launch {
            try {
                val db = withContext(Dispatchers.IO) { AppDatabase.open(getApplication()) }
                val opened = FinanceRepository(db)
                repoFlow.value = opened
                _dbState.value = DbState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _dbState.value = DbState.Failed(
                    when (e) {
                        is MasterKeyUnavailableException, is MigrationFailedException ->
                            e.message ?: "Database tidak bisa dibuka."
                        else ->
                            "Database SakuMe tidak bisa dibuka. SakuMe tidak menghapus atau memindahkan file " +
                                "database saat gagal seperti ini. Detail teknis: " +
                                (e.message ?: e.javaClass.simpleName)
                    }
                )
            }
        }
    }

    fun run(action: suspend FinanceRepository.() -> Unit, onError: (String) -> Unit = { report(it) }) {
        viewModelScope.launch {
            try {
                repo.action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun addAccount(name: String, type: String, balance: Long, creditLimit: Long? = null, error: (String) -> Unit) =
        run({ addAccount(name, type, balance, creditLimit) }, error)
        
    fun renameAccount(accountId: String, newName: String, error: (String) -> Unit) =
        run({ renameAccount(accountId, newName) }, error)    
        
    fun deleteAccount(accountId: String, error: (String) -> Unit) =
        run({ deleteAccount(accountId) }, error)

    fun reorderAccounts(orderedIds: List<String>) =
        run({ reorderAccounts(orderedIds) })

    fun recalculateBalances(onDone: () -> Unit = {}, error: (String) -> Unit = { report(it) }) {
        viewModelScope.launch {
            try {
                repo.recalculateBalances()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error(e.message ?: "Gagal menghitung ulang saldo")
            }
        }
    }

    fun deleteTransaction(id: String, error: (String) -> Unit = { report(it) }) = run({ deleteTransaction(id) }, error)

    fun deleteTransactions(ids: List<String>, error: (String) -> Unit = { report(it) }) =
        run({ deleteTransactions(ids) }, error)

    fun addTransaction(
        type: String,
        amount: Long,
        categoryId: String,
        accountId: String,
        date: Long,
        note: String,
        error: (String) -> Unit
    ) = run({ addTransaction(type, amount, categoryId, accountId, date, note) }, error)

    fun updateTransaction(
        id: String,
        amount: Long,
        categoryId: String,
        note: String,
        date: Long,
        accountId: String,
        error: (String) -> Unit
    ) = run({ updateTransaction(id, amount, categoryId, note, date, accountId) }, error)

    fun addCategory(name: String, kind: String, icon: String, color: Long, error: (String) -> Unit) =
        run({ addCategory(name, kind, icon, color) }, error)

    fun updateCategory(id: String, name: String, icon: String, color: Long, error: (String) -> Unit) =
        run({ updateCategory(id, name, icon, color) }, error)

    fun deleteCategory(id: String, error: (String) -> Unit) =
        run({ deleteCategory(id) }, error)

    fun transfer(from: String, to: String, amount: Long, note: String = "", error: (String) -> Unit) =
        run({ transfer(from, to, amount, note) }, error)

    fun addBill(provider: String, item: String, amount: Long, due: Long, tenor: Int, error: (String) -> Unit) =
        run({ addBill(provider, item, amount, due, tenor) }, error)

    fun payBill(bill: BillEntity, accountId: String, error: (String) -> Unit) =
        run({ payBill(bill.id, accountId) }, error)

    fun payBills(list: List<BillEntity>, accountId: String, error: (String) -> Unit) =
        run({ payBills(list, accountId) }, error)

    fun unpayBill(bill: BillEntity, error: (String) -> Unit = { report(it) }) =
        run({ unpayBill(bill.id) }, error)

    fun deleteBill(bill: BillEntity, error: (String) -> Unit = { report(it) }) =
        run({ deleteBill(bill.id) }, error)

    fun deleteBills(ids: List<String>, error: (String) -> Unit, onSuccess: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = try {
                repo.deleteBills(ids)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error(e.message ?: "Gagal menghapus tagihan")
                return@launch
            }
            onSuccess(count)
        }
    }

    fun addInvestment(name: String, target: Long?) = run({ addInvestment(name, target) })

    fun addGoal(name: String, target: Long, deadlineMillis: Long?, category: String, error: (String) -> Unit = { report(it) }) =
        run({ addGoal(name, target, deadlineMillis, category) }, error)

    fun updateGoal(id: String, name: String, target: Long, deadlineMillis: Long?, category: String, error: (String) -> Unit = { report(it) }) =
        run({ updateGoal(id, name, target, deadlineMillis, category) }, error)

    fun updateInvestment(id: String, name: String, target: Long?, error: (String) -> Unit = { report(it) }) =
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
            val result = try {
                val days = AppPrefs.maxCatchupDays(getApplication())
                repo.processDueSchedules(maxCatchupDays = days)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val kind = e.javaClass.simpleName
                Log.w("FinanceViewModel", "Pemrosesan jadwal gagal: $kind")
                if (Notify.scheduleProcessingError(getApplication(), kind, showNotification = false)) {
                    report("Jadwal otomatis tidak bisa diproses (kode: $kind). Gaji dan setoran terjadwal belum berjalan. Coba buka ulang aplikasi.")
                }
                return@launch
            }
            onDone(result)
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

    fun addDebt(type: String, person: String, desc: String, amount: Long, dueDateMillis: Long? = null, error: (String) -> Unit) =
        run({ addDebt(type, person, desc, amount, dueDateMillis) }, error)

    fun updateDebt(debtId: String, person: String, desc: String, amount: Long, dueDateMillis: Long?, error: (String) -> Unit) =
        run({ updateDebt(debtId, person, desc, amount, dueDateMillis) }, error)

    fun deleteDebt(debtId: String, error: (String) -> Unit = { report(it) }) =
        run({ deleteDebt(debtId) }, error)

    fun payDebt(debtId: String, accountId: String, amount: Long, error: (String) -> Unit = { report(it) }) =
        run({ payDebt(debtId, accountId, amount) }, error)

    fun markDebtPaid(debt: DebtEntity, accountId: String, error: (String) -> Unit = { report(it) }) =
        run({ markDebtPaid(debt, accountId) }, error)

    fun markDebtUnpaid(debt: DebtEntity, error: (String) -> Unit = { report(it) }) =
        run({ markDebtUnpaid(debt) }, error)

    suspend fun exportJson() = BackupManager.buildFullJson(getApplication())

    fun importJson(
        json: String,
        password: CharArray? = null,
        onDone: (String) -> Unit = {},
        error: (String) -> Unit = { report(it) },
        onNeedPassword: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val decoded = try {
                    withContext(Dispatchers.Default) { BackupCodec.decodeMaybeEncrypted(app, json, password) }
                } catch (e: BackupPasswordException) {
                    onNeedPassword(e.wrong)
                    return@launch
                }

                val preImportName = try {
                    BackupManager.savePreImport(app, repo.snapshot())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error("Gagal membuat backup pengaman, impor dibatalkan: ${e.message ?: "tidak diketahui"}")
                    return@launch
                }

                val imported = repo.importDecoded(decoded)
                BackupManager.applySettings(app, imported.settings)

                val notes = mutableListOf<String>()
                if (preImportName != null) {
                    notes += "Kondisi sebelum impor tersimpan di daftar backup (\"Sebelum impor\")."
                }
                if (password != null && password.isNotEmpty() && SecureCrypto.isPasswordEncrypted(json)) {
                    val adopted = withContext(Dispatchers.IO) {
                        if (BackupManager.hasPassword(app)) null else BackupManager.setPassword(app, password)
                    }
                    when (adopted) {
                        true -> notes += "Password backup disimpan di HP ini, jadi backup berikutnya memakai password yang sama."
                        false -> notes += "Password backup tidak bisa disimpan di HP ini. Atur password di Backup & Restore agar backup berikutnya portabel."
                        null -> {}
                    }
                }
                onDone(notes.joinToString(" "))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error(e.message ?: "Gagal mengimpor backup")
            } finally {
                password?.fill('\u0000')
            }
        }
    }

    fun backupNow(onDone: (BackupManager.Outcome) -> Unit) {
        viewModelScope.launch { onDone(BackupManager.runBackup(getApplication(), force = true, source = "manual")) }
    }
}
