package net.einsa.lotta.ui.view.messaging

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.watch
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.ID

class ConversationViewModel : ViewModel() {
    private val _conversation = mutableStateOf<GetConversationQuery.Conversation?>(null)
    val conversation: GetConversationQuery.Conversation?
        get() = _conversation.value

    private val _errors = mutableStateListOf<Error>()
    val errors
        get() = _errors

    suspend fun watchConversation(
        id: ID,
        userSession: UserSession
    ) {
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
}