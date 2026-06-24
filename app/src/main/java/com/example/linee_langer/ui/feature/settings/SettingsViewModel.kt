package com.example.linee_langer.ui.feature.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.domain.usecases.UserUseCase
import com.example.linee_langer.ui.theme.locale.AppLocaleManager
import com.example.linee_langer.ui.theme.locale.SupportedLocale
import com.example.linee_langer.worker.CacheCleanupWorker
import com.example.linee_langer.worker.ImageRecoveryWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userUseCase: UserUseCase,
    private val userPreferencesManager: UserPreferencesManager,
    private val localeManager: AppLocaleManager
) : AndroidViewModel(application){

    val currentLocale: StateFlow<SupportedLocale> = MutableStateFlow(
        localeManager.currentLocale()
    )

    fun setLocale(locale: SupportedLocale){
        localeManager.applyLocale(locale)
        (currentLocale as MutableStateFlow).value = locale
    }
    val isOnBoardingCompleted = userPreferencesManager.isOnBoardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isDarkMode = userPreferencesManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAutoCleanEnabled = userPreferencesManager.isAutoCleanEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)


    fun toggleTheme(enabled: Boolean){
        viewModelScope.launch {
            userPreferencesManager.setDarkMode(enabled)
        }
    }


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
     fun clearCache(): Boolean {
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
            Log.e("Clear cache problem: ", "${e.message}")
            false
        }
    }

    fun triggerImageRecovery() {
        val context = getApplication<Application>()
        val recoveryRequest = OneTimeWorkRequestBuilder<ImageRecoveryWorker>().build()

        WorkManager.getInstance(context).enqueue(recoveryRequest)
        Log.d("SettingsVM", "Image Recovery Worker accodato")
    }


    fun logout(onComplete: () -> Unit) {
            viewModelScope.launch {
                userUseCase.performFullLogout()
                onComplete()
            }
    }




}