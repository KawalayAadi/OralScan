package com.oralscan.app.ml

import android.graphics.Bitmap

/**
 * The single boundary between the app and the on-device model.
 * The UI only ever talks to this interface, so swapping the placeholder for a real
 * model means writing one new implementation and returning it from [AnalyzerProvider].
 */
interface LesionAnalyzer {
    /** Shown on the result screen and stored with each scan. */
    val name: String

    /** Runs off the main thread. Should never throw; report problems as [AnalysisStatus.FAILED]. */
    suspend fun analyze(image: Bitmap): AnalysisResult
}
