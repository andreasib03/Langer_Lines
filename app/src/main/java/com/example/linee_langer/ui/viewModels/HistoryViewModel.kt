package com.example.linee_langer.ui.viewModels

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.dao.AnalysisWithLines
import com.example.linee_langer.data.AnalysisRepository
import com.example.linee_langer.data.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repositorAnalysis: AnalysisRepository,
    private val repositoryNotification: NotificationRepository
) : ViewModel() {

    val history = repositorAnalysis.allAnalyses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalAnalyses = repositorAnalysis.analysisCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val lastAnalysis = history.map { list ->
        list.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun deleteAnalysis(analysisWithLines: AnalysisWithLines, snackbarHostState: SnackbarHostState){
        viewModelScope.launch {
            try {

                withContext(Dispatchers.IO) {
                    repositorAnalysis.deleteFullAnalysis(analysisWithLines.analysis)
                }

                val result = snackbarHostState.showSnackbar(
                    message = "Analisi del ${analysisWithLines.analysis.bodyPartId} eliminata",
                    actionLabel = "Annulla",
                    duration = SnackbarDuration.Short
                )

                if(result == SnackbarResult.ActionPerformed){
                    repositorAnalysis.saveFullAnalysis(
                        analysisWithLines.analysis,
                        analysisWithLines.lines
                    )
                } else {
                    repositoryNotification.addNotification(
                        title = "Analisi eliminata",
                        description = "L'analisi del ${analysisWithLines.analysis.bodyPartId} è stata rimossa."
                    )
                }


            } catch (e: Exception){
                Log.e("View model", "error deleting analysis", e)
            }
        }
    }

}