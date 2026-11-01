package net.einsa.lotta

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.einsa.lotta.util.SecretStore
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.Date
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

object JWTFactory {
    fun make(
        typ: String,
        expiresInSeconds: Long = 3600,
        userId: String = MockBackend.USER_ID,
        tenantId: Int = MockBackend.TENANT_ID.toInt(),
    ): String = JWT.create()
        .withJWTId(UUID.randomUUID().toString())
        .withSubject(userId)
        .withClaim("typ", typ)
        .withClaim("tid", tenantId)
        .withExpiresAt(Date(System.currentTimeMillis() + expiresInSeconds * 1000))
        .sign(Algorithm.HMAC256("test-secret"))
}

class InMemorySecretStore : SecretStore {
    val values = mutableMapOf<String, String>()

    override fun get(key: String): String? = synchronized(values) { values[key] }
    override fun set(key: String, value: String) {
        synchronized(values) { values[key] = value }
    }

    override fun remove(key: String) {
        synchronized(values) { values.remove(key) }
    }
}

/** An in-process fake of the lotta core-api. */
class MockBackend {
    companion object {
        const val TENANT_ID = "1"
        const val USER_ID = "42"
        const val DEFAULT_TENANT_SLUG = "test-school"

        fun cookieHeaders(refreshToken: String, accessToken: String): List<String> = listOf(
            "SignInAccessToken=$accessToken; path=/; HttpOnly",
            "SignInRefreshToken=$refreshToken; path=/; max-age=2419200; HttpOnly",
        )

        fun jsonResponse(body: String, status: Int = 200, headers: List<String> = emptyList()) =
            MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
                .apply { headers.forEach { addHeader("Set-Cookie", it) } }

        /** A URL nothing listens on: requests fail with "connection refused", like being offline. */
        const val UNREACHABLE_URL = "http://127.0.0.1:1"
    }

    data class Request(
        val path: String,
        val operationName: String?,
        val headers: Map<String, String>,
        val queryToken: String?,
    )

    val server = MockWebServer()
    val requests = CopyOnWriteArrayList<Request>()

    var tenantSlug = DEFAULT_TENANT_SLUG

    /** Current backend behaviour: refresh token only in the cookie, `success` in the body. */
    var refreshHandler: (RecordedRequest) -> MockResponse = {
        jsonResponse(
            """{"success":true,"accessToken":"${JWTFactory.make("access")}"}""",
            headers = cookieHeaders(
                refreshToken = JWTFactory.make("refresh", expiresInSeconds = 3600L * 24 * 28),
                accessToken = JWTFactory.make("access"),
            )
        )
    }

    var loginHandler: (RecordedRequest) -> MockResponse = {
        jsonResponse(
            """{"data":{"login":{"__typename":"Authresult","accessToken":"${JWTFactory.make("access")}","refreshToken":"${
                JWTFactory.make("refresh", expiresInSeconds = 3600L * 24 * 28)
            }"}}}"""
        )
    }

    var graphqlHandler: ((RecordedRequest, String?) -> MockResponse)? = null

    private val json = Json { ignoreUnknownKeys = true }

    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                val body = request.body.readUtf8()
                val operationName = if (path == "/api") {
                    runCatching {
                        json.parseToJsonElement(body).jsonObject["operationName"]?.jsonPrimitive?.content
                    }.getOrNull()
                } else null

                requests.add(
                    Request(
                        path = path,
                        operationName = operationName,
                        headers = request.headers.associate { it.first.lowercase() to it.second },
                        queryToken = request.requestUrl?.queryParameter("token"),
                    )
                )

                if (path == "/auth/token/refresh") {
                    return refreshHandler(request)
                }
                if (path == "/api") {
                    graphqlHandler?.let { return it(request, operationName) }
                    return when (operationName) {
                        "GetTenant" -> jsonResponse(tenantResponse())
                        "Login" -> loginHandler(request)
                        "GetCurrentUser" -> jsonResponse(currentUserResponse())
                        else -> jsonResponse("""{"data":null,"errors":[{"message":"unknown operation $operationName"}]}""")
                    }
                }
                return MockResponse().setResponseCode(404)
            }
        }
        server.start()
    }

    val baseUrl: String
        get() = server.url("/").toString().trimEnd('/')

    fun requests(forPath: String) = requests.filter { it.path == forPath }

    fun shutdown() = server.shutdown()

    fun tenantResponse() = """
        {"data":{"tenant":{"__typename":"Tenant","id":"$TENANT_ID","title":"Test School","slug":"$tenantSlug","host":"test.lotta.schule",
          "configuration":{"__typename":"TenantConfiguration","backgroundImageFile":null,"logoImageFile":null,"customTheme":null,"userMaxStorageConfig":null}}}}
    """.trimIndent()

    fun currentUserResponse() = """
        {"data":{"currentUser":{"__typename":"User","id":"$USER_ID","insertedAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z",
          "lastSeen":null,"name":"Test User","nickname":null,"email":"test@lotta.schule","class":null,"hideFullName":false,
          "enrollmentTokens":[],"unreadMessages":0,"hasChangedDefaultPassword":true,"avatarImageFile":null,"groups":[]}}}
    """.trimIndent()
}
