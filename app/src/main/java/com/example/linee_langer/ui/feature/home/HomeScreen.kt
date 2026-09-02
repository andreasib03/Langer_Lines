package com.example.linee_langer.ui.feature.home

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.linee_langer.ui.shared.components.LangerScaffold
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import com.example.linee_langer.R
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.core.database.entity.dateFormatted
import com.example.linee_langer.core.utils.summaryText
import com.example.linee_langer.domain.models.SkinTypeIds
import com.example.linee_langer.ui.shared.utils.DailyAdvice
import com.example.linee_langer.ui.feature.history.HistoryViewModel
import com.example.linee_langer.ui.feature.home.components.SkinInfoDialog
import com.example.linee_langer.ui.feature.profile.ProfileViewModel
import com.example.linee_langer.ui.shared.utils.AdviceCategory
import com.example.linee_langer.ui.theme.Dimens


@Composable
fun skinTypeLabel(skinType: String): String {
    return when (skinType) {
        SkinTypeIds.DRY -> stringResource(R.string.secca)
        SkinTypeIds.OILY -> stringResource(R.string.grassa)
        SkinTypeIds.MIXED -> stringResource(R.string.mista)
        SkinTypeIds.NORMAL -> stringResource(R.string.normale)
        else -> stringResource(R.string.not_set)
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    notificationViewModel: NotificationViewModel,
    profileViewModel: ProfileViewModel,
    historyViewModel: HistoryViewModel,
    onNavigateToCamera: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAdvice: () -> Unit = {},
) {
    val userProfile by profileViewModel.userProfile.collectAsState()
    val skinTypeSaved by profileViewModel.userSkinType.collectAsState()
    val lastAnalysis by historyViewModel.lastAnalysis.collectAsState(initial = null)
    val advices by homeViewModel.todayAdvices.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.refreshIfNeeded()
    }

    LangerScaffold(
        title = stringResource(R.string.home),
        notificationViewModel = notificationViewModel,
        canNavigateBack = false
    ) { innerPadding ->

        if(userProfile == null){
            FullScreenLoading(modifier = Modifier.padding(innerPadding))
        } else {
            val state = HomeUiState.Success(
                name = userProfile?.name ?: "",
                skinType = skinTypeSaved,
                lastAnalysis = lastAnalysis,
                advices = advices
            )

            HomeContent(
                state = state,
                innerPadding = innerPadding,
                onNavigateToCamera = onNavigateToCamera,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToAdvice = onNavigateToAdvice
            )

        }
    }
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun WelcomeHeaderHomepage(name: String, skinType: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.Large),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(
                    R.string.home_greeting,
                    name.ifBlank { stringResource(R.string.user) }
                ),
                style = typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.today_advice),
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(Dimens.Medium),
            color = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer
        ) {
            Text(
                text = skinTypeLabel(skinType).ifBlank { stringResource(R.string.not_set) },
                modifier = Modifier.padding(horizontal = Dimens.Medium, vertical = Dimens.MediumSmall),
                style = typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LastAnalysisCard(
    analysis: AnalysisWithLines?,
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.XLarge),
        shape = RoundedCornerShape(Dimens.RadiusHuge),
        colors = CardDefaults.cardColors(containerColor = colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(Dimens.XLarge)) {
            Text(
                text = stringResource(R.string.last_scan),
                style = typography.titleMedium,
                color = colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(Dimens.Small))

            if (analysis != null) {
                Text(
                    text = analysis.summaryText(),
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(Dimens.ExtraSmall))
                Text(
                    text = stringResource(
                        R.string.last_analysis_date,
                        analysis.analysis.dateFormatted
                    ),
                    style = typography.bodySmall,
                    color = colorScheme.onPrimary.copy(alpha = 0.6f)
                )
                Button(
                    onClick = onNavigateToHistory,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.onPrimary, contentColor = colorScheme.primary)
                ) {
                    Text(stringResource(R.string.go_to_history), fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = stringResource(R.string.no_analysis_done),
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(Dimens.Medium))
                Button(
                    onClick = onNavigateToCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.onPrimary, contentColor = colorScheme.primary),
                ) {
                    Text(stringResource(R.string.analyze_now), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdviceSection(advices: List<DailyAdvice>) {

    var selectedAdvice by remember { mutableStateOf<DailyAdvice?>(null)}

    selectedAdvice?.let { advice ->
        AdviceDetailDialog(
            advice = advice,
            onDismiss = { selectedAdvice = null }
        )
    }

    Column {
        Text(
            text = stringResource(R.string.today_advice_2),
            style = typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Dimens.Medium)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.Medium),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.XLarge)
        ) {
            items(advices) { advice ->
                RoutineMiniCard(
                    title = advice.title,
                    subtitle = advice.subtitle,
                    icon = advice.icon,
                    category = advice.category,
                    onClick = { selectedAdvice = advice }
                )
            }
        }
    }
}
@Composable
private fun AdviceDetailDialog(
    advice: DailyAdvice,
    onDismiss: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Dimens.XLarge),
        icon = {
            Icon(
                painter = painterResource(advice.icon),
                contentDescription = stringResource(advice.title),
                tint = colorScheme.primary,
                modifier = Modifier.size(Dimens.XXLarge)
            )
        },
        title = {
            Text(
                text = stringResource(advice.title),
                style = typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Standard)) {
                // Descrizione completa
                Text(
                    text = stringResource(advice.subtitle),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                // Se DailyAdvice viene estesa con description, benefits, usage
                // aggiungere qui i blocchi corrispondenti:
                //
                // advice.description?.let { descRes ->
                //     HorizontalDivider()
                //     Text(stringResource(R.string.advice_how_to), fontWeight = FontWeight.SemiBold)
                //     Text(stringResource(descRes), style = bodyMedium)
                // }
                //
                // advice.benefits?.let { benefitsRes ->
                //     Text(stringResource(R.string.advice_benefits), fontWeight = FontWeight.SemiBold)
                //     Text(stringResource(benefitsRes))
                // }

                // Badge categoria
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusHuge),
                    color = categoryColor(advice.category).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = categoryLabel(advice.category),
                        modifier = Modifier.padding(
                            horizontal = Dimens.Standard,
                            vertical   = Dimens.ExtraSmall
                        ),
                        style = typography.labelSmall,
                        color = categoryColor(advice.category)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.understand),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )

}

