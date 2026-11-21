package com.sakume.personalfinance.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.BillEntity
import com.sakume.personalfinance.data.DebtEntity
import com.sakume.personalfinance.data.InvestmentEntity
import com.sakume.personalfinance.data.TransactionEntity
import com.sakume.personalfinance.ui.AppPrefs
import com.sakume.personalfinance.ui.buildDisplayTransactions
import com.sakume.personalfinance.ui.components.TransactionRow
import com.sakume.personalfinance.ui.daysUntilDue
import com.sakume.personalfinance.ui.dueInLabel
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.isCashAccount
import com.sakume.personalfinance.ui.monthKey
import com.sakume.personalfinance.ui.monthLabel
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.startOfMonthMillis
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Panel
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.transactionAccountLabel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val HIDDEN_MONEY = "Rp******"
private val ScreenPad = 16.dp
private val CardRadius = 20.dp
private val HeroRadius = 28.dp
private val CollapsedBarHeight = 60.dp
private val Hairline = Ink.copy(alpha = 0.08f)
private const val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000

private fun moneyText(hidden: Boolean, value: Long): String = if (hidden) HIDDEN_MONEY else rupiah(value)

private data class MonthFlow(val income: Long, val expense: Long)

private data class BillGroup(
    val provider: String,
    val count: Int,
    val monthText: String,
    val total: Long,
    val days: Int,
    val dueText: String
)

