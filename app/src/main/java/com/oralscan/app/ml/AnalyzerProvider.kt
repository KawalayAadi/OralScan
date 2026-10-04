package com.oralscan.app.ml

import android.content.Context
import com.oralscan.app.data.DevSettings

/**
 * The one place that decides which model the app uses.
 *
 * When the real model is ready (e.g. a UNet++ exported to ONNX and placed in assets/models/),
 * add an implementation such as `OnnxAnalyzer(context, "models/unetpp.onnx")` and return it here.
 */
object AnalyzerProvider {
    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context, settings: DevSettings): LesionAnalyzer =
        PlaceholderAnalyzer(settings)
}
