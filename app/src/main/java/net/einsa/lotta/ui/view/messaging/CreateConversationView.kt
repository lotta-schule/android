package net.einsa.lotta.ui.view.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.einsa.lotta.SearchUsersQuery
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.model.Group
import net.einsa.lotta.ui.component.LottaButton
import net.einsa.lotta.ui.component.SearchUserList

enum class NewMessageDestination {
    USER,
    GROUP,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConversationView(
    onDismiss: () -> Unit,
    onSelect: suspend (NewMessageDestination, user: SearchUsersQuery.User?, group: Group?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedNewMessageType by remember { mutableStateOf<NewMessageDestination?>(null) }
    val scope = rememberCoroutineScope()
    val session = LocalUserSession.current
    val theme = session.tenant.customTheme
    val currentUser = session.user

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                {
                    Text(
                        when (selectedNewMessageType) {
                            null ->
                                "Neue Nachricht"

                            NewMessageDestination.USER ->
                                "Nutzer anschreiben"

                            NewMessageDestination.GROUP ->
                                "Gruppe anschreiben"
                        }
                    )
                },
                navigationIcon = {
                    if (selectedNewMessageType != null) {
                        IconButton(onClick = { selectedNewMessageType = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zurück"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onDismiss.invoke() }) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Schließen")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (selectedNewMessageType) {
            null -> {
                SelectMessageType(
                    onSelect = { selectedNewMessageType = it },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            NewMessageDestination.USER -> {
                SearchUserList(onSelect = { user ->
                    scope.launch {
                        onSelect(NewMessageDestination.USER, user, null)
                    }
                }, modifier = Modifier.padding(innerPadding))
            }

            NewMessageDestination.GROUP -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
                ) {
                    itemsIndexed(currentUser.groups, key = { _, user -> user.id }) { index, group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        onSelect(NewMessageDestination.GROUP, null, group)
                                    }
                                },
                        ) {
                            Text(
                                group.name,
                                modifier = Modifier
                                    .padding(theme.spacing)
                                    .fillMaxSize()
                            )
                        }
                        if (index < currentUser.groups.size - 1)
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = theme.spacing),
                                color = Color(theme.dividerColor.toArgb()),
                                thickness = 1.dp
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectMessageType(onSelect: (NewMessageDestination) -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalUserSession.current.tenant.customTheme

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LottaButton(
            onClick = { onSelect(NewMessageDestination.USER) },
            text = "Nutzer anschreiben",
            modifier = Modifier
                .fillMaxWidth()
                .padding(theme.spacing)
        )
        LottaButton(
            onClick = { onSelect(NewMessageDestination.GROUP) },
            text = "Gruppe anschreiben",
            modifier = Modifier
                .fillMaxWidth()
                .padding(theme.spacing)
        )
    }
}