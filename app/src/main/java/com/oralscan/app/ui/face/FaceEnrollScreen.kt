package com.oralscan.app.ui.face

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.oralscan.app.data.PatientEntity
import com.oralscan.app.data.PatientFaceEntity
import com.oralscan.app.data.PatientRepository
import com.oralscan.app.ui.Routes
import com.oralscan.app.ui.common.requireContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** Recommended number of reference photos (the Kivy app compared against a folder of them). */
const val RECOMMENDED_FACE_PHOTOS = 5

class FaceEnrollViewModel(
    savedStateHandle: SavedStateHandle,
    private val patients: PatientRepository,
) : ViewModel() {
    val patientId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_PATIENT_ID))

    val patient: StateFlow<PatientEntity?> = patients.observe(patientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val faces: StateFlow<List<PatientFaceEntity>> = patients.observeFaces(patientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun addFace(face: Bitmap) {
        viewModelScope.launch {
            _saving.value = true
            try {
                patients.addFace(patientId, face)
            } finally {
                _saving.value = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { FaceEnrollViewModel(createSavedStateHandle(), requireContainer().patients) }
        }
    }
}

@Composable
fun FaceEnrollScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: FaceEnrollViewModel = viewModel(factory = FaceEnrollViewModel.Factory),
) {
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val faces by viewModel.faces.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()

    FaceCamera(
        title = "Face photos for ${patient?.name ?: "patient"}",
        hint = "Fit the face inside the circle. Take $RECOMMENDED_FACE_PHOTOS photos: straight on, " +
            "turned slightly left and right, in good light.",
        busy = saving,
        onBack = onBack,
        onFaceCaptured = viewModel::addFace,
        bottomContent = {
            if (faces.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    items(faces, key = { it.id }) { face ->
                        AsyncImage(
                            model = File(face.imagePath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${faces.size} of $RECOMMENDED_FACE_PHOTOS photos",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Spacer(Modifier.size(12.dp))
                Button(onClick = onDone, enabled = faces.isNotEmpty() && !saving) {
                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Done")
                }
            }
        },
    )
}
