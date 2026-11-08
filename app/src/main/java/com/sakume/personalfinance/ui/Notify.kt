package com.sakume.personalfinance.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notify {
    private const val CHANNEL_ID = "sakume_schedules"
    private const val ID_INVEST = 1001
    private const val ID_SALARY = 1002
    private const val ID_SCHEDULE_FAILURE = 1004

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jadwal SakuMe",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi setor investasi & gaji otomatis"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            nm.cancel(1003)
            nm.createNotificationChannel(channel)
        }
    }

    fun investmentSuccess(context: Context, body: String) {
        show(context, ID_INVEST, "Setor investasi terjadwal", body)
    }

    fun salarySuccess(context: Context, body: String) {
        show(context, ID_SALARY, "Gaji otomatis masuk", body)
    }

    fun scheduleFailure(context: Context, body: String) {
        show(context, ID_SCHEDULE_FAILURE, "Jadwal otomatis gagal", body)
    }

    private fun show(context: Context, id: Int, title: String, body: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {

        }
    }
}
