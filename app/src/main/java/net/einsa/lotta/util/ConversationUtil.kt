package net.einsa.lotta.util

import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.model.Tenant

sealed class ConversationUtil {
    companion object {
        fun getImage(
            conversation: GetConversationsQuery.Conversation,
            excludingUserId: String,
            tenant: Tenant
        ): String? {
            return conversation.users?.find {
                it.id != excludingUserId
            }?.avatarImageFile?.formats?.find { true }?.url
        }

        fun getTitle(
            conversation: GetConversationsQuery.Conversation,
            excludingUserId: String
        ): String {
            return conversation.users?.find { it.id != excludingUserId }?.let { user ->
                UserUtil.getVisibleName(user)
            } ?: conversation.groups?.firstOrNull()?.name ?: "?"
        }
    }
}