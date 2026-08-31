package com.example.linee_langer.ui.feature.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.linee_langer.R
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import com.example.linee_langer.ui.shared.components.LangerScaffold
import com.example.linee_langer.ui.theme.Dimens

private data class FaqEntry(val questionRes: Int, val answerRes: Int)

// Elenco completo delle FAQ — includere qui anche la FAQ sul collagene per mantenere
// coerenza con la spiegazione già mostrata nella Home (SkinInfoDialog).
private val faqEntries = listOf(
    FaqEntry(R.string.faq_q1, R.string.faq_a1),
    FaqEntry(R.string.faq_q9, R.string.faq_a9),   // Collagene — coerente con la Home
    FaqEntry(R.string.faq_q10, R.string.faq_a10),
    FaqEntry(R.string.faq_q14, R.string.faq_a14),
    FaqEntry(R.string.faq_q2, R.string.faq_a2),
    FaqEntry(R.string.faq_q3, R.string.faq_a3),
    FaqEntry(R.string.faq_q4, R.string.faq_a4),
    FaqEntry(R.string.faq_q5, R.string.faq_a5),
    FaqEntry(R.string.faq_q6, R.string.faq_a6),
    FaqEntry(R.string.faq_q7, R.string.faq_a7),
    FaqEntry(R.string.faq_q8, R.string.faq_a8),
    FaqEntry(R.string.faq_q11, R.string.faq_a11),
    FaqEntry(R.string.faq_q12, R.string.faq_a12),
    FaqEntry(R.string.faq_q13, R.string.faq_a13)
)

@Composable
fun FaqScreen(
    notificationViewModel: NotificationViewModel,
    onBack: () -> Unit
) {
    LangerScaffold(
        title = stringResource(R.string.faq_title),
        notificationViewModel = notificationViewModel,
        canNavigateBack = true,
        onBackClick = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Dimens.Standard),
            verticalArrangement = Arrangement.spacedBy(Dimens.Medium)
        ) {
            item {
                Text(
                    text = stringResource(R.string.faq_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.Small)
                )
            }

            items(faqEntries) { entry ->
                FaqItem(questionRes = entry.questionRes, answerRes = entry.answerRes)
            }
        }
    }
}

@Composable
private fun FaqItem(questionRes: Int, answerRes: Int) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 90f else 0f, label = "faqChevron")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(Dimens.RadiusStandard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.BorderThin)
    ) {
        Column(modifier = Modifier.padding(Dimens.Standard)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(questionRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_back),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(Dimens.IconSmall)
                        .graphicsLayer { rotationZ = 180f + rotation }
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = stringResource(answerRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.Small)
                )
            }
        }
    }
}
