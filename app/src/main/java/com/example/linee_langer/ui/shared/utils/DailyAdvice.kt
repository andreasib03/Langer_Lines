package com.example.linee_langer.ui.shared.utils


data class DailyAdvice(
    val id: Int,
    val title: Int,
    val subtitle: Int,
    val icon: Int,
    val category: AdviceCategory
)

enum class AdviceCategory {
    HYDRATION, PROTECTION, MASSAGE, CLEANSING, OTHER
}