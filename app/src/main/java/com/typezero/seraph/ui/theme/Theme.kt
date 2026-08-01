package com.typezero.seraph.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Ink,
    primaryContainer = TealDeep.copy(alpha = 0.34f),
    onPrimaryContainer = Frost,
    secondary = Violet,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF292340),
    onSecondaryContainer = Frost,
    tertiary = Amber,
    background = Ink,
    onBackground = Frost,
    surface = Graphite900,
    onSurface = Frost,
    surfaceVariant = Graphite850,
    onSurfaceVariant = Mist,
    surfaceContainer = Graphite900,
    surfaceContainerHigh = Graphite850,
    surfaceContainerHighest = Graphite800,
    outline = Graphite700,
    outlineVariant = Graphite800,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A61),
    secondary = Color(0xFF63558F),
    tertiary = Color(0xFF765A00),
)

private val SeraphShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun SeraphTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = SeraphShapes,
        content = content,
    )
}
