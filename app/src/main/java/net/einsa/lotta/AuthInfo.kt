package net.einsa.lotta

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.einsa.lotta.api.LottaEnvironment
import net.einsa.lotta.util.SecretKeyStore
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

sealed class RenewException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** The server rejected the refresh token: the user has to log in again. */
    class InvalidToken(message: String, cause: Throwable? = null) : RenewException(message, cause)

    /** Network or server-side problem: keep the session and try again later. */
    class ConnectionError(message: String, cause: Throwable? = null) :
        RenewException(message, cause)
}

class AuthInfo {
    constructor(accessToken: DecodedJWT? = null, refreshToken: DecodedJWT? = null) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    var accessToken: DecodedJWT? = null

    /** Setting a refresh token persists it to the secret store right away. */
    var refreshToken: DecodedJWT? = null
        set(value) {
            value?.let { refreshToken ->
                val tid = refreshToken.getClaim("tid")?.asInt()
                    ?: throw Exception("Refresh token is not valid")
                val uid = refreshToken.subject
                    ?: throw Exception("Refresh token is not valid")

                LottaEnvironment.secretStore.set(
                    SecretKeyStore.refreshTokenKey(uid, tid.toString()),
                    refreshToken.token
                )
            }
            field = value
        }

    val needsRenew: Boolean
        get() {
            if (accessToken == null) {
                return true
            }
            if (refreshToken == null) {
                return false
            }
            return refreshToken!!.expiresAt!!.time < System.currentTimeMillis()
        }

    val isLoggedIn: Boolean
        get() {
            if (accessToken != null) {
                return accessToken!!.expiresAt!!.time > System.currentTimeMillis()
            }
            if (refreshToken != null) {
                return refreshToken!!.expiresAt!!.time > System.currentTimeMillis()
            }
            return false
        }

    /**
     * Requests a new access token using the refresh token.
     *
     * @throws RenewException.InvalidToken if the server rejected the refresh token
     * @throws RenewException.ConnectionError on network or server errors
     */
    suspend fun renew() {
        val refreshTokenBeforeWaiting = refreshToken
        renewMutex.withLock {
            // Refresh tokens are rotated: if another caller renewed while we waited for the lock,
            // the old token may already be invalidated, so don't renew again.
            if (refreshTokenBeforeWaiting != null && refreshToken !== refreshTokenBeforeWaiting && accessToken != null) {
                return
            }
            performRenew()
        }
    }

    private val renewMutex = Mutex()

    private suspend fun performRenew() {
        val currentRefreshToken =
            refreshToken ?: throw RenewException.InvalidToken("No refresh token available")
        val tid = currentRefreshToken.getClaim("tid")?.asInt()
            ?: throw RenewException.InvalidToken("Refresh token is not valid")

        val url =
            "${LottaEnvironment.apiBaseUrl}/auth/token/refresh?token=${currentRefreshToken.token}"

        val request =
            okhttp3.Request.Builder()
                .url(url)
                .header("tenant", "id:$tid")
                .method(
                    "POST", ByteArray(0).toRequestBody(null, 0, 0)
                )
                .build()

        val (accessToken, newRefreshToken) = withContext(Dispatchers.IO) {
            val response = try {
                LottaEnvironment.okHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                throw RenewException.ConnectionError("Could not reach server", e)
            }

            response.use { response ->
                if (!response.isSuccessful) {
                    when (response.code) {
                        400, 401, 403 ->
                            throw RenewException.InvalidToken("Refresh token was rejected (${response.code})")

                        else -> {
                            Sentry.captureMessage("AuthInfo#renew: unexpected status code ${response.code}")
                            throw RenewException.ConnectionError("Unexpected status code ${response.code}")
                        }
                    }
                }

                val body = try {
                    response.body?.string()
                } catch (e: IOException) {
                    throw RenewException.ConnectionError("Could not read response", e)
                }

                val tokens = try {
                    json.decodeFromString<TokenPair.Companion.Json>(body ?: "")
                } catch (e: Exception) {
                    // Not a lotta response (e.g. a captive portal): keep the session, try again later
                    Sentry.captureMessage("AuthInfo#renew: invalid response data")
                    throw RenewException.ConnectionError("Invalid response data", e)
                }

                val accessToken = tokens.accessToken?.let { runCatching { JWT.decode(it) }.getOrNull() }
                    ?: throw RenewException.InvalidToken("No valid access token in response")

                // The refresh token is rotated on every renewal. The server sends it in the body
                // and/or as a cookie (current backend: cookie only). If neither is present,
                // the current refresh token stays valid until it expires.
                val newRefreshToken =
                    tokens.refreshToken?.let { runCatching { JWT.decode(it) }.getOrNull() }
                        ?: refreshTokenFromSetCookie(response.headers("Set-Cookie"))
                        ?: currentRefreshToken

                accessToken to newRefreshToken
            }
        }

        this.accessToken = accessToken
        this.refreshToken = newRefreshToken
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        const val REFRESH_TOKEN_COOKIE_NAME = "SignInRefreshToken"

        /** Extracts the `SignInRefreshToken` cookie the server sets on login and token refresh. */
        fun refreshTokenFromSetCookie(setCookieHeaders: List<String>): DecodedJWT? {
            return setCookieHeaders
                .asSequence()
                .mapNotNull { header ->
                    val nameValue = header.substringBefore(";").trim()
                    val name = nameValue.substringBefore("=", missingDelimiterValue = "").trim()
                    if (name == REFRESH_TOKEN_COOKIE_NAME) {
                        nameValue.substringAfter("=").trim().trim('"')
                    } else {
                        null
                    }
                }
                .filter { it.isNotEmpty() }
                .mapNotNull { runCatching { JWT.decode(it) }.getOrNull() }
                .firstOrNull()
        }
    }
}

@Serializable
class TokenPair(
    @Serializable(with = net.einsa.lotta.serializer.JWTSerializer::class)
    var accessToken: DecodedJWT? = null,

    @Serializable(with = net.einsa.lotta.serializer.JWTSerializer::class)
    var refreshToken: DecodedJWT? = null,
) {

    companion object {
        fun from(json: Json): TokenPair {
            var accessToken: DecodedJWT? = null
            var refreshToken: DecodedJWT? = null
            if (json.accessToken != null) {
                try {
                    accessToken = JWT.decode(json.accessToken)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (json.refreshToken != null) {
                try {
                    refreshToken = JWT.decode(json.refreshToken)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return TokenPair(accessToken, refreshToken)
        }

        @Serializable()
        data class Json(
            val accessToken: String? = null,
            val refreshToken: String? = null
        )
    }
}
