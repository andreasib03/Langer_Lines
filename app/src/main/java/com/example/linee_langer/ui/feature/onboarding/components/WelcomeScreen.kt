package com.example.linee_langer.ui.feature.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
                modifier = Modifier.padding(Dimens.Large)
            ) {
                // Logo placeholder — sostituire con ic_logo dedicato
                Surface(
                    modifier = Modifier.size(Dimens.Ottantotto),
                    shape = RoundedCornerShape(Dimens.RadiusXLarge),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = Dimens.ExtraSmall
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "",
                            modifier = Modifier.size(Dimens.IconLarge),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
                    modifier = Modifier.padding(horizontal = Dimens.Large)
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