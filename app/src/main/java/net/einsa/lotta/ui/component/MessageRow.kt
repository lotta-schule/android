package net.einsa.lotta.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.util.UserUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun MessageRow(
    message: GetConversationQuery.Message,
    fromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val session = LocalUserSession.current
    val theme = session.tenant.customTheme

    val formattedDateLine = {
        val date = message.insertedAt

        if (date is String) {
            LocalDateTime.parse(date.replace(Regex("Z$"), ""))
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
        } else {
            ""
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(theme.spacing),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        if (fromCurrentUser) {
            Spacer(modifier = Modifier.width(48.dp))
        } else {
            UserAvatar(
                user = message.user!!,
                size = 48,
                modifier = Modifier.padding(theme.spacing)
            )
        }
        Column(horizontalAlignment = if (fromCurrentUser) Alignment.End else Alignment.Start) {
            val userName = message.user?.let { UserUtil.getVisibleName(message.user) } ?: "?"
            MessageBubble(
                message,
                fromCurrentUser = fromCurrentUser,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Text(
                userName + ", " + formattedDateLine(),
                style = TextStyle(fontSize = 10.sp, color = Color(theme.disabledColor.toArgb()))
            )
        }
        if (!fromCurrentUser) {
            Spacer(modifier = Modifier.width(48.dp))
        } else {
            UserAvatar(
                user = message.user!!,
                size = 48,
                modifier = Modifier
                    .padding(theme.spacing)
            )
        }
    }
}