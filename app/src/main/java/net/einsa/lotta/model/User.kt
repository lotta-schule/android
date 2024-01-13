package net.einsa.lotta.model

import net.einsa.lotta.GetCurrentUserQuery

data class User(
    var tenant: Tenant,
    var id: ID,
    var name: String? = null,
    var nickname: String? = null,
    var email: String? = null,
    // TODO: var groups: List<Group>? = null,
    var avatarImageFileId: String? = null,
) {
    companion object {
        fun from(userData: GetCurrentUserQuery.CurrentUser, tenant: Tenant): User {
            return User(
                tenant = tenant,
                id = userData.id!!,
                name = userData.name,
                nickname = userData.nickname,
                email = userData.email,
                avatarImageFileId = userData.avatarImageFile?.id,
            )
        }
    }
}