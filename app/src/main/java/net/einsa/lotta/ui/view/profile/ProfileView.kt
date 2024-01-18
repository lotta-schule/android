package net.einsa.lotta.ui.view.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.ui.component.UserAvatar
import net.einsa.lotta.util.UserUtil.Companion.getVisibleName

@Composable
fun ProfileView() {
    val modelData = LocalModelData.current

    Column {
        Text("Angemeldet als:")
        Column {
            modelData.userSessions.forEach { session ->
                Row {
                    UserAvatar(session.user)

                    Column {
                        Text(getVisibleName(session.user))
                        Text(session.tenant.title)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { modelData.removeCurrentSession() }) {
            Text("Abmelden")
        }
    }
}