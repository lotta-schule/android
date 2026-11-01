package net.einsa.lotta.api

import android.content.Context
import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import net.einsa.lotta.App
import net.einsa.lotta.util.SecretKeyStore
import net.einsa.lotta.util.SecretStore
import okhttp3.OkHttpClient
import java.io.File

/**
 * Overridable runtime configuration. The defaults are what the app uses in production;
 * unit tests swap them out to use a mocked backend, a temporary directory and an in-memory secret store.
 */
object LottaEnvironment {
    /** Base URL of the lotta API (GraphQL lives at `$apiBaseUrl/api`, token refresh at `$apiBaseUrl/auth/...`). */
    var apiBaseUrl: String = LOTTA_API_HTTP_URL

    /** Client used for plain HTTP requests to the lotta API (e.g. token refresh). */
    var okHttpClient: OkHttpClient = OkHttpClient()

    private var _secretStore: SecretStore? = null
    var secretStore: SecretStore
        get() = _secretStore ?: SecretKeyStore.instance.also { _secretStore = it }
        set(value) {
            _secretStore = value
        }

    private var _baseCacheDir: File? = null

    /** Directory where user sessions and caches are stored. */
    var baseCacheDir: File
        get() = _baseCacheDir ?: App.context.getDir(
            LOTTA_API_HOST.replace(Regex(":\\d{4,5}\$"), ""),
            Context.MODE_PRIVATE
        )!!.also { _baseCacheDir = it }
        set(value) {
            _baseCacheDir = value
        }

    /** Creates the persistent normalized cache for a tenant. */
    var normalizedCacheFactory: (File) -> NormalizedCacheFactory =
        { file -> SqlNormalizedCacheFactory(file.absolutePath) }
}
