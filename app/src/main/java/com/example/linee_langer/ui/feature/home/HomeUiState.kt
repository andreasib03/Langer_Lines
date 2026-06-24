package com.example.linee_langer.ui.feature.home

import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.ui.shared.utils.DailyAdvice

sealed interface HomeUiState {
    data class Success(
        val name: String,
        val skinType: String,
        val lastAnalysis: AnalysisWithLines?,
        val advices: List<DailyAdvice>
    ) : HomeUiState
}