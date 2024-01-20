package net.einsa.lotta.ui.component

import android.util.Log
import androidx.lifecycle.ViewModel
import com.apollographql.apollo3.api.Optional
import net.einsa.lotta.SendMessageMutation
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.ID
import net.einsa.lotta.type.MessageInput
import net.einsa.lotta.type.SelectUserGroupInput
import net.einsa.lotta.type.SelectUserInput

class MessageInputViewModel : ViewModel() {
    suspend fun sendMessage(content: String, session: UserSession, userId: ID?, groupId: ID?) {
        val response = session.api.apollo.mutation(
            SendMessageMutation(
                MessageInput(
                    content = Optional.present(content),
                    recipientUser = userId?.let {
                        Optional.present(
                            SelectUserInput(
                                Optional.present(
                                    it
                                )
                            )
                        )
                    } ?: Optional.absent(),
                    recipientGroup = groupId?.let {
                        Optional.present(
                            SelectUserGroupInput(
                                Optional.present(
                                    it
                                )
                            )
                        )
                    } ?: Optional.absent(),
                )
            )
        ).execute()

        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message)
        }

        if (response.data?.message?.id == null) {
            throw Exception("No message returned")
        }

        Log.i("SendMessage", "Sent message ${response.data?.message?.id}")
    }
}