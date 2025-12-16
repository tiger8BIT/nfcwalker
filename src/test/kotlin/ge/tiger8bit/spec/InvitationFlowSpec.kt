package ge.tiger8bit.spec

import ge.tiger8bit.domain.Role
import ge.tiger8bit.dto.CreateInvitationRequest
import ge.tiger8bit.dto.InvitationResponse
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
            val org = fixtures.createOrganization("Test Org")
            val (appOwnerToken, _) = specHelpers.createAppOwnerTokenForOrg(org.id!!, "appowner@invite.test")

            val request = CreateInvitationRequest(
                email = "boss@test.com",
                organizationId = org.id!!,
                role = Role.ROLE_BOSS
            )

            val response = postJson(
                "/api/invitations",
                request,
                appOwnerToken,
                InvitationResponse::class.java
            )

            response.email shouldBe "boss@test.com"
            response.role shouldBe Role.ROLE_BOSS
            response.status shouldBe "pending"
        }

        "BOSS can invite WORKER" {
            val org = fixtures.createOrganization("Test Org")
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, "boss@invite.test")

            val request = CreateInvitationRequest(
                email = "worker@test.com",
                organizationId = org.id!!,
                role = Role.ROLE_WORKER
            )

            val response = postJson(
                "/api/invitations",
                request,
                bossToken,
                InvitationResponse::class.java
            )

            response.email shouldBe "worker@test.com"
            response.role shouldBe Role.ROLE_WORKER
            response.status shouldBe "pending"
        }

        "BOSS cannot invite BOSS (role hierarchy violation)" {
            val org = fixtures.createOrganization("Test Org")
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, "boss1@invite.test")

            val request = CreateInvitationRequest(
                email = "boss2@test.com",
                organizationId = org.id!!,
                role = Role.ROLE_BOSS
            )

            assertThrows<HttpClientResponseException> {
                postJson(
                    "/api/invitations",
                    request,
                    bossToken,
                    InvitationResponse::class.java
                )
            }.status shouldBe HttpStatus.FORBIDDEN
        }

        "WORKER cannot invite anyone" {
            val org = fixtures.createOrganization("Test Org")
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, "worker@invite.test")

            val request = CreateInvitationRequest(
                email = "newworker@test.com",
                organizationId = org.id!!,
                role = Role.ROLE_WORKER
            )

            assertThrows<HttpClientResponseException> {
                postJson(
                    "/api/invitations",
                    request,
                    workerToken,
                    InvitationResponse::class.java
                )
            }.status shouldBe HttpStatus.FORBIDDEN
        }

        "LIST invitations by organization" {
            val org = fixtures.createOrganization("Test Org")
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, "boss@list.test")

            val invite1 = CreateInvitationRequest(
                email = "worker1@test.com",
                organizationId = org.id!!,
                role = Role.ROLE_WORKER
            )
            val invite2 = CreateInvitationRequest(
                email = "worker2@test.com",
                organizationId = org.id!!,
                role = Role.ROLE_WORKER
            )

            postJson("/api/invitations", invite1, bossToken, InvitationResponse::class.java)
            postJson("/api/invitations", invite2, bossToken, InvitationResponse::class.java)

            val url = "/api/invitations?organizationId=${org.id}"
            val request = HttpRequest.GET<List<InvitationResponse>>(url)
                .bearerAuth(bossToken)
            val responses = client.toBlocking().retrieve(
                request,
                Argument.listOf(InvitationResponse::class.java)
            )

            responses.size shouldBe 2
            responses[0].email shouldBe "worker1@test.com"
            responses[1].email shouldBe "worker2@test.com"
        }

        "CANCEL pending invitation" {
            val org = fixtures.createOrganization("Test Org")
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, "boss@cancel.test")

            val inviteRequest = CreateInvitationRequest(
                email = "worker@test.com",
                organizationId = org.id!!,
                role = Role.ROLE_WORKER
            )
            val inviteResponse = postJson(
                "/api/invitations",
                inviteRequest,
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
