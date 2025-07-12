package net.einsa.lotta.model

import io.sentry.SentryOptions
import kotlinx.serialization.Contextual
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
    var avatarImageFile: @Contextual @Serializable GetCurrentUserQuery.AvatarImageFile? = null
) {
    companion object {
        fun from(userData: GetCurrentUserQuery.CurrentUser, tenant: Tenant): User {
            return User(
                tenant = tenant,
                id = userData.id,
                name = userData.name,
                nickname = userData.nickname,
                groups = userData.groups.map { it.let { Group.from(it) } },
                email = userData.email,
                avatarImageFile = userData.avatarImageFile
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