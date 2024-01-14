package net.einsa.lotta.ui.view.messaging

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.ui.component.Avatar
import net.einsa.lotta.util.ConversationUtil

@Composable()
fun ConversationsList(vm: ConversationsListViewModel = viewModel()) {
    val userSession = LocalUserSession.current
    val scope = rememberCoroutineScope()

    DisposableEffect(key1 = userSession) {
        val job = scope.launch { vm.watchConversations(userSession) }
        onDispose {
            job.cancel()
        }
    }

    LazyColumn {
        items(vm.conversations, key = { it.id!! }) { conversation ->
            ConversationListItemView(conversation)
        }
    }
}

@Composable()
fun ConversationListItemView(conversation: GetConversationsQuery.Conversation) {
    val userSession = LocalUserSession.current
    val imageUrl = ConversationUtil.getImage(
        conversation = conversation,
        excludingUserId = userSession.user.id,
        tenant = userSession.tenant
    )

    Row(
        modifier = Modifier
            .height(48.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        imageUrl?.let {
            Avatar(
                url = imageUrl,
                contentDescription = "Avatar von ${
                    ConversationUtil.getTitle(
                        conversation,
                        excludingUserId = userSession.user.id
                    )
                }",
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp)
            )
        }
        Text(
            ConversationUtil.getTitle(
                conversation = conversation,
                excludingUserId = userSession.user.id
            )
        )
    }
}