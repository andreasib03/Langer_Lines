package com.example.linee_langer.ui.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.linee_langer.data.UserPreferencesManager
import com.example.linee_langer.logic.UserUseCase
import com.example.linee_langer.service.CacheCleanupWorker
import com.example.linee_langer.service.ImageRecoveryWorker
import com.example.linee_langer.service.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userUseCase: UserUseCase,
    val userPreferencesManager: UserPreferencesManager
) : AndroidViewModel(application){

    val isOnBoardingCompleted = userPreferencesManager.isOnBoardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(500), null)

    val isDarkMode = userPreferencesManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAutoCleanEnabled = userPreferencesManager.isAutoCleanEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)


    fun toggleTheme(enabled: Boolean){
        viewModelScope.launch {
            userPreferencesManager.setDarkMode(enabled)
        }
    }

    val notificationsEnabled = userPreferencesManager.isNotificationEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun toggleNotifications(enabled: Boolean){
        viewModelScope.launch {
            userPreferencesManager.setNotificationsEnabled(enabled)
        }
    }

    fun toggleAutoClean(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.saveAutoCleanPreference(enabled)

            // Usiamo getApplication<Application>() di AndroidViewModel
            val context = getApplication<Application>()
            val workManager = WorkManager.getInstance(context)

            if (enabled) {
                val request = PeriodicWorkRequestBuilder<CacheCleanupWorker>(24, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiresDeviceIdle(true).build())
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    "CacheCleanup",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Log.d("SettingsVM", "Pulizia automatica attivata")
            } else {
                workManager.cancelUniqueWork("CacheCleanup")
                Log.d("SettingsVM", "Pulizia automatica disattivata")
            }
        }
    }

    /**
     * Esegue una pulizia manuale della cache tramite WorkManager.
     * Restituisce true se l'operazione è stata accodata correttamente.
     */
    suspend fun clearCache(): Boolean {
        return try {
            val context = getApplication<Application>()

            // Passiamo il flag "is_manual" = true
            val data = Data.Builder()
                .putBoolean("is_manual", true)
                .build()

            val clearRequest = OneTimeWorkRequestBuilder<CacheCleanupWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("ManualCacheCleanup", ExistingWorkPolicy.REPLACE, clearRequest)

            true
        } catch (e: Exception) {
            false
        }
    }

    fun triggerImageRecovery() {
        val context = getApplication<Application>()
        val recoveryRequest = OneTimeWorkRequestBuilder<ImageRecoveryWorker>().build()

        WorkManager.getInstance(context).enqueue(recoveryRequest)
        Log.d("SettingsVM", "Image Recovery Worker accodato")
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Deve esserci internet
            .setRequiresBatteryNotLow(true)               // Non se la batteria è scarica
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL, // Se fallisce, aspetta progressivamente di più
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(getApplication())
            .enqueueUniqueWork("DataSync", ExistingWorkPolicy.REPLACE, syncRequest)
    }


    fun logout(onComplete: () -> Unit) {
            viewModelScope.launch {
                userUseCase.performFullLogout()
                onComplete()
            }
    }




}