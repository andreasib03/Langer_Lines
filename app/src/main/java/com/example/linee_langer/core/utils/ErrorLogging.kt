package com.example.linee_langer.core.utils

import android.util.Log
import kotlin.coroutines.cancellation.CancellationException

/**
 * Gestisce in modo centralizzato un'eccezione catturata da un blocco `catch` generico
 * all'interno di una coroutine.
 *
 * Comportamento:
 * - Se [e] è una [CancellationException] viene SEMPRE ripropagata. Non va mai trattata
 *   come un errore applicativo: ingoiarla rompe la cancellazione strutturata (es. un
 *   ViewModel distrutto mentre un'operazione è in corso non riuscirebbe più a
 *   propagare correttamente la cancellazione ai suoi figli).
 * - In tutti gli altri casi l'eccezione viene loggata con [tag] e [message], così il
 *   problema resta diagnosticabile (LogCat, e in futuro Crashlytics) invece di sparire
 *   silenziosamente in un `catch` vuoto.
 *
 * Uso tipico in un repository/ViewModel:
 * ```
 * catch (e: Exception) {
 *     logCaughtException(TAG, "Errore durante il salvataggio analisi", e)
 *     Result.failure(e)
 * }
 * ```
 *
 * Nota: questa funzione NON sostituisce i catch specifici già presenti per eccezioni
 * tipizzate (es. FirebaseAuthUserCollisionException) che vanno mappate a errori di
 * dominio distinti; va usata solo nel catch generico "di chiusura".
 */

fun logCaughtException(tag: String, message: String, e: Exception) {
    if (e is CancellationException) throw e
    Log.e(tag, message, e)
}