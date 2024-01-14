package net.einsa.lotta.util

import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.model.Tenant
import net.einsa.lotta.model.getUrl

sealed class ConversationUtil {
    companion object {
        fun getImage(
            conversation: GetConversationsQuery.Conversation,
            excludingUserId: String,
            tenant: Tenant
        ): String? {
            return conversation.users?.find { it.id != excludingUserId }?.avatarImageFile?.id?.let { im ->
                im.getUrl(tenant)
            }
        }

        fun getTitle(
            conversation: GetConversationsQuery.Conversation,
            excludingUserId: String
        ): String {
            return conversation.users?.find { it.id != excludingUserId }?.let { user ->
                user.name ?: user.nickname // TODO: Make a util to show name correctly
            } ?: conversation.groups?.firstOrNull()?.name ?: "?"
        }
    }
}