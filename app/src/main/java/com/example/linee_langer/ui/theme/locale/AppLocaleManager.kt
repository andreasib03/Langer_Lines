package com.example.linee_langer.ui.theme.locale


import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLocaleManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * Applica la lingua scelta.
     * - API 33+: usa il nuovo LocaleManager (per-app language, nessun riavvio)
     * - API < 33: usa AppCompatDelegate (aggiorna la configurazione dell'activity)
     */
    fun applyLocale(locale: SupportedLocale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                .applicationLocales = LocaleList.forLanguageTags(locale.tag)
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(locale.tag)
            )
        }
    }

    /**
     * Restituisce la lingua attualmente attiva nell'app.
     */
    fun currentLocale(): SupportedLocale {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                .applicationLocales[0]?.toLanguageTag()
        } else {
            AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
        }
        return SupportedLocale.fromTag(tag?.substringBefore("-") ?: "it")
    }
}