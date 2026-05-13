package com.example.linee_langer.domain.models

data class UserFirebaseModel(
    val email: String = "",
    val name: String = "",
    val eta: String = "",
    val skinType: String = "",
    val goalId: Int = 0
)
