package net.einsa.lotta

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.einsa.lotta.api.LOTTA_API_HTTP_URL
import net.einsa.lotta.serializer.JWTSerializer
import net.einsa.lotta.util.SecretKeyStore
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

class AuthInfo {
    constructor(accessToken: DecodedJWT? = null, refreshToken: DecodedJWT? = null) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    var accessToken: DecodedJWT? = null
    var refreshToken: DecodedJWT? = null
        set(value) {
            value?.let { refreshToken ->
                val tid = refreshToken.getClaim("tid")?.asInt()
                    ?: throw Exception("Refresh token is not valid")
                val uid = refreshToken.subject
                    ?: throw Exception("Refresh token is not valid")

                SecretKeyStore.instance.set(
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

    suspend fun renew() {
        if (refreshToken == null) {
            throw Exception("No refresh token available")
        }
        val tid = refreshToken!!.getClaim("tid")?.asInt()
            ?: throw Exception("Refresh token is not valid")

        val url = "$LOTTA_API_HTTP_URL/auth/token/refresh?token=${refreshToken!!.token}"

        val request =
            okhttp3.Request.Builder()
                .url(url)
                .header("tenant", "id:$tid")
                .method(
                    "POST", ByteArray(0).toRequestBody(null, 0, 0)
                )
                .build()

        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Could not renew auth token")
            }
            val jsonString = response.body!!.string()

            val tokenPair = Json.decodeFromString<TokenPair>(jsonString)

            accessToken = tokenPair.accessToken
            refreshToken = tokenPair.refreshToken
        }
    }
}

@Serializable
class TokenPair(
    @Serializable(with = JWTSerializer::class)
    var accessToken: DecodedJWT? = null,

    @Serializable(with = JWTSerializer::class)
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
            val accessToken: String?,
            val refreshToken: String?
        )
    }
}