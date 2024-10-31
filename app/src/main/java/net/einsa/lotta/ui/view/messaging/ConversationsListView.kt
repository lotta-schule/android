package net.einsa.lotta.ui.view.messaging

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.type.DateTime
import net.einsa.lotta.ui.component.Avatar
import net.einsa.lotta.util.ConversationUtil
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalField
import java.util.Date
import java.util.logging.Level
import java.util.logging.Logger

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

    LazyColumn(Modifier.background(Color(userSession.tenant.customTheme.boxBackgroundColor.toArgb()))) {
        itemsIndexed(vm.conversations, key = { _, con -> con.id!! }) { index, conversation ->
            ConversationListItemView(
                conversation,
                onSelect = { onSelectConversation(conversation) },
            )
            if (index < vm.conversations.size - 1)
                HorizontalDivider(
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
            .fillMaxWidth()
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
        Column {
            if ((conversation.unreadMessages ?: 0) > 0) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = Color(userSession.tenant.customTheme.primaryColor.toArgb()),
                            contentColor = Color(userSession.tenant.customTheme.primaryContrastTextColor.toArgb()),
                            modifier = Modifier.padding(start = 8.dp)
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
                        ), Modifier.padding(end = 4.dp)
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
            formatLastUpdatedInfo(conversation)?.let {
                Text(
                    it,
                    style = TextStyle(fontSize = 10.sp),
                    color = Color(userSession.tenant.customTheme.disabledColor.toArgb())
                )
            }
        }
    }
}

private fun formatLastUpdatedInfo(conversation: GetConversationsQuery.Conversation): String? {
    try {
        val accessor = DateTimeFormatter.ISO_DATE_TIME.parse(conversation.updatedAt)
        val instant = Instant.from(accessor)
        return DateUtils.getRelativeTimeSpanString(instant.toEpochMilli()).toString()
    } catch (e: Exception) {
        Logger.getGlobal().log(Level.WARNING, "Could not parse date: ${conversation.updatedAt}")
        return null
    }
}