package com.sakume.personalfinance.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.BillEntity
import com.sakume.personalfinance.ui.components.ActionChip
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.ProviderLogo
import com.sakume.personalfinance.ui.components.SegmentedChips
import com.sakume.personalfinance.ui.components.providerStyle
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

private val Hairline = Ink.copy(alpha = 0.0f)

private const val PAID_PREVIEW_COUNT = 4

@OptIn(ExperimentalFoundationApi::class)
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
    var showAllPaid by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Any>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val unpaid = bills.filterNot { it.isPaid }
    val paid = remember(bills) {
        bills.filter { it.isPaid }
            .sortedByDescending { it.paidAtMillis ?: it.dueDateMillis }
    }
    val providers = listOf("Semua") + unpaid.map { it.provider }.distinct().sorted()
    val visible = if (provider == "Semua") unpaid else unpaid.filter { it.provider == provider }
    val grouped = visible.groupBy { monthKey(it.dueDateMillis) }.toSortedMap()

    val paidVisible = if (showAllPaid) paid else paid.take(PAID_PREVIEW_COUNT)
    val hasMorePaid = paid.size > PAID_PREVIEW_COUNT

    val selectedBills = unpaid.filter { it.id in selectedIds }
    val selecting = selectedBills.isNotEmpty()

    fun clearSelection() { selectedIds = emptySet() }
    fun toggle(b: BillEntity) {
        selectedIds = if (b.id in selectedIds) selectedIds - b.id else selectedIds + b.id
    }

    BackHandler(enabled = selecting) { clearSelection() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (selecting) {
            stickyHeader(key = "selection-bar") {
                SelectionBar(
                    count = selectedBills.size,
                    onClose = { clearSelection() },
                    onSelectAll = { selectedIds = visible.map { it.id }.toSet() },
                    onDelete = { confirmDelete = true }
                )
            }
        } else {
            item(key = "title") {
                Text("Tagihan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
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
                onSelect = {
                    provider = providers[it]
                    clearSelection()
                }
            )
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                EmptyHint("Belum ada tagihan aktif")
            }
        }

        grouped.forEach { (key, monthBills) ->
            item(key = "month-$key") {
                SectionHeader(
                    title = monthLabel(monthBills.first().dueDateMillis),
                    meta = "${monthBills.size} tagihan",
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        rupiah(monthBills.sumOf { it.amount }),
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    if (provider != "Semua" && monthBills.size > 1 && !selecting) {
                        Spacer(Modifier.width(8.dp))
                        ActionChip(label = "Bayar semua", onClick = { onPayMonth(monthBills) })
                    }
                }
            }
            items(monthBills, key = { "bill-${it.id}" }) { b ->
                BillCard(
                    bill = b,
                    selecting = selecting,
                    selected = b.id in selectedIds,
                    onPay = { onPay(b) },
                    onToggle = { toggle(b) }
                )
            }
        }

        if (paid.isNotEmpty()) {
            item(key = "paid-title") {
                SectionHeader(
                    title = "Riwayat lunas",
                    meta = "${paid.size}",
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    if (hasMorePaid) {
                        HeaderAction(
                            label = if (showAllPaid) "Sembunyikan" else "Lihat semua",
                            onClick = { showAllPaid = !showAllPaid }
                        )
                    }
                }
            }
            items(paidVisible, key = { "paid-${it.id}" }) { b ->
                PaidRow(b, onUnpay = { onUnpay(b) })
            }
        }
    }

    if (confirmDelete && selecting) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus ${selectedBills.size} tagihan?", fontWeight = FontWeight.Bold) },
            text = { Text("Tagihan yang dihapus tidak bisa dikembalikan.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedBills.forEach(onDelete)
                    clearSelection()
                    confirmDelete = false
                }) { Text("Hapus", color = Red, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Batal", color = Ink) }
            }
        )
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Paper)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = "Batal pilih", onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Batal pilih", tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("$count dipilih", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
        Spacer(Modifier.weight(1f))
        HeaderAction(label = "Pilih semua", onClick = onSelectAll)
        Spacer(Modifier.width(4.dp))
        Row(
            Modifier
                .clip(CircleShape)
                .background(RedSoft)
                .clickable(onClickLabel = "Hapus tagihan terpilih", onClick = onDelete)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Delete, null, tint = Red, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Hapus", color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            color = Ink,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (meta != null) {
            Spacer(Modifier.width(6.dp))
            Text(meta, color = Muted, fontSize = 12.sp, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun BillsHero(title: String, visible: List<BillEntity>) {
    val total = visible.sumOf { it.amount }
    val overdue = visible.count { daysUntilDue(it.dueDateMillis) < 0 }
    val next = visible.minByOrNull { it.dueDateMillis }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Ink, Panel)))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    rupiah(total),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                HeroPill("${visible.size} tagihan", Color.White.copy(alpha = 0.12f), Color.White)
                if (overdue > 0) HeroPill("$overdue terlambat", RedSoft, Red)
            }
        }
        if (next != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Terdekat", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
                    Text(
                        "${next.provider} · ${next.itemName}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        rupiah(next.amount),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        dueInLabel(next.dueDateMillis),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
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
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillCard(
    bill: BillEntity,
    selecting: Boolean,
    selected: Boolean,
    onPay: () -> Unit,
    onToggle: () -> Unit
) {
    val overdue = urgency(bill) == "Overdue"
    val style = remember(bill.provider) { providerStyle(bill.provider) }
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(22.dp)
    val borderColor = when {
        selected -> Ink
        overdue -> Red.copy(alpha = 0.35f)
        else -> Hairline
    }
    val tint = if (overdue) Red.copy(alpha = 0.08f) else style.fg.copy(alpha = 0.10f)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(if (selected) 1.5.dp else 0.dp, borderColor, shape)
            .combinedClickable(
                onClickLabel = if (selecting) "Pilih tagihan" else null,
                onClick = { if (selecting) onToggle() },
                onLongClickLabel = "Pilih tagihan",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }
            )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(tint, Color.Transparent)))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderLogo(provider = bill.provider, overdue = overdue)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        bill.provider,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        bill.itemName,
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (selecting) {
                    SelectCheck(selected)
                } else {
                    Text(
                        dueInLabel(bill.dueDateMillis),
                        color = if (overdue) Red else Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (overdue) RedSoft else Color.White.copy(alpha = 0.75f))
                            .border(0.5.dp, if (overdue) Color.Transparent else Hairline, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            if (bill.tenorMonths > 1) {
                Spacer(Modifier.height(12.dp))
                InstallmentBar(bill.installmentIndex, bill.tenorMonths, overdue)
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .offset(x = (-7).dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Paper)
            )
            DashedLine(Modifier.weight(1f))
            Box(
                Modifier
                    .offset(x = 7.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Paper)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Jatuh tempo · ${formatDate(bill.dueDateMillis)}",
                    color = if (overdue) Red else Muted,
                    fontSize = 11.sp,
                    fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    rupiah(bill.amount),
                    color = if (overdue) Red else Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (!selecting) {
                Spacer(Modifier.width(10.dp))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Ink, Panel)))
                        .clickable(onClickLabel = "Bayar ${bill.provider}", onClick = onPay)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Payments, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bayar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SelectCheck(selected: Boolean) {
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) Ink else Color.Transparent)
            .border(1.5.dp, if (selected) Ink else Ink.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Terpilih", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun InstallmentBar(index: Int, tenor: Int, overdue: Boolean) {
    val fill = if (overdue) Red else Ink
    val track = Ink.copy(alpha = 0.10f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (tenor <= 12) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(tenor) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(if (i < index) fill else track)
                    )
                }
            }
        } else {
            val progress = (index.toFloat() / tenor).coerceIn(0f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(fill, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("$index/$tenor", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun DashedLine(modifier: Modifier = Modifier) {
    val color = Ink.copy(alpha = 0.18f)
    Canvas(modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
        )
    }
}



@Composable
private fun PaidRow(b: BillEntity, onUnpay: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(0.5.dp, Hairline, shape)
            .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${b.provider} · ${b.itemName}",
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(rupiah(b.amount), color = Muted, fontSize = 12.sp, maxLines = 1, softWrap = false)
        Text(
            "Batalkan",
            fontSize = 12.sp,
            color = Red,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClickLabel = "Batalkan pelunasan", onClick = onUnpay)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        )
    }
}