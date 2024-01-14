package net.einsa.lotta.ui.view.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.einsa.lotta.composition.LocalUserSession

@Composable()
fun MessagingView(viewModel: MessagingViewModel = viewModel()) {
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

    ConversationsList()
}
