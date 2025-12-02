package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.Role
import ge.tiger8bit.dto.AcceptInvitationRequest
import ge.tiger8bit.dto.CreateInvitationRequest
import ge.tiger8bit.dto.InvitationResponse
import ge.tiger8bit.repository.InvitationRepository
import ge.tiger8bit.repository.UserRoleRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class AcceptInvitationFlowSpec @Inject constructor(
    @Client("/") client: HttpClient,
    beanContext: io.micronaut.context.BeanContext,
    private val userRoleRepository: UserRoleRepository,
    private val invitationRepository: InvitationRepository
) : BaseApiSpec(client, beanContext) {

    override fun StringSpec.registerTests() {
        "user can accept invitation and gets role" {
            val org = TestFixtures.createOrganization("Accept Org")
            val (boss, _) = TestFixtures.createUserWithRole(org.id!!, Role.ROLE_BOSS)
            val bossToken = TestAuth.generateBossToken(boss.id!!.toString())

            val inviteReq = CreateInvitationRequest(
                email = "worker-accept@test.com",
                organizationId = org.id!!,
                role = Role.ROLE_WORKER
            )

            val invite = postJson(
                "/api/invitations",
                inviteReq,
                bossToken,
                InvitationResponse::class.java
            )

            // Load actual invitation from DB to get the token
            val invitationEntity = invitationRepository.findById(invite.id).orElseThrow()

            val worker = TestFixtures.createUser(email = invite.email)
            val workerToken = TestAuth.generateWorkerToken(worker.id!!.toString())

            val acceptReq = AcceptInvitationRequest(token = invitationEntity.token)
            val request = HttpRequest.POST("/auth/invite/accept", acceptReq).bearerAuth(workerToken)

            val response = client.toBlocking().retrieve(request, Map::class.java)

            response["status"] shouldBe "accepted"

            val roles = userRoleRepository.findByIdUserId(worker.id!!)
            roles.any { it.role == Role.ROLE_WORKER && it.id.organizationId == org.id } shouldBe true

            val updatedInvitation = invitationRepository.findById(invite.id).orElseThrow()
            updatedInvitation.status shouldBe "accepted"
        }
    }
}
