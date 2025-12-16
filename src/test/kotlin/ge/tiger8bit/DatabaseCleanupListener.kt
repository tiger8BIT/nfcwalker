package ge.tiger8bit

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.micronaut.context.ApplicationContext
import jakarta.inject.Singleton

@Singleton
class DatabaseCleanupListener(
    private val applicationContext: ApplicationContext
) : TestListener {

    override suspend fun beforeEach(testCase: TestCase) {
        try {
            val fixtures = applicationContext.getBean(TestFixtures::class.java)
            fixtures.cleanAll()
        } catch (e: Exception) {
            println("WARNING: Could not clean database before test: ${e.message}")
        }
    }
}

