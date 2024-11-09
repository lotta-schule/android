package net.einsa.lotta.ui.component

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.einsa.lotta.composition.LocalTheme

@Composable
fun BadgedIcon(count: Int?, icon: @Composable () -> Unit) {
    val theme = LocalTheme.current

    if (count == null || count == 0) {
        icon()
        return
    }

    BadgedBox(
        badge = {
            Badge(
                containerColor = Color(theme.primaryColor.toArgb()),
                contentColor = Color(theme.primaryContrastTextColor.toArgb()),
            ) {
                Text(
                    count.toString(),
                )
            }
        }) {
        icon()
    }
}