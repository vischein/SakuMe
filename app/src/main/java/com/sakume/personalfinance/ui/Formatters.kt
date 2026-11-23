package com.sakume.personalfinance.ui

import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.BillEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun rupiah(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }.format(value)

fun displayCategory(raw: String): String =
    raw
        .replace(" • ", " ")
        .replace('•', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { "Lainnya" }

fun isCashAccount(a: AccountEntity): Boolean = a.type.equals("Cash", true) || a.name.contains("cash", true)

fun monthKey(ms: Long): Int =
    Calendar.getInstance().apply { timeInMillis = ms }.let { it.get(Calendar.YEAR) * 100 + it.get(Calendar.MONTH) }

fun monthLabel(ms: Long): String = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date(ms))

fun formatDate(ms: Long): String = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(ms))

const val DUE_SOON_DAYS = 3

fun daysUntilDue(dueDateMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val due = java.time.Instant.ofEpochMilli(dueDateMillis).atZone(zone).toLocalDate()
    return java.time.temporal.ChronoUnit.DAYS.between(today, due).toInt()
}

enum class BillUrgency { Paid, Overdue, Critical, Soon, Normal }

fun urgency(b: BillEntity, nowMillis: Long = System.currentTimeMillis()): BillUrgency {
    if (b.isPaid) return BillUrgency.Paid
    val days = daysUntilDue(b.dueDateMillis, nowMillis)
    return when {
        days < 0 -> BillUrgency.Overdue
        days <= 2 -> BillUrgency.Critical
        days <= 7 -> BillUrgency.Soon
        else -> BillUrgency.Normal
    }
}

fun dueSoonBills(bills: List<BillEntity>, nowMillis: Long = System.currentTimeMillis()): List<BillEntity> =
    bills
        .asSequence()
        .filterNot { it.isPaid }
        .map { it to daysUntilDue(it.dueDateMillis, nowMillis) }
        .filter { (_, days) -> days <= DUE_SOON_DAYS }
        .sortedWith(compareBy<Pair<BillEntity, Int>>({ it.second }, { it.first.dueDateMillis }))
        .map { it.first }
        .toList()

fun dueAttentionLabel(dueDateMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val days = daysUntilDue(dueDateMillis, nowMillis)
    return when {
        days < 0 -> "Telat ${-days} hari"
        days == 0 -> "Jatuh tempo hari ini"
        else -> "H-$days"
    }
}

fun dueAttentionSummary(dueSoon: List<BillEntity>, nowMillis: Long = System.currentTimeMillis()): String {
    val overdue = dueSoon.count { daysUntilDue(it.dueDateMillis, nowMillis) < 0 }
    val upcoming = dueSoon.size - overdue
    return listOfNotNull(
        if (overdue > 0) "$overdue telat" else null,
        if (upcoming > 0) "$upcoming jatuh tempo ≤ $DUE_SOON_DAYS hari" else null
    ).joinToString(" · ")
}

fun dueInLabel(dueDateMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val days = daysUntilDue(dueDateMillis, nowMillis)
    return when {
        days < 0 -> "Lewat ${-days} hari"
        days == 0 -> "Hari ini"
        days == 1 -> "Besok"
        else -> "$days hari lagi"
    }
}

fun timeGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> "Selamat pagi"
        in 11..14 -> "Selamat siang"
        in 15..18 -> "Selamat sore"
        else -> "Selamat malam"
    }
}

fun startOfMonthMillis(): Long =
    Calendar
        .getInstance()
        .apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

private val ID_LOCALE = Locale("id", "ID")

fun startOfDayMillis(ms: Long = System.currentTimeMillis()): Long =
    Calendar
        .getInstance()
        .apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

fun startOfWeekMillis(ms: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = startOfDayMillis(ms) }
    
    val sinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    cal.add(Calendar.DAY_OF_MONTH, -sinceMonday)
    return cal.timeInMillis
}

fun formatTime(ms: Long): String = SimpleDateFormat("HH:mm", ID_LOCALE).format(Date(ms))

fun isStartOfDay(ms: Long): Boolean = ms == startOfDayMillis(ms)

fun dayHeaderLabel(dayStartMillis: Long, todayStartMillis: Long = startOfDayMillis()): String {
    val zone = java.time.ZoneId.systemDefault()
    val day = java.time.Instant.ofEpochMilli(dayStartMillis).atZone(zone).toLocalDate()
    val today = java.time.Instant.ofEpochMilli(todayStartMillis).atZone(zone).toLocalDate()
    return when (java.time.temporal.ChronoUnit.DAYS.between(day, today)) {
        0L -> "Hari ini"
        1L -> "Kemarin"
        else -> SimpleDateFormat("EEEE, d MMM yyyy", ID_LOCALE).format(Date(dayStartMillis))
    }
}