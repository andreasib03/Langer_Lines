package com.example.linee_langer.ui.feature.camera.utils

import android.content.Context
import android.graphics.Bitmap
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import com.example.linee_langer.core.utils.toEntity
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.domain.models.LangerLine
import com.example.linee_langer.core.utils.saveBitmapToGallery
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.domain.models.BodyPartIds
import com.example.linee_langer.domain.models.TensionLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisPersistenceHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val analysisRepository: AnalysisRepository,
    private val authRepository: AuthRepository
) {

    /**
     * Salva bitmap + linee: prima su disco (gallery), poi su Room.
     * Lancia eccezione se uno dei due step fallisce.
     *
     * @return URI pubblica dell'immagine salvata
     */
    suspend fun save(
        date: Long,
        bitmap: Bitmap,
        lines: List<LangerLine>,
        bodyPartId: String
    ): String = withContext(Dispatchers.IO) {
        val uri = saveBitmapToGallery(context, bitmap, date)
            ?: throw IllegalStateException("Impossibile scrivere l'immagine nella galleria")

        saveToDatabase(date, uri.toString(), lines, bodyPartId)

        uri.toString()
    }

    /**
     * Salva solo su Room (path già noto, es. dopo upload Worker).
     */
    suspend fun saveToDatabase(
        date: Long,
        path: String,
        lines: List<LangerLine>,
        bodyPartId: String
    ) = withContext(Dispatchers.IO) {
        val uid = authRepository.currentUser?.uid.orEmpty()
        val effectivePartId = bodyPartId.ifBlank { BodyPartIds.DEFAULT }
        val analysis = SkinAnalysisEntity(
            date = date,
            bodyPartId = effectivePartId,
            imagePath = path,
            resultSummary = buildResultSummary(lines, effectivePartId),
            userId = uid
        )
        val entities = lines.map { it.toEntity() }
        analysisRepository.saveFullAnalysis(analysis, entities)
    }

    private fun buildResultSummary(
        lines: List<LangerLine>,
        bodyPartId: String): String {
        val avgIntensity = if (lines.isNotEmpty())
            lines.map { it.intensity }.average().toFloat()
        else 0f

        val tensionLevel = TensionLevel.fromAvgIntensity(avgIntensity)

        return JSONObject().apply {
            put("lineCount",    lines.size)
            put("avgIntensity", avgIntensity.toDouble())
            put("tensionLevel", tensionLevel)
            put("bodyPart",     bodyPartId)
        }.toString()
    }

}