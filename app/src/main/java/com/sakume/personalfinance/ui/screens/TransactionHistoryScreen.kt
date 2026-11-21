package com.sakume.personalfinance.ui.screens

import com.sakume.personalfinance.ui.components.FilterSection
import com.sakume.personalfinance.ui.components.UnifiedFilterControl
import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.TransactionEntity
import com.sakume.personalfinance.ui.buildDisplayTransactions
import com.sakume.personalfinance.ui.components.ActionChip
import com.sakume.personalfinance.ui.components.TransactionRow
import com.sakume.personalfinance.ui.dayHeaderLabel
import com.sakume.personalfinance.ui.displayCategory
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.startOfDayMillis
import com.sakume.personalfinance.ui.startOfMonthMillis
import com.sakume.personalfinance.ui.startOfWeekMillis
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.GreenSoft
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft
import com.sakume.personalfinance.ui.transactionAccountLabel
import java.util.Calendar

data class HistoryFilter(
    val type: String? = null,
    val categories: Set<String>? = null,
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val accountId: String? = null,
    val title: String? = null
)

private fun HistoryFilter.matches(t: TransactionEntity): Boolean {
    if (type != null || categories != null) {
        val isIncome = t.type.equals("income", true)
        val isExpense = t.type.equals("expense", true)
        if (!isIncome && !isExpense) return false
        if (type != null && !t.type.equals(type, true)) return false
    }
    if (categories != null && normalizeCategory(t.category.ifBlank { t.note }) !in categories) return false
    if (startMillis != null && t.dateMillis < startMillis) return false
    if (endMillis != null && t.dateMillis >= endMillis) return false
    return true
}

private enum class HistoryPeriod(val label: String) {
    ALL("Semua"),
    TODAY("Hari ini"),
    WEEK("Minggu ini"),
    MONTH("Bulan ini"),
    DAY("Tanggal")
}

private enum class HistoryType(val label: String) {
    ALL("Semua"),
    IN("Masuk"),
    OUT("Keluar"),
    INTERNAL("Transfer")
}

private class HistoryEntry(
    val t: TransactionEntity,
    val accountLabel: String,
    val internal: Boolean,
    val search: String
) {
    val isIncome: Boolean get() = t.type.equals("income", true)
}

private class DayGroup(
    val dayStart: Long,
    val label: String,
    val items: List<HistoryEntry>,
    val income: Long,
    val expense: Long,
    val hasFlow: Boolean
) {
    val net: Long get() = income - expense
}

private class FlowTotals(val income: Long, val expense: Long) {
    val net: Long get() = income - expense
}

