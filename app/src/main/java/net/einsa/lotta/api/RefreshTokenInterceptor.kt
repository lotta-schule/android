package net.einsa.lotta.api

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import net.einsa.lotta.AuthInfo

/** Picks up a (rotated) refresh token the server sends via the `SignInRefreshToken` cookie. */
class RefreshTokenInterceptor(val loginSession: AuthInfo?) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        val response = chain.proceed(request)

        loginSession?.let { authInfo ->
            // There can be multiple Set-Cookie headers (access token + refresh token): check all of them
            val setCookieHeaders = response.headers
                .filter { it.name.equals("set-cookie", ignoreCase = true) }
                .map { it.value }
            AuthInfo.refreshTokenFromSetCookie(setCookieHeaders)?.let { refreshToken ->
                runCatching { authInfo.refreshToken = refreshToken }
            }
        }

        return response
    }
}
