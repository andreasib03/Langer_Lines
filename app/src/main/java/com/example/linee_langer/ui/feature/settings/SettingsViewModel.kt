package com.example.linee_langer.ui.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.linee_langer.core.utils.logCaughtException
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "SettingsViewModel"
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userUseCase: UserUseCase,
    private val userPreferencesManager: UserPreferencesManager,
    private val localeManager: AppLocaleManager
) : AndroidViewModel(application){

    private val _recoveryState = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()

    sealed interface RecoveryState {
        object Idle : RecoveryState
        object Loading : RecoveryState
        data class Success(val count: Int) : RecoveryState
        object Error : RecoveryState
    }

    fun resetRecoveryState() {
        _recoveryState.value = RecoveryState.Idle
    }

    sealed interface CacheCleanState {
        object Idle : CacheCleanState
        object Loading : CacheCleanState
        object Success : CacheCleanState
        object Error : CacheCleanState
    }

    private val _cacheCleanState = MutableStateFlow<CacheCleanState>(CacheCleanState.Idle)
    val cacheCleanState: StateFlow<CacheCleanState> = _cacheCleanState.asStateFlow()

    fun resetCacheCleanState() {
        _cacheCleanState.value = CacheCleanState.Idle
    }



    private val _currentLocale = MutableStateFlow(localeManager.currentLocale())
    val currentLocale: StateFlow<SupportedLocale> = _currentLocale

    fun setLocale(locale: SupportedLocale){
        localeManager.applyLocale(locale)
        _currentLocale.value = locale
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
            } else {
                workManager.cancelUniqueWork("CacheCleanup")
            }
        }
    }

    /**
     * Esegue una pulizia manuale della cache tramite WorkManager.
     * Restituisce true se l'operazione è stata accodata correttamente.
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                _cacheCleanState.value = CacheCleanState.Loading

                val context = getApplication<Application>()
                val data = Data.Builder()
                    .putBoolean("is_manual", true)
                    .build()

                val clearRequest = OneTimeWorkRequestBuilder<CacheCleanupWorker>()
                    .setInputData(data)
                    .build()

                val workManager = WorkManager.getInstance(context)
                workManager.enqueueUniqueWork(
                    "ManualCacheCleanup",
                    ExistingWorkPolicy.REPLACE,
                    clearRequest
                )

                // Ascolto del completamento tramite Flow
                workManager.getWorkInfoByIdLiveData(clearRequest.id)
                    .asFlow()
                    .collect { workInfo ->
                        if (workInfo != null && workInfo.state.isFinished) {
                            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                _cacheCleanState.value = CacheCleanState.Success
                            } else if (workInfo.state == WorkInfo.State.FAILED) {
                                _cacheCleanState.value = CacheCleanState.Error
                            }
                        }
                    }
            } catch (e: Exception) {
                logCaughtException(TAG, "Accodamento pulizia cache manuale fallito", e)
                _cacheCleanState.value = CacheCleanState.Error
            }
        }
    }

    fun triggerImageRecovery() {
        viewModelScope.launch {
            try {
                _recoveryState.value = RecoveryState.Loading

                val context = getApplication<Application>()
                val recoveryRequest = OneTimeWorkRequestBuilder<ImageRecoveryWorker>().build()
                val workManager = WorkManager.getInstance(context)

                // Accodiamo il lavoro
                workManager.enqueue(recoveryRequest)

                // Trasformiamo il LiveData del WorkInfo in Flow per osservarlo nelle Coroutine
                workManager.getWorkInfoByIdLiveData(recoveryRequest.id)
                    .asFlow()
                    .collect { workInfo ->
                        if (workInfo != null && workInfo.state.isFinished) {
                            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                // Recuperiamo il parametro "recovered_count" inviato da ImageRecoveryWorker
                                val count = workInfo.outputData.getInt("recovered_count", 0)
                                _recoveryState.value = RecoveryState.Success(count)
                            } else if (workInfo.state == WorkInfo.State.FAILED) {
                                _recoveryState.value = RecoveryState.Error
                            }
                        }
                    }
            } catch (e: Exception) {
                logCaughtException(TAG, "Errore avvio recupero immagini", e)
                _recoveryState.value = RecoveryState.Error
            }
        }
    }


    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            userUseCase.performFullLogout()
            onComplete()
        }
    }




}