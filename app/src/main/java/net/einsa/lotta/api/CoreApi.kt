package net.einsa.lotta.api

import com.apollographql.apollo3.ApolloClient
import net.einsa.lotta.App
import net.einsa.lotta.AuthInfo
import java.net.URI

const val LOTTA_API_HOST = "core.lotta.schule"
const val USE_SECURE_CONNECTION = true
val LOTTA_API_HTTP_URL =
    (if (USE_SECURE_CONNECTION) "https" else "http") + "://$LOTTA_API_HOST"
val LOTTA_API_WEBSOCKET_URL =
    (if (USE_SECURE_CONNECTION) "wss" else "ws") + "://$LOTTA_API_HOST/api/graphql-socket/websocket"

val baseCacheDirURL = App.get().filesDir.toURI()
    .resolve(
        LOTTA_API_HOST.replace(Regex(":\\d{4,5}\$"), "")
            .trim('.')
    )

class CoreApi() {
    var apollo: ApolloClient
        private set

    var cacheUrl: URI? = null

    companion object {
        fun getCacheUrl(tenantId: String): URI {
            val sqliteFileURL = baseCacheDirURL.resolve("tenant_$tenantId.sqlite")
            println(sqliteFileURL)
            return sqliteFileURL
        }
    }

    init {
        this.apollo =
            ApolloClient.Builder()
                .httpServerUrl("$LOTTA_API_HTTP_URL/api")
                .build()
    }

    constructor(tenantSlug: String, loginSession: AuthInfo? = null) : this() {
        this.apollo =
            ApolloClient.Builder()
                .httpServerUrl("$LOTTA_API_HTTP_URL/api")
                .addHttpHeader("Tenant", "slug:$tenantSlug")
                .addHttpInterceptor(AuthorizationInterceptor(loginSession))
                // TODO: Add refresh token interceptor
                .build()

    }
}