package com.sakume.personalfinance

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sakume.personalfinance.data.BackupWorker
import com.sakume.personalfinance.ui.AppPrefs
import com.sakume.personalfinance.ui.FinanceApp
import com.sakume.personalfinance.ui.FinanceViewModel
import com.sakume.personalfinance.ui.Notify

class MainActivity : ComponentActivity() {
    private val vm: FinanceViewModel by viewModels()

    /** Tab yang diminta dari ketukan notifikasi (mis. "Bills"); dikosongkan setelah dipakai. */
    private var requestedTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedTab = intent?.getStringExtra(Notify.EXTRA_OPEN_TAB)
        setContent {
            FinanceApp(
                vm,
                requestedTab = requestedTab,
                onTabHandled = {
                    requestedTab = null
                    // Supaya rotasi layar tidak melempar pengguna ke tab itu lagi.
                    intent?.removeExtra(Notify.EXTRA_OPEN_TAB)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTab = intent.getStringExtra(Notify.EXTRA_OPEN_TAB)
    }

    /** Setiap app ditinggalkan, simpan backup terbaru (dilewati otomatis bila data tidak berubah). */
    override fun onStop() {
        super.onStop()
        if (AppPrefs.autoBackupEnabled(this)) BackupWorker.enqueueNow(this)
    }
}
