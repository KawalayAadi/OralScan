package com.oralscan.app.ui.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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
import com.oralscan.app.data.ScanEntity
import com.oralscan.app.data.ScanWithPatient
import com.oralscan.app.data.faceCheckValue
import com.oralscan.app.data.ScanRepository
import com.oralscan.app.data.debugEntries
import com.oralscan.app.data.isSuccess
import com.oralscan.app.data.regions
import com.oralscan.app.ml.PlaceholderAnalyzer
import com.oralscan.app.ui.Routes
import com.oralscan.app.ui.common.DISCLAIMER
import com.oralscan.app.ui.common.FaceCheckBadge
import com.oralscan.app.ui.common.ScanImage
import com.oralscan.app.ui.common.formatTimestamp
import com.oralscan.app.ui.common.requireContainer
import com.oralscan.app.ui.theme.LocalStatusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResultViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ScanRepository,
) : ViewModel() {
    private val scanId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_SCAN_ID))

    val scan: StateFlow<ScanWithPatient?> = repository.observe(scanId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) {
        val current = scan.value?.scan ?: return
        viewModelScope.launch {
            repository.delete(current)
            onDeleted()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ResultViewModel(createSavedStateHandle(), requireContainer().repository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onNewScan: () -> Unit,
    onOpenPatient: (Long) -> Unit,
    viewModel: ResultViewModel = viewModel(factory = ResultViewModel.Factory),
) {
    val scan by viewModel.scan.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (scan != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete scan") }
                    }
                },
            )
        },
    ) { padding ->
        val current = scan
        if (current == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            ResultContent(
                scan = current.scan,
                patientName = current.patientName,
                onNewScan = onNewScan,
                onOpenPatient = onOpenPatient,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this scan?") },
            text = { Text("The photo and its result will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ResultContent(
    scan: ScanEntity,
    patientName: String?,
    onNewScan: () -> Unit,
    onOpenPatient: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val regions = remember(scan.id) { scan.regions() }
    val debug = remember(scan.id) { scan.debugEntries() }
    val hasOverlay = scan.maskPath != null || regions.isNotEmpty()
    var showOverlay by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScanImage(
            imagePath = scan.imagePath,
            imageWidth = scan.imageWidth,
            imageHeight = scan.imageHeight,
            maskPath = scan.maskPath,
            regions = regions,
            showOverlay = showOverlay,
        )

        if (hasOverlay) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show highlighted region", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = showOverlay, onCheckedChange = { showOverlay = it })
            }
        }

        PatientCard(scan, patientName, onOpenPatient)

        StatusCard(scan)

        if (scan.modelName == PlaceholderAnalyzer.NAME) {
            InfoCard(
                "This version uses a placeholder model. It only checks that the photo reached the model; " +
                    "it does not detect cancer yet."
            )
        }

        DetailsCard(scan, debug)

        Button(onClick = onNewScan, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("New scan")
        }

        Text(
            DISCLAIMER,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun PatientCard(scan: ScanEntity, patientName: String?, onOpenPatient: (Long) -> Unit) {
    val pid = scan.patientId
    Card(
        onClick = { pid?.let(onOpenPatient) },
        enabled = pid != null && patientName != null,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when {
                        pid == null -> "No patient (anonymous scan)"
                        patientName == null -> "Patient deleted"
                        else -> patientName
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                scan.faceCheckValue()?.let { check ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FaceCheckBadge(check)
                        scan.faceScore?.let {
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "%.0f %% of reference photos matched".format(it * 100),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (pid != null && patientName != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }
    }
}

@Composable
private fun StatusCard(scan: ScanEntity) {
    val success = scan.isSuccess()
    val status = LocalStatusColors.current
    val container = if (success) status.successContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (success) status.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (success) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.size(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (success) "Analysis complete" else "Analysis failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(scan.message, style = MaterialTheme.typography.bodyLarge)
                scan.label?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                }
                scan.confidence?.let {
                    Text("Confidence: %.0f %%".format(it * 100), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.size(12.dp))
            Text(text, color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DetailsCard(scan: ScanEntity, debug: List<Pair<String, String>>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailRow("Model", scan.modelName)
            DetailRow("Time taken", "${scan.latencyMs} ms")
            DetailRow("Date", formatTimestamp(scan.createdAt))

            if (debug.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                    Text("Technical details", Modifier.weight(1f))
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                }
                AnimatedVisibility(expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        debug.forEach { (k, v) -> DetailRow(k, v, monospace = true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, monospace: Boolean = false) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            modifier = Modifier.weight(0.55f),
        )
    }
}
