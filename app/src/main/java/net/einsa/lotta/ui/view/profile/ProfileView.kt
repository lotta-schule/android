package net.einsa.lotta.ui.view.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.composition.LocalTheme
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.Theme
import net.einsa.lotta.ui.component.LottaButton
import net.einsa.lotta.ui.component.UserAvatar
import net.einsa.lotta.ui.view.login.CreateLoginSessionView
import net.einsa.lotta.util.UserUtil.Companion.getVisibleName

@Composable
fun ProfileView(onSwitchProfile: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    val modelData = LocalModelData.current
    val currentSession = LocalUserSession.current
    val theme = LocalTheme.current

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
                onDismiss = { showDialog = false },
                modifier = Modifier.fillMaxHeight(.9f)
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .background(Color(theme.boxBackgroundColor.toArgb()))
                .padding(theme.spacing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Angemeldet als:", modifier = Modifier.fillMaxWidth())
            modelData.userSessions.forEach { session ->
                UserSessionListItem(
                    session = session,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    isSelected = session == currentSession,
                    onSelect = {
                        if (session != currentSession) {
                            modelData.setSession(session.tenant.id)
                            onSwitchProfile()
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
            text = "Account hinzufügen",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(theme.spacing.times(2)))

        LottaButton(
            onClick = { modelData.removeCurrentSession() },
            text = "Abmelden",
            modifier = Modifier.fillMaxWidth()
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
    val badgeCount = remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = session) {
        val response = session.api.apollo.query(GetConversationsQuery()).execute()
        badgeCount.intValue = response.data?.conversations?.sumOf { it?.unreadMessages ?: 0 } ?: 0
    }

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
        if (badgeCount.intValue > 0) {
            BadgedBox(
                modifier = Modifier.padding(end = defaultTheme.spacing),
                badge = {
                    Badge(
                        containerColor = Color(theme.primaryColor.toArgb()),
                        contentColor = Color(theme.primaryContrastTextColor.toArgb()),
                    ) {
                        Text(
                            badgeCount.intValue.toString(),
                        )
                    }
                }) {
                UserAvatar(
                    session.user,
                    size = 48,
                )
            }
        } else {
            UserAvatar(
                session.user,
                size = 48,
            )
        }

        Column(modifier = Modifier.padding(start = defaultTheme.spacing)) {
            Text(
                session.tenant.title, style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Text(getVisibleName(session.user), style = TextStyle(fontSize = 12.sp))
        }
    }
}
