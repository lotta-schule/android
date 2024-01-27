package net.einsa.lotta.ui.view

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.watch
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.ReceiveMessageSubscription
import net.einsa.lotta.SearchUsersQuery
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.Group
import net.einsa.lotta.ui.view.messaging.NewMessageDestination
import net.einsa.lotta.util.UserUtil

class MainViewModel() : ViewModel() {
    private val _newMessageCount = mutableIntStateOf(0)
    val newMessageCount
        get() = _newMessageCount.value

    suspend fun subscribeToMessages(session: UserSession) {
        try {
            session.api.apollo.subscription(ReceiveMessageSubscription()).toFlow().collect { data ->

                if (data.hasErrors()) {
                    Log.e("MainViewModel", "Error in subscription: ${data.errors}")
                }

                data.data?.message?.let { message ->
                    Log.d("MainViewModel", "Received message: $message")


                    try {
                        val conversations =
                            session.api.apollo.apolloStore.readOperation(GetConversationsQuery()).conversations
                        val isNewConversation =
                            conversations?.find { it?.id == message.conversation?.id } == null

                        val newConversations = if (isNewConversation) {
                            mutableListOf(
                                GetConversationsQuery.Conversation(
                                    id = message.conversation?.id,
                                    groups = message.conversation?.groups?.map { group ->
                                        GetConversationsQuery.Group(
                                            id = group.id,
                                            name = group.name,
                                        )
                                    },
                                    users = message.conversation?.users?.map { user ->
                                        GetConversationsQuery.User(
                                            id = user.id,
                                            name = null,
                                            avatarImageFile = null,
                                            nickname = null,
                                        )
                                    },
                                    unreadMessages = 0,
                                    updatedAt = message.insertedAt,
                                    messages = listOf(
                                        GetConversationsQuery.Message(
                                            id = message.id,
                                        )
                                    )
                                )
                            ).apply { addAll(conversations?.filterNotNull() ?: emptyList()) }
                        } else {
                            conversations!!.map {
                                if (it?.id == message.conversation?.id) {
                                    it!!.copy(
                                        messages = it.messages?.plus(
                                            GetConversationsQuery.Message(
                                                id = message.conversation?.id,
                                            )
                                        ),
                                        unreadMessages = message.conversation?.unreadMessages
                                            ?: ((it.unreadMessages
                                                ?: 0) + 1),
                                        updatedAt = message.updatedAt,
                                    )
                                } else {
                                    it
                                }
                            }
                        }

                        session.api.apollo.apolloStore.writeOperation(
                            GetConversationsQuery(),
                            GetConversationsQuery.Data(newConversations)
                        )

                    } catch (e: Exception) {
                        Log.e("SendMessage", "Failed to update conversations", e)
                    }

                    try {
                        val conversation =
                            session.api.apollo.apolloStore.readOperation(
                                GetConversationQuery(
                                    message.conversation?.id!!
                                )
                            ).conversation

                        session.api.apollo.apolloStore.writeOperation(
                            GetConversationQuery(message.conversation.id),
                            GetConversationQuery.Data(
                                GetConversationQuery.Conversation(
                                    id = conversation?.id,
                                    groups = conversation?.groups,
                                    users = conversation?.users,
                                    messages = mutableListOf(
                                        GetConversationQuery.Message(
                                            id = message.id,
                                            content = message.content,
                                            files = message.files?.map { file ->
                                                GetConversationQuery.File(
                                                    id = file?.id,
                                                    filename = file?.filename,
                                                    filesize = file?.filesize,
                                                    fileType = file?.fileType,
                                                )
                                            },
                                            user = GetConversationQuery.User1(
                                                id = session.user.id,
                                                name = session.user.name,
                                                nickname = session.user.nickname,
                                                avatarImageFile = GetConversationQuery.AvatarImageFile1(
                                                    id = session.user.avatarImageFileId
                                                ),
                                            ),
                                            updatedAt = message.updatedAt,
                                            insertedAt = message.insertedAt,
                                        )
                                    ).let {
                                        it.addAll(
                                            conversation?.messages?.filter { it.id != message.id }
                                                ?: emptyList()
                                        )

                                        it.toList()
                                    },
                                    updatedAt = message.insertedAt,
                                    unreadMessages = message.conversation.unreadMessages
                                        ?: ((conversation?.unreadMessages
                                            ?: 0) + 1),
                                )
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("SendMessage", "Failed to update conversation", e)
                    }

                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error in subscription", e)
        }
    }

    suspend fun watchNewMessageCount(
        userSession: UserSession
    ) {
        userSession.api.apollo.query(GetConversationsQuery()).watch().collect { response ->
            response.data?.conversations?.mapNotNull { it?.unreadMessages }
                ?.reduce { acc, next -> acc + next }
                ?.let { _newMessageCount.intValue = it }
        }
    }

    suspend fun onCreateNewMessage(
        destination: NewMessageDestination,
        user: SearchUsersQuery.User?,
        group: Group?,
        session: UserSession
    ): String {
        when (destination) {
            NewMessageDestination.GROUP -> {
                runCatching {
                    session.api.apollo.apolloStore
                        .readOperation(GetConversationsQuery())
                        .conversations?.find { it?.groups?.firstOrNull()?.id == group?.id }
                        ?.let { conversation ->
                            return@onCreateNewMessage MainScreen.CONVERSATION.route.replace(
                                "{conversationId}",
                                conversation.id!!,
                            ).replace(
                                "{title}",
                                conversation.groups?.firstOrNull()?.name ?: ""
                            )
                        }
                }
                return MainScreen.NEW_CONVERSATION.route
                    .replace("{title}", group!!.name)
                    .replace("{groupId}", group.id)
                    .replace("&userId={userId}", "")
            }

            NewMessageDestination.USER -> {
                runCatching {
                    session.api.apollo.apolloStore.readOperation(GetConversationsQuery())
                        .conversations?.find { conversation ->
                            conversation?.users?.mapNotNull { it.id }?.contains(user!!.id!!)
                                ?: false
                        }?.let { conversation ->
                            return@onCreateNewMessage MainScreen.CONVERSATION.route.replace(
                                "{conversationId}",
                                conversation.id!!,
                            ).replace(
                                "{title}",
                                UserUtil.getVisibleName(user!!)
                            )
                        }
                }
                return MainScreen.NEW_CONVERSATION.route
                    .replace(
                        "{title}", UserUtil.getVisibleName(user!!)
                    )
                    .replace("{userId}", user.id!!)
                    .replace("&groupId={groupId}", "")
            }
        }
    }
}