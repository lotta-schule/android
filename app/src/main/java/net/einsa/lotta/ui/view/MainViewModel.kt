package net.einsa.lotta.ui.view

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import kotlinx.coroutines.flow.collectLatest
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.ReceiveMessageSubscription
import net.einsa.lotta.SearchUsersQuery
import net.einsa.lotta.composition.ModelData
import net.einsa.lotta.model.Group
import net.einsa.lotta.model.ID
import net.einsa.lotta.ui.view.messaging.NewMessageDestination
import net.einsa.lotta.util.UserUtil

class MainViewModel() : ViewModel() {
    private val modelData = ModelData.instance

    private val _newMessageCount = mutableIntStateOf(0)
    val newMessageCount
        get() = _newMessageCount.intValue
    private val _otherNewMessageCount = mutableIntStateOf(0)
    val otherNewMessageCount
        get() = _otherNewMessageCount.intValue

    suspend fun subscribeToMessages() {
        val session = modelData.currentSession!!
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
                            conversations?.find { it?.id == message.conversation.id } == null

                        val newConversations = if (isNewConversation) {
                            mutableListOf(
                                GetConversationsQuery.Conversation(
                                    id = message.conversation.id,
                                    groups = message.conversation.groups?.map { group ->
                                        GetConversationsQuery.Group(
                                            id = group.id,
                                            name = group.name,
                                        )
                                    },
                                    users = message.conversation.users?.map { user ->
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
                                if (it?.id == message.conversation.id) {
                                    it!!.copy(
                                        messages = it.messages?.plus(
                                            GetConversationsQuery.Message(
                                                id = message.conversation.id!!,
                                            )
                                        ),
                                        unreadMessages = message.conversation.unreadMessages
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
                                    message.conversation.id!!,
                                    markAsRead = Optional.present(true)
                                )
                            ).conversation

                        session.api.apollo.apolloStore.writeOperation(
                            GetConversationQuery(
                                message.conversation.id,
                                markAsRead = Optional.present(true)
                            ),
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
                                                    id = file.id,
                                                    filename = file.filename,
                                                    filesize = file.filesize,
                                                    fileType = file.fileType,
                                                    formats = file.formats.map { format ->
                                                        GetConversationQuery.Format1(
                                                            name = format.name,
                                                            url = format.url
                                                        )
                                                    },
                                                )
                                            },
                                            updatedAt = message.updatedAt,
                                            insertedAt = message.insertedAt,
                                            user = GetConversationQuery.User1(
                                                id = session.user.id,
                                                name = session.user.name,
                                                nickname = session.user.nickname,
                                                avatarImageFile = session.user.avatarImageFile?.let {
                                                    GetConversationQuery.AvatarImageFile1(
                                                        id = it.id,
                                                        formats = it.formats.map { format ->
                                                            GetConversationQuery.Format2(
                                                                name = format.name,
                                                                url = format.url
                                                            )
                                                        }
                                                    )
                                                },
                                            )
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
                                ),
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("SendMessage", "Failed to update conversation", e)
                    }

                }

                updateNewMessageCounts()
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error in subscription", e)
        }
    }

    suspend fun updateNewMessageCounts(ignoringConversationId: ID? = null) {
        val currentSession = modelData.currentSession!!
        val otherSessions = modelData.userSessions
            .filter {
                it.tenant.id != currentSession.tenant.id || it.user.id != currentSession.user.id
            }

        _newMessageCount.intValue =
            currentSession.getCurrentUnreadMessages(ignoringConversationId)
        _otherNewMessageCount.intValue = otherSessions.sumOf { it.getCurrentUnreadMessages() }
    }

    suspend fun watchNewMessageCount() {
        val userSession = modelData.currentSession!!
        userSession.api.apollo
            .query(GetConversationsQuery())
            .watch().collectLatest {
                updateNewMessageCounts()
            }
    }

    suspend fun onCreateNewMessage(
        destination: NewMessageDestination,
        user: SearchUsersQuery.User?,
        group: Group?
    ): String {
        val session = ModelData.instance.currentSession!!
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
                            conversation?.users?.map { it.id }?.contains(user!!.id)
                                ?: false
                        }?.let { conversation ->
                            return@onCreateNewMessage MainScreen.CONVERSATION.route.replace(
                                "{conversationId}",
                                conversation.id!!,
                            ).replace(
                                "{title}",
                                UserUtil.getVisibleName(user!!)
                            ).replace(
                                "{imageUrl}",
                                user.avatarImageFile?.formats?.find { true }?.url ?: ""
                            )
                        }
                }
                return MainScreen.NEW_CONVERSATION.route
                    .replace(
                        "{title}", UserUtil.getVisibleName(user!!)
                    )
                    .replace("{userId}", user.id)
                    .replace("&groupId={groupId}", "")
            }
        }
    }

    suspend fun getConversation(
        conversationId: ID
    ): GetConversationQuery.Conversation? {
        val session = modelData.currentSession!!
        return session.api.apollo.query(
            GetConversationQuery(
                conversationId,
                markAsRead = Optional.present(true)
            )
        )
            .fetchPolicy(FetchPolicy.CacheFirst)
            .execute().data?.conversation
    }
}