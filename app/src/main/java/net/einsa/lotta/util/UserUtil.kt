package net.einsa.lotta.util

import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.model.User

class UserUtil {
    companion object {
        fun getVisibleName(user: User): String {
            if (user.name.isNullOrBlank()) {
                return user.nickname ?: "?"
            }

            return if (user.nickname.isNullOrBlank()) {
                user.name!!
            } else {
                "${user.name} (${user.nickname})"
            }
        }

        fun getVisibleName(user: GetConversationQuery.User1): String {
            if (user.name.isNullOrBlank()) {
                return user.nickname ?: "?"
            }

            return if (user.nickname.isNullOrBlank()) {
                user.name
            } else {
                "${user.name} (${user.nickname})"
            }
        }

        fun getVisibleName(user: GetConversationQuery.User): String {
            if (user.name.isNullOrBlank()) {
                return user.nickname ?: "?"
            }

            return if (user.nickname.isNullOrBlank()) {
                user.name
            } else {
                "${user.name} (${user.nickname})"
            }
        }
    }
}