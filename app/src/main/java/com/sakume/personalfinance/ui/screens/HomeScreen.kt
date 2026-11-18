package com.sakume.personalfinance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.key
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sakume.personalfinance.ui.AppPrefs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.BillEntity
import com.sakume.personalfinance.data.DebtEntity
import com.sakume.personalfinance.data.InvestmentEntity
import com.sakume.personalfinance.data.TransactionEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.MetricChip
import com.sakume.personalfinance.ui.components.QuickAction
import com.sakume.personalfinance.ui.components.SectionHeader
import com.sakume.personalfinance.ui.components.StatTile
import com.sakume.personalfinance.ui.components.TransactionRow
import com.sakume.personalfinance.ui.buildDisplayTransactions
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.isCashAccount
import com.sakume.personalfinance.ui.monthKey
import com.sakume.personalfinance.ui.monthLabel
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.startOfMonthMillis
import com.sakume.personalfinance.ui.transactionAccountLabel
import com.sakume.personalfinance.ui.timeGreeting
import com.sakume.personalfinance.ui.theme.Accent
import com.sakume.personalfinance.ui.theme.AccentSoft
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.GreenSoft
import com.sakume.personalfinance.ui.theme.Gold
import com.sakume.personalfinance.ui.theme.GoldSoft
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Panel
import com.sakume.personalfinance.ui.theme.PanelSoft
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft


private const val HIDDEN_MONEY = "Rp******"