@Composable
private fun QuickActions(
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAdvice: () -> Unit
) {

    var showSkinInfoDialog by remember { mutableStateOf(false) }
    if(showSkinInfoDialog){
        SkinInfoDialog(onDismiss = {showSkinInfoDialog = false})
    }

    Column {
        Text(
            text = stringResource(R.string.quick_action),
            style = typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Dimens.Medium)
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Medium)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Medium)) {
                GridActionCard(
                    title = stringResource(R.string.new_analysis),
                    desc = stringResource(R.string.scan_face_line),
                    icon = R.drawable.ic_camera,
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCamera
                )
                GridActionCard(
                    title = stringResource(R.string.history),
                    desc = stringResource(R.string.see_your_progress),
                    icon = R.drawable.ic_home,
                    color = colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToHistory
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Medium)) {
                GridActionCard(
                    title = stringResource(R.string.routine),
                    desc = stringResource(R.string.advice_dedicated),
                    icon = R.drawable.ic_settings,
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAdvice
                )
                GridActionCard(
                    title = stringResource(R.string.skin_info),
                    desc = stringResource(R.string.what_is_collagene),
                    icon = R.drawable.ic_profile,
                    color = colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = { showSkinInfoDialog = true }
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    innerPadding: PaddingValues,
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAdvice: () -> Unit
){
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(colorScheme.background),
        contentPadding = PaddingValues(Dimens.Standard)
    ) {
        // HEADER: Ora usiamo state.name e state.skinType (garantiti non nulli)
        item {
            WelcomeHeaderHomepage(name = state.name, skinType = state.skinType)
        }

        // CARD ULTIMA ANALISI: Usiamo state.lastAnalysis
        item {
            LastAnalysisCard(
                analysis = state.lastAnalysis,
                onNavigateToCamera = onNavigateToCamera,
                onNavigateToHistory = onNavigateToHistory
            )
        }

        // CONSIGLI: Usiamo state.advices
        item {
            AdviceSection(advices = state.advices)
        }

        // AZIONI RAPIDE (rimangono uguali)
        item {
            QuickActions(
                onNavigateToCamera = onNavigateToCamera,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToAdvice = onNavigateToAdvice
            )
        }
    }
}

@Composable
private fun RoutineMiniCard(
    title: Int,
    subtitle: Int,
    icon: Int,
    category: AdviceCategory,
    onClick: () -> Unit
) {
    val bgColor = when(category){
        AdviceCategory.HYDRATION   -> colorScheme.primaryContainer
        AdviceCategory.PROTECTION  -> colorScheme.tertiaryContainer
        AdviceCategory.MASSAGE     -> colorScheme.secondaryContainer
        AdviceCategory.CLEANSING   -> colorScheme.surfaceVariant
        AdviceCategory.OTHER       -> colorScheme.surface
    }
    Surface(
        modifier = Modifier
            .width(Dimens.CameraBottomPadding)
            .clickable{onClick()},
        shape = RoundedCornerShape(Dimens.Large),
        color = bgColor
    ) {
        Column(modifier = Modifier.padding(Dimens.Standard)) {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(id = title),
                modifier = Modifier.size(Dimens.XLarge),
                tint = colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.Standard))
            Text(text = stringResource(id = title), style = typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = stringResource(id = subtitle), maxLines = 2, style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GridActionCard(
    title: String,
    desc: String,
    icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .height(Dimens.ButtonWidth)
            .clickable { onClick() },
        shape = RoundedCornerShape(Dimens.XLarge),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.Standard)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                modifier = Modifier.size(Dimens.RadiusHuge)
            )
            Column {
                Text(title, style = typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(desc, style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
            }
        }
    }
}


@Composable
private fun categoryColor(category: AdviceCategory): Color = when (category) {
    AdviceCategory.HYDRATION  -> colorScheme.primary
    AdviceCategory.PROTECTION -> colorScheme.tertiary
    AdviceCategory.MASSAGE    -> colorScheme.secondary
    AdviceCategory.CLEANSING  -> colorScheme.tertiaryContainer
    AdviceCategory.OTHER      -> colorScheme.outline
}

@Composable
private fun categoryLabel(category: AdviceCategory): String = when (category) {
    AdviceCategory.HYDRATION  -> stringResource(R.string.category_hydration)
    AdviceCategory.PROTECTION -> stringResource(R.string.category_protection)
    AdviceCategory.MASSAGE    -> stringResource(R.string.category_massage)
    AdviceCategory.CLEANSING  -> stringResource(R.string.category_cleansing)
    AdviceCategory.OTHER      -> stringResource(R.string.category_other)
}