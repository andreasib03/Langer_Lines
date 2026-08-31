package com.example.linee_langer.ui.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.linee_langer.R
import com.example.linee_langer.core.database.entity.NotificationItem
import com.example.linee_langer.ui.theme.CameraOverlayBg
import com.example.linee_langer.ui.theme.CameraOverlayText
import com.example.linee_langer.ui.theme.appColors
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import com.example.linee_langer.ui.navigation.LocalNavController
import com.example.linee_langer.ui.theme.BadgeCountTextStyle
import com.example.linee_langer.ui.theme.Dimens



@Composable
fun OpenCvUnavailableBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.RadiusStandard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.qualityLow.copy(alpha = 0.92f)
        ),
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.onError.copy(0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.Standard, vertical = Dimens.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Medium)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = "",
                tint = CameraOverlayText,
                modifier = Modifier.size(Dimens.XLarge)
            )
            Column {
                Text(
                    text = stringResource(R.string.analysis_unavailable),
                    color = CameraOverlayText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.analysis_engine_missing),
                    color = CameraOverlayText.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LangerTopAppBar(
    title: String,
    unreadCount: Int, // Riceve il numero
    hasUnreadNotifications: Boolean, // <--- external state
    onNotificationClick: () -> Unit,
    canNavigateBack: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )},
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        // Usa l'icona freccia predefinita o la tua ic_back
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.size(Dimens.IconXLarge)) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.offset(x = Dimens.Negative, y = Dimens.CardElevation)
                            ) {
                                // Se sono troppe (es. > 9), puoi mostrare "9+"
                                Text(
                                    text = if (unreadCount > 9) stringResource(R.string.notification_count_overflow) else unreadCount.toString(),
                                    style = BadgeCountTextStyle,
                                    modifier = Modifier.padding(horizontal = Dimens.BorderStandard)
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bell),
                        contentDescription = stringResource(R.string.notifications),
                        modifier = Modifier.size(Dimens.XXLarge)
                    )
                }
            }
        }
    )
}


@Composable
fun PermissionPermanentlyDeniedUI(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraOverlayBg)
            .padding(Dimens.XXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = "",
            tint = CameraOverlayText.copy(alpha = 0.5f),
            modifier = Modifier.size(Dimens.ThumbnailSize)
        )
        Spacer(modifier = Modifier.height(Dimens.XLarge))
        Text(
            text = stringResource(R.string.permission_blocked),
            color = CameraOverlayText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.Small))
        Text(
            text = stringResource(R.string.permission_blocked_desc),
            color = CameraOverlayText.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.XXLarge))
        Button(onClick = onOpenSettings) {
            Text(stringResource(R.string.open_settings))
        }
    }
}
@Composable
fun PermissionDeniedUI(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraOverlayBg)
            .padding(Dimens.XXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = "",
            tint = CameraOverlayText,
            modifier = Modifier.size(Dimens.ThumbnailSize)
        )
        Spacer(modifier = Modifier.height(Dimens.XLarge))
        Text(
            text = stringResource(R.string.camera_permission_title),
            color = CameraOverlayText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.Small))
        Text(
            text = stringResource(R.string.camera_permission_body),
            color = CameraOverlayText.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.XXLarge))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.grant_permission))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LangerScaffold(
    title: String,
    notificationViewModel: NotificationViewModel,
    canNavigateBack: Boolean = false,
    onBackClick: () -> Unit = {}, // <--- Add this parameter
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
    ,
) {
    val sheetState = rememberModalBottomSheetState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val hasUnread by notificationViewModel.hasUnread.collectAsState()
    var showBottomSheet by remember{ mutableStateOf(false) }
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    val navController = LocalNavController.current


    if(showBottomSheet){
        ModalBottomSheet(
            onDismissRequest = {
                notificationViewModel.markAllAsRead()
                showBottomSheet = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle()}
        ) {
            NotificationItems(
                notifications = notifications,
                onDelete = {notification -> notificationViewModel.deleteNotification(notification)},
                onItemClick = { notification ->
                    notificationViewModel.onNotificationClicked(notification)
                    if (!notification.targetRoute.isNullOrBlank()) {
                        navController.navigate(notification.targetRoute) {
                            launchSingleTop = true
                        }
                        showBottomSheet = false
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            LangerTopAppBar(
                title = title,
                hasUnreadNotifications = hasUnread,
                onNotificationClick = { showBottomSheet = true },
                canNavigateBack = canNavigateBack,
                unreadCount = unreadCount,
                onBackClick = onBackClick
            )
        },
        snackbarHost = {
            if(snackbarHostState != null){
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        floatingActionButton = floatingActionButton,
        content = content
    )
}

@Composable
private fun NotificationItems(
    notifications: List<NotificationItem>,
    onDelete: (NotificationItem) -> Unit,
    onItemClick: (NotificationItem) -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f) // 60% of the window
    ) {
        Text(
            text = stringResource(R.string.notifications),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(Dimens.Standard)
        )

        HorizontalDivider()

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_notifications))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = notifications,
                    key = { it.id } // ID for smooth animations
                ) { notification ->

                    // --- LOGIC SWIPE-TO-DISMISS ---
                    val dismissState = rememberSwipeToDismissBoxState()

                    // react when lo swipe is completed
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(notification)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false, // Solo swipe verso left
                        backgroundContent = {
                            val color = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = Dimens.Large),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_trash),
                                    contentDescription = stringResource(R.string.delete_general),
                                    tint = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    ) {
                        // Content della card
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            NotificationCard(notification, onClick = { onItemClick(notification) })
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Dimens.Standard),
                        thickness = Dimens.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }

}

@Composable
private fun NotificationCard(notification: NotificationItem, onClick: () -> Unit){
    val isClickable = !notification.targetRoute.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(Dimens.Standard),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Un piccolo indicator colored per le notifications non read
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(Dimens.Small)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(Dimens.Medium))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = notification.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = notification.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isClickable) {
            Spacer(modifier = Modifier.width(Dimens.Small))
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(Dimens.IconSmall)
                    .graphicsLayer { rotationZ = 180f }
            )
        }
    }
}