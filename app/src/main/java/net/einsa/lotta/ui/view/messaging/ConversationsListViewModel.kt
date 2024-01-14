package net.einsa.lotta.ui.view.messaging

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.cache.normalized.watch
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.composition.UserSession

class ConversationsListViewModel : ViewModel() {
    private val _conversations = mutableStateListOf<GetConversationsQuery.Conversation>()
    val conversations: List<GetConversationsQuery.Conversation>
        get() = _conversations

    private val _errors = mutableStateListOf<Error>()
    val errors
        get() = _errors

    suspend fun watchConversations(
        userSession: UserSession
    ) {
        userSession.api.apollo.query(GetConversationsQuery()).watch().collect { response ->
            _errors.apply {
                clear()
                addAll(response.errors ?: emptyList())
            }
            _conversations.apply {
                clear()
                addAll(response.data?.conversations?.filterNotNull() ?: emptyList())
            }
        }
    }

}