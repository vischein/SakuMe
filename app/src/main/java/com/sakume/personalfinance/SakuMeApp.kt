package com.sakume.personalfinance

import android.app.Application
import com.sakume.personalfinance.data.BackupWorker
import com.sakume.personalfinance.data.ScheduleWorker
import com.sakume.personalfinance.ui.AppPrefs
import com.sakume.personalfinance.ui.Notify

class SakuMeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notify.ensureChannel(this)
        AppPrefs.removeLegacyPin(this)

        ScheduleWorker.enqueue(this)
        if (AppPrefs.autoBackupEnabled(this)) BackupWorker.enqueuePeriodic(this)
    }
}
