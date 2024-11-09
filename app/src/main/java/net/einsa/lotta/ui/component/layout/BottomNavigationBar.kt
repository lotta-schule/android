package net.einsa.lotta.ui.component.layout

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import net.einsa.lotta.composition.LocalTheme
import net.einsa.lotta.ui.component.BadgedIcon
import net.einsa.lotta.ui.view.MainScreen

@Composable
fun BottomNavigationBar(
    currentNewMessageCount: Int,
    otherNewMessageCount: Int,
    onSelect: (MainScreen) -> Unit,
    currentNavDestination: NavDestination? = null
) {
    val theme = LocalTheme.current

    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color(theme.primaryColor.toArgb()),
        selectedTextColor = Color(theme.navigationContrastTextColor.toArgb()),
        indicatorColor = ColorUtils.blendARGB(
            theme.navigationBackgroundColor.toArgb(),
            theme.primaryColor.toArgb(),
            0.1f
        ).let {
            Color(it)
        },
        unselectedIconColor = Color(theme.navigationContrastTextColor.toArgb()),
        unselectedTextColor = Color(theme.navigationContrastTextColor.toArgb())
    )

    NavigationBar(
        containerColor = Color(theme.navigationBackgroundColor.toArgb()),
    ) {
        NavigationBarItem(
            selected = currentNavDestination?.hierarchy?.any { it.route == MainScreen.MESSAGING.route }
                ?: false,
            onClick = { onSelect(MainScreen.MESSAGING) },
            icon = {
                BadgedIcon(currentNewMessageCount) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = null
                    )
                }
            },
            label = { MainScreen.MESSAGING.title?.let { Text(it) } },
            colors = colors
        )
        NavigationBarItem(
            selected = currentNavDestination?.hierarchy?.any { it.route == MainScreen.PROFILE.route }
                ?: false,
            onClick = { onSelect(MainScreen.PROFILE) },
            icon = {
                BadgedIcon(otherNewMessageCount) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                }
            },
            label = { MainScreen.PROFILE.title?.let { Text(it) } },
            colors = colors
        )
    }
}
