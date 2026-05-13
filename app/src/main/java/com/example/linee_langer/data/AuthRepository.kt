package com.example.linee_langer.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
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

    /**
     * Invia l'email di verifica all'utente attualmente loggato.
     * Restituisce true se l'invio ha successo, false altrimenti.
     */
    suspend fun sendVerificationEmail(): Boolean {
        return try {
            val user = auth.currentUser
            if (user != null) {
                // .await() trasforma la Task di Firebase in una coroutine suspend
                user.sendEmailVerification().await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            // Gestisci eccezioni come: troppe richieste inviate di fila (TooManyRequestsException)
            false
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            auth.currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false


    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser>{
        return try {
            val result = auth.signInWithEmailAndPassword(email,password).await()
            val user = result.user ?: throw Exception("Utente nullo")

            if(!user.isEmailVerified){
                //Excpetion
            }

            Result.success(user)
        } catch (e: Exception){
            Result.failure(e)
        }

    }

    fun signOut(){
        auth.signOut()
    }

    fun checkProfileExistence(email: String, onComplete: (Boolean) -> Unit) {
        firebaseRepository.checkIfUserProfileExists(email, onComplete)
    }

    suspend fun checkIfUserProfileExistsSuspend(email: String): Boolean {
        if (email.isBlank()) return false

        return try {
            // .await() sospende la coroutine finché il task non è completato
            val document = firestore.collection("users")
                .document(email)
                .get()
                .await()

            document.exists()
        } catch (e: Exception) {
            // Gestisci errori di rete o permessi
            false
        }
    }

    suspend fun signInWithGoogle(idToken: String) : Result<FirebaseUser>{
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google User is null")
            Result.success(user)
        } catch(e: Exception){
            Result.failure(e)
        }
    }
}

