package com.oralscan.app.ml

import android.graphics.Bitmap
import android.os.SystemClock
import com.oralscan.app.data.DevSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.atan2
import kotlin.math.sin

/**
 * Stand-in until the real model exists. It does NOT detect anything.
 * It runs the real preprocessing, checks the tensor is valid, waits like inference would,
 * and reports whether the image was successfully passed to the "model".
 */
class PlaceholderAnalyzer(private val settings: DevSettings) : LesionAnalyzer {

    override val name: String = NAME

    override suspend fun analyze(image: Bitmap): AnalysisResult = withContext(Dispatchers.Default) {
        val start = SystemClock.elapsedRealtime()
        try {
            if (settings.simulateFailure.value) error("Simulated failure (developer option)")

            val size = ImagePreprocessor.INPUT_SIZE
            val tensor = ImagePreprocessor.toNchwTensor(image, size)
            val expected = 3 * size * size
            check(tensor.size == expected) { "Tensor has ${tensor.size} values, expected $expected" }

            var min = Float.POSITIVE_INFINITY
            var max = Float.NEGATIVE_INFINITY
            var sum = 0.0
            for (v in tensor) {
                check(v.isFinite()) { "Tensor contains NaN or infinite values" }
                if (v < min) min = v
                if (v > max) max = v
                sum += v
            }

            delay(SIMULATED_INFERENCE_MS)

            val debug = linkedMapOf(
                "Input image" to "${image.width} × ${image.height} px",
                "Tensor shape" to "1 × 3 × $size × $size (NCHW)",
                "Tensor values" to expected.toString(),
                "Normalization" to "ImageNet mean/std",
                "Value range" to "%.3f … %.3f".format(min, max),
                "Mean value" to "%.4f".format(sum / expected),
            )

            var mask: Bitmap? = null
            var label: String? = null
            if (settings.fakeMask.value) {
                val probs = fakeProbabilities(size)
                mask = MaskUtils.toOverlay(probs, size)
                label = "Demo region (fake mask, not a real finding)"
                debug["Fake mask coverage"] = "%.1f %%".format(MaskUtils.coverage(probs) * 100)
            }

            AnalysisResult(
                status = AnalysisStatus.SUCCESS,
                message = "Image successfully passed to model",
                latencyMs = SystemClock.elapsedRealtime() - start,
                mask = mask,
                label = label,
                debug = debug,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AnalysisResult(
                status = AnalysisStatus.FAILED,
                message = "Image could not be passed to model: ${e.message ?: e::class.simpleName}",
                latencyMs = SystemClock.elapsedRealtime() - start,
                debug = mapOf("Error" to (e::class.simpleName ?: "Exception")),
            )
        }
    }

    /** An organic-looking blob near the center, used only to exercise the overlay UI. */
    private fun fakeProbabilities(size: Int): FloatArray {
        val cx = 0.55f
        val cy = 0.58f
        val rx = 0.17f
        val ry = 0.11f
        return FloatArray(size * size) { i ->
            val x = (i % size) / size.toFloat() - cx
            val y = (i / size) / size.toFloat() - cy
            val wobble = 1f + 0.18f * sin(5f * atan2(y, x))
            val d = (x / rx) * (x / rx) + (y / ry) * (y / ry)
            1f - d / wobble
        }
    }

    companion object {
        const val NAME = "Placeholder model v0"
        private const val SIMULATED_INFERENCE_MS = 1500L
    }
}
