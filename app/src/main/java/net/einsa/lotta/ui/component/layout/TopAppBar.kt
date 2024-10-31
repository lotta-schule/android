package net.einsa.lotta.ui.component.layout

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.einsa.lotta.composition.LocalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: String,
    leftSection: (@Composable () -> Unit)?,
    rightSection: (@Composable () -> Unit)?,
    onCreateMessage: (() -> Unit)?,
    onNavigateBack: (() -> Unit)?
) {
    val theme = LocalTheme.current

    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück"
                    )
                }
            } else if (leftSection != null) {
                leftSection()
            }
        },
        actions = {
            if (onCreateMessage != null) {
                IconButton(onClick = onCreateMessage) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Neue Nachricht schreiben"
                    )
                }
            } else if (rightSection != null) {
                rightSection()
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            titleContentColor = Color(theme.textColor.toArgb()),
            containerColor = Color(theme.pageBackgroundColor.toArgb()),
            actionIconContentColor = Color(theme.textColor.toArgb()),
            navigationIconContentColor = Color(theme.textColor.toArgb()),
        )
    )
}

