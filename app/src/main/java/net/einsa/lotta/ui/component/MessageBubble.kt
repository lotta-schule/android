package net.einsa.lotta.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.composition.LocalUserSession

@Composable
fun MessageBubble(
    message: GetConversationQuery.Message,
    fromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val session = LocalUserSession.current
    val theme = session.tenant.customTheme

    val shape = if (fromCurrentUser) {
        RoundedCornerShape(
            topStart = theme.borderRadius,
            topEnd = theme.borderRadius,
            bottomStart = theme.borderRadius
        )
    } else {
        RoundedCornerShape(
            topStart = theme.borderRadius,
            topEnd = theme.borderRadius,
            bottomEnd = theme.borderRadius
        )
    }

    Column(
        modifier = modifier
            .background(
                if (fromCurrentUser) {
                    Color(theme.primaryColor.toArgb()).copy(alpha = 0.3f)
                } else {
                    Color(
                        theme.primaryColor.toArgb()
                    ).copy(alpha = 0.08f)
                }
            )
            .border(
                width = 1.dp,
                color = Color(theme.primaryColor.toArgb()),
                shape = shape
            )
            .padding(theme.spacing)
            .clip(shape),
    ) {
        message.content?.let {
            Text(it)
        }
    }
}