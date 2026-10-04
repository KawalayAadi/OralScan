package com.oralscan.app.ml

import android.graphics.Bitmap

enum class AnalysisStatus { SUCCESS, FAILED }

/** A detected area, in coordinates normalized to 0..1 of the image width/height. */
data class Region(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String? = null,
    val score: Float? = null,
)

/**
 * Everything any model can hand back to the UI. Each model fills in only what it produces:
 * - segmentation (UNet / UNet++ / Attention UNet) -> [mask]
 * - detection (Mask R-CNN) -> [regions] (+ [mask])
 * - vision LLM -> [label] text
 */
data class AnalysisResult(
    val status: AnalysisStatus,
    val message: String,
    val latencyMs: Long,
    /** Transparent bitmap with the lesion area painted in; drawn stretched over the photo. */
    val mask: Bitmap? = null,
    val regions: List<Region> = emptyList(),
    val label: String? = null,
    val confidence: Float? = null,
    val debug: Map<String, String> = emptyMap(),
)
