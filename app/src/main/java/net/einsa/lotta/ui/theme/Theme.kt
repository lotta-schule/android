package net.einsa.lotta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.einsa.lotta.model.Theme

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun LottaTheme(
    theme: Theme,
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = Color(theme.primaryColor.toArgb()),
        tertiaryContainer = Color(theme.navigationBackgroundColor.toArgb()),
        error = Color(theme.errorColor.toArgb()),
        background = Color(theme.pageBackgroundColor.toArgb()),
        // inversePrimary = Color(theme.primaryContrastTextColor.pack()),
        // surface = Color(theme.boxBackgroundColor.pack()),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}