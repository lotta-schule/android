package net.einsa.lotta.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.http.get
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.auth0.jwt.JWT
import net.einsa.lotta.AuthInfo

class RefreshTokenInterceptor(val loginSession: AuthInfo?) : HttpInterceptor {
    @OptIn(ApolloExperimental::class)
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        val response = chain.proceed(request)

        loginSession?.let { authInfo ->
            response.headers.get("set-cookie")?.let { cookies ->
                print("Cookies: $cookies")
                val refreshToken = cookies.split(";")
                    .firstOrNull { it.contains("SignInRefreshToken=") }?.split("=")?.get(1)
                if (refreshToken != null) {
                    authInfo.refreshToken = JWT.decode(refreshToken)

                }
            }
        }

        return response
    }
}