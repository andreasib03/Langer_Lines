package com.example.linee_langer.ui.feature.onboarding

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.remote.FirebaseRepository
import com.example.linee_langer.domain.models.UserFirebaseModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import com.example.linee_langer.domain.models.LangerGoal
import com.example.linee_langer.ui.feature.auth.AuthViewModel

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val userPreferencesManager: UserPreferencesManager
): ViewModel() {

    var name by mutableStateOf("")
    var lastName by mutableStateOf("")
    var email by mutableStateOf("")
    var birthDate by mutableStateOf("")
    var selectedGoal by mutableStateOf(emptySet<LangerGoal>())
    var selectedSkinType by mutableStateOf("")


    fun importUserData(authViewModel: AuthViewModel) {
        name      = authViewModel.pendingFirstName
        lastName  = authViewModel.pendingLastName
        birthDate = authViewModel.pendingBirthDate
        email     = authViewModel.email
    }

    fun finishOnBoarding(onFinished: () -> Unit) {
        val uid = authRepository.currentUser?.uid ?: run { onFinished(); return }

        viewModelScope.launch {

            val activeEmail = email.ifBlank { authRepository.currentUser?.email ?: "" }
            val goalIds = LangerGoal.toIds(selectedGoal)
            val profile = UserFirebaseModel(
                email     = activeEmail,
                name      = name,
                eta       = birthDate,
                skinType  = selectedSkinType,
                goalId    = goalIds
            )
            firebaseRepository.saveUserProfile(uid, profile)
            userPreferencesManager.saveUserData(
                name     = name,
                email    = activeEmail,
                eta      = birthDate,
                goals    = goalIds,
                skinType = selectedSkinType
            )
            userPreferencesManager.setOnboardingCompleted(true)
            onFinished()
        }
    }
}