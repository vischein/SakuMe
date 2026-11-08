package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.DebtEntity
import com.sakume.personalfinance.data.SalaryScheduleEntity
import com.sakume.personalfinance.ui.AppPrefs
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.SelectableChip
import com.sakume.personalfinance.ui.components.StatTile
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.Accent
import com.sakume.personalfinance.ui.theme.AccentSoft
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.GreenSoft
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft

private fun freqLabel(f: String) = when (f) {
    "daily" -> "Setiap hari"
    "biweekly" -> "Setiap 2 minggu"
    "monthly" -> "Setiap bulan"
    else -> f
}

private fun formatExample(prefix: Int, zeros: Int): String {
    val full = prefix.toString() + "0".repeat(zeros)
    return try {
        java.text.NumberFormat.getInstance(java.util.Locale("id", "ID")).format(full.toLong())
    } catch (_: Exception) {
        full
    }
}

@Composable
fun MoreScreen(
    debts: List<DebtEntity>,
    accounts: List<AccountEntity>,
    salaries: List<SalaryScheduleEntity>,
    initialDebtFilter: String = "Semua",
    onAddDebt: (filter: String) -> Unit,
    onPaid: (DebtEntity) -> Unit,
    onUnpaid: (DebtEntity) -> Unit,
    onAddSalary: () -> Unit,
    onEditSalary: (SalaryScheduleEntity) -> Unit,
    onToggleSalary: (SalaryScheduleEntity, Boolean) -> Unit,
    onDeleteSalary: (SalaryScheduleEntity) -> Unit,
    onOpenSettings: () -> Unit = {},
    onDebtFilterChange: (String) -> Unit = {}
) {
    var debtFilter by remember(initialDebtFilter) { mutableStateOf(initialDebtFilter) }
    fun setDebtFilter(value: String) {
        debtFilter = value
        onDebtFilterChange(value)
    }
    val piutang = debts.filter { it.type == "piutang" && !it.isPaid }.sumOf { it.remaining }
    val utang = debts.filter { it.type == "utang" && !it.isPaid }.sumOf { it.remaining }
    val accountName = { id: String -> accounts.firstOrNull { it.id == id }?.name ?: "Rekening" }
    val visibleDebts = when (debtFilter) {
        "Piutang" -> debts.filter { it.type == "piutang" }
        "Utang" -> debts.filter { it.type == "utang" }
        else -> debts
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            "Lainnya",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.padding(top = 18.dp, bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Gaji otomatis", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Text(
                    "Atur nominal dan jadwal pemasukan otomatis.",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                Button(
                    onClick = onAddSalary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah jadwal gaji")
                }
            }

            if (salaries.isEmpty()) {
                item { EmptyHint("Belum ada jadwal gaji.") }
            } else {
                items(salaries, key = { it.id }) { s ->
                    Surface(shape = RoundedCornerShape(18.dp), color = CardBg, modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFECECF0), RoundedCornerShape(18.dp))) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(s.note, fontWeight = FontWeight.Bold, color = Ink)
                                    Text(
                                        listOf(rupiah(s.amount), freqLabel(s.frequency), "jam " + s.hourOfDay.toString().padStart(2, '0') + ":00").joinToString(" · "),
                                        color = Muted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Ke ${accountName(s.accountId)}" +
                                            if (s.lastRunMillis != null) " ( terakhir ${formatDate(s.lastRunMillis)} )" else "",
                                        color = Muted,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(checked = s.enabled, onCheckedChange = { onToggleSalary(s, it) })
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                       onClick = { onEditSalary(s) },
                                       modifier = Modifier.weight(1f).height(36.dp),
                                       shape = RoundedCornerShape(10.dp),
                                       colors = ButtonDefaults.buttonColors(
                                       containerColor = Ink,
                                       contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                       onClick = { onDeleteSalary(s) },
                                       modifier = Modifier.weight(1f).height(36.dp),
                                       shape = RoundedCornerShape(10.dp),
                                       colors = ButtonDefaults.buttonColors(
                                       containerColor = RedSoft,
                                       contentColor = Red
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                               Text("Hapus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                               }
                            }

                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Piutang / Utang", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val piutangSelected = debtFilter == "Piutang"
                    val utangSelected = debtFilter == "Utang"
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .then(
                                if (piutangSelected) Modifier.border(2.dp, Ink, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable {
                                setDebtFilter(if (debtFilter == "Piutang") "Semua" else "Piutang")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(32.dp).background(GreenSoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CallReceived, null, tint = Green, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Piutang", color = Muted, fontSize = 12.sp)
                            Text(
                                rupiah(piutang),
                                color = Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .then(
                                if (utangSelected) Modifier.border(2.dp, Ink, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable {
                                setDebtFilter(if (debtFilter == "Utang") "Semua" else "Utang")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(32.dp).background(RedSoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CallMade, null, tint = Red, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Utang", color = Muted, fontSize = 12.sp)
                            Text(
                                rupiah(utang),
                                color = Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Ink)
                        .clickable { onAddDebt(debtFilter) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when (debtFilter) {
                            "Piutang" -> "Tambah piutang"
                            "Utang" -> "Tambah utang"
                            else -> "Tambah piutang / utang"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            if (visibleDebts.isEmpty()) {
                item {
                    EmptyHint(
                        when (debtFilter) {
                            "Piutang" -> "Belum ada piutang."
                            "Utang" -> "Belum ada utang."
                            else -> "Belum ada piutang / utang."
                        }
                    )
                }
            }

            items(visibleDebts, key = { it.id }) { d ->
                val isPiutang = d.type == "piutang"
                val typeColor = if (isPiutang) Green else Red
                val typeBg = if (isPiutang) GreenSoft else RedSoft
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (d.isPaid) Color(0xFFF7F7F7) else CardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isPiutang) "Piutang" else "Utang",
                                    color = typeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(typeBg)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                                if (d.isPaid) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Lunas",
                                        color = Green,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GreenSoft)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(d.person, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                            if (d.description.isNotBlank()) {
                                Text(
                                    d.description,
                                    color = Muted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                rupiah(if (d.isPaid) d.amount else d.remaining),
                                fontWeight = FontWeight.Bold,
                                color = if (d.isPaid) Muted else Ink,
                                fontSize = 15.sp
                            )
                            if (!d.isPaid && d.paidAmount > 0L) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Sisa dari ${rupiah(d.amount)} · ${if (isPiutang) "diterima" else "dibayar"} ${rupiah(d.paidAmount)}",
                                    color = Muted,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (d.paidAmount.toFloat() / d.amount.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = typeColor,
                                    trackColor = typeBg
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        if (!d.isPaid) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (isPiutang) "Terima" else "Bayar",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(typeColor)
                                        .clickable { onPaid(d) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                                if (d.paidAmount > 0L) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Batalkan cicilan",
                                        color = Muted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onUnpaid(d) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Batalkan",
                                color = Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEEEEEE))
                                    .clickable { onUnpaid(d) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
