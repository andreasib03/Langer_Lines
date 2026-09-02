package com.example.linee_langer.ui.feature.onboarding.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.linee_langer.R
import com.example.linee_langer.ui.feature.auth.AuthUiState
import com.example.linee_langer.ui.feature.auth.AuthViewModel
import com.example.linee_langer.ui.theme.CircleShape
import com.example.linee_langer.ui.theme.Dimens
import kotlinx.coroutines.delay


// Intervallo polling in millisecondi — controlla ogni 4 secondi
private const val POLLING_INTERVAL_MS = 4_000L

// Cooldown reinvio email in secondi
private const val RESEND_COOLDOWN_SECONDS = 30
@Composable
fun EmailVerificationScreen(
    authViewModel: AuthViewModel,
    onVerified: () -> Unit,        // → Onboarding
    onBack: () -> Unit             // → WelcomeScreen
) {
    val uiState = authViewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    // Messaggi snackbar letti nel contesto @Composable
    val msgSent  = stringResource(R.string.email_sent)
    val msgError = stringResource(R.string.email_send_error)
    val msgNotVerified = stringResource(R.string.email_not_verified)

    var resendCooldown by remember { mutableIntStateOf(0) }
    BackHandler(true) { }

    LaunchedEffect(Unit) {
        while(true){
            delay(POLLING_INTERVAL_MS)
            authViewModel.checkAndProceed(
                onVerified = onVerified,
                onNotVerified = { /*non fa niente*/}
            )
        }
    }

    LaunchedEffect(resendCooldown) {
        if(resendCooldown > 0){
            delay(1_000L)
            resendCooldown--
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.EmailSent      -> snackbarHostState.showSnackbar(msgSent)
            is AuthUiState.EmailSendError -> snackbarHostState.showSnackbar(msgError)
            is AuthUiState.NotVerifiedYet -> snackbarHostState.showSnackbar(msgNotVerified)
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(end = Dimens.Standard, top = Dimens.Small),
                contentAlignment = Alignment.CenterEnd
            ){
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.back_to_welcome),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.XLarge),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.Standard)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_profile), // icona mail
                    contentDescription = "",
                    modifier = Modifier.size(Dimens.IconXLarge),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.verify_email_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.verify_email_body),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(Dimens.Standard))

                PollingIndicator()

                Text(
                    text = stringResource(R.string.verify_email_polling_hint),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(Dimens.Standard))

                // CTA principale — controlla se l'email è verificata
                Button(
                    onClick = {
                        authViewModel.checkAndProceed(
                            onVerified = onVerified,
                            onNotVerified = {}
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.Huge),
                    shape = RoundedCornerShape(Dimens.RadiusLarge)
                ) {
                    Text(
                        text = stringResource(R.string.verify_email_clicked),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Reinvia email
                TextButton(
                    onClick = {
                        if(resendCooldown == 0){
                            resendCooldown = RESEND_COOLDOWN_SECONDS
                            authViewModel.resendVerificationEmail(
                                onSuccess = { },
                                onError = { }
                            )
                        }
                    },
                    enabled = resendCooldown == 0
                ) {
                    if(resendCooldown > 0){
                        Text(
                            text = stringResource(R.string.verify_email_resend_cooldown,
                                resendCooldown
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(stringResource(R.string.verify_email_resend))
                    }

                }

                // Torna alla schermata iniziale
                TextButton(
                    onClick = {
                        authViewModel.deleteUnverifiedAccount(onBack)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.email_undo),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Errori / feedback
                if (uiState is AuthUiState.Error) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

}

@Composable
private fun PollingIndicator() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            PulsatingDot(delayMs = index * 250)
        }
    }
}

@Composable
private fun PulsatingDot(delayMs: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_$delayMs")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = delayMs,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale_$delayMs"
    )

    Box(
        modifier = Modifier
            .size(Dimens.Small)
            .scale(scale)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
    )
}