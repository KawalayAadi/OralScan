package com.oralscan.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/**
 * All image files live in app-private storage:
 * - cacheDir/captures: fresh photos waiting for review (temporary)
 * - filesDir/scans: images and masks belonging to saved scans
 * - filesDir/faces: patients' reference face crops
 */
class ImageStore(private val context: Context) {

    private val capturesDir: File
        get() = File(context.cacheDir, "captures").apply { mkdirs() }

    private val scansDir: File
        get() = File(context.filesDir, "scans").apply { mkdirs() }

    private val facesDir: File
        get() = File(context.filesDir, "faces").apply { mkdirs() }

    fun newCaptureFile(): File = File(capturesDir, "capture_${System.currentTimeMillis()}.jpg")

    /** Copies a gallery image into the capture folder. Returns null if it can't be read. */
    fun importFromUri(uri: Uri): File? {
        val file = File(capturesDir, "upload_${System.currentTimeMillis()}.img")
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { src -> file.outputStream().use { dst -> src.copyTo(dst) } }
            file
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    /** Decodes an image upright (EXIF-corrected) and downsampled so its longest side is <= [maxDim]. */
    fun loadBitmap(path: String, maxDim: Int = 2048): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2

            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(path, options) ?: return null
            applyExifOrientation(path, decoded)
        } catch (e: Exception) {
            null
        }
    }

    fun saveScanImage(bitmap: Bitmap): File {
        val file = File(scansDir, "scan_${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return file
    }

    fun saveMask(mask: Bitmap): File {
        val file = File(scansDir, "mask_${UUID.randomUUID()}.png")
        file.outputStream().use { mask.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun saveFace(face: Bitmap): File {
        val file = File(facesDir, "face_${UUID.randomUUID()}.jpg")
        file.outputStream().use { face.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        return file
    }

    /**
     * Cuts a square out of [bitmap] centred on ([centerX], [centerY]) with the given [side]
     * (clamped to the image), then scales it to [outputSize] x [outputSize].
     */
    fun cropSquare(bitmap: Bitmap, centerX: Float, centerY: Float, side: Float, outputSize: Int): Bitmap {
        val s = side.toInt().coerceIn(1, minOf(bitmap.width, bitmap.height))
        val left = (centerX - s / 2f).toInt().coerceIn(0, bitmap.width - s)
        val top = (centerY - s / 2f).toInt().coerceIn(0, bitmap.height - s)
        val cropped = Bitmap.createBitmap(bitmap, left, top, s, s)
        val scaled = Bitmap.createScaledBitmap(cropped, outputSize, outputSize, true)
        if (scaled !== cropped) cropped.recycle()
        return scaled
    }

    fun delete(path: String) {
        File(path).delete()
    }

    private fun applyExifOrientation(path: String, bitmap: Bitmap): Bitmap {
        val exif = ExifInterface(path)
        val degrees = exif.rotationDegrees
        val flipped = exif.isFlipped
        if (degrees == 0 && !flipped) return bitmap

        val matrix = Matrix().apply {
            if (flipped) postScale(-1f, 1f)
            postRotate(degrees.toFloat())
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
