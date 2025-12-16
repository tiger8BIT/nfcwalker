package ge.tiger8bit.spec

import ge.tiger8bit.TestContainersManager
import ge.tiger8bit.TestFixtures
import io.kotest.core.spec.style.StringSpec
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject

abstract class BaseApiSpec : StringSpec(), TestPropertyProvider {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Inject
    lateinit var specHelpers: SpecHelpers

    @Inject
    lateinit var fixtures: TestFixtures

    init {
        // Clean database before each test and before spec starts
        beforeTest {
            try {
                initializeTestDataBuilder()
                TestDataBuilder.fixtures.cleanAll()
            } catch (e: Exception) {
                println("WARNING: Could not clean database before test: ${e.message}")
            }
        }

        beforeSpec {
            try {
                TestDataBuilder.fixtures.cleanAll()
            } catch (e: Exception) {
                println("WARNING: Could not clean database at spec start: ${e.message}")
            }
        }
    }

    private fun initializeTestDataBuilder() {
        try {
            TestDataBuilder.fixtures = fixtures
            TestDataBuilder.specHelpers = specHelpers

        } catch (e: Exception) {
            println("ERROR: Could not initialize TestDataBuilder: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getProperties(): Map<String, String> {
        // Start containers lazily via TestContainersManager
        val postgres = TestContainersManager.postgres
        TestContainersManager.mailhog
        val oauth2 = TestContainersManager.oauth2

        val props = mutableMapOf(
            // JDBC via singleton PostgreSQL container
            "datasources.default.url" to "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}",
            "datasources.default.username" to postgres.username,
            "datasources.default.password" to postgres.password,
            "datasources.default.driver-class-name" to "org.postgresql.Driver",
            "datasources.default.db-type" to "postgres",

            // Wire Micronaut mail to Mailhog
            "mail.smtp.host" to "localhost",
            "mail.smtp.port" to TestContainersManager.getSmtpPort().toString(),
        )

        // If MockOAuth2Container exposes issuer/jwksUrl, wire Micronaut google OAuth2 client to it.
        // We use reflection and null checks to avoid crashes when the mock container is not available.
        if (oauth2 != null) {
            try {
                val issuer = oauth2.javaClass.getMethod("getIssuer").invoke(oauth2)?.toString()
                val jwksUrl = oauth2.javaClass.getMethod("getJwksUrl").invoke(oauth2)?.toString()
                if (!issuer.isNullOrBlank() && !jwksUrl.isNullOrBlank()) {
                    props += mapOf(
                        "micronaut.security.oauth2.clients.google.openid.enabled" to "true",
                        "micronaut.security.oauth2.clients.google.issuer" to issuer,
                        "micronaut.security.oauth2.clients.google.client-id" to "test-client-id",
                        "micronaut.security.oauth2.clients.google.client-secret" to "test-client-secret",
                        "micronaut.security.oauth2.clients.google.endpoints.jwks" to jwksUrl,
                    )
                }
            } catch (_: Exception) {
                // If reflection fails (different library version), we simply skip wiring mock oauth2.
            }
        }

        return props
    }

    protected abstract fun StringSpec.registerTests()

    init {
        @Suppress("LeakingThis")
        (this as StringSpec).registerTests()
    }

    /**
     * Helper-метод для POST запросов с JSON телом
     */
    @Suppress("unused")
    protected fun <T> postJson(
        url: String,
        body: Any,
        token: String? = null,
        responseType: Class<T>
    ): T {
        val request = HttpRequest.POST(url, body)
        if (token != null) {
            request.bearerAuth(token)
        }
        return client.toBlocking().retrieve(request, responseType)
    }

    /**
     * Helper-метод для GET запросов
     */
    @Suppress("unused")
    protected fun <T> getJson(
        url: String,
        token: String? = null,
        responseType: Class<T>
    ): T {
        val request = HttpRequest.GET<T>(url)
        if (token != null) {
            request.bearerAuth(token)
        }
        return client.toBlocking().retrieve(request, responseType)
    }

    /**
     * Helper-метод для GET запросов с типом Argument (для списков)
     */
    @Suppress("unused")
    protected fun <T> getJsonList(
        url: String,
        token: String? = null,
        listType: Argument<T>
    ): T {
        val request = HttpRequest.GET<T>(url)
        if (token != null) {
            request.bearerAuth(token)
        }
        return client.toBlocking().retrieve(request, listType)
    }

    /**
     * Helper-метод для DELETE запросов
     */
    @Suppress("unused")
    protected fun deleteJson(
        url: String,
        token: String? = null
    ) {
        // Use Any for request body generic and pass explicit Unit response type to avoid inference issues
        val request = HttpRequest.DELETE<Any>(url)
        if (token != null) {
            request.bearerAuth(token)
        }
        client.toBlocking().exchange(request, Unit::class.java)
    }
}
