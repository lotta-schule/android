package net.einsa.lotta

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import net.einsa.lotta.ui.theme.LottaTheme
import net.einsa.lotta.ui.view.messaging.MessagingView

enum class MainScreen(val route: String, val title: String? = null) {
    MESSAGING("messages", title = "Nachrichten"),
    PROFILE("profile", title = "Profil"),
    CONVERSATIONS("messages/all", title = "Nachrichten"),
    CONVERSATION("messages/{conversationId}")
}

@Composable()
fun MainView() {
    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState()

    val currentScreen =
        MainScreen.entries.find { it.route == backStackEntry.value?.destination?.route }
            ?: MainScreen.MESSAGING

    Scaffold(
        topBar = {
            TopAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.currentBackStackEntry != null,
                onNavigateBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentNavDestination = backStackEntry.value?.destination,
                onSelect = { navController.navigate(it.route) }
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainScreen.MESSAGING.route,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            navigation(
                startDestination = MainScreen.CONVERSATIONS.route,
                route = MainScreen.MESSAGING.route
            ) {
                composable(route = MainScreen.CONVERSATIONS.route) {
                    MessagingView()
                }
                composable(route = MainScreen.CONVERSATION.route) {
                    Text("Nachricht")
                }
            }
            composable(route = MainScreen.PROFILE.route) {
                Text("Profil")
            }
        }

        Text(
            navController.currentDestination?.route ?: "keine aktuelle Route"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable()
fun TopAppBar(currentScreen: MainScreen, canNavigateBack: Boolean, onNavigateBack: () -> Unit) {
    MediumTopAppBar(
        title = { currentScreen.title?.let { Text(it) } },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Zurück"
                    )

                }
            }
        },
        actions = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Neue Nachricht schreiben"
            )
        }
    )
}

@Composable()
fun BottomNavigationBar(
    onSelect: (MainScreen) -> Unit,
    currentNavDestination: NavDestination? = null
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentNavDestination?.hierarchy?.any { it.route == MainScreen.MESSAGING.route }
                ?: false,
            onClick = { onSelect(MainScreen.MESSAGING) },
            icon = {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = null
                )
            },
            label = { MainScreen.MESSAGING.title?.let { Text(it) } }
        )
        NavigationBarItem(
            selected = currentNavDestination?.hierarchy?.any { it.route == MainScreen.PROFILE.route }
                ?: false,
            onClick = { onSelect(MainScreen.PROFILE) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null
                )
            },
            label = { MainScreen.PROFILE.title?.let { Text(it) } }
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