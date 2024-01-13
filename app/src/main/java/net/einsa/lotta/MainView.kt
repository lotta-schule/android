package net.einsa.lotta

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.einsa.lotta.ui.theme.LottaTheme

@Composable()
fun MainView() {
    Scaffold(
        topBar = { TopAppBar() },
        bottomBar = { BottomNavigationBar() },
    ) { innerPadding ->
        Text(
            "Nachrichten", modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable()
fun TopAppBar() {
    MediumTopAppBar(
        title = { Text("Lotta") },
        actions = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Neue Nachricht schreiben"
            )
        }
    )
}

@Composable()
fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = { /*TODO*/ },
            icon = {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = null
                )
            },
            label = { Text("Nachrichten") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /*TODO*/ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null
                )
            },
            label = { Text("Profil") }
        )
    }
}

@Preview()
@Composable()
fun MainViewPreview() {
    LottaTheme {
        MainView()
    }
}