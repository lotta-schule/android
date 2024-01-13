package net.einsa.lotta.api

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.auth0.jwt.interfaces.DecodedJWT
import net.einsa.lotta.AuthInfo

class AuthorizationInterceptor(
    var loginSession: AuthInfo?,
) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        // If the user has neither an access token nor a refresh token, don't bother
        if (loginSession?.accessToken == null && loginSession?.refreshToken == null) {
            return chain.proceed(request)
        }

        // If we've gotten here, there is a token!
        // If we have an expired refresh token, we abort
        // If the refresh token is valid or there is no refresh token, we continue
        if (loginSession?.refreshToken?.expiresAt != null && (loginSession!!.refreshToken!!.expiresAt.time < System.currentTimeMillis())) {
            loginSession!!.accessToken = null
            loginSession!!.refreshToken = null
            // TODO: Here we must notify of a logout action
            return chain.proceed(request)
        }

        // We now know we have:
        //  - either no refreshtoken but some accessToken
        //  - A valid refresh token and maybe an access token
        // This means, if:
        //  - there is no access token or it is not valid, try to renew with the refresh Token
        // else
        //  - the access token may be valid, just use it
        if (loginSession?.accessToken?.expiresAt == null || loginSession!!.accessToken!!.expiresAt.time < System.currentTimeMillis()) {
            try {
                loginSession?.renew()
                return addTokenAndProceed(loginSession?.accessToken, request, chain)
            } catch (e: Exception) {
                e.printStackTrace()
                return chain.proceed(request)
            }
        } else {
            return addTokenAndProceed(loginSession?.accessToken, request, chain)
        }
    }

    private suspend fun addTokenAndProceed(
        accessToken: DecodedJWT?,
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        val newReq = request.newBuilder()
            .apply {
                accessToken?.let {
                    addHeader("Authorization", "Bearer ${it.token}")
                }
            }
            .build()

        return chain.proceed(newReq)
    }
}