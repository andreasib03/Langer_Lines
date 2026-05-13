package com.example.linee_langer.ui.viewModels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.dao.AnalysisWithLines
import com.example.linee_langer.data.AnalysisRepository
import com.example.linee_langer.data.AuthRepository
import com.example.linee_langer.data.FirebaseRepository
import com.example.linee_langer.data.UserPreferencesManager
import com.example.linee_langer.domain.models.UserFirebaseModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri
import com.example.linee_langer.ui.utils.exportDataAsPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val repositoryAnalysis: AnalysisRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val fullHistory: Flow<List<AnalysisWithLines>> = repositoryAnalysis.allAnalyses
    private val _userProfile = MutableStateFlow<UserFirebaseModel?>(null)
    val userProfile: StateFlow<UserFirebaseModel?> = _userProfile.asStateFlow()

    private val _profileImageUri = MutableStateFlow<Uri?>(null)
    val profileImageUri = _profileImageUri.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    val userSkinType = _userProfile.map { it?.skinType ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified = _isEmailVerified.asStateFlow()

    val isGoogleUser: Boolean
        get() {
            val user = authRepository.currentUser ?: return false

            // Controlliamo se tra i provider registrati nell'account c'è "google.com"
            val hasGoogleProvider = user.providerData.any { provider ->
                provider.providerId == com.google.firebase.auth.GoogleAuthProvider.PROVIDER_ID ||
                        provider.providerId == "google.com"
            }

            return hasGoogleProvider
        }

    init {
        viewModelScope.launch {
            userPreferencesManager.profileImageUriFlow.collect { uriString ->
                // Se c'è una stringa salvata, la riconvertiamo in Uri, altrimenti rimane null
                _profileImageUri.value = if (!uriString.isNullOrBlank()) uriString.toUri() else null
            }
        }
        loadUserProfile()

    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                // Aggiorna lo stato dell'utente corrente da Firebase (es. se ha appena cliccato il link)
                authRepository.currentUser?.reload()?.await()
                // Aggiorna il nostro StateFlow locale
                _isEmailVerified.value = authRepository.currentUser?.isEmailVerified ?: false

                val userEmail = authRepository.currentUser?.email
                if (userEmail != null) {
                    val profile = firebaseRepository.getUserProfile(userEmail)
                    if (profile != null) {
                        _userProfile.value = profile
                        Log.d("ProfileViewModel", "Profilo caricato con successo per: ${profile.email}")
                    } else {
                        Log.e("ProfileViewModel", "Firebase ha restituito un profilo null")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Errore durante il caricamento: ${e.message}")
            }
        }
    }

    var isExporting by mutableStateOf(false)
        private set

    fun generateReport(context: Context, userData: UserFirebaseModel, analyses: List<AnalysisWithLines>, skinType: String) {
        viewModelScope.launch(Dispatchers.Default) {
            isExporting = true
            // Chiamiamo la tua utility modificata
            try {
                withContext(Dispatchers.Main) {
                    exportDataAsPdf(context, userData, analyses, skinType)
                }
            } finally {
                isExporting = false
            }
        }
    }

    // Funzione di Reset modificata per scollegare l'account da Firebase Auth
    fun signOut(onSignOutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            _userProfile.value = null
            onSignOutComplete()
        }
    }

    fun updateProfileImage(uri: Uri) {
        _profileImageUri.value = uri
        viewModelScope.launch {
            try {
                userPreferencesManager.saveProfileImageUri(uri.toString())
            } catch (e: Exception) {
                Log.e("Error updating profile image", "${e.message}")
                // Gestisci eventuali errori di scrittura (es. disco pieno)
            }
        }
    }

    fun updateEmail(newEmail: String) {
        val currentProfile = _userProfile.value ?: return
        val updatedProfile = currentProfile.copy(email = newEmail)

        viewModelScope.launch {
            val success = firebaseRepository.saveUserProfile(updatedProfile)
            if (success) {
                _userProfile.value = updatedProfile
            }
        }
    }

    fun sendVerification(onResult: (Boolean) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            val success = authRepository.sendVerificationEmail()
            _isLoading.value = false
            onResult(success)
        }
    }

    fun clearAllData(user: UserFirebaseModel?, onComplete: () -> Unit) {
        // 1. Attiva un eventuale indicatore di caricamento nella UI
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 2. Cancella il documento da Firestore PRIMA del sign out (necessario per le regole di autenticazione)
                val isFirebaseDeleted = firebaseRepository.deleteDocument(user?.email ?: "")

                if (isFirebaseDeleted) {
                    Log.d("ProfileViewModel", "Documento cloud eliminato con successo.")
                } else {
                    Log.e("ProfileViewModel", "Errore o documento non trovato sul cloud, procedo comunque con i dati locali.")
                }

                // 3. Svuota il database locale Room e le SharedPreferences/DataStore
                repositoryAnalysis.deleteAllAnalysis()
                userPreferencesManager.clearAll()

                // 4. Reset dello stato della UI del profilo
                _userProfile.value = null

                // 5. Effettua il sign out da Firebase Auth (Scollega l'utente)
                authRepository.signOut()

                // 6. Fine del caricamento e navigazione alla schermata di Login/Onboarding
                _isLoading.value = false
                onComplete()

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Errore critico durante la cancellazione dei dati: ${e.message}")
                _isLoading.value = false
                // Opzionale: mostra un messaggio di errore all'utente tramite uno stato errorMessage
            }
        }
    }

}

