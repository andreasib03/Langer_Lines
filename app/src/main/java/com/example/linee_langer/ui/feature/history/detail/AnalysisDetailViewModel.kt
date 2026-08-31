package com.example.linee_langer.ui.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.data.local.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalysisDetailViewModel @Inject constructor(
    private val repository: AnalysisRepository,
    savedStateHandle: SavedStateHandle
): ViewModel(){

    private val analysisId: Long =
        checkNotNull(savedStateHandle["analysisId"]){
            "analysisId navigation argument is missing — assicurati di passarlo nel route."
        }

    val analysis: StateFlow<AnalysisWithLines?> = repository.getAnalysisById(analysisId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

}