package net.einsa.lotta.util

import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.SearchUsersQuery
import net.einsa.lotta.SendMessageMutation
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

        fun getVisibleName(user: GetConversationsQuery.User): String {
            if (user.name.isNullOrBlank()) {
                return user.nickname ?: "?"
            }

            return if (user.nickname.isNullOrBlank()) {
                user.name
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

        fun getVisibleName(user: SearchUsersQuery.User): String {
            if (user.name.isNullOrBlank()) {
                return user.nickname ?: "?"
            }

            return if (user.nickname.isNullOrBlank()) {
                user.name
            } else {
                "${user.name} (${user.nickname})"
            }
        }

        fun getVisibleName(user: SendMessageMutation.User): String {
            if (user.name.isNullOrBlank()) {
                return user.nickname ?: "?"
            }

            return if (user.nickname.isNullOrBlank()) {
                user.name
            } else {
                "${user.name} (${user.nickname})"
            }
        }

        fun getVisibleName(user: SendMessageMutation.User1): String {
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