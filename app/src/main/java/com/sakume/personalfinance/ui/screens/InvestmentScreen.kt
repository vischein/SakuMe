package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.DebtEntity
import com.sakume.personalfinance.data.InvestmentEntity
import com.sakume.personalfinance.data.InvestmentHoldingEntity
import com.sakume.personalfinance.data.ScheduledInvestmentEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.SegmentedChips
import com.sakume.personalfinance.ui.daysUntilDue
import com.sakume.personalfinance.ui.dueInLabel
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.Accent
import com.sakume.personalfinance.ui.theme.AccentMuted
import com.sakume.personalfinance.ui.theme.AccentSoft
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Gold
import com.sakume.personalfinance.ui.theme.GoldSoft
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.GreenSoft
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Panel
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft
import kotlin.math.ceil
import kotlin.math.max

private val dayNames = listOf(
    "", "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"
)

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Ink,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = Muted
)

@Composable
fun InvestmentScreen(
    items: List<InvestmentEntity>,
    accounts: List<AccountEntity>,
    schedules: List<ScheduledInvestmentEntity>,
    holdings: List<InvestmentHoldingEntity> = emptyList(),
    segment: Int,
    onSegmentChange: (Int) -> Unit,
    onAdd: () -> Unit,
    onAddGoal: () -> Unit = {},
    onEditGoal: (InvestmentEntity) -> Unit = {},
    onAddHolding: (InvestmentEntity) -> Unit = {},
    onDeleteHolding: (InvestmentHoldingEntity) -> Unit = {},
    onContribute: (InvestmentEntity) -> Unit,
    onWithdraw: (InvestmentEntity) -> Unit,
    onSchedule: (InvestmentEntity) -> Unit,
    onToggleSchedule: (ScheduledInvestmentEntity, Boolean) -> Unit,
    onDeleteSchedule: (ScheduledInvestmentEntity) -> Unit,
    onEdit: (InvestmentEntity) -> Unit = {},
    onDelete: (InvestmentEntity) -> Unit,
    debts: List<DebtEntity> = emptyList(),
    debtFilter: String = "Semua",
    onDebtFilterChange: (String) -> Unit = {},
    onAddDebt: () -> Unit = {},
    onPaidDebt: (DebtEntity) -> Unit = {},
    onUnpaidDebt: (DebtEntity) -> Unit = {},
    onEditDebt: (DebtEntity) -> Unit = {},
    onDeleteDebt: (DebtEntity) -> Unit = {}
) {
    val active = items.filterNot { it.archived }
    val goals = active.filter { it.isGoal }.sortedWith(
        compareBy<InvestmentEntity> { (it.target ?: 0L) in 1..it.value }
            .thenBy { it.deadlineMillis ?: Long.MAX_VALUE }
            .thenBy { it.name.lowercase() }
    )
    val platforms = active.filterNot { it.isGoal }
    val accountName = { id: String -> accounts.firstOrNull { it.id == id }?.name ?: "Rekening" }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (segment) {
                        0 -> "Tabungan"
                        1 -> "Investasi"
                        else -> "Piutang / Utang"
                    },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Text(
                    when (segment) {
                        0 -> "${goals.size} goal aktif"
                        1 -> "${platforms.size} platform aktif"
                        else -> "${debts.count { !it.isPaid }} belum lunas"
                    },
                    color = Muted,
                    fontSize = 13.sp
                )
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Ink)
                    .clickable {
                        when (segment) {
                            0 -> onAddGoal()
                            1 -> onAdd()
                            else -> onAddDebt()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = when (segment) {
                        0 -> "Tambah goal"
                        1 -> "Tambah platform"
                        else -> "Tambah piutang / utang"
                    },
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        SegmentedChips(
            options = listOf("Tabungan", "Investasi", "Piutang/Utang"),
            selectedIndex = segment,
            onSelect = onSegmentChange
        )
        Spacer(Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (segment == 0) {
                if (goals.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Savings,
                            title = "Mulai nabung untuk tujuanmu",
                            body = "Buat goal seperti dana darurat, liburan, atau gadget. " +
                                "Pantau progres dan atur setor otomatis tiap minggu.",
                            action = "Buat goal pertama",
                            onAction = onAddGoal
                        )
                    }
                } else {
                    item { GoalsHero(goals) }
                    items(goals, key = { it.id }) { g ->
                        GoalCard(
                            goal = g,
                            schedules = schedules.filter { it.assetId == g.id },
                            accountName = accountName,
                            onContribute = { onContribute(g) },
                            onWithdraw = { onWithdraw(g) },
                            onSchedule = { onSchedule(g) },
                            onToggleSchedule = onToggleSchedule,
                            onDeleteSchedule = onDeleteSchedule,
                            onEdit = { onEditGoal(g) },
                            onDelete = { onDelete(g) }
                        )
                    }
                }
            } else if (segment == 1) {
                if (platforms.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.TrendingUp,
                            title = "Belum ada platform investasi",
                            body = "Tambahkan platform (mis. Pluang, Bybit, Binance), lalu setor dari rekening " +
                                "dan catat aset Crypto, ETF, atau Saham di dalamnya.",
                            action = "Tambah platform",
                            onAction = onAdd
                        )
                    }
                } else {
                    item { InvestHero(platforms, holdings) }
                    items(platforms, key = { it.id }) { p ->
                        PlatformCard(
                            platform = p,
                            holdings = holdings.filter { it.platformId == p.id },
                            onContribute = { onContribute(p) },
                            onWithdraw = { onWithdraw(p) },
                            onAddHolding = { onAddHolding(p) },
                            onDeleteHolding = onDeleteHolding,
                            onSchedule = { onSchedule(p) },
                            onEdit = { onEdit(p) },
                            onDelete = { onDelete(p) }
                        )
                    }

                    val platformIds = platforms.map { it.id }.toSet()
                    val platformSchedules = schedules.filter { it.assetId in platformIds }
                    item {
                        Column(Modifier.padding(top = 6.dp)) {
                            Text("Setor otomatis", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                            Text(
                                "Setor tiap minggu di hari yang dipilih.",
                                color = Muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    if (platformSchedules.isEmpty()) {
                        item { EmptyHint("Belum ada jadwal. Pilih menu ⋮ pada platform lalu Jadwal setor.") }
                    } else {
                        items(platformSchedules, key = { it.id }) { s ->
                            ScheduleRow(
                                title = platforms.firstOrNull { it.id == s.assetId }?.name ?: "Platform",
                                schedule = s,
                                accountName = accountName,
                                card = true,
                                onToggle = { onToggleSchedule(s, it) },
                                onDelete = { onDeleteSchedule(s) }
                            )
                        }
                    }
                }
            } else {
                debtSection(
                    debts = debts,
                    debtFilter = debtFilter,
                    onDebtFilterChange = onDebtFilterChange,
                    onPaid = onPaidDebt,
                    onUnpaid = onUnpaidDebt,
                    onEdit = onEditDebt,
                    onDelete = onDeleteDebt
                )
            }
        }
    }
}

