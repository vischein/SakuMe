package com.sakume.personalfinance.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sakume.personalfinance.data.BackupCodec
import com.sakume.personalfinance.ui.components.AccountPickerDialog
import com.sakume.personalfinance.ui.components.CashTransferDialog
import com.sakume.personalfinance.ui.components.ConfirmDialog
import com.sakume.personalfinance.ui.components.DebtPaymentDialog
import com.sakume.personalfinance.ui.components.SakuMeBottomBar
import com.sakume.personalfinance.ui.components.FormDialog
import com.sakume.personalfinance.ui.components.MessageDialog
import com.sakume.personalfinance.ui.isCashAccount
import com.sakume.personalfinance.ui.screens.AnalyticsScreen
import com.sakume.personalfinance.ui.screens.ManageAccountsScreen
import com.sakume.personalfinance.ui.screens.BackupScreen
import com.sakume.personalfinance.ui.screens.TransactionHistoryScreen
import com.sakume.personalfinance.ui.screens.BillsScreen
import com.sakume.personalfinance.ui.screens.HomeScreen
import com.sakume.personalfinance.ui.screens.InvestmentScreen
import com.sakume.personalfinance.ui.screens.MoreScreen
import com.sakume.personalfinance.ui.screens.SettingsScreen
import com.sakume.personalfinance.ui.theme.SakuMeTheme
import com.sakume.personalfinance.ui.theme.Paper
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted

