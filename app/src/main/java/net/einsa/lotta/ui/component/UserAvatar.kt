package net.einsa.lotta.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.einsa.lotta.GetConversationQuery
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
