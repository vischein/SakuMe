package com.sakume.personalfinance

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sakume.personalfinance.data.BackupWorker
import com.sakume.personalfinance.ui.AppLock
import com.sakume.personalfinance.ui.AppPrefs
import com.sakume.personalfinance.ui.FinanceApp
import com.sakume.personalfinance.ui.FinanceViewModel
import com.sakume.personalfinance.ui.Notify
import com.sakume.personalfinance.ui.screens.LockScreen

class MainActivity : FragmentActivity() {
    private val vm: FinanceViewModel by viewModels()

    private var requestedTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedTab = intent?.getStringExtra(Notify.EXTRA_OPEN_TAB)
        AppLock.init(this)
        setContent {
            if (AppLock.isLocked) {
                LockScreen()
            } else {
                FinanceApp(
                    vm,
                    requestedTab = requestedTab,
                    onTabHandled = {
                        requestedTab = null
                        intent?.removeExtra(Notify.EXTRA_OPEN_TAB)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTab = intent.getStringExtra(Notify.EXTRA_OPEN_TAB)
    }

    override fun onStart() {
        super.onStart()
        AppLock.onForegrounded(this)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) AppLock.onBackgrounded()
        if (AppPrefs.autoBackupEnabled(this)) BackupWorker.enqueueNow(this)
    }
}
