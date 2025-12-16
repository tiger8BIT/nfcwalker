package ge.tiger8bit.spec

import ge.tiger8bit.domain.Role
import ge.tiger8bit.dto.AcceptInvitationRequest
import ge.tiger8bit.dto.CreateInvitationRequest
import ge.tiger8bit.dto.InvitationResponse
import ge.tiger8bit.repository.InvitationRepository
import ge.tiger8bit.repository.UserRoleRepository
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.MailhogHelper
import ge.tiger8bit.spec.common.TestAuth
import ge.tiger8bit.spec.common.TestData.Emails
import ge.tiger8bit.spec.common.TestData.Orgs
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class AcceptInvitationFlowSpec : BaseApiSpec() {

    @Inject
    private lateinit var userRoleRepository: UserRoleRepository

    @Inject
    private lateinit var invitationRepository: InvitationRepository

    override fun StringSpec.registerTests() {
        "user can accept invitation and gets role" {
            val org = fixtures.createOrganization(Orgs.ACCEPT)
            val inviteEmail = Emails.unique("worker-accept")
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val invite = postJson(
                "/api/invitations",
                CreateInvitationRequest(inviteEmail, org.id!!, Role.ROLE_WORKER),
                bossToken,
                InvitationResponse::class.java
            )

            val email = MailhogHelper.waitForMessage(inviteEmail)
            MailhogHelper.assertMessageSentTo(email, inviteEmail)

            val invitationEntity = invitationRepository.findById(invite.id).orElseThrow()
            val worker = fixtures.createUser(email = invite.email)
            val workerToken = TestAuth.generateWorkerToken(worker.id!!.toString())

            val request = HttpRequest.POST("/auth/invite/accept", AcceptInvitationRequest(invitationEntity.token))
                .bearerAuth(workerToken)
            val response = client.toBlocking().retrieve(request, Map::class.java)

            response["status"] shouldBe "accepted"
            userRoleRepository.findByIdUserId(worker.id!!)
                .any { it.role == Role.ROLE_WORKER && it.id.organizationId == org.id } shouldBe true
            invitationRepository.findById(invite.id).orElseThrow().status shouldBe "accepted"
        }
    }
}
