package com.oralscan.app.ml.face

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Stand-in used until `face_siamese.tflite` is bundled. It is NOT face recognition:
 * it compares tiny grayscale thumbnails (normalized cross-correlation), so near-identical
 * photos score high and different scenes score low. Enough to exercise the whole flow.
 */
class PlaceholderFaceVerifier(private val reason: String) : FaceVerifier {

    override val name: String = "Placeholder face matcher ($reason)"
    override val isPlaceholder: Boolean = true

    override suspend fun similarity(face: Bitmap, reference: Bitmap): Float = withContext(Dispatchers.Default) {
        val a = grayThumbnail(face)
        val b = grayThumbnail(reference)
        val ma = a.average()
        val mb = b.average()
        var num = 0.0
        var da = 0.0
        var db = 0.0
        for (i in a.indices) {
            val x = a[i] - ma
            val y = b[i] - mb
            num += x * y
            da += x * x
            db += y * y
        }
        val ncc = if (da == 0.0 || db == 0.0) 0.0 else num / sqrt(da * db)
        ((ncc + 1.0) / 2.0).toFloat().coerceIn(0f, 1f)
    }

    private fun grayThumbnail(bitmap: Bitmap): DoubleArray {
        val size = 32
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== bitmap) scaled.recycle()
        return DoubleArray(pixels.size) { i ->
            val p = pixels[i]
            0.299 * ((p shr 16) and 0xFF) + 0.587 * ((p shr 8) and 0xFF) + 0.114 * (p and 0xFF)
        }
    }
}
