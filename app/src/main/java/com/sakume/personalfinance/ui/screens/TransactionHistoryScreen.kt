package com.sakume.personalfinance.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.TransactionEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.SelectableChip
import com.sakume.personalfinance.ui.components.TransactionRow
import com.sakume.personalfinance.ui.buildDisplayTransactions
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.startOfMonthMillis
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Red
import java.util.Calendar

@Composable
fun TransactionHistoryScreen(
    accounts: List<AccountEntity>,
    tx: List<TransactionEntity>,
    onBack: () -> Unit,
    onDeleteMany: (List<String>) -> Unit = {},
    accountFilter: String? = null
) {
    var filter by remember { mutableStateOf("all") }
    var dayMillis by remember { mutableStateOf<Long?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val context = LocalContext.current
    val monthStart = startOfMonthMillis()

    val visibleTransactions = remember(tx, accounts, accountFilter) {
        val source = if (accountFilter == null) {
            tx
        } else {
            tx.filter { it.accountId == accountFilter }
        }

        buildDisplayTransactions(source, accounts)
    }

    val filtered = remember(visibleTransactions, filter, dayMillis) {
        when (filter) {
            "month" -> visibleTransactions.filter { it.dateMillis >= monthStart }
            "day" -> {
                val day = dayMillis ?: return@remember visibleTransactions
                val start = Calendar.getInstance().apply {
                    timeInMillis = day
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = start + 86_400_000L
                visibleTransactions.filter { it.dateMillis in start until end }
            }
            else -> visibleTransactions
        }
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { dayMillis?.let { timeInMillis = it } }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                dayMillis = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                filter = "day"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (selectionMode) "${selected.size} dipilih" else if (accountFilter != null) "Histori rekening" else "Histori transaksi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Text(
                    if (selectionMode) "Pilih transaksi yang ingin dihapus" else "${filtered.size} transaksi",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
            if (selectionMode) {
                if (selected.isNotEmpty()) {
                    Button(
                        onClick = {
                            onDeleteMany(selected.toList())
                            selected = emptySet()
                            selectionMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.padding(end = 4.dp))
                        Text("Hapus")
                    }
                }
            } else {
                Button(
                    onClick = { selectionMode = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus histori")
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item { SelectableChip("Semua", filter == "all") { filter = "all" } }
            item { SelectableChip("Bulan ini", filter == "month") { filter = "month" } }
            item {
                val dayLabel = if (filter == "day" && dayMillis != null) formatDate(dayMillis!!) else "Pilih tanggal"
                SelectableChip(dayLabel, filter == "day") { openDatePicker() }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (filtered.isEmpty()) {
            EmptyHint("Tidak ada transaksi pada filter ini.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
            ) {
                items(filtered, key = { it.id }) { t ->
                    TransactionRow(
                        t = t,
                        accountName = if (t.note == "Transfer") t.category else accounts.firstOrNull { it.id == t.accountId }?.let { if (it.name.contains("tunai", true) || it.name.equals("Cash", true)) "Cash" else it.name } ?: "Rekening",
                        selected = t.id in selected,
                        selectionMode = selectionMode,
                        onToggleSelect = {
                            selected = if (t.id in selected) selected - t.id else selected + t.id
                        }
                    )
                }
            }
        }
    }
}
