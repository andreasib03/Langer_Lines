package com.example.linee_langer.ui.feature.onboarding

import androidx.compose.foundation.lazy.items
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.linee_langer.R
import com.example.linee_langer.domain.models.LangerGoal
import com.example.linee_langer.ui.feature.onboarding.components.OnboardingSkinTypeScreen
import com.example.linee_langer.ui.theme.AppTypography
import com.example.linee_langer.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

const val ONBOARDING_PAGE_COUNT = 2
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun OnBoardingScreen(
    onBoardingViewModel: OnBoardingViewModel,
    onFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    var showSuccessAnimation by remember { mutableStateOf(false) }

    val canProceed = when (pagerState.currentPage) {
        0 -> onBoardingViewModel.selectedSkinType.isNotBlank()
        1 -> onBoardingViewModel.selectedGoal.isNotEmpty()
        else -> true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if(!showSuccessAnimation) {
                OnBoardingBottomBar(
                    pagerState = pagerState,
                    onNext = {
                        if (!canProceed) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.infos),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            if (pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1) {
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

        if(!showSuccessAnimation){
            // Layer 1 — Pager SkinType + Goal
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> OnboardingSkinTypeScreen(
                        selectedId = onBoardingViewModel.selectedSkinType,
                        onOptionSelected = { onBoardingViewModel.selectedSkinType = it }
                    )
                    1 -> CardChoicesPage(
                        selectedGoals = onBoardingViewModel.selectedGoal,
                        onGoalToggled = { goal ->
                            onBoardingViewModel.selectedGoal =
                                if (goal in onBoardingViewModel.selectedGoal)
                                    onBoardingViewModel.selectedGoal - goal
                                else
                                    onBoardingViewModel.selectedGoal + goal
                        }

                    )
                }
            }
        }


        // Layer 2 — Animazione successo finale
        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.surface),
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
internal fun CardChoicesPage(
    selectedGoals: Set<LangerGoal>,
    onGoalToggled: (LangerGoal) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(
                horizontal = Dimens.XLarge,
                vertical = Dimens.XLarge
            )
        ) {
            Text(
                text = stringResource(R.string.purpose),
                style = typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.onSurface
            )
            Spacer(Modifier.height(Dimens.ExtraSmall))
            Text(
                text = stringResource(R.string.goal_subtitle),
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Dimens.Standard))

            // Counter selezioni — animato
            AnimatedContent(
                targetState = selectedGoals.size,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "goal_counter"
            ) { count ->
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusHuge),
                    color = if (count > 0)
                        colorScheme.primaryContainer
                    else
                        colorScheme.surfaceVariant,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = if (count == 0)
                            stringResource(R.string.goal_none_selected)
                        else
                            stringResource(R.string.goal_count_selected, count),
                        modifier = Modifier.padding(
                            horizontal = Dimens.Standard,
                            vertical = Dimens.ExtraSmall
                        ),
                        style = typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (count > 0)
                            colorScheme.onPrimaryContainer
                        else
                            colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Lista card ────────────────────────────────────────────────────────
        // LazyColumn invece di Column: se si aggiungono goal in futuro,
        // la lista scorre senza ricomporre tutto.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = Dimens.XLarge,
                vertical = Dimens.Small
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.Standard)
        ) {
            items(
                items = LangerGoal.entries,
                key = { it.id }
            ) { goal ->
                GoalCard(
                    goal = goal,
                    isSelected = goal in selectedGoals,
                    onToggle = { onGoalToggled(goal) }
                )
            }

            // Spacer finale per evitare che l'ultima card sia coperta dal BottomBar
            item { Spacer(Modifier.height(Dimens.Huge)) }
        }
    }
}

