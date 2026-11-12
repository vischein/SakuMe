package com.sakume.personalfinance.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sakume.personalfinance.MainActivity
import com.sakume.personalfinance.R
import com.sakume.personalfinance.data.FinanceRepository

object Notify {
    private const val CHANNEL_SCHEDULES = "sakume_schedules"
    private const val CHANNEL_BILLS = "sakume_bills"
    private const val ID_INVEST = 1001
    private const val ID_SALARY = 1002
    private const val ID_SCHEDULE_FAILURE = 1004
    private const val ID_BILL = 1005

    const val EXTRA_OPEN_TAB = "open_tab"

    /** Satu tagihan yang akan/sudah jatuh tempo. [daysLeft] 0 = hari H. */
    data class BillDue(
        val provider: String,
        val itemName: String,
        val installmentIndex: Int,
        val tenorMonths: Int,
        val amount: Long,
        val daysLeft: Int
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(1003)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_SCHEDULES, "Jadwal SakuMe", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Gaji masuk & setor investasi otomatis"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_BILLS, "Pengingat tagihan", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Tagihan yang jatuh tempo hari ini atau segera"
                }
            )
        }
    }

    // ------------------------------------------------------------------ hasil jadwal

    /** Dipakai bersama oleh worker background dan saat app dibuka; menghormati saklar notifikasi. */
    fun scheduleResult(context: Context, result: FinanceRepository.ScheduleRunResult) {
        if (!AppPrefs.notificationsEnabled(context)) return
        if (result.salaryRuns.isNotEmpty()) salary(context, result.salaryRuns)
        if (result.investmentRuns.isNotEmpty()) investment(context, result.investmentRuns)
        if (result.failures.isNotEmpty()) scheduleFailure(context, result.failures)
    }

    /**
     * Satu gaji:
     *   Gaji masuk Rp5.000.000
     *   Gaji bulanan ke BCA · saldo sekarang Rp8.250.000
     * Beberapa gaji: judul ringkas + satu baris per gaji.
     */
    private fun salary(context: Context, runs: List<FinanceRepository.SalaryRun>) {
        if (runs.size == 1) {
            val r = runs.first()
            val label = frequencyLabel(r.frequency)
            val body = "${r.note}${if (label.isEmpty()) "" else " $label"} ke ${r.accountName}" +
                " · saldo sekarang ${rupiah(r.balanceAfter)}"
            post(
                context, CHANNEL_SCHEDULES, ID_SALARY,
                title = "Gaji masuk ${rupiah(r.amount)}",
                text = body, lines = emptyList(), tab = "Home"
            )
        } else {
            val total = runs.sumOf { it.amount }
            post(
                context, CHANNEL_SCHEDULES, ID_SALARY,
                title = "${runs.size} pemasukan masuk · ${rupiah(total)}",
                text = runs.joinToString(", ") { it.note },
                lines = runs.map { "${it.note} → ${it.accountName}  +${rupiah(it.amount)}" },
                tab = "Home"
            )
        }
    }

    private fun investment(context: Context, runs: List<FinanceRepository.InvestmentRun>) {
        if (runs.size == 1) {
            val r = runs.first()
            post(
                context, CHANNEL_SCHEDULES, ID_INVEST,
                title = "Setor investasi ${rupiah(r.amount)}",
                text = "Ke ${r.assetName} dari ${r.accountName}",
                lines = emptyList(), tab = "Invest"
            )
        } else {
            post(
                context, CHANNEL_SCHEDULES, ID_INVEST,
                title = "${runs.size} setoran investasi · ${rupiah(runs.sumOf { it.amount })}",
                text = runs.joinToString(", ") { it.assetName },
                lines = runs.map { "${it.assetName} ← ${it.accountName}  ${rupiah(it.amount)}" },
                tab = "Invest"
            )
        }
    }

    private fun scheduleFailure(context: Context, failures: List<String>) {
        post(
            context, CHANNEL_SCHEDULES, ID_SCHEDULE_FAILURE,
            title = "Jadwal otomatis belum berhasil",
            text = failures.first(),
            lines = if (failures.size > 1) failures else emptyList(),
            tab = "Home"
        )
    }

    // ------------------------------------------------------------------ tagihan

    /**
     * Hari H, satu tagihan:
     *   Hari ini jatuh tempo: Kredivo
     *   Laptop · cicilan 2/6 · Rp450.000
     * Beberapa tagihan: ringkasan jumlah + total, lalu satu baris per tagihan.
     */
    fun billsDue(context: Context, items: List<BillDue>) {
        if (items.isEmpty()) return
        val sorted = items.sortedWith(compareBy({ it.daysLeft }, { it.provider.lowercase() }))
        val today = sorted.filter { it.daysLeft == 0 }
        val total = sorted.sumOf { it.amount }

        val title: String
        val text: String
        if (sorted.size == 1) {
            val b = sorted.first()
            title = "${whenLabel(b.daysLeft, capitalized = true)} jatuh tempo: ${b.provider}"
            text = billDetail(b)
        } else {
            title = when {
                today.isNotEmpty() && today.size == sorted.size -> "Hari ini ${sorted.size} tagihan jatuh tempo"
                today.isNotEmpty() -> "Hari ini ${today.size} tagihan jatuh tempo, ${sorted.size - today.size} lagi menyusul"
                else -> "${sorted.size} tagihan segera jatuh tempo"
            }
            text = "Total ${rupiah(total)} · ${sorted.joinToString(", ") { it.provider }}"
        }
        val lines = if (sorted.size == 1) emptyList() else sorted.map {
            val prefix = if (sorted.any { s -> s.daysLeft != 0 }) "${whenLabel(it.daysLeft, capitalized = true)} · " else ""
            "$prefix${it.provider} — ${billDetail(it)}"
        }
        post(context, CHANNEL_BILLS, ID_BILL, title, text, lines, tab = "Bills", important = true)
    }

    private fun billDetail(b: BillDue): String {
        val parts = mutableListOf<String>()
        if (b.itemName.isNotBlank()) parts += b.itemName
        if (b.tenorMonths > 1) parts += "cicilan ${b.installmentIndex}/${b.tenorMonths}"
        parts += rupiah(b.amount)
        return parts.joinToString(" · ")
    }

    private fun whenLabel(daysLeft: Int, capitalized: Boolean): String = when (daysLeft) {
        0 -> if (capitalized) "Hari ini" else "hari ini"
        1 -> if (capitalized) "Besok" else "besok"
        else -> if (capitalized) "$daysLeft hari lagi" else "$daysLeft hari lagi"
    }

    private fun frequencyLabel(freq: String): String = when (freq) {
        "daily" -> "harian"
        "biweekly" -> "2 mingguan"
        "monthly" -> "bulanan"
        else -> ""
    }

    // ------------------------------------------------------------------ dasar

    private fun post(
        context: Context,
        channel: String,
        id: Int,
        title: String,
        text: String,
        lines: List<String>,
        tab: String,
        important: Boolean = false
    ) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TAB, tab)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val style: NotificationCompat.Style =
            if (lines.isEmpty()) NotificationCompat.BigTextStyle().bigText(text)
            else NotificationCompat.InboxStyle().also { s -> lines.take(6).forEach { s.addLine(it) } }
                .setSummaryText(text)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_sakume)
            .setColor(0xFF1C1C1E.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style)
            .setContentIntent(open)
            .setCategory(if (important) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_STATUS)
            .setPriority(if (important) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Izin notifikasi belum diberikan.
        }
    }
}
