package com.example.linee_langer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.worker.CacheCleanupWorker
import com.example.linee_langer.worker.ReminderWorker
import com.example.linee_langer.worker.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class LangerApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var syncScheduler: SyncScheduler

    // SupervisorJob conservato per poter fare cancel() in onTerminate()
    private val appJob = SupervisorJob()
    private val applicationScope = CoroutineScope(appJob + Dispatchers.IO)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            // DEBUG solo nei build di sviluppo; in release si usa WARN per evitare
            // output verboso in produzione e potenziali leak di dati nei log.
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG
                else android.util.Log.WARN
            )
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleReminder()
        applicationScope.launch {
            // .first() legge il valore corrente e chiude il collect immediatamente
            val isEnabled = userPreferencesManager.isAutoCleanEnabledFlow.first()
            if (isEnabled) scheduleInitialWork()
        }

        if (authRepository.isUserLoggedIn()){
            syncScheduler.scheduleFullSync()
        }
    }

    /** Chiamato dal sistema (principalmente in test/emulatore) — libera il CoroutineScope. */
    override fun onTerminate() {
        appJob.cancel()
        super.onTerminate()
    }

    private fun scheduleInitialWork() {
        val request = PeriodicWorkRequestBuilder<CacheCleanupWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CacheCleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleReminder() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ReminderWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}