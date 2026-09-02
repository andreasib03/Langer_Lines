package com.example.linee_langer.ui.feature.profile.components

import android.net.Uri
import android.util.Patterns
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
            contentDescription = title,
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
            contentDescription = stringResource(R.string.back),
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
                contentDescription = stringResource(R.string.new_scan),
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
    name: String,
    email: String,
    skinType: String,
    isEmailEditable: Boolean,
    isEmailVerified: Boolean,
    onVerifyClick: () -> Unit,
    onCredentialsChange: ((newEmail: String?, newPassword: String?, currentPassword: String) -> Unit)?
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog && isEmailEditable && onCredentialsChange != null) {
        EditCredentialsDialog(
            currentEmail = email,
            onDismiss = { showEditDialog = false },
            onConfirm = { newEmail, newPassword, currentPassword ->
                onCredentialsChange(newEmail, newPassword, currentPassword)
                showEditDialog = false
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

            // Campo Email (Sempre Read-Only visivamente, ma con pulsante Modifica se permesso)
            OutlinedTextField(
                value = email,
                onValueChange = { },
                label = { Text(stringResource(R.string.email_)) },
                readOnly = true,
                enabled = true,
                modifier = Modifier.fillMaxWidth(0.9f),
                singleLine = true,
                supportingText = {
                    if (!isEmailEditable) {
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
                                append(" " + stringResource(R.string.send_verification))
                            }
                        }
                        Text(text = annotatedString, style = MaterialTheme.typography.bodySmall)
                    }
                },
                trailingIcon = {
                    if (isEmailEditable) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_save), // Icona edit/penna sarebbe meglio se disponibile
                                contentDescription = stringResource(R.string.changed_email),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
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
fun EditCredentialsDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (newEmail: String?, newPassword: String?, currentPassword: String) -> Unit
) {
    var newEmail by remember { mutableStateOf(currentEmail) }
    var newPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }

    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()
    val isFormValid = currentPassword.length >= 6 && isEmailValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_credentials)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Standard)) {
                Text(stringResource(R.string.confirm_changes))

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text(stringResource(R.string.new_email)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isEmailValid && newEmail.isNotEmpty()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.Small))

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.current_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.confirm_email_question)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalEmail = if (newEmail != currentEmail) newEmail else null
                    val finalPassword = newPassword.ifBlank { null }
                    onConfirm(finalEmail, finalPassword, currentPassword)
                },
                enabled = isFormValid
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
