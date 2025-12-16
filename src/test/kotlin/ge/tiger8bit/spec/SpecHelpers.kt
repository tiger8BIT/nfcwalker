package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.Role
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.*

@Singleton
class SpecHelpers @Inject constructor(
    private val fixtures: TestFixtures,
) {

    fun createBossToken(
        orgId: UUID,
        email: String = "boss@test.com"
    ): Pair<String, UUID> {
        val (user, _) = fixtures.createUserWithRole(
            orgId,
            Role.ROLE_BOSS,
            email = email
        )
        return TestAuth.generateBossToken(user.id.toString()) to user.id!!
    }

    fun createWorkerToken(
        orgId: UUID,
        email: String = "worker@test.com"
    ): Pair<String, UUID> {
        val (user, _) = fixtures.createUserWithRole(
            orgId,
            Role.ROLE_WORKER,
            email = email
        )
        return TestAuth.generateWorkerToken(user.id.toString()) to user.id!!
    }

    fun createAppOwnerToken(
        email: String = "app-owner@test.com"
    ): Pair<String, UUID> {
        val org = fixtures.createOrganization("AppOwner Org ${UUID.randomUUID()}")
        val (user, _) = fixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_APP_OWNER,
            email = email
        )
        return TestAuth.generateAppOwnerToken(user.id.toString()) to user.id!!
    }

    fun createAppOwnerTokenForOrg(
        orgId: UUID,
        email: String = "app-owner@test.com"
    ): Pair<String, UUID> {
        val (user, _) = fixtures.createUserWithRole(
            orgId,
            Role.ROLE_APP_OWNER,
            email = email
        )
        return TestAuth.generateAppOwnerToken(user.id.toString()) to user.id!!
    }
}
