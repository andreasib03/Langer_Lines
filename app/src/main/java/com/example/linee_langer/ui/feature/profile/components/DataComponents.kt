package com.example.linee_langer.ui.feature.profile.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.example.linee_langer.R
import androidx.core.net.toUri
import com.example.linee_langer.ui.feature.profile.ProfileViewModel
import com.example.linee_langer.ui.theme.Dimens

@Composable
fun DataHeaderSection() {
    Column(modifier = Modifier.padding(vertical = Dimens.Small)) {
        Text(
            text = stringResource(R.string.information),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.description_information),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DataManagementCard(
    onExportData: () -> Unit,
    onDeleteAll: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.Large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(Dimens.Standard)) {
            // Export
            SettingsRow(
                title = stringResource(R.string.export),
                subtitle = stringResource(R.string.download_export),
                icon = R.drawable.ic_save, // download icon
                onClick = onExportData
            )

            Spacer(modifier = Modifier.height(Dimens.Standard))

            // Delete everything
            SettingsRow(
                title = stringResource(R.string.delete),
                subtitle = stringResource(R.string.description_delete),
                icon = R.drawable.ic_trash, // delete icon
                color = MaterialTheme.colorScheme.error,
                onClick = onDeleteAll
            )
        }
    }
}

@Composable
fun ConfirmDeleteAllChangeDialog(
    profileViewModel: ProfileViewModel,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit    // chiamato dopo verifica + conferma
) {
    val context = LocalContext.current
    val isGoogleUser = profileViewModel.isGoogleUser
    val isLoading by profileViewModel.isLoading.collectAsState()

    var isIdentityVerified by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (!isIdentityVerified) {
        // ── STEP 1: verifica identità ─────────────────────────────────────
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.verify_identity)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.Standard)) {
                    Text(stringResource(R.string.elimination_data))

                    if (!isGoogleUser) {
                        // Utente email/password → campo password
                        Spacer(Modifier.height(Dimens.Small))
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null   // resetta errore ad ogni modifica
                            },
                            label = { Text(stringResource(R.string.password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = errorMessage != null,
                            supportingText = errorMessage?.let { err ->
                                { Text(err, color = MaterialTheme.colorScheme.error) }
                            }
                        )
                    } else {
                        // Utente Google → spiegazione
                        Text(
                            text = stringResource(R.string.delete_account_google_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                // Verifica password
                Button(
                    onClick = {
                        if(!isGoogleUser){
                            profileViewModel.verifyPasswordOnly(
                                password = password,
                                onSuccess = { isIdentityVerified = true },
                                onError = { msg -> errorMessage = msg }
                            )
                        }  else {
                            profileViewModel.verifyGoogleOnly(
                                context = context,
                                onSuccess = { isIdentityVerified = true },
                                onError = { msg -> errorMessage = msg}
                            )
                        }

                    },
                    enabled = (!isGoogleUser && password.length >= 6 || isGoogleUser) && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.IconMedium),
                            strokeWidth = Dimens.BorderThin,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            if (isGoogleUser)
                                stringResource(R.string.delete_account_confirm_google)
                            else
                                stringResource(R.string.verify)
                        )
                    }
                }

            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.email_undo))
                }
            }
        )

    } else {
        // ── STEP 2: conferma distruttiva ──────────────────────────────────
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.are_you_sure),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.subtitle_are_you_sure))
            },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.executeAccountDeletion(
                            onComplete = onDeleted,
                            onError = { /* gestire se necessario */ }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.IconMedium),
                            strokeWidth = Dimens.BorderThin,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.delete_account_final))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.email_undo))
                }
            }
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: Int,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Dimens.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = "",
            tint = color,
            modifier = Modifier.size(Dimens.XLarge)
        )
        Spacer(modifier = Modifier.width(Dimens.Standard))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color)
            Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PrivacyPolicyCard(){
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showAdvertisationDialog by remember { mutableStateOf(false) }

    if (showPriorityDialog) {
        InfoDetailDialog(
            title = stringResource(R.string.priority),
            text = stringResource(R.string.priority_content),
            onDismiss = { showPriorityDialog = false }
        )
    }

    if (showAdvertisationDialog) {
        InfoDetailDialog(
            title = stringResource(R.string.advertisation),
            text = stringResource(R.string.advertisation_content),
            onDismiss = { showAdvertisationDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.Large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(Dimens.Large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Security Icon — centrata
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.IconXLarge)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shield),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(Dimens.Medium)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Standard))

            // Priority (sinistra) e Advertisation (destra)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = stringResource(R.string.priority),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showPriorityDialog = true }
                )

                Spacer(modifier = Modifier.width(Dimens.Standard))

                Text(
                    text = stringResource(R.string.advertisation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showAdvertisationDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Standard))

            // Bottone per i dettagli — centrato
            val context = LocalContext.current
            val privacyUrl = "https://andreasib03.github.io/Langer_Lines/"
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, privacyUrl.toUri())
                    context.startActivity(intent)
                }
            ) {
                Text(
                    stringResource(R.string.complete_privacy),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Dialog generico per mostrare il contenuto esteso di una voce della PrivacyPolicyCard. */
@Composable
private fun InfoDetailDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = text) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.understand))
            }
        },
        shape = RoundedCornerShape(Dimens.XLarge),
        containerColor = MaterialTheme.colorScheme.surface
    )
}