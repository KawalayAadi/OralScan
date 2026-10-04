package com.oralscan.app.ml.face

import android.graphics.Bitmap

/**
 * Compares two face crops. Mirrors the Siamese network from Face_Recognition_App
 * (`siamese_model([input_img, validation_img])`), which outputs one sigmoid score.
 */
interface FaceVerifier {
    val name: String

    /** True until a real model is bundled; the UI warns the user. */
    val isPlaceholder: Boolean

    /** 0..1 where higher means "same person". */
    suspend fun similarity(face: Bitmap, reference: Bitmap): Float
}

data class FaceVerificationResult(
    val verified: Boolean,
    /** One score per reference photo, same order as the references. */
    val scores: List<Float>,
    val matched: Int,
    val matchFraction: Float,
    val detectionThreshold: Float,
    val verificationThreshold: Float,
    val verifierName: String,
    val isPlaceholder: Boolean,
    val latencyMs: Long,
) {
    val total: Int get() = scores.size
    val bestScore: Float get() = scores.maxOrNull() ?: 0f
}

/**
 * Same decision rule as `CamApp.verify()` in faceid.py:
 *   detection    = number of reference scores > detectionThreshold
 *   verification = detection / number of references
 *   verified     = verification > verificationThreshold
 */
suspend fun FaceVerifier.verify(
    face: Bitmap,
    references: List<Bitmap>,
    detectionThreshold: Float,
    verificationThreshold: Float,
): FaceVerificationResult {
    require(references.isNotEmpty()) { "Patient has no reference face photos" }
    val start = System.currentTimeMillis()
    val scores = references.map { similarity(face, it) }
    val matched = scores.count { it > detectionThreshold }
    val fraction = matched.toFloat() / scores.size
    return FaceVerificationResult(
        verified = fraction > verificationThreshold,
        scores = scores,
        matched = matched,
        matchFraction = fraction,
        detectionThreshold = detectionThreshold,
        verificationThreshold = verificationThreshold,
        verifierName = name,
        isPlaceholder = isPlaceholder,
        latencyMs = System.currentTimeMillis() - start,
    )
}
