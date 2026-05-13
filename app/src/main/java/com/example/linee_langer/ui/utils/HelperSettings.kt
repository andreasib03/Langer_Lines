package com.example.linee_langer.ui.utils

import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.linee_langer.ui.viewModels.SettingsViewModel
import com.example.linee_langer.R

@Composable
fun ChevronRightIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_back),
        contentDescription = null,
        modifier = Modifier.rotate(180f).size(16.dp),
        tint = Color.Gray
    )
}

@Composable
fun VersionFooter(version: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Versione $version", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

fun handleNotificationToggle(
    context: android.content.Context,
    enabled: Boolean,
    viewModel: SettingsViewModel,
    launcher: androidx.activity.result.ActivityResultLauncher<String>
) {
    if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            if (status == PackageManager.PERMISSION_GRANTED) {
                viewModel.toggleNotifications(true)
            } else {
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.toggleNotifications(true)
        }
    } else {
        viewModel.toggleNotifications(false)
    }
}