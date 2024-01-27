package net.einsa.lotta.ui.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
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
import net.einsa.lotta.ui.view.messaging.CreateConversationView
import net.einsa.lotta.ui.view.messaging.MessagingView
import net.einsa.lotta.ui.view.messaging.NewConversationView
import net.einsa.lotta.ui.view.profile.ProfileView

enum class MainScreen(
    val route: String,
    val title: String? = null,
    arguments: List<NamedNavArgument> = emptyList()
) {
    MESSAGING("messages", title = "Nachrichten"),
    PROFILE("profile", title = "Profil"),
    CONVERSATIONS("messages/all", title = "Nachrichten"),
    NEW_CONVERSATION(
        "messages/new?title={title}&groupId={groupId}&userId={userId}", arguments = listOf(
            navArgument("title") { nullable = true },
            navArgument("groupId") { nullable = true },
            navArgument("userId") { nullable = true },
        )
    ),
    CONVERSATION(
        "messages/{conversationId}?title={title}", arguments = listOf(
            navArgument("title") { nullable = true }
        )
    ),
}

@Composable
fun MainView(vm: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry = navController.currentBackStackEntryAsState()
    var showNewConversationDialog by remember { mutableStateOf(false) }

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

    DisposableEffect(session) {
        val job = scope.launch {
            vm.watchNewMessageCount(session)
        }

        onDispose {
            job.cancel()
        }
    }

    if (showNewConversationDialog) {
        Dialog(onDismissRequest = { showNewConversationDialog = false }) {
            CreateConversationView { destination, user, group ->
                showNewConversationDialog = false
                vm.onCreateNewMessage(destination, user, group, session)
                    .let(navController::navigate)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                onNavigateBack = if (!canNavigateBack) null else ({ navController.popBackStack() }),
                onCreateMessage = if (!canCreateMessage) null else ({
                    showNewConversationDialog = true
                }),
                title = navController.currentBackStackEntry?.arguments?.getString("title")
                    ?: currentScreen.title ?: currentScreen.name
            )
        },
        bottomBar = {
            BottomNavigationBar(
                newMessageCount = vm.newMessageCount,
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
                composable(route = MainScreen.NEW_CONVERSATION.route) {
                    val userId = it.arguments?.getString("userId")
                    val groupId = it.arguments?.getString("groupId")
                    NewConversationView(userId, groupId) { path ->
                        navController.navigate(path) {
                            popUpTo(MainScreen.NEW_CONVERSATION.route) { inclusive = true }
                        }
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable()
fun BottomNavigationBar(
    newMessageCount: Int,
    onSelect: (MainScreen) -> Unit,
    currentNavDestination: NavDestination? = null
) {
    val modelData = LocalModelData.current

    val theme = modelData.theme

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
                if (newMessageCount == 0) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = null
                    )
                } else {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = Color(theme.primaryColor.toArgb()),
                                contentColor = Color(theme.primaryContrastTextColor.toArgb()),
                            ) {
                                Text(
                                    newMessageCount.toString(),
                                )
                            }
                        }) {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = null
                        )
                    }
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