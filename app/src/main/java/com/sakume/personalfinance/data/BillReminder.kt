package com.sakume.personalfinance.data

import android.content.Context
import com.sakume.personalfinance.ui.AppPrefs
import com.sakume.personalfinance.ui.Notify
import com.sakume.personalfinance.ui.daysUntilDue
import java.util.Calendar

object BillReminder {

    suspend fun check(context: Context, now: Long = System.currentTimeMillis()) {
        if (!AppPrefs.notificationsEnabled(context) || !AppPrefs.billReminderEnabled(context)) return

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val today = cal.get(Calendar.YEAR) * 10_000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
        if (AppPrefs.billReminderLastDay(context) == today) return
        if (cal.get(Calendar.HOUR_OF_DAY) < AppPrefs.billReminderHour(context)) return

        val early = AppPrefs.billReminderEarlyDays(context)
        val bills = AppDatabase.get(context).dao().allBills()
        val due = bills.asSequence()
            .filter { !it.isPaid }
            .mapNotNull { b ->
                val left = daysUntilDue(b.dueDateMillis)
                if (left == 0 || (early > 0 && left == early)) {
                    Notify.BillDue(b.provider, b.itemName, b.installmentIndex, b.tenorMonths, b.amount, left)
                } else null
            }
            .toList()

        AppPrefs.setBillReminderLastDay(context, today)
        if (due.isNotEmpty()) Notify.billsDue(context, due)
    }
}
