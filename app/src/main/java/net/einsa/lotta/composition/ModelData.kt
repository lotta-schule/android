package net.einsa.lotta.composition

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import net.einsa.lotta.UserSession
import net.einsa.lotta.model.ID

class ModelData {
    companion object {
        var instance: ModelData = ModelData()
    }

    var userSessions = mutableStateListOf<UserSession>()
        private set

    var currentSessionTenantId = mutableStateOf<ID?>(null)
        private set

    var initialized = false
        private set

    val currentSession: UserSession?
        get() {
            userSessions.find { it.tenant.id == currentSessionTenantId.value }
            return userSessions.find {
                it.tenant.id == currentSessionTenantId.value
            } ?: userSessions.firstOrNull()
        }

    fun initializeSessions() {
        // TODO: Read sessions from disk
        // TODO: Setup Remote notifications
        initialized = true
    }

    fun setSession(tenantId: ID): Boolean {
        if (currentSessionTenantId.value == tenantId) {
            return true
        }
        val session = userSessions.find { it.tenant.id == tenantId } ?: return false

        // TODO: persist current session. Do UserDefaults exist?
        currentSessionTenantId.value = tenantId
        // TODO: Configure Sentry
        return true
    }

    fun add(session: UserSession) {
        userSessions.removeIf {
            it.tenant.id == session.tenant.id
        }
        userSessions.add(session)
        setSession(tenantId = session.tenant.id)
        // TODO: Start getting notifications
    }
}

val LocalModelData = compositionLocalOf {
    ModelData.instance
}