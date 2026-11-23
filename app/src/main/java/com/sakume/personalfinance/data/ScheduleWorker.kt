package com.sakume.personalfinance.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sakume.personalfinance.ui.Notify
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class ScheduleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        try {
            BillReminder.check(applicationContext)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Pengingat tagihan gagal: ${e.javaClass.simpleName}")
        }
        return try {
            val db = AppDatabase.get(applicationContext)
            val repo = FinanceRepository(db)
            val days = com.sakume.personalfinance.ui.AppPrefs.maxCatchupDays(applicationContext)
            val result = repo.processDueSchedules(maxCatchupDays = days)

            Notify.scheduleResult(applicationContext, result)
            ListenableWorker.Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val kind = e.javaClass.simpleName
            Log.w(TAG, "Pemrosesan jadwal gagal: $kind")
            val permanent = e is IllegalArgumentException || e is IllegalStateException
            if (!permanent && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                ListenableWorker.Result.retry()
            } else {
                Notify.scheduleProcessingError(applicationContext, kind)
                ListenableWorker.Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "ScheduleWorker"
        private const val UNIQUE_WORK = "sakume_schedule_worker"
        private const val MAX_RETRY_ATTEMPTS = 2

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }
    }
}
