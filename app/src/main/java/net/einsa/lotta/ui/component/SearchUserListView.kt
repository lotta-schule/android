package net.einsa.lotta.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.einsa.lotta.SearchUsersQuery
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.util.UserUtil

@Composable
fun SearchUserList(
    onSelect: (SearchUsersQuery.User) -> Unit,
    modifier: Modifier = Modifier,
    vm: SearchUserListViewModel = viewModel(),
) {
    val session = LocalUserSession.current
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        TextField(
            value = vm.searchText,
            onValueChange = { vm.updateSearchText(it, session) },
            singleLine = true,
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(session.tenant.customTheme.spacing)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(vm.searchResults, key = { _, user -> user.id!! }) { index, user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(user) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        user, modifier = Modifier.padding(session.tenant.customTheme.spacing)
                    )
                    Text(UserUtil.getVisibleName(user))
                }
                if (index < vm.searchResults.size - 1)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = session.tenant.customTheme.spacing),
                        color = Color(session.tenant.customTheme.dividerColor.toArgb()),
                        thickness = 1.dp
                    )
            }
        }
    }
}