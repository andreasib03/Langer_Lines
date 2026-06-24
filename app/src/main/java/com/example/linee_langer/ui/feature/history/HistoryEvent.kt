package com.example.linee_langer.ui.feature.history

import com.example.linee_langer.core.database.entity.AnalysisWithLines

sealed class HistoryEvent {
    data class ShowUndo(val item: AnalysisWithLines) : HistoryEvent()
}
