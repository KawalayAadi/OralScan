package com.oralscan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = TealLightPrimary,
    onPrimary = Color.White,
    primaryContainer = TealLightPrimaryContainer,
    onPrimaryContainer = TealLightOnPrimaryContainer,
    secondary = TealLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealLightSecondaryContainer,
    onSecondaryContainer = TealLightOnSecondaryContainer,
    tertiary = TealLightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TealLightTertiaryContainer,
    onTertiaryContainer = TealLightOnTertiaryContainer,
    background = TealLightBackground,
    onBackground = TealLightOnBackground,
    surface = TealLightBackground,
    onSurface = TealLightOnBackground,
    surfaceVariant = TealLightSurfaceVariant,
    onSurfaceVariant = TealLightOnSurfaceVariant,
    outline = TealLightOutline,
    outlineVariant = TealLightOutlineVariant,
    surfaceContainerLowest = TealLightContainerLowest,
    surfaceContainerLow = TealLightContainerLow,
    surfaceContainer = TealLightContainer,
    surfaceContainerHigh = TealLightContainerHigh,
    surfaceContainerHighest = TealLightContainerHighest,
)

private val DarkColors = darkColorScheme(
    primary = TealDarkPrimary,
    onPrimary = TealDarkOnPrimary,
    primaryContainer = TealDarkPrimaryContainer,
    onPrimaryContainer = TealDarkOnPrimaryContainer,
    secondary = TealDarkSecondary,
    secondaryContainer = TealDarkSecondaryContainer,
    onSecondaryContainer = TealDarkOnSecondaryContainer,
    tertiary = TealDarkTertiary,
    tertiaryContainer = TealDarkTertiaryContainer,
    onTertiaryContainer = TealDarkOnTertiaryContainer,
    background = TealDarkBackground,
    onBackground = TealDarkOnBackground,
    surface = TealDarkBackground,
    onSurface = TealDarkOnBackground,
    surfaceVariant = TealDarkSurfaceVariant,
    onSurfaceVariant = TealDarkOnSurfaceVariant,
    outline = TealDarkOutline,
    outlineVariant = TealDarkOutlineVariant,
    surfaceContainerLowest = TealDarkContainerLowest,
    surfaceContainerLow = TealDarkContainerLow,
    surfaceContainer = TealDarkContainer,
    surfaceContainerHigh = TealDarkContainerHigh,
    surfaceContainerHighest = TealDarkContainerHighest,
)

@Immutable
data class StatusColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(SuccessContainerLight, OnSuccessContainerLight)
}

@Composable
fun OralScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val statusColors = if (darkTheme) {
        StatusColors(SuccessContainerDark, OnSuccessContainerDark)
    } else {
        StatusColors(SuccessContainerLight, OnSuccessContainerLight)
    }
    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography(),
            content = content,
        )
    }
}
