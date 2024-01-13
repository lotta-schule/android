package net.einsa.lotta

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.einsa.lotta.api.LOTTA_API_HTTP_URL
import okhttp3.OkHttpClient

class AuthInfo {
    var accessToken: DecodedJWT? = null
    var refreshToken: DecodedJWT? = null
        set(value) {
            // TODO: Save refresh token to secure storage
            // if let stringValue = newValue ?. string, let tid = newValue?.claim(name: "tid").integer, let uid = newValue?.subject {
            //     keychain.set(stringValue, forKey: "\(tid)-\(uid)--refresh-token")
            // }
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

        val url = "$LOTTA_API_HTTP_URL/auth/token/refresh?token=${refreshToken.toString()}"

        val request =
            okhttp3.Request.Builder()
                .url(url)
                .header("tenant", "id:$tid")
                .method("POST", null)
                .build()

        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Could not renew auth token")
            }
            val jsonString = response.body!!.string()

            Json.decodeFromString<TokenPair>(jsonString)
        }
    }
}

class TokenPair(
    var accessToken: DecodedJWT? = null,
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