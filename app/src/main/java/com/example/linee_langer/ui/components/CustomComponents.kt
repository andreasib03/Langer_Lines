package com.example.linee_langer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.linee_langer.R
import com.example.linee_langer.db.NotificationItem
import com.example.linee_langer.ui.viewModels.NotificationViewModel


fun Modifier.slothShadow(
    color: Color = Color.Black,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    shape: Shape
) = this.drawBehind {
    drawIntoCanvas { canvas ->
    val paint = Paint()
    paint.color = color

    canvas.drawOutline(
        outline = shape.createOutline(
            size = size,
            layoutDirection = layoutDirection,
            density = this
        ),
        paint = paint
    )
}
}.offset(x = -offsetX, y = -offsetY)

@Composable
fun CardChoice(
    title: String,
    subtitle: String,
    description: String,
    icon: Int,
    isSelected: Boolean,
    onSelect: () -> Unit
){

    val backgroundColor = if (isSelected) Color(0xFFF3F3F3) else Color.White
    val borderColor = Color.Black
    val cornerRadius = 16.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .then(if (isSelected) Modifier.slothShadow(shape = shape) else Modifier)
            .background(backgroundColor, shape)
            .border(2.dp, borderColor, shape)
            .clickable { onSelect() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.Black
                    )
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                )
                Text(
                    text = description,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
            }

            // Custom Selection Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, Color.Black, CircleShape)
                    .padding(4.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Subtle Label above the field
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            // CUSTOM STYLING START
            shape = RoundedCornerShape(16.dp), // Modern rounded corners
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent, // No border when not typing for a cleaner look
                errorBorderColor = MaterialTheme.colorScheme.error
            ),
            isError = isError,
            keyboardOptions = keyboardOptions,
            singleLine = true
            // CUSTOM STYLING END
        )
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
            IconButton(onClick = onNotificationClick) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                // Se sono troppe (es. > 9), puoi mostrare "9+"
                                Text(if (unreadCount > 9) "9+" else unreadCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bell),
                        contentDescription = stringResource(R.string.notifications)
                    )
                }
            }
        }
    )
}

@Composable
fun PermissionDeniedUI(onClick: () -> Unit) {
// UI Denial permission
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.camera_screen_permission), color = Color.White)
        Button(
            onClick = { onClick() }
        ) {
            Text(stringResource(R.string.camera_screen_permission_success))
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
                onDelete = {notification -> notificationViewModel.deleteNotification(notification)}
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
private fun NotificationItems(notifications: List<NotificationItem>, onDelete: (NotificationItem) -> Unit){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f) // 60% of the window
    ) {
        Text(
            text = stringResource(R.string.notifications),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
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
                                SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_trash),
                                    contentDescription = stringResource(R.string.delete_general),
                                    tint = Color.White
                                )
                            }
                        }
                    ) {
                        // Content della card
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            NotificationCard(notification)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }

}

@Composable
private fun NotificationCard(notification: NotificationItem){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Un piccolo indicator colored per le notifications non read
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
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
    }
}

