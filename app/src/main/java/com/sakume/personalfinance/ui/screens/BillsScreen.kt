package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.BillEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.SelectableChip
import com.sakume.personalfinance.ui.dueInLabel
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.monthKey
import com.sakume.personalfinance.ui.monthLabel
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Gold
import com.sakume.personalfinance.ui.theme.GoldSoft
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft
import com.sakume.personalfinance.ui.urgency

@Composable
fun BillsScreen(
    bills: List<BillEntity>,
    accounts: List<AccountEntity>,
    initialProvider: String = "Semua",
    onAdd: () -> Unit,
    onPay: (BillEntity) -> Unit,
    onPayMonth: (List<BillEntity>) -> Unit,
    onDelete: (BillEntity) -> Unit,
    onUnpay: (BillEntity) -> Unit = {}
) {
    var provider by remember(initialProvider) { mutableStateOf(initialProvider) }
    val unpaid = bills.filterNot { it.isPaid }
    val providers = listOf("Semua") + unpaid.map { it.provider }.distinct().sorted()
    val visible = if (provider == "Semua") unpaid else unpaid.filter { it.provider == provider }
    val totalActive = visible.sumOf { it.amount }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Tagihan", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    if (visible.isEmpty()) "Tidak ada tagihan aktif"
                    else "Total aktif ${rupiah(totalActive)}",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Ink)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(providers) { p ->
                SelectableChip(label = p, selected = provider == p, onClick = { provider = p })
            }
        }

        Spacer(Modifier.height(12.dp))

        if (visible.isEmpty()) {
            EmptyHint("Tidak ada tagihan aktif.")
            Spacer(Modifier.height(16.dp))
            Text("Riwayat lunas", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(bills.filter { it.isPaid }.take(40)) { b ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBg)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${b.provider} · ${b.itemName}",
                                color = Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(rupiah(b.amount), color = Muted, fontSize = 12.sp)
                        }
                        TextButton(onClick = { onUnpay(b) }) {
                            Text("Batalkan", fontSize = 12.sp, color = Red)
                        }
                    }
                }
            }
        } else {
            val grouped = visible.groupBy { monthKey(it.dueDateMillis) }.toSortedMap()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                grouped.forEach { (_, monthBills) ->
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    monthLabel(monthBills.first().dueDateMillis),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "${monthBills.size} tagihan · ${rupiah(monthBills.sumOf { it.amount })}",
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                            }
                            if (provider != "Semua" && monthBills.size > 1) {
                                Text(
                                    "Bayar semua",
                                    color = Ink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(CardBg)
                                        .clickable { onPayMonth(monthBills) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    items(monthBills) { b ->
                        BillCard(
                            bill = b,
                            onPay = { onPay(b) },
                            onDelete = { onDelete(b) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillCard(
    bill: BillEntity,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    val urg = urgency(bill)
    val badgeBg = when (urg) {
        "Overdue" -> RedSoft
        "Critical", "Soon" -> GoldSoft
        else -> Paper
    }
    val badgeFg = when (urg) {
        "Overdue" -> Red
        "Critical", "Soon" -> Gold
        else -> Muted
    }
    val installment = if (bill.tenorMonths > 1) " · ${bill.installmentIndex}/${bill.tenorMonths}" else ""

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        bill.provider,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        fontSize = 15.sp
                    )
                    Text(
                        "${bill.itemName}$installment",
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    rupiah(bill.amount),
                    fontWeight = FontWeight.Bold,
                    color = if (urg == "Overdue") Red else Ink,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    dueInLabel(bill.dueDateMillis),
                    color = badgeFg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(formatDate(bill.dueDateMillis), color = Muted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Hapus",
                        tint = Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ink)
                        .clickable(onClick = onPay)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Payments,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Bayar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
