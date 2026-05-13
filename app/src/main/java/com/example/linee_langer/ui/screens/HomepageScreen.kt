package com.example.linee_langer.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.linee_langer.ui.components.LangerScaffold
import com.example.linee_langer.ui.viewModels.NotificationViewModel
import com.example.linee_langer.R
import com.example.linee_langer.ui.viewModels.HomeViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import com.example.linee_langer.dao.AnalysisWithLines
import com.example.linee_langer.db.dateFormatted
import com.example.linee_langer.ui.utils.DailyAdvice
import com.example.linee_langer.ui.viewModels.HistoryViewModel
import com.example.linee_langer.ui.viewModels.HomeUiState
import com.example.linee_langer.ui.viewModels.ProfileViewModel

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

    LangerScaffold(
        title = "Dashboard",
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
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Ciao ${name.ifBlank { stringResource(R.string.user) }}!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ecco la situazione della tua pelle oggi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = skinType.ifBlank { "Non impostato" },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
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
            .padding(bottom = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Ultima Scansione",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (analysis != null) {
                Text(
                    text = analysis.analysis.resultSummary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Eseguita il ${analysis.analysis.dateFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
                Button(
                    onClick = onNavigateToHistory,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Vai alla sezione storico", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Nessuna analisi eseguita",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Analizza Ora", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdviceSection(advices: List<DailyAdvice>) {
    Column {
        Text(
            text = "I tuoi consigli del giorno",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            items(advices) { advice ->
                RoutineMiniCard(
                    title = advice.title,
                    subtitle = advice.subtitle,
                    icon = advice.icon,
                    color = Color(advice.colorHex)
                )
            }
        }
    }
}

@Composable
private fun QuickActions(
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAdvice: () -> Unit
) {
    Column {
        Text(
            text = "Azioni Rapide",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GridActionCard(
                    title = "Nuova Analisi",
                    desc = "Scansiona linee viso",
                    icon = R.drawable.ic_camera,
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCamera
                )
                GridActionCard(
                    title = "Storico",
                    desc = "Vedi i progressi",
                    icon = R.drawable.ic_home,
                    color = Color(0xFFF3E5F5),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToHistory
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GridActionCard(
                    title = "Routine",
                    desc = "Consigli dedicati",
                    icon = R.drawable.ic_settings,
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAdvice
                )
                GridActionCard(
                    title = "Info Pelle",
                    desc = "Cos'è il collagene",
                    icon = R.drawable.ic_profile,
                    color = Color(0xFFFFE0B2),
                    modifier = Modifier.weight(1f),
                    onClick = { /* Implementare destinazione o Dialog */ }
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
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(16.dp)
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
private fun RoutineMiniCard(title: String, subtitle: String, icon: Int, color: Color) {
    Surface(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(20.dp),
        color = color
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray.copy(alpha = 0.8f))
            }
        }
    }
}

