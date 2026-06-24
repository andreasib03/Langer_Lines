package com.example.linee_langer.domain.exceptions

sealed class AppException(message: String) : Exception(message) {

    class Network(message: String) : AppException(message)
    class Database(message: String) : AppException(message)
    class Validation(message: String) : AppException(message)

    // Authentication — centralizza tutti gli errori di autenticazione
    sealed class Authentication(message: String) : AppException(message) {

        // Email già registrata con un altro provider
        class EmailAlreadyExists(val existingProvider: String) :
            Authentication("Email già registrata con provider: $existingProvider")

        // Credenziali non valide
        class InvalidCredentials(message: String) : Authentication(message)

        // Email non verificata
        class EmailNotVerified(message: String) : Authentication(message)

        // Utente non trovato / nullo dopo autenticazione
        class UserNull(message: String) : Authentication(message)

        // Google Sign-In fallito
        class GoogleSignInFailed(message: String) : Authentication(message)
    }
}
