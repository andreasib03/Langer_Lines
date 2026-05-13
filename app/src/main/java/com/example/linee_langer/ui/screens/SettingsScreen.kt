package com.example.linee_langer.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.linee_langer.ui.components.LangerScaffold
import com.example.linee_langer.ui.viewModels.NotificationViewModel
import com.example.linee_langer.ui.viewModels.SettingsViewModel
import com.example.linee_langer.R
import com.example.linee_langer.ui.components.LangerInfoDialog
import com.example.linee_langer.ui.components.SettingsItem
import com.example.linee_langer.ui.components.SettingsSection
import com.example.linee_langer.ui.components.SupportDialog
import com.example.linee_langer.ui.utils.ChevronRightIcon
import com.example.linee_langer.ui.utils.VersionFooter
import com.example.linee_langer.ui.utils.launchSupportEmail
import com.example.linee_langer.ui.viewModels.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel,
    notificationViewModel: NotificationViewModel,
    onNavigateToData: () -> Unit = {},
    onLogoutSuccess: () -> Unit
) {

    var showInfoDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isDark by settingsViewModel.isDarkMode.collectAsState()

    val autoCleanEnabled by settingsViewModel.isAutoCleanEnabled.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Controlliamo se almeno uno dei permessi (totale o parziale) è stato concesso
        val isGranted = permissions.entries.any { it.value }
        if (isGranted) {
            settingsViewModel.triggerImageRecovery()
            Toast.makeText(context, "Scansione avviata...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permesso negato. Impossibile recuperare immagini.", Toast.LENGTH_LONG).show()
        }
    }

    if(showEmailDialog){
        SupportDialog(
            onDismiss = { showEmailDialog = false},
            onSend = { subject, message ->
                showEmailDialog = false
                launchSupportEmail(context, subject, message)
            }
        )
    }
    if(showInfoDialog){
        LangerInfoDialog(
            onDismiss = { showInfoDialog = false }
        )
    }


    LangerScaffold(
        title = "Settings",
        notificationViewModel = notificationViewModel,
        canNavigateBack = false,
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Colore di sfondo uniforme
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp) // Spazio extra in fondo
        ) {
            // 1. SEZIONE ACCOUNT (Nuova)
            item {
                SettingsSection(title = "Account") {
                    SettingsItem(
                        title = "Profilo",
                        subtitle = "Gestisci i tuoi dati e l'email",
                        icon = R.drawable.ic_profile,
                        onClick = onNavigateToData,
                        trailing = { ChevronRightIcon() }
                    )
                }
            }

            // --- PREFERENCES SECTION ---
            item {
                SettingsSection(title = stringResource(R.string.preferences)) {
                    SettingsItem(
                        title = stringResource(R.string.dark_mode),
                        icon = R.drawable.ic_dark_light,
                        trailing = {
                            Switch(
                                checked = isDark ?: isSystemInDarkTheme(),
                                onCheckedChange = { isChecked ->
                                    settingsViewModel.toggleTheme(isChecked)
                                    val message = if(isChecked){
                                        "Tema scuro attivato"
                                    } else {
                                        "Tema chiaro attivato"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SettingsItem(
                        title = "Pulizia Cache",
                        subtitle = "Libera spazio eliminando file temporanei",
                        icon = R.drawable.ic_memory,
                        onClick = {
                            scope.launch {
                                settingsViewModel.clearCache() // Funzione suspend
                                Toast.makeText(context, "Pulizia avviata...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Dentro la LazyColumn della SettingsScreen
            item {
                SettingsSection(title = "Manutenzione") {
                    SettingsItem(
                        title = "Pulizia automatica",
                        subtitle = "Rimuove i file temporanei ogni 24h",
                        icon = R.drawable.ic_broom, // Usa un'icona tipo scopa o orologio
                        trailing = {
                            // Possiamo usare uno switch per attivare/disattivare il WorkManager
                            Switch(
                                checked = autoCleanEnabled,
                                onCheckedChange = { isChecked ->
                                    settingsViewModel.toggleAutoClean(isChecked)

                                    val message = if (isChecked) {
                                        "Pulizia automatica attivata"
                                    } else {
                                        "Pulizia automatica disattivata"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Recupero") {
                    SettingsItem(
                        title = "Recupera immagini",
                        subtitle = "Ricollega le foto salvate nel telefono",
                        icon = R.drawable.ic_retrieve_image,
                        onClick = {
                            launcher.launch(permissionsToRequest)
                            settingsViewModel.triggerImageRecovery()
                            Toast.makeText(context, "Scansione avviata...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // --- SECTION SUPPORT ---
            item {
                SettingsSection(title = stringResource(R.string.faq)) {
                    SettingsItem(
                        title = stringResource(R.string.meaning_langer),
                        icon = R.drawable.ic_home,
                        onClick = { showInfoDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SettingsItem(
                        title = stringResource(R.string.contact),
                        icon = R.drawable.ic_email,
                        onClick = { showEmailDialog = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // OutlinedButton per azioni secondarie
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { /* Logica per resettare solo i tutorial */ }
                    ) {
                        Text("Ripristina Tutorial")
                    }

                    // Button con colore "Error" per il Logout
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = {
                            profileViewModel.signOut { onLogoutSuccess() }
                        }
                    ) {
                        Icon(painterResource(R.drawable.ic_logout), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }

            // --- FOOTER ---
            item {
                VersionFooter(version = "1.0.0 (BETA)")
            }
        }
    }
}
