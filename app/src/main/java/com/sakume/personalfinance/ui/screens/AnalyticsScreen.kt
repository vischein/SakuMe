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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.BillEntity
import com.sakume.personalfinance.data.DebtEntity
import com.sakume.personalfinance.data.TransactionEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.SegmentedChips
import com.sakume.personalfinance.ui.daysUntilDue
import com.sakume.personalfinance.ui.displayCategory
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.monthLabel
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Panel
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

private val HeroMuted = Color(0xFFAEAEB2)
private val HeroLoss = Color(0xFFFF6B63)
private val LineColor = Color(0xFFF0F0F2)
private val BarIdle = Color(0xFFD9D9DE)
private val SliceRest = Color(0xFFC7C7CC)

private data class PeriodRange(val start: Long, val end: Long, val label: String)

private fun calendarStartOfDay(cal: Calendar): Calendar = cal.apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun periodRanges(): List<PeriodRange> {
    val now = Calendar.getInstance()
    val thisMonthStart = calendarStartOfDay(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }).timeInMillis
    val nextMonthStart = calendarStartOfDay(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
    }).timeInMillis
    val lastMonthStart = calendarStartOfDay(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, -1)
    }).timeInMillis
    val threeMonthsStart = calendarStartOfDay(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, -2)
    }).timeInMillis
    val yearStart = calendarStartOfDay(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_YEAR, 1)
    }).timeInMillis
    return listOf(
        PeriodRange(thisMonthStart, nextMonthStart, "Bulan ini"),
        PeriodRange(lastMonthStart, thisMonthStart, "Bulan lalu"),
        PeriodRange(threeMonthsStart, nextMonthStart, "3 bulan"),
        PeriodRange(yearStart, nextMonthStart, "Tahun ini")
    )
}

private fun previousRange(current: PeriodRange): PeriodRange {
    val duration = (current.end - current.start).coerceAtLeast(1L)
    return PeriodRange(current.start - duration, current.start, "Sebelumnya")
}

private fun daysInRange(start: Long, end: Long): Int {
    val days = ((end - start) / 86_400_000L).toInt().coerceAtLeast(1)
    val todayEnd = calendarStartOfDay(Calendar.getInstance()).timeInMillis + 86_400_000L
    val effectiveEnd = minOf(end, todayEnd)
    return ((effectiveEnd - start) / 86_400_000L).toInt().coerceAtLeast(1)
}

fun normalizeCategory(raw: String): String {
    val original = raw.trim().ifBlank { return "Lainnya" }
    val lower = original.lowercase()
        .replace('•', ' ')
        .replace('-', ' ')
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    val rules = listOf(
        listOf("rokok", "cigarette", "vape") to "Rokok",
        listOf("bensin", "pertamax", "pertalite", "solar", "bbm", "spbu") to "BBM",
        listOf("investasi", "saham", "reksadana", "crypto", "bitcoin") to "Investasi",
        listOf("tagihan", "kredivo", "spinjam", "shopee paylater", "spaylater", "cicilan") to "Tagihan",
        listOf("admin", "biaya admin", "transfer fee") to "Admin bank",
        listOf("makan", "kopi", "tea", "teh", "nasi", "resto", "warung", "go food", "gofood", "grabfood") to "Makan & minum",
        listOf("transport", "ojek", "grab", "gojek", "parkir", "tol") to "Transport",
        listOf("belanja", "supermarket", "minimarket", "alfamart", "indomaret") to "Belanja",
        listOf("gaji", "salary", "upah") to "Gaji",
        listOf("hiburan", "game", "netflix", "spotify", "youtube") to "Hiburan",
        listOf("kesehatan", "obat", "dokter", "apotek") to "Kesehatan",
        listOf("pulsa", "kuota", "internet", "paket data") to "Pulsa & data",
        listOf("sewa", "kontrakan", "kos", "kost") to "Sewa / kos",
        listOf("listrik", "air", "pdam", "pln") to "Utilitas",
        listOf("tarik tunai", "setor tunai") to "Tunai"
    )
    for ((keys, label) in rules) {
        if (keys.any { lower.contains(it) }) return label
    }

    val first = lower.split(" ").firstOrNull { it.length >= 3 } ?: lower
    return first.replaceFirstChar { it.uppercase() }
}

