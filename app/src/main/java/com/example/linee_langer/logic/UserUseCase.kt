package com.example.linee_langer.logic

import com.example.linee_langer.data.AnalysisRepository
import com.example.linee_langer.data.AuthRepository
import com.example.linee_langer.data.UserPreferencesManager
import javax.inject.Inject

// Dominio: Gestisce la logica di business dell'utente
class UserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val repositoryAnalysis: AnalysisRepository,
    private val userPreferencesManager: UserPreferencesManager,
) {
    suspend fun performFullLogout() {
        authRepository.signOut()
        repositoryAnalysis.deleteAllAnalysis()
        userPreferencesManager.clearAll()
    }

}

