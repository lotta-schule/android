package net.einsa.lotta.ui.view.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.util.ConversationUtil

@Composable
fun MessagingView(viewModel: MessagingViewModel = viewModel(), navController: NavHostController) {
    val userSession = LocalUserSession.current
    val scope = rememberCoroutineScope()

    DisposableEffect(key1 = userSession, key2 = scope) {
        val job = scope.launch {
            viewModel.watchConversations(userSession)
        }
        onDispose {
            job.cancel()
        }
    }

    ConversationsList(onSelectConversation = { conversation ->
        navController.navigate(
            "messages/${conversation.id}?title=${
                ConversationUtil.getTitle(
                    conversation,
                    userSession.user.id
                )
            }"
        )
    })
}
