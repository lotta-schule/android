package net.einsa.lotta.model

import io.sentry.SentryOptions
import kotlinx.serialization.Serializable
import net.einsa.lotta.GetCurrentUserQuery
import io.sentry.protocol.User as SentryUser

@Serializable
data class User(
    var tenant: Tenant,
    var id: ID,
    var name: String? = null,
    var nickname: String? = null,
    var email: String? = null,
    var groups: List<Group> = emptyList(),
    var avatarImageFileId: String? = null,
) {
    companion object {
        fun from(userData: GetCurrentUserQuery.CurrentUser, tenant: Tenant): User {
            return User(
                tenant = tenant,
                id = userData.id!!,
                name = userData.name,
                nickname = userData.nickname,
                groups = userData.groups?.mapNotNull { it?.let { Group.from(it) } }
                    ?: emptyList(),
                email = userData.email,
                avatarImageFileId = userData.avatarImageFile?.id,
            )
        }
    }

    fun toSentryUser(): SentryUser {
        return SentryUser.fromMap(
            mapOf(
                "id" to id,
                "name" to name,
                "username" to nickname,
                "email" to email,
            ),
            SentryOptions()
        )
    }
}