private data class RecentTx(val tx: TransactionEntity, val accountLabel: String)

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
    fun money(value: Long): String = moneyText(hideBalance, value)

    val total = remember(accounts) { accounts.sumOf { it.balance } }
    val cashTotal = remember(accounts) { accounts.filter { isCashAccount(it) }.sumOf { it.balance } }
    val monthFlow = remember(tx) {
        val monthStart = startOfMonthMillis()
        var income = 0L
        var expense = 0L
        for (t in tx) {
            if (t.dateMillis < monthStart) continue
            when (t.type) {
                "income" -> income += t.amount
                "expense" -> expense += t.amount
            }
        }
        MonthFlow(income, expense)
    }
    val miniAccounts = remember(accounts) { accounts.filterNot { isCashAccount(it) } }
    val unpaidBills = remember(bills) { bills.filterNot { it.isPaid } }
    val unpaidTotal = remember(unpaidBills) { unpaidBills.sumOf { it.amount } }
    val dueSoon = remember(unpaidBills) {
        val now = System.currentTimeMillis()
        unpaidBills
            .filter { it.dueDateMillis in now..(now + THREE_DAYS_MS) }
            .sortedBy { it.dueDateMillis }
    }
    val billGroups = remember(unpaidBills) {
        unpaidBills.groupBy { it.provider }.toList().take(4).map { (provider, list) ->
            val nearest = list.minOf { monthKey(it.dueDateMillis) }
            val monthBills = list.filter { monthKey(it.dueDateMillis) == nearest }
            val dueMillis = monthBills.minOf { it.dueDateMillis }
            BillGroup(
                provider = provider,
                count = monthBills.size,
                monthText = monthLabel(monthBills.first().dueDateMillis),
                total = monthBills.sumOf { it.amount },
                days = daysUntilDue(dueMillis),
                dueText = dueInLabel(dueMillis)
            )
        }
    }
    val goalsTotal = remember(investments) {
        investments.filter { !it.archived && it.isGoal }.sumOf { it.value }
    }
    val investTotal = remember(investments) {
        investments.filter { !it.archived && !it.isGoal }.sumOf { it.value }
    }
    val goalsCount = remember(investments) { investments.count { !it.archived && it.isGoal } }
    val goalsTargetSum = remember(investments) {
        investments.filter { !it.archived && it.isGoal }.sumOf { it.target ?: 0L }
    }
    val goalsProgress = remember(investments, goalsTargetSum) {
        if (goalsTargetSum <= 0L) 0f
        else investments.filter { !it.archived && it.isGoal && (it.target ?: 0L) > 0L }
            .sumOf { it.value }.toFloat() / goalsTargetSum
    }
    val investCount = remember(investments) { investments.count { !it.archived && !it.isGoal } }
    val piutangCount = remember(debts) { debts.count { it.type == "piutang" && !it.isPaid } }
    val utangCount = remember(debts) { debts.count { it.type == "utang" && !it.isPaid } }
    val piutangTotal = remember(debts) {
        debts.filter { it.type == "piutang" && !it.isPaid }.sumOf { it.remaining }
    }
    val utangTotal = remember(debts) {
        debts.filter { it.type == "utang" && !it.isPaid }.sumOf { it.remaining }
    }
    val displayTx = remember(tx, accounts) { buildDisplayTransactions(tx, accounts) }
    val recentTx = remember(displayTx, accounts) {
        displayTx.take(5).map { RecentTx(it, transactionAccountLabel(it, accounts)) }
    }

    val accountsSection = rememberSectionState(AppPrefs.HOME_SECTION_ACCOUNTS)
    val billsSection = rememberSectionState(AppPrefs.HOME_SECTION_BILLS)
    val transactionsSection = rememberSectionState(AppPrefs.HOME_SECTION_TRANSACTIONS)
    val othersSection = rememberSectionState(AppPrefs.HOME_SECTION_OTHERS)

    val accountsSummary = if (miniAccounts.isEmpty()) "Belum ada rekening"
    else "${miniAccounts.size} rekening · ${money(miniAccounts.sumOf { it.balance })}"
    val billsSummary = if (unpaidBills.isEmpty()) "Tidak ada tagihan aktif" else buildString {
        append("${unpaidBills.size} tagihan · ${money(unpaidTotal)}")
        if (dueSoon.isNotEmpty()) append(" · ${dueSoon.size} jatuh tempo ≤ 3 hari")
    }
    val transactionsSummary = if (displayTx.isEmpty()) "Belum ada transaksi"
    else "${displayTx.size} transaksi · terakhir ${formatDate(displayTx.first().dateMillis)}"
    val othersSummary = "Tabungan ${money(goalsTotal)} · Investasi ${money(investTotal)}"

    val density = LocalDensity.current
    val topInsetPx = WindowInsets.statusBars.getTop(density)
    val topInset = with(density) { topInsetPx.toDp() }
    val barHeightPx = topInsetPx + with(density) { CollapsedBarHeight.roundToPx() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var amountBottom by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val showBar by remember(barHeightPx) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || amountBottom < barHeightPx }
    }
    val toggleHidden = {
        hideBalance = !hideBalance
        AppPrefs.setHideHomeBalance(context, hideBalance)
    }

    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previous = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = false
            onDispose { controller.isAppearanceLightStatusBars = previous }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                if (constraints.maxHeight == Constraints.Infinity || topInsetPx <= 0) {
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                } else {
                    val h = constraints.maxHeight + topInsetPx
                    val placeable = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                    layout(placeable.width, constraints.maxHeight) { placeable.place(0, -topInsetPx) }
                }
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "top_block") {
                TopBlock(
                    topInset = topInset,
                    total = total,
                    moneyIn = monthFlow.income,
                    moneyOut = monthFlow.expense,
                    cashTotal = cashTotal,
                    hidden = hideBalance,
                    onToggleHidden = toggleHidden,
                    onAction = onAction,
                    onAmountBottom = { amountBottom = it }
                )
            }

            if (dueSoon.isNotEmpty()) {
                item(key = "attention") {
                    AttentionBanner(
                        dueSoon = dueSoon,
                        hidden = hideBalance,
                        onClick = { onAction("bills") },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item(key = "section_accounts") {
                HomeSection(
                    title = "Rekening",
                    action = "Lihat semua",
                    onAction = { onAction("accounts") },
                    state = accountsSection,
                    summary = accountsSummary,
                    bleedContent = true,
                    modifier = Modifier.animateItem()
                ) {
                    if (miniAccounts.isEmpty()) {
                        Box(Modifier.padding(horizontal = ScreenPad)) {
                            EmptyLine("Belum ada rekening. Tambahkan untuk mulai mencatat.")
                        }
                    } else {
                        AccountStrip(
                            accounts = miniAccounts,
                            hidden = hideBalance,
                            onOpen = { onAction("accountHistory:${it.id}") }
                        )
                    }
                }
            }

            item(key = "section_bills") {
                HomeSection(
                    title = "Tagihan aktif",
                    action = if (unpaidBills.isNotEmpty()) "Lihat semua" else null,
                    onAction = { onAction("bills") },
                    state = billsSection,
                    summary = billsSummary,
                    modifier = Modifier.animateItem()
                ) {
                    if (billGroups.isEmpty()) {
                        EmptyLine("Tidak ada tagihan aktif. Bagus!")
                    } else {
                        BillsCard(
                            groups = billGroups,
                            totalAmount = unpaidTotal,
                            totalCount = unpaidBills.size,
                            hidden = hideBalance,
                            onOpen = { onAction("bills:$it") }
                        )
                    }
                }
            }

            item(key = "section_transactions") {
                HomeSection(
                    title = "Transaksi terbaru",
                    action = "Lihat semua",
                    onAction = onSeeAllTx,
                    state = transactionsSection,
                    summary = transactionsSummary,
                    modifier = Modifier.animateItem()
                ) {
                    if (recentTx.isEmpty()) {
                        EmptyLine("Transaksi terbaru akan muncul di sini.")
                    } else {
                        RecentTransactionsCard(items = recentTx, hidden = hideBalance)
                    }
                }
            }

            item(key = "section_others") {
                HomeSection(
                    title = "Lainnya",
                    state = othersSection,
                    summary = othersSummary,
                    modifier = Modifier.animateItem()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MiniTile(
                                title = "Tabungan",
                                value = goalsTotal,
                                footnote = if (goalsTargetSum > 0L) "${(goalsProgress * 100).roundToInt()}% dari target" else "$goalsCount target",
                                icon = Icons.Filled.Savings,
                                hidden = hideBalance,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                progress = if (goalsTargetSum > 0L) goalsProgress else null,
                                onClick = { onAction("invest:goals") }
                            )
                            MiniTile(
                                title = "Investasi",
                                value = investTotal,
                                footnote = "$investCount platform",
                                icon = Icons.Filled.TrendingUp,
                                hidden = hideBalance,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { onAction("invest") }
                            )
                        }
                        Row(
                            Modifier.height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MiniTile(
                                title = "Piutang",
                                value = piutangTotal,
                                footnote = "$piutangCount belum lunas",
                                icon = Icons.Filled.CallReceived,
                                hidden = hideBalance,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { onAction("more:piutang") }
                            )
                            MiniTile(
                                title = "Utang",
                                value = utangTotal,
                                footnote = "$utangCount belum lunas",
                                icon = Icons.Filled.CallMade,
                                hidden = hideBalance,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { onAction("more:utang") }
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(200)) + slideInVertically(tween(250, easing = FastOutSlowInEasing)) { -it / 3 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 3 }
        ) {
            CollapsedBar(
                topInset = topInset,
                total = total,
                hidden = hideBalance,
                onToggleHidden = toggleHidden,
                onScrollTop = { scope.launch { listState.animateScrollToItem(0) } }
            )
        }
    }
}

// ───────────────────────────── Helpers ─────────────────────────────

@Composable
private fun Modifier.pressable(
    description: String? = null,
    onClickLabel: String? = null,
    pressedScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "homePressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick
        )
        .then(
            if (description != null) Modifier.clearAndSetSemantics { contentDescription = description }
            else Modifier
        )
}

/** Kartu putih dengan garis tipis — dasar semua kartu di Home. */
private fun Modifier.card(): Modifier {
    val shape = RoundedCornerShape(CardRadius)
    return this
        .clip(shape)
        .background(CardBg)
        .border(0.5.dp, Hairline, shape)
}

@Composable
private fun FitText(
    text: String,
    baseSize: Float,
    minSize: Float,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Bold,
    align: TextAlign = TextAlign.Start
) {
    var size by remember(text, baseSize) { mutableFloatStateOf(baseSize) }
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        modifier = modifier.fillMaxWidth(),
        onTextLayout = { result ->
            if (result.didOverflowWidth && size > minSize) {
                size = (size - 1f).coerceAtLeast(minSize)
            }
        }
    )
}

