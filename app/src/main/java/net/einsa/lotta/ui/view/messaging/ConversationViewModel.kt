package net.einsa.lotta.ui.view.messaging

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.doNotStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.ID

class ConversationViewModel : ViewModel() {
    companion object {
        const val PAGE_SIZE = 30
    }

    private val _conversation = mutableStateOf<GetConversationQuery.Conversation?>(null)
    val conversation: GetConversationQuery.Conversation?
        get() = _conversation.value

    private val _errors = mutableStateListOf<Error>()
    val errors
        get() = _errors

    // Older pages, loaded on demand. The newest page is kept up to date by the watched query.
    private val olderMessages = mutableStateListOf<GetConversationQuery.Message>()

    var isLoadingOlder by mutableStateOf(false)
        private set

    private var exhausted by mutableStateOf(false)

    private var currentConversationId: ID? = null

    val allMessages: List<GetConversationQuery.Message>
        get() {
            val live = conversation?.messages ?: emptyList()
            val liveIds = live.map { it.id }.toSet()
            return olderMessages.filter { it.id !in liveIds } + live
        }

    val hasMoreOlder: Boolean
        get() = !exhausted && (conversation?.messages?.size ?: 0) >= PAGE_SIZE

    suspend fun watchConversation(
        id: ID,
        userSession: UserSession
    ) {
        if (currentConversationId != id) {
            currentConversationId = id
            olderMessages.clear()
            exhausted = false
        }

        userSession.api.apollo.query(
            GetConversationQuery(
                id = id,
                markAsRead = Optional.present(true)
            )
        ).watch()
            .collect { response ->
                _errors.apply {
                    clear()
                    addAll(response.errors ?: emptyList())
                }
                response.data?.conversation?.let { conversation ->
                    _conversation.value = conversation
                }
            }
    }

    suspend fun loadOlderMessages(id: ID, userSession: UserSession) {
        if (isLoadingOlder || !hasMoreOlder) return

        val oldest = allMessages.minByOrNull { it.id.toLongOrNull() ?: Long.MAX_VALUE }
            ?: return

        isLoadingOlder = true
        try {
            val response = userSession.api.apollo.query(
                GetConversationQuery(
                    id = id,
                    markAsRead = Optional.present(false),
                    before = Optional.present(oldest.id),
                    first = Optional.present(PAGE_SIZE)
                )
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .doNotStore(true)
                .execute()

            val fetched = response.data?.conversation?.messages ?: emptyList()
            val knownIds = allMessages.map { it.id }.toSet()
            olderMessages.addAll(fetched.filter { it.id !in knownIds })
            exhausted = fetched.size < PAGE_SIZE
        } catch (e: Exception) {
            Log.e("Conversation", "Failed to load older messages", e)
        } finally {
            isLoadingOlder = false
        }
    }
}
