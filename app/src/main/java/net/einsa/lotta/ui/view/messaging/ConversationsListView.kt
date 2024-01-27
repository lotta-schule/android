package net.einsa.lotta.ui.view.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.ui.component.Avatar
import net.einsa.lotta.util.ConversationUtil

@Composable
fun ConversationsList(
    vm: ConversationsListViewModel = viewModel(),
    onSelectConversation: (GetConversationsQuery.Conversation) -> Unit
) {
    val userSession = LocalUserSession.current
    val scope = rememberCoroutineScope()

    DisposableEffect(key1 = userSession) {
        val job = scope.launch { vm.watchConversations(userSession) }
        onDispose {
            job.cancel()
        }
    }

    LazyColumn {
        itemsIndexed(vm.conversations, key = { i, con -> con.id!! }) { index, conversation ->
            ConversationListItemView(
                conversation,
                onSelect = { onSelectConversation(conversation) },
            )
            if (index < vm.conversations.size - 1)
                Divider(
                    modifier = Modifier.padding(horizontal = userSession.tenant.customTheme.spacing),
                    color = Color(userSession.tenant.customTheme.dividerColor.toArgb()),
                    thickness = 1.dp
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListItemView(
    conversation: GetConversationsQuery.Conversation,
    onSelect: () -> Unit
) {
    val userSession = LocalUserSession.current
    val imageUrl = ConversationUtil.getImage(
        conversation = conversation,
        excludingUserId = userSession.user.id,
        tenant = userSession.tenant
    )

    val lineHeight = 54
    val imageSize = (lineHeight - userSession.tenant.customTheme.spacing.value * 2).toInt()

    Row(
        modifier = Modifier
            .height(lineHeight.dp)
            .padding(userSession.tenant.customTheme.spacing)
            .clickable { onSelect() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageUrl != null) {
            Avatar(
                url = imageUrl,
                size = imageSize,
                contentDescription = "Avatar von ${
                    ConversationUtil.getTitle(
                        conversation,
                        excludingUserId = userSession.user.id
                    )
                }",
                modifier = Modifier
                    .padding(userSession.tenant.customTheme.spacing)
            )
        } else {
            Spacer(modifier = Modifier.width(lineHeight.dp))
        }
        if ((conversation.unreadMessages ?: 0) > 0) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = Color(userSession.tenant.customTheme.primaryColor.toArgb()),
                        contentColor = Color(userSession.tenant.customTheme.primaryContrastTextColor.toArgb()),
                    ) {
                        Text(
                            conversation.unreadMessages.toString(),
                        )
                    }
                }) {
                Text(
                    ConversationUtil.getTitle(
                        conversation = conversation,
                        excludingUserId = userSession.user.id
                    )
                )
            }
        } else {
            Text(
                ConversationUtil.getTitle(
                    conversation = conversation,
                    excludingUserId = userSession.user.id
                )
            )
        }
    }
}