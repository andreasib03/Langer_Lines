package com.example.linee_langer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.linee_langer.ui.viewModels.AuthViewModel
import com.example.linee_langer.R
import com.example.linee_langer.ui.viewModels.AuthUiState

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    initialEmail: String,
    onEmailChanged: (String) -> Unit,
    onAuthSuccess: (isExistingUser: Boolean) -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val uiState = authViewModel.uiState

    val isLoading = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message

    // Sincronizza lo stato locale dell'AuthViewModel con l'email dell'onboarding
    LaunchedEffect(initialEmail) {
        if (authViewModel.email.isEmpty()) {
            authViewModel.email = initialEmail
        }
    }
    LaunchedEffect(uiState){
        if (uiState is AuthUiState.Success) {
            onAuthSuccess(uiState.isExistingUser)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isLoading) 0.3f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isLoginMode) stringResource(R.string.login_langer) else stringResource(R.string.create_account),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (isLoginMode) stringResource(R.string.credentials) else stringResource(R.string.register),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = authViewModel.email,
                onValueChange = {
                    authViewModel.email = it
                    onEmailChanged(it) // Aggiorna l'onboarding ViewModel
                },
                label = { Text(stringResource(R.string.email_)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = authViewModel.password,
                onValueChange = { authViewModel.password = it },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))



            //email password autentication button
            Button(
                onClick = {
                    authViewModel.handleAuth(isLoginMode)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                Text(if (isLoginMode) stringResource(R.string.login_start) else stringResource(R.string.register_save))
            }



            Button(
                onClick = {
                    authViewModel.handleGoogleSignIn(context = context) // Va alla FinalPage se ha successo)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.login_google))
            }

            TextButton(
                onClick = { isLoginMode = !isLoginMode },
                enabled = !isLoading
            ) {
                Text(text = if (isLoginMode) stringResource(R.string.noaccount) else stringResource(R.string.have_account))
            }
        }

        // Mostra il caricamento in primo piano senza rompere il layout della colonna
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}