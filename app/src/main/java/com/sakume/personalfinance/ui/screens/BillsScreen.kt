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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.BillEntity
import com.sakume.personalfinance.ui.components.ActionChip
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.SegmentedChips
import com.sakume.personalfinance.ui.daysUntilDue
import com.sakume.personalfinance.ui.dueInLabel
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.monthKey
import com.sakume.personalfinance.ui.monthLabel
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Panel
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft
import com.sakume.personalfinance.ui.urgency

private val Hairline = Ink.copy(alpha = 0.08f)

/**
 * Layar Tagihan (relayout): hero ringkasan → filter provider → daftar per bulan → riwayat lunas.
 * Tombol "tambah tagihan" dipindah ke tombol "+" di bottom bar.
 */
@Composable
fun BillsScreen(
    bills: List<BillEntity>,
    accounts: List<AccountEntity>,
    initialProvider: String = "Semua",
    onPay: (BillEntity) -> Unit,
    onPayMonth: (List<BillEntity>) -> Unit,
    onDelete: (BillEntity) -> Unit,
    onUnpay: (BillEntity) -> Unit = {}
) {
    var provider by remember(initialProvider) { mutableStateOf(initialProvider) }
    val unpaid = bills.filterNot { it.isPaid }
    val paid = bills.filter { it.isPaid }
    val providers = listOf("Semua") + unpaid.map { it.provider }.distinct().sorted()
    val visible = if (provider == "Semua") unpaid else unpaid.filter { it.provider == provider }
    val grouped = visible.groupBy { monthKey(it.dueDateMillis) }.toSortedMap()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Text("Tagihan", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        }

        item(key = "hero") {
            BillsHero(
                title = if (provider == "Semua") "Total belum dibayar" else provider,
                visible = visible
            )
        }

        item(key = "filter") {
            SegmentedChips(
                options = providers,
                selectedIndex = providers.indexOf(provider).coerceAtLeast(0),
                onSelect = { provider = providers[it] }
            )
        }

        if (visible.isEmpty()) {
            item(key = "empty") { EmptyHint("Tidak ada tagihan aktif. Tambah lewat tombol + di bawah.") }
        }

        grouped.forEach { (month, monthBills) ->
            item(key = "month-$month") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            monthLabel(monthBills.first().dueDateMillis),
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            fontSize = 17.sp
                        )
                        Text(
                            "${monthBills.size} tagihan · ${rupiah(monthBills.sumOf { it.amount })}",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    if (provider != "Semua" && monthBills.size > 1) {
                        ActionChip(label = "Bayar semua", onClick = { onPayMonth(monthBills) })
                    }
                }
            }
            items(monthBills, key = { "bill-${it.id}" }) { b ->
                BillCard(bill = b, onPay = { onPay(b) }, onDelete = { onDelete(b) })
            }
        }

        if (paid.isNotEmpty()) {
            item(key = "paid-title") {
                Text(
                    "Riwayat lunas",
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            items(paid.take(10), key = { "paid-${it.id}" }) { b ->
                PaidRow(b, onUnpay = { onUnpay(b) })
            }
        }
    }
}

@Composable
private fun BillsHero(title: String, visible: List<BillEntity>) {
    val total = visible.sumOf { it.amount }
    val overdue = visible.count { daysUntilDue(it.dueDateMillis) < 0 }
    val next = visible.minByOrNull { it.dueDateMillis }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Ink, Panel)))
            .padding(20.dp)
    ) {
        Text(title, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(rupiah(total), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            HeroPill("${visible.size} tagihan", Color.White.copy(alpha = 0.12f), Color.White)
            if (overdue > 0) HeroPill("$overdue terlambat", RedSoft, Red)
        }
        if (next != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Terdekat", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                    Text(
                        "${next.provider} · ${next.itemName}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(rupiah(next.amount), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(dueInLabel(next.dueDateMillis), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HeroPill(text: String, bg: Color, fg: Color) {
    Text(
        text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun BillCard(
    bill: BillEntity,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    val urg = urgency(bill)
    val days = daysUntilDue(bill.dueDateMillis)
    val overdue = urg == "Overdue"
    val urgent = urg == "Critical" || urg == "Soon"
    val installment = if (bill.tenorMonths > 1) " · ${bill.installmentIndex}/${bill.tenorMonths}" else ""
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(0.5.dp, Hairline, shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DayBadge(days = days, overdue = overdue, urgent = urgent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    bill.provider,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${bill.itemName}$installment",
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${dueInLabel(bill.dueDateMillis)} · ${formatDate(bill.dueDateMillis)}",
                    color = if (overdue) Red else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    rupiah(bill.amount),
                    fontWeight = FontWeight.Bold,
                    color = if (overdue) Red else Ink,
                    fontSize = 15.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Ink)
                        .clickable(onClickLabel = "Bayar ${bill.provider}", onClick = onPay)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Payments, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Bayar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Menu tagihan") { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Muted, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Hapus", color = Red) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
        if (bill.tenorMonths > 1) {
            Spacer(Modifier.height(12.dp))
            val progress = (bill.installmentIndex.toFloat() / bill.tenorMonths).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Ink.copy(alpha = 0.08f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Ink, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun DayBadge(days: Int, overdue: Boolean, urgent: Boolean) {
    val bg = when {
        overdue -> RedSoft
        urgent -> Ink
        else -> Ink.copy(alpha = 0.06f)
    }
    val fg = when {
        overdue -> Red
        urgent -> Color.White
        else -> Ink
    }
    val big = when {
        days < 0 -> (-days).toString()
        days == 0 -> "!"
        else -> days.toString()
    }
    val small = when {
        days < 0 -> "hr telat"
        days == 0 -> "hari ini"
        else -> "hari"
    }
    Column(
        Modifier
            .size(54.dp)
            .background(bg, RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(big, color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
        Text(small, color = fg.copy(alpha = 0.75f), fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun PaidRow(b: BillEntity, onUnpay: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(0.5.dp, Hairline, shape)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
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
        TextButton(onClick = onUnpay) {
            Text("Batalkan", fontSize = 12.sp, color = Red)
        }
    }
}
