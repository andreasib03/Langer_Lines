package com.example.linee_langer.ui.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.data.AuthRepository
import com.example.linee_langer.data.FirebaseRepository
import com.example.linee_langer.data.UserPreferencesManager
import com.example.linee_langer.domain.models.UserFirebaseModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val userPreferencesManager: UserPreferencesManager
): ViewModel() {

    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var eta by mutableStateOf("")
    var selectedGoal by mutableIntStateOf(0)
    var selectedSkinType by mutableStateOf("")

    val isInputValid: Boolean
        get() = name.isNotBlank() &&
                eta.isNotBlank() &&
                selectedSkinType.isNotBlank() &&
                selectedGoal != 0

    fun finishOnBoarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            userPreferencesManager.setOnboardingCompleted(true)

            val activeEmail = email.ifBlank { authRepository.currentUser?.email ?: "" }

            if (name.isNotBlank() && eta.isNotBlank() && selectedSkinType.isNotBlank() && selectedGoal != 0) {
                val newUserProfile = UserFirebaseModel(
                    email = activeEmail,
                    name = name,
                    eta = eta,
                    skinType = selectedSkinType,
                    goalId = selectedGoal
                )

                val isSaved = firebaseRepository.saveUserProfile(newUserProfile)
                if(isSaved){
                    onFinished()
                } else {
                    // Opzionale: qui potresti gestire un errore di rete
                    // Per ora procediamo comunque per non bloccare l'utente
                    onFinished()
                }

            } else {
                onFinished()
            }

        }
    }
}
