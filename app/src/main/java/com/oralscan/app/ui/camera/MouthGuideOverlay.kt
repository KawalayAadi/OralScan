package com.oralscan.app.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Fraction of the screen width the guide oval spans. */
private const val OVAL_WIDTH_FRACTION = 0.8f
/** Height/width ratio of an open mouth. */
private const val OVAL_ASPECT = 0.68f

/**
 * Dims everything except a mouth-shaped oval so users frame photos consistently,
 * which matters a lot for model accuracy later on.
 */
@Composable
fun MouthGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxSize()
            // Offscreen compositing so BlendMode.Clear punches a real hole in the scrim.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val ovalWidth = size.width * OVAL_WIDTH_FRACTION
        val ovalHeight = ovalWidth * OVAL_ASPECT
        val topLeft = Offset(
            x = (size.width - ovalWidth) / 2f,
            y = (size.height - ovalHeight) / 2f - size.height * 0.04f,
        )
        val ovalSize = Size(ovalWidth, ovalHeight)

        drawRect(Color.Black.copy(alpha = 0.55f))
        drawOval(Color.Transparent, topLeft, ovalSize, blendMode = BlendMode.Clear)
        drawOval(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = topLeft,
            size = ovalSize,
            style = Stroke(
                width = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18.dp.toPx(), 10.dp.toPx())),
            ),
        )
    }
}
