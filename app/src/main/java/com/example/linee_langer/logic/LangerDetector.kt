package com.example.linee_langer.logic

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import com.example.linee_langer.domain.models.LangerLine
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


//math/openCV/ML logic
class LangerDetector {

    companion object {
        init {
            try {
                // "opencv_java4" è il nome standard della libreria nelle versioni 4.x
                // Se usi la 3.x, prova "opencv_java3"
                System.loadLibrary("opencv_java4")
                Log.d("OpenCV", "Libreria caricata con System.loadLibrary")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("OpenCV", "${e.message}")
            }
        }
    }

    private data class RegionPrior(val angleRad: Double, val anisotropy: Double)

    private val PRIORS: Map<String, RegionPrior> = mapOf(
        // Face — primarily horizontal / slightly oblique
        "face"         to RegionPrior(Math.toRadians(0.0),   0.75),
        "forehead"     to RegionPrior(Math.toRadians(0.0),   0.75),
        "cheek"        to RegionPrior(Math.toRadians(30.0),  0.55),
        "chin"         to RegionPrior(Math.toRadians(0.0),   0.60),
        "neck"         to RegionPrior(Math.toRadians(0.0),   0.70),
        // Upper extremity — longitudinal along limb axis
        "upper_arm"    to RegionPrior(Math.toRadians(90.0),  0.65),
        "forearm"      to RegionPrior(Math.toRadians(90.0),  0.70),
        "hand"         to RegionPrior(Math.toRadians(90.0),  0.50),
        // Trunk
        "chest"        to RegionPrior(Math.toRadians(0.0),   0.65),
        "abdomen"      to RegionPrior(Math.toRadians(0.0),   0.60),
        "back_upper"   to RegionPrior(Math.toRadians(10.0),  0.55),
        "back_lower"   to RegionPrior(Math.toRadians(30.0),  0.60),
        // Lower extremity
        "thigh"        to RegionPrior(Math.toRadians(90.0),  0.65),
        "lower_leg"    to RegionPrior(Math.toRadians(90.0),  0.70),
        "foot"         to RegionPrior(Math.toRadians(0.0),   0.50)
    )

    private val UNKNOWN_PRIOR = RegionPrior(0.0,0.0)

    private val PRIOR_WEIGHT = 0.30

    /**
     * @param sensitivity: 0.0 to 1.0 (Lower = stricter/fewer lines, Higher = more detail)
     * @param partId: The body part being analyzed to adjust grid density
     */
    fun detectLines(
        bitmap: Bitmap,
        sensitivity: Float = 0.5f,
        partId: String = "face"
    ): List <LangerLine>{
        val mat = Mat()

        Utils.bitmapToMat(bitmap, mat)
        // 1. Convert bitmap to grayscale/process pixels

        val rows = mat.rows()
        val cols = mat.cols()

        if( rows == 0 || cols == 0){
            mat.release()
            return emptyList()
        }


        // ── 1. Skin mask (HSV range) ──────────────────────────────────────────
        val hsv = Mat()
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_RGB2HSV)
        val skinMask = Mat()
        Core.inRange(hsv, Scalar(0.0, 30.0, 40.0), Scalar(25.0, 180.0, 255.0), skinMask)

        // ── 2. Preprocess to grayscale ────────────────────────────────────────
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.createCLAHE(2.0, Size(8.0, 8.0)).apply(gray, gray)
        Imgproc.bilateralFilter(gray.clone(), gray, 9, 75.0, 75.0)
        val morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(gray, gray, Imgproc.MORPH_OPEN, morphKernel)

        // ── 3. Structure tensor via Scharr ────────────────────────────────────
        //
        //   J = Gσ * [ Ix²    Ix·Iy ]
        //             [ Ix·Iy  Iy²   ]
        //
        val gradX = Mat(); val gradY = Mat()
        Imgproc.Scharr(gray, gradX, CvType.CV_32F, 1, 0)
        Imgproc.Scharr(gray, gradY, CvType.CV_32F, 0, 1)

        val jxx = Mat(); val jxy = Mat(); val jyy = Mat()
        Core.multiply(gradX, gradX, jxx)
        Core.multiply(gradX, gradY, jxy)
        Core.multiply(gradY, gradY, jyy)

        val blurSize = Size(15.0, 15.0)
        Imgproc.GaussianBlur(jxx, jxx, blurSize, 3.0)
        Imgproc.GaussianBlur(jxy, jxy, blurSize, 3.0)
        Imgproc.GaussianBlur(jyy, jyy, blurSize, 3.0)

        // ── 4. Read tensor arrays ─────────────────────────────────────────────
        val jxxArr = FloatArray(rows * cols); jxx.get(0, 0, jxxArr)
        val jxyArr = FloatArray(rows * cols); jxy.get(0, 0, jxyArr)
        val jyyArr = FloatArray(rows * cols); jyy.get(0, 0, jyyArr)
        val maskArr = ByteArray(rows * cols); skinMask.get(0, 0, maskArr)

