package com.example.linee_langer.ui.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import com.example.linee_langer.R
import com.example.linee_langer.ui.theme.Dimens

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: (isExistingUser: Boolean, isGoogleUser: Boolean) -> Unit,
    onSwitchToRegister: () -> Unit
) {
    val uiState = authViewModel.uiState
    val isLoading = uiState is AuthUiState.Loading
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    // Navigazione automatica al successo
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onAuthSuccess(uiState.isExistingUser, uiState.isGoogleUser)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.XLarge)
            .imePadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.Start
    ) {

        Spacer(Modifier.height(Dimens.SuperHuge))

        // ── HEADER ───────────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.auth_login),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Dimens.ExtraSmall))
        Text(
            text = stringResource(R.string.auth_login_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Dimens.XXLarge))

        // ── CAMPI ────────────────────────────────────────────────────────────
        AuthTextField(
            value = authViewModel.email,
            onValueChange = { authViewModel.updateEmail(it) },
            label = stringResource(R.string.auth_email_hint),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !isLoading
        )

        Spacer(Modifier.height(Dimens.Standard))

        AuthTextField(
            value = authViewModel.password,
            onValueChange = { authViewModel.updatePassword(it) },
            label = stringResource(R.string.auth_password_hint),
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            if (passwordVisible) R.drawable.ic_close else R.drawable.ic_check
                        ),
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.hide_password else R.string.show_password
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (!isLoading) authViewModel.handleLogin()
                }
            ),
            enabled = !isLoading
        )

        Spacer(Modifier.height(Dimens.Standard))

        // ── ERRORE ───────────────────────────────────────────────────────────
        // AnimatedVisibility evita il layout shift brusco
        AnimatedVisibility(
            visible = uiState is AuthUiState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (uiState is AuthUiState.Error) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusMedium),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = Dimens.Standard,
                            vertical = Dimens.Medium
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Small)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Dimens.IconMedium)
                        )
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.XLarge))

        // ── CTA PRINCIPALE ───────────────────────────────────────────────────
        Button(
            onClick = {
                focusManager.clearFocus()
                authViewModel.handleLogin()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.Huge),
            shape = RoundedCornerShape(Dimens.RadiusLarge),
            enabled = !isLoading &&
                    authViewModel.email.isNotBlank() &&
                    authViewModel.password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.IconMedium),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = Dimens.BorderThin
                )
            } else {
                Text(
                    text = stringResource(R.string.login_start),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(Dimens.XLarge))

        // ── SWITCH REGISTRAZIONE ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.login_no_account_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onSwitchToRegister,
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = Dimens.ExtraSmall)
            ) {
                Text(
                    text = stringResource(R.string.noaccount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(Dimens.XXLarge))
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Dimens.ExtraSmall)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(enabled = enabled) {onClick()}
                    else Modifier
                ),
            shape = RoundedCornerShape(Dimens.RadiusStandard),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            ),
            isError = isError,
            supportingText = if (supportingText != null) {
                {
                    Text(
                        text = supportingText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else null,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            enabled = enabled,
            readOnly = readOnly
        )
    }
}
