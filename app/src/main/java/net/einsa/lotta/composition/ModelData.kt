package net.einsa.lotta.composition

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.sentry.Sentry
import net.einsa.lotta.api.baseCacheDir
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
        get() = userSessions.find {
            it.tenant.id == currentSessionTenantId.value
        } ?: userSessions.firstOrNull()

    suspend fun initializeSessions() {
        if (!baseCacheDir.exists()) {
            try {
                baseCacheDir.mkdirs()
            } catch (e: Exception) {
                Sentry.captureException(e)
                println("Failed to create cache dir")
                println(e)
            }
        }
        userSessions.addAll(UserSession.readFromDisk())
        // TODO: Setup Remote notifications
        initialized = true
    }

    private fun setSession(tenantId: ID): Boolean {
        if (currentSessionTenantId.value == tenantId) {
            return true
        }
        val session = userSessions.find { it.tenant.id == tenantId } ?: return false

        // TODO: persist current session. Do UserDefaults exist?
        currentSessionTenantId.value = tenantId
        Sentry.configureScope { scope ->
            scope.setContexts(
                "tenant", mapOf(
                    "id" to session.tenant.id,
                    "slug" to session.tenant.slug
                )
            )
            scope.user = session.user.toSentryUser()
        }
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