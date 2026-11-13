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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Insights
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.BillEntity
import com.sakume.personalfinance.data.DebtEntity
import com.sakume.personalfinance.data.TransactionEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.SegmentedChips
import com.sakume.personalfinance.ui.displayCategory
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.monthLabel
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.GreenSoft
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft
import com.sakume.personalfinance.ui.theme.Gold
import com.sakume.personalfinance.ui.theme.GoldSoft
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

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
    val outByAccount = remember(expenses, accounts) {
        expenses.groupBy { it.accountId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .map { e ->
                val name = accounts.firstOrNull { it.id == e.key }?.name ?: "Rekening"
                name to e.value
            }
    }
    val topExpenses = remember(expenses) {
        expenses.sortedByDescending { it.amount }.take(5)
    }

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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(
                    title = "Pemasukan",
                    value = rupiah(totalIn),
                    changePct = inChangePct,
                    positiveIsGood = true,
                    accent = Green,
                    soft = GreenSoft,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Pengeluaran",
                    value = rupiah(totalOut),
                    changePct = outChangePct,
                    positiveIsGood = false,
                    accent = Red,
                    soft = RedSoft,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(
                    label = "Netto",
                    value = rupiah(net),
                    valueColor = if (net >= 0) Green else Red,
                    modifier = Modifier.weight(1f)
                )
                MiniStat(
                    label = "Rata-rata / hari",
                    value = rupiah(dailyAvg),
                    valueColor = Ink,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (savingsRate != null) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tingkat tabungan", color = Muted, fontSize = 12.sp)
                        Text(
                            "${"%.0f".format(savingsRate)}%",
                            color = if (savingsRate >= 20) Green else if (savingsRate >= 0) Ink else Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (savingsRate / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (savingsRate >= 20) Green else if (savingsRate >= 0) Ink else Red,
                        trackColor = Color(0xFFEEEEEE)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            savingsRate >= 20 -> "Baik — ideal menabung ≥20% pemasukan"
                            savingsRate >= 0 -> "Cukup — coba naikkan porsi tabungan"
                            else -> "Pengeluaran melebihi pemasukan periode ini"
                        },
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GoldSoft)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Insights, null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Insight", fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(insight, color = Ink, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }

        item {
            Text("Pengeluaran per kategori", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
        }
        if (outByCategory.isEmpty()) {
            item { EmptyHint("Belum ada pengeluaran di periode ini.") }
        } else {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    outByCategory.forEachIndexed { index, (cat, amount) ->
                        val pct = if (totalOut > 0) amount.toDouble() / totalOut.toDouble() else 0.0
                        CategoryBar(
                            label = cat,
                            amount = amount,
                            fraction = pct.toFloat(),
                            color = categoryColors[index % categoryColors.size]
                        )
                    }
                }
            }
        }

        if (inByCategory.isNotEmpty()) {
            item {
                Text("Pemasukan per kategori", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    inByCategory.forEachIndexed { index, (cat, amount) ->
                        val pct = if (totalIn > 0) amount.toDouble() / totalIn.toDouble() else 0.0
                        CategoryBar(
                            label = cat,
                            amount = amount,
                            fraction = pct.toFloat(),
                            color = categoryColors[(index + 2) % categoryColors.size]
                        )
                    }
                }
            }
        }

        if (outByAccount.isNotEmpty()) {
            item {
                Text("Pengeluaran per rekening", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    outByAccount.forEach { (name, amount) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFF0F0F0), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.AccountBalanceWallet,
                                        null,
                                        tint = Ink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    name,
                                    fontWeight = FontWeight.Medium,
                                    color = Ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(rupiah(amount), color = Red, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (topExpenses.isNotEmpty()) {
            item {
                Text("Pengeluaran terbesar", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    topExpenses.forEach { t ->
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
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(formatDate(t.dateMillis), color = Muted, fontSize = 11.sp)
                            }
                            Text(rupiah(t.amount), color = Red, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (billsPaidTotal > 0L || debtPaidTotal > 0L) {
            item {
                Text("Pembayaran lain", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (billsPaidTotal > 0L) {
                        MiniStat(
                            label = "Tagihan dibayar (${billsPaid.size})",
                            value = rupiah(billsPaidTotal),
                            valueColor = Ink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (debtPaidTotal > 0L) {
                        MiniStat(
                            label = "Utang/piutang dilunasi",
                            value = rupiah(debtPaidTotal),
                            valueColor = Ink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .padding(14.dp)
            ) {
                Text("Saldo rekening saat ini", fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                if (accounts.isEmpty()) {
                    Text("Belum ada rekening.", color = Muted, fontSize = 13.sp)
                } else {
                    accounts.sortedByDescending { it.balance }.forEach { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(a.name, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(rupiah(a.balance), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", fontWeight = FontWeight.Bold, color = Ink)
                        Text(
                            rupiah(accounts.sumOf { it.balance }),
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                    }
                }
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
    if (savingsRate != null && savingsRate < 0) {
        parts.add("Periode ini defisit — cek kategori yang bisa dipangkas.")
    }
    return parts.joinToString(" ")
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    changePct: Double,
    positiveIsGood: Boolean,
    accent: Color,
    soft: Color,
    modifier: Modifier = Modifier
) {
    val isUp = changePct >= 0
    val good = if (positiveIsGood) isUp else !isUp
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(soft)
            .padding(14.dp)
    ) {
        Text(title, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
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

@Composable
private fun MiniStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp)
    ) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CategoryBar(
    label: String,
    amount: Long,
    fraction: Float,
    color: Color
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = Ink,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${rupiah(amount)} · ${(fraction * 100).roundToInt()}%",
                color = Muted,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0xFFEEEEEE)
        )
    }
}
