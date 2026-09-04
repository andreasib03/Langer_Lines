package com.example.linee_langer.ui.shared.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.linee_langer.R
import com.example.linee_langer.ui.theme.Dimens


@Composable
fun VersionFooter(version: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = Dimens.XXLarge, bottom = Dimens.Standard),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.version_label, version),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}