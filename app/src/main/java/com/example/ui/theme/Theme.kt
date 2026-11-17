package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF00252E),
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = Color(0xFFA6F5FF),
    secondary = SapphireBlue,
    onSecondary = Color.White,
    secondaryContainer = SapphireContainer,
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = KineticCoral,
    onTertiary = Color.White,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = Color(0xFFFFD8C9),
    background = ObsidianBg,
    onBackground = InkWhite,
    surface = ObsidianSurface,
    onSurface = InkWhite,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = InkMuted,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1F2B45)
)

private val FrostLightColorScheme = lightColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = Color(0xFF002938),
    secondary = SapphireLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = KineticCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDE6),
    onTertiaryContainer = Color(0xFF7C2D12),
    background = FrostBg,
    onBackground = InkDark,
    surface = FrostSurface,
    onSurface = InkDark,
    surfaceVariant = FrostSurfaceVariant,
    onSurfaceVariant = InkDarkMuted,
    surfaceContainerHigh = Color(0xFFEEF3FA),
    outline = FrostOutline,
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun GesturesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ObsidianDarkColorScheme else FrostLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
