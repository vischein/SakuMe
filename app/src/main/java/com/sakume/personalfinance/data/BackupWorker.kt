package com.sakume.personalfinance.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Menjalankan backup otomatis di background:
 *  - berkala tiap 12 jam (jaring pengaman kalau app tidak pernah ditutup/dibuka),
 *  - sekali setiap app ditinggalkan (lihat MainActivity.onStop) — di sinilah data terbaru biasanya aman.
 * Kalau tidak ada perubahan data, backup dilewati (lihat BackupManager.runBackup).
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        val outcome = BackupManager.runBackup(applicationContext, force = false, source = "auto")
        return when (outcome.status) {
            BackupManager.Status.FAILED -> if (runAttemptCount < 3) ListenableWorker.Result.retry() else ListenableWorker.Result.success()
            else -> ListenableWorker.Result.success()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC = "sakume_backup_periodic"
        private const val UNIQUE_NOW = "sakume_backup_now"

        private val constraints = Constraints.Builder().setRequiresStorageNotLow(true).build()

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        /** Satu kali, segera. KEEP: kalau sudah ada yang antre/berjalan, tidak ditumpuk. */
        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NOW, ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NOW)
        }
    }
}
