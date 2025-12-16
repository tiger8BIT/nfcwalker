package ge.tiger8bit.spec

import ge.tiger8bit.domain.Role
import ge.tiger8bit.spec.common.TestAuth
import ge.tiger8bit.spec.common.TestData
import ge.tiger8bit.spec.common.TestFixtures
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.*

@Singleton
class SpecHelpers @Inject constructor(
    private val fixtures: TestFixtures,
) {

    fun createBossToken(
        orgId: UUID,
        email: String = TestData.Emails.BOSS
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
        email: String = TestData.Emails.WORKER
    ): Pair<String, UUID> {
        val (user, _) = fixtures.createUserWithRole(
            orgId,
            Role.ROLE_WORKER,
            email = email
        )
        return TestAuth.generateWorkerToken(user.id.toString()) to user.id!!
    }

    fun createAppOwnerToken(
        email: String = TestData.Emails.APP_OWNER
    ): Pair<String, UUID> {
        val org = fixtures.createOrganization(TestData.Orgs.unique("AppOwner"))
        val (user, _) = fixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_APP_OWNER,
            email = email
        )
        return TestAuth.generateAppOwnerToken(user.id.toString()) to user.id!!
    }

    fun createAppOwnerTokenForOrg(
        orgId: UUID,
        email: String = TestData.Emails.APP_OWNER
    ): Pair<String, UUID> {
        val (user, _) = fixtures.createUserWithRole(
            orgId,
            Role.ROLE_APP_OWNER,
            email = email
        )
        return TestAuth.generateAppOwnerToken(user.id.toString()) to user.id!!
    }
}