private fun nextDayMillis(dayStart: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = dayStart
        add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

private fun signedRupiah(v: Long): String = when {
    v > 0 -> "+${rupiah(v)}"
    v < 0 -> "-${rupiah(-v)}"
    else -> rupiah(0)
}

private fun netColor(v: Long): Color = when {
    v > 0 -> Green
    v < 0 -> Red
    else -> Muted
}

private fun cardShape(index: Int, lastIndex: Int): RoundedCornerShape {
    val r = 16.dp
    return RoundedCornerShape(
        topStart = if (index == 0) r else 0.dp,
        topEnd = if (index == 0) r else 0.dp,
        bottomStart = if (index == lastIndex) r else 0.dp,
        bottomEnd = if (index == lastIndex) r else 0.dp
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionHistoryScreen(
    accounts: List<AccountEntity>,
    tx: List<TransactionEntity>,
    onBack: () -> Unit,
    onDeleteMany: (List<String>) -> Unit = {},
    accountFilter: String? = null,
    initialFilter: HistoryFilter? = null
) {
    var activeFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var period by rememberSaveable { mutableStateOf(HistoryPeriod.ALL) }
    var typeFilter by rememberSaveable { mutableStateOf(HistoryType.ALL) }
    var dayMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    val visibleTransactions = remember(tx, accounts, accountFilter, activeFilter) {
        val f = activeFilter
        val accountId = f?.accountId ?: accountFilter
        val source = tx.filter { t ->
            (accountId == null || t.accountId == accountId) && (f == null || f.matches(t))
        }
        buildDisplayTransactions(source, accounts)
    }

    val entries = remember(visibleTransactions, accounts) {
        visibleTransactions.map { t ->
            val label = transactionAccountLabel(t, accounts)
            HistoryEntry(
                t = t,
                accountLabel = label,
                internal = t.transferGroupId != null || t.type.startsWith("transfer", true),
                search = "${t.note} ${t.category} ${displayCategory(t.note.ifBlank { t.category })} $label".lowercase()
            )
        }
    }

    val filtered = remember(entries, period, dayMillis, typeFilter, query) {
        val range: LongRange? = when (period) {
            HistoryPeriod.ALL -> null
            HistoryPeriod.TODAY -> startOfDayMillis().let { it until nextDayMillis(it) }
            HistoryPeriod.WEEK -> startOfWeekMillis()..Long.MAX_VALUE
            HistoryPeriod.MONTH -> startOfMonthMillis()..Long.MAX_VALUE
            HistoryPeriod.DAY -> dayMillis?.let { d -> startOfDayMillis(d).let { it until nextDayMillis(it) } }
        }
        val q = query.trim().lowercase()
        entries.filter { e ->
            (range == null || e.t.dateMillis in range) &&
                when (typeFilter) {
                    HistoryType.ALL -> true
                    HistoryType.IN -> !e.internal && e.isIncome
                    HistoryType.OUT -> !e.internal && !e.isIncome
                    HistoryType.INTERNAL -> e.internal
                } &&
                (q.isEmpty() || e.search.contains(q))
        }
    }

    val totals = remember(filtered) {
        var inc = 0L
        var out = 0L
        filtered.forEach { e ->
            if (!e.internal) if (e.isIncome) inc += e.t.amount else out += e.t.amount
        }
        FlowTotals(inc, out)
    }

    val groups = remember(filtered) {
        val today = startOfDayMillis()
        val cal = Calendar.getInstance()
        val byDay = LinkedHashMap<Long, MutableList<HistoryEntry>>()
        filtered.forEach { e ->
            cal.timeInMillis = e.t.dateMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            byDay.getOrPut(cal.timeInMillis) { ArrayList() }.add(e)
        }
        byDay.map { (day, list) ->
            var inc = 0L
            var out = 0L
            var flow = false
            list.forEach { e ->
                if (!e.internal) {
                    flow = true
                    if (e.isIncome) inc += e.t.amount else out += e.t.amount
                }
            }
            DayGroup(day, dayHeaderLabel(day, today), list, inc, out, flow)
        }
    }

    val visibleIds = remember(filtered) { filtered.mapTo(HashSet()) { it.t.id } }
    val selectedVisible = remember(selected, visibleIds) { selected.filterTo(HashSet()) { it in visibleIds } }
    val allSelected = visibleIds.isNotEmpty() && selectedVisible.size == visibleIds.size

    fun exitSelection() {
        selectionMode = false
        selected = emptySet()
    }

    fun enterSelection(id: String) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        selectionMode = true
        selected = selected + id
    }

    fun closeSearch() {
        searchOpen = false
        query = ""
        focusManager.clearFocus()
        keyboard?.hide()
    }

    fun resetFilters() {
        period = HistoryPeriod.ALL
        typeFilter = HistoryType.ALL
        dayMillis = null
        activeFilter = null
        closeSearch()
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
                period = HistoryPeriod.DAY
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    BackHandler(enabled = selectionMode || searchOpen) {
        if (selectionMode) exitSelection() else closeSearch()
    }

    LaunchedEffect(query) { listState.scrollToItem(0) }

    val f = activeFilter
    val accountId = f?.accountId ?: accountFilter
    val accountName = accountId?.let { id -> accounts.firstOrNull { it.id == id }?.name }
    val title: String = f?.title
        ?: if (accountId != null) {
            if (accountName != null) "Histori rekening: $accountName" else "Histori rekening"
        } else {
            "Histori transaksi"
        }
    val hasActiveControls = period != HistoryPeriod.ALL || typeFilter != HistoryType.ALL || f != null
    val canReset = hasActiveControls || query.isNotBlank()
    val bottomPad by animateDpAsState(if (selectionMode) 104.dp else 24.dp, tween(220), label = "historyBottomPad")
    val dividerStart by animateDpAsState(if (selectionMode) 96.dp else 64.dp, tween(200), label = "historyDividerStart")

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HistoryHeader(
                title = title,
                subtitle = if (selectionMode) "Ketuk tanggal untuk pilih semua di hari itu" else "${filtered.size} transaksi",
                selectionMode = selectionMode,
                selectedCount = selectedVisible.size,
                allSelected = allSelected,
                searchOpen = searchOpen,
                onBack = onBack,
                onToggleSearch = { if (searchOpen) closeSearch() else searchOpen = true },
                onEnterSelection = { selectionMode = true },
                onExitSelection = ::exitSelection,
                onToggleAll = { selected = if (allSelected) emptySet() else visibleIds.toSet() }
            )

            AnimatedVisibility(
                visible = searchOpen,
                enter = fadeIn(tween(180)) + expandVertically(tween(240, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(200, easing = FastOutSlowInEasing))
            ) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSubmit = { focusManager.clearFocus(); keyboard?.hide() },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = bottomPad)
            ) {
                item(key = "summary", contentType = "summary") {
                    SummaryCard(
                        income = totals.income,
                        expense = totals.expense,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }

                item(key = "controls", contentType = "controls") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .padding(bottom = 6.dp)
                    ) {
                        UnifiedFilterControl(
                            title = f?.title ?: when {
                                period == HistoryPeriod.ALL && typeFilter == HistoryType.ALL -> "Semua transaksi"
                                typeFilter != HistoryType.ALL -> typeFilter.label
                                else -> period.label
                            },
                            summary = "${accountName ?: "Semua rekening"}, ${if (period == HistoryPeriod.DAY && dayMillis != null) formatDate(dayMillis!!) else period.label}, ${typeFilter.label}",
                            sections = listOf(
                                FilterSection(
                                    title = "Periode",
                                    options = listOf("Semua waktu", "Hari ini", "Minggu ini", "Bulan ini", "Pilih tanggal"),
                                    selectedIndex = when (period) {
                                        HistoryPeriod.ALL -> 0
                                        HistoryPeriod.TODAY -> 1
                                        HistoryPeriod.WEEK -> 2
                                        HistoryPeriod.MONTH -> 3
                                        HistoryPeriod.DAY -> 4
                                    }
                                ),
                                FilterSection(
                                    title = "Jenis transaksi",
                                    options = listOf("Semua", "Masuk", "Keluar", "Transfer / Tunai"),
                                    selectedIndex = typeFilter.ordinal
                                )
                            ),
                            onApply = { selected ->
                                when (selected[0]) {
                                    0 -> { period = HistoryPeriod.ALL; dayMillis = null }
                                    1 -> { period = HistoryPeriod.TODAY; dayMillis = null }
                                    2 -> { period = HistoryPeriod.WEEK; dayMillis = null }
                                    3 -> { period = HistoryPeriod.MONTH; dayMillis = null }
                                    4 -> openDatePicker()
                                }
                                typeFilter = HistoryType.entries[selected[1].coerceIn(0, HistoryType.entries.lastIndex)]
                            },
                            onReset = ::resetFilters
                        )
                    }
                }

                if (groups.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        HistoryEmptyState(
                            noData = entries.isEmpty() && f == null,
                            canReset = canReset,
                            onReset = ::resetFilters
                        )
                    }
                }

                groups.forEach { g ->
                    stickyHeader(key = "h_${g.dayStart}", contentType = "dayHeader") {
                        val ids = g.items.map { it.t.id }
                        val n = ids.count { it in selectedVisible }
                        DayHeader(
                            group = g,
                            selectionMode = selectionMode,
                            checkState = when (n) {
                                0 -> 0
                                ids.size -> 2
                                else -> 1
                            },
                            onClick = {
                                selected = if (n == ids.size) selected - ids.toSet() else selected + ids
                            }
                        )
                    }
                    itemsIndexed(g.items, key = { _, e -> e.t.id }, contentType = { _, _ -> "row" }) { index, e ->
                        Column(
                            Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .clip(cardShape(index, g.items.lastIndex))
                                .background(CardBg)
                        ) {
                            TransactionRow(
                                t = e.t,
                                accountName = e.accountLabel,
                                selected = e.t.id in selectedVisible,
                                selectionMode = selectionMode,
                                grouped = true,
                                showDate = false,
                                onToggleSelect = {
                                    selected = if (e.t.id in selected) selected - e.t.id else selected + e.t.id
                                },
                                onLongPress = if (selectionMode) null else ({ enterSelection(e.t.id) })
                            )
                            if (index != g.items.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = dividerStart),
                                    thickness = 0.5.dp,
                                    color = Ink.copy(alpha = 0.07f)
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectionMode,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(120))
        ) {
            val count = selectedVisible.size
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .shadow(
                        8.dp,
                        RoundedCornerShape(20.dp),
                        ambientColor = Ink.copy(alpha = 0.12f),
                        spotColor = Ink.copy(alpha = 0.18f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("$count dipilih", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        if (count == 0) "Pilih transaksi yang ingin dihapus" else "Siap dihapus",
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = {
                        onDeleteMany(selectedVisible.toList())
                        exitSelection()
                    },
                    enabled = count > 0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red,
                        disabledContainerColor = Ink.copy(alpha = 0.10f),
                        disabledContentColor = Muted
                    )
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    title: String,
    subtitle: String,
    selectionMode: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    searchOpen: Boolean,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onEnterSelection: () -> Unit,
    onExitSelection: () -> Unit,
    onToggleAll: () -> Unit
) {
    Crossfade(targetState = selectionMode, animationSpec = tween(180), label = "historyHeader") { selecting ->
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    if (selecting) "$selectedCount dipilih" else title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    subtitle,
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selecting) {
                ActionChip(
                    label = if (allSelected) "Kosongkan" else "Pilih semua",
                    onClick = onToggleAll
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (searchOpen) Ink else CardBg)
                            .border(
                                width = 1.dp,
                                color = if (searchOpen) Ink else Ink.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                            .clickable(
                                onClickLabel = if (searchOpen) "Tutup pencarian" else "Cari transaksi",
                                role = Role.Button,
                                onClick = onToggleSearch
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searchOpen) "Tutup pencarian" else "Cari transaksi",
                            tint = if (searchOpen) Color.White else Ink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    ActionChip(
                        label = "Pilih",
                        icon = Icons.Outlined.CheckCircle,
                        contentDescription = "Pilih transaksi",
                        onClick = onEnterSelection
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(CardBg)
            .border(1.dp, Ink.copy(alpha = 0.09f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = Muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Cari catatan, kategori, rekening", color = Muted, fontSize = 14.sp, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 14.sp),
                cursorBrush = SolidColor(Ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        }
        if (query.isNotEmpty()) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Ink.copy(alpha = 0.08f))
                    .clickable(onClickLabel = "Hapus kata kunci", role = Role.Button) { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Hapus kata kunci", tint = Muted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(income: Long, expense: Long, modifier: Modifier = Modifier) {
    val net = income - expense
    val totalFlow = (income + expense).coerceAtLeast(1L)
    val targetRatio = if (income == 0L && expense == 0L) 0.5f else income.toFloat() / totalFlow.toFloat()
    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio.coerceIn(0.04f, 0.96f),
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "flowRatio"
    )

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, Ink.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Arus kas bersih (Netto)",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Crossfade(targetState = net, animationSpec = tween(220), label = "summaryNet") { v ->
                    Text(
                        text = signedRupiah(v),
                        color = netColor(v),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (income > 0L || expense > 0L) {
                val (badgeBg, badgeText, badgeLabel) = when {
                    net > 0L -> Triple(GreenSoft, Green, "Surplus")
                    net < 0L -> Triple(RedSoft, Red, "Defisit")
                    else -> Triple(Ink.copy(alpha = 0.07f), Muted, "Seimbang")
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (income > 0L || expense > 0L) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(RedSoft),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (income > 0L) {
                    Box(
                        modifier = Modifier
                            .weight(animatedRatio)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Green)
                    )
                }
                if (expense > 0L) {
                    Box(
                        modifier = Modifier
                            .weight(1f - animatedRatio)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Red)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Ink.copy(alpha = 0.07f))
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowColumnItem(
                label = "Masuk",
                value = income,
                tint = Green,
                soft = GreenSoft,
                icon = Icons.Filled.SouthWest,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Ink.copy(alpha = 0.08f))
            )

            FlowColumnItem(
                label = "Keluar",
                value = expense,
                tint = Red,
                soft = RedSoft,
                icon = Icons.Filled.NorthEast,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FlowColumnItem(
    label: String,
    value: Long,
    tint: Color,
    soft: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(soft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                color = Muted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Crossfade(targetState = value, animationSpec = tween(220), label = "flow$label") { v ->
                Text(
                    text = rupiah(v),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DayHeader(
    group: DayGroup,
    selectionMode: Boolean,
    checkState: Int,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Paper)
            .then(
                if (selectionMode) Modifier.clickable(
                    onClickLabel = if (checkState == 2) "Lepas pilihan hari ini" else "Pilih semua transaksi hari ini",
                    role = Role.Button,
                    onClick = onClick
                ) else Modifier
            )
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = selectionMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (checkState == 2) Ink else Color.Transparent)
                        .border(1.5.dp, if (checkState == 0) Color(0xFFCCCCCC) else Ink, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when (checkState) {
                        2 -> Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        1 -> Box(Modifier.size(width = 8.dp, height = 2.dp).background(Ink))
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        Text(
            group.label,
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (group.hasFlow) {
            Spacer(Modifier.width(8.dp))
            Text(
                signedRupiah(group.net),
                color = netColor(group.net),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(noData: Boolean, canReset: Boolean, onReset: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).background(Ink.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (noData) Icons.Filled.ReceiptLong else Icons.Filled.SearchOff,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (noData) "Belum ada transaksi" else "Tidak ada hasil untuk filter ini",
            color = Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (noData) "Transaksi yang kamu catat akan muncul di sini."
            else "Coba ubah periode, tipe, atau kata kunci pencarian.",
            color = Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        if (!noData && canReset) {
            Spacer(Modifier.height(14.dp))
            ActionChip(label = "Atur ulang filter", onClick = onReset)
        }
    }
}

@Composable
private fun ActiveFilterChip(label: String, onClear: () -> Unit) {
    Row(
        Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(Ink)
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(onClickLabel = "Hapus filter", role = Role.Button, onClick = onClear),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Hapus filter", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
