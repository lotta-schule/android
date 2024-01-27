package net.einsa.lotta.ui.view.messaging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.einsa.lotta.model.ID
import net.einsa.lotta.ui.component.MessageInputView
import net.einsa.lotta.ui.view.MainScreen
import net.einsa.lotta.util.UserUtil

@Composable
fun NewConversationView(userId: ID?, groupId: ID?, onSent: (path: String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxHeight()
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        MessageInputView(
            userId = userId,
            groupId = groupId,
        ) { message ->
            val conversationId = message.conversation?.id!!
            val title =
                if (userId != null) {
                    message.conversation.users?.firstOrNull { it.id == userId }?.let {
                        UserUtil.getVisibleName(it)
                    } ?: "?"
                } else {
                    message.conversation.groups?.firstOrNull()?.name ?: "?"
                }

            onSent(
                MainScreen.CONVERSATION.route.replace(
                    "{conversationId}",
                    conversationId
                ).replace(
                    "{title}",
                    title
                )
            )
        }
    }
}