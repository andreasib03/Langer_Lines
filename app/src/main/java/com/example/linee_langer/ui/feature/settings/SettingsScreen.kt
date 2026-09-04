package com.example.linee_langer.ui.feature.settings

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.linee_langer.ui.shared.utils.VersionFooter
import com.example.linee_langer.ui.shared.utils.launchSupportEmail
import com.example.linee_langer.ui.feature.profile.ProfileViewModel
import com.example.linee_langer.ui.feature.settings.components.LangerVideoDialog
import com.example.linee_langer.ui.feature.settings.components.SettingsItem
import com.example.linee_langer.ui.feature.settings.components.SettingsSection
import com.example.linee_langer.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel,
    notificationViewModel: NotificationViewModel,
    onNavigateToData: () -> Unit = {},
    onLogoutSuccess: () -> Unit,
    onNavigateToTutorial:() -> Unit,
    onNavigateToFaq: () -> Unit = {}
) {

    val currentLocale by settingsViewModel.currentLocale.collectAsState()

    var showInfoDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false)}

    val recoveryState by settingsViewModel.recoveryState.collectAsState()
    var pendingLocale by remember { mutableStateOf<SupportedLocale?>(null) }
    val isChangingLocale = pendingLocale != null

    LaunchedEffect(pendingLocale) {
        val locale = pendingLocale ?: return@LaunchedEffect
        delay(200.milliseconds) // lascia disegnare almeno un frame dell'overlay prima della ricreazione
        settingsViewModel.setLocale(locale)
    }

    val context = LocalContext.current
    val isDark by settingsViewModel.isDarkMode.collectAsState()

    val autoCleanEnabled by settingsViewModel.isAutoCleanEnabled.collectAsState()

    val syncState by settingsViewModel.syncState.collectAsState()

    val recoveryScanStartedMessage = stringResource(R.string.recovery_scan_started)
    val recoveryPermissionDeniedMessage = stringResource(R.string.recovery_permission_denied)
    val cacheCleanStartedMessage = stringResource(R.string.cache_clean_started)

    val scanCompleted = stringResource(R.string.recovery_success_empty)
    val errorImageRecovery = stringResource(R.string.error_image_recovery)

    val cacheCleaningError = stringResource(R.string.error_cache_cleaning)
    val cacheCleaningSuccess = stringResource(R.string.success_cache_cleaning)

    val syncSuccessMessage = stringResource(R.string.sync_done)
    val syncErrorMessage = stringResource(R.string.error_generic)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncState) {
        when (syncState) {
            is SettingsViewModel.SyncState.Success -> {
                Toast.makeText(context, syncSuccessMessage, Toast.LENGTH_SHORT).show()
                settingsViewModel.resetSyncState()
            }
            is SettingsViewModel.SyncState.Error -> {
                Toast.makeText(context, syncErrorMessage, Toast.LENGTH_SHORT).show()
                settingsViewModel.resetSyncState()
            }
            else -> {}
        }
    }

    LaunchedEffect(recoveryState) {
        when (val state = recoveryState) {
            is SettingsViewModel.RecoveryState.Success -> {
                val message = if (state.count > 0) {
                    context.getString(R.string.recovery_success_found, state.count)
                } else {
                    scanCompleted
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                settingsViewModel.resetRecoveryState()
            }
            is SettingsViewModel.RecoveryState.Error -> {
                Toast.makeText(context,errorImageRecovery , Toast.LENGTH_SHORT).show()
                settingsViewModel.resetRecoveryState()
            }
            else -> {}
        }
    }

    val cacheCleanState by settingsViewModel.cacheCleanState.collectAsState()

    LaunchedEffect(cacheCleanState) {
        when (cacheCleanState) {
            is SettingsViewModel.CacheCleanState.Success -> {
                Toast.makeText(context, cacheCleaningSuccess, Toast.LENGTH_SHORT).show()
                settingsViewModel.resetCacheCleanState()
            }
            is SettingsViewModel.CacheCleanState.Error -> {
                Toast.makeText(context, cacheCleaningError , Toast.LENGTH_SHORT).show()
                settingsViewModel.resetCacheCleanState()
            }
            else -> {}
        }
    }


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
            Toast.makeText(context, recoveryScanStartedMessage, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, recoveryPermissionDeniedMessage, Toast.LENGTH_LONG).show()
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                            onClick = onNavigateToData
                        )
                    }
                }

                item {
                    SettingsSection(title = stringResource(R.string.settings_section_language)) {
                        SupportedLocale.entries.forEach { locale ->
                            LanguageItem(
                                locale = locale,
                                selected = locale == currentLocale,
                                onSelect = {
                                    if (!isChangingLocale && locale != currentLocale) {
                                        pendingLocale = locale
                                    }
                                }
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
                                Toast.makeText(context, cacheCleanStartedMessage, Toast.LENGTH_SHORT).show()
                                settingsViewModel.clearCache()
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
                            title = stringResource(R.string.settings_sync_cloud),
                            subtitle = stringResource(R.string.settings_sync_cloud_desc),
                            icon = R.drawable.ic_save, // Usa un'icona di sync o cloud se disponibile
                            onClick = {
                                settingsViewModel.triggerSync()
                                Toast.makeText(context, context.getString(R.string.sync_started), Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Dimens.Standard),
                            thickness = Dimens.Thickness
                        )
                        SettingsItem(
                            title = stringResource(R.string.settings_recovery_image),
                            subtitle = stringResource(R.string.settings_recovery_image_desc),
                            icon = R.drawable.ic_retrieve_image,
                            onClick = { launcher.launch(permissionsToRequest) }
                        )
                    }
                }

                // --- SECTION SUPPORT ---
                item {
                    SettingsSection(title = stringResource(R.string.faq)) {
                        SettingsItem(
                            title = stringResource(R.string.faq_title),
                            icon = R.drawable.ic_faq,
                            onClick = onNavigateToFaq
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Dimens.Standard),
                            thickness = Dimens.Thickness
                        )
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
                            icon = R.drawable.ic_video,
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
                            Icon(
                                tint = MaterialTheme.colorScheme.onSurface,
                                painter = painterResource(R.drawable.ic_logout),
                                contentDescription = ""
                            )
                            Spacer(Modifier.width(Dimens.Small))
                            Text(text = stringResource(R.string.settings_logout), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // --- FOOTER ---
                item {
                    VersionFooter(version = stringResource(R.string.app_version))
                }
            }

            if (isChangingLocale) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { /* assorbe i tap, blocca l'interazione sotto */ },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(Dimens.Standard))
                        Text(
                            text = stringResource(R.string.changing_language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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