package net.einsa.lotta.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.composition.LocalUserSession

private const val LOAD_OLDER_THRESHOLD = 5

@Composable
fun MessageList(
    messages: List<GetConversationQuery.Message>,
    isGroupChat: Boolean,
    modifier: Modifier = Modifier,
    hasMoreOlder: Boolean = false,
    isLoadingOlder: Boolean = false,
    onLoadOlder: () -> Unit = {},
) {
    val session = LocalUserSession.current
    val listState = rememberLazyListState()

    // Newest first, because the list is laid out in reverse (index 0 is the bottom)
    val sortedMessages = remember(messages) {
        messages.sortedWith(
            compareByDescending<GetConversationQuery.Message> { it.id.toLongOrNull() ?: Long.MIN_VALUE }
                .thenByDescending { it.updatedAt }
        )
    }
    val newestId = sortedMessages.firstOrNull()?.id

    // Only scroll to the bottom when a *newer* message arrives, not when older pages are prepended
    LaunchedEffect(newestId) {
        if (!listState.isScrollInProgress && newestId != null) {
            listState.animateScrollToItem(0)
        }
    }

    val currentOnLoadOlder by rememberUpdatedState(onLoadOlder)
    val currentHasMore by rememberUpdatedState(hasMoreOlder)

    // Triggers whenever the oldest messages come close to being visible. Re-evaluated when a
    // page finishes loading, so it keeps loading while the end of the list is still on screen.
    LaunchedEffect(listState, isLoadingOlder, sortedMessages.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= info.totalItemsCount - 1 - LOAD_OLDER_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it && currentHasMore && !isLoadingOlder && sortedMessages.isNotEmpty() }
            .collect { currentOnLoadOlder() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(session.tenant.customTheme.boxBackgroundColor.toArgb())),
        reverseLayout = true,
        state = listState
    ) {
        items(sortedMessages, key = { it.id }) { message ->
            MessageRow(message, fromCurrentUser = message.user?.id == session.user.id, isGroupChat = isGroupChat)
        }
        if (isLoadingOlder) {
            item(key = "older-messages-loader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
