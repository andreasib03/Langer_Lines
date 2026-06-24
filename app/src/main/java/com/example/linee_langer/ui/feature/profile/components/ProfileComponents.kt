package com.example.linee_langer.ui.feature.profile.components

import android.content.Context
import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import coil.compose.AsyncImage
import com.example.linee_langer.R
import com.example.linee_langer.domain.models.SkinTypeIds
import com.example.linee_langer.ui.theme.Dimens
import com.example.linee_langer.ui.theme.appColors


@Composable
fun ProfileHeader(
    name: String,
    email: String,
    profileImageUri: Uri?,
    onImageClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.XLarge)
    ) {
        Box(modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.BottomEnd)
        {
            Surface(
                modifier = Modifier
                    .size(Dimens.CameraTopPadding)
                    .clip(CircleShape)
                    .clickable { onImageClick() }, // Rende l'intera area cliccabile
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {

                if(profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = stringResource(R.string.image_profile),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile),
                        contentDescription = stringResource(R.string.change_image_profile),
                        modifier = Modifier.padding(Dimens.Large),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Piccolo badge con icona fotocamera per suggerire l'azione
            Surface(
                modifier = Modifier
                    .size(Dimens.RadiusHuge)
                    .clip(CircleShape)
                    .clickable { onImageClick() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = Dimens.ExtraSmall
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_camera),
                        contentDescription = stringResource(R.string.change_image_profile),
                        modifier = Modifier.size(Dimens.Standard),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Medium))
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


    @Composable
    fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
        Card(
            modifier = modifier.fillMaxHeight(),
            shape = RoundedCornerShape(Dimens.Standard),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.Standard),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,

            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Dimens.ExtraSmall))
                Text(text = label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    @Composable
    fun ProfileMenuItem(
        title: String,
        icon: Int,
        showBadge: Boolean = false,
        onClick: () -> Unit = {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Medium))
                .clickable { onClick() }
                .padding(Dimens.Standard),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(icon),
                contentDescription = "",
                modifier = Modifier.size(Dimens.XLarge),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(Dimens.Standard))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (showBadge) {
                Surface(
                    modifier = Modifier.size(Dimens.Small).padding(end = Dimens.ExtraSmall),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error
                ) {}
            }

            Icon(
                painterResource(R.drawable.ic_back),
                contentDescription = "",
                modifier = Modifier.size(Dimens.Standard).rotate(180f),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }

    @Composable
    fun MainActionCard(onClick: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.Standard),
            shape = RoundedCornerShape(Dimens.XLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.padding(Dimens.XLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.new_scan),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(stringResource(R.string.analyze_new_lines), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                }
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimens.XXLarge)
                )
            }
        }
    }

@Composable
fun getSkinTypeDisplayName(skinTypeId: String?): String {
    return when (skinTypeId) {
        SkinTypeIds.DRY  -> stringResource(R.string.secca)
        SkinTypeIds.OILY -> stringResource(R.string.grassa)
        SkinTypeIds.MIXED -> stringResource(R.string.mista)
        SkinTypeIds.NORMAL -> stringResource(R.string.normale)
        else -> stringResource(R.string.unspecified) // <--- FALLBACK
    }
}
@Composable
fun EditableUserInfoCard(
    context: Context,
    name: String,
    email: String,
    skinType: String,
    isEmailEditable: Boolean,
    isEmailVerified: Boolean,
    onVerifyClick: () -> Unit,
    onEmailChange: ((String) -> Unit)?
) {


    var tempEmail by remember(email) { mutableStateOf(email) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Validiamo l'email solo se è modificabile
    val isEmailValid = !isEmailEditable || Patterns.EMAIL_ADDRESS.matcher(tempEmail).matches()
    val isChanged = tempEmail != email

    if (showConfirmDialog && isEmailEditable && onEmailChange != null) {
        ConfirmEmailChangeDialog(
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                Toast.makeText(context, context.getString(R.string.changed_email), Toast.LENGTH_SHORT).show()
                // Usiamo l'operatore safe call ?.invoke() perché onEmailChange può essere null
                onEmailChange.invoke(tempEmail)
                showConfirmDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.Standard)
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.Standard)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.your_data), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(Dimens.Small))

            // Campi in Sola Lettura
            ReadOnlyField(label = stringResource(R.string.name), value = name)
            ReadOnlyField(label = stringResource(R.string.skin_type), value = skinType)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimens.Standard),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )

            // Campo Email (Dinamico: editabile o bloccato)
            OutlinedTextField(
                value = tempEmail,
                onValueChange = { if (isEmailEditable) tempEmail = it },
                label = { Text(stringResource(R.string.email_)) },
                enabled = isEmailEditable, // 👈 Blocca l'interazione se l'utente ha Google
                isError = !isEmailValid && tempEmail.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(0.9f),
                singleLine = true,
                supportingText = {
                    if (!isEmailValid && tempEmail.isNotEmpty()) {
                        Text(stringResource(R.string.email_not_valid), color = MaterialTheme.colorScheme.error)
                    } else if (!isEmailEditable) {
                        Text(
                            text = stringResource(R.string.email_google),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!isEmailVerified) {
                        val annotatedString = buildAnnotatedString {
                            append(stringResource(R.string.email_not_verified))
                            val link = LinkAnnotation.Clickable(
                                tag = "verify_email",
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ),
                                linkInteractionListener = { onVerifyClick() }
                            )
                            withLink(link) {
                                append(stringResource(R.string.send_verification))
                            }
                        }
                        Text(text = annotatedString, style = MaterialTheme.typography.bodySmall)
                    } else {
                        // 👈 Mantiene lo spazio riservato senza mostrare nulla se tutto è ok
                        Text(text = "", style = MaterialTheme.typography.bodySmall)
                    }
                },
                trailingIcon = {
                    if (isEmailEditable) {
                        // Se è modificabile ed è cambiata, mostra la spunta di salvataggio
                        if (isChanged && isEmailValid) {
                            IconButton(onClick = { showConfirmDialog = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_save), // Sostituisci eventualmente con un'icona di spunta/save
                                    contentDescription = stringResource(R.string.save),
                                    tint = MaterialTheme.appColors.syncDone
                                )
                            }
                        }
                    } else {
                        // Se NON è modificabile, mostra un'icona di lucchetto o info
                        Icon(
                            painter = painterResource(R.drawable.ic_lock), // Sostituisci con un ic_lock se ce l'hai
                            contentDescription = stringResource(R.string.not_edible),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun ConfirmEmailChangeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    // Qui andrebbe la logica per verificare la password reale (es. dal ViewModel)
    // Per ora simuliamo una verifica semplice o una lunghezza minima
    val isPasswordValid = password.length >= 6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_email)) },
        text = {
            Column {
                Text(stringResource(R.string.confirm_email_question))
                Spacer(modifier = Modifier.height(Dimens.Standard))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isPasswordValid
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.email_undo))
            }
        }
    )
}

@Composable
fun ReadOnlyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Etichetta piccola e leggermente sbiadita (stile Material Design)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(Dimens.ExtraSmall))

        // Valore principale in evidenza
        Text(
            text = value.ifEmpty { stringResource(R.string.unspecified) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
    }
}
