package com.example.linee_langer.ui.feature.auth

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
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.local.UserPreferencesManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import com.example.linee_langer.R
import com.example.linee_langer.domain.exceptions.AppException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject



@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferencesManager: UserPreferencesManager,
    @ApplicationContext private val appContext: Context
): ViewModel(){

    private var _email    by mutableStateOf("")
    private var _password by mutableStateOf("")

    val email: String get() = _email
    val password: String get() = _password

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    // Dati anagrafici raccolti da RegisterScreen — letti da OnBoardingViewModel
    var pendingFirstName by mutableStateOf("")
    var pendingLastName  by mutableStateOf("")
    var pendingBirthDate by mutableStateOf("")

    fun updateEmail(value: String)    { _email = value }
    fun updatePassword(value: String) { _password = value }

    fun resetState() {
        uiState = AuthUiState.Idle
        _email = ""
        _password = ""
        pendingFirstName = ""
        pendingLastName  = ""
        pendingBirthDate = ""
    }

    // --- LOGIN ---
    fun handleLogin() {
        val emailToUse = _email.trim()
        if (emailToUse.isBlank() || _password.isBlank()) {
            uiState = AuthUiState.Error(
                appContext.getString(R.string.error_fill_all_fields)
            )
            return
        }
        viewModelScope.launch {
            try {
                uiState = AuthUiState.Loading
                authRepository.signInWithEmail(emailToUse, _password)
                    .onSuccess { user ->
                        if (!authRepository.isEmailVerified()) {
                            uiState = AuthUiState.Error(
                                appContext.getString(R.string.error_email_not_verified)
                            )
                        } else {
                            checkFirestoreProfileAndProceed(user.uid)
                        }
                    }
                    .onFailure {
                        uiState = AuthUiState.Error(
                            it.localizedMessage
                                ?: appContext.getString(R.string.error_unknown)
                        )
                    }
            } finally {
                _password = ""
            }
        }
    }

    // --- REGISTRAZIONE ---
    fun handleRegister() {
        val emailToUse = _email.trim()
        if (emailToUse.isBlank() || _password.isBlank()) {
            uiState = AuthUiState.Error(
                appContext.getString(R.string.error_fill_all_fields)
            )
            return
        }
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            try {
                // 1. Recupero immediato se già loggato (unverified)
                val currentUser = authRepository.currentUser
                if (currentUser != null && currentUser.email == emailToUse && !currentUser.isEmailVerified) {
                    authRepository.sendVerificationEmail()
                    uiState = AuthUiState.RegistrationPendingVerification
                    return@launch
                }

                // 2. Tentativo creazione account
                val regResult = authRepository.signUpWithEmail(emailToUse, _password)
                
                if (regResult.isSuccess) {
                    uiState = AuthUiState.RegistrationPendingVerification
                    return@launch
                }

                // 3. Gestione errore collisione (account già esistente)
                val error = regResult.exceptionOrNull()
                if (error is AppException.Authentication.EmailAlreadyExists) {
                    val loginResult = authRepository.signInWithEmail(emailToUse, _password)
                    
                    if (loginResult.isSuccess) {
                        val user = loginResult.getOrThrow()
                        // Se loggato con successo, controlliamo il profilo
                        checkFirestoreProfileAndProceed(user.uid)
                    } else {
                        val loginError = loginResult.exceptionOrNull()
                        // Se fallisce il login perché non verificato, inviamo email
                        if (loginError is AppException.Authentication.EmailNotVerified) {
                            authRepository.sendVerificationEmail()
                            uiState = AuthUiState.RegistrationPendingVerification
                        } else {
                            // Altro errore (es. password errata per l'account esistente)
                            uiState = AuthUiState.Error(buildErrorMessage(error))
                        }
                    }
                } else {
                    // Altro errore di registrazione
                    uiState = AuthUiState.Error(
                        error?.localizedMessage ?: appContext.getString(R.string.error_unknown)
                    )
                }
            } catch (e: Exception) {
                uiState = AuthUiState.Error(
                    e.localizedMessage ?: appContext.getString(R.string.error_unknown)
                )
            } finally {
                _password = ""
            }
        }
    }

    // --- GOOGLE ---
    fun handleGoogleSignIn(context: Context) {
        val credentialManager = CredentialManager.create(context)
        uiState = AuthUiState.Loading

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    authRepository.signInWithGoogle(credential.idToken)
                        .onSuccess { user ->
                            _email = authRepository.getGoogleEmail()
                            checkFirestoreProfileAndProceed(user.uid, isGoogle = true)
                        }
                        .onFailure { error ->
                            uiState = AuthUiState.Error(buildErrorMessage(error))
                        }
                } else {
                    uiState = AuthUiState.Error(
                        appContext.getString(R.string.error_google_signin)
                    )
                }
            } catch (e: GetCredentialException) {
                uiState = AuthUiState.Error(
                    e.message ?: appContext.getString(R.string.error_google_signin)
                )
            }
        }
    }

    // --- VERIFICA EMAIL ---
    fun checkAndProceed(onVerified: () -> Unit, onNotVerified: () -> Unit) {
        viewModelScope.launch {
            authRepository.currentUser?.reload()?.await()
            if (authRepository.isEmailVerified()) {
                onVerified()
            } else {
                uiState = AuthUiState.NotVerifiedYet
                onNotVerified()
            }
        }
    }

    fun resendVerificationEmail(onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = authRepository.sendVerificationEmail()
            uiState = if (success) AuthUiState.EmailSent else AuthUiState.EmailSendError
        }
    }

    /**
     * Elimina l'account corrente se non è verificato.
     * Utile per "ripulire" account creati per errore o rimasti in sospeso
     * che impediscono una nuova registrazione pulita.
     */
    fun deleteUnverifiedAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            val user = authRepository.currentUser
            if (user != null && !user.isEmailVerified) {
                authRepository.deleteCurrentUser()
                resetState()
                onComplete()
            }
        }
    }

    // --- INTERNO ---
    private suspend fun checkFirestoreProfileAndProceed(
        uid: String,
        isGoogle: Boolean = false
    ) {
        val exists = authRepository.userProfileExists(uid)
        if (exists) {
            userPreferencesManager.setOnboardingCompleted(true)
            uiState = AuthUiState.Success(isExistingUser = true)
        } else {
            uiState = AuthUiState.Success(isExistingUser = false, isGoogleUser = isGoogle)
        }
    }

    private fun buildErrorMessage(error: Throwable): String {
        return when (error){
            is AppException.Authentication.EmailAlreadyExists -> when (error.existingProvider){
                "google.com" -> appContext.getString(R.string.error_email_exists_google)
                "password"   -> appContext.getString(R.string.error_email_exists_password)
                else         -> appContext.getString(R.string.error_email_exists_password) // Default to password prompt for standard collision
            }
            is AppException.Authentication.EmailNotVerified ->
                appContext.getString(R.string.error_email_not_verified)
            is AppException.Authentication.UserNull ->
                appContext.getString(R.string.error_unknown)
            else -> error.localizedMessage ?: appContext.getString(R.string.error_unknown)
        }
    }

}

