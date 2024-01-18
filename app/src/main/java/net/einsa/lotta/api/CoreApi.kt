package net.einsa.lotta.api

import android.content.Context
import com.apollographql.apollo3.ApolloClient
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
    App.get().getDir(
        LOTTA_API_HOST.replace(Regex(":\\d{4,5}\$"), ""),
        Context.MODE_PRIVATE
    )!!

class CoreApi() {
    var apollo: ApolloClient
        private set

    var cacheUrl: File? = null

    companion object {
        fun getCacheFile(tenantId: String): File {
            return baseCacheDir.resolve("tenant_$tenantId.sqlite")
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
                .addHttpInterceptor(RefreshTokenInterceptor(loginSession))
                .build()
    }

    fun resetCache() {
        cacheUrl?.let {
            it.runCatching { delete() }
        }
    }
}