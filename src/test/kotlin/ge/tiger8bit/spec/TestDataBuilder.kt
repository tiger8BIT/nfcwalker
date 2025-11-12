package ge.tiger8bit.spec

import ge.tiger8bit.TestFixtures
import java.util.*

/**
 * Builder object для инкапсуляции создания тестовых данных
 */
object TestDataBuilder {
    /**
     * Создаёт организацию и площадку за один вызов
     */
    fun orgAndSite() = TestFixtures.seedOrgAndSite()

    /**
     * Создаёт токен для BOSS роли (менеджер организации)
     */
    fun bossToken(
        orgId: UUID,
        email: String = "boss@test-${System.currentTimeMillis()}.com"
    ) = createBossToken(orgId, email = email)

    /**
     * Создаёт токен для WORKER роли (охранник/патрульный)
     */
    fun workerToken(
        orgId: UUID,
        email: String = "worker@test-${System.currentTimeMillis()}.com"
    ) = createWorkerToken(orgId, email = email)

    /**
     * Создаёт токен для APP_OWNER роли (суперадмин)
     */
    fun appOwnerToken(
        email: String = "owner@test-${System.currentTimeMillis()}.com"
    ) = createAppOwnerToken(email = email)
}