@Composable
private fun MoneyText(
    hidden: Boolean,
    value: Long,
    baseSize: Float,
    minSize: Float,
    color: Color,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start
) {
    Crossfade(
        targetState = hidden,
        modifier = modifier.fillMaxWidth(),
        animationSpec = tween(250),
        label = "moneyCrossfade"
    ) { h ->
        FitText(moneyText(h, value), baseSize, minSize, color, align = align)
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(18.dp)
    )
}

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

// ───────────────────────────── Hero ─────────────────────────────

@Composable
private fun TopBlock(
    topInset: Dp,
    total: Long,
    moneyIn: Long,
    moneyOut: Long,
    cashTotal: Long,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onAction: (String) -> Unit,
    onAmountBottom: (Float) -> Unit
) {
    val toggleLabel = if (hidden) "Tampilkan nominal" else "Sembunyikan nominal"
    val net = moneyIn - moneyOut
    val netText = if (hidden) HIDDEN_MONEY else (if (net >= 0) "+" else "−") + rupiah(abs(net))

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = HeroRadius, bottomEnd = HeroRadius))
            .background(Brush.verticalGradient(listOf(Ink, Panel)))
            .padding(top = topInset)
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 18.dp)
    ) {
        Text(
            "SakuMe",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(16.dp))
        Text("Total saldo", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        // Ketuk nominal total saldo = sembunyikan / tampilkan semua nominal
        Box(
            Modifier
                .fillMaxWidth()
                .pressable(
                    description = "Total saldo ${if (hidden) "disembunyikan" else rupiah(total)}",
                    onClickLabel = toggleLabel,
                    pressedScale = 0.98f,
                    onClick = onToggleHidden
                )
        ) {
            MoneyText(
                hidden,
                total,
                baseSize = 36f,
                minSize = 22f,
                color = Color.White,
                modifier = Modifier.onGloballyPositioned {
                    onAmountBottom(it.positionInRoot().y + it.size.height)
                }
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Netto bulan ini", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            Crossfade(targetState = netText, animationSpec = tween(250), label = "nettoCrossfade") { t ->
                Text(
                    t,
                    color = if (net >= 0) Green else Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(
                label = "Masuk",
                value = moneyText(hidden, moneyIn),
                icon = Icons.Filled.SouthWest,
                modifier = Modifier.weight(1f),
                onClick = { onAction("history:income") }
            )
            StatDivider()
            StatCell(
                label = "Keluar",
                value = moneyText(hidden, moneyOut),
                icon = Icons.Filled.NorthEast,
                modifier = Modifier.weight(1f),
                onClick = { onAction("history:expense") }
            )
            StatDivider()
            StatCell(
                label = "Cash",
                value = moneyText(hidden, cashTotal),
                icon = Icons.Filled.Payments,
                modifier = Modifier.weight(1f),
                onClick = { onAction("cashHistory") }
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill(
                icon = Icons.Filled.Add,
                label = "Tambah",
                modifier = Modifier.weight(1f)
            ) { onAction("transaction") }
            ActionPill(
                icon = Icons.Filled.CallReceived,
                label = "Tarik",
                primary = true,
                description = "Tarik tunai",
                modifier = Modifier.weight(1f)
            ) { onAction("cash") }
            ActionPill(
                icon = Icons.Filled.SwapHoriz,
                label = "Transfer",
                modifier = Modifier.weight(1f)
            ) { onAction("transfer") }
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(0.5.dp)
            .height(34.dp)
            .background(Color.White.copy(alpha = 0.14f))
    )
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val tap = if (onClick != null) {
        Modifier.pressable(
            description = "$label $value, buka riwayat",
            onClickLabel = "Buka riwayat $label",
            onClick = onClick
        )
    } else Modifier
    Column(modifier.then(tap).padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (onClick != null) {
                Icon(
                    Icons.Filled.ChevronRight,
                    null,
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        FitText(value, baseSize = 15f, minSize = 10f, color = Color.White)
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    description: String = label,
    onClick: () -> Unit
) {
    val fg = if (primary) Ink else Color.White
    Row(
        modifier
            .height(46.dp)
            .pressable(description = description, onClickLabel = description, pressedScale = 0.96f, onClick = onClick)
            .clip(CircleShape)
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun CollapsedBar(
    topInset: Dp,
    total: Long,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onScrollTop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toggleLabel = if (hidden) "Tampilkan nominal" else "Sembunyikan nominal"
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = CardRadius, bottomEnd = CardRadius))
            .background(Ink)
            .padding(top = topInset)
            .height(CollapsedBarHeight)
            .padding(start = 22.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(onClickLabel = toggleLabel, role = Role.Button, onClick = onToggleHidden)
        ) {
            Text("Total saldo", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
            MoneyText(hidden, total, baseSize = 20f, minSize = 12f, color = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClickLabel = "Kembali ke atas", role = Role.Button, onClick = onScrollTop),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Kembali ke atas",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ───────────────────────────── Banner ─────────────────────────────

@Composable
private fun AttentionBanner(
    dueSoon: List<BillEntity>,
    hidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val first = dueSoon.first()
    val detail = buildString {
        append("${first.provider}, ${first.itemName} · ${moneyText(hidden, first.amount)} · ${dueInLabel(first.dueDateMillis)}")
        if (dueSoon.size > 1) append(" · +${dueSoon.size - 1} lainnya")
    }
    val title = "${dueSoon.size} tagihan jatuh tempo ≤ 3 hari"
    val shape = RoundedCornerShape(CardRadius)
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPad)
            .pressable(
                description = "$title. $detail",
                onClickLabel = "Buka tagihan",
                onClick = onClick
            )
            .clip(shape)
            .background(CardBg)
            .border(1.dp, Ink, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(Ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                detail,
                color = Muted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
    }
}

// ───────────────────────────── Rekening ─────────────────────────────

@Composable
private fun AccountStrip(
    accounts: List<AccountEntity>,
    hidden: Boolean,
    onOpen: (AccountEntity) -> Unit
) {
    val count = accounts.size
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val inner = maxWidth - ScreenPad * 2
        val cardWidth = if (count == 1) inner else (inner * 0.62f).coerceIn(172.dp, 250.dp)
        LazyRow(
            contentPadding = PaddingValues(horizontal = ScreenPad),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(accounts, key = { it.id }) { a ->
                AccountChip(a, cardWidth, hidden) { onOpen(a) }
            }
        }
    }
}

@Composable
private fun AccountChip(
    a: AccountEntity,
    width: Dp,
    hidden: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .width(width)
            .pressable(
                description = "Rekening ${a.name}, ${a.type}, saldo ${if (hidden) "disembunyikan" else rupiah(a.balance)}",
                onClickLabel = "Buka riwayat ${a.name}",
                onClick = onClick
            )
            .card()
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(Ink.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                a.type,
                color = Ink.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 90.dp)
                    .clip(CircleShape)
                    .background(Ink.copy(alpha = 0.06f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            a.name,
            color = Muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        MoneyText(hidden, a.balance, baseSize = 20f, minSize = 12f, color = Ink)
    }
}

// ───────────────────────────── Tagihan ─────────────────────────────

@Composable
private fun BillsCard(
    groups: List<BillGroup>,
    totalAmount: Long,
    totalCount: Int,
    hidden: Boolean,
    onOpen: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().card()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Total belum dibayar", color = Muted, fontSize = 12.sp, maxLines = 1)
                MoneyText(hidden, totalAmount, baseSize = 22f, minSize = 12f, color = Ink)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "$totalCount tagihan",
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Ink.copy(alpha = 0.06f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = Hairline)
        groups.forEachIndexed { index, g ->
            key(g.provider) {
                BillRow(g, hidden) { onOpen(g.provider) }
                if (index != groups.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        thickness = 0.5.dp,
                        color = Hairline
                    )
                }
            }
        }
    }
}

@Composable
private fun BillRow(g: BillGroup, hidden: Boolean, onClick: () -> Unit) {
    val overdue = g.days < 0
    val urgent = g.days <= 3
    val badgeBg = if (urgent && !overdue) Ink else Ink.copy(alpha = 0.06f)
    val badgeFg = if (urgent && !overdue) Color.White else Ink
    val pillBg = when {
        overdue -> Red.copy(alpha = 0.10f)
        urgent -> Ink
        else -> Ink.copy(alpha = 0.06f)
    }
    val pillFg = when {
        overdue -> Red
        urgent -> Color.White
        else -> Muted
    }
    val initial = remember(g.provider) { g.provider.trim().firstOrNull()?.uppercase() ?: "?" }
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(
                description = "Tagihan ${g.provider}, ${g.count} tagihan, ${g.monthText}, ${g.dueText}, total ${if (hidden) "disembunyikan" else rupiah(g.total)}",
                onClickLabel = "Buka tagihan ${g.provider}",
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(badgeBg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = badgeFg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                g.provider,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${g.count} tagihan · ${g.monthText}",
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 150.dp)) {
            Text(
                moneyText(hidden, g.total),
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontSize = 15.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                g.dueText,
                color = pillFg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// ───────────────────────────── Transaksi ─────────────────────────────

/** Memakai TransactionRow yang sama dengan halaman riwayat (warna & ikon identik). */
@Composable
private fun RecentTransactionsCard(items: List<RecentTx>, hidden: Boolean) {
    Column(Modifier.fillMaxWidth().card()) {
        items.forEachIndexed { index, item ->
            key(item.tx.id) {
                TransactionRow(
                    t = item.tx,
                    accountName = item.accountLabel,
                    grouped = true,
                    hideAmount = hidden
                )
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp),
                        thickness = 0.5.dp,
                        color = Hairline
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Lainnya ─────────────────────────────

@Composable
private fun MiniTile(
    title: String,
    value: Long,
    footnote: String,
    icon: ImageVector,
    hidden: Boolean,
    modifier: Modifier,
    progress: Float? = null,
    onClick: () -> Unit
) {
    Column(
        modifier
            .pressable(
                description = "$title, ${if (hidden) "nominal disembunyikan" else rupiah(value)}, $footnote",
                onClickLabel = "Buka $title",
                onClick = onClick
            )
            .card()
            .padding(14.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(Ink.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        MoneyText(hidden, value, baseSize = 18f, minSize = 11f, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(footnote, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (progress != null) {
            Spacer(Modifier.height(8.dp))
            val animated by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "miniProgress"
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Ink.copy(alpha = 0.10f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animated)
                        .height(5.dp)
                        .background(Ink, CircleShape)
                )
            }
        }
    }
}

// ───────────────────────────── Section ─────────────────────────────

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

@Composable
private fun HomeSection(
    title: String,
    state: SectionState,
    summary: String?,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    bleedContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val chevron by animateFloatAsState(
        targetValue = if (state.expanded) 90f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "sectionChevron"
    )
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        onClickLabel = if (state.expanded) "Ciutkan bagian $title" else "Buka bagian $title",
                        role = Role.Button,
                        onClick = { state.toggle() }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = chevron }
                )
            }
            if (action != null && onAction != null) {
                Text(
                    action,
                    color = Ink.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = !state.expanded && !summary.isNullOrBlank(),
            enter = fadeIn(tween(250)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(300, easing = FastOutSlowInEasing))
        ) {
            Text(
                summary.orEmpty(),
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = ScreenPad)
            )
        }
        AnimatedVisibility(
            visible = state.expanded,
            enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) +
                fadeIn(tween(300, easing = FastOutSlowInEasing)),
            exit = shrinkVertically(tween(300, easing = FastOutSlowInEasing)) +
                fadeOut(tween(200))
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .then(if (bleedContent) Modifier else Modifier.padding(horizontal = ScreenPad)),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}
