package com.example.linee_langer.ui.feature.auth

import android.icu.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import com.example.linee_langer.R
import com.example.linee_langer.core.utils.AgeValidationResult
import com.example.linee_langer.core.utils.AuthValidators
import com.example.linee_langer.ui.theme.CircleShape
import com.example.linee_langer.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: (isExistingUser: Boolean, isGoogleUser: Boolean) -> Unit,
    onRegistrationComplete: () -> Unit,
    onSwitchToLogin: () -> Unit
) {

    val uiState = authViewModel.uiState
    val isLoading = uiState is AuthUiState.Loading
    val focusManager = LocalFocusManager.current

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = run {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, -18)
            cal.timeInMillis
        },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val minDate = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -120)
                }.timeInMillis
                val yesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }.timeInMillis
                return utcTimeMillis in minDate..yesterday
            }
        }
    )

    val formattedDate: String = selectedDateMillis?.let { millis ->
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
    } ?: ""

    val errorInvalidEmail = stringResource(R.string.error_invalid_email)
    val errorPasswordWeak = stringResource(R.string.error_password_weak)
    val errorAgeTooYoung   = stringResource(R.string.error_age_too_young)
    val errorAgeTooOld     = stringResource(R.string.error_age_too_old)


    // Error states
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val ageError: String? = if (formattedDate.isNotBlank()) {
        when (AuthValidators.validateBirthDate(formattedDate)) {
            AgeValidationResult.TooYoung -> errorAgeTooYoung
            AgeValidationResult.TooOld   -> errorAgeTooOld
            else -> null
        }
    } else null

    val currentPassword = authViewModel.password
    val hasMinLength = AuthValidators.hasMinLength(currentPassword)
    val hasNumber = AuthValidators.hasNumber(currentPassword)
    val hasUppercase = AuthValidators.hasUpperCase(currentPassword)
    val hasSpecialChar = AuthValidators.hasSpecialChar(currentPassword)

    val canSubmit =
            emailError == null &&
            passwordError == null &&
            ageError == null &&
            firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            formattedDate.isNotBlank() &&
            authViewModel.email.isNotBlank() &&
            authViewModel.password.isNotBlank()
            AuthValidators.isPasswordValid(currentPassword)

    LaunchedEffect(uiState) {

        when(uiState) {
            is AuthUiState.Success -> onAuthSuccess(uiState.isExistingUser, uiState.isGoogleUser)
            is AuthUiState.RegistrationPendingVerification -> onRegistrationComplete()
            else -> Unit
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        ) {
            DatePicker(state = datePickerState)
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

        // ── Header ───────────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.credentials),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Dimens.ExtraSmall))
        Text(
            text = stringResource(R.string.auth_register_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Dimens.XLarge))

        // ── Stepper visivo ────────────────────────────────────────────────────
        // Due punti collegati: indica i due blocchi del form (anagrafica / credenziali).
        // Aiuta l'utente a capire che il form è strutturato e non infinito.
        RegisterStepper(
            steps = listOf(
                stringResource(R.string.register_step_personal),
                stringResource(R.string.register_step_credentials)
            )
        )

        Spacer(Modifier.height(Dimens.XLarge))

        // ── BLOCCO 1: Dati anagrafici ─────────────────────────────────────────
        SectionLabel(stringResource(R.string.register_step_personal))

        Spacer(Modifier.height(Dimens.Standard))

        AuthTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = stringResource(R.string.auth_first_name),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !isLoading
        )

        Spacer(Modifier.height(Dimens.Standard))

        AuthTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = stringResource(R.string.auth_last_name),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            enabled = !isLoading
        )

        Spacer(Modifier.height(Dimens.Standard))

        // Campo data — readonly, tap apre DatePicker
        AuthTextField(
            value = formattedDate,
            onValueChange = {},
            label = stringResource(R.string.auth_birth_date),
            isError = ageError != null,
            supportingText = ageError,
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }, enabled = !isLoading) {
                    Icon(
                        painter = painterResource(R.drawable.ic_analysis),
                        contentDescription = stringResource(R.string.auth_pick_date),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            onClick = { showDatePicker = true },
            enabled = !isLoading
        )

        Spacer(Modifier.height(Dimens.XLarge))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Dimens.XLarge))

        // ── BLOCCO 2: Credenziali ─────────────────────────────────────────────
        SectionLabel(stringResource(R.string.register_step_credentials))

        Spacer(Modifier.height(Dimens.Standard))

        AuthTextField(
            value = authViewModel.email,
            onValueChange = { newVal ->
                authViewModel.updateEmail(newVal)
                emailError = if (newVal.isNotBlank() && !AuthValidators.isEmailValid(newVal))
                    errorInvalidEmail else null
            },
            label = stringResource(R.string.auth_email_hint),
            isError = emailError != null,
            supportingText = emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !isLoading
        )

        if (currentPassword.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.Small))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Dimens.Small),
                verticalArrangement = Arrangement.spacedBy(Dimens.ExtraSmall)
            ) {
                PasswordRequirementItem(
                    text = stringResource(R.string.req_min_chars),
                    isMet = hasMinLength
                )
                PasswordRequirementItem(
                    text = stringResource(R.string.req_number),
                    isMet = hasNumber
                )
                PasswordRequirementItem(
                    text = stringResource(R.string.req_uppercase),
                    isMet = hasUppercase
                )
                PasswordRequirementItem(
                    text = stringResource(R.string.req_special_char),
                    isMet = hasSpecialChar
                )
            }
        }

        Spacer(Modifier.height(Dimens.Standard))

        AuthTextField(
            value = authViewModel.password,
            onValueChange = { newVal ->
                authViewModel.updatePassword(newVal)
                passwordError = if (newVal.isNotBlank() && !AuthValidators.isPasswordValid(newVal))
                    errorPasswordWeak else null
            },
            label = stringResource(R.string.auth_password_hint),
            isError = passwordError != null,
            supportingText = passwordError,
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
                    if (canSubmit) {
                        authViewModel.pendingFirstName = firstName
                        authViewModel.pendingLastName  = lastName
                        authViewModel.pendingBirthDate = formattedDate
                        authViewModel.handleRegister()
                    }
                }
            ),
            enabled = !isLoading
        )

        Spacer(Modifier.height(Dimens.Standard))

        // ── Feedback stato ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState is AuthUiState.Error || uiState is AuthUiState.RegistrationPendingVerification,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (uiState) {
                is AuthUiState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.RadiusMedium),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.Standard),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Small),
                            verticalAlignment = Alignment.CenterVertically
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
                is AuthUiState.RegistrationPendingVerification -> {
                    // Banner positivo — verde/primaryContainer, NON errorContainer
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.RadiusMedium),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.Standard),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.IconMedium)
                            )
                            Text(
                                text = stringResource(R.string.account_created_verify),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                else -> Unit
            }
        }

        Spacer(Modifier.height(Dimens.XLarge))

        // ── CTA principale ────────────────────────────────────────────────────
        Button(
            onClick = {
                focusManager.clearFocus()
                if (canSubmit) {
                    authViewModel.pendingFirstName = firstName
                    authViewModel.pendingLastName  = lastName
                    authViewModel.pendingBirthDate = formattedDate
                    authViewModel.handleRegister()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.Huge),
            shape = RoundedCornerShape(Dimens.RadiusLarge),
            enabled = !isLoading && canSubmit
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.IconMedium),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = Dimens.BorderThin
                )
            } else {
                Text(
                    text = stringResource(R.string.register_save),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(Dimens.Standard))

        // ── Switch login ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.register_already_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onSwitchToLogin,
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = Dimens.ExtraSmall)
            ) {
                Text(
                    text = stringResource(R.string.have_account),
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
private fun PasswordRequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ExtraSmall)
    ) {
        Icon(
            painter = painterResource(
                if (isMet) R.drawable.ic_check else R.drawable.ic_close
            ),
            contentDescription = null,
            tint = if (isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Dimens.IconSmall)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun RegisterStepper(steps: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            // Pallino step
            Box(
                modifier = Modifier
                    .size(Dimens.Standard)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(Dimens.ExtraSmall))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Linea connettore (non dopo l'ultimo)
            if (index < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Dimens.Small),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

