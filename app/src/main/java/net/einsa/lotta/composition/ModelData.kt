package net.einsa.lotta.composition

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.sentry.Sentry
import net.einsa.lotta.api.baseCacheDir
import net.einsa.lotta.model.ID
import net.einsa.lotta.model.Theme
import net.einsa.lotta.util.UserDefaults

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

    val theme: Theme
        get() = currentSession?.tenant?.customTheme ?: Theme()

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

        UserDefaults.instance.getTenantId()?.let { persistedTenantId ->
            if (userSessions.map { it.tenant.id }.contains(persistedTenantId)) {
                currentSessionTenantId.value = persistedTenantId
            } else {
                UserDefaults.instance.removeTenantId()
            }

            // TODO: Setup Remote notifications
            initialized = true
        }
    }

    fun setSession(tenantId: ID): Boolean {
        if (currentSessionTenantId.value == tenantId) {
            return true
        }
        val session = userSessions.find { it.tenant.id == tenantId } ?: return false

        UserDefaults.instance.setTenantId(tenantId)
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

    fun remove(session: UserSession) {
        session.runCatching { removeFromDisk() }
        session.removeFromKeychain()

        userSessions.removeIf { it.tenant.id == session.tenant.id }

        session.api.resetCache()
        // TODO: session.deleteDevices()
    }

    fun removeCurrentSession() {
        currentSession?.let {
            remove(it)
        }

        if (currentSession == null) {
            UserDefaults.instance.removeTenantId()
        } else {
            this.currentSessionTenantId.value = currentSession!!.tenant.id
            UserDefaults.instance.setTenantId(currentSession!!.tenant.id)
        }
    }
}

val LocalModelData = compositionLocalOf {
    ModelData.instance
}