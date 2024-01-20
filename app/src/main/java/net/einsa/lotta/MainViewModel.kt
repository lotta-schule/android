package net.einsa.lotta

import android.util.Log
import androidx.lifecycle.ViewModel
import com.apollographql.apollo3.cache.normalized.apolloStore
import net.einsa.lotta.composition.UserSession

class MainViewModel() : ViewModel() {
    suspend fun subscribeToMessages(session: UserSession) {
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
                                        name = null, // TODO: Add name to group
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
                        )
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
    }
}