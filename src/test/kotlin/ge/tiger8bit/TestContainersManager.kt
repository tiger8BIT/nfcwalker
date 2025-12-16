package ge.tiger8bit

import com.buralotech.oss.testcontainers.mockoauth2.MockOAuth2Container
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.netty.DefaultHttpClient
import io.micronaut.json.JsonMapper
import org.testcontainers.containers.ContainerFetchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI

object TestContainersManager {
    private val logger = getLogger()

    // PostgreSQL container - singleton for all tests
    val postgres: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:15")).apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            withReuse(true)
            start()
            logger.info(
                "PostgreSQL container started: {}",
                jdbcUrl
            )
        }
    }

    // Mailhog SMTP + HTTP UI
    val mailhog: GenericContainer<*> by lazy {
        GenericContainer(DockerImageName.parse("mailhog/mailhog:latest")).apply {
            withExposedPorts(1025, 8025)
            start()
            logger.info(
                "MailHog container started: SMTP on port {}, HTTP API on port {}",
                getMappedPort(1025), getMappedPort(8025)
            )
        }
    }

    // Mock OAuth2 server for SSO / JWKS (used via TestContainersProvider)
    val oauth2: MockOAuth2Container? by lazy {
        try {
            val image = DockerImageName
                .parse("mock-oauth2-server:latest")
                .asCompatibleSubstituteFor("ghcr.io/navikt/mock-oauth2-server")

            MockOAuth2Container(image).apply {
                withReuse(true)
                start()
                logger.info("Mock OAuth2 container started on {}:{}", host, firstMappedPort)
            }
        } catch (e: ContainerFetchException) {
            logger.warn("Mock OAuth2 container image not available, SSO tests will be skipped", e)
            null
        }
    }

    fun getSmtpPort(): Int = mailhog.getMappedPort(1025)

    fun getPostgresJdbcUrl(): String = postgres.jdbcUrl
    fun getPostgresUsername(): String = postgres.username
    fun getPostgresPassword(): String = postgres.password

    /**
     * Issues an ID token from the mock-oauth2-server for the default realm.
     * This uses the mock server's token endpoint and does not touch real Google.
     */
    fun issueGoogleIdToken(
        subject: String = "google-user-123",
        email: String = "google.user@test.com",
        name: String = "Google User",
        clientId: String = "test-client-id",
    ): String {
        val container = oauth2
            ?: error("Mock OAuth2 container not available; ensure Docker image mock-oauth2-server:latest is present or skip SSO tests")

        // Build base URL to mock-oauth2-server using Testcontainers host and mapped port
        val host = container.host
        val port = container.firstMappedPort
        val serverUrl = "http://$host:$port/default" // default realm base path
        val tokenEndpoint = URI.create("$serverUrl/token")

        val httpClient: HttpClient = DefaultHttpClient(tokenEndpoint)
        val jsonMapper = JsonMapper.createDefault()

        val body = mapOf(
            "client_id" to clientId,
            "subject" to subject,
            "claims" to mapOf(
                "email" to email,
                "name" to name,
            ),
            "scope" to "openid email profile",
        )
        val jsonBody = jsonMapper.writeValueAsString(body)

        val request: HttpRequest<*> = HttpRequest.POST("", jsonBody)
            .contentType(MediaType.APPLICATION_JSON_TYPE)

        val response: HttpResponse<String> = httpClient.toBlocking()
            .exchange(request, String::class.java)

        if (response.status.code != 200) {
            error("Failed to obtain id_token from mock-oauth2-server: status=${'$'}{response.status}")
        }

        val responseJson = jsonMapper.readValue(response.body()!!, Map::class.java) as Map<*, *>
        val idToken = responseJson["id_token"] as? String
            ?: responseJson["access_token"] as? String
            ?: error("Mock-oauth2 response does not contain id_token/access_token: ${'$'}responseJson")

        logger.info("Issued mock Google id_token for subject={} email={}", subject, email)
        return idToken
    }
}
