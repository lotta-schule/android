package net.einsa.lotta.ui.view.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apollographql.apollo3.cache.normalized.watch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.Theme
import net.einsa.lotta.ui.component.UserAvatar
import net.einsa.lotta.util.UserUtil.Companion.getVisibleName

@Composable
fun UserSessionListItem(
    session: UserSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = session.tenant.customTheme
    val scope = rememberCoroutineScope()
    val defaultTheme = Theme()
    val badgeCount = remember { mutableIntStateOf(0) }

    DisposableEffect(key1 = session) {
        val job = scope.launch {
            session.api.apollo
                .query(GetConversationsQuery())
                .watch().collectLatest {
                    badgeCount.intValue =
                        session.getCurrentUnreadMessages()
                }
        }
        onDispose {
            job.cancel()
        }
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


