package com.example.linee_langer.ui.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.linee_langer.R
import com.example.linee_langer.ui.feature.history.components.EmptyHistoryPlaceholder
import com.example.linee_langer.ui.shared.components.LangerScaffold
import com.example.linee_langer.ui.feature.history.components.SwipeableHistoryCard
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import com.example.linee_langer.ui.theme.Dimens

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    notificationViewModel: NotificationViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit
) {
    // Suppose che il ViewModel expose: val history by repository.getAllAnalyses().collectAsState(initial = emptyList())
    val history by historyViewModel.history.collectAsState()
    val snackbarHostState = remember { SnackbarHostState()}

    val msgDeleted  = stringResource(R.string.history_deleted)
    val msgUndo     = stringResource(R.string.history_undo)

    LaunchedEffect(Unit) {
        historyViewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.ShowUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = msgDeleted,
                        actionLabel = msgUndo,
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        historyViewModel.restoreAnalysis(event.item)
                    }
                }
            }
        }
    }

    LangerScaffold(
        title = stringResource(R.string.analysis),
        notificationViewModel = notificationViewModel,
        canNavigateBack = true,
        onBackClick = onBack,
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        if (history.isEmpty()) {
            EmptyHistoryPlaceholder()
        } else {
            val totalCount = history.size
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(Dimens.Standard),
                verticalArrangement = Arrangement.spacedBy(Dimens.Standard)
            ) {
                itemsIndexed(
                    items = history,
                    key = {_, item -> item.analysis.id }
                ) { index, item ->
                    SwipeableHistoryCard(
                        analysis = item,
                        progressiveNumber = totalCount - index,
                        onDelete = { historyViewModel.deleteAnalysis(it) },
                        onClick = { onNavigateToDetail(item.analysis.id) }
                    )
                }
            }
        }
    }
}