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

private val DorbarLightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = NavyDeep,
    secondary = RoyalPurple,
    onSecondary = Color.White,
    secondaryContainer = RoyalPurpleContainer,
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = WarmGold,
    onTertiary = NavyDeep,
    tertiaryContainer = WarmGoldContainer,
    onTertiaryContainer = Color(0xFF451A03),
    background = SoftGreyBackground,
    onBackground = SlateTextPrimary,
    surface = Color.White,
    onSurface = SlateTextPrimary,
    surfaceVariant = SoftGreySurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    outline = SoftGreyBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = RoseError,
    onError = Color.White,
    errorContainer = RoseContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DorbarDarkColorScheme = darkColorScheme(
    primary = IndigoLight,
    onPrimary = NavyDeep,
    primaryContainer = IndigoDarkContainer,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = RoyalPurpleLight,
    onSecondary = NavyDeep,
    secondaryContainer = RoyalPurpleDarkContainer,
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = WarmGold,
    onTertiary = NavyDeep,
    tertiaryContainer = Color(0xFF451A03),
    onTertiaryContainer = WarmGoldContainer,
    background = NavyDeep,
    onBackground = LightTextPrimary,
    surface = NavySurface,
    onSurface = LightTextPrimary,
    surfaceVariant = NavyContainer,
    onSurfaceVariant = LightTextSecondary,
    outline = NavyOutline,
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    onError = NavyDeep,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = RoseContainer
)

val DorbarShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DorbarTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DorbarDarkColorScheme else DorbarLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DorbarShapes,
        content = content
    )
}
