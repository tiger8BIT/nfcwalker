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
import java.sql.DriverManager
import java.util.*

@Suppress("unused")
abstract class BaseApiSpec : StringSpec(), TestPropertyProvider {

    private val schemaName = "test_${UUID.randomUUID().toString().replace("-", "").take(12)}"

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Inject
    lateinit var specHelpers: SpecHelpers

    @Inject
    lateinit var fixtures: TestFixtures

    init {
        beforeTest {
            runCatching {
                initializeTestDataBuilder()
                TestDataBuilder.fixtures.cleanAll()
                MailhogHelper.clearMessages()
            }.onFailure { println("WARNING: Could not clean database: ${it.message}") }
        }

        afterSpec {
            runCatching {
                TestDataBuilder.clear()
                dropSchema()
            }.onFailure { println("WARNING: Could not cleanup after spec: ${it.message}") }
        }
    }

    private fun initializeTestDataBuilder() {
        TestDataBuilder.fixtures = fixtures
        TestDataBuilder.specHelpers = specHelpers
    }

    private fun createSchema() {
        executeOnPostgres("CREATE SCHEMA IF NOT EXISTS $schemaName")
    }

    private fun dropSchema() {
        executeOnPostgres("DROP SCHEMA IF EXISTS $schemaName CASCADE")
    }

    private fun executeOnPostgres(sql: String) {
        val postgres = TestContainersManager.postgres
        DriverManager.getConnection(
            "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}",
            postgres.username,
            postgres.password
        ).use { it.createStatement().execute(sql) }
    }

    private fun buildJdbcUrl(): String {
        val postgres = TestContainersManager.postgres
        return "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}?currentSchema=$schemaName"
    }

    override fun getProperties(): Map<String, String> {
        val postgres = TestContainersManager.postgres
        val mailhog = TestContainersManager.mailhog
        val oauth2 = TestContainersManager.oauth2

        val smtpPort = mailhog.getMappedPort(1025)

        createSchema()

        val props = mutableMapOf(
            "datasources.default.url" to buildJdbcUrl(),
            "datasources.default.username" to postgres.username,
            "datasources.default.password" to postgres.password,
            "datasources.default.driver-class-name" to "org.postgresql.Driver",
            "datasources.default.db-type" to "postgres",
            "flyway.datasources.default.default-schema" to schemaName,
            "flyway.datasources.default.schemas" to schemaName,
            "javamail.properties.mail.smtp.host" to mailhog.host,
            "javamail.properties.mail.smtp.port" to smtpPort.toString(),
        )

        addOAuth2PropsIfAvailable(oauth2, props)

        return props
    }

    private fun addOAuth2PropsIfAvailable(oauth2: Any?, props: MutableMap<String, String>) {
        if (oauth2 == null) return
        runCatching {
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
        }
    }

    protected abstract fun StringSpec.registerTests()

    init {
        @Suppress("LeakingThis")
        (this as StringSpec).registerTests()
    }

    protected fun <T> postJson(url: String, body: Any, token: String? = null, responseType: Class<T>): T {
        val request = HttpRequest.POST(url, body)
        token?.let { request.bearerAuth(it) }
        return client.toBlocking().retrieve(request, responseType)
    }

    protected fun <T> getJson(url: String, token: String? = null, responseType: Class<T>): T {
        val request = HttpRequest.GET<T>(url)
        token?.let { request.bearerAuth(it) }
        return client.toBlocking().retrieve(request, responseType)
    }

    protected fun <T> getJsonList(url: String, token: String? = null, listType: Argument<T>): T {
        val request = HttpRequest.GET<T>(url)
        token?.let { request.bearerAuth(it) }
        return client.toBlocking().retrieve(request, listType)
    }

    protected fun deleteJson(url: String, token: String? = null) {
        val request = HttpRequest.DELETE<Any>(url)
        token?.let { request.bearerAuth(it) }
        client.toBlocking().exchange(request, Unit::class.java)
    }
}
