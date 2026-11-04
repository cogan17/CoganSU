package me.weishu.kernelsu.cogan

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import me.weishu.kernelsu.ui.theme.Typography

// Fixed steel-blue accents: the Masculine preset never inherits a light wallpaper palette.
val CoganDarkColors = darkColorScheme(
    primary = Color(0xFF9DCBFA),
    onPrimary = Color(0xFF002F50),
    primaryContainer = Color(0xFF153D60),
    onPrimaryContainer = Color(0xFFD2E7FF),
    inversePrimary = Color(0xFF315F86),
    secondary = Color(0xFFBCC9D8),
    onSecondary = Color(0xFF263340),
    secondaryContainer = Color(0xFF293B4D),
    onSecondaryContainer = Color(0xFFD8E5F4),
    tertiary = Color(0xFF9CD5C2),
    onTertiary = Color(0xFF00382B),
    tertiaryContainer = Color(0xFF164F40),
    onTertiaryContainer = Color(0xFFB7F1DD),
    background = Color.Black,
    onBackground = Color(0xFFE7EDF4),
    surface = Color.Black,
    onSurface = Color(0xFFE7EDF4),
    surfaceVariant = Color(0xFF303943),
    onSurfaceVariant = Color(0xFFBCC6D2),
    inverseSurface = Color(0xFF303943),
    inverseOnSurface = Color(0xFFE7EDF4),
    surfaceTint = Color(0xFF9DCBFA),
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF19222C),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0C1117),
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color(0xFF19222C),
    surfaceContainerHighest = Color(0xFF232E39),
    outline = Color(0xFF8793A1),
    outlineVariant = Color(0xFF3C4856),
)

val CoganTypography = Typography.copy(
    headlineLarge = Typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = Typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)
