package ge.tiger8bit

// This test helper used to start a Testcontainer and provide JDBC properties for tests.
// It's kept here for reference but disabled because we rely on Micronaut Test Resources
// configured via `src/test/resources/application-test.yml`.

import io.micronaut.test.support.TestPropertyProvider

object TestContainersProvider : TestPropertyProvider {
    override fun getProperties(): Map<String, String> {
        // Start containers lazily via TestContainersManager
        TestContainersManager.mailhog
        val oauth2 = TestContainersManager.oauth2

        return mapOf(
            // JDBC via Testcontainers JDBC URL for Postgres
            "datasources.default.url" to "jdbc:tc:postgresql:15:///testdb",
            "datasources.default.username" to "test",
            "datasources.default.password" to "test",
            "datasources.default.driver-class-name" to "org.testcontainers.jdbc.ContainerDatabaseDriver",
            "datasources.default.db-type" to "postgres",

            // Wire Micronaut mail to Mailhog
            "mail.smtp.host" to "localhost",
            "mail.smtp.port" to TestContainersManager.getSmtpPort().toString(),

            // Wire Micronaut security JWT to issuer from mock OAuth2 (for resource-server style tests)
            // NOTE: adjust property names and usage when you start consuming oauth2 for JWTs.
            // For now we just ensure the container is up and can be wired later.
        )
    }
}
