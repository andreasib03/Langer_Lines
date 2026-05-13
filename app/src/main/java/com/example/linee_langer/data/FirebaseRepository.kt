package com.example.linee_langer.data

import android.net.Uri
import com.example.linee_langer.db.SkinAnalysisEntry
import com.example.linee_langer.domain.models.UserFirebaseModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume

class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage

){

    // Dentro FirebaseRepository.kt
    suspend fun getUserProfile(email: String): UserFirebaseModel? {
        return try {
            val document = firestore.collection("users").document(email).get().await()
            document.toObject(UserFirebaseModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verifica in modo rapido se il profilo dell'utente esiste già su Firestore.
     * Restituisce true se il documento esiste, false altrimenti o in caso di errore.
     */
    fun checkIfUserProfileExists(email: String, onResult: (Boolean) -> Unit) {
        if (email.isBlank()) {
            onResult(false)
            return
        }

        firestore.collection("users")
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                // Il profilo esiste se il documento non è nullo e i dati sono presenti sul database
                val exists = document != null && document.exists()
                onResult(exists)
            }
            .addOnFailureListener { exception ->
                // In caso di problemi di rete o permessi, ritorniamo false per sicurezza
                onResult(false)
            }
    }

    suspend fun deleteDocument(email: String): Boolean = suspendCancellableCoroutine { continuation ->
        if (email.isBlank()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        firestore.collection("users")
            .document(email)
            .delete()
            .addOnSuccessListener { if (continuation.isActive) continuation.resume(true) }
            .addOnFailureListener { if (continuation.isActive) continuation.resume(false) }
    }



    suspend fun saveUserProfile(user: UserFirebaseModel): Boolean{
        return try {
            firestore.collection("users").document(user.email).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadSkinImage(userEmail: String, imageUri: Uri): String? {
        return try {
            val imageRef = storage.reference.child("skin_images/$userEmail/${System.currentTimeMillis()}.jpg")
            imageRef.putFile(imageUri).await() // Richiede import kotlinx.coroutines.tasks.await
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadAnalysisSync(userEmail: String, analysis: SkinAnalysisEntry): Boolean = suspendCancellableCoroutine { continuation ->
        val analysisData = hashMapOf(
            "date" to analysis.date,
            "bodyPartId" to analysis.bodyPartId,
            "resultSummary" to analysis.resultSummary,
            "imagePath" to analysis.imagePath // Qui potresti voler caricare prima l'immagine su Storage
        )

        firestore.collection("users")
            .document(userEmail)
            .collection("analyses")
            .document(analysis.date.toString())
            .set(analysisData)
            .addOnSuccessListener { if (continuation.isActive) continuation.resume(true) }
            .addOnFailureListener { if (continuation.isActive) continuation.resume(false) }
    }
}