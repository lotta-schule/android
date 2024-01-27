package net.einsa.lotta.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.SearchUsersQuery
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.model.User
import net.einsa.lotta.model.getUrl

@Composable
fun UserAvatar(user: User, modifier: Modifier = Modifier, size: Int = 44) {
    user.avatarImageFileId?.getUrl(user.tenant)
        ?.let { url ->
            Avatar(
                url = url,
                size = size,
                modifier = modifier,
                contentDescription = "Profilbild von ${user.name}"
            )
        }
}

@Composable
fun UserAvatar(user: GetConversationQuery.User1, modifier: Modifier = Modifier, size: Int = 44) {
    val session = LocalUserSession.current

    user.avatarImageFile?.id?.getUrl(session.tenant)
        ?.let { url ->
            Avatar(
                url = url,
                size = size,
                modifier = modifier,
                contentDescription = "Profilbild von ${user.name}"
            )
        }
}

@Composable
fun UserAvatar(user: SearchUsersQuery.User, modifier: Modifier = Modifier, size: Int = 44) {
    val session = LocalUserSession.current

    val url = user.avatarImageFile?.id?.getUrl(session.tenant)

    if (url == null) {
        Image(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = modifier
                .size(size.dp)
                .aspectRatio(1f)
                .clip(CircleShape),
        )
    } else {
        Avatar(
            url = url,
            size = size,
            modifier = modifier,
            contentDescription = "Profilbild von ${user.name}"
        )
    }
}
