package com.example.linee_langer.ui.feature.home

import androidx.lifecycle.ViewModel
import com.example.linee_langer.ui.shared.utils.DailyAdvice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import com.example.linee_langer.R
import com.example.linee_langer.ui.shared.utils.AdviceCategory
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import kotlin.random.Random


@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel(){

    private val _todayAdvices = MutableStateFlow<List<DailyAdvice>>(emptyList())
    val todayAdvices = _todayAdvices.asStateFlow()


    private val allAdvices = listOf(
        DailyAdvice(1, R.string.hydratation, R.string.desc_hydratation,R.drawable.ic_settings,AdviceCategory.HYDRATION),
        DailyAdvice(2, R.string.protection,  R.string.desc_protection,R.drawable.ic_profile,AdviceCategory.PROTECTION),
        DailyAdvice(3, R.string.lines_massage, R.string.lines_massage_desc,R.drawable.ic_camera,AdviceCategory.MASSAGE),
        DailyAdvice(4, R.string.detergency, R.string.detergency_desc, R.drawable.ic_profile,AdviceCategory.CLEANSING),
        DailyAdvice(5, R.string.eyeliner, R.string.eyeliner_desc,R.drawable.ic_settings,AdviceCategory.OTHER),
        DailyAdvice(6, R.string.water, R.string.water_desc, R.drawable.ic_home,AdviceCategory.HYDRATION),
        DailyAdvice(7, R.string.massage, R.string.massage_desc,R.drawable.ic_camera,AdviceCategory.MASSAGE)
    )

    init {
        generateAdvicesForToday()
    }

    fun generateAdvicesForToday() {
        // Recuperiamo il giorno dell'anno corrente (un numero da 1 a 365)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        // Usiamo il giorno dell'anno per mescolare la lista originaria sempre nello stesso modo durante tutto il giorno
        // .shuffled(java.util.Random(seed)) garantisce che lo "shuffle" sia identico per lo stesso giorno
        val shuffledList = allAdvices.shuffled(Random(dayOfYear.toLong()))

        // Prendiamo i primi 3 consigli estratti
        _todayAdvices.value = shuffledList.take(3)
    }
}

