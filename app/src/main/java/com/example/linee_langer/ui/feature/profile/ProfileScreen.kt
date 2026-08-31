package com.example.linee_langer.ui.feature.profile

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.linee_langer.R
import com.example.linee_langer.ui.shared.components.LangerScaffold
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.linee_langer.ui.feature.home.components.AdviceCategoryCard
import com.example.linee_langer.ui.feature.home.components.AdviceHeader
import com.example.linee_langer.ui.feature.profile.components.ConfirmDeleteAllChangeDialog
import com.example.linee_langer.ui.feature.profile.components.DataHeaderSection
import com.example.linee_langer.ui.feature.profile.components.DataManagementCard
import com.example.linee_langer.ui.feature.home.components.MedicalDisclaimerCard
import com.example.linee_langer.ui.feature.profile.components.PrivacyPolicyCard
import com.example.linee_langer.ui.feature.history.HistoryViewModel
import com.example.linee_langer.ui.feature.profile.components.EditableUserInfoCard
import com.example.linee_langer.ui.feature.profile.components.MainActionCard
import com.example.linee_langer.ui.feature.profile.components.ProfileHeader
import com.example.linee_langer.ui.feature.profile.components.ProfileMenuItem
import com.example.linee_langer.ui.feature.profile.components.StatCard
import com.example.linee_langer.ui.feature.profile.components.getSkinTypeDisplayName
import com.example.linee_langer.ui.shared.utils.restartApp
import com.example.linee_langer.ui.theme.Dimens
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    historyViewModel: HistoryViewModel,
    profileViewModel: ProfileViewModel,
    notificationViewModel: NotificationViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToData: () -> Unit,
    onNavigateToAdvice: () -> Unit
){

    val context = LocalContext.current
    val userProfile by profileViewModel.userProfile.collectAsState()
    val count by historyViewModel.totalAnalyses.collectAsState()
    val skinTypeSaved by profileViewModel.userSkinType.collectAsState()

    val profileImageUri by profileViewModel.profileImageUri.collectAsState()

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                // Notifichiamo il ViewModel per salvare l'URI localmente (Room/DataStore) o caricarlo
                profileViewModel.updateProfileImage(it)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { sourceUri ->
            val destinationFile =
                File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
            val destinationUri = Uri.fromFile(destinationFile)

            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(300, 300) // 300x300 è perfetto per un avatar leggero

            val options = UCrop.Options().apply {
                setCircleDimmedLayer(true) // Maschera circolare di anteprima
                setShowCropGrid(false)
                setHideBottomControls(false)
            }
            uCrop.withOptions(options)

            cropLauncher.launch(uCrop.getIntent(context))
        }
    }


    LangerScaffold(
        title = stringResource(R.string.nav_profile),
        notificationViewModel = notificationViewModel,
        canNavigateBack = false // Nella Home è false
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {

                ProfileHeader(
                    name = userProfile?.name ?: stringResource(R.string.user),
                    email = userProfile?.email ?: stringResource(R.string.unspecified_email),
                    onImageClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    profileImageUri = profileImageUri

                )

            }
            // 2. Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Medium)
                ) {
                    StatCard(
                        label = when (count) {
                            0 -> {
                                stringResource(R.string.home_no_analysis)
                            }

                            1 -> {
                                stringResource(R.string.one_analysis)
                            }

                            else -> {
                                stringResource(R.string.more_analysis)
                            }
                        },
                        value = count.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.profile_skin_type),
                        value = getSkinTypeDisplayName(skinTypeSaved),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.XLarge))
            }

            // 3. The "Main Action" Card (Modern Button replacement)
            item {
                MainActionCard(onClick = onNavigateToCamera)
                Spacer(modifier = Modifier.height(Dimens.XLarge))
            }


            // 4. Menu Options
            item {
                Column(modifier = Modifier.padding(horizontal = Dimens.Standard), verticalArrangement = Arrangement.spacedBy(
                    Dimens.Small)) {
                    ProfileMenuItem(
                        title = stringResource(R.string.history),
                        icon = R.drawable.ic_analysis,
                        onClick = onNavigateToHistory
                    )
                    ProfileMenuItem(
                        title = stringResource(R.string.routine),
                        icon = R.drawable.ic_routine,
                        onClick = onNavigateToAdvice
                    )
                    ProfileMenuItem(
                        title = stringResource(R.string.settings_section_data),
                        icon = R.drawable.ic_privacy_data,
                        onClick = onNavigateToData
                    )
                }
            }


        }
    }
}










