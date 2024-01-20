package net.einsa.lotta.api

import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import net.einsa.lotta.App
import net.einsa.lotta.AuthInfo
import java.io.File

const val LOTTA_API_HOST = "core.lotta.schule"
const val USE_SECURE_CONNECTION = true
val LOTTA_API_HTTP_URL =
    (if (USE_SECURE_CONNECTION) "https" else "http") + "://$LOTTA_API_HOST"
val LOTTA_API_WEBSOCKET_URL =
    (if (USE_SECURE_CONNECTION) "wss" else "ws") + "://$LOTTA_API_HOST/api/graphql-socket/websocket"

val baseCacheDir =
    App.context.getDir(
        LOTTA_API_HOST.replace(Regex(":\\d{4,5}\$"), ""),
        Context.MODE_PRIVATE
    )!!

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
                .httpServerUrl("$LOTTA_API_HTTP_URL/api")
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
                .httpServerUrl("$LOTTA_API_HTTP_URL/api")
                .addHttpHeader("Tenant", "slug:$tenantSlug")
                .addHttpInterceptor(AuthorizationInterceptor(loginSession))
                .addHttpInterceptor(RefreshTokenInterceptor(loginSession))
                .build()
    }

    constructor(tenantSlug: String, tenantId: String, loginSession: AuthInfo) : this() {
        cacheFile = getCacheFile(tenantId)

        val store = SqlNormalizedCacheFactory(cacheFile!!.absolutePath)

        apollo =
            ApolloClient.Builder()
                .normalizedCache(store)
                .httpServerUrl("$LOTTA_API_HTTP_URL/api")
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