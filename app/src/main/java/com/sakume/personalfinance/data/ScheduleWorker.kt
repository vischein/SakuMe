package com.sakume.personalfinance.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sakume.personalfinance.ui.Notify
import java.util.concurrent.TimeUnit

class ScheduleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        runCatching { BillReminder.check(applicationContext) }
        return try {
            val db = AppDatabase.get(applicationContext)
            val repo = FinanceRepository(db)
            val days = com.sakume.personalfinance.ui.AppPrefs.maxCatchupDays(applicationContext)
            val result = repo.processDueSchedules(maxCatchupDays = days)

            Notify.scheduleResult(applicationContext, result)
            ListenableWorker.Result.success()
        } catch (e: IllegalArgumentException) {
            ListenableWorker.Result.success()
        } catch (e: IllegalStateException) {
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            ListenableWorker.Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK = "sakume_schedule_worker"

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
