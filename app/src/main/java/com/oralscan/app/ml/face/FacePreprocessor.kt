package com.oralscan.app.ml.face

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Mirrors `preprocess()` in faceid.py / face_rec.ipynb:
 *   tf.image.resize(img, (100, 100)); img / 255.0
 * Output is a direct float32 buffer in NHWC order (1 x 100 x 100 x 3), RGB, values 0..1.
 */
object FacePreprocessor {
    const val INPUT_SIZE = 100

    /** Side of the stored face crop; the Kivy app cropped 250x250 webcam patches. */
    const val STORED_FACE_SIZE = 250

    fun toNhwcBuffer(bitmap: Bitmap, size: Int = INPUT_SIZE): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== bitmap) scaled.recycle()

        val buffer = ByteBuffer.allocateDirect(4 * size * size * 3).order(ByteOrder.nativeOrder())
        for (p in pixels) {
            buffer.putFloat(((p shr 16) and 0xFF) / 255f)
            buffer.putFloat(((p shr 8) and 0xFF) / 255f)
            buffer.putFloat((p and 0xFF) / 255f)
        }
        buffer.rewind()
        return buffer
    }
}
