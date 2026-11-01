package net.einsa.lotta.composition

import androidx.compose.runtime.compositionLocalOf
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.auth0.jwt.JWT
import io.sentry.Sentry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.einsa.lotta.AuthInfo
import net.einsa.lotta.DeleteDeviceMutation
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.GetConversationsQuery
import net.einsa.lotta.GetCurrentUserQuery
import net.einsa.lotta.GetTenantQuery
import net.einsa.lotta.LoginMutation
import net.einsa.lotta.RegisterDeviceMutation
import net.einsa.lotta.RenewException
import net.einsa.lotta.api.CoreApi
import net.einsa.lotta.api.LottaEnvironment
import net.einsa.lotta.api.baseCacheDir
import net.einsa.lotta.model.ID
import net.einsa.lotta.model.Tenant
import net.einsa.lotta.model.User
import net.einsa.lotta.service.DeviceIdentificationService
import net.einsa.lotta.type.RegisterDeviceInput
import net.einsa.lotta.util.SecretKeyStore

class UserSession(tenant: Tenant, authInfo: AuthInfo, user: User) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun createFromCredentials(
            tenantSlug: String,
            username: String,
            password: String
        ): UserSession {
            val genericApi = CoreApi(tenantSlug = tenantSlug)
            val tenantGraphqlResponse = genericApi.apollo.query(GetTenantQuery()).execute()
            val tenantData = tenantGraphqlResponse.dataAssertNoErrors

            val tenant = Tenant.from(tenantData.tenant!!)

            val authInfo = AuthInfo()
            val tenantApi = CoreApi(tenantSlug = tenant.slug, loginSession = authInfo)

            val tokenGraphqlResponse = tenantApi.apollo.mutation(
                LoginMutation(
                    username = username,
                    password = password
                )
            ).execute()
            val tokenData = tokenGraphqlResponse.dataAssertNoErrors
            val accessTokenString =
                tokenData.login?.accessToken ?: throw Exception("No access token in response")
            val accessToken = JWT.decode(accessTokenString)

            if (!accessToken.claims.containsKey("typ") || accessToken.getClaim("typ")
                    .asString() != "access"
            ) {
                throw Exception("Auth token is not a valid lotta jwt access token")
            }
            if (accessToken.subject == null) {
                throw Exception("Auth token is not a valid lotta jwt access token")
            }

            authInfo.accessToken = accessToken
            // Prefer the refresh token from the response body; RefreshTokenInterceptor may already
            // have picked one up from the Set-Cookie header. Setting it persists it right away,
            // so the session survives an app restart independently of the renewal below.
            tokenData.login?.refreshToken
                ?.let { runCatching { JWT.decode(it) }.getOrNull() }
                ?.let { authInfo.refreshToken = it }

            val user = User(tenant = tenant, id = accessToken.subject)
            val userSession = UserSession(tenant, authInfo, user)

            try {
                authInfo.renew()
            } catch (e: Exception) {
                e.printStackTrace()
                print("Could not renew auth token: ${e.message}")
            }

            userSession.refetchUserData()

            userSession.writeToDisk()

            return userSession
        }

        suspend fun readFromDisk(): List<UserSession> {
            val sessions = mutableListOf<UserSession>()

            try {
                val files = baseCacheDir.listFiles { file ->
                    file.isFile
                }?.mapNotNull { file ->
                    // The tenant slug may contain dashes, the user id does not
                    Regex("^usersession-(.+)-([^-]+)\\.json\$").matchEntire(file.name)
                        ?.let { match ->
                            val userId = match.groupValues[2]
                            file to userId
                        }
                } ?: emptyList()

                for ((file, userId) in files) {
                    var tenantId: ID? = null

                    // Only drop a session if it really is unusable: unreadable, no or expired
                    // refresh token, or the server rejected the token. Network or server errors
                    // must not log the user out.
                    fun dropSession(e: Exception) {
                        Sentry.captureException(e)
                        println("Dropping user session ${file.name}: ${e.message}")

                        tenantId?.let {
                            LottaEnvironment.secretStore.remove(
                                SecretKeyStore.refreshTokenKey(userId, it)
                            )
                        }
                        file.runCatching { delete() }
                    }

                    val userSession = try {
                        val userSessionData = file.readText(Charsets.UTF_8)
                        val persistedUserSession =
                            json.decodeFromString<PersistedUserSession>(userSessionData)
                        tenantId = persistedUserSession.tenant.id
                        val refreshToken = LottaEnvironment.secretStore.get(
                            SecretKeyStore.refreshTokenKey(
                                userId,
                                persistedUserSession.tenant.id
                            )
                        ) ?: throw Exception("No refresh token found")
                        val jwt = JWT.decode(refreshToken)

                        if (jwt.expiresAt.time < System.currentTimeMillis()) {
                            throw Exception("Refresh token expired")
                        }

                        val authInfo = AuthInfo()
                        authInfo.refreshToken = jwt
                        try {
                            authInfo.renew()
                        } catch (e: RenewException.ConnectionError) {
                            // Offline or server problem: keep the session, renew later on demand
                            println("Could not renew auth token: ${e.message}")
                        }

                        UserSession(
                            tenant = persistedUserSession.tenant,
                            authInfo = authInfo,
                            user = persistedUserSession.user
                        )
                    } catch (e: Exception) {
                        dropSession(e)
                        continue
                    }

                    sessions.add(userSession)

                    try {
                        userSession.refetchUserData()
                        userSession.refetchTenantData()
                        userSession.writeToDisk()
                    } catch (e: Exception) {
                        // Keep the persisted user and tenant data
                        Sentry.captureException(e)
                        println("Could not refresh user session data: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Sentry.captureException(e)
                print("Could not read user sessions from disk: ${e.message}")
            }

            return sessions
        }
    }

    var tenant: Tenant = tenant
        private set
    var user: User = user
        private set
    var api: CoreApi
        private set
    var deviceId: String? = null
        private set

    init {
        this.api = CoreApi(tenantSlug = tenant.slug, tenantId = tenant.id, loginSession = authInfo)
    }

    suspend fun getCurrentUnreadMessages(ignoringConversationId: ID? = null): Int {
        val response = api.apollo
            .query(GetConversationsQuery())
            .fetchPolicy(FetchPolicy.CacheFirst)
            .execute()
        return response.data?.conversations
            ?.filter { conversation ->
                conversation?.id != null && ignoringConversationId != conversation.id
            }
            ?.mapNotNull { it?.unreadMessages ?: 0 }
            ?.reduceOrNull(Int::plus) ?: 0
    }

    suspend fun refetchUserData() {
        val userGraphqlResponse =
            api.apollo.query(GetCurrentUserQuery()).fetchPolicy(FetchPolicy.NetworkFirst)
                .execute()
        val user = userGraphqlResponse.dataAssertNoErrors.currentUser!!
        this.user = User.from(user, tenant = tenant)
    }

    suspend fun refetchTenantData() {
        val tenantGraphqlResponse = api.apollo.query(GetTenantQuery()).execute()
        val tenant = tenantGraphqlResponse.dataAssertNoErrors.tenant!!
        this.tenant = Tenant.from(tenant)
    }

    suspend fun forceReloadConversations(clearingCache: Boolean = false) {
        if (clearingCache) {
            api.apollo.apolloStore.clearAll()
        }
        api.apollo.query(GetConversationsQuery()).fetchPolicy(FetchPolicy.NetworkOnly).execute()
    }

    suspend fun forceReloadConversation(conversationId: String) {
        val result = api.apollo.query(
            GetConversationQuery(
                id = conversationId,
                markAsRead = Optional.present(false)
            )
        )
            .fetchPolicy(FetchPolicy.NetworkOnly).execute()

        api.apollo.apolloStore.writeOperation(
            GetConversationQuery(id = conversationId, markAsRead = Optional.present(true)),
            result.dataAssertNoErrors
        )
    }

    fun writeToDisk() {
        val file = baseCacheDir.resolve("usersession-${tenant.slug}-${user.id}.json")
        try {
            val persistedUserSession = PersistedUserSession(tenant, user)
            val userSessionData = Json.encodeToString(persistedUserSession)
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText(userSessionData, Charsets.UTF_8)
        } catch (e: Exception) {
            Sentry.captureException(e)
            print("Could not write user session to disk: ${e.message}")
        }
    }

    fun removeFromDisk() {
        baseCacheDir.resolve("usersession-${tenant.slug}-${user.id}.json")
            .delete()
    }

    fun removeFromKeychain() {
        LottaEnvironment.secretStore.remove(
            SecretKeyStore.refreshTokenKey(
                user.id,
                tenant.id
            )
        )
    }

    suspend fun registerDevice(token: String) {
        val result = api.apollo.mutation(
            RegisterDeviceMutation(
                RegisterDeviceInput(
                    deviceType = Optional.present(DeviceIdentificationService.instance.getDeviceType()),
                    modelName = Optional.present(DeviceIdentificationService.instance.getModelName()),
                    operatingSystem = Optional.present("Android ${DeviceIdentificationService.instance.getOsVersion()}"),
                    platformId = "android/${DeviceIdentificationService.instance.getDeviceIdentifier() ?: "0"}",
                    pushToken = Optional.present("fcm/$token"),
                )
            )
        ).execute()

        result.dataAssertNoErrors.device?.id?.let { deviceId = it }
    }

    suspend fun deleteDevice() {
        deviceId?.let { deviceId ->
            val result = api.apollo.mutation(
                DeleteDeviceMutation(deviceId)
            ).execute()

            if (result.data?.device?.id == deviceId) {
                this.deviceId = null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is UserSession) {
            return false
        }
        return other.tenant.id == tenant.id && other.user.id == user.id
    }
}

@Serializable()
data class PersistedUserSession(
    val tenant: Tenant,
    val user: User
)

val LocalUserSession = compositionLocalOf<UserSession> {
    error("No UserSession provided")
}
