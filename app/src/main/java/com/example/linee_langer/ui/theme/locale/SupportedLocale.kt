package com.example.linee_langer.ui.theme.locale


import androidx.annotation.StringRes
import com.example.linee_langer.R

enum class SupportedLocale(
    val tag: String,          // BCP-47 language tag
    @param:StringRes val labelRes: Int,
    val flagEmoji: String,
) {
    ITALIAN(tag = "it", labelRes = R.string.language_italian, flagEmoji = "🇮🇹"),
    ENGLISH(tag = "en", labelRes = R.string.language_english, flagEmoji = "🇬🇧");

    companion object {
        val default = ITALIAN

        fun fromTag(tag: String): SupportedLocale =
            entries.firstOrNull { it.tag == tag } ?: default
    }
}