@Composable
fun FinanceApp(vm: FinanceViewModel) {
    val accounts by vm.accounts.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val bills by vm.bills.collectAsState()
    val investments by vm.investments.collectAsState()
    val debts by vm.debts.collectAsState()
    val schedules by vm.schedules.collectAsState()
    val salaries by vm.salaries.collectAsState()
    var tab by remember { mutableStateOf("Home") }
    var subScreen by remember { mutableStateOf<String?>(null) }
    var historyAccountId by remember { mutableStateOf<String?>(null) }
    var billsProviderFilter by remember { mutableStateOf("Semua") }
    var debtFilter by remember { mutableStateOf("Semua") }
    var dialog by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var importResult by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var unlocked by remember {
        mutableStateOf(!AppPrefs.appLockEnabled(context))
    }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    BackHandler(enabled = subScreen != null || tab != "Home") {
        when {
            subScreen != null -> subScreen = null
            tab != "Home" -> tab = "Home"
        }
    }

    fun shareBackup() {
        scope.launch {
            try {
                val text = vm.exportJson()
                val file = File(context.cacheDir, "sakume_backup_${System.currentTimeMillis()}.json")
                file.writeText(text)
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Backup SakuMe")
                    putExtra(Intent.EXTRA_TEXT, "Backup data SakuMe")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Bagikan backup SakuMe"))
            } catch (e: Exception) {
                importResult = "Gagal membagikan backup: ${e.message}"
            }
        }
    }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.processDueSchedules { result ->
            if (result.investmentRuns.isNotEmpty()) {
                Notify.investmentSuccess(
                    context,
                    result.investmentRuns.joinToString("\n") { "$it" }
                )
            }
            if (result.salaryRuns.isNotEmpty()) {
                Notify.salarySuccess(
                    context,
                    result.salaryRuns.joinToString("\n") { "$it" }
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val legacy = BackupCodec.isLegacySakuMe(text)
                vm.importJson(
                    json = text,
                    onDone = { path ->
                        importResult = if (legacy) {
                            "Data lama berhasil diimpor. Backup otomatis sebelum impor tersimpan di $path"
                        } else {
                            "Backup berhasil dipulihkan. Backup otomatis sebelum impor tersimpan di $path"
                        }
                    },
                    error = { err -> importResult = "Gagal mengimpor: $err" }
                )
            } catch (e: Exception) {
                importResult = "Gagal membaca file: ${e.message}"
            }
        }
    }

    SakuMeTheme {
        if (!unlocked) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Paper)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SakuMe", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Masukkan PIN untuk membuka", color = Muted, fontSize = 14.sp)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                pinInput = it
                                pinError = ""
                            }
                        },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (pinError.isNotEmpty()) {
                        Text(pinError, color = Color(0xFFD32F2F), fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            if (AppPrefs.verifyPin(context, pinInput)) {
                                unlocked = true
                                pinInput = ""
                                pinError = ""
                            } else {
                                pinError = "PIN salah"
                                pinInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink),
                        enabled = pinInput.length in 4..6
                    ) {
                        Text("Buka")
                    }
                }
            }
            return@SakuMeTheme
        }

        Scaffold(
            containerColor = Paper,
            bottomBar = {
                SakuMeBottomBar(selected = tab, onSelect = { label ->
                    tab = label
                    subScreen = null
                    if (label == "Bills") billsProviderFilter = "Semua"
                    if (label == "More") debtFilter = "Semua"
                })
            },
            content = { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (subScreen == "settings") {
                        SettingsScreen(
                            onBack = { subScreen = null },
                            onOpenBackup = { subScreen = "backup" },
                            onAddSalary = {
                                subScreen = null
                                dialog = "salary"
                            }
                        )
                    } else if (subScreen == "txHistory") {
                        TransactionHistoryScreen(
                            accounts = accounts,
                            tx = transactions,
                            onBack = { subScreen = null; historyAccountId = null },
                            accountFilter = historyAccountId,
                            onDeleteMany = { ids ->
                                dialog = "deletetxmany:${ids.joinToString(",")}"
                            }
                        )
                    } else if (subScreen == "backup") {
                        BackupScreen(
                            onBack = { subScreen = null; historyAccountId = null },
                            onExport = { shareBackup() },
                            onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "text/*", "*/*")) },
                            onRecalculate = {
                                vm.recalculateBalances(
                                    onDone = { importResult = "Saldo berhasil dihitung ulang dari transaksi." },
                                    error = { importResult = "Gagal: $it" }
                                )
                            }
                        )
                    } else if (subScreen == "manageAccounts") {
                        ManageAccountsScreen(
                            accounts = accounts,
                            tx = transactions,
                            onAdd = { dialog = "account" },
                            onTransfer = { dialog = "transfer" },
                            onAddTx = { dialog = "transaction" },
                            onAccountClick = { account -> historyAccountId = account.id; subScreen = "txHistory" },
                            onCash = { dialog = "cash" },
                            onDelete = { a -> dialog = "deleteaccount:${a.id}" },
                            onReorder = { ids -> vm.reorderAccounts(ids) },
                            onBack = { subScreen = null }
                        )
                    } else when (tab) {
                        "Home" -> HomeScreen(
                            accounts = accounts,
                            tx = transactions,
                            bills = bills,
                            investments = investments,
                            debts = debts,
                            onAction = { action ->
                                when {
                                    action == "accounts" -> subScreen = "manageAccounts"
                                    action.startsWith("accountHistory:") -> {
                                        historyAccountId = action.removePrefix("accountHistory:")
                                        subScreen = "txHistory"
                                    }
                                    action == "cashHistory" -> {
                                        val cash = accounts.firstOrNull { isCashAccount(it) }
                                        if (cash != null) {
                                            historyAccountId = cash.id
                                            subScreen = "txHistory"
                                        } else {
                                            subScreen = "manageAccounts"
                                        }
                                    }
                                    action == "bills" -> {
                                        billsProviderFilter = "Semua"
                                        tab = "Bills"
                                    }
                                    action.startsWith("bills:") -> {
                                        billsProviderFilter = action.removePrefix("bills:")
                                        tab = "Bills"
                                    }
                                    action == "invest" -> tab = "Invest"
                                    action == "more:piutang" -> {
                                        debtFilter = "Piutang"
                                        tab = "More"
                                    }
                                    action == "more:utang" -> {
                                        debtFilter = "Utang"
                                        tab = "More"
                                    }
                                    else -> dialog = action
                                }
                            },
                            onOpenSettings = { subScreen = "settings" },
                            onSeeAllTx = { subScreen = "txHistory" }
                        )
                        "Analisis" -> AnalyticsScreen(
                            accounts = accounts,
                            tx = transactions,
                            bills = bills,
                            debts = debts,
                            onManageAccounts = { subScreen = "manageAccounts" }
                        )
                        "Bills" -> BillsScreen(
                            bills = bills,
                            accounts = accounts,
                            initialProvider = billsProviderFilter,
                            onAdd = { dialog = "bill" },
                            onPay = { b -> dialog = "paybill:${b.id}" },
                            onPayMonth = { list -> dialog = "paymany:${list.joinToString(",") { it.id }}" },
                            onDelete = { b -> dialog = "deletebill:${b.id}" },
                            onUnpay = { b -> dialog = "unpaybill:${b.id}" }
                        )
                        "Invest" -> InvestmentScreen(
                            items = investments,
                            accounts = accounts,
                            schedules = schedules,
                            onAdd = { dialog = "investment" },
                            onContribute = { a -> dialog = "contribute:${a.id}" },
                            onWithdraw = { a -> dialog = "withdraw:${a.id}" },
                            onSchedule = { a -> dialog = "schedule:${a.id}" },
                            onToggleSchedule = { s, on -> vm.setScheduleEnabled(s.id, on) },
                            onDeleteSchedule = { s -> vm.deleteSchedule(s.id) },
                            onEdit = { a -> dialog = "editinvest:${a.id}" },
                            onDelete = { a -> dialog = "deleteinvest:${a.id}" }
                        )
                        else -> MoreScreen(
                            debts = debts,
                            accounts = accounts,
                            salaries = salaries,
                            initialDebtFilter = debtFilter,
                            onAddDebt = { filter ->
                                debtFilter = filter
                                dialog = "debt"
                            },
                            onPaid = { d -> dialog = "paydebt:${d.id}" },
                            onUnpaid = { d -> vm.markDebtUnpaid(d) { error = it } },
                            onAddSalary = { dialog = "salary" },
                            onEditSalary = { s -> dialog = "editsalary:${s.id}" },
                            onToggleSalary = { s, on -> vm.setSalaryEnabled(s.id, on) },
                            onDeleteSalary = { s -> dialog = "deletesalary:${s.id}" },
                            onOpenSettings = { subScreen = "settings" },
                            onDebtFilterChange = { debtFilter = it }
                        )
                    }
                }
            }
        )

        if (importResult.isNotBlank()) {
            MessageDialog("Backup", importResult) { importResult = "" }
        }
        if (error.isNotBlank()) {
            MessageDialog("Tidak dapat memproses", error) { error = "" }
        }

        when {
            dialog == "account" -> FormDialog(
                title = "Tambah rekening",
                fields = listOf("Nama rekening", "Saldo awal"),
                options = listOf("Cash", "E-Wallet", "Bank"),
                optionLabel = "Tipe rekening",
                onDismiss = { dialog = "" }
            ) { v ->
                val balance = v[1].filter { it.isDigit() }.toLongOrNull() ?: 0L
                if (v[0].isBlank()) error = "Nama rekening wajib diisi"
                else {
                    vm.addAccount(v[0], v.getOrElse(2) { "Cash" }, balance) { error = it }
                    dialog = ""
                }
            }

            dialog == "transaction" -> FormDialog(
                title = "Transaksi baru",
                fields = listOf("Nominal", "Catatan"),
                options = listOf("expense", "income"),
                optionLabel = "Jenis",
                optionDisplay = { if (it == "expense") "Pengeluaran" else "Pemasukan" },
                extraOptions = accounts.map { it.name },
                extraOptionLabel = "Rekening",
                onDismiss = { dialog = "" }
            ) { v ->
                val amount = (v[0].filter { it.isDigit() }.toLongOrNull() ?: 0L) * AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                val type = v.getOrElse(2) { "expense" }
                val accName = v.getOrElse(3) { accounts.firstOrNull()?.name.orEmpty() }
                val acc = accounts.firstOrNull { it.name == accName }
                val note = v.getOrElse(1) { "" }
                when {
                    accounts.isEmpty() -> error = "Tambahkan rekening terlebih dahulu"
                    acc == null -> error = "Pilih rekening"
                    amount <= 0 -> error = "Nominal harus lebih dari nol"
                    else -> {
                        vm.addTransaction(
                            type,
                            amount,
                            note.ifBlank { if (type == "income") "Pemasukan" else "Pengeluaran" },
                            acc.id,
                            System.currentTimeMillis(),
                            note
                        ) { error = it }
                        dialog = ""
                    }
                }
            }

            dialog == "transfer" -> FormDialog(
                title = "Transfer saldo",
                fields = listOf("Nominal", "Catatan (opsional)"),
                options = accounts.map { it.name },
                optionLabel = "Dari rekening",
                extraOptions = accounts.map { it.name },
                extraOptionLabel = "Ke rekening",
                onDismiss = { dialog = "" }
            ) { v ->
                val amount = (v[0].filter { it.isDigit() }.toLongOrNull() ?: 0L) * AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                val from = accounts.firstOrNull { it.name == v.getOrElse(2) { "" } }
                val to = accounts.firstOrNull { it.name == v.getOrElse(3) { "" } }
                when {
                    accounts.size < 2 -> error = "Butuh minimal dua rekening"
                    from == null || to == null -> error = "Pilih rekening asal dan tujuan"
                    from.id == to.id -> error = "Rekening asal dan tujuan harus berbeda"
                    amount <= 0 -> error = "Nominal harus lebih dari nol"
                    else -> {
                        vm.transfer(from.id, to.id, amount, v.getOrElse(1) { "" }) { error = it }
                        dialog = ""
                    }
                }
            }

            dialog == "cash" -> CashTransferDialog(
                accounts = accounts,
                isCash = { isCashAccount(it) },
                onDismiss = { dialog = "" }
            ) { isWithdraw, otherAccount, cashAccount, amount, note ->
                if (isWithdraw) {
                    vm.withdrawCash(otherAccount.id, cashAccount.id, amount, note.ifBlank { "Tarik tunai" }) { error = it }
                } else {
                    vm.depositCash(cashAccount.id, otherAccount.id, amount, note.ifBlank { "Setor tunai" }) { error = it }
                }
                dialog = ""
            }

            dialog == "bill" -> FormDialog(
                title = "Tambah tagihan",
                fields = listOf("Provider", "Nama item", "Total nominal"),
                options = listOf("1", "3", "6", "9", "12"),
                optionLabel = "Tenor (bulan)",
                optionDisplay = { "$it bulan" },
                fieldSuggestions = mapOf(
                    0 to bills.map { it.provider }.distinct().sorted()
                ),
                includeDueDate = true,
                dueDateLabel = "Mulai / jatuh tempo pertama",
                onDismiss = { dialog = "" }
            ) { v ->
                val total = v[2].filter { it.isDigit() }.toLongOrNull() ?: 0L
                val due = v.getOrElse(3) { "" }.toLongOrNull()
                val tenor = v.getOrElse(4) { "1" }.toIntOrNull() ?: 1
                if (v[0].isBlank() || v[1].isBlank()) error = "Provider dan nama item wajib diisi"
                else if (total <= 0) error = "Nominal harus lebih dari nol"
                else if (due == null) error = "Pilih tanggal jatuh tempo"
                else {
                    vm.addBill(v[0], v[1], total, due, tenor) { error = it }
                    dialog = ""
                }
            }

            dialog.startsWith("paybill:") -> {
                val bill = bills.firstOrNull { it.id == dialog.substringAfter(":") }
                AccountPickerDialog("Bayar tagihan", accounts, { dialog = "" }) { acc ->
                    bill?.let { vm.payBill(it, acc.id) { error = it } }
                    dialog = ""
                }
            }

            dialog.startsWith("paymany:") -> {
                val ids = dialog.substringAfter(":").split(",").filter { it.isNotBlank() }
                AccountPickerDialog("Bayar tagihan bulan ini", accounts, { dialog = "" }) { acc ->
                    vm.payBills(bills.filter { it.id in ids }, acc.id) { error = it }
                    dialog = ""
                }
            }

            dialog.startsWith("deletebill:") -> ConfirmDialog(
                title = "Hapus tagihan ini?",
                confirm = {
                    val id = dialog.substringAfter(":")
                    bills.firstOrNull { it.id == id }?.let { vm.deleteBill(it) }
                    dialog = ""
                },
                cancel = { dialog = "" }
            )

            dialog.startsWith("unpaybill:") -> ConfirmDialog(
                title = "Batalkan pembayaran?",
                confirm = {
                    val id = dialog.substringAfter(":")
                    bills.firstOrNull { it.id == id }?.let { b ->
                        vm.unpayBill(b) { error = it }
                    }
                    dialog = ""
                },
                cancel = { dialog = "" }
            )

            dialog == "investment" -> FormDialog(
                title = "Aset investasi",
                fields = listOf("Nama aset", "Target (opsional)"),
                options = emptyList(),
                onDismiss = { dialog = "" }
            ) { v ->
                if (v[0].isBlank()) error = "Nama aset wajib diisi"
                else {
                    vm.addInvestment(v[0], v[1].filter { it.isDigit() }.toLongOrNull())
                    dialog = ""
                }
            }

            dialog.startsWith("editinvest:") -> {
                val asset = investments.firstOrNull { it.id == dialog.substringAfter(":") }
                if (asset == null) {
                    dialog = ""
                } else {
                    FormDialog(
                        title = "Edit aset",
                        fields = listOf("Nama aset", "Target (opsional)"),
                        options = emptyList(),
                        initialValues = listOf(
                            asset.name,
                            asset.target?.toString().orEmpty()
                        ),
                        onDismiss = { dialog = "" }
                    ) { v ->
                        if (v[0].isBlank()) error = "Nama aset wajib diisi"
                        else {
                            vm.updateInvestment(
                                asset.id,
                                v[0],
                                v[1].filter { it.isDigit() }.toLongOrNull()
                            ) { error = it }
                            dialog = ""
                        }
                    }
                }
            }

            dialog.startsWith("schedule:") -> {
                val asset = investments.firstOrNull { it.id == dialog.substringAfter(":") }
                FormDialog(
                    title = "Jadwal setor mingguan",
                    fields = listOf("Nominal per minggu"),
                    options = listOf("2", "3", "4", "5", "6", "7", "1"),
                    optionLabel = "Hari (Senin–Minggu)",
                    optionDisplay = {
                        when (it) {
                            "1" -> "Minggu"
                            "2" -> "Senin"
                            "3" -> "Selasa"
                            "4" -> "Rabu"
                            "5" -> "Kamis"
                            "6" -> "Jumat"
                            "7" -> "Sabtu"
                            else -> it
                        }
                    },
                    extraOptions = accounts.map { it.name },
                    extraOptionLabel = "Dari rekening",
                    onDismiss = { dialog = "" }
                ) { v ->
                    val amount = (v[0].filter { it.isDigit() }.toLongOrNull() ?: 0L) * AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                    val day = v.getOrElse(1) { "2" }.toIntOrNull() ?: 2
                    val acc = accounts.firstOrNull { it.name == v.getOrElse(2) { "" } }
                    when {
                        asset == null -> error = "Aset tidak ditemukan"
                        acc == null -> error = "Pilih rekening"
                        amount <= 0 -> error = "Nominal harus lebih dari nol"
                        else -> {
                            vm.addSchedule(asset.id, acc.id, amount, day) { error = it }
                            dialog = ""
                        }
                    }
                }
            }

            dialog.startsWith("withdraw:") -> {
                val asset = investments.firstOrNull { it.id == dialog.substringAfter(":") }
                FormDialog(
                    title = "Tarik investasi",
                    fields = listOf("Nominal"),
                    options = accounts.map { it.name },
                    optionLabel = "Ke rekening",
                    onDismiss = { dialog = "" }
                ) { v ->
                    val amount = (v[0].filter { it.isDigit() }.toLongOrNull() ?: 0L) * AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                    val acc = accounts.firstOrNull { it.name == v.getOrElse(1) { "" } }
                    when {
                        asset == null -> error = "Aset tidak ditemukan"
                        acc == null -> error = "Pilih rekening"
                        amount <= 0 -> error = "Nominal harus lebih dari nol"
                        amount > asset.value -> error = "Maksimal ${asset.value}"
                        else -> {
                            vm.withdraw(asset.id, acc.id, amount) { error = it }
                            dialog = ""
                        }
                    }
                }
            }

            dialog.startsWith("contribute:") -> {
                val asset = investments.firstOrNull { it.id == dialog.substringAfter(":") }
                FormDialog(
                    title = "Setor investasi",
                    fields = listOf("Nominal"),
                    options = accounts.map { it.name },
                    optionLabel = "Dari rekening",
                    onDismiss = { dialog = "" }
                ) { v ->
                    val amount = (v[0].filter { it.isDigit() }.toLongOrNull() ?: 0L) * AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                    val acc = accounts.firstOrNull { it.name == v.getOrElse(1) { "" } }
                    when {
                        asset == null -> error = "Aset tidak ditemukan"
                        acc == null -> error = "Pilih rekening"
                        amount <= 0 -> error = "Nominal harus lebih dari nol"
                        else -> {
                            vm.contribute(asset.id, acc.id, amount) { error = it }
                            dialog = ""
                        }
                    }
                }
            }

            dialog.startsWith("deleteinvest:") -> ConfirmDialog(
                title = "Hapus aset dan refund semua kontribusi ke rekening asal?",
                confirm = {
                    vm.deleteInvestment(dialog.substringAfter(":"))
                    dialog = ""
                },
                cancel = { dialog = "" }
            )

            dialog.startsWith("deleteaccount:") -> {
                val acc = accounts.firstOrNull { it.id == dialog.substringAfter(":") }
                ConfirmDialog(
                    title = if (acc == null) "Hapus rekening?"
                    else "Hapus rekening \"${acc.name}\"? Saldo harus Rp0. Riwayat transaksi rekening ini ikut dihapus.",
                    confirm = {
                        acc?.let { vm.deleteAccount(it.id) { err -> error = err } }
                        dialog = ""
                    },
                    cancel = { dialog = "" }
                )
            }

            dialog == "salary" -> FormDialog(
                title = "Jadwal gaji",
                fields = listOf("Nominal", "Catatan (opsional)"),
                options = listOf("daily", "biweekly", "monthly"),
                optionLabel = "Frekuensi",
                optionDisplay = {
                    when (it) {
                        "daily" -> "Setiap hari"
                        "biweekly" -> "Setiap 2 minggu"
                        "monthly" -> "Setiap bulan"
                        else -> it
                    }
                },
                extraOptions = accounts.map { it.name },
                extraOptionLabel = "Ke rekening",
                tertiaryOptions = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"),
                tertiaryOptionLabel = "Jam masuk",
                tertiaryDisplay = { h -> h.toIntOrNull()?.toString()?.padStart(2, '0')?.plus(":00") ?: h },
                initialTertiaryIndex = 9,
                onDismiss = { dialog = "" }
            ) { v ->
                val amount = (v[0].filter { it.isDigit() }.toLongOrNull() ?: 0L) * AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                val freq = v.firstOrNull { it in listOf("daily", "biweekly", "monthly") } ?: "monthly"
                val acc = accounts.firstOrNull { a -> v.any { it == a.name } }
                val hour = v.mapNotNull { it.toIntOrNull() }.firstOrNull { it in 0..23 } ?: 9
                when {
                    acc == null -> error = "Pilih rekening"
                    amount <= 0 -> error = "Nominal harus lebih dari nol"
                    else -> {
                        vm.addSalary(acc.id, amount, freq, v.getOrElse(1) { "Gaji" }, hour) { error = it }
                        dialog = ""
                    }
                }
            }

            dialog.startsWith("editsalary:") -> {
                val s = salaries.firstOrNull { it.id == dialog.substringAfter(":") }
                FormDialog(
                    title = "Edit jadwal gaji",
                    fields = listOf("Nominal", "Catatan"),
                    options = listOf("daily", "biweekly", "monthly"),
                    optionLabel = "Frekuensi",
                    optionDisplay = {
                        when (it) {
                            "daily" -> "Setiap hari"
                            "biweekly" -> "Setiap 2 minggu"
                            "monthly" -> "Setiap bulan"
                            else -> it
                        }
                    },
                    extraOptions = accounts.map { it.name },
                    extraOptionLabel = "Ke rekening",
                    tertiaryOptions = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"),
                    tertiaryOptionLabel = "Jam masuk",
                    tertiaryDisplay = { h -> h.toIntOrNull()?.toString()?.padStart(2, '0')?.plus(":00") ?: h },
                    initialTertiaryIndex = s?.hourOfDay?.coerceIn(0, 23) ?: 9,
                    onDismiss = { dialog = "" }
                ) { v ->
                    val amount = (v[0].filter { it.isDigit() }.toLongOrNull() ?: 0L) * AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                    val freq = v.firstOrNull { it in listOf("daily", "biweekly", "monthly") } ?: "monthly"
                    val acc = accounts.firstOrNull { a -> v.any { it == a.name } }
                    val hour = v.mapNotNull { it.toIntOrNull() }.firstOrNull { it in 0..23 } ?: 9
                    when {
                        s == null -> error = "Jadwal tidak ditemukan"
                        acc == null -> error = "Pilih rekening"
                        amount <= 0 -> error = "Nominal harus lebih dari nol"
                        else -> {
                            vm.updateSalary(s.id, acc.id, amount, freq, v.getOrElse(1) { "Gaji" }, hour) { error = it }
                            dialog = ""
                        }
                    }
                }
            }

            dialog.startsWith("deletetxmany:") -> {
                val ids = dialog.substringAfter(":").split(",").filter { it.isNotBlank() }
                ConfirmDialog(
                    title = "Hapus ${ids.size} transaksi? Saldo rekening akan dikembalikan.",
                    confirm = {
                        vm.deleteTransactions(ids) { error = it }
                        dialog = ""
                    },
                    cancel = { dialog = "" }
                )
            }

            dialog.startsWith("deletesalary:") -> ConfirmDialog(
                title = "Hapus jadwal gaji ini?",
                confirm = {
                    vm.deleteSalary(dialog.substringAfter(":"))
                    dialog = ""
                },
                cancel = { dialog = "" }
            )

            dialog.startsWith("paydebt:") -> {
                val debtId = dialog.substringAfter(":")
                val debt = debts.firstOrNull { it.id == debtId }
                if (debt == null) {
                    dialog = ""
                } else {
                    val isPiutang = debt.type == "piutang"
                    DebtPaymentDialog(
                        title = if (isPiutang) "Terima pembayaran dari ${debt.person}" else "Bayar utang ke ${debt.person}",
                        actionLabel = if (isPiutang) "diterima" else "dibayar",
                        total = debt.amount,
                        paid = debt.paidAmount,
                        accounts = accounts,
                        onDismiss = { dialog = "" }
                    ) { acc, amount ->
                        vm.payDebt(debt.id, acc.id, amount) { error = it }
                        dialog = ""
                    }
                }
            }

            dialog == "debt" -> FormDialog(
                title = when (debtFilter) {
                    "Piutang" -> "Tambah piutang"
                    "Utang" -> "Tambah utang"
                    else -> "Tambah piutang / utang"
                },
                fields = listOf("Nama orang", "Deskripsi", "Nominal"),
                options = if (debtFilter == "Piutang" || debtFilter == "Utang") emptyList()
                else listOf("piutang", "utang"),
                optionLabel = "Jenis",
                optionDisplay = { if (it == "piutang") "Piutang" else "Utang" },
                initialOptionIndex = when (debtFilter) { "Utang" -> 1; else -> 0 },
                onDismiss = { dialog = "" }
            ) { v ->
                val amount = (v[2].filter { it.isDigit() }.toLongOrNull() ?: 0L) * AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                val type = when (debtFilter) {
                    "Piutang" -> "piutang"
                    "Utang" -> "utang"
                    else -> v.getOrElse(3) { "piutang" }
                }
                if (v[0].isBlank()) error = "Nama orang wajib diisi"
                else if (amount <= 0) error = "Nominal harus lebih dari nol"
                else {
                    vm.addDebt(type, v[0], v[1], amount) { error = it }
                    dialog = ""
                }
            }
        }
    }
}
