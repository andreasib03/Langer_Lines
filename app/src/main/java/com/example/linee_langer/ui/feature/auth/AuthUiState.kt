package com.example.linee_langer.ui.feature.auth

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()

    data class Success(
        val isExistingUser: Boolean,
        val isGoogleUser: Boolean = false
    ) : AuthUiState()

    data class Error(val message: String) : AuthUiState()

    // Registrazione completata — in attesa verifica email
    object RegistrationPendingVerification : AuthUiState()

    // Feedback verifica email (per snackbar in EmailVerificationScreen)
    object EmailSent      : AuthUiState()
    object EmailSendError : AuthUiState()
    object NotVerifiedYet : AuthUiState()
}

