package com.oralscan.app.ui.analyze

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.oralscan.app.AppContainer
import com.oralscan.app.ui.Routes
import com.oralscan.app.ui.common.requireContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

sealed interface AnalyzeUiState {
    data object Running : AnalyzeUiState
    data class Done(val scanId: Long) : AnalyzeUiState
    data class Error(val message: String) : AnalyzeUiState
}

class AnalyzeViewModel(
    savedStateHandle: SavedStateHandle,
    private val container: AppContainer,
) : ViewModel() {
    val imagePath: String = savedStateHandle.get<String>(Routes.ARG_PATH).orEmpty()
    private val scanContext = Routes.scanContextFrom(savedStateHandle)

    private val analyzer = container.createAnalyzer()
    val modelName: String = analyzer.name

    private val _state = MutableStateFlow<AnalyzeUiState>(AnalyzeUiState.Running)
    val state: StateFlow<AnalyzeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { run() }
    }

    private suspend fun run() {
        val bitmap = withContext(Dispatchers.IO) { container.imageStore.loadBitmap(imagePath) }
        if (bitmap == null) {
            _state.value = AnalyzeUiState.Error(
                "This image couldn't be read. It may be corrupted or in an unsupported format."
            )
            return
        }
        _state.value = try {
            val result = analyzer.analyze(bitmap)
            val id = container.repository.save(bitmap, result, analyzer.name, scanContext)
            container.imageStore.delete(imagePath) // the saved scan has its own copy now
            AnalyzeUiState.Done(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AnalyzeUiState.Error("Couldn't save the scan: ${e.message}")
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AnalyzeViewModel(createSavedStateHandle(), requireContainer()) }
        }
    }
}

@Composable
fun AnalyzeScreen(
    onFinished: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AnalyzeViewModel = viewModel(factory = AnalyzeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        (state as? AnalyzeUiState.Done)?.let { onFinished(it.scanId) }
    }
    // Don't allow leaving mid-analysis; the result is saved automatically when it finishes.
    BackHandler(enabled = state is AnalyzeUiState.Running) {}

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = File(viewModel.imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .alpha(if (state is AnalyzeUiState.Error) 0.4f else 1f),
            )
            Spacer(Modifier.size(32.dp))

            when (val s = state) {
                is AnalyzeUiState.Error -> {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text("Analysis failed", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.size(8.dp))
                    Text(s.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(24.dp))
                    Button(onClick = onBack) { Text("Go back") }
                }
                else -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(20.dp))
                    Text("Analyzing on device…", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        viewModel.modelName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Nothing is uploaded. Everything runs on this phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
