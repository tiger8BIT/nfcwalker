package ge.tiger8bit.spec

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class HealthSpec : StringSpec() {
    @Inject
    lateinit var application: EmbeddedApplication<*>

    init {
        "server is running" {
            application.isRunning.shouldBeTrue()
        }
    }
}
