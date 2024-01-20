package net.einsa.lotta.ui.view.messaging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.model.ID
import net.einsa.lotta.ui.component.MessageInputView
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

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxHeight()
    ) {
        MessageList(
            messages = vm.conversation?.messages ?: emptyList(),
            modifier = Modifier.weight(1f)
        )
        MessageInputView(
            userId = vm.conversation?.users?.firstOrNull { it.id != session.user.id }?.id,
            groupId = vm.conversation?.groups?.firstOrNull()?.id
        )
    }
}