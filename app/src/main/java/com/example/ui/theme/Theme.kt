package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarbarLightColorScheme = lightColorScheme(
    primary = RoyalPurple,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFEDE7FF),
    onPrimaryContainer = DeepNavy,
    secondary = IndigoPrimary,
    onSecondary = PureWhite,
    secondaryContainer = IndigoContainerLight,
    onSecondaryContainer = DeepNavy,
    tertiary = WarmGold,
    onTertiary = DeepNavy,
    tertiaryContainer = Color(0xFFFFF2D6),
    onTertiaryContainer = Color(0xFF472C00),
    background = SoftGreyBg,
    onBackground = DeepNavy,
    surface = PureWhite,
    onSurface = DeepNavy,
    surfaceVariant = Color(0xFFE7EAF4),
    onSurfaceVariant = Color(0xFF475073),
    outline = Color(0xFFC5CBE0),
    error = CrimsonError,
    onError = PureWhite
)

private val DarbarDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DeepNavy,
    primaryContainer = Color(0xFF39237D),
    onPrimaryContainer = Color(0xFFEDE7FF),
    secondary = DarkSecondary,
    onSecondary = DeepNavy,
    secondaryContainer = Color(0xFF233075),
    onSecondaryContainer = IndigoContainerLight,
    tertiary = DarkTertiary,
    onTertiary = Color(0xFF3A2500),
    tertiaryContainer = Color(0xFF5A3D00),
    onTertiaryContainer = Color(0xFFFFE5B0),
    background = DarkBg,
    onBackground = Color(0xFFEEF1FF),
    surface = DarkSurface,
    onSurface = Color(0xFFEEF1FF),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB3BCDB),
    outline = Color(0xFF3B4673),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0000)
)

val DarbarShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun DarbarTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (isDark) DarbarDarkColorScheme else DarbarLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DarbarTypography,
        shapes = DarbarShapes,
        content = content
    )
}
