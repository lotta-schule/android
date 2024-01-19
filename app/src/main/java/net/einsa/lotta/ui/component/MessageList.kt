package net.einsa.lotta.ui.component

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.composition.LocalUserSession

@Composable
fun MessageList(messages: List<GetConversationQuery.Message>) {
    val session = LocalUserSession.current

    LazyColumn {
        items(messages, key = { it.id!! }) { message ->
            MessageRow(message, fromCurrentUser = message.user?.id == session.user.id)
        }
    }
}