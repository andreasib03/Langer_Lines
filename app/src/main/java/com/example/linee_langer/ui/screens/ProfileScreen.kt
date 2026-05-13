package com.example.linee_langer.ui.screens

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.linee_langer.R
import com.example.linee_langer.dao.AnalysisWithLines
import com.example.linee_langer.ui.components.LangerScaffold
import com.example.linee_langer.ui.viewModels.NotificationViewModel
import com.example.linee_langer.ui.components.LangerOverlay
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.linee_langer.ui.components.AdviceCategoryCard
import com.example.linee_langer.ui.components.AdviceHeader
import com.example.linee_langer.ui.components.ConfirmDeleteAllChangeDialog
import com.example.linee_langer.ui.components.DataHeaderSection
import com.example.linee_langer.ui.components.DataManagementCard
import com.example.linee_langer.ui.components.EditableUserInfoCard
import com.example.linee_langer.ui.components.EmptyHistoryPlaceholder
import com.example.linee_langer.ui.components.MainActionCard
import com.example.linee_langer.ui.components.MedicalDisclaimerCard
import com.example.linee_langer.ui.components.PrivacyPolicyCard
import com.example.linee_langer.ui.components.ProfileHeader
import com.example.linee_langer.ui.components.ProfileMenuItem
import com.example.linee_langer.ui.components.StatCard
import com.example.linee_langer.ui.components.SwipeableHistoryCard
import com.example.linee_langer.ui.components.getSkinTypeDisplayName
import com.example.linee_langer.ui.utils.*
import com.example.linee_langer.ui.viewModels.CameraAnalysisViewModel
import com.example.linee_langer.ui.viewModels.HistoryViewModel
import com.example.linee_langer.ui.viewModels.ProfileViewModel
import com.yalantis.ucrop.UCrop
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
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
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
                File(context.cacheDir, "profile_${System.currentTimeMillis()}.jpg")
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
        title = stringResource(R.string.profile),
        notificationViewModel = notificationViewModel,
        canNavigateBack = false // Nella Home è false
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {

                ProfileHeader(
                    name = userProfile?.name ?: stringResource(R.string.user),
                    email = userProfile?.email ?: stringResource(R.string.no_email),
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = when (count) {
                            0 -> {
                                stringResource(R.string.noanalysis)
                            }
                            1 -> {
                                "Analisi trovata"
                            }
                            else -> {
                                "Analisi trovate"
                            }
                        },
                        value = count.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Tipo di pelle",
                        value = getSkinTypeDisplayName(skinTypeSaved),
                        modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. The "Main Action" Card (Modern Button replacement)
            item {
                MainActionCard(onClick = onNavigateToCamera)
                Spacer(modifier = Modifier.height(24.dp))
            }


            // 4. Menu Options
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        title = stringResource(R.string.privacy),
                        icon = R.drawable.ic_privacy_data,
                        onClick = onNavigateToData
                    )
                }
            }


        }
    }
}




@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    notificationViewModel: NotificationViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit
) {
    // Suppose che il ViewModel expose: val history by repository.getAllAnalyses().collectAsState(initial = emptyList())
    val history by historyViewModel.history.collectAsState()

    val snackbarHostState = remember { SnackbarHostState()}

    LangerScaffold(
        title = stringResource(R.string.analysis),
        notificationViewModel = notificationViewModel,
        canNavigateBack = true,
        onBackClick = onBack,
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        if (history.isEmpty()) {
            EmptyHistoryPlaceholder()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = history,
                    key = { item -> item.analysis.id }
                ) { item ->

                    SwipeableHistoryCard(
                        analysis = item,
                        onDelete = { analysisToDelete ->
                            historyViewModel.deleteAnalysis(analysisToDelete, snackbarHostState)
                        }, onClick = {
                            onNavigateToDetail(item.analysis.id)
                        }
                    )
                }
            }
        }
    }
}






@Composable
fun AnalysisDetailScreen(
    analysisId: Long,
    notificationViewModel: NotificationViewModel,
    analysisViewModel: CameraAnalysisViewModel,
    onBack: () -> Unit
) {
    // Retrieve analysis dal database with ViewModel
    val analysis by analysisViewModel.getAnalysisById(analysisId).collectAsState(initial = null)

    LangerScaffold(
        title = stringResource(R.string.detailed_analysis),
        canNavigateBack = true,
        notificationViewModel = notificationViewModel,
        onBackClick = onBack
    ) { innerPadding ->
        analysis?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Black)
            ) {
                // 1. Visualization con Overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                ) {
                    // Saved image
                    AsyncImage(
                        model = data.analysis.imagePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay delle linee saved
                    // animation
                    LangerOverlay(
                        lines = data.lines.map { it.toDomainModel() }, // Convert Entity -> Model
                        isVisible = true
                    )
                }

                // 2. Info Panel (Modern Sheet)
                AnalysisInfoPanel(data)
            }
        }
    }
}



@Composable
fun AnalysisInfoPanel(data: AnalysisWithLines) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = data.analysis.bodyPartId.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = data.analysis.date.toDateString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Badge intensity media
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Tensione: Alta", // Logic sulla media delle linee
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card dei dynamic advice
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.text),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
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
            onConfirm = {
                showDeleteDialog = false
                profileViewModel.clearAllData(userData) { restartApp(context) }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    LangerScaffold(
        title = stringResource(R.string.privacy),
        notificationViewModel = notificationViewModel,
        canNavigateBack = true,
        onBackClick = onBack
    ) { innerPadding ->

        val currentProfile = userData
        if(currentProfile == null){
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
            DataHeaderSection()
        }
            item {
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
                                Toast.makeText(context, "Email di verifica inviata!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Errore durante l'invio. Riprova più tardi.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                ) {
                    profileViewModel.updateEmail(it)
                }
            }

            item {
                DataManagementCard(
                    onExportData = {
                        if(!profileViewModel.isExporting){
                            profileViewModel.generateReport(context, currentProfile, history, currentProfile.skinType)
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }

            item {
                AdviceCategoryCard(
                    title = stringResource(R.string.hydratation),
                    description = stringResource(R.string.desc_hydratation),
                    icon = R.drawable.ic_profile,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }

            item {
                // Disclaimer Medico (Sempre important in app di analys)
                MedicalDisclaimerCard()
            }
        }
    }
}


