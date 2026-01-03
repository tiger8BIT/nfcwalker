package ge.tiger8bit.spec

import ge.tiger8bit.domain.Role
import ge.tiger8bit.domain.UserRole
import ge.tiger8bit.dto.AuthMeResponse
import ge.tiger8bit.repository.UserRoleRepository
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.TestAuth
import ge.tiger8bit.spec.common.TestData.Emails
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class AuthMeSpec : BaseApiSpec() {

    @Inject
    lateinit var userRoleRepository: UserRoleRepository

    override fun StringSpec.registerTests() {
        "GET /auth/me returns user details and roles" {
            val org1 = fixtures.createOrganization("Org 1")
            val org2 = fixtures.createOrganization("Org 2")

            val email = Emails.unique("me-test")
            val name = "Me Tester"
            val user = fixtures.createUser(email = email, name = name)

            // Manually link roles to the same user
            userRoleRepository.save(UserRole(user.id!!, org1.id!!, Role.ROLE_BOSS))
            userRoleRepository.save(UserRole(user.id!!, org2.id!!, Role.ROLE_WORKER))

            val token = TestAuth.generateCustomToken(user.id.toString(), listOf("ROLE_BOSS"))

            val response = getJson("/auth/me", token, AuthMeResponse::class.java)

            response.user.id shouldBe user.id
            response.user.email shouldBe email
            response.user.name shouldBe name
            response.roles.size shouldBe 2
            response.roles[org1.id.toString()] shouldBe Role.ROLE_BOSS
            response.roles[org2.id.toString()] shouldBe Role.ROLE_WORKER
        }

        "GET /auth/me returns empty roles if user has none" {
            val email = Emails.unique("no-roles")
            val user = fixtures.createUser(email = email)
            val token = TestAuth.generateCustomToken(user.id.toString(), listOf("ROLE_WORKER"))

            val response = getJson("/auth/me", token, AuthMeResponse::class.java)

            response.user.id shouldBe user.id
            response.roles shouldBe emptyMap()
        }
    }
}
