package com.example.linee_langer.ui.shared.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.linee_langer.ui.feature.settings.SettingsViewModel
import com.example.linee_langer.R
import com.example.linee_langer.ui.theme.Dimens

@Composable
fun ChevronRightIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_back),
        contentDescription = "",
        modifier = Modifier.rotate(180f).size(Dimens.Standard),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

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

fun handleNotificationToggle(
    context: Context,
    enabled: Boolean,
    viewModel: SettingsViewModel,
    launcher: ActivityResultLauncher<String>
) {
    if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (status == PackageManager.PERMISSION_GRANTED) {
                viewModel.toggleNotifications(true)
            } else {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.toggleNotifications(true)
        }
    } else {
        viewModel.toggleNotifications(false)
    }
}