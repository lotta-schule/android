package net.einsa.lotta.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.model.ID

@Composable
fun MessageInputView(
    userId: ID?,
    groupId: ID?,
    modifier: Modifier = Modifier,
    vm: MessageInputViewModel = viewModel()
) {
    val session = LocalUserSession.current
    val focusManager = LocalFocusManager.current

    var content by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val theme = session.tenant.customTheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                Color(theme.boxBackgroundColor.toArgb())
            )
            .padding(theme.spacing)
            .fillMaxWidth()
    ) {
        TextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("Nachricht") },
            singleLine = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier
                .weight(1f)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key.keyCode == Key.Enter.keyCode && !keyEvent.isShiftPressed) {
                        scope.launch {
                            focusManager.clearFocus()
                            vm.sendMessage(
                                content,
                                session = session,
                                userId = userId,
                                groupId = groupId
                            )
                            content = ""
                        }

                        return@onKeyEvent true
                    } else {
                        return@onKeyEvent false
                    }
                }
        )
        IconButton(
            onClick = {
                scope.launch {
                    focusManager.clearFocus()
                    vm.sendMessage(
                        content,
                        session = session,
                        userId = userId,
                        groupId = groupId
                    )
                    content = ""
                }
            },
            enabled = content.isNotBlank(),
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Nachricht senden"
            )
        }
    }
}