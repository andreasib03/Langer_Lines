package com.example.linee_langer.domain.detector

import android.graphics.Bitmap
import android.util.Log
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.domain.models.LangerLine
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


private const val TAG = "LangerDetector"
class LangerDetector : ILangerDetector {

    override var isAvailable: Boolean = false
        private set

    companion object {
        private val UNKNOWN_PRIOR = RegionPrior(0.0,0.0)
        private const val PRIOR_WEIGHT = 0.30

        private var libraryLoaded = false

        init {
            try {
                // "opencv_java4" è il nome standard della libreria nelle versioni 4.x
                // Se usi la 3.x, prova "opencv_java3"
                System.loadLibrary("opencv_java4")
                libraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Libreria nativa OpenCV (opencv_java4) non disponibile su questo device: rilevamento linee disabilitato", e)
            }
        }
    }

    init {
        isAvailable = libraryLoaded
    }

    private data class RegionPrior(val angleRad: Double, val anisotropy: Double)


    private val priors: Map<String, RegionPrior> = mapOf(
        // Face — primarily horizontal / slightly oblique
        "face"         to RegionPrior(Math.toRadians(0.0),   0.75),
        "forehead"     to RegionPrior(Math.toRadians(0.0),   0.75),
        "cheek"        to RegionPrior(Math.toRadians(30.0),  0.55),
        // Upper extremity — longitudinal along limb axis
        // NB: "arms"/"hands" qui DEVONO coincidere con i valori di BodyPartIds,
        // altrimenti il fallback su UNKNOWN_PRIOR annulla la conoscenza anatomica.
        "arms"         to RegionPrior(Math.toRadians(90.0),  0.67),
        "hands"        to RegionPrior(Math.toRadians(90.0),  0.50),
        // Trunk
        "chest"        to RegionPrior(Math.toRadians(0.0),   0.65),
        "abdomen"      to RegionPrior(Math.toRadians(0.0),   0.60),
        // Lower extremity
        "legs"         to RegionPrior(Math.toRadians(90.0),  0.67)
    )

