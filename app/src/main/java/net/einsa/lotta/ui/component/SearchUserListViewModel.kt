package net.einsa.lotta.ui.component

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.einsa.lotta.SearchUsersQuery
import net.einsa.lotta.composition.UserSession

class SearchUserListViewModel : ViewModel() {
    private var _searchText = mutableStateOf("")
    val searchText
        get() = _searchText.value

    private var _searchResults = mutableStateListOf<SearchUsersQuery.User>()
    val searchResults
        get() = _searchResults

    private var searchJob: Job? = null

    fun updateSearchText(text: String, session: UserSession) {
        _searchText.value = text
        searchDebounced(session)
    }

    private fun searchDebounced(session: UserSession) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            search(session)
        }
    }

    private suspend fun search(session: UserSession) {
        try {
            session.api.apollo.query(SearchUsersQuery(searchText)).execute()
                .let { response ->
                    response.data?.users?.let { users ->
                        _searchResults.clear()
                        _searchResults.addAll(users.filterNotNull())
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}