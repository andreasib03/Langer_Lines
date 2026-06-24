package com.example.linee_langer.data.remote

import com.example.linee_langer.domain.exceptions.AppException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firebaseRepository: FirebaseRepository
) {

    // Single Source of Truth per l'utente
    val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false

    fun getGoogleEmail(): String = auth.currentUser?.email ?: ""

    /**
     * Invia l'email di verifica all'utente attualmente loggato.
     * Restituisce true se l'invio ha successo, false altrimenti.
     */
    suspend fun sendVerificationEmail(): Boolean {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.sendEmailVerification().await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }


    suspend fun userProfileExists(uid: String): Boolean = firebaseRepository.checkIfUserProfileExists(uid)



    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            auth.currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(AppException.Authentication.EmailAlreadyExists("unknown"))
        } catch (e: Exception){
            Result.failure(e)
        }
    }



    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser>{
        return try {
            val result = auth.signInWithEmailAndPassword(email,password).await()
            val user = result.user ?: return Result.failure(AppException.Authentication.UserNull("Utente nullo dopo signIn"))

            if(!user.isEmailVerified){
                return Result.failure(
                    AppException.Authentication.EmailNotVerified("Email non verificata")
                )
            }

            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException){
            Result.failure(AppException.Authentication.EmailAlreadyExists("google.com"))
        } catch (e: Exception){
            Result.failure(e)
        }

    }


    suspend fun signInWithGoogle(idToken: String) : Result<FirebaseUser>{
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(AppException.Authentication.UserNull("Google User is null"))

            Result.success(user)

        } catch(e: FirebaseAuthUserCollisionException){
            Result.failure(AppException.Authentication.EmailAlreadyExists("password"))
        } catch(e: Exception){
            if (e.message?.contains("account-exists-with-different-credential") == true ||
                e.message?.contains("ERROR_ACCOUNT_EXISTS") == true) {
                Result.failure(AppException.Authentication.EmailAlreadyExists("password"))
            } else {
                Result.failure(e)
            }
        }
    }

    fun signOut(){
        auth.signOut()
    }

    suspend fun deleteCurrentUser(): Result<Unit>{
        return try {
            val user = auth.currentUser ?: return Result.failure(AppException.Authentication.UserNull("Nessun utente loggato"))
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    // Re-autenticazione con password (per utenti email/password)
    suspend fun reauthenticateWithPassword(password: String): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(AppException.Authentication.UserNull("Nessun utente"))
            val email = user.email
                ?: return Result.failure(AppException.Authentication.UserNull("Email mancante"))
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(AppException.Authentication.InvalidCredentials("Password errata"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(AppException.Authentication.UserNull("Nessun utente"))
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isGoogleUser(): Boolean {
        val user = auth.currentUser ?: return false
        return user.providerData.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID || it.providerId == "google.com"
        }
    }

}