package com.example.linee_langer.ui.feature.onboarding.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.linee_langer.ui.feature.auth.AuthUiState
import com.example.linee_langer.ui.feature.auth.AuthViewModel

@Composable
fun GoogleAuthHandler(
    authViewModel: AuthViewModel,
    onExistingUser: () -> Unit,
    onNewUser: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState = authViewModel.uiState
    var hasNavigated by remember { mutableStateOf(false) }

    // Lancia il Sign-In Google appena il composable entra in composizione.
    // LaunchedEffect(Unit) garantisce che venga eseguito una sola volta.
    LaunchedEffect(Unit) {
        authViewModel.handleGoogleSignIn(context)
    }

    // Reagisce ai cambi di stato del ViewModel
    LaunchedEffect(uiState) {
        if (hasNavigated) return@LaunchedEffect

        when (uiState) {
            is AuthUiState.Success -> {
                if (uiState.isExistingUser) {
                    hasNavigated = true
                    // Utente già registrato → vai direttamente a Home
                    onExistingUser()
                } else {
                    // Nuovo utente Google → raccolta dati anagrafici obbligatoria
                    hasNavigated = true
                    onNewUser()
                }
            }
            is AuthUiState.Error -> {
                // Errore o annullamento dialog Google → torna a Welcome
                // L'errore è già in uiState — se vuoi mostrarlo aggiungi
                // uno Snackbar qui prima di chiamare onBack()
                hasNavigated = true
                onBack()
            }
            else -> Unit
        }
    }

    // UI minima: solo loader centrato mentre il sistema elabora il Sign-In
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}