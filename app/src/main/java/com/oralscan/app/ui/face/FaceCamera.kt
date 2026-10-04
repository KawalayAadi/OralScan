package com.oralscan.app.ui.face

import android.graphics.Bitmap
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.oralscan.app.data.ImageStore
import com.oralscan.app.ml.face.FacePreprocessor
import com.oralscan.app.ui.camera.CameraPermissionGate
import com.oralscan.app.ui.camera.ShutterButton
import com.oralscan.app.ui.common.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Guide circle diameter as a fraction of the preview width. */
private const val CIRCLE_WIDTH_FRACTION = 0.72f
/** Vertical position of the circle centre as a fraction of the preview height. */
private const val CIRCLE_CENTER_Y_FRACTION = 0.42f

/**
 * Camera that captures a square face crop: exactly what is inside the guide circle,
 * scaled to 250x250 (like the 250x250 webcam crop in faceid.py).
 */
@Composable
fun FaceCamera(
    title: String,
    hint: String,
    busy: Boolean,
    onBack: () -> Unit,
    onFaceCaptured: (Bitmap) -> Unit,
    topActions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable ColumnScope.() -> Unit = {},
) {
    CameraPermissionGate(onBack = onBack) {
        FaceCameraContent(title, hint, busy, onBack, onFaceCaptured, topActions, bottomContent)
    }
}

@Composable
private fun FaceCameraContent(
    title: String,
    hint: String,
    busy: Boolean,
    onBack: () -> Unit,
    onFaceCaptured: (Bitmap) -> Unit,
    topActions: @Composable RowScope.() -> Unit,
    bottomContent: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageStore = appContainer().imageStore
    val scope = rememberCoroutineScope()

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            // Clinician photographs the patient, so the back camera is the default.
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose { cameraController.unbind() }
    }

    var useFrontCamera by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    fun capture() {
        if (capturing || busy || viewSize == IntSize.Zero) return
        capturing = true
        val file = imageStore.newCaptureFile()
        val size = viewSize
        cameraController.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    scope.launch {
                        val face = withContext(Dispatchers.IO) {
                            val photo = imageStore.loadBitmap(file.absolutePath, maxDim = 2000)
                            file.delete()
                            photo?.let { cropToGuide(imageStore, it, size) }
                        }
                        capturing = false
                        if (face != null) {
                            onFaceCaptured(face)
                        } else {
                            Toast.makeText(context, "Couldn't read the photo, try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    file.delete()
                    Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            },
        )
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller = cameraController
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it },
    )

    FaceGuideOverlay()

    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            topActions()
            IconButton(onClick = {
                useFrontCamera = !useFrontCamera
                cameraController.cameraSelector =
                    if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            }) {
                Icon(Icons.Filled.Cameraswitch, "Switch camera", tint = Color.White)
            }
        }
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            hint,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 24.dp, end = 24.dp),
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        bottomContent()
        ShutterButton(busy = capturing || busy, onClick = ::capture)
    }
}

/**
 * Maps the guide circle from preview coordinates to photo coordinates.
 * PreviewView FILL_CENTER scales the photo by max(viewW/imgW, viewH/imgH) and centres it,
 * and CameraController keeps preview and capture at the same aspect ratio.
 */
private fun cropToGuide(imageStore: ImageStore, photo: Bitmap, view: IntSize): Bitmap {
    val scale = maxOf(view.width.toFloat() / photo.width, view.height.toFloat() / photo.height)
    val circleCenterX = view.width / 2f
    val circleCenterY = view.height * CIRCLE_CENTER_Y_FRACTION
    val circleDiameter = view.width * CIRCLE_WIDTH_FRACTION

    val centerX = photo.width / 2f + (circleCenterX - view.width / 2f) / scale
    val centerY = photo.height / 2f + (circleCenterY - view.height / 2f) / scale
    val side = circleDiameter / scale

    val face = imageStore.cropSquare(photo, centerX, centerY, side, FacePreprocessor.STORED_FACE_SIZE)
    photo.recycle()
    return face
}

@Composable
private fun FaceGuideOverlay() {
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val radius = size.width * CIRCLE_WIDTH_FRACTION / 2f
        val center = Offset(size.width / 2f, size.height * CIRCLE_CENTER_Y_FRACTION)
        drawRect(Color.Black.copy(alpha = 0.55f))
        drawCircle(Color.Transparent, radius, center, blendMode = BlendMode.Clear)
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = radius,
            center = center,
            style = Stroke(
                width = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18.dp.toPx(), 10.dp.toPx())),
            ),
        )
    }
}
