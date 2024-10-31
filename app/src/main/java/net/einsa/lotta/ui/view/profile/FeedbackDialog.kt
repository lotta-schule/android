package net.einsa.lotta.ui.view.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.sentry.Sentry
import io.sentry.UserFeedback
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.model.Theme
import net.einsa.lotta.ui.component.LottaButton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: (() -> Unit),
) {
    val focusManager = LocalFocusManager.current
    val modelData = LocalModelData.current

    val theme = remember { Theme() }

    var comments by remember { mutableStateOf("") }
    var isThankYouOpen by remember { mutableStateOf(false) }

    fun onSubmit() {
        val sentryId = Sentry.captureMessage("User feedback")
        val userFeedback = UserFeedback(sentryId).apply {
            this.comments = comments
            this.email = modelData.userSessions.firstOrNull()?.user?.email
            this.name = modelData.userSessions.firstOrNull()?.user?.name
            this.unknown = mapOf("tenant" to modelData.userSessions.firstOrNull()?.tenant?.id)
        }
        Sentry.captureUserFeedback(userFeedback)

        focusManager.clearFocus()
        comments = ""
        isThankYouOpen = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = Color(theme.boxBackgroundColor.toArgb()),
                    titleContentColor = Color(theme.textColor.toArgb()),
                ),
                title = {
                    Text("Feedback")
                },
                actions = {
                    IconButton(onClick = { onDismiss.invoke() }) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Schließen")
                    }
                }
            )
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxHeight(0.9F)) {
            Text(
                text = "Wir freuen uns sehr über jegliches Feedback. Änderungswünsche, Fehlermeldungen, Anregungen, Idden, etc. sind willkommen.",
                color = Color.Black,
                modifier = Modifier.padding(theme.spacing)
            )

            Spacer(modifier = Modifier.requiredHeight(20.dp))

            TextField(
                value = comments,
                onValueChange = { comments = it },
                label = { Text("Feedback") },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(theme.spacing)
                    .fillMaxWidth()
                    .requiredHeight(200.dp)
            )

            Spacer(modifier = Modifier.requiredHeight(20.dp))

            LottaButton(
                disabled = comments.isEmpty(),
                modifier = Modifier
                    .padding(theme.spacing)
                    .align(Alignment.End),
                text = "senden",
                onClick = {
                    onSubmit()
                }
            )
        }

        if (isThankYouOpen) {
            AlertDialog(
                onDismissRequest = {
                    isThankYouOpen = false
                    onDismiss()
                },
                title = { Text("Vielen Dank!") },
                text = { Text("Vielen Dank dafür, dass du dir diese Minuten Zeit genommen hast!") },
                confirmButton = {
                    LottaButton(
                        text = "Schließen",
                        onClick = {
                            isThankYouOpen = false
                            onDismiss()
                        }
                    )
                }
            )
        }

    }
}


@Preview
@Composable
fun Preview_FeedbackDialog() {
    FeedbackDialog(onDismiss = {})
}

