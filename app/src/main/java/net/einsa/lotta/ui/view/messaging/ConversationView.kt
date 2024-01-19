package net.einsa.lotta.ui.view.messaging

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.model.ID
import net.einsa.lotta.ui.component.MessageList

@Composable
fun ConversationView(conversationId: ID, vm: ConversationViewModel = viewModel()) {
    val session = LocalUserSession.current
    val scope = rememberCoroutineScope()

    DisposableEffect(key1 = session) {
        val job = scope.launch { vm.watchConversation(conversationId, session) }
        onDispose {
            job.cancel()
        }
    }

    Column {
        MessageList(
            messages = vm.conversation?.messages ?: emptyList()
        )
    }
}