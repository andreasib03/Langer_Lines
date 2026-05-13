package com.example.linee_langer.ui.viewModels

import androidx.lifecycle.ViewModel
import com.example.linee_langer.ui.utils.DailyAdvice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import com.example.linee_langer.R
import com.example.linee_langer.dao.AnalysisWithLines
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar


@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel(){

    private val _todayAdvices = MutableStateFlow<List<DailyAdvice>>(emptyList())
    val todayAdvices = _todayAdvices.asStateFlow()


    private val allAdvices = listOf(
        DailyAdvice(1, "Idratazione", "Applica crema viso idratante", R.drawable.ic_settings, 0xFFE8F5E9),
        DailyAdvice(2, "Protezione", "Usa filtro SPF 50+ oggi", R.drawable.ic_profile, 0xFFFFF9C4),
        DailyAdvice(3, "Massaggio Lines", "Segui le linee Langer verso l'alto", R.drawable.ic_camera, 0xFFE1F5FE),
        DailyAdvice(4, "Detergenza", "Usa un detergente schiumogeno delicato", R.drawable.ic_profile, 0xFFFFE0B2),
        DailyAdvice(5, "Contorno Occhi", "Picchietta la crema dall'interno all'esterno", R.drawable.ic_settings, 0xFFF3E5F5),
        DailyAdvice(6, "Idratazione Profonda", "Bevi almeno 2 litri d'acqua oggi", R.drawable.ic_home, 0xFFE0F7FA),
        DailyAdvice(7, "Massaggio Fronte", "Massaggia distendendo dal centro alle tempie", R.drawable.ic_camera, 0xFFFCE4EC)
    )

    init {
        generateAdvicesForToday()
    }

    fun generateAdvicesForToday() {
        // Recuperiamo il giorno dell'anno corrente (un numero da 1 a 365)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        // Usiamo il giorno dell'anno per mescolare la lista originaria sempre nello stesso modo durante tutto il giorno
        // .shuffled(java.util.Random(seed)) garantisce che lo "shuffle" sia identico per lo stesso giorno
        val shuffledList = allAdvices.shuffled(java.util.Random(dayOfYear.toLong()))

        // Prendiamo i primi 3 consigli estratti
        _todayAdvices.value = shuffledList.take(3)
    }
}

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val name: String,
        val skinType: String,
        val lastAnalysis: AnalysisWithLines?,
        val advices: List<DailyAdvice>
    ) : HomeUiState
}