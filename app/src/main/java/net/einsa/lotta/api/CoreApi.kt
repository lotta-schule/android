package net.einsa.lotta.api

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.network.ws.GraphQLWsProtocol
import net.einsa.lotta.AuthInfo
import net.einsa.lotta.BuildConfig
import java.io.File

const val LOTTA_API_HOST = BuildConfig.API_HOST
val USE_SECURE_CONNECTION: Boolean = BuildConfig.USE_SECURE_CONNECTION

val LOTTA_API_HTTP_URL =
    (if (USE_SECURE_CONNECTION) "https" else "http") + "://$LOTTA_API_HOST"
val LOTTA_API_WEBSOCKET_URL =
    (if (USE_SECURE_CONNECTION) "wss" else "ws") + "://$LOTTA_API_HOST/api/graphql-socket/websocket"

val baseCacheDir: File
    get() = LottaEnvironment.baseCacheDir

class CoreApi() {
    var apollo: ApolloClient
        private set

    var cacheFile: File? = null

    companion object {
        fun getCacheFile(tenantId: String): File {
            return baseCacheDir.resolve("tenant_$tenantId.sqlite")
        }
    }

    init {
        this.apollo =
            ApolloClient.Builder()
                .httpServerUrl("${LottaEnvironment.apiBaseUrl}/api")
                .normalizedCache(
                    MemoryCacheFactory(maxSizeBytes = 25 * 1024 * 1024)
                )
                .build()
    }

    constructor(tenantSlug: String, loginSession: AuthInfo? = null) : this() {
        this.apollo =
            ApolloClient.Builder()
                .normalizedCache(
                    MemoryCacheFactory(maxSizeBytes = 25 * 1024 * 1024)
                )
                .httpServerUrl("${LottaEnvironment.apiBaseUrl}/api")
                .addHttpHeader("Tenant", "slug:$tenantSlug")
                .addHttpInterceptor(AuthorizationInterceptor(loginSession))
                .addHttpInterceptor(RefreshTokenInterceptor(loginSession))
                .build()
    }

    constructor(tenantSlug: String, tenantId: String, loginSession: AuthInfo) : this() {
        cacheFile = getCacheFile(tenantId)

        val store = LottaEnvironment.normalizedCacheFactory(cacheFile!!)

        apollo =
            ApolloClient.Builder()
                .normalizedCache(store)
                .httpServerUrl("${LottaEnvironment.apiBaseUrl}/api")
                .addHttpHeader("Tenant", "slug:$tenantSlug")
                .addHttpInterceptor(AuthorizationInterceptor(loginSession))
                .addHttpInterceptor(RefreshTokenInterceptor(loginSession))
                .wsProtocol(
                    GraphQLWsProtocol.Factory(
                        connectionPayload = {
                            mapOf(
                                "tid" to tenantId,
                                "token" to loginSession.accessToken?.token
                            )
                        },
                    )
                )
                .webSocketServerUrl(LOTTA_API_WEBSOCKET_URL)
                .build()
    }

    fun resetCache() {
        cacheFile?.let {
            it.runCatching { delete() }
        }
    }
}