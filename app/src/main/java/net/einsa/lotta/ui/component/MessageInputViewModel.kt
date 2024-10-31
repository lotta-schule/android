package net.einsa.lotta.ui.component

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.apolloStore
import net.einsa.lotta.App
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.SendMessageMutation
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.ID
import net.einsa.lotta.type.MessageInput
import net.einsa.lotta.type.SelectUserGroupInput
import net.einsa.lotta.type.SelectUserInput

class MessageInputViewModel : ViewModel() {
    suspend fun sendMessage(
        content: String,
        session: UserSession,
        userId: ID?,
        groupId: ID?
    ): SendMessageMutation.Message? {
        try {
            val response = session.api.apollo.mutation(
                SendMessageMutation(
                    MessageInput(
                        content = Optional.present(content),
                        recipientUser = userId?.let {
                            Optional.present(
                                SelectUserInput(it)
                            )
                        } ?: Optional.absent(),
                        recipientGroup = groupId?.let {
                            Optional.present(
                                SelectUserGroupInput(it)
                            )
                        } ?: Optional.absent(),
                    )
                )
            ).execute()

            if (response.hasErrors()) {
                throw Exception(response.errors?.first()?.message)
            }

            if (response.data?.message?.id == null) {
                throw Exception("No message returned")
            }

            val conversationId = response.data?.message?.conversation?.id
            val messageId = response.data?.message?.id

            if (conversationId != null && messageId != null) {
                try {
                    val conversations =
                        session.api.apollo.apolloStore.readOperation(GetConversationsQuery()).conversations
                    val isNewConversation = conversations?.find { it?.id == conversationId } == null

                    val newConversations = if (isNewConversation) {
                        mutableListOf(
                            GetConversationsQuery.Conversation(
                                id = conversationId,
                                groups = response.data?.message?.conversation?.groups?.map { group ->
                                    GetConversationsQuery.Group(
                                        id = group.id,
                                        name = group.name,
                                    )
                                },
                                users = response.data?.message?.conversation?.users?.map { user ->
                                    GetConversationsQuery.User(
                                        id = user.id,
                                        name = null,
                                        avatarImageFile = null,
                                        nickname = null,
                                    )
                                },
                                unreadMessages = 0,
                                updatedAt = response.data?.message?.insertedAt,
                                messages = listOf(
                                    GetConversationsQuery.Message(
                                        id = messageId,
                                    )
                                )
                            )
                        ).apply { addAll(conversations?.filterNotNull() ?: emptyList()) }
                    } else {
                        conversations!!.map {
                            if (it?.id == conversationId) {
                                it.copy(
                                    messages = it.messages?.plus(
                                        GetConversationsQuery.Message(
                                            id = messageId,
                                        )
                                    ),
                                    unreadMessages = 0,
                                    updatedAt = response.data?.message?.insertedAt,
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
                                conversationId
                            )
                        ).conversation

                    session.api.apollo.apolloStore.writeOperation(
                        GetConversationQuery(conversationId),
                        GetConversationQuery.Data(
                            GetConversationQuery.Conversation(
                                id = conversationId,
                                groups = conversation?.groups,
                                users = conversation?.users,
                                messages = mutableListOf(
                                    GetConversationQuery.Message(
                                        id = messageId,
                                        content = response.data?.message?.content,
                                        files = emptyList(),
                                        user = GetConversationQuery.User1(
                                            id = session.user.id,
                                            name = session.user.name,
                                            nickname = session.user.nickname,
                                            avatarImageFile = session.user.avatarImageFileId?.let { fileId ->
                                                GetConversationQuery.AvatarImageFile1(fileId)
                                            },
                                        ),
                                        insertedAt = response.data?.message?.insertedAt!!,
                                        updatedAt = response.data?.message?.updatedAt!!,
                                    ),
                                ).let {
                                    it.addAll(
                                        conversation?.messages?.filter { it.id != response.data?.message?.id }
                                            ?: emptyList()
                                    )

                                    it.toList()
                                },
                                updatedAt = response.data?.message?.insertedAt,
                                unreadMessages = 0,
                            )
                        )
                    )
                } catch (e: Exception) {
                    Log.e("SendMessage", "Failed to update conversation", e)
                }

                Log.i("SendMessage", "Sent message ${response.data?.message?.id}")
            }

            return response.data?.message
        } catch (e: Exception) {
            Log.e("SendMessage", "Failed to send message", e)
            Toast.makeText(App.context, e.localizedMessage, Toast.LENGTH_LONG).show()
        }

        return null
    }
}