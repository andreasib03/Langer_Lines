package com.example.linee_langer.ui.feature.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.linee_langer.ui.feature.camera.model.SkinTypeOption
import com.example.linee_langer.ui.feature.camera.model.skinOptions
import com.example.linee_langer.R
import com.example.linee_langer.domain.models.SkinTypeIds
import com.example.linee_langer.ui.theme.Dimens
import com.example.linee_langer.ui.theme.appColors
@Composable
private fun skinTypeColor(id: String): Color {
    val colors = MaterialTheme.appColors
    return when (id) {
        SkinTypeIds.DRY    -> colors.skinTypeDryBg
        SkinTypeIds.OILY   -> colors.skinTypeOilyBg
        SkinTypeIds.MIXED  -> colors.skinTypeMixedBg
        SkinTypeIds.NORMAL -> colors.skinTypeNormalBg
        else               -> colors.skinTypeDefaultBg
    }
}

@Composable
private fun skinTypeIconColor(id: String): Color {
    val colors = MaterialTheme.appColors
    return when (id) {
        SkinTypeIds.DRY    -> colors.skinTypeDryIcon
        SkinTypeIds.OILY   -> colors.skinTypeOilyIcon
        SkinTypeIds.MIXED  -> colors.skinTypeMixedIcon
        SkinTypeIds.NORMAL -> colors.skinTypeNormalIcon
        else               -> colors.skinTypeDefaultIcon
    }
}

@Composable
fun OnboardingSkinTypeScreen(
    selectedId: String,
    onOptionSelected: (String) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.XLarge)
    ) {
        Spacer(Modifier.height(Dimens.XLarge))

        // Header allineato a sinistra — coerente con le altre pagine di onboarding
        Text(
            text = stringResource(R.string.first_question),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Dimens.ExtraSmall))
        Text(
            text = stringResource(R.string.description_skintype),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Dimens.XLarge))

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Standard)) {
            skinOptions.forEach { option ->
                SkinOptionCard(
                    option = option,
                    isSelected = selectedId == option.id,
                    onClick = { onOptionSelected(option.id) }
                )
            }
        }

        Spacer(Modifier.height(Dimens.XXLarge))
    }
}



@Composable
fun SkinOptionCard(
    option: SkinTypeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Scala leggera al momento della selezione — feedback tattile visivo
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(150),
        label = "card_scale_${option.id}"
    )

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        onClick = onClick,
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
            defaultElevation = if (isSelected) Dimens.CardElevation else Dimens.None
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Standard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Standard)
        ) {
            // Indicatore colore tipo pelle — più informativo del pallino generico
            Surface(
                modifier = Modifier.size(Dimens.Huge),
                shape = RoundedCornerShape(Dimens.RadiusMedium),
                color = skinTypeColor(option.id)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(option.icon),
                        contentDescription = null,
                        tint = skinTypeIconColor(option.id),
                        modifier = Modifier.size(Dimens.IconMedium)
                    )
                }
            }

            // Testo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(option.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Dimens.CardElevation))
                Text(
                    text = stringResource(option.description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // Checkbox visivo — cerchio con riempimento se selezionato
            Surface(
                modifier = Modifier.size(Dimens.Standard),
                shape = CircleShape,
                border = BorderStroke(
                    width = Dimens.CardElevation,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            ) {
                if (isSelected) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(Dimens.IconSmall)
                        )
                    }
                }
            }
        }
    }
}


