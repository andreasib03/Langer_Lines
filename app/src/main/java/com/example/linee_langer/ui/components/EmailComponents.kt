package com.example.linee_langer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.linee_langer.R

@Composable
fun SupportDialog(
    onDismiss: () -> Unit,
    onSend: (subject: String, body: String) -> Unit
) {
        var userSubject by remember { mutableStateOf("") }
        var userMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(R.string.problem), style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.email),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    // Campo object
                    OutlinedTextField(
                        value = userSubject,
                        onValueChange = { userSubject = it },
                        label = { Text(stringResource(R.string.email_object)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Campo description
                    OutlinedTextField(
                        value = userMessage,
                        onValueChange = { userMessage = it },
                        label = { Text(stringResource(R.string.email_body)) },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text(stringResource(R.string.email_body2)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSend (userSubject, userMessage) },
                    enabled = userSubject.isNotBlank() && userMessage.isNotBlank()
                ) {
                    Text(stringResource(R.string.email_send))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.email_undo))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
}