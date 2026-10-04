package com.oralscan.app.ml

import android.graphics.Bitmap
import kotlin.math.exp

/** Helpers for turning a segmentation model's per-pixel output into something drawable. */
object MaskUtils {
    const val DEFAULT_THRESHOLD = 0.5f
    private const val OVERLAY_COLOR = 0x99E53935.toInt() // semi-transparent red

    /** Models exported from smp output raw logits; apply this before thresholding. */
    fun sigmoid(logits: FloatArray): FloatArray =
        FloatArray(logits.size) { 1f / (1f + exp(-logits[it])) }

    /** Probabilities (size x size, row-major) -> transparent bitmap with the positive pixels painted. */
    fun toOverlay(
        probabilities: FloatArray,
        size: Int,
        threshold: Float = DEFAULT_THRESHOLD,
        color: Int = OVERLAY_COLOR,
    ): Bitmap {
        val pixels = IntArray(size * size) { if (probabilities[it] >= threshold) color else 0 }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    /** Fraction of the image flagged as lesion, 0..1. */
    fun coverage(probabilities: FloatArray, threshold: Float = DEFAULT_THRESHOLD): Float =
        probabilities.count { it >= threshold }.toFloat() / probabilities.size
}
