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

    fun updateEmail(value: String)    { _email = value.trim() }
    fun updatePassword(value: String) { _password = value }

    fun resetState() { uiState = AuthUiState.Idle }

    // --- LOGIN ---
    fun handleLogin() {
        if (_email.isBlank() || _password.isBlank()) {
            uiState = AuthUiState.Error(
                appContext.getString(R.string.error_fill_all_fields)
            )
            return
        }
        viewModelScope.launch {
            try {
                uiState = AuthUiState.Loading
                authRepository.signInWithEmail(_email, _password)
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
        if (_email.isBlank() || _password.isBlank()) {
            uiState = AuthUiState.Error(
                appContext.getString(R.string.error_fill_all_fields)
            )
            return
        }
        viewModelScope.launch {
            try {
                uiState = AuthUiState.Loading
                authRepository.signUpWithEmail(_email, _password)
                    .onSuccess {
                        uiState = AuthUiState.RegistrationPendingVerification
                    }
                    .onFailure { error ->
                        uiState = AuthUiState.Error(buildErrorMessage(error))
                    }
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
                else         -> appContext.getString(R.string.error_email_exists_generic)
            }
            is AppException.Authentication.EmailNotVerified ->
                appContext.getString(R.string.error_email_not_verified)
            is AppException.Authentication.UserNull ->
                appContext.getString(R.string.error_unknown)
            else -> error.localizedMessage ?: appContext.getString(R.string.error_unknown)
        }
    }

}

