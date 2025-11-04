package ge.tiger8bit

// This test helper used to start a Testcontainer and provide JDBC properties for tests.
// It's kept here for reference but disabled because we rely on Micronaut Test Resources
// configured via `src/test/resources/application-test.yml`.

import io.micronaut.test.support.TestPropertyProvider

object TestContainersProvider : TestPropertyProvider {
    override fun getProperties(): Map<String, String> {
        // Return empty map - Micronaut Test Resources (configured in application-test.yml)
        // will provide the datasource properties and start the Postgres container.
        return emptyMap()
    }
}
