package com.example.linee_langer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.worker.CacheCleanupWorker
import com.example.linee_langer.worker.ReminderWorker
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory) // 👈 Dì a WorkManager di usare Hilt
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate(){
        super.onCreate()
        applicationScope.launch {
            // .first() legge il valore corrente e chiude il collect
            val isEnabled = userPreferencesManager.isAutoCleanEnabledFlow.first()
            if (isEnabled) {
                scheduleInitialWork()
            }
            scheduleReminder()
        }
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
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}