    /**
     * @param sensitivity: 0.0 to 1.0 (Lower = stricter/fewer lines, Higher = more detail)
     * @param partId: The body part being analyzed to adjust grid density
     */
    override fun detectLines(
        bitmap: Bitmap,
        sensitivity: Float,
        partId: String
    ): List <LangerLine> {

        if (!isAvailable) {
            return emptyList()
        }

        val mat = Mat()
        val hsv = Mat()
        val skinMask = Mat()
        val gray = Mat()
        val gradX = Mat()
        val gradY = Mat()
        val jxx = Mat()
        val jxy = Mat()
        val jyy = Mat()
        val morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))

        try {
            Utils.bitmapToMat(bitmap, mat)
            // 1. Convert bitmap to grayscale/process pixels

            val rows = mat.rows()
            val cols = mat.cols()

            if (rows == 0 || cols == 0) {
                mat.release()
                return emptyList()
            }


            // ── 1. Skin mask (HSV range) ──────────────────────────────────────────
            Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_RGB2HSV)
            Core.inRange(hsv, Scalar(0.0, 30.0, 40.0), Scalar(25.0, 180.0, 255.0), skinMask)

            // ── 2. Preprocess to grayscale ────────────────────────────────────────

            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.createCLAHE(2.0, Size(8.0, 8.0)).apply(gray, gray)

            val tempGray = Mat()
            gray.copyTo(tempGray)
            Imgproc.bilateralFilter(tempGray, gray, 9, 75.0, 75.0)
            tempGray.release()

            Imgproc.morphologyEx(gray, gray, Imgproc.MORPH_OPEN, morphKernel)

            // ── 3. Structure tensor via Scharr ────────────────────────────────────
            //
            //   J = Gσ * [ Ix²    Ix·Iy ]
            //             [ Ix·Iy  Iy²   ]
            //
            Imgproc.Scharr(gray, gradX, CvType.CV_32F, 1, 0)
            Imgproc.Scharr(gray, gradY, CvType.CV_32F, 0, 1)

            Core.multiply(gradX, gradX, jxx)
            Core.multiply(gradX, gradY, jxy)
            Core.multiply(gradY, gradY, jyy)

            val blurSize = Size(15.0, 15.0)
            val tenorSigma = 3.0
            Imgproc.GaussianBlur(jxx, jxx, blurSize, tenorSigma)
            Imgproc.GaussianBlur(jxy, jxy, blurSize, tenorSigma)
            Imgproc.GaussianBlur(jyy, jyy, blurSize, tenorSigma)

            return computeLangerLines(
                rows, cols, jxx, jxy, jyy, skinMask,
                sensitivity, partId.lowercase()
            )
        } catch (e: Exception) {
            logCaughtException(TAG, "Rilevamento linee di Langer fallito (partId=$partId)", e)
            return emptyList()
        } finally {
            listOf(mat, hsv, skinMask, gray, gradX, gradY, jxx, jxy, jyy, morphKernel).forEach {
                if (!it.empty()) it.release()
            }
        }
    }

    private fun computeLangerLines(
        rows: Int, cols: Int,
        jxx: Mat, jxy: Mat, jyy: Mat,
        skinMask: Mat,
        sensitivity: Float,
        partId: String
    ): List<LangerLine> {
        val lines = mutableListOf<LangerLine>()

        // Accesso ai dati tramite array per massimizzare le performance del loop
        val jxxArr = FloatArray(rows * cols).also { jxx.get(0, 0, it) }
        val jxyArr = FloatArray(rows * cols).also { jxy.get(0, 0, it) }
        val jyyArr = FloatArray(rows * cols).also { jyy.get(0, 0, it) }
        val maskArr = ByteArray(rows * cols).also { skinMask.get(0, 0, it) }

        val prior = priors[partId] ?: UNKNOWN_PRIOR
        val step = if (partId == "face") 45 else 55
        val minCoherence = 0.08 + (1.0f - sensitivity) * 0.42f
        val aspect = cols.toFloat() / rows.toFloat()

        for (y in step until rows - step step step) {
            for (x in step until cols - step step step) {
                val idx = y * cols + x

                // Salta pixel non appartenenti alla pelle
                if ((maskArr[idx].toInt() and 0xFF) == 0) continue

                val a = jxxArr[idx].toDouble()
                val b = jxyArr[idx].toDouble()
                val c = jyyArr[idx].toDouble()

                // Autovalori della matrice simmetrica 2x2
                val halfTrace = (a + c) * 0.5
                val disc = sqrt(((a - c) * 0.5).pow(2) + b.pow(2))
                val lambda1 = halfTrace + disc
                val lambda2 = halfTrace - disc

                if (lambda1 + lambda2 < 1e-6) continue

                // Coerenza di Weickert: definisce quanto la direzione è marcata
                val coherence = ((lambda1 - lambda2) / (lambda1 + lambda2)).pow(2)
                if (coherence < minCoherence) continue

                // Direzione Langer: perpendicolare all'autovettore del gradiente massimo
                val stressAngle = atan2(lambda1 - a, b)
                val langerAngle = wrapAngle(stressAngle + PI / 2.0)

                // Fusione con conoscenza anatomica a priori (Circular Mean)
                val wImg = (1.0 - PRIOR_WEIGHT) * coherence
                val wPrior = PRIOR_WEIGHT * prior.anisotropy
                val sinF = wImg * sin(2 * langerAngle) + wPrior * sin(2 * prior.angleRad)
                val cosF = wImg * cos(2 * langerAngle) + wPrior * cos(2 * prior.angleRad)
                val fusedAngle = wrapAngle(atan2(sinF, cosF) / 2.0)

                // Mapping coordinate normalizzate [0, 1] per la UI
                val normX = x.toFloat() / cols
                val normY = y.toFloat() / rows
                val length = (0.015 + coherence * 0.020).coerceIn(0.012, 0.035).toFloat()

                val dx = (cos(fusedAngle).toFloat() * length) / aspect
                val dy = sin(fusedAngle).toFloat() * length

                lines.add(
                    LangerLine(
                        startX = normX - dx,
                        startY = normY - dy,
                        endX = normX + dx,
                        endY = normY + dy,
                        intensity = coherence.toFloat().coerceIn(0.3f, 0.95f)
                    )
                )
            }
        }
        return lines
    }


    private fun wrapAngle(fi: Double): Double {
        var a = fi % PI
        if (a < 0) a += PI
        return a
    }


}