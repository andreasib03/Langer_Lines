package com.example.linee_langer.ui.theme.locale


import androidx.annotation.StringRes
import com.example.linee_langer.R

enum class SupportedLocale(
    val tag: String,          // BCP-47 language tag
    @param:StringRes val labelRes: Int,
    val flagEmoji: String,
) {
    ITALIAN(tag = "it", labelRes = R.string.language_italian, flagEmoji = "🇮🇹"),
    ENGLISH(tag = "en", labelRes = R.string.language_english, flagEmoji = "🇬🇧"),
    DEUTSCHLAND(tag = "de", labelRes = R.string.language_deutschland, flagEmoji = "\uD83C\uDDE9\uD83C\uDDEA"),
    FRENCH(tag = "fr", labelRes = R.string.language_french, flagEmoji = "\uD83C\uDDEB\uD83C\uDDF7"),
    JAPANESE(tag = "ja", labelRes = R.string.language_japanese, flagEmoji = "\uD83C\uDDEF\uD83C\uDDF5");

    companion object {
        val default = ITALIAN

        fun fromTag(tag: String): SupportedLocale =
            entries.firstOrNull { it.tag == tag } ?: default
    }
}