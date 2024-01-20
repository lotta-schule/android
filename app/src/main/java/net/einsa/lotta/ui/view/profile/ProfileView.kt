package net.einsa.lotta.ui.view.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.Theme
import net.einsa.lotta.ui.component.LottaButton
import net.einsa.lotta.ui.component.UserAvatar
import net.einsa.lotta.ui.view.login.CreateLoginSessionView
import net.einsa.lotta.util.UserUtil.Companion.getVisibleName

@Composable
fun ProfileView() {
    val modelData = LocalModelData.current
    var showDialog by remember { mutableStateOf(false) }

    val theme = modelData.theme

    if (showDialog) {
        Dialog(
            properties = DialogProperties(
                dismissOnBackPress = true,
                usePlatformDefaultWidth = false
            ),
            onDismissRequest = { showDialog = false }
        ) {
            CreateLoginSessionView(
                onLogin = { userSession ->
                    showDialog = false
                    modelData.add(userSession)
                },
                onDismiss = { showDialog = false }
            )
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .fillMaxWidth(.75f)
        ) {
            Text("Angemeldet als:", modifier = Modifier.fillMaxWidth())
            modelData.userSessions.forEach { session ->
                UserSessionListItem(
                    session = session,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    isSelected = session.equals(modelData.currentSession),
                    onSelect = {
                        if (!session.equals(modelData.currentSession)) {
                            modelData.setSession(session.tenant.id)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(theme.spacing.times(2)))

        LottaButton(
            onClick = {
                showDialog = true
            },
            text = "Account hinzufügen"
        )

        Spacer(modifier = Modifier.height(theme.spacing.times(2)))

        LottaButton(
            onClick = { modelData.removeCurrentSession() },
            text = "Abmelden"
        )
    }
}

@Composable
fun UserSessionListItem(
    session: UserSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = session.tenant.customTheme
    val defaultTheme = Theme()
    Row(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(theme.dividerColor.toArgb()),
                        Color(theme.dividerColor.toArgb())
                    )
                ),
                shape = RoundedCornerShape(theme.borderRadius)
            )
            .background(
                if (isSelected) {
                    Color(theme.primaryColor.toArgb()).copy(alpha = .1f)
                } else {
                    Color(theme.boxBackgroundColor.toArgb())
                }
            )
            .padding(defaultTheme.spacing)
            .clickable { onSelect() },
    ) {
        UserAvatar(session.user, size = 48, modifier = Modifier.padding(end = defaultTheme.spacing))

        Column {
            Text(
                getVisibleName(session.user), style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Text(session.tenant.title, style = TextStyle(fontSize = 12.sp))
        }
    }
}
