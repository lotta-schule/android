package net.einsa.lotta

import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.auth0.jwt.JWT
import kotlinx.coroutines.runBlocking
import net.einsa.lotta.api.LottaEnvironment
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.util.SecretKeyStore
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/**
 * Makes sure a login survives an app restart, i.e. the refresh token ends up in the secret store
 * and `UserSession.readFromDisk()` restores the session.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthPersistenceTest {
    private lateinit var backend: MockBackend
    private lateinit var secretStore: InMemorySecretStore
    private lateinit var baseDirectory: File

    private val refreshTokenKey =
        SecretKeyStore.refreshTokenKey(MockBackend.USER_ID, MockBackend.TENANT_ID)

    @Before
    fun setUp() {
        backend = MockBackend()
        secretStore = InMemorySecretStore()
        baseDirectory = Files.createTempDirectory("lotta-tests-").toFile()

        LottaEnvironment.apiBaseUrl = backend.baseUrl
        LottaEnvironment.okHttpClient = OkHttpClient()
        LottaEnvironment.secretStore = secretStore
        LottaEnvironment.baseCacheDir = baseDirectory
        LottaEnvironment.normalizedCacheFactory = { MemoryCacheFactory(10 * 1024 * 1024) }
    }

    @After
    fun tearDown() {
        backend.shutdown()
        baseDirectory.deleteRecursively()
    }

    // region Helpers

    private suspend fun login(): UserSession = UserSession.createFromCredentials(
        tenantSlug = backend.tenantSlug,
        username = "test@lotta.schule",
        password = "password",
    )

    private fun sessionFile(slug: String = backend.tenantSlug) =
        baseDirectory.resolve("usersession-$slug-${MockBackend.USER_ID}.json")

    private fun makeAuthInfo(): Pair<AuthInfo, String> {
        val oldRefreshToken = JWTFactory.make("refresh", expiresInSeconds = 3600L * 24)
        return AuthInfo(refreshToken = JWT.decode(oldRefreshToken)) to oldRefreshToken
    }

    private inline fun <reified T : Throwable> assertThrowsSuspend(crossinline block: suspend () -> Unit): T {
        try {
            runBlocking { block() }
        } catch (e: Throwable) {
            assertTrue("Expected ${T::class.simpleName}, got $e", e is T)
            return e as T
        }
        throw AssertionError("Expected ${T::class.simpleName} to be thrown")
    }

    // endregion

    // region Login persistence

    @Test
    fun credentialLoginIsRestoredAfterRestart() = runBlocking {
        val session = login()
        assertEquals(MockBackend.USER_ID, session.user.id)

        val storedToken = secretStore.get(refreshTokenKey)
        assertNotNull("refresh token was not saved to the secret store", storedToken)
        assertEquals("refresh", JWT.decode(storedToken).getClaim("typ").asString())
        assertTrue(sessionFile().exists())

        val restored = UserSession.readFromDisk()
        assertEquals(1, restored.size)
        assertEquals(MockBackend.USER_ID, restored.first().user.id)
        assertEquals(MockBackend.TENANT_ID, restored.first().tenant.id)
    }

    @Test
    fun cookieOnlyLoginIsRestoredAfterRestart() = runBlocking {
        backend.loginHandler = {
            val accessToken = JWTFactory.make("access")
            MockBackend.jsonResponse(
                """{"data":{"login":{"__typename":"Authresult","accessToken":"$accessToken","refreshToken":null}}}""",
                headers = MockBackend.cookieHeaders(
                    refreshToken = JWTFactory.make("refresh", expiresInSeconds = 3600L * 24),
                    accessToken = accessToken,
                )
            )
        }

        login()
        assertNotNull(secretStore.get(refreshTokenKey))

        assertEquals(1, UserSession.readFromDisk().size)
    }

    @Test
    fun tenantSlugWithoutDashIsRestored() = runBlocking {
        backend.tenantSlug = "testschool"
        login()

        assertTrue(sessionFile("testschool").exists())
        assertEquals(1, UserSession.readFromDisk().size)
    }

    @Test
    fun sessionRestoredWhileOfflineIsKept() = runBlocking {
        login()

        LottaEnvironment.apiBaseUrl = MockBackend.UNREACHABLE_URL

        val restored = UserSession.readFromDisk()
        assertEquals(1, restored.size)
        assertNotNull(secretStore.get(refreshTokenKey))
        assertTrue(sessionFile().exists())
    }

    @Test
    fun sessionWithServerErrorOnRestoreIsKept() = runBlocking {
        login()

        backend.refreshHandler = { MockBackend.jsonResponse("", status = 502) }
        backend.graphqlHandler = { _, _ -> MockBackend.jsonResponse("", status = 502) }

        assertEquals(1, UserSession.readFromDisk().size)
        assertNotNull(secretStore.get(refreshTokenKey))
        assertTrue(sessionFile().exists())
    }

    @Test
    fun sessionWithRejectedRefreshTokenIsRemoved() = runBlocking {
        login()

        backend.refreshHandler = {
            MockBackend.jsonResponse("""{"errors":{"detail":"Unauthorized"}}""", status = 401)
        }

        assertEquals(0, UserSession.readFromDisk().size)
        assertNull(secretStore.get(refreshTokenKey))
        assertTrue(!sessionFile().exists())
    }

    @Test
    fun loggingOutRemovesRefreshToken() = runBlocking {
        val session = login()
        assertNotNull(secretStore.get(refreshTokenKey))

        session.removeFromKeychain()

        assertNull(secretStore.get(refreshTokenKey))
    }

    // endregion

    // region Token renewal

    @Test
    fun renewSendsRefreshTokenAndTenantId() = runBlocking {
        val (authInfo, oldRefreshToken) = makeAuthInfo()
        authInfo.renew()

        val request = backend.requests(forPath = "/auth/token/refresh").first()
        assertEquals(oldRefreshToken, request.queryToken)
        assertEquals("id:${MockBackend.TENANT_ID}", request.headers["tenant"])
    }

    @Test
    fun renewStoresRefreshTokenFromBody() = runBlocking {
        val (authInfo, oldRefreshToken) = makeAuthInfo()
        val returnedRefreshToken = JWTFactory.make("refresh", expiresInSeconds = 3600L * 24 * 28)
        backend.refreshHandler = {
            MockBackend.jsonResponse(
                """{"accessToken":"${JWTFactory.make("access")}","refreshToken":"$returnedRefreshToken"}"""
            )
        }

        authInfo.renew()

        assertNotEquals(oldRefreshToken, returnedRefreshToken)
        assertEquals(returnedRefreshToken, authInfo.refreshToken?.token)
        assertEquals(returnedRefreshToken, secretStore.get(refreshTokenKey))
        assertNotNull(authInfo.accessToken)
    }

    @Test
    fun renewStoresRotatedRefreshTokenFromCookie() = runBlocking {
        val (authInfo, oldRefreshToken) = makeAuthInfo()
        val rotatedRefreshToken = JWTFactory.make("refresh", expiresInSeconds = 3600L * 24 * 28)
        backend.refreshHandler = {
            MockBackend.jsonResponse(
                """{"success":true,"accessToken":"${JWTFactory.make("access")}"}""",
                headers = MockBackend.cookieHeaders(
                    refreshToken = rotatedRefreshToken,
                    accessToken = JWTFactory.make("access"),
                )
            )
        }

        authInfo.renew()

        assertNotEquals(oldRefreshToken, rotatedRefreshToken)
        assertEquals(rotatedRefreshToken, authInfo.refreshToken?.token)
        assertEquals(rotatedRefreshToken, secretStore.get(refreshTokenKey))
    }

    @Test
    fun renewKeepsCurrentRefreshTokenIfServerReturnsNone() = runBlocking {
        val (authInfo, oldRefreshToken) = makeAuthInfo()
        backend.refreshHandler = {
            MockBackend.jsonResponse("""{"success":true,"accessToken":"${JWTFactory.make("access")}"}""")
        }

        authInfo.renew()

        assertEquals(oldRefreshToken, authInfo.refreshToken?.token)
        assertEquals(oldRefreshToken, secretStore.get(refreshTokenKey))
        assertNotNull(authInfo.accessToken)
    }

    @Test
    fun renewReportsRejectedRefreshTokenAsInvalid() {
        val (authInfo, _) = makeAuthInfo()
        backend.refreshHandler = {
            MockBackend.jsonResponse("""{"errors":{"detail":"Unauthorized"}}""", status = 401)
        }

        assertThrowsSuspend<RenewException.InvalidToken> { authInfo.renew() }
    }

    @Test
    fun renewReportsServerErrorsAsConnectionErrors() {
        val (authInfo, _) = makeAuthInfo()
        backend.refreshHandler = { MockBackend.jsonResponse("", status = 502) }

        assertThrowsSuspend<RenewException.ConnectionError> { authInfo.renew() }
    }

    @Test
    fun renewReportsNetworkFailuresAsConnectionErrors() {
        val (authInfo, _) = makeAuthInfo()
        LottaEnvironment.apiBaseUrl = MockBackend.UNREACHABLE_URL

        assertThrowsSuspend<RenewException.ConnectionError> { authInfo.renew() }
    }

    @Test
    fun renewReportsNonLottaResponsesAsConnectionErrors() {
        val (authInfo, oldRefreshToken) = makeAuthInfo()
        // e.g. a captive portal
        backend.refreshHandler = {
            MockBackend.jsonResponse("<html>Please log in to the WiFi</html>")
        }

        assertThrowsSuspend<RenewException.ConnectionError> { authInfo.renew() }
        assertEquals(oldRefreshToken, secretStore.get(refreshTokenKey))
    }

    // endregion

    // region Cookie parsing

    @Test
    fun refreshTokenIsPickedFromMultipleCookies() {
        val refreshToken = JWTFactory.make("refresh")
        val parsed = AuthInfo.refreshTokenFromSetCookie(
            MockBackend.cookieHeaders(refreshToken = refreshToken, accessToken = JWTFactory.make("access"))
        )
        assertEquals(refreshToken, parsed?.token)
    }

    @Test
    fun missingRefreshTokenCookieReturnsNull() {
        assertNull(AuthInfo.refreshTokenFromSetCookie(listOf("SignInAccessToken=abc; path=/")))
        assertNull(AuthInfo.refreshTokenFromSetCookie(emptyList()))
    }

    // endregion
}
