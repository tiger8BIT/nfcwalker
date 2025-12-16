package ge.tiger8bit.spec

import java.util.*

/**
 * Builder object для инкапсуляции создания тестовых данных
 */
object TestDataBuilder {
    // These are now simple pass-through helpers that expect callers to already be in a MicronautTest with DI.

    lateinit var fixtures: ge.tiger8bit.TestFixtures

    // specHelpers is optional - will be set by concrete test classes
    var specHelpers: SpecHelpers? = null

    private fun requireSpecHelpers(): SpecHelpers {
        return specHelpers ?: throw IllegalStateException(
            "SpecHelpers not initialized. " +
                    "Make sure your test class has @Inject lateinit var specHelpers: SpecHelpers " +
                    "and calls TestDataBuilder.specHelpers = specHelpers in init block"
        )
    }

    /**
     * Создаёт организацию и площадку за один вызов
     */
    fun orgAndSite() = fixtures.seedOrgAndSite()

    /**
     * Создаёт токен для BOSS роли (менеджер организации)
     */
    fun bossToken(
        orgId: UUID,
        email: String = "boss@test-${System.currentTimeMillis()}.com"
    ) = requireSpecHelpers().createBossToken(orgId, email = email)

    /**
     * Создаёт токен для WORKER роли (охранник/патрульный)
     */
    fun workerToken(
        orgId: UUID,
        email: String = "worker@test-${System.currentTimeMillis()}.com"
    ) = requireSpecHelpers().createWorkerToken(orgId, email = email)

    /**
     * Создаёт токен для APP_OWNER роли (суперадмин)
     */
    fun appOwnerToken(
        email: String = "owner@test-${System.currentTimeMillis()}.com"
    ) = requireSpecHelpers().createAppOwnerToken(email = email)
}
