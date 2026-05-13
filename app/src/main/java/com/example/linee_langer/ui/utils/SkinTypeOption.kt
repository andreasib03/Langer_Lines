package com.example.linee_langer.ui.utils
import com.example.linee_langer.R
import com.example.linee_langer.db.SkinAnalysisEntry
import com.example.linee_langer.logic.SkinTypeIds
import java.text.SimpleDateFormat
import java.util.Locale

data class SkinTypeOption(
    val id: String,
    val title: Int,
    val description: Int,
    val icon: Int
)

val skinOptions = listOf(
    SkinTypeOption(SkinTypeIds.DRY, R.string.secca, R.string.description_secca, R.drawable.ic_star),
    SkinTypeOption(SkinTypeIds.OILY, R.string.grassa, R.string.description_grassa, R.drawable.ic_star),
    SkinTypeOption(SkinTypeIds.MIXED, R.string.mista, R.string.description_mista, R.drawable.ic_star),
    SkinTypeOption(SkinTypeIds.NORMAL, R.string.normale, R.string.description_normale, R.drawable.ic_star)
)