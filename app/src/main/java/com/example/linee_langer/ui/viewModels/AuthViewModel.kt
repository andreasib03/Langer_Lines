package com.example.linee_langer.ui.viewModels

import android.content.Context
import com.example.linee_langer.BuildConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.data.AuthRepository
import com.example.linee_langer.data.UserPreferencesManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferencesManager: UserPreferencesManager
): ViewModel(){

    var email by mutableStateOf("")
    var password by mutableStateOf("")

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    fun handleAuth(isLoginMode: Boolean) {
        if (email.isBlank() || password.isBlank()) {
            uiState = AuthUiState.Error("Compila tutti i campi")
            return
        }

        viewModelScope.launch {
            uiState = AuthUiState.Loading

            // 2. FIX: Chiamata alle funzioni suspend che restituiscono Result
            val result = if (isLoginMode) {
                authRepository.signInWithEmail(email, password)
            } else {
                authRepository.signUpWithEmail(email, password)
                // Se è un nuovo utente, dopo il signup restituiamo un Result di successo generico
                // Nota: Assicurati che signUpWithEmail restituisca Result<Unit> o Result<FirebaseUser>
                Result.success(Unit)
            }

            result.onSuccess {
                // 3. FIX: isEmailVerified va controllato sull'utente corrente del repository
                if (!authRepository.isEmailVerified()) {
                    uiState = AuthUiState.Error("Verifica la tua email prima di accedere.")
                } else {
                    val userEmail = authRepository.currentUser?.email ?: email
                    checkFirestoreProfileAndProceed(userEmail)
                }
            }.onFailure { error ->
                uiState = AuthUiState.Error(error.localizedMessage ?: "Errore sconosciuto")
            }
        }
    }


    private suspend fun checkFirestoreProfileAndProceed(email: String) {
        // Usiamo suspendCancellableCoroutine o convertiamo il metodo in FirebaseRepo in suspend
        val exists = authRepository.checkIfUserProfileExistsSuspend(email)

        if (exists) {
            userPreferencesManager.setOnboardingCompleted(true)
            uiState = AuthUiState.Success(isExistingUser = true)
        } else {
            uiState = AuthUiState.Success(isExistingUser = false)
        }
    }

    fun handleGoogleSignIn(context: Context) {
        val credentialManager = CredentialManager.create(context)
        uiState = AuthUiState.Loading

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID) // 5. FIX: Usa il BuildConfig generato
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    // 6. FIX: Converti anche signInWithGoogle in suspend nel Repository
                    val signInResult = authRepository.signInWithGoogle(credential.idToken)

                    signInResult.onSuccess { user ->
                        checkFirestoreProfileAndProceed(user.email ?: "")
                    }.onFailure { error ->
                        uiState = AuthUiState.Error(error.localizedMessage ?: "Google Sign-In failed")
                    }

                }
            } catch (e: GetCredentialException) {
                uiState = AuthUiState.Error(e.message ?: "Google Sign-In fallito.")
            }
        }
    }

}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val isExistingUser: Boolean) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