private data class SpendBucket(val startMillis: Long, val amount: Long)

private data class SpendSeries(val buckets: List<SpendBucket>, val unit: String)

private data class Slice(val label: String, val amount: Long, val color: Color)

private fun buildSpendSeries(expenses: List<TransactionEntity>, period: PeriodRange): SpendSeries {
    val spanDays = (period.end - period.start) / 86_400_000L
    val (field, step, unit) = when {
        spanDays <= 32L -> Triple(Calendar.DAY_OF_MONTH, 1, "harian")
        spanDays <= 100L -> Triple(Calendar.DAY_OF_MONTH, 7, "mingguan")
        else -> Triple(Calendar.MONTH, 1, "bulanan")
    }
    val limit = minOf(period.end, calendarStartOfDay(Calendar.getInstance()).timeInMillis + 86_400_000L)
    val starts = ArrayList<Long>()
    val cal = Calendar.getInstance().apply { timeInMillis = period.start }
    while (cal.timeInMillis < limit) {
        starts.add(cal.timeInMillis)
        cal.add(field, step)
    }
    if (starts.isEmpty()) starts.add(period.start)
    val sums = LongArray(starts.size)
    expenses.forEach { t ->
        val idx = starts.indexOfLast { it <= t.dateMillis }.coerceAtLeast(0)
        sums[idx] += t.amount
    }
    return SpendSeries(starts.mapIndexed { i, start -> SpendBucket(start, sums[i]) }, unit)
}

private fun bucketLabel(ms: Long, unit: String): String {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    val months = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    val month = months[c.get(Calendar.MONTH)]
    return if (unit == "bulanan") month else "${c.get(Calendar.DAY_OF_MONTH)} $month"
}

private fun toSlices(entries: List<Map.Entry<String, Long>>, palette: List<Color>, maxShown: Int = 5): List<Slice> {
    val shown = entries.take(maxShown).mapIndexed { i, e -> Slice(e.key, e.value, palette[i % palette.size]) }
    val rest = entries.drop(maxShown)
    return if (rest.isEmpty()) shown else shown + Slice("Lainnya (${rest.size})", rest.sumOf { it.value }, SliceRest)
}

