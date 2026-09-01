package com.example.linee_langer.domain.models

import com.google.firebase.firestore.PropertyName

data class UserFirebaseModel(
    val email: String = "",
    val name: String = "",
    val eta: String = "",
    val skinType: String = "",
    val goalId: List<String> = emptyList(),

    @get:PropertyName("imageBase64")
    val imageBase64: String? = null
)
