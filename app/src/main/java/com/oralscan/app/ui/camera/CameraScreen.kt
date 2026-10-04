package com.oralscan.app.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.oralscan.app.ui.common.appContainer

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onPhotoCaptured: (String) -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        askedOnce = true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            CameraContent(onBack = onBack, onPhotoCaptured = onPhotoCaptured)
        } else {
            PermissionNeeded(
                showSettingsButton = askedOnce,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun CameraContent(
    onBack: () -> Unit,
    onPhotoCaptured: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageStore = appContainer().imageStore

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose { cameraController.unbind() }
    }

    var torchOn by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }

    fun capture() {
        if (capturing) return
        capturing = true
        val file = imageStore.newCaptureFile()
        val metadata = ImageCapture.Metadata().apply { isReversedHorizontal = useFrontCamera }
        val options = ImageCapture.OutputFileOptions.Builder(file).setMetadata(metadata).build()
        cameraController.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturing = false
                    onPhotoCaptured(file.absolutePath)
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
        modifier = Modifier.fillMaxSize(),
    )

    MouthGuideOverlay()

    // Top bar
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
            if (!useFrontCamera) {
                IconButton(onClick = {
                    torchOn = !torchOn
                    cameraController.enableTorch(torchOn)
                }) {
                    Icon(
                        if (torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                        contentDescription = if (torchOn) "Turn light off" else "Turn light on",
                        tint = Color.White,
                    )
                }
            }
            IconButton(onClick = {
                useFrontCamera = !useFrontCamera
                torchOn = false
                cameraController.cameraSelector =
                    if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            }) {
                Icon(Icons.Filled.Cameraswitch, "Switch camera", tint = Color.White)
            }
        }
        Text(
            "Fit the open mouth inside the outline",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }

    // Bottom: tips + shutter
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipChip(Icons.Filled.LightMode, "Bright light")
            TipChip(Icons.Filled.Straighten, "~15 cm away")
            TipChip(Icons.Filled.PanTool, "Hold steady")
        }
        Spacer(Modifier.size(24.dp))
        ShutterButton(capturing = capturing, onClick = ::capture)
    }
}

@Composable
private fun TipChip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(6.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ShutterButton(capturing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (capturing) {
            CircularProgressIndicator(color = Color.White)
        } else {
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, CircleShape),
            ) {}
        }
    }
}

@Composable
private fun PermissionNeeded(
    showSettingsButton: Boolean,
    onRequest: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.NoPhotography,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(16.dp))
        Text("Camera access needed", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "OralScan uses the camera only to take the photo you analyze. Photos stay on this device.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))
        Button(onClick = onRequest) { Text("Allow camera") }
        if (showSettingsButton) {
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }) { Text("Open app settings") }
        }
        Spacer(Modifier.size(8.dp))
        OutlinedButton(onClick = onBack) { Text("Go back") }
    }
}