@Composable
fun DataScreen(
    profileViewModel: ProfileViewModel,
    notificationViewModel: NotificationViewModel,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val userData by profileViewModel.userProfile.collectAsState()
    val context = LocalContext.current
    val history by profileViewModel.fullHistory.collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isVerified by profileViewModel.isEmailVerified.collectAsState()

    // FORZA IL CARICAMENTO DEI DATI OGNI VOLTA CHE LA SCHERMATA VIENE APERTA
    LaunchedEffect(Unit) {
        profileViewModel.loadUserProfile()
    }

    val isGoogleUser = profileViewModel.isGoogleUser

    if(showDeleteDialog){
        ConfirmDeleteAllChangeDialog(
            profileViewModel = profileViewModel,
            onDeleted = {
                showDeleteDialog = false
                profileViewModel.clearAllData { restartApp(context) }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    LangerScaffold(
        title = stringResource(R.string.privacy),
        notificationViewModel = notificationViewModel,
        canNavigateBack = true,
        onBackClick = onBack,
        snackbarHostState = snackbarHostState
    ) { innerPadding ->

        val currentProfile = userData
        if(currentProfile == null){
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colorScheme.primary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .background(colorScheme.surface),
                contentPadding = PaddingValues(Dimens.Standard),
                verticalArrangement = Arrangement.spacedBy(Dimens.Standard)
            ) {
                item {
                    DataHeaderSection()
                }
                item {
                    val profileImage = stringResource(R.string.profile_image_updated)
                    val profileNoImage = stringResource(R.string.profile_image_no_updated)
                    val emailChangeFailedMessage = stringResource(R.string.error_wrong_password)
                    EditableUserInfoCard(
                        context = context,
                        name = currentProfile.name,
                        email = currentProfile.email,
                        skinType = getSkinTypeDisplayName(currentProfile.skinType),
                        isEmailEditable = !isGoogleUser,
                        isEmailVerified = isVerified,
                        onVerifyClick = {
                            profileViewModel.sendVerification { success ->
                                if (success) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = profileImage,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = profileNoImage,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        },
                    ) { newEmail, password ->
                        profileViewModel.updateEmail(
                            newEmail = newEmail,
                            password = password,
                            onResult = { success ->
                                if (!success) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = emailChangeFailedMessage,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                item {
                    val skinTypeDisplayName = getSkinTypeDisplayName(currentProfile.skinType)
                    DataManagementCard(
                        onExportData = {
                            if(!profileViewModel.isExporting){
                                profileViewModel.generateReport(context, currentProfile, history, skinTypeDisplayName)
                            }
                        },
                        onDeleteAll = {
                            showDeleteDialog = true

                        }
                    )
                }

                item {
                    PrivacyPolicyCard()
                }
            }
        }
    }
}

@Composable
fun AdviceScreen(
    notificationViewModel: NotificationViewModel,
    onBack: () -> Unit
) {
    LangerScaffold(
        title = stringResource(R.string.advice),
        notificationViewModel = notificationViewModel,
        canNavigateBack = true,
        onBackClick = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Dimens.Standard),
            verticalArrangement = Arrangement.spacedBy(Dimens.Large)
        ) {
            // Introduction section
            item {
                AdviceHeader()
            }

            // advice list
            item {
                AdviceCategoryCard(
                    title = stringResource(R.string.routine_massage),
                    description = stringResource(R.string.desc_massage),
                    icon = R.drawable.ic_star, // Icon right
                    color = colorScheme.primaryContainer
                )
            }

            item {
                AdviceCategoryCard(
                    title = stringResource(R.string.hydratation),
                    description = stringResource(R.string.desc_hydratation),
                    icon = R.drawable.ic_profile,
                    color = colorScheme.secondaryContainer
                )
            }

            item {
                AdviceCategoryCard(
                    title = stringResource(R.string.protection),
                    description = stringResource(R.string.desc_protection),
                    icon = R.drawable.ic_profile,
                    color = colorScheme.secondaryContainer
                )
            }
            item {
                AdviceCategoryCard(
                    title = stringResource(R.string.lines_massage),
                    description = stringResource(R.string.lines_massage_desc),
                    icon = R.drawable.ic_profile,
                    color = colorScheme.secondaryContainer
                )
            }

            item {
                AdviceCategoryCard(
                    title = stringResource(R.string.detergency),
                    description = stringResource(R.string.detergency_desc),
                    icon = R.drawable.ic_profile,
                    color = colorScheme.secondaryContainer
                )
            }

            item {
                AdviceCategoryCard(
                    title = stringResource(R.string.eyeliner),
                    description = stringResource(R.string.eyeliner_desc),
                    icon = R.drawable.ic_profile,
                    color = colorScheme.secondaryContainer
                )
            }

            item {
                AdviceCategoryCard(
                    title = stringResource(R.string.water),
                    description = stringResource(R.string.water_desc),
                    icon = R.drawable.ic_profile,
                    color = colorScheme.secondaryContainer
                )
            }

            item {
                AdviceCategoryCard(
                    title = stringResource(R.string.massage),
                    description = stringResource(R.string.massage_desc),
                    icon = R.drawable.ic_profile,
                    color = colorScheme.secondaryContainer
                )
            }


            item {
                // Disclaimer Medico (Sempre important in app di analys)
                MedicalDisclaimerCard()
            }
        }
    }
}


