package net.einsa.lotta

import com.auth0.jwt.JWT
import net.einsa.lotta.api.CoreApi
import net.einsa.lotta.model.Tenant
import net.einsa.lotta.model.User

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

            val tenant = Tenant.from(tenantData)

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

            // TODO
            // userSession.writeToDisk()

            return userSession
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
        val userGraphqlResponse = api.apollo.query(GetCurrentUserQuery()).execute()
        val user = userGraphqlResponse.dataAssertNoErrors.currentUser!!
        this.user = User.from(user, tenant = tenant)

        // Todo: Register for remote notifications
    }
}