@Composable
fun AnalyticsScreen(
    accounts: List<AccountEntity>,
    tx: List<TransactionEntity>,
    bills: List<BillEntity> = emptyList(),
    debts: List<DebtEntity> = emptyList(),
    onManageAccounts: () -> Unit = {}
) {
    val periods = remember { periodRanges() }
    var periodIndex by remember { mutableStateOf(0) }
    val period = periods[periodIndex]
    val prev = remember(period) { previousRange(period) }

    val inPeriod = remember(tx, period) {
        tx.filter { it.dateMillis >= period.start && it.dateMillis < period.end }
    }
    val inPrev = remember(tx, prev) {
        tx.filter { it.dateMillis >= prev.start && it.dateMillis < prev.end }
    }

    fun isExpense(t: TransactionEntity) = t.type.equals("expense", true)
    fun isIncome(t: TransactionEntity) = t.type.equals("income", true)

    val expenses = remember(inPeriod) { inPeriod.filter(::isExpense) }
    val incomes = remember(inPeriod) { inPeriod.filter(::isIncome) }
    val prevExpenses = remember(inPrev) { inPrev.filter(::isExpense) }
    val prevIncomes = remember(inPrev) { inPrev.filter(::isIncome) }

    val totalOut = expenses.sumOf { it.amount }
    val totalIn = incomes.sumOf { it.amount }
    val net = totalIn - totalOut
    val prevOut = prevExpenses.sumOf { it.amount }
    val prevIn = prevIncomes.sumOf { it.amount }
    val days = daysInRange(period.start, period.end)
    val dailyAvg = if (days > 0) totalOut / days else 0L
    val savingsRate = if (totalIn > 0) ((net.toDouble() / totalIn.toDouble()) * 100.0) else null

    val outByCategory = remember(expenses) {
        expenses.groupBy { normalizeCategory(it.category.ifBlank { it.note }) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
    }
    val inByCategory = remember(incomes) {
        incomes.groupBy { normalizeCategory(it.category.ifBlank { it.note }) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
    }
    val spentByAccount = remember(expenses) {
        expenses.groupBy { it.accountId }.mapValues { (_, list) -> list.sumOf { it.amount } }
    }
    val topExpenses = remember(expenses) {
        expenses.sortedByDescending { it.amount }.take(5)
    }
    val series = remember(expenses, period) { buildSpendSeries(expenses, period) }

    val billsPaid = remember(bills, period) {
        bills.filter { it.isPaid && (it.paidAtMillis ?: 0L) in period.start until period.end }
    }
    val billsPaidTotal = billsPaid.sumOf { it.amount }
    val debtPaid = remember(debts, period) {
        debts.filter {
            val paidAt = it.paidAtMillis
            paidAt != null && paidAt >= period.start && paidAt < period.end && it.paidAmount > 0
        }
    }
    val debtPaidTotal = debtPaid.sumOf { it.paidAmount.coerceAtMost(it.amount) }
    val activeDebts = remember(debts) { debts.filter { !it.isPaid && it.remaining > 0L } }
    val piutangLeft = activeDebts.filter { it.type == "piutang" }.sumOf { it.remaining }
    val utangLeft = activeDebts.filter { it.type == "utang" }.sumOf { it.remaining }
    val overdueCount = activeDebts.count { d -> d.dueDateMillis?.let { daysUntilDue(it) < 0 } == true }

    val outChangePct = if (prevOut > 0) {
        ((totalOut - prevOut).toDouble() / prevOut.toDouble() * 100.0)
    } else if (totalOut > 0) 100.0 else 0.0
    val inChangePct = if (prevIn > 0) {
        ((totalIn - prevIn).toDouble() / prevIn.toDouble() * 100.0)
    } else if (totalIn > 0) 100.0 else 0.0

    val insight = remember(totalOut, totalIn, outByCategory, dailyAvg, outChangePct, savingsRate) {
        buildInsight(
            totalIn = totalIn,
            totalOut = totalOut,
            topCategory = outByCategory.firstOrNull()?.key,
            topCategoryAmount = outByCategory.firstOrNull()?.value ?: 0L,
            dailyAvg = dailyAvg,
            outChangePct = outChangePct,
            savingsRate = savingsRate
        )
    }

    val categoryColors = listOf(
        Color(0xFF4B48D6), Color(0xFF34C759), Color(0xFFFF9F0A),
        Color(0xFFFF3B30), Color(0xFF5AC8FA), Color(0xFFAF52DE),
        Color(0xFFFF2D55), Color(0xFF8E8E93)
    )
    val incomeColors = categoryColors.drop(2) + categoryColors.take(2)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Analisis", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        monthLabel(period.start).replaceFirstChar { it.uppercase() },
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
                Text(
                    "Kelola rekening",
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBg)
                        .clickable(onClick = onManageAccounts)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        item {
            SegmentedChips(
                options = periods.map { it.label },
                selectedIndex = periodIndex,
                onSelect = { periodIndex = it }
            )
        }

        item {
            NetHero(
                periodLabel = period.label.lowercase(),
                net = net,
                totalIn = totalIn,
                totalOut = totalOut,
                inChangePct = inChangePct,
                outChangePct = outChangePct,
                savingsRate = savingsRate,
                insight = insight
            )
        }

        item {
            if (totalOut == 0L) {
                EmptyHint("Belum ada pengeluaran di periode ini.")
            } else {
                SpendTrendCard(
                    series = series,
                    dailyAvg = dailyAvg,
                    transactionCount = expenses.size
                )
            }
        }

        if (outByCategory.isNotEmpty()) {
            item {
                CompositionCard(
                    title = "Pengeluaran per kategori",
                    total = totalOut,
                    slices = toSlices(outByCategory, categoryColors)
                )
            }
        }

        if (topExpenses.isNotEmpty()) {
            item { TopExpensesCard(topExpenses) }
        }

        if (inByCategory.isNotEmpty()) {
            item {
                CompositionCard(
                    title = "Pemasukan per kategori",
                    total = totalIn,
                    slices = toSlices(inByCategory, incomeColors)
                )
            }
        }

        item { AccountsCard(accounts, spentByAccount) }

        if (activeDebts.isNotEmpty() || billsPaidTotal > 0L || debtPaidTotal > 0L) {
            item {
                ObligationsCard(
                    piutangLeft = piutangLeft,
                    utangLeft = utangLeft,
                    overdueCount = overdueCount,
                    billsPaidCount = billsPaid.size,
                    billsPaidTotal = billsPaidTotal,
                    debtPaidTotal = debtPaidTotal
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

private fun buildInsight(
    totalIn: Long,
    totalOut: Long,
    topCategory: String?,
    topCategoryAmount: Long,
    dailyAvg: Long,
    outChangePct: Double,
    savingsRate: Double?
): String {
    val parts = mutableListOf<String>()
    if (totalOut == 0L && totalIn == 0L) {
        return "Belum ada transaksi di periode ini. Catat pemasukan & pengeluaran agar insight muncul."
    }
    if (topCategory != null && totalOut > 0) {
        val pct = (topCategoryAmount.toDouble() / totalOut * 100).roundToInt()
        parts.add("Kategori terbesar: $topCategory (${pct}% pengeluaran).")
    }
    when {
        outChangePct > 15 -> parts.add("Pengeluaran naik ${outChangePct.roundToInt()}% dibanding periode sebelumnya.")
        outChangePct < -15 -> parts.add("Pengeluaran turun ${abs(outChangePct).roundToInt()}% — bagus!")
        totalOut > 0 -> parts.add("Pengeluaran relatif stabil vs periode sebelumnya.")
    }
    if (dailyAvg > 0) parts.add("Rata-rata belanja ${rupiah(dailyAvg)}/hari.")
    if (savingsRate != null) {
        val rate = savingsRate.roundToInt()
        when {
            savingsRate < 0 -> parts.add("Periode ini defisit — cek kategori yang bisa dipangkas.")
            savingsRate >= 20 -> parts.add("Kamu menabung $rate% dari pemasukan, di atas target 20%.")
            else -> parts.add("Kamu menabung $rate% dari pemasukan; targetnya 20% atau lebih.")
        }
    }
    return parts.joinToString(" ")
}

@Composable
private fun NetHero(
    periodLabel: String,
    net: Long,
    totalIn: Long,
    totalOut: Long,
    inChangePct: Double,
    outChangePct: Double,
    savingsRate: Double?,
    insight: String
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Ink)
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Netto $periodLabel", color = HeroMuted, fontSize = 13.sp)
            if (savingsRate != null) {
                Text(
                    "Tabungan ${"%.0f".format(savingsRate)}%",
                    color = if (savingsRate >= 20) Green else if (savingsRate >= 0) Color.White else HeroLoss,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Panel)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            rupiah(net),
            color = if (net >= 0) Color.White else HeroLoss,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Panel)
        ) {
            if (totalIn > 0L) {
                Box(Modifier.weight(totalIn.toFloat()).fillMaxHeight().background(Green))
            }
            if (totalIn > 0L && totalOut > 0L) Spacer(Modifier.width(2.dp))
            if (totalOut > 0L) {
                Box(Modifier.weight(totalOut.toFloat()).fillMaxHeight().background(Red))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            HeroFlow(
                label = "Masuk",
                value = totalIn,
                changePct = inChangePct,
                positiveIsGood = true,
                dot = Green,
                modifier = Modifier.weight(1f)
            )
            HeroFlow(
                label = "Keluar",
                value = totalOut,
                changePct = outChangePct,
                positiveIsGood = false,
                dot = Red,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Panel))
        Spacer(Modifier.height(12.dp))
        Text(insight, color = Color(0xFFE5E5EA), fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun HeroFlow(
    label: String,
    value: Long,
    changePct: Double,
    positiveIsGood: Boolean,
    dot: Color,
    modifier: Modifier = Modifier
) {
    val isUp = changePct >= 0
    val good = if (positiveIsGood) isUp else !isUp
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(dot, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, color = HeroMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            rupiah(value),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (value > 0L || changePct != 0.0) {
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isUp) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    null,
                    tint = if (good) Green else Red,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${if (isUp) "+" else ""}${changePct.roundToInt()}% vs lalu",
                    color = if (good) Green else Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SpendTrendCard(series: SpendSeries, dailyAvg: Long, transactionCount: Int) {
    val max = series.buckets.maxOfOrNull { it.amount } ?: 0L
    val peakIndex = if (max > 0L) series.buckets.indexOfFirst { it.amount == max } else -1
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pengeluaran ${series.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            if (peakIndex >= 0) {
                Text(
                    "Tertinggi ${bucketLabel(series.buckets[peakIndex].startMillis, series.unit)} · ${rupiah(max)}",
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(if (series.buckets.size > 20) 2.dp else 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            series.buckets.forEachIndexed { i, b ->
                val fraction = if (max > 0L) (b.amount.toFloat() / max.toFloat()).coerceIn(0.04f, 1f) else 0.04f
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (i == peakIndex) Ink else BarIdle)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(bucketLabel(series.buckets.first().startMillis, series.unit), color = Muted, fontSize = 11.sp)
            if (series.buckets.size > 1) {
                Text(bucketLabel(series.buckets.last().startMillis, series.unit), color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(LineColor))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCell("Rata-rata / hari", rupiah(dailyAvg), Ink, Modifier.weight(1f))
            StatCell("Transaksi keluar", transactionCount.toString(), Ink, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompositionCard(title: String, total: Long, slices: List<Slice>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            Text(rupiah(total), color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            slices.forEachIndexed { i, s ->
                if (i > 0) Spacer(Modifier.width(2.dp))
                Box(
                    Modifier
                        .weight(s.amount.toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(s.color)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            slices.forEach { s ->
                val pct = if (total > 0L) s.amount.toDouble() / total.toDouble() * 100.0 else 0.0
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(s.color, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        s.label,
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(rupiah(s.amount), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        "${pct.roundToInt()}%",
                        color = Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopExpensesCard(items: List<TransactionEntity>) {
    val maxAmount = (items.firstOrNull()?.amount ?: 1L).coerceAtLeast(1L)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Text("Pengeluaran terbesar", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { t ->
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                displayCategory(t.category.ifBlank { t.note.ifBlank { "Pengeluaran" } }),
                                fontWeight = FontWeight.Medium,
                                color = Ink,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(formatDate(t.dateMillis), color = Muted, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(rupiah(t.amount), color = Red, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (t.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Red,
                        trackColor = RedSoft
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountsCard(accounts: List<AccountEntity>, spentByAccount: Map<String, Long>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Text("Rekening", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
        Spacer(Modifier.height(8.dp))
        if (accounts.isEmpty()) {
            Text("Belum ada rekening.", color = Muted, fontSize = 13.sp)
            return@Column
        }
        accounts.sortedByDescending { it.balance }.forEachIndexed { i, a ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(LineColor))
            val spent = spentByAccount[a.id] ?: 0L
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(Color(0xFFF0F0F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, null, tint = Ink, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        a.name,
                        fontWeight = FontWeight.Medium,
                        color = Ink,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (spent > 0L) "Keluar ${rupiah(spent)} periode ini" else "Tidak ada pengeluaran",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(rupiah(a.balance), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(LineColor))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total saldo", fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
            Text(rupiah(accounts.sumOf { it.balance }), fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ObligationsCard(
    piutangLeft: Long,
    utangLeft: Long,
    overdueCount: Int,
    billsPaidCount: Int,
    billsPaidTotal: Long,
    debtPaidTotal: Long
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Text("Utang, piutang & tagihan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCell("Piutang belum diterima", rupiah(piutangLeft), Green, Modifier.weight(1f))
            StatCell("Utang belum dibayar", rupiah(utangLeft), Red, Modifier.weight(1f))
        }
        if (overdueCount > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                "$overdueCount utang/piutang lewat jatuh tempo",
                color = Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (billsPaidTotal > 0L || debtPaidTotal > 0L) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(LineColor))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                if (billsPaidTotal > 0L) {
                    StatCell("Tagihan dibayar ($billsPaidCount)", rupiah(billsPaidTotal), Ink, Modifier.weight(1f))
                }
                if (debtPaidTotal > 0L) {
                    StatCell("Utang/piutang dilunasi", rupiah(debtPaidTotal), Ink, Modifier.weight(1f))
                }
            }
        }
    }
}
