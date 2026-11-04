package io.github.lobojoel2003.saywell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.lobojoel2003.saywell.data.ThemeChoice

private val Teal = Color(0xFF0E6E5C)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F0DC),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4A635B),
    secondaryContainer = Color(0xFFCDE8DF),
    onSecondaryContainer = Color(0xFF06201A),
    background = Color(0xFFF8FAF9),
    surface = Color(0xFFF8FAF9),
    surfaceVariant = Color(0xFFDBE5E0),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF86D6BE),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFFA2F2DA),
    secondary = Color(0xFFB1CCC2),
    secondaryContainer = Color(0xFF334B44),
    onSecondaryContainer = Color(0xFFCDE8DF),
    background = Color(0xFF101413),
    surface = Color(0xFF101413),
    surfaceVariant = Color(0xFF3F4945),
)

private val HighContrastLight = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEB3B),
    onPrimaryContainer = Color.Black,
    secondary = Color.Black,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color.Black,
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    onBackground = Color.Black,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
)

private val HighContrastDark = darkColorScheme(
    primary = Color(0xFFFFEB3B),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFEB3B),
    onPrimaryContainer = Color.Black,
    secondary = Color.White,
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color.White,
    background = Color.Black,
    surface = Color.Black,
    onSurface = Color.White,
    onBackground = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color.White,
    outline = Color.White,
)

/** Colours used on the talking screen beyond the Material scheme. */
@Immutable
data class SaywellColors(
    /** The scanning highlight: thick and unmistakable in both themes. */
    val scanHighlight: Color,
    val cellBorder: Color,
    val emptyCell: Color,
    val highContrast: Boolean,
)

val LocalSaywellColors = staticCompositionLocalOf {
    SaywellColors(scanHighlight = Color(0xFFFF6F00), cellBorder = Color(0x33000000), emptyCell = Color(0x0F000000), highContrast = false)
}

@Composable
fun SaywellTheme(theme: ThemeChoice = ThemeChoice.SYSTEM, highContrast: Boolean = false, content: @Composable () -> Unit) {
    val dark = when (theme) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    val scheme = when {
        highContrast && dark -> HighContrastDark
        highContrast -> HighContrastLight
        dark -> DarkColors
        else -> LightColors
    }
    val extra = SaywellColors(
        scanHighlight = if (dark) Color(0xFFFFB300) else Color(0xFFE65100),
        cellBorder = if (highContrast) scheme.outline else if (dark) Color(0x40FFFFFF) else Color(0x33000000),
        emptyCell = if (dark) Color(0x14FFFFFF) else Color(0x0F000000),
        highContrast = highContrast,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalSaywellColors provides extra) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
