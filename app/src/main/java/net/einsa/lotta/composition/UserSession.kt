package net.einsa.lotta.composition

import androidx.compose.runtime.compositionLocalOf
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.auth0.jwt.JWT
import io.sentry.Sentry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.einsa.lotta.AuthInfo
import net.einsa.lotta.GetCurrentUserQuery
import net.einsa.lotta.GetTenantQuery
import net.einsa.lotta.LoginMutation
import net.einsa.lotta.api.CoreApi
import net.einsa.lotta.api.baseCacheDir
import net.einsa.lotta.model.Tenant
import net.einsa.lotta.model.User
import net.einsa.lotta.util.SecretKeyStore

class UserSession// TODO: Should get tenantId for subscriptions
    (tenant: Tenant, authInfo: AuthInfo, user: User) {
    companion object {
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
                tokenData.login?.accessToken ?: throw Exception("No access token in repsonse")
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
                    Regex("^usersession-([^-]+)-([^-]+)\\.json\$").matchEntire(file.name)
                        ?.let { match ->
                            val tenantSlug = match.groupValues[1]
                            val userId = match.groupValues[2]
                            Triple(file, tenantSlug, userId)
                        }
                } ?: emptyList()

                for ((file, tenantSlug, userId) in files) {
                    try {
                        val userSessionData = file.readText(Charsets.UTF_8)
                        val persistedUserSession =
                            Json.decodeFromString<PersistedUserSession>(userSessionData)
                        val tenantId = persistedUserSession.tenant.id
                        val refreshToken = SecretKeyStore.instance.get(
                            SecretKeyStore.refreshTokenKey(
                                userId,
                                tenantId
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                            print("Could not renew auth token: ${e.message}")
                        }

                        val userSession = UserSession(
                            tenant = persistedUserSession.tenant,
                            authInfo = authInfo,
                            user = persistedUserSession.user
                        )

                        sessions.add(userSession)

                        userSession.refetchUserData()
                        userSession.refetchTenantData()

                        userSession.writeToDisk()
                    } catch (e: Exception) {
                        Sentry.captureException(e)
                        print("Could not read user session from disk: ${e.message}")

                        SecretKeyStore.instance.remove(
                            "refreshToken-$tenantSlug-$userId"
                        )
                        file.runCatching { delete() }
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
    var authInfo: AuthInfo = authInfo
        private set
    var user: User = user
        private set
    var api: CoreApi
        private set
    var deviceId: Any? = null // TODO: Type this
        private set

    init {
        this.api = CoreApi(tenantSlug = tenant.slug, loginSession = authInfo)
    }

    suspend fun refetchUserData() {
        // TODO: Make sure apollo does not hit the cache on this one
        val userGraphqlResponse =
            api.apollo.query(GetCurrentUserQuery()).fetchPolicy(FetchPolicy.NetworkFirst)
                .execute()
        val user = userGraphqlResponse.dataAssertNoErrors.currentUser!!
        this.user = User.from(user, tenant = tenant)

        // Todo: Register for remote notifications
    }

    suspend fun refetchTenantData() {
        val tenantGraphqlResponse = api.apollo.query(GetTenantQuery()).execute()
        val tenant = tenantGraphqlResponse.dataAssertNoErrors.tenant!!
        this.tenant = Tenant.from(tenant)

        // Todo: Register for remote notifications
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
        SecretKeyStore.instance.remove(
            SecretKeyStore.refreshTokenKey(
                user.id,
                tenant.id
            )
        )
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
