package net.einsa.lotta.ui.theme

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import net.einsa.lotta.model.Theme

@Composable
fun LottaTheme(
    theme: Theme,
    content: @Composable () -> Unit
) {
    val tertiaryColor = Color(
        ColorUtils.blendARGB(
            theme.primaryColor.toArgb(),
            theme.navigationBackgroundColor.toArgb(),
            0.8f
        )
    )
    Log.d("LottaTheme", "tertiaryColor: $tertiaryColor")

    val colorScheme = lightColorScheme(
        primary = Color(theme.primaryColor.toArgb()),
        tertiary = tertiaryColor,
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