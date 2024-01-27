package net.einsa.lotta.model

import kotlinx.serialization.Serializable
import net.einsa.lotta.GetCurrentUserQuery

@Serializable
class Group(var id: ID, var name: String) {
    companion object {
        fun from(userData: GetCurrentUserQuery.Group): Group {
            return Group(
                id = userData.id!!,
                name = userData.name!!,
            )
        }
    }
}