package net.einsa.lotta.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.composition.LocalUserSession

@Composable
fun MessageList(messages: List<GetConversationQuery.Message>, isGroupChat: Boolean, modifier: Modifier = Modifier) {
    val session = LocalUserSession.current
    val listState = rememberLazyListState()

    LaunchedEffect(messages.count()) {
        if (!listState.isScrollInProgress && messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(session.tenant.customTheme.boxBackgroundColor.toArgb())),
        reverseLayout = true,
        state = listState
    ) {
        items(messages, key = { it.id!! }) { message ->
            MessageRow(message, fromCurrentUser = message.user?.id == session.user.id, isGroupChat = isGroupChat)
        }
    }
}