package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorBorderMuted = androidx.compose.ui.graphics.Color(0xFF1C2333)

private val ClutchXColorScheme = darkColorScheme(
    primary = ElectricOrange,
    onPrimary = TextPrimary,
    primaryContainer = ElectricOrangeContainer,
    onPrimaryContainer = ElectricOrangeBright,
    secondary = ElectricBlue,
    onSecondary = ObsidianBg,
    secondaryContainer = ElectricBlueContainer,
    onSecondaryContainer = ElectricBlueBright,
    tertiary = VictoryGold,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalElevated,
    surfaceContainerHigh = CharcoalHighest,
    outline = BorderCarbon,
    outlineVariant = ColorBorderMuted,
    error = CrimsonError,
    onError = TextPrimary,
    errorContainer = CrimsonErrorBg
)

val ClutchXShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ClutchXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClutchXColorScheme,
        typography = ClutchXTypography,
        shapes = ClutchXShapes,
        content = content
    )
}
