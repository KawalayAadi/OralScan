package com.oralscan.app.ml

import android.graphics.Bitmap

/**
 * Mirrors the training transforms in the UNet notebooks:
 * Resize((256, 256)) -> ToDtype(float32, scale=True) -> Normalize(ImageNet mean/std).
 * Output is a flat float array in NCHW order (1 x 3 x size x size), ready for ONNX Runtime / LiteRT.
 */
object ImagePreprocessor {
    const val INPUT_SIZE = 256
    val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    val IMAGENET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    fun toNchwTensor(
        bitmap: Bitmap,
        size: Int = INPUT_SIZE,
        mean: FloatArray = IMAGENET_MEAN,
        std: FloatArray = IMAGENET_STD,
    ): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== bitmap) scaled.recycle()

        val plane = size * size
        val out = FloatArray(3 * plane)
        for (i in 0 until plane) {
            val p = pixels[i]
            out[i] = (((p shr 16) and 0xFF) / 255f - mean[0]) / std[0]
            out[plane + i] = (((p shr 8) and 0xFF) / 255f - mean[1]) / std[1]
            out[2 * plane + i] = ((p and 0xFF) / 255f - mean[2]) / std[2]
        }
        return out
    }
}
