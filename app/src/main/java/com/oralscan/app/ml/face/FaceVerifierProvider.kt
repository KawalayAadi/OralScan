package com.oralscan.app.ml.face

import android.content.Context
import android.util.Log

/**
 * Picks the face model: the real Siamese model if `assets/models/face_siamese.tflite` exists,
 * otherwise the placeholder. Dropping the .tflite file in and rebuilding is all that's needed.
 */
object FaceVerifierProvider {
    const val MODEL_ASSET = "models/face_siamese.tflite"
    private const val TAG = "FaceVerifierProvider"

    fun create(context: Context): FaceVerifier {
        val bundled = runCatching { context.assets.list("models")?.contains("face_siamese.tflite") == true }
            .getOrDefault(false)
        if (!bundled) return PlaceholderFaceVerifier("no face model bundled")

        return try {
            TfliteFaceVerifier(context, MODEL_ASSET)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load $MODEL_ASSET", e)
            PlaceholderFaceVerifier("model failed to load: ${e.message}")
        }
    }
}