        // ── 5. Anatomical prior for this region ───────────────────────────────
        val prior = PRIORS[partId.lowercase()] ?: UNKNOWN_PRIOR

        // ── 6. Grid sampling ──────────────────────────────────────────────────
        val step   = if (partId == "face") 45 else 55
        val aspect = cols.toFloat() / rows.toFloat()

        // Sensitivity controls the minimum coherence threshold.
        // Higher sensitivity → accept less coherent (noisier) pixels.
        // coherence C = ((λ₁−λ₂)/(λ₁+λ₂))²  ∈ [0,1]
        val minCoherence = 0.08 + (1.0f - sensitivity) * 0.42f  // range [0.08, 0.50]

        val lines = mutableListOf<LangerLine>()

        for (y in step until rows - step step step) {
            for (x in step until cols - step step step) {

                val idx = y * cols + x
                if ((maskArr[idx].toInt() and 0xFF) == 0) continue

                val a = jxxArr[idx].toDouble()
                val b = jxyArr[idx].toDouble()
                val c = jyyArr[idx].toDouble()

                // ── Eigenvalues (exact closed form for 2×2 symmetric matrix) ──
                //
                //   λ₁,₂ = (a+c)/2  ±  √[ ((a−c)/2)² + b² ]
                //
                val halfTrace = (a + c) * 0.5
                val disc      = sqrt(((a - c) * 0.5).pow(2) + b.pow(2))
                val lambda1   = halfTrace + disc   // max principal stress (σ₁)
                val lambda2   = halfTrace - disc   // min principal stress (σ₂)

                // Discard flat/textureless pixels
                if (lambda1 + lambda2 < 1e-6) continue

                // ── Weickert coherence ────────────────────────────────────────
                //
                //   C = ( (λ₁ − λ₂) / (λ₁ + λ₂) )²   ∈ [0,1]
                //
                //   C ≈ 1  →  strongly anisotropic (fibres clearly oriented)
                //   C ≈ 0  →  isotropic (no dominant direction, unreliable)
                //
                val coherence = ((lambda1 - lambda2) / (lambda1 + lambda2)).pow(2)
                if (coherence < minCoherence) continue

                // ── Max-stress eigenvector angle ──────────────────────────────
                //
                //   v₁ = (b, λ₁ − a)   →   θ_σ₁ = atan2(λ₁ − a, b)
                //
                val stressAngle = atan2(lambda1 - a, b)

                // ── Langer line direction: perpendicular to σ₁ ───────────────
                //
                //   θ_L = θ_σ₁ + π/2   (wrapped to [0, π))
                //
                val langerAngle = wrapAngle(stressAngle + PI / 2.0)

                // ── Fuse with anatomical prior (double-angle circular mean) ────
                //
                // Doubling angles before averaging and halving after correctly
                // handles the π-periodicity of line orientations.
                //
                //   sinFused = w_img · C · sin(2θ_L) + w_prior · A · sin(2θ_prior)
                //   cosFused = w_img · C · cos(2θ_L) + w_prior · A · cos(2θ_prior)
                //   θ_fused  = atan2(sinFused, cosFused) / 2
                //
                val wImg   = (1.0 - PRIOR_WEIGHT) * coherence
                val wPrior = PRIOR_WEIGHT * prior.anisotropy
                val sinF   = wImg * sin(2 * langerAngle) + wPrior * sin(2 * prior.angleRad)
                val cosF   = wImg * cos(2 * langerAngle) + wPrior * cos(2 * prior.angleRad)
                val fusedAngle = wrapAngle(atan2(sinF, cosF) / 2.0)

                // ── Build LangerLine segment ──────────────────────────────────
                val normX  = x.toFloat() / cols
                val normY  = y.toFloat() / rows

                // Length scales with coherence — more confident pixels get
                // longer segments so the dominant direction stands out visually.
                val length = (0.015 + coherence * 0.020).coerceIn(0.012, 0.035).toFloat()

                val dx = (cos(fusedAngle).toFloat() * length) / aspect
                val dy =  sin(fusedAngle).toFloat() * length

                // Intensity maps coherence to visual opacity [0.3, 0.95]
                val intensity = coherence.toFloat().coerceIn(0.3f, 0.95f)

                lines.add(
                    LangerLine(
                        startX    = normX - dx,
                        startY    = normY - dy,
                        endX      = normX + dx,
                        endY      = normY + dy,
                        intensity = intensity
                    )
                )
            }
        }

        // ── 7. Release all Mats ───────────────────────────────────────────────
        listOf(mat, hsv, skinMask, gray, gradX, gradY, jxx, jxy, jyy).forEach { it.release() }

        return lines


    }

    private fun wrapAngle(fi: Double): Double {
        var a = fi % PI
        if (a < 0) a += PI
        return a
    }

}