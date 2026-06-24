package com.example.linee_langer.domain.usecases

import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.remote.FirebaseRepository
import javax.inject.Inject

// Dominio: Gestisce la logica di business dell'utente
class UserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val repositoryAnalysis: AnalysisRepository,
    private val firebaseRepository: FirebaseRepository,
    private val userPreferencesManager: UserPreferencesManager,
) {
    suspend fun performFullLogout() {
        authRepository.signOut()
        repositoryAnalysis.deleteAllAnalysis()
        userPreferencesManager.clearUserSession()
    }

    suspend fun performFullAccountDeletion(): Result<Unit>{
        val uid = authRepository.currentUser?.uid

        if(uid != null){
            try {
                firebaseRepository.deleteDocument(uid)
            } catch (_: Exception){ }
        }

        repositoryAnalysis.deleteAllAnalysis()
        userPreferencesManager.clearUserSession()

        val deleteResult = authRepository.deleteCurrentUser()
        authRepository.signOut()
        return deleteResult
    }

}