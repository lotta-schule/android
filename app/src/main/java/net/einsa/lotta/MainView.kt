package net.einsa.lotta

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import kotlinx.coroutines.launch
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.ui.view.messaging.ConversationView
import net.einsa.lotta.ui.view.messaging.MessagingView
import net.einsa.lotta.ui.view.profile.ProfileView

enum class MainScreen(
    val route: String,
    val title: String? = null,
    arguments: List<NamedNavArgument> = emptyList()
) {
    MESSAGING("messages", title = "Nachrichten"),
    PROFILE("profile", title = "Profil"),
    CONVERSATIONS("messages/all", title = "Nachrichten"),
    CONVERSATION(
        "messages/{conversationId}?title={title}", arguments = listOf(
            navArgument("title") { nullable = true }
        )
    )
}

@Composable
fun MainView(vm: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry = navController.currentBackStackEntryAsState()

    val session = LocalUserSession.current

    val currentScreen =
        MainScreen.entries.find { it.route == backStackEntry.value?.destination?.route }
            ?: MainScreen.CONVERSATIONS

    val canNavigateBack = navController.currentBackStackEntry != null &&
            currentScreen.route != MainScreen.CONVERSATIONS.route && currentScreen.route != MainScreen.PROFILE.route

    val canCreateMessage = currentScreen.route == MainScreen.CONVERSATIONS.route

    DisposableEffect(session) {
        val job = scope.launch {
            vm.subscribeToMessages(session)
        }

        onDispose {
            job.cancel()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                onNavigateBack = if (!canNavigateBack) null else ({ navController.popBackStack() }),
                onCreateMessage = if (!canCreateMessage) null else ({}),
                title = navController.currentBackStackEntry?.arguments?.getString("title")
                    ?: currentScreen.title ?: currentScreen.name
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
                    MessagingView(navController = navController)
                }
                composable(route = MainScreen.CONVERSATION.route) {
                    val conversationId = it.arguments?.getString("conversationId")
                    if (conversationId != null) {
                        ConversationView(conversationId)
                    }
                }
            }
            composable(route = MainScreen.PROFILE.route) {
                ProfileView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(title: String, onCreateMessage: (() -> Unit)?, onNavigateBack: (() -> Unit)?) {
    val modelData = LocalModelData.current

    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            onNavigateBack?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Zurück"
                    )
                }
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
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            titleContentColor = Color(modelData.theme.textColor.toArgb()),
            containerColor = Color(modelData.theme.pageBackgroundColor.toArgb()),
            actionIconContentColor = Color(modelData.theme.textColor.toArgb()),
            navigationIconContentColor = Color(modelData.theme.textColor.toArgb()),
        )
    )
}

@Composable()
fun BottomNavigationBar(
    onSelect: (MainScreen) -> Unit,
    currentNavDestination: NavDestination? = null
) {
    val modelData = LocalModelData.current

    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color(modelData.theme.primaryColor.toArgb()),
        selectedTextColor = Color(modelData.theme.navigationContrastTextColor.toArgb()),
        indicatorColor = ColorUtils.blendARGB(
            modelData.theme.navigationBackgroundColor.toArgb(),
            modelData.theme.primaryColor.toArgb(),
            0.1f
        ).let {
            Color(it)
        },
        unselectedIconColor = Color(modelData.theme.navigationContrastTextColor.toArgb()),
        unselectedTextColor = Color(modelData.theme.navigationContrastTextColor.toArgb())
    )

    NavigationBar(
        containerColor = Color(modelData.theme.navigationBackgroundColor.toArgb()),
    ) {
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
            label = { MainScreen.MESSAGING.title?.let { Text(it) } },
            colors = colors
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
            label = { MainScreen.PROFILE.title?.let { Text(it) } },
            colors = colors
        )
    }
}

@Preview
@Composable
fun MainViewPreview() {
    MainView()
}