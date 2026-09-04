package com.example.linee_langer.domain.usecases

import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.NotificationRepository
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.remote.FirebaseRepository
import com.example.linee_langer.worker.SyncScheduler
import java.util.UUID
import javax.inject.Inject

private const val TAG = "UserUseCase"
// Dominio: Gestisce la logica di business dell'utente
class UserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val repositoryAnalysis: AnalysisRepository,
    private val firebaseRepository: FirebaseRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val notificationRepository: NotificationRepository,
    private val syncScheduler: SyncScheduler
) {

    fun scheduleSync(force: Boolean = false): UUID? {
        return syncScheduler.scheduleFullSync(force)
    }
    suspend fun performFullLogout() {
        val uid = authRepository.currentUser?.uid
        authRepository.signOut()
        if (!uid.isNullOrBlank()){
            repositoryAnalysis.deleteSyncedAnalysis(uid)
        }
        notificationRepository.deleteAllNotifications()
        userPreferencesManager.clearUserSession()
    }

    suspend fun performFullAccountDeletion(): Result<Unit>{
        val uid = authRepository.currentUser?.uid ?: return Result.failure(IllegalStateException("Nessun utente loggato"))

        try {
            val localAnalyses = repositoryAnalysis.getAllAnalysesInternal(uid)
            localAnalyses.forEach { analysisEntity ->
                firebaseRepository.deleteAnalysisDocument(uid, analysisEntity.date)
            }
        } catch (e: Exception){
            logCaughtException(TAG, "Pulizia best-effort delle analisi remote fallita (uid=$uid), proseguo comunque", e)
        }

        try {
            firebaseRepository.deleteDocument(uid)
        } catch (e: Exception) {
            logCaughtException(TAG, "Eliminazione documento Firestore fallita (uid=$uid), proseguo comunque", e)
        }

        val deleteResult = authRepository.deleteCurrentUser()
        if(deleteResult.isFailure){
            return deleteResult
        }

        repositoryAnalysis.deleteAllAnalysisForUser(uid)
        notificationRepository.deleteAllNotifications()
        userPreferencesManager.clearUserSession()
        authRepository.signOut()

        return deleteResult
    }

}