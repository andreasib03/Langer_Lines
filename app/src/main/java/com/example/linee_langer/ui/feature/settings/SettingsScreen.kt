package com.example.linee_langer.ui.feature.settings

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.linee_langer.ui.shared.components.LangerScaffold
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import com.example.linee_langer.R
import com.example.linee_langer.ui.feature.home.components.LangerInfoDialog
import com.example.linee_langer.ui.shared.components.SupportDialog
import com.example.linee_langer.ui.theme.locale.SupportedLocale
import com.example.linee_langer.ui.shared.utils.ChevronRightIcon
import com.example.linee_langer.ui.shared.utils.VersionFooter
import com.example.linee_langer.ui.shared.utils.launchSupportEmail
import com.example.linee_langer.ui.feature.profile.ProfileViewModel
import com.example.linee_langer.ui.feature.settings.components.LangerVideoDialog
import com.example.linee_langer.ui.feature.settings.components.SettingsItem
import com.example.linee_langer.ui.feature.settings.components.SettingsSection
import com.example.linee_langer.ui.theme.Dimens
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel,
    notificationViewModel: NotificationViewModel,
    onNavigateToData: () -> Unit = {},
    onLogoutSuccess: () -> Unit,
    onNavigateToTutorial:() -> Unit
) {

    val currentLocale by settingsViewModel.currentLocale.collectAsState()

    var showInfoDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false)}

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

    if(showVideoDialog){
        LangerVideoDialog(
            onDismiss = { showVideoDialog = false }
        )
    }


    LangerScaffold(
        title = stringResource(R.string.settings),
        notificationViewModel = notificationViewModel,
        canNavigateBack = false,
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Colore di sfondo uniforme
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = Dimens.XXLarge) // Spazio extra in fondo
        ) {
            // 1. SEZIONE ACCOUNT (Nuova)
            item {
                SettingsSection(title = stringResource(R.string.profile_managing)) {
                    SettingsItem(
                        title = stringResource(R.string.profile_title),
                        subtitle = stringResource(R.string.adjust_data),
                        icon = R.drawable.ic_profile,
                        onClick = onNavigateToData,
                        trailing = { ChevronRightIcon() }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_section_language)) {
                    SupportedLocale.entries.forEach { locale ->
                        LanguageItem(
                            locale = locale,
                            selected = locale == currentLocale,
                            onSelect = { settingsViewModel.setLocale(locale) }
                        )
                    }
                }
            }

            // --- PREFERENCES SECTION ---
            item {
                SettingsSection(title = stringResource(R.string.preferences)) {
                    val themeDark = stringResource(R.string.dark_mode_activated)
                    val lightDark = stringResource(R.string.light_mode_activated)
                    SettingsItem(
                        title = stringResource(R.string.dark_mode),
                        icon = R.drawable.ic_dark_light,
                        trailing = {
                            Switch(
                                checked = isDark ?: isSystemInDarkTheme(),
                                onCheckedChange = { isChecked ->
                                    settingsViewModel.toggleTheme(isChecked)
                                    val message = if (isChecked) {
                                        themeDark
                                    } else {
                                        lightDark
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Dimens.Standard),
                        thickness = Dimens.Thickness
                    )
                    SettingsItem(
                        title = stringResource(R.string.settings_clean),
                        subtitle = stringResource(R.string.settings_auto_clean_description),
                        icon = R.drawable.ic_memory,
                        onClick = {
                            scope.launch {
                                settingsViewModel.clearCache() // Funzione suspend
                                Toast.makeText(context, "Pulizia avviata...", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    )
                }
            }

            // Dentro la LazyColumn della SettingsScreen
            item {
                SettingsSection(title = stringResource(R.string.settings_manutentation)) {
                    val automaticClean = stringResource(R.string.automatic_clean_activated)
                    val automaticNoClean = stringResource(R.string.automatic_clean_deactivated)
                    SettingsItem(
                        title = stringResource(R.string.settings_auto_clean),
                        subtitle = stringResource(R.string.settings_24h),
                        icon = R.drawable.ic_broom, // Usa un'icona tipo scopa o orologio
                        trailing = {
                            // Possiamo usare uno switch per attivare/disattivare il WorkManager
                            Switch(
                                checked = autoCleanEnabled,
                                onCheckedChange = { isChecked ->
                                    settingsViewModel.toggleAutoClean(isChecked)

                                    val message = if (isChecked) {
                                        automaticClean
                                    } else {
                                        automaticNoClean
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_recovery)) {
                    SettingsItem(
                        title = stringResource(R.string.settings_recovery_image),
                        subtitle = stringResource(R.string.settings_recovery_image_desc),
                        icon = R.drawable.ic_retrieve_image,
                        onClick = {
                            launcher.launch(permissionsToRequest)
                            settingsViewModel.triggerImageRecovery()
                            Toast.makeText(context, "Scansione avviata...", Toast.LENGTH_SHORT)
                                .show()
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
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Dimens.Standard),
                        thickness = Dimens.Thickness
                    )
                    SettingsItem(
                        title = stringResource(R.string.tutorial_video),
                        icon = R.drawable.ic_email,
                        onClick = { onNavigateToTutorial() }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Dimens.Standard),
                        thickness = Dimens.Thickness
                    )
                    SettingsItem(
                        title = stringResource(R.string.contact),
                        icon = R.drawable.ic_email,
                        onClick = { showEmailDialog = true }
                    )

                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimens.XLarge))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.Standard),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Medium)
                ) {
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
                        Spacer(Modifier.width(Dimens.Small))
                        Text(stringResource(R.string.settings_logout))
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

@Composable
private fun LanguageItem(
    locale: SupportedLocale,
    selected: Boolean,
    onSelect: () -> Unit
) {
    SettingsItem(
        title = "${locale.flagEmoji}  ${stringResource(locale.labelRes)}",
        onClick = onSelect,
        trailing = {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
