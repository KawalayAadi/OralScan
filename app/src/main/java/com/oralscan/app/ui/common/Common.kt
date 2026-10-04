package com.oralscan.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.oralscan.app.AppContainer
import com.oralscan.app.OralScanApplication
import com.oralscan.app.ui.theme.LocalStatusColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val DISCLAIMER =
    "OralScan is a screening aid for research use. It does not provide a medical diagnosis. " +
        "Always consult a qualified clinician."

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())

fun formatTimestamp(millis: Long): String = dateFormatter.format(Instant.ofEpochMilli(millis))

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as OralScanApplication).container

/** For ViewModel factories: `initializer { MyViewModel(requireContainer()) }`. */
fun CreationExtras.requireContainer(): AppContainer =
    (this[APPLICATION_KEY] as OralScanApplication).container

@Composable
fun StatusBadge(success: Boolean, modifier: Modifier = Modifier) {
    val status = LocalStatusColors.current
    val container = if (success) status.successContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (success) status.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (success) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(if (success) "Passed" else "Failed", style = MaterialTheme.typography.labelMedium)
        }
    }
}