@Composable
fun HomeScreen(
    accounts: List<AccountEntity>,
    tx: List<TransactionEntity>,
    bills: List<BillEntity>,
    investments: List<InvestmentEntity>,
    debts: List<DebtEntity>,
    onAction: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onSeeAllTx: () -> Unit = {}
) {
    val context = LocalContext.current
    var hideBalance by remember { mutableStateOf(AppPrefs.hideHomeBalance(context)) }
    fun money(value: Long): String = if (hideBalance) HIDDEN_MONEY else rupiah(value)

    val total = accounts.sumOf { it.balance }
    val monthStart = startOfMonthMillis()
    val monthTx = tx.filter { it.dateMillis >= monthStart }
    val moneyIn = monthTx.filter { it.type == "income" }.sumOf { it.amount }
    val moneyOut = monthTx.filter { it.type == "expense" }.sumOf { it.amount }
    val unpaidBills = bills.filterNot { it.isPaid }
    val threeDays = 3L * 24 * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    val dueSoon = unpaidBills.filter { it.dueDateMillis in now..(now + threeDays) }
        .sortedBy { it.dueDateMillis }
    val activeInvest = investments.filter { !it.archived && !it.isGoal }
    val activeGoals = investments.filter { !it.archived && it.isGoal }
    fun isCash(a: AccountEntity) = isCashAccount(a)
    val cashAccounts = accounts.filter { isCash(it) }
    val bankAccounts = accounts.filterNot { isCash(it) }
    val displayTx = buildDisplayTransactions(tx, accounts)

    val accountsSection = rememberSectionState(AppPrefs.HOME_SECTION_ACCOUNTS)
    val billsSection = rememberSectionState(AppPrefs.HOME_SECTION_BILLS)
    val transactionsSection = rememberSectionState(AppPrefs.HOME_SECTION_TRANSACTIONS)
    val othersSection = rememberSectionState(AppPrefs.HOME_SECTION_OTHERS)

    // Ringkasan satu baris untuk header saat section tertutup (nominal ikut hideBalance)
    val accountsSummary = if (bankAccounts.isEmpty()) "Belum ada rekening"
    else "${bankAccounts.size} rekening · ${money(bankAccounts.sumOf { it.balance })}"
    val billsSummary = if (unpaidBills.isEmpty()) "Tidak ada tagihan aktif" else buildString {
        append("${unpaidBills.size} tagihan · ${money(unpaidBills.sumOf { it.amount })}")
        if (dueSoon.isNotEmpty()) append(" · ${dueSoon.size} jatuh tempo ≤ 3 hari")
    }
    val transactionsSummary = if (displayTx.isEmpty()) "Belum ada transaksi"
    else "${displayTx.size} transaksi · terakhir ${formatDate(displayTx.first().dateMillis)}"
    val othersSummary =
        "Tabungan ${money(activeGoals.sumOf { it.value })} · Investasi ${money(activeInvest.sumOf { it.value })}"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(timeGreeting(), color = Muted, fontSize = 14.sp)
                    Text("Keuangan saya", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Ink, Panel)))
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total saldo", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(
                                onClickLabel = if (hideBalance) "Tampilkan nominal" else "Sembunyikan nominal"
                            ) {
                                hideBalance = !hideBalance
                                AppPrefs.setHideHomeBalance(context, hideBalance)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (hideBalance) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (hideBalance) "Tampilkan nominal" else "Sembunyikan nominal",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(money(total), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    QuickAction(Icons.Filled.Add, "Tambah") { onAction("transaction") }
                    QuickAction(Icons.Filled.CallReceived, "Tarik Tunai", bgColor = Color.White, iconTint = Ink) { onAction("cash") }
                    QuickAction(Icons.Filled.SwapHoriz, "Transfer") { onAction("transfer") }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PanelSoft)
                        .clickable { onAction("cashHistory") }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).background(Gold.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Payments, null, tint = Gold, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cash", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                        Text(money(cashAccounts.sumOf { it.balance }), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricChip("Masuk bulan ini", money(moneyIn), Green, Modifier.weight(1f))
                    MetricChip("Keluar bulan ini", money(moneyOut), Red, Modifier.weight(1f))
                }
            }
        }

        item(key = "section_accounts") {
            HomeSection(
                title = "Rekening saya",
                action = "Lihat semua",
                onAction = { onAction("accounts") },
                state = accountsSection,
                summary = accountsSummary
            ) {
                if (bankAccounts.isEmpty()) {
                    EmptyHint("Belum ada rekening. Tambahkan untuk mulai mencatat.")
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(bankAccounts) { a ->
                            AccountMiniCard(a, isCash = false, hidden = hideBalance) { onAction("accountHistory:${a.id}") }
                        }
                    }
                }
            }
        }

        item(key = "section_bills") {
            HomeSection(
                title = "Tagihan aktif",
                action = if (unpaidBills.isNotEmpty()) "Lihat semua" else null,
                onAction = { onAction("bills") },
                state = billsSection,
                summary = billsSummary
            ) {
                if (dueSoon.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GoldSoft,
                        modifier = Modifier.fillMaxWidth().clickable { onAction("bills") }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "⚠ Tagihan jatuh tempo ≤ 3 hari",
                                fontWeight = FontWeight.Bold,
                                color = Gold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            dueSoon.take(4).forEach { b ->
                                val daysLeft = ((b.dueDateMillis - now) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                                val dayText = when (daysLeft) {
                                    0 -> "hari ini"
                                    1 -> "besok"
                                    else -> "$daysLeft hari lagi"
                                }
                                Text(
                                    "${b.provider}, ${b.itemName} : ${rupiah(b.amount)} ( $dayText )",
                                    color = Color(0xFF8A4B2A),
                                    fontSize = 12.sp
                                )
                            }
                            if (dueSoon.size > 4) {
                                Text("${dueSoon.size - 4} lainnya", color = Color(0xFF8A4B2A), fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (unpaidBills.isEmpty()) {
                    EmptyHint("Tidak ada tagihan aktif. Bagus!")
                } else {
                    unpaidBills.groupBy { it.provider }.toList().take(4).forEach { (provider, list) ->
                        key(provider) {
                            val nearest = list.minOf { monthKey(it.dueDateMillis) }
                            val monthBills = list.filter { monthKey(it.dueDateMillis) == nearest }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CardBg,
                                modifier = Modifier.fillMaxWidth().clickable { onAction("bills:$provider") }
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(provider, fontWeight = FontWeight.SemiBold, color = Ink)
                                        Text(
                                            "${monthBills.size} tagihan · ${monthLabel(monthBills.first().dueDateMillis)}",
                                            color = Muted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(rupiah(monthBills.sumOf { it.amount }), fontWeight = FontWeight.Bold, color = Ink)
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "section_transactions") {
            HomeSection(
                title = "Transaksi terbaru",
                action = "Lihat semua",
                onAction = onSeeAllTx,
                state = transactionsSection,
                summary = transactionsSummary
            ) {
                if (displayTx.isEmpty()) {
                    EmptyHint("Transaksi terbaru akan muncul di sini.")
                } else {
                    displayTx.take(6).forEach { t ->
                        key(t.id) { TransactionRow(t, transactionAccountLabel(t, accounts)) }
                    }
                }
            }
        }

        item(key = "section_others") {
            HomeSection(
                title = "Lainnya",
                state = othersSection,
                summary = othersSummary
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        "Tabungan",
                        rupiah(activeGoals.sumOf { it.value }),
                        Icons.Filled.Savings,
                        CardBg,
                        Green,
                        Modifier.weight(1f)
                    ) { onAction("invest:goals") }
                    StatTile(
                        "Investasi",
                        rupiah(activeInvest.sumOf { it.value }),
                        Icons.Filled.TrendingUp,
                        CardBg,
                        Accent,
                        Modifier.weight(1f)
                    ) { onAction("invest") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        "Piutang",
                        rupiah(debts.filter { it.type == "piutang" && !it.isPaid }.sumOf { it.remaining }),
                        Icons.Filled.CallReceived,
                        CardBg,
                        Green,
                        Modifier.weight(1f)
                    ) { onAction("more:piutang") }
                    StatTile(
                        "Utang",
                        rupiah(debts.filter { it.type == "utang" && !it.isPaid }.sumOf { it.remaining }),
                        Icons.Filled.CallMade,
                        CardBg,
                        Red,
                        Modifier.weight(1f)
                    ) { onAction("more:utang") }
                }
            }
        }
    }
}

@Composable
private fun AccountMiniCard(a: AccountEntity, isCash: Boolean = false, hidden: Boolean = false, onClick: () -> Unit) {
    val bg = Ink
    Column(
        Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            if (isCash) "CASH" else a.type.uppercase(),
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Text(a.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(if (hidden) HIDDEN_MONEY else rupiah(a.balance), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** Status buka/tutup satu section Home; tersimpan di AppPrefs (default terbuka). */
private class SectionState(initial: Boolean, private val persist: (Boolean) -> Unit) {
    var expanded by mutableStateOf(initial)
        private set

    fun toggle() {
        expanded = !expanded
        persist(expanded)
    }
}

@Composable
private fun rememberSectionState(key: String): SectionState {
    val context = LocalContext.current
    return remember(key) {
        SectionState(AppPrefs.homeSectionExpanded(context, key)) {
            AppPrefs.setHomeSectionExpanded(context, key, it)
        }
    }
}

/**
 * Satu section = SATU item LazyColumn: header + body yang dianimasikan.
 * Jarak 12dp header-body ada di DALAM body (ikut menyusut), sehingga saat tertutup tidak ada celah kosong.
 */
@Composable
private fun HomeSection(
    title: String,
    state: SectionState,
    summary: String?,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            action = action,
            onAction = onAction,
            collapsible = true,
            expanded = state.expanded,
            summary = summary,
            onToggle = { state.toggle() }
        )
        AnimatedVisibility(
            visible = state.expanded,
            enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) +
                fadeIn(tween(300, easing = FastOutSlowInEasing)),
            exit = shrinkVertically(tween(300, easing = FastOutSlowInEasing)) +
                fadeOut(tween(200))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}
