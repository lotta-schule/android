package net.einsa.lotta.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import kotlinx.coroutines.launch
import net.einsa.lotta.App
import net.einsa.lotta.composition.LocalTheme
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.ui.component.Avatar
import net.einsa.lotta.ui.component.UserAvatar
import net.einsa.lotta.ui.component.layout.BottomNavigationBar
import net.einsa.lotta.ui.component.layout.TopAppBar
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
        "messages/{conversationId}?title={title}&imageUrl={imageUrl}", arguments = listOf(
            navArgument("title") { nullable = true },
            navArgument("imageUrl") { nullable = true }
        )
    ),
}

@Composable
fun MainView(vm: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry = navController.currentBackStackEntryAsState()
    var showNewConversationDialog by remember { mutableStateOf(false) }
    val theme = LocalTheme.current

    val session = LocalUserSession.current

    val currentScreen =
        MainScreen.entries.find { it.route == backStackEntry.value?.destination?.route }
            ?: MainScreen.CONVERSATIONS

    val canNavigateBack = navController.currentBackStackEntry != null &&
            currentScreen.route != MainScreen.CONVERSATIONS.route && currentScreen.route != MainScreen.PROFILE.route

    val canCreateMessage = currentScreen.route == MainScreen.CONVERSATIONS.route

    val currentlySelectedConversationId =
        backStackEntry.value?.arguments?.getString("conversationId")

    DisposableEffect(currentlySelectedConversationId, backStackEntry.value?.id) {
        val job = scope.launch {
            vm.updateNewMessageCounts(ignoringConversationId = currentlySelectedConversationId)
        }

        onDispose { job.cancel() }
    }

    DisposableEffect(session) {
        val job = scope.launch {
            vm.subscribeToMessages()
        }

        onDispose {
            job.cancel()
        }
    }

    DisposableEffect(session) {
        val job = scope.launch {
            vm.watchNewMessageCount()
        }

        onDispose {
            job.cancel()
        }
    }

    if (showNewConversationDialog) {
        Dialog(onDismissRequest = { showNewConversationDialog = false }) {
            CreateConversationView(
                modifier = Modifier.fillMaxHeight(.75f),
                onDismiss = { showNewConversationDialog = false },
                onSelect = { destination, user, group ->
                    showNewConversationDialog = false
                    vm.onCreateNewMessage(destination, user, group)
                        .let(navController::navigate)
                })
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
                    ?: currentScreen.title ?: currentScreen.name,
                leftSection = {
                    if (currentScreen.name === "CONVERSATIONS") {
                        Box(Modifier.padding(start = theme.spacing)) {
                            UserAvatar(
                                session.user,
                                size = 40,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                rightSection = {
                    if (currentScreen.name == "CONVERSATION") {
                        navController.currentBackStackEntry?.arguments?.getString("imageUrl")?.let {
                            Box(Modifier.padding(end = theme.spacing)) {
                                Avatar(
                                    url = it,
                                    size = 40,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentNewMessageCount = vm.newMessageCount,
                otherNewMessageCount = vm.otherNewMessageCount,
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
                ProfileView(onSwitchProfile = {
                    navController.navigate(MainScreen.MESSAGING.route)
                })
            }
        }
    }

    LaunchedEffect(session) {
        App.mainActivity.intent?.extras?.let { extras ->
            if (extras.getString("category") == "receive_message") {
                val tenantId = extras.getString("tenant_id")

                val conversationId = extras.getString("conversation_id")

                if (tenantId == session.tenant.id && conversationId != null) {
                    val conversation = vm.getConversation(conversationId)
                    val user = conversation?.users?.firstOrNull { it.id != session.user.id }

                    val imageUrl = user?.avatarImageFile?.formats?.find { true }?.url
                    navController.navigate(
                        MainScreen.CONVERSATION.route
                            .replace("{conversationId}", conversationId)
                            .replace("{title}", extras.getString("title") ?: "?")
                            .replace("{imageUrl}", imageUrl ?: "")
                    )

                }

                extras.remove("category")
            }
        }
    }

}


@Preview
@Composable
fun MainViewPreview() {
    MainView()
}