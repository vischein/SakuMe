package com.sakume.personalfinance.ui

import android.content.Context
import android.content.Intent
import com.sakume.personalfinance.data.DebtEntity

fun debtReminderMessage(d: DebtEntity): String {
    val what = d.description.trim().takeIf { it.isNotEmpty() }
    val due = d.dueDateMillis
    return buildString {
        append("Halo ${d.person}, mau mengingatkan ")
        if (what != null) append("soal $what ")
        append("sebesar ${rupiah(d.remaining)}")
        if (d.paidAmount > 0L) append(" (sisa dari ${rupiah(d.amount)})")
        if (due != null) append(", jatuh tempo ${formatDate(due)}")
        append(". Kalau sudah ditransfer, kabari ya. Terima kasih!")
    }
}

fun shareDebtReminder(context: Context, d: DebtEntity) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, debtReminderMessage(d))
    }
    context.startActivity(
        Intent.createChooser(send, "Kirim pengingat").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
