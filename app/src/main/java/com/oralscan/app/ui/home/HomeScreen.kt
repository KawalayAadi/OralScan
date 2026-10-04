package com.oralscan.app.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oralscan.app.AppContainer
import com.oralscan.app.ui.common.DISCLAIMER
import com.oralscan.app.ui.common.requireContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val scanCount: StateFlow<Int> = container.repository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val fakeMask = container.devSettings.fakeMask
    val simulateFailure = container.devSettings.simulateFailure

    fun setFakeMask(enabled: Boolean) = container.devSettings.setFakeMask(enabled)
    fun setSimulateFailure(enabled: Boolean) = container.devSettings.setSimulateFailure(enabled)

    /** Copies the picked image into app storage and returns its path, or null on failure. */
    suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        container.imageStore.importFromUri(uri)?.absolutePath
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(requireContainer()) }
        }
    }
}

@Composable
fun HomeScreen(
    onTakePhoto: () -> Unit,
    onPhotoImported: (String) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanCount by viewModel.scanCount.collectAsStateWithLifecycle()
    var showDevOptions by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val path = viewModel.importPhoto(uri)
            if (path != null) {
                onPhotoImported(path)
            } else {
                Toast.makeText(context, "Couldn't open that image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CameraAlt, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("OralScan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "On-device oral lesion screening",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showDevOptions = true }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Developer options")
                }
            }

            Spacer(Modifier.height(8.dp))

            HomeActionCard(
                icon = Icons.Filled.CameraAlt,
                title = "Take photo",
                subtitle = "In-app camera with an alignment guide",
                primary = true,
                onClick = onTakePhoto,
            )
            HomeActionCard(
                icon = Icons.Filled.PhotoLibrary,
                title = "Upload photo",
                subtitle = "Choose an existing image from the gallery",
                onClick = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            HomeActionCard(
                icon = Icons.Filled.History,
                title = "Scan history",
                subtitle = when (scanCount) {
                    0 -> "No scans yet"
                    1 -> "1 saved scan"
                    else -> "$scanCount saved scans"
                },
                onClick = onOpenHistory,
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WifiOff, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "Works fully offline. Photos and results never leave this phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Text(
                DISCLAIMER,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    if (showDevOptions) {
        DevOptionsDialog(viewModel = viewModel, onDismiss = { showDevOptions = false })
    }
}

@Composable
private fun HomeActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    val container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val content = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val iconBackground = if (primary) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer
    val iconTint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = if (primary) 24.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBackground, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.8f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun DevOptionsDialog(viewModel: HomeViewModel, onDismiss: () -> Unit) {
    val fakeMask by viewModel.fakeMask.collectAsStateWithLifecycle()
    val simulateFailure by viewModel.simulateFailure.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Developer options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "For testing the app before the real model is added.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                DevToggle(
                    title = "Fake lesion mask",
                    subtitle = "Draw a demo region so the overlay can be tested",
                    checked = fakeMask,
                    onCheckedChange = viewModel::setFakeMask,
                )
                DevToggle(
                    title = "Simulate model failure",
                    subtitle = "Make the placeholder model report an error",
                    checked = simulateFailure,
                    onCheckedChange = viewModel::setSimulateFailure,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun DevToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
