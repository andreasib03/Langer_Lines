package com.example.linee_langer.ui.feature.camera.model

import androidx.compose.ui.graphics.Color
import com.example.linee_langer.R

data class BodyPart(
    val id: String,    // Es: "face", "arms"
    val name: Int,      // Riferimento a stringa: R.string.face
    val icon: Int       // Riferimento a drawable: R.drawable.ic_face
)

val bodyPartsList = listOf(
    BodyPart("face", R.string.face, R.drawable.ic_face),
    BodyPart("arms", R.string.arms, R.drawable.ic_arms),
    BodyPart("legs", R.string.legs, R.drawable.ic_legs),
    BodyPart("hands", R.string.hands, R.drawable.ic_hands),
    BodyPart("chest", R.string.face, R.drawable.ic_face),
    BodyPart("abdomen", R.string.arms, R.drawable.ic_arms),
    BodyPart("forehead", R.string.legs, R.drawable.ic_legs),
    BodyPart("cheek", R.string.hands, R.drawable.ic_hands)
)

data class QualityInfo(val label: String, val color: Color, val icon: Int)