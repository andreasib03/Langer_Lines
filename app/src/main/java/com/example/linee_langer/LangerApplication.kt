package com.example.linee_langer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.worker.CacheCleanupWorker
import com.example.linee_langer.worker.ReminderWorker
import com.example.linee_langer.worker.SyncScheduler
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "LangerApplication"
@HiltAndroidApp
class LangerApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var analysisRepository: AnalysisRepository

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

        Firebase.initialize(context = this)
        val firebaseAppCheck = Firebase.appCheck
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        // per warning
        authRepository.currentUser?.let {
            FirebaseAuth.getInstance().useAppLanguage()
        }
        scheduleReminder()
        applicationScope.launch {
            // .first() legge il valore corrente e chiude il collect immediatamente
            val isEnabled = userPreferencesManager.isAutoCleanEnabledFlow.first()
            if (isEnabled) scheduleInitialWork()
        }

        if (authRepository.isUserLoggedIn()){
            syncScheduler.scheduleFullSync()
            applicationScope.launch { backfillLegacyAnalysesOwnership() }
        }
    }

    private suspend fun backfillLegacyAnalysesOwnership() {
        try {
            val uid = authRepository.currentUser?.uid ?: return
            if (analysisRepository.getLegacyUnassignedCount() > 0) {
                analysisRepository.assignLegacyRowsToUser(uid)
            }
        } catch (e: Exception) {
            logCaughtException(TAG, "Backfill proprietario analisi legacy fallito", e)
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