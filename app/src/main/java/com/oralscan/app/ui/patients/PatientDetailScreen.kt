package com.oralscan.app.ui.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import com.oralscan.app.data.ScanEntity
import com.oralscan.app.data.faceCheckValue
import com.oralscan.app.data.isSuccess
import com.oralscan.app.data.subtitle
import com.oralscan.app.ui.Routes
import com.oralscan.app.ui.common.FaceCheckBadge
import com.oralscan.app.ui.common.StatusBadge
import com.oralscan.app.ui.common.formatTimestamp
import com.oralscan.app.ui.common.requireContainer
import com.oralscan.app.ui.face.RECOMMENDED_FACE_PHOTOS
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class PatientDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val patients: PatientRepository,
) : ViewModel() {
    val patientId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_PATIENT_ID))

    val patient: StateFlow<PatientEntity?> = patients.observe(patientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val faces: StateFlow<List<PatientFaceEntity>> = patients.observeFaces(patientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val scans: StateFlow<List<ScanEntity>> = patients.observeScans(patientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteFace(face: PatientFaceEntity) {
        viewModelScope.launch { patients.deleteFace(face) }
    }

    fun deletePatient(onDeleted: () -> Unit) {
        val current = patient.value ?: return
        viewModelScope.launch {
            patients.delete(current)
            onDeleted()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { PatientDetailViewModel(createSavedStateHandle(), requireContainer().patients) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    onStartScan: (Long) -> Unit,
    onAddFacePhotos: (Long) -> Unit,
    onOpenScan: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: PatientDetailViewModel = viewModel(factory = PatientDetailViewModel.Factory),
) {
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val faces by viewModel.faces.collectAsStateWithLifecycle()
    val scans by viewModel.scans.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (patient != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete patient") }
                    }
                },
            )
        },
    ) { padding ->
        val p = patient
        if (p == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PatientAvatar(p.name, faces.firstOrNull()?.imagePath, 72.dp)
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    val sub = p.subtitle()
                    if (sub.isNotEmpty()) {
                        Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "Added ${formatTimestamp(p.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            p.notes?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Button(onClick = { onStartScan(p.id) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Start new scan")
            }

            SectionCard(title = "Reference face photos (${faces.size})") {
                if (faces.size < RECOMMENDED_FACE_PHOTOS) {
                    Text(
                        if (faces.isEmpty()) {
                            "No face photos yet. This patient can't be verified until you add some."
                        } else {
                            "Add ${RECOMMENDED_FACE_PHOTOS - faces.size} more for more reliable verification."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (faces.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (faces.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(faces, key = { it.id }) { face ->
                            Box {
                                AsyncImage(
                                    model = File(face.imagePath),
                                    contentDescription = "Reference face photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    IconButton(onClick = { viewModel.deleteFace(face) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Close, "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { onAddFacePhotos(p.id) }) {
                    Icon(Icons.Filled.AddAPhoto, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add face photos")
                }
            }

            SectionCard(title = "Oral scans (${scans.size})") {
                if (scans.isEmpty()) {
                    Text(
                        "No scans yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                scans.forEach { scan ->
                    ScanRow(scan = scan, onClick = { onOpenScan(scan.id) })
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${patient?.name}?") },
            text = { Text("This removes the patient, their face photos and all of their scans from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deletePatient(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ScanRow(scan: ScanEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = File(scan.imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatTimestamp(scan.createdAt), style = MaterialTheme.typography.titleSmall)
                scan.faceCheckValue()?.let { FaceCheckBadge(it) }
            }
            StatusBadge(success = scan.isSuccess())
        }
    }
}
