package com.example.linee_langer.ui.feature.history.detail

import androidx.lifecycle.ViewModel
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.data.local.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AnalysisDetailViewModel @Inject constructor(
    private val repository: AnalysisRepository
): ViewModel(){

    fun getAnalysisById(id: Long): Flow<AnalysisWithLines?> {
        return repository.getAnalysisById(id)
    }
}