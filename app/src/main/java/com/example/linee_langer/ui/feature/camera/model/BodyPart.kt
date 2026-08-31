package com.example.linee_langer.ui.feature.camera.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.linee_langer.R
import com.example.linee_langer.domain.models.BodyPartIds

data class BodyPart(
    val id: String,    // Es: "face", "arms"
    val name: Int,      // Riferimento a stringa: R.string.face
    val icon: Int       // Riferimento a drawable: R.drawable.ic_face
)

val bodyPartsList = listOf(
    BodyPart(BodyPartIds.FACE, R.string.face, R.drawable.ic_face),
    BodyPart(BodyPartIds.ARMS, R.string.arms, R.drawable.ic_arms),
    BodyPart(BodyPartIds.LEGS, R.string.legs, R.drawable.ic_legs),
    BodyPart(BodyPartIds.HANDS, R.string.hands, R.drawable.ic_hands),
    BodyPart(BodyPartIds.FOREHEAD, R.string.forehead, R.drawable.ic_forehead),
    BodyPart(BodyPartIds.CHEEK, R.string.cheek, R.drawable.ic_cheek),
    BodyPart(BodyPartIds.CHEST, R.string.chest, R.drawable.ic_chest),
    BodyPart(BodyPartIds.ABDOMEN, R.string.abdomen, R.drawable.ic_abdomen)
)

@Composable
fun bodyPartLabel(bodyPartId: String): String {
    val part = bodyPartsList.find { it.id == bodyPartId.lowercase() }
    return if (part != null) stringResource(part.name) else bodyPartId
}

data class QualityInfo(val label: String, val color: Color, val icon: Int)