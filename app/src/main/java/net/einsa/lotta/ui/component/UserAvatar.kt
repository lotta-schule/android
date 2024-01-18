package net.einsa.lotta.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.einsa.lotta.model.User
import net.einsa.lotta.model.getUrl

@Composable
fun UserAvatar(user: User, modifier: Modifier = Modifier) {
    user.avatarImageFileId?.getUrl(user.tenant)?.let { url ->
        Avatar(
            url = url,
            modifier = modifier,
            contentDescription = "Profilbild von ${user.name}"
        )
    }
}