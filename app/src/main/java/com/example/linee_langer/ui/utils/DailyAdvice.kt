package com.example.linee_langer.ui.utils

data class DailyAdvice(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: Int,
    val colorHex: Long // Salviamo il colore come Long per gestirlo facilmente
)