@Composable
private fun GoalsHero(goals: List<InvestmentEntity>) {
    val saved = goals.sumOf { it.value }
    val targetTotal = goals.sumOf { it.target ?: 0L }
    val counted = goals.sumOf { minOf(it.value, it.target ?: it.value) }
    val progress = if (targetTotal > 0) (counted.toFloat() / targetTotal).coerceIn(0f, 1f) else 0f
    val reached = goals.count { (it.target ?: 0L) in 1..it.value }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Ink, Panel)))
            .padding(22.dp)
    ) {
        Text("Total tabungan", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(rupiah(saved), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        ThinProgress(progress, Green, Color.White.copy(alpha = 0.15f))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${(progress * 100).toInt()}% dari ${rupiah(targetTotal)}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Text(
                "$reached/${goals.size} tercapai",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

private val holdingColors = mapOf(
    "Crypto" to Gold,
    "ETF" to AccentMuted,
    "Saham" to Green
)

@Composable
private fun InvestHero(platforms: List<InvestmentEntity>, holdings: List<InvestmentHoldingEntity>) {
    val ids = platforms.map { it.id }.toSet()
    val total = platforms.sumOf { it.value }
    val inAssets = holdings.filter { it.platformId in ids }
    val held = inAssets.sumOf { it.cost }
    val cash = (total - held).coerceAtLeast(0L)

    val segments = buildList {
        holdingColors.forEach { (type, color) ->
            val v = inAssets.filter { it.type == type }.sumOf { it.cost }
            if (v > 0) add(Triple(type, v, color))
        }
        if (cash > 0) add(Triple("Saldo platform", cash, Color.White.copy(alpha = 0.35f)))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Ink, Panel)))
            .padding(22.dp)
    ) {
        Text("Total investasi", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(rupiah(total), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        if (segments.isNotEmpty() && total > 0) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                segments.forEach { (_, v, c) ->
                    Box(Modifier.weight(v.toFloat()).fillMaxHeight().background(c))
                }
            }
            Spacer(Modifier.height(10.dp))
            segments.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    pair.forEach { (label, v, c) ->
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(c))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "$label ${rupiah(v)}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class CategoryStyle(val icon: ImageVector, val tint: Color, val soft: Color)

private fun categoryStyle(category: String): CategoryStyle = when (category) {
    "Darurat" -> CategoryStyle(Icons.Filled.Shield, Red, RedSoft)
    "Liburan" -> CategoryStyle(Icons.Filled.Flight, Accent, AccentSoft)
    "Gadget" -> CategoryStyle(Icons.Filled.PhoneAndroid, Gold, GoldSoft)
    "Kendaraan" -> CategoryStyle(Icons.Filled.DirectionsCar, Green, GreenSoft)
    "Rumah" -> CategoryStyle(Icons.Filled.Home, Accent, AccentSoft)
    "Pendidikan" -> CategoryStyle(Icons.Filled.School, Gold, GoldSoft)
    else -> CategoryStyle(Icons.Filled.Savings, Ink, Paper)
}

@Composable
private fun GoalCard(
    goal: InvestmentEntity,
    schedules: List<ScheduledInvestmentEntity>,
    accountName: (String) -> String,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    onSchedule: () -> Unit,
    onToggleSchedule: (ScheduledInvestmentEntity, Boolean) -> Unit,
    onDeleteSchedule: (ScheduledInvestmentEntity) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val style = categoryStyle(goal.category)
    val target = goal.target ?: 0L
    val done = target > 0 && goal.value >= target
    val fraction = if (target > 0) (goal.value.toFloat() / target).coerceIn(0f, 1f) else 0f
    val remaining = (target - goal.value).coerceAtLeast(0L)
    val deadline = goal.deadlineMillis
    val daysLeft = deadline?.let { daysUntilDue(it) }
    val perMonth = if (!done && daysLeft != null && daysLeft > 0) {
        remaining / max(1, ceil(daysLeft / 30.0).toInt())
    } else null
    val barColor = if (done) Green else Accent
    var menuOpen by remember(goal.id) { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(style.soft),
                contentAlignment = Alignment.Center
            ) {
                Icon(style.icon, null, tint = style.tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    goal.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(
                        goal.category.ifBlank { "Lainnya" },
                        deadline?.let { "s.d. ${formatDate(it)}" }
                    ).joinToString(" · "),
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreVert, "Menu", tint = Muted, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Ubah goal") }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem(text = { Text("Atur setor otomatis") }, onClick = { menuOpen = false; onSchedule() })
                    DropdownMenuItem(
                        text = { Text("Hapus & kembalikan dana", color = Red) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(rupiah(goal.value), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.width(6.dp))
            Text(
                "/ ${rupiah(target)}",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f).padding(bottom = 3.dp)
            )
            PillTag(
                if (done) "Tercapai" else "${(fraction * 100).toInt()}%",
                fg = if (done) Green else Accent,
                bg = if (done) GreenSoft else AccentSoft
            )
        }
        Spacer(Modifier.height(10.dp))
        ThinProgress(fraction, barColor, Paper)
        Spacer(Modifier.height(8.dp))

        if (done) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = Green, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Target tabungan sudah terpenuhi.", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Text(
                buildString {
                    append("Kurang ${rupiah(remaining)}")
                    if (perMonth != null) append(" · ±${rupiah(perMonth)}/bulan")
                },
                color = Muted,
                fontSize = 12.sp
            )
            if (deadline != null) {
                Text(
                    if (daysLeft != null && daysLeft < 0) "Tenggat sudah lewat (${dueInLabel(deadline)})" else dueInLabel(deadline),
                    color = when {
                        daysLeft != null && daysLeft < 0 -> Red
                        daysLeft != null && daysLeft <= 30 -> Gold
                        else -> Muted
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionButton(
                label = "Nabung",
                icon = Icons.Filled.Add,
                filled = true,
                modifier = Modifier.weight(1.4f),
                onClick = onContribute
            )
            ActionButton(
                label = "Ambil",
                filled = false,
                enabled = goal.value > 0,
                modifier = Modifier.weight(1f),
                onClick = onWithdraw
            )
        }

        if (schedules.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            schedules.forEach { s ->
                ScheduleRow(
                    title = "Setor otomatis",
                    schedule = s,
                    accountName = accountName,
                    card = false,
                    onToggle = { onToggleSchedule(s, it) },
                    onDelete = { onDeleteSchedule(s) }
                )
            }
        }
    }
}

@Composable
private fun PlatformCard(
    platform: InvestmentEntity,
    holdings: List<InvestmentHoldingEntity>,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    onAddHolding: () -> Unit,
    onDeleteHolding: (InvestmentHoldingEntity) -> Unit,
    onSchedule: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val held = holdings.sumOf { it.cost }
    val available = platform.value - held
    val target = platform.target
    var menuOpen by remember(platform.id) { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(AccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    platform.name.trim().take(1).uppercase().ifBlank { "?" },
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    platform.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(rupiah(platform.value), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Box {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreVert, "Menu", tint = Muted, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Jadwal setor") }, onClick = { menuOpen = false; onSchedule() })
                    DropdownMenuItem(text = { Text("Edit platform") }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text("Hapus / Refund", color = Red) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }

        if (target != null && target > 0) {
            val pct = (platform.value.toFloat() / target).coerceIn(0f, 1f)
            Spacer(Modifier.height(12.dp))
            ThinProgress(pct, Accent, Paper, height = 6.dp)
            Spacer(Modifier.height(4.dp))
            Text("${(pct * 100).toInt()}% dari target ${rupiah(target)}", color = Muted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Paper)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.AccountBalanceWallet, null, tint = Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Saldo tersedia", color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(rupiah(available), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        if (holdings.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            holdings.forEach { h ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(
                            when (h.type) {
                                "Crypto" -> Gold
                                "ETF" -> Accent
                                else -> Green
                            }
                        )
                    )
                    Spacer(Modifier.width(10.dp))
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
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Hapus aset",
                        tint = Red,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onDeleteHolding(h) }
                            .padding(6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Setor", Icons.Filled.Add, filled = true, modifier = Modifier.weight(1f), onClick = onContribute)
            ActionButton("Tarik", null, filled = false, enabled = available > 0, modifier = Modifier.weight(1f), onClick = onWithdraw)
            ActionButton("Aset", null, filled = false, modifier = Modifier.weight(1f), onClick = onAddHolding)
        }
    }
}

@Composable
private fun ScheduleRow(
    title: String,
    schedule: ScheduledInvestmentEntity,
    accountName: (String) -> String,
    card: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (card) 18.dp else 14.dp))
            .background(if (card) CardBg else Paper)
            .padding(horizontal = 12.dp, vertical = if (card) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(if (card) AccentSoft else CardBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Schedule, null, tint = Accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${rupiah(schedule.amount)} tiap ${dayNames.getOrElse(schedule.dayOfWeek) { "?" }} · dari ${accountName(schedule.accountId)}",
                color = Muted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (schedule.lastRunMillis != null) {
                Text("Terakhir ${formatDate(schedule.lastRunMillis)}", color = Muted, fontSize = 11.sp)
            }
        }
        Switch(checked = schedule.enabled, onCheckedChange = onToggle, colors = switchColors())
        Icon(
            Icons.Outlined.Delete,
            contentDescription = "Hapus jadwal",
            tint = Red,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete)
                .padding(8.dp)
        )
    }
    if (!card) Spacer(Modifier.height(6.dp))
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector? = null,
    filled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (filled) Ink else Paper
    val fg = if (filled) Color.White else if (enabled) Ink else Muted
    Row(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PillTag(text: String, fg: Color, bg: Color) {
    Text(
        text,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun ThinProgress(
    fraction: Float,
    color: Color,
    track: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height))
                .background(color)
        )
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBg)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, fontSize = 13.sp, color = Muted, textAlign = TextAlign.Center, lineHeight = 19.sp)
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Ink)
                .clickable(onClick = onAction)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(action, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
