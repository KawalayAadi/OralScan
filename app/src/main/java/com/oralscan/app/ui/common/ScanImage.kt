package com.oralscan.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.oralscan.app.ml.Region
import java.io.File

/**
 * The scan photo with the model's mask and boxes drawn on top.
 * The box takes the photo's exact aspect ratio, so an overlay stretched to fill it lines up pixel-for-pixel.
 */
@Composable
fun ScanImage(
    imagePath: String,
    imageWidth: Int,
    imageHeight: Int,
    maskPath: String?,
    regions: List<Region>,
    showOverlay: Boolean,
    modifier: Modifier = Modifier,
) {
    val ratio = if (imageWidth > 0 && imageHeight > 0) imageWidth.toFloat() / imageHeight else 1f
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(20.dp))
    ) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = "Scanned photo",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        if (showOverlay && maskPath != null) {
            AsyncImage(
                model = File(maskPath),
                contentDescription = "Highlighted region",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
        }
        if (showOverlay && regions.isNotEmpty()) {
            Canvas(Modifier.matchParentSize()) {
                regions.forEach { r ->
                    drawRect(
                        color = Color(0xFFFFC107),
                        topLeft = Offset(r.left * size.width, r.top * size.height),
                        size = Size((r.right - r.left) * size.width, (r.bottom - r.top) * size.height),
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                }
            }
        }
    }
}
