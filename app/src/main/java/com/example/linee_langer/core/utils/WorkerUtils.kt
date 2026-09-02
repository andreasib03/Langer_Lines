package com.example.linee_langer.core.utils

object WorkerUtils{
    /**
     * Restituisce true se il path è già un URL remoto HTTPS generico.
     * Evita di ricaricare immagini già presenti su cloud.
     */
    fun isRemoteUrl(path: String): Boolean =
        path.startsWith("https://")
}