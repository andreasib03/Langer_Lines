package com.example.linee_langer.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.linee_langer.ui.components.CardChoice
import com.example.linee_langer.R
import com.example.linee_langer.ui.components.OnboardingSkinTypeScreen
import com.example.linee_langer.ui.components.StyledTextField
import com.example.linee_langer.ui.interfacesUser.AppDimension
import com.example.linee_langer.ui.interfacesUser.AppTypography
import com.example.linee_langer.ui.viewModels.OnBoardingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val PAGECOUNT = 4
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun OnBoardingScreen(
    onBoardingViewModel: OnBoardingViewModel,
    onFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { PAGECOUNT })
    val context = LocalContext.current

    var showSuccessAnimation by remember { mutableStateOf(false)}

    val canProceed = when (pagerState.currentPage) {
        0 -> false
        1 -> onBoardingViewModel.name.isNotBlank() && onBoardingViewModel.eta.isNotBlank()
        2 -> onBoardingViewModel.selectedSkinType.isNotBlank() // Validation pelle
        3 -> onBoardingViewModel.selectedGoal != 0 // Validation goal
        else -> true
    }

    Scaffold(
        snackbarHost = { SnackbarHost (hostState = snackbarHostState)},
        bottomBar = {
            if (pagerState.currentPage > 0) {
                OnBoardingBottomBar(
                    pagerState = pagerState,
                    onNext = {
                        if (!canProceed) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.infos), // da vedere con context.getString() perchè così è solo id
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            if(pagerState.currentPage == PAGECOUNT - 1){
                                showSuccessAnimation = true
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }

                        }
                    },

                    onBack = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    isNextEnabled = canProceed
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            userScrollEnabled = canProceed && pagerState.currentPage > 0
        ) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (page) {
                    0 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        WelcomeHeader()

                        AuthScreen(
                            initialEmail = onBoardingViewModel.email,
                            onEmailChanged = { onBoardingViewModel.email = it },
                            onAuthSuccess = { isExistingUser ->
                                if (isExistingUser) {
                                    // Se l'utente esisteva già, saltiamo l'onboarding ed entriamo in Home
                                    showSuccessAnimation = true
                                } else {
                                    // Se è un nuovo utente, sblocchiamo il flusso e andiamo alla raccolta dati
                                    scope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                }
                            }
                        )
                    }

                    // STEP 1: Dati Personali
                    1 -> DataCollectionPage(
                        name = onBoardingViewModel.name,
                        onNameChange = { onBoardingViewModel.name = it },
                        eta = onBoardingViewModel.eta,
                        onEtaChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }

                            if(digitsOnly.isEmpty()){
                                onBoardingViewModel.eta = ""
                            } else if (digitsOnly.length <= 3) {
                                val ageValue = digitsOnly.toIntOrNull()
                                if(ageValue != null && ageValue <= 110){
                                    onBoardingViewModel.eta = digitsOnly
                                }

                            }
                        }
                    )

                    // STEP 2: Analisi Tipo di Pelle
                    2 -> OnboardingSkinTypeScreen(
                        selectedId = onBoardingViewModel.selectedSkinType,
                        onOptionSelected = { onBoardingViewModel.selectedSkinType = it }
                    )

                    // STEP 3: Scelta Obiettivi / Traguardo
                    3 -> CardChoicesPage(onBoardingViewModel.selectedGoal) {
                        onBoardingViewModel.selectedGoal = it
                    }

                }
            }
        }

        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface), // Forza lo sfondo solido su TUTTO lo schermo
                contentAlignment = Alignment.Center
            ) {
                FinalPage(
                    onAnimationFinished = {
                        onBoardingViewModel.finishOnBoarding(onFinished)
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Column(
        modifier = Modifier.padding(top = 40.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_settings), // insert logo
            contentDescription = null,
            modifier = Modifier.size(AppDimension.IconSuper),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(AppDimension.Large))
        Text(
            text = "Langer line application",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
private fun DataCollectionPage(
    name: String,
    onNameChange: (String) -> Unit,
    eta: String,
    onEtaChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimension.Large)
            .imePadding()
    ) {
        Text(
            text = stringResource(R.string.personal),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(AppDimension.Large))

        StyledTextField(
            value = name,
            onValueChange = onNameChange,
            label = stringResource(R.string.name_full),
            placeholder = stringResource(R.string.example_name)
        )

        Spacer(modifier = Modifier.height(AppDimension.Large))

        StyledTextField(
            value = eta,
            onValueChange = onEtaChange,
            label = "Età",
            placeholder = "Esempio 19"
        )

    }
}
@Composable
private fun CardChoicesPage(
    selectedOption: Int,
    onSelectedOptionChange: (Int) -> Unit
){

    Column( modifier = Modifier
        .fillMaxSize()
        .padding(AppDimension.Medium)){

        Text(
            stringResource(R.string.purpose),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
        )
        Spacer(modifier = Modifier.height(AppDimension.Large))

        CardChoice(
            title = stringResource(R.string.muscle),
            subtitle = stringResource(R.string.muscle_sub),
            description = stringResource(R.string.muscle_desc),
            icon = R.drawable.ic_star,
            isSelected = selectedOption == 1,
            onSelect = { onSelectedOptionChange(1) }
        )

        CardChoice(
            title = stringResource(R.string.cut),
            subtitle = stringResource(R.string.cut_sub),
            description = stringResource(R.string.cut_desc),
            icon = R.drawable.ic_star,
            isSelected = selectedOption == 2,
            onSelect = { onSelectedOptionChange(2) }
        )

    }
}


@Composable
private fun FinalPage(onAnimationFinished: () -> Unit) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_success)) // Se usi Lottie
    val composition by compositionResult

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    LaunchedEffect(progress) {
        if (progress == 1f) {
            // Un piccolissimo delay artificiale per non far scattare il cambio schermo in modo troppo brusco
            delay(500)
            onAnimationFinished()
        }
    }

    LaunchedEffect(compositionResult.isFailure) {
        if (compositionResult.isFailure) {
            delay(2000)
            onAnimationFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icona di successo o Animazione
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(250.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tutto pronto!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.finish_settings),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnBoardingBottomBar(
    pagerState: PagerState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isNextEnabled: Boolean
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimension.Large, vertical = AppDimension.VerticalPadding)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(modifier = Modifier.size(AppDimension.Huge)){
            if(pagerState.currentPage > 1){
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .border(AppDimension.Border, Color.Black, RoundedCornerShape(AppDimension.CornerShape))
                        .background(Color.White, RoundedCornerShape(AppDimension.CornerShape))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back), // da change
                        contentDescription = stringResource(R.string.back),
                        tint = Color.Black
                    )
                }
            }
        }

        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimension.Small)
        ){
            repeat(PAGECOUNT - 1) {index ->
                val virtualIndex = index + 1
                val isSelected = pagerState.currentPage == virtualIndex
                val width = if (isSelected) AppDimension.Large else AppDimension.Small
                val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray

                Box(
                    modifier = Modifier
                        .height(AppDimension.Small)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                        .border(if (isSelected) AppDimension.One else AppDimension.None, Color.Black, CircleShape)
                )
            }
        }

        Button(
            onClick = onNext,
            enabled = true,
            shape = RoundedCornerShape(AppDimension.CornerShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isNextEnabled) Color.Black else Color.Gray,
                contentColor = Color.White
            ),
            modifier = Modifier
                .alpha(if (isNextEnabled) 1f else 0.5f)
                .height(AppDimension.Huge)
                .border(
                    AppDimension.Border,
                    if (isNextEnabled) Color.Black else Color.Gray,
                    RoundedCornerShape(AppDimension.CornerShape)
                ),
            contentPadding = PaddingValues(horizontal = AppDimension.HorizontalPadding)

        ){
            Text(
                text = if (pagerState.currentPage == PAGECOUNT-1) stringResource(R.string.finish) else stringResource(R.string.next),
                style = TextStyle(fontWeight = AppTypography.labelMedium.fontWeight, fontSize = AppTypography.labelMedium.fontSize)
            )
        }
    }
}



