package com.example.linee_langer.ui.feature.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.linee_langer.R
import com.example.linee_langer.ui.theme.Dimens
import com.example.linee_langer.ui.theme.Skin200
import com.example.linee_langer.ui.theme.Skin500

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    // Fade-in all'ingresso — migliora la prima impressione visiva
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "welcome_fade"
    )
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
    ) {

        // ── ZONA HERO ────────────────────────────────────────────────────────
        // Gradiente verticale brand (Skin200 → Skin500) che occupa il 58% dello
        // schermo. Dà identità visiva immediata senza richiedere un'immagine.
        //
        // NOTA: Skin200/Skin500 sono presi direttamente dalla palette (non da
        // MaterialTheme.colorScheme) DELIBERATAMENTE — è l'identità di brand della
        // schermata di benvenuto e resta invariata in dark mode, come primo colore
        // del gradiente (MaterialTheme.colorScheme.surface) resta comunque theme-aware
        // per garantire un blend morbido con lo sfondo. Se in futuro si vuole un
        // gradiente completamente theme-aware, va introdotto un semantic color
        // dedicato in AppColors (es. `heroGradientStart/End`) invece di riusare
        // Skin200/Skin500 fuori contesto.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            Skin200.copy(alpha = 0.4f),
                            Skin500.copy(alpha = 0.15f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Large)
            ) {
                Surface(
                    modifier = Modifier.size(Dimens.LogoPlaceholderSize),
                    shape = RoundedCornerShape(Dimens.RadiusXLarge),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = Dimens.ExtraSmall
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_langer),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(Dimens.RadiusXLarge)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(Dimens.XLarge))

                Text(
                    text = stringResource(R.string.langer_line_app),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(Dimens.Small))

                Text(
                    text = stringResource(R.string.welcome_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── ZONA CTA ─────────────────────────────────────────────────────────
        // Card bianca/superficie che si sovrappone alla zona hero con bordi
        // arrotondati in alto — effetto "sheet" moderno.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.46f)
                .clip(RoundedCornerShape(topStart = Dimens.RadiusXXLarge, topEnd = Dimens.RadiusXXLarge))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.XLarge)
                    .padding(top = Dimens.XLarge, bottom = Dimens.Large)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Dimens.Medium)
            ) {

                // Label sezione
                Text(
                    text = stringResource(R.string.welcome_cta_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(Dimens.ExtraSmall))

                // CTA primaria — Registrati
                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.Huge),
                    shape = RoundedCornerShape(Dimens.RadiusLarge),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.register_save),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // CTA secondaria — Google
                OutlinedButton(
                    onClick = onGoogleClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.Huge),
                    shape = RoundedCornerShape(Dimens.RadiusLarge),
                    border = ButtonDefaults.outlinedButtonBorder(
                        enabled = true
                    ).copy(
                        width = Dimens.BorderThin
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile),
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(Dimens.Small))
                    Text(
                        text = stringResource(R.string.auth_google),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Divisore testuale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.or_label),
                        modifier = Modifier.padding(horizontal = Dimens.Standard),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Accedi — tertiary, meno prominente
                TextButton(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.welcome_already_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}