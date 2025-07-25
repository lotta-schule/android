package net.einsa.lotta.composition

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.sentry.Sentry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.einsa.lotta.App
import net.einsa.lotta.MainActivity
import net.einsa.lotta.api.baseCacheDir
import net.einsa.lotta.model.ID
import net.einsa.lotta.service.PushNotificationService
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

    val currentSession: UserSession?
        get() = userSessions.find { it.tenant.id == currentSessionTenantId.value }

    suspend fun initializeSessions(activity: MainActivity? = null) {
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
        }

        if (userSessions.isNotEmpty() && activity != null) {
            PushNotificationService.instance.startReceivingNotifications(activity = activity)
        }

        Sentry.configureScope { scope ->
            scope.user = userSessions.firstOrNull()?.user?.toSentryUser()
            scope.setContexts(
                "tenant", mapOf(
                    "id" to currentSessionTenantId.value
                )
            )
            scope.setContexts(
                "sessionCount", mapOf(
                    "id" to userSessions.count()
                )
            )
        }
        initialized = true
    }

    fun setSession(tenantId: ID): Boolean {
        if (currentSessionTenantId.value == tenantId) {
            return true
        }
        val session = userSessions.find { it.tenant.id == tenantId } ?: return false

        UserDefaults.instance.setTenantId(tenantId)
        currentSessionTenantId.value = tenantId
        Sentry.configureScope { scope ->
            scope.user = session.user.toSentryUser()
            scope.setContexts(
                "tenant", mapOf(
                    "id" to session.tenant.id,
                    "slug" to session.tenant.slug
                )
            )
            scope.setContexts(
                "sessionCount", mapOf(
                    "id" to userSessions.count()
                )
            )
        }
        return true
    }

    fun add(session: UserSession) {
        userSessions.removeIf {
            it.tenant.id == session.tenant.id
        }
        userSessions.add(session)
        setSession(tenantId = session.tenant.id)

        try {
            PushNotificationService.instance.startReceivingNotifications(App.mainActivity)
        } catch (e: Exception) {
            println("Failed to start receiving notifications")
            println(e)
            Sentry.captureException(e)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun remove(session: UserSession) {
        GlobalScope.launch {
            session.deleteDevice()

            session.runCatching {
                removeFromDisk()
                removeFromKeychain()
            }
            session.api.resetCache()
        }
    }

    fun removeCurrentSession() {
        val currentSession = userSessions.find {
            it.tenant.id == currentSessionTenantId.value
        } ?: userSessions.firstOrNull()

        currentSession?.let {
            remove(it)
        }

        val nextSession = userSessions.firstOrNull()

        if (nextSession == null) {
            UserDefaults.instance.removeTenantId()
        } else {
            this.currentSessionTenantId.value = nextSession.tenant.id
            UserDefaults.instance.setTenantId(nextSession.tenant.id)
        }
    }
}

val LocalModelData = compositionLocalOf {
    ModelData.instance
}