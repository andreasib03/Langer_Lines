package com.example.linee_langer.domain.usecases

import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.NotificationRepository
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.remote.FirebaseRepository
import javax.inject.Inject

private const val TAG = "UserUseCase"
// Dominio: Gestisce la logica di business dell'utente
class UserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val repositoryAnalysis: AnalysisRepository,
    private val firebaseRepository: FirebaseRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val notificationRepository: NotificationRepository
) {
    suspend fun performFullLogout() {
        authRepository.signOut()
        repositoryAnalysis.deleteAllAnalysis()
        notificationRepository.deleteAllNotifications()
        userPreferencesManager.clearUserSession()
    }

    suspend fun performFullAccountDeletion(): Result<Unit>{
        val uid = authRepository.currentUser?.uid

        if(uid != null){
            try {
                firebaseRepository.deleteDocument(uid)
            } catch (e: Exception){
                logCaughtException(TAG, "Eliminazione documento Firestore fallita (uid=$uid), proseguo comunque", e)
            }
        }

        repositoryAnalysis.deleteAllAnalysis()
        notificationRepository.deleteAllNotifications()
        userPreferencesManager.clearUserSession()

        val deleteResult = authRepository.deleteCurrentUser()
        authRepository.signOut()
        return deleteResult
    }

}