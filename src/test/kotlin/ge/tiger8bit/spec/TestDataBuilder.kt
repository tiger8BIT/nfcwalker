package ge.tiger8bit.spec

import ge.tiger8bit.TestFixtures
import java.util.*

object TestDataBuilder {
    private val fixturesHolder = ThreadLocal<TestFixtures>()
    private val specHelpersHolder = ThreadLocal<SpecHelpers>()

    var fixtures: TestFixtures
        get() = fixturesHolder.get()
            ?: error("TestFixtures not initialized. Call initializeTestDataBuilder() in beforeTest")
        set(value) = fixturesHolder.set(value)

    var specHelpers: SpecHelpers?
        get() = specHelpersHolder.get()
        set(value) = if (value != null) specHelpersHolder.set(value) else specHelpersHolder.remove()

    private fun requireSpecHelpers() = specHelpers
        ?: error("SpecHelpers not initialized. Inject specHelpers and set TestDataBuilder.specHelpers")

    fun clear() {
        fixturesHolder.remove()
        specHelpersHolder.remove()
    }

    fun orgAndSite() = fixtures.seedOrgAndSite()

    fun bossToken(orgId: UUID, email: String = "boss@test-${System.currentTimeMillis()}.com") =
        requireSpecHelpers().createBossToken(orgId, email = email)

    fun workerToken(orgId: UUID, email: String = "worker@test-${System.currentTimeMillis()}.com") =
        requireSpecHelpers().createWorkerToken(orgId, email = email)

    fun appOwnerToken(email: String = "owner@test-${System.currentTimeMillis()}.com") =
        requireSpecHelpers().createAppOwnerToken(email = email)
}
