package com.sakume.personalfinance.ui

import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.BillEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun rupiah(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }.format(value)

fun isCashAccount(a: AccountEntity): Boolean = a.type.equals("Cash", true) || a.name.contains("cash", true)

fun monthKey(ms: Long): Int =
    Calendar.getInstance().apply { timeInMillis = ms }.let { it.get(Calendar.YEAR) * 100 + it.get(Calendar.MONTH) }

fun monthLabel(ms: Long): String = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date(ms))

fun formatDate(ms: Long): String = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(ms))

fun daysUntilDue(dueDateMillis: Long): Int {
    val today =
        Calendar
            .getInstance()
            .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
    val due =
        Calendar
            .getInstance()
            .apply {
                timeInMillis = dueDateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
    return ((due - today) / 86_400_000L).toInt()
}

fun urgency(b: BillEntity): String {
    if (b.isPaid) return "Paid"
    val days = daysUntilDue(b.dueDateMillis)
    return when {
        days < 0 -> "Overdue"
        days <= 2 -> "Critical"
        days <= 7 -> "Soon"
        else -> "Normal"
    }
}

fun dueInLabel(dueDateMillis: Long): String {
    val days = daysUntilDue(dueDateMillis)
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
