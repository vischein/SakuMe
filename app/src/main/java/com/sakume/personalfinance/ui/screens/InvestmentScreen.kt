package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.InvestmentEntity
import com.sakume.personalfinance.data.InvestmentHoldingEntity
import com.sakume.personalfinance.data.ScheduledInvestmentEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.Accent
import com.sakume.personalfinance.ui.theme.AccentSoft
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.PanelSoft
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft

private val dayNames = listOf(
    "", "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"
)

@Composable
fun InvestmentScreen(
    items: List<InvestmentEntity>,
    accounts: List<AccountEntity>,
    schedules: List<ScheduledInvestmentEntity>,
    holdings: List<InvestmentHoldingEntity> = emptyList(),
    onAdd: () -> Unit,
    onAddHolding: (InvestmentEntity) -> Unit = {},
    onDeleteHolding: (InvestmentHoldingEntity) -> Unit = {},
    onContribute: (InvestmentEntity) -> Unit,
    onWithdraw: (InvestmentEntity) -> Unit,
    onSchedule: (InvestmentEntity) -> Unit,
    onToggleSchedule: (ScheduledInvestmentEntity, Boolean) -> Unit,
    onDeleteSchedule: (ScheduledInvestmentEntity) -> Unit,
    onEdit: (InvestmentEntity) -> Unit = {},
    onDelete: (InvestmentEntity) -> Unit
) {
    val active = items.filterNot { it.archived }
    val accountName = { id: String -> accounts.firstOrNull { it.id == id }?.name ?: "Rekening" }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Investasi", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    "Total aktif: ${rupiah(active.sumOf { it.value })}",
                    fontWeight = FontWeight.SemiBold,
                    color = Muted,
                    fontSize = 13.sp
                )
            }
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Platform", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 2.dp, bottom = 20.dp)
        ) {
            if (active.isEmpty()) {
                item { EmptyHint("Belum ada platform investasi (mis. Pluang, Bybit, Binance). Tambahkan platform lalu setor dari rekening.") }
            } else {
                items(active, key = { it.id }) { a ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(
                                a.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                rupiah(a.value),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            if (a.target != null && a.target > 0) {
                                val pct = (a.value.toFloat() / a.target).coerceIn(0f, 1f)
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Accent,
                                    trackColor = Paper
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${(pct * 100).toInt()}% dari target ${rupiah(a.target)}",
                                    color = Muted,
                                    fontSize = 11.sp
                                )
                            }
                            val platformHoldings = holdings.filter { it.platformId == a.id }
                            val held = platformHoldings.sumOf { it.cost }
                            val available = a.value - held
                            Text(
                                "Saldo tersedia: ${rupiah(available)}",
                                color = Muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            if (platformHoldings.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                platformHoldings.forEach { h ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                h.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = Ink,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(h.type, color = Muted, fontSize = 11.sp)
                                        }
                                        Text(rupiah(h.cost), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink)
                                        Text(
                                            "Hapus",
                                            color = Red,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .clickable { onDeleteHolding(h) }
                                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { onAddHolding(a) },
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Ink),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Aset ( Crypto / ETF / Saham )", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onContribute(a) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Ink),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Setor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { onWithdraw(a) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = available > 0,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Paper,
                                        contentColor = Ink,
                                        disabledContainerColor = Paper,
                                        disabledContentColor = Muted
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Tarik", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { onSchedule(a) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Ink,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Jadwal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onEdit(a) },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Paper,
                                        contentColor = Ink
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { onDelete(a) },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RedSoft,
                                        contentColor = Red
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Hapus / Refund", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Transfer terjadwal (mingguan)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Ink
                )
                Text(
                    "Otomatis setor setiap minggu di hari yang dipilih.",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )
            }

            if (schedules.isEmpty()) {
                item { EmptyHint("Belum ada jadwal. Ketuk Jadwal pada platform untuk menambah.") }
            } else {
                items(schedules, key = { it.id }) { s ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        active.firstOrNull { it.id == s.assetId }?.name ?: "Platform",
                                        fontWeight = FontWeight.Bold,
                                        color = Ink,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${rupiah(s.amount)} ( tiap ${dayNames.getOrElse(s.dayOfWeek) { "?" }} )",
                                        color = Muted,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Dari ${accountName(s.accountId)}" +
                                            if (s.lastRunMillis != null) " · terakhir ${formatDate(s.lastRunMillis)}" else "",
                                        color = Muted,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = s.enabled,
                                    onCheckedChange = { onToggleSchedule(s, it) }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = { onDeleteSchedule(s) },
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedSoft,
                                    contentColor = Red
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Hapus jadwal", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
