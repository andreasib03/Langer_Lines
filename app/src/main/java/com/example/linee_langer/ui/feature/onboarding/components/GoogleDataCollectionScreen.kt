package com.example.linee_langer.ui.feature.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.linee_langer.R
import com.example.linee_langer.ui.feature.auth.AuthViewModel
import com.example.linee_langer.ui.feature.onboarding.OnBoardingViewModel
import com.example.linee_langer.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDataCollectionScreen(
    onBoardingViewModel: OnBoardingViewModel,
    authViewModel: AuthViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    // Stato locale del form — non va nel VM finché l'utente preme "Continua"
    var name      by remember { mutableStateOf("") }
    var lastName  by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    // Data formattata per la visualizzazione nel campo
    val formattedDate: String = selectedDateMillis?.let { millis ->
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
    } ?: ""

    // DatePickerState — pre-impostato a 18 anni fa, limita a [oggi-120anni, ieri]
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Calendar.getInstance()
            .apply { add(Calendar.YEAR, -18) }.timeInMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val minDate = Calendar.getInstance()
                    .apply { add(Calendar.YEAR, -120) }.timeInMillis
                val yesterday = Calendar.getInstance()
                    .apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
                return utcTimeMillis in minDate..yesterday
            }
        }
    )

    // Il pulsante "Continua" è attivo solo se tutti e tre i campi sono compilati
    val canProceed = name.isNotBlank() &&
            lastName.isNotBlank() &&
            formattedDate.isNotBlank()

    // ── DatePicker Dialog ─────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
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

    // ── Layout ────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Bottone "Continua" fisso in fondo — sempre visibile sopra la tastiera
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.XLarge, vertical = Dimens.Standard)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        if (canProceed) {
                            // Salva in OnBoardingViewModel (usato da finishOnBoarding)
                            onBoardingViewModel.name      = name
                            onBoardingViewModel.lastName  = lastName
                            onBoardingViewModel.birthDate = formattedDate

                            // Salva in AuthViewModel.pending* (usato da importUserData
                            // chiamato in EmailVerificationScreen.onVerified)
                            authViewModel.pendingFirstName = name
                            authViewModel.pendingLastName  = lastName
                            authViewModel.pendingBirthDate = formattedDate

                            onContinue()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.Huge),
                    shape = RoundedCornerShape(Dimens.RadiusLarge),
                    enabled = canProceed
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_next),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.XLarge)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Standard)
        ) {

            Spacer(Modifier.height(Dimens.Large))

            // ── Header ───────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.personal),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.google_data_collection_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Dimens.Small))

            // ── Nome ─────────────────────────────────────────────────────────
            Column {
                Text(
                    text = stringResource(R.string.auth_first_name),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Dimens.ExtraSmall)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.example_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.RadiusStandard),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            }

            // ── Cognome ──────────────────────────────────────────────────────
            Column {
                Text(
                    text = stringResource(R.string.auth_last_name),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Dimens.ExtraSmall)
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = { Text(stringResource(R.string.example_last_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.RadiusStandard),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            }

            // ── Data di nascita — readonly, apre il DatePicker al tap ────────
            Column {
                Text(
                    text = stringResource(R.string.auth_birth_date),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Dimens.ExtraSmall)
                )
                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    placeholder = { Text(stringResource(R.string.auth_birth_date_placeholder)) },
                    singleLine = true,
                    readOnly = true,
                    shape = RoundedCornerShape(Dimens.RadiusStandard),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_analysis),
                                contentDescription = stringResource(R.string.auth_pick_date),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(Modifier.height(Dimens.XXLarge))
        }
    }
}