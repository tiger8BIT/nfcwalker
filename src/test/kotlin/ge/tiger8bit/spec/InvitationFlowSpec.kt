package ge.tiger8bit.spec

import ge.tiger8bit.domain.Role
import ge.tiger8bit.dto.CreateInvitationRequest
import ge.tiger8bit.dto.InvitationResponse
import ge.tiger8bit.dto.InvitationStatus
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.MailhogHelper
import ge.tiger8bit.spec.common.TestData.Emails
import ge.tiger8bit.spec.common.TestData.Orgs
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.junit.jupiter.api.assertThrows

@MicronautTest(transactional = false)
class InvitationFlowSpec : BaseApiSpec() {

    override fun StringSpec.registerTests() {
        "APP_OWNER can invite BOSS and invitation created" {
            val org = fixtures.createOrganization(Orgs.DEFAULT)
            val inviteEmail = Emails.unique("boss-invite")
            val (appOwnerToken, _) = specHelpers.createAppOwnerTokenForOrg(org.id!!, Emails.unique("appowner"))

            val request = CreateInvitationRequest(
                email = inviteEmail,
                organizationId = org.id!!,
                role = Role.ROLE_BOSS
            )

            val response = postJson("/api/invitations", request, appOwnerToken, InvitationResponse::class.java)

            response.email shouldBe inviteEmail
            response.role shouldBe Role.ROLE_BOSS
            response.status shouldBe InvitationStatus.PENDING

            val email = MailhogHelper.waitForMessage(inviteEmail)
            MailhogHelper.assertMessageSentTo(email, inviteEmail)
        }

        "BOSS can invite WORKER" {
            val org = fixtures.createOrganization(Orgs.DEFAULT)
            val inviteEmail = Emails.unique("worker-invite")
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, Emails.unique("boss"))

            val request = CreateInvitationRequest(
                email = inviteEmail,
                organizationId = org.id!!,
                role = Role.ROLE_WORKER
            )

            val response = postJson("/api/invitations", request, bossToken, InvitationResponse::class.java)

            response.email shouldBe inviteEmail
            response.role shouldBe Role.ROLE_WORKER
            response.status shouldBe InvitationStatus.PENDING

            val email = MailhogHelper.waitForMessage(inviteEmail)
            MailhogHelper.assertMessageSentTo(email, inviteEmail)
        }

        "BOSS cannot invite BOSS (role hierarchy violation)" {
            val org = fixtures.createOrganization(Orgs.DEFAULT)
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, Emails.unique("boss1"))

            val request = CreateInvitationRequest(
                email = Emails.boss("other"),
                organizationId = org.id!!,
                role = Role.ROLE_BOSS
            )

            assertThrows<HttpClientResponseException> {
                postJson("/api/invitations", request, bossToken, InvitationResponse::class.java)
            }.status shouldBe HttpStatus.FORBIDDEN
        }

        "WORKER cannot invite anyone" {
            val org = fixtures.createOrganization(Orgs.DEFAULT)
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, Emails.unique("worker"))

            val request = CreateInvitationRequest(
                email = Emails.worker("new"),
                organizationId = org.id!!,
                role = Role.ROLE_WORKER
            )

            assertThrows<HttpClientResponseException> {
                postJson("/api/invitations", request, workerToken, InvitationResponse::class.java)
            }.status shouldBe HttpStatus.FORBIDDEN
        }

        "LIST invitations by organization" {
            val org = fixtures.createOrganization(Orgs.DEFAULT)
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, Emails.unique("boss"))
            val worker1Email = Emails.unique("worker-list1")
            val worker2Email = Emails.unique("worker-list2")

            postJson(
                "/api/invitations",
                CreateInvitationRequest(worker1Email, org.id!!, Role.ROLE_WORKER),
                bossToken,
                InvitationResponse::class.java
            )
            postJson(
                "/api/invitations",
                CreateInvitationRequest(worker2Email, org.id!!, Role.ROLE_WORKER),
                bossToken,
                InvitationResponse::class.java
            )

            val request = HttpRequest.GET<List<InvitationResponse>>("/api/invitations?organizationId=${org.id}")
                .bearerAuth(bossToken)
            val responses = client.toBlocking().retrieve(request, Argument.listOf(InvitationResponse::class.java))

            responses.size shouldBe 2
            responses.map { it.email } shouldBe listOf(worker1Email, worker2Email)
        }

        "CANCEL pending invitation" {
            val org = fixtures.createOrganization(Orgs.DEFAULT)
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, Emails.unique("boss"))
            val inviteEmail = Emails.worker("cancel")

            val inviteResponse = postJson(
                "/api/invitations",
                CreateInvitationRequest(inviteEmail, org.id!!, Role.ROLE_WORKER),
                bossToken,
                InvitationResponse::class.java
            )

            val request = HttpRequest.DELETE<Map<String, String>>("/api/invitations/${inviteResponse.id}")
                .bearerAuth(bossToken)
            val response = client.toBlocking().retrieve(request, Map::class.java)

            response["status"] shouldBe "cancelled"
        }
    }
}