@Composable
private fun GoalCard(
    goal: LangerGoal,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(180),
        label = "goal_scale_${goal.id}"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            colorScheme.surface,
        animationSpec = tween(200),
        label = "goal_color_${goal.id}"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            colorScheme.primary
        else
            colorScheme.outlineVariant.copy(alpha = 0.6f),
        animationSpec = tween(200),
        label = "goal_border_${goal.id}"
    )

    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(Dimens.RadiusStandard),
        border = BorderStroke(
            width = if (isSelected) Dimens.CardElevation else Dimens.BorderThin,
            color = borderColor
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) Dimens.BorderStandard else Dimens.None
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Standard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Standard)
        ) {

            // Icona contestuale del goal
            Surface(
                modifier = Modifier.size(Dimens.Huge),
                shape = RoundedCornerShape(Dimens.RadiusMedium),
                color = if (isSelected)
                    colorScheme.primary.copy(alpha = 0.12f)
                else
                    colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(goal.iconRes),
                        contentDescription = null,
                        tint = if (isSelected)
                            colorScheme.primary
                        else
                            colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.IconMedium)
                    )
                }
            }

            // Testo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(goal.titleRes),
                    style = typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Spacer(Modifier.height(Dimens.CardElevation))
                Text(
                    text = stringResource(goal.subtitleRes),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // Checkbox circolare animata
            Surface(
                modifier = Modifier.size(Dimens.Standard),
                shape = CircleShape,
                border = BorderStroke(
                    width = Dimens.CardElevation,
                    color = if (isSelected)
                        colorScheme.primary
                    else
                        colorScheme.outlineVariant
                ),
                color = if (isSelected)
                    colorScheme.primary
                else
                    Color.Transparent
            ) {
                if (isSelected) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(Dimens.IconSmall)
                        )
                    }
                }
            }
        }
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
            delay(500.milliseconds)
            onAnimationFinished()
        }
    }

    LaunchedEffect(compositionResult.isFailure) {
        if (compositionResult.isFailure) {
            delay(2000.milliseconds)
            onAnimationFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.XLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icona di successo o Animazione
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(Dimens.Animation)
        )

        Spacer(modifier = Modifier.height(Dimens.XLarge))

        Text(
            text = stringResource(R.string.onboarding_ready),
            style = typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Dimens.Small))

        Text(
            text = stringResource(R.string.finish_settings),
            style = typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = colorScheme.onSurfaceVariant
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
            .padding(horizontal = Dimens.Large, vertical = Dimens.Standard)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(modifier = Modifier.size(Dimens.Huge)){
            if(pagerState.currentPage == 1){
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .border(
                            Dimens.BorderStandard, colorScheme.surface, RoundedCornerShape(
                                Dimens.RadiusLarge))
                        .background(colorScheme.onBackground, RoundedCornerShape(Dimens.RadiusLarge))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back), // da change
                        contentDescription = stringResource(R.string.back),
                        tint = colorScheme.outlineVariant
                    )
                }
            }
        }

        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Small)
        ){
            repeat(ONBOARDING_PAGE_COUNT - 1) {index ->
                val virtualIndex = index + 1
                val isSelected = pagerState.currentPage == virtualIndex
                val width = if (isSelected) Dimens.Large else Dimens.Small
                val color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant

                Box(
                    modifier = Modifier
                        .height(Dimens.Small)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                        .border(if (isSelected) Dimens.BorderStandard else Dimens.None, colorScheme.surface, CircleShape)
                )
            }
        }

        Button(
            onClick = onNext,
            enabled = true,
            shape = RoundedCornerShape(Dimens.RadiusLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isNextEnabled) colorScheme.onBackground else colorScheme.onSurfaceVariant,
                contentColor = colorScheme.outlineVariant
            ),
            modifier = Modifier
                .alpha(if (isNextEnabled) 1f else 0.5f)
                .height(Dimens.Huge)
                .border(
                    Dimens.BorderStandard,
                    if (isNextEnabled) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                    RoundedCornerShape(Dimens.RadiusLarge)
                ),
            contentPadding = PaddingValues(horizontal = Dimens.Medium)

        ){
            when(pagerState.currentPage){
                0 -> stringResource(R.string.onboarding_next)
                1 -> stringResource(R.string.onboarding_finish)
                else -> null
            }?.let {
                Text(
                    text = it,
                    style = TextStyle(fontWeight = AppTypography.labelMedium.fontWeight, fontSize = AppTypography.labelMedium.fontSize)
                )
            }
        }
    }
}



