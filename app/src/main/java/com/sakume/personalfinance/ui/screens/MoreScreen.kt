package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.DebtEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.SegmentedChips
import com.sakume.personalfinance.ui.daysUntilDue
import com.sakume.personalfinance.ui.dueInLabel
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.shareDebtReminder
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Gold
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.GreenSoft
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Panel
import com.sakume.personalfinance.ui.theme.PanelSoft
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft

private val debtFilters = listOf("Semua", "Piutang", "Utang")





internal fun LazyListScope.debtSection(
    debts: List<DebtEntity>,
    debtFilter: String,
    onDebtFilterChange: (String) -> Unit,
    onPaid: (DebtEntity) -> Unit,
    onUnpaid: (DebtEntity) -> Unit,
    onEdit: (DebtEntity) -> Unit,
    onDelete: (DebtEntity) -> Unit
) {
    val piutang = debts.filter { it.type == "piutang" && !it.isPaid }.sumOf { it.remaining }
    val utang = debts.filter { it.type == "utang" && !it.isPaid }.sumOf { it.remaining }
    val visibleDebts = when (debtFilter) {
        "Piutang" -> debts.filter { it.type == "piutang" }
        "Utang" -> debts.filter { it.type == "utang" }
        else -> debts
    }

    item(key = "debt-hero") { DebtHero(piutang = piutang, utang = utang) }

    item(key = "debt-filter") {
        SegmentedChips(
            options = debtFilters,
            selectedIndex = debtFilters.indexOf(debtFilter).coerceAtLeast(0),
            onSelect = { onDebtFilterChange(debtFilters[it]) }
        )
    }

    if (visibleDebts.isEmpty()) {
        item(key = "debt-empty") {
            EmptyHint(
                when (debtFilter) {
                    "Piutang" -> "Belum ada piutang. Tambah lewat tombol + di bawah."
                    "Utang" -> "Belum ada utang. Tambah lewat tombol + di bawah."
                    else -> "Belum ada piutang / utang. Tambah lewat tombol + di bawah."
                }
            )
        }
    }

    items(visibleDebts, key = { "debt-${it.id}" }) { d ->
        DebtCard(
            d = d,
            onPaid = { onPaid(d) },
            onUnpaid = { onUnpaid(d) },
            onEdit = { onEdit(d) },
            onDelete = { onDelete(d) }
        )
    }
}

@Composable
private fun DebtHero(piutang: Long, utang: Long) {
    val net = piutang - utang
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Ink, Panel)))
            .padding(20.dp)
    ) {
        Text("Posisi bersih", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            if (net < 0) "-" + rupiah(-net) else rupiah(net),
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            when {
                net > 0 -> "Orang lain berutang lebih banyak padamu"
                net < 0 -> "Kamu punya lebih banyak utang"
                else -> "Piutang dan utang seimbang"
            },
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroTile("Piutang", piutang, Green, Icons.Filled.CallReceived, Modifier.weight(1f))
            HeroTile("Utang", utang, Red, Icons.Filled.CallMade, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroTile(label: String, value: Long, tint: Color, icon: ImageVector, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PanelSoft)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).background(tint.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
            Text(
                rupiah(value),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DebtCard(
    d: DebtEntity,
    onPaid: () -> Unit,
    onUnpaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPiutang = d.type == "piutang"
    val typeColor = if (isPiutang) Green else Red
    val typeBg = if (isPiutang) GreenSoft else RedSoft
    val context = LocalContext.current
    var menuOpen by remember(d.id) { mutableStateOf(false) }
    val dueMillis = d.dueDateMillis
    val daysLeft = if (dueMillis != null && !d.isPaid) daysUntilDue(dueMillis) else null

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (d.isPaid) Color(0xFFF7F7F7) else CardBg)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(typeBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                d.person.trim().take(1).uppercase().ifBlank { "?" },
                color = typeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(if (isPiutang) "Piutang" else "Utang", typeColor, typeBg)
                if (d.isPaid) {
                    Spacer(Modifier.width(6.dp))
                    Tag("Lunas", Green, GreenSoft)
                }
                if (daysLeft != null && daysLeft < 0) {
                    Spacer(Modifier.width(6.dp))
                    Tag("Terlambat", Red, RedSoft)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                d.person,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (d.description.isNotBlank()) {
                Text(d.description, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                rupiah(if (d.isPaid) d.amount else d.remaining),
                fontWeight = FontWeight.Bold,
                color = if (d.isPaid) Muted else Ink,
                fontSize = 17.sp
            )
            if (!d.isPaid && d.paidAmount > 0L) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Sisa dari ${rupiah(d.amount)} · ${if (isPiutang) "diterima" else "dibayar"} ${rupiah(d.paidAmount)}",
                    color = Muted,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(typeBg)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((d.paidAmount.toFloat() / d.amount.toFloat()).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(typeColor)
                    )
                }
            }
            if (dueMillis != null && !d.isPaid) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Jatuh tempo ${formatDate(dueMillis)} · ${dueInLabel(dueMillis)}",
                    color = when {
                        daysLeft != null && daysLeft < 0 -> Red
                        daysLeft != null && daysLeft <= 3 -> Gold
                        else -> Muted
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Box {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreVert, "Menu", tint = Muted, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Ubah") }, onClick = { menuOpen = false; onEdit() })
                    if (isPiutang && !d.isPaid) {
                        DropdownMenuItem(
                            text = { Text("Kirim pesan tagih") },
                            onClick = { menuOpen = false; shareDebtReminder(context, d) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Hapus", color = Red) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (!d.isPaid) {
                Text(
                    if (isPiutang) "Terima" else "Bayar",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(typeColor)
                        .clickable(onClick = onPaid)
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
                            .clickable(onClick = onUnpaid)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
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
                        .clickable(onClick = onUnpaid)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Tag(text: String, fg: Color, bg: Color) {
    Text(
        text,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
