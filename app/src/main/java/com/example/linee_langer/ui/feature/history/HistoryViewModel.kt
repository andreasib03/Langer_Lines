package com.example.linee_langer.ui.feature.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repositorAnalysis: AnalysisRepository,
    private val repositoryNotification: NotificationRepository
) : ViewModel() {

    val history = repositorAnalysis.allAnalyses
        .onEach {
            list -> Log.d("HistoryVM", "Ricevute ${list.size} analisi dal DB")
        }
        .stateIn(
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

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events = _events.asSharedFlow()


    fun deleteAnalysis(analysisWithLines: AnalysisWithLines){
        viewModelScope.launch {
            try {

                withContext(Dispatchers.IO) {
                    repositorAnalysis.deleteFullAnalysis(analysisWithLines.analysis)
                }
                _events.emit(HistoryEvent.ShowUndo(analysisWithLines))
            } catch (e: Exception){
                Log.e("View model", "error deleting analysis", e)
            }
        }
    }

    fun restoreAnalysis(analysisWithLines: AnalysisWithLines) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repositorAnalysis.restoreFullAnalysis(analysisWithLines)
                }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Errore ripristino analisi", e)
            }
        }
    }

}