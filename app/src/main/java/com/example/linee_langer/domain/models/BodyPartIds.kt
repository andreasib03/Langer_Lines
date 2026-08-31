package com.example.linee_langer.domain.models

object BodyPartIds {
    const val FACE      = "face"
    const val ARMS      = "arms"
    const val LEGS      = "legs"
    const val HANDS     = "hands"
    const val CHEST     = "chest"
    const val ABDOMEN   = "abdomen"
    const val FOREHEAD  = "forehead"
    const val CHEEK     = "cheek"
    const val DEFAULT   = "face"

    val ALL = setOf(
        FACE,
        ARMS,
        LEGS,
        HANDS,
        CHEST,
        ABDOMEN,
        FOREHEAD,
        CHEEK
    )

    fun isValid(id: String): Boolean = ALL.contains(id.lowercase())
}