package com.oralscan.app.ui.face

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oralscan.app.AppContainer
import com.oralscan.app.data.FaceCheck
import com.oralscan.app.data.PatientEntity
import com.oralscan.app.data.PatientFaceEntity
import com.oralscan.app.data.ScanContext
import com.oralscan.app.ml.face.FaceVerificationResult
import com.oralscan.app.ml.face.verify
import com.oralscan.app.ui.Routes
import com.oralscan.app.ui.common.requireContainer
import com.oralscan.app.ui.theme.LocalStatusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

sealed interface VerifyUiState {
    data object Capturing : VerifyUiState
    data object Verifying : VerifyUiState
    data class Done(val face: Bitmap, val result: FaceVerificationResult) : VerifyUiState
    data class Error(val message: String) : VerifyUiState
}

class FaceVerifyViewModel(
    savedStateHandle: SavedStateHandle,
    private val container: AppContainer,
) : ViewModel() {
    val patientId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_PATIENT_ID))

    val patient: StateFlow<PatientEntity?> = container.patients.observe(patientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null while loading. */
    val faces: StateFlow<List<PatientFaceEntity>?> = container.patients.observeFaces(patientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _state = MutableStateFlow<VerifyUiState>(VerifyUiState.Capturing)
    val state: StateFlow<VerifyUiState> = _state.asStateFlow()

    fun verify(face: Bitmap) {
        _state.value = VerifyUiState.Verifying
        viewModelScope.launch {
            _state.value = try {
                val references = container.patients.loadFaceBitmaps(patientId)
                if (references.isEmpty()) {
                    VerifyUiState.Error("This patient has no reference face photos yet.")
                } else {
                    // First access loads the model from assets; keep it off the main thread.
                    val verifier = withContext(Dispatchers.IO) { container.faceVerifier }
                    val settings = container.devSettings
                    val result = verifier.verify(
                        face = face,
                        references = references,
                        detectionThreshold = settings.faceDetectionThreshold.value,
                        verificationThreshold = settings.faceVerificationThreshold.value,
                    )
                    VerifyUiState.Done(face, result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                VerifyUiState.Error("Face verification failed: ${e.message}")
            }
        }
    }

    fun retry() {
        _state.value = VerifyUiState.Capturing
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { FaceVerifyViewModel(createSavedStateHandle(), requireContainer()) }
        }
    }
}

@Composable
fun FaceVerifyScreen(
    onContinue: (ScanContext) -> Unit,
    onAddFacePhotos: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: FaceVerifyViewModel = viewModel(factory = FaceVerifyViewModel.Factory),
) {
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val faces by viewModel.faces.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pid = viewModel.patientId
    val name = patient?.name ?: "patient"
    val skip = { onContinue(ScanContext(pid, FaceCheck.SKIPPED, null)) }

    val faceList = faces
    when {
        faceList == null -> LoadingScreen()
        faceList.isEmpty() -> NoReferencePhotos(
            name = name,
            onAddFacePhotos = { onAddFacePhotos(pid) },
            onSkip = skip,
            onBack = onBack,
        )
        state is VerifyUiState.Done -> VerifyResult(
            name = name,
            done = state as VerifyUiState.Done,
            onContinue = { result ->
                val check = if (result.verified) FaceCheck.VERIFIED else FaceCheck.NOT_VERIFIED
                onContinue(ScanContext(pid, check, result.matchFraction))
            },
            onRetry = viewModel::retry,
            onBack = onBack,
        )
        state is VerifyUiState.Error -> VerifyError(
            message = (state as VerifyUiState.Error).message,
            onRetry = viewModel::retry,
            onBack = onBack,
        )
        else -> FaceCamera(
            title = "Verify: $name",
            hint = "Fit the patient's face inside the circle, then take the photo.",
            busy = state is VerifyUiState.Verifying,
            onBack = onBack,
            onFaceCaptured = viewModel::verify,
            topActions = {
                TextButton(onClick = skip) { Text("Skip", color = Color.White) }
            },
            bottomContent = {
                if (state is VerifyUiState.Verifying) {
                    Text(
                        "Comparing with ${faceList.size} reference photo(s)…",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
        )
    }
}

@Composable
private fun VerifyResult(
    name: String,
    done: VerifyUiState.Done,
    onContinue: (FaceVerificationResult) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val result = done.result
    val status = LocalStatusColors.current
    val container = if (result.verified) status.successContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (result.verified) status.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                bitmap = done.face.asImageBitmap(),
                contentDescription = "Captured face",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape),
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (result.verified) Icons.Filled.VerifiedUser else Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.size(14.dp))
                    Column {
                        Text(
                            if (result.verified) "Verified as $name" else "Not verified",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${result.matched} of ${result.total} reference photos matched",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            if (result.isPlaceholder) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "Placeholder matcher: the face recognition model isn't bundled yet, " +
                                "so this result is not a real identity check.",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Scores per reference photo", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        result.scores.joinToString("   ") { "%.2f".format(it) },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Match if score > %.2f · verified if > %.0f%% match · %d ms"
                            .format(result.detectionThreshold, result.verificationThreshold * 100, result.latencyMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        result.verifierName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (result.verified) {
                Button(onClick = { onContinue(result) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue to oral photo")
                }
                TextButton(onClick = onRetry) { Text("Retake face photo") }
            } else {
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
                OutlinedButton(onClick = { onContinue(result) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue anyway (marked unverified)")
                }
                TextButton(onClick = onBack) { Text("Choose a different patient") }
            }
        }
    }
}

@Composable
private fun NoReferencePhotos(
    name: String,
    onAddFacePhotos: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    MessageScreen(
        icon = { Icon(Icons.Filled.AddAPhoto, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary) },
        title = "No face photos for $name",
        body = "Add reference face photos first so this patient can be verified on future visits.",
    ) {
        Button(onClick = onAddFacePhotos, modifier = Modifier.fillMaxWidth()) { Text("Add face photos") }
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip verification") }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun VerifyError(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    MessageScreen(
        icon = { Icon(Icons.Filled.ErrorOutline, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error) },
        title = "Verification failed",
        body = message,
    ) {
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun MessageScreen(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    actions: @Composable () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon()
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.size(8.dp))
            actions()
        }
    }
}

@Composable
private fun LoadingScreen() {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
    }
}
