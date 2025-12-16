package ge.tiger8bit.spec

import ge.tiger8bit.dto.CreateOrganizationRequest
import ge.tiger8bit.dto.OrganizationResponse
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.TestData.Emails
import ge.tiger8bit.spec.common.TestData.Orgs
import ge.tiger8bit.spec.common.TestDataBuilder
import ge.tiger8bit.spec.common.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.junit.jupiter.api.assertThrows

@MicronautTest(transactional = false)
class OrganizationSpec : BaseApiSpec() {

    override fun StringSpec.registerTests() {
        "APP_OWNER can create organization" {
            val (token, _) = TestDataBuilder.appOwnerToken(Emails.unique("owner"))

            val response = client.toBlocking().retrieve(
                HttpRequest.POST("/api/admin/organizations", CreateOrganizationRequest(Orgs.unique("Create"))).withAuth(token),
                OrganizationResponse::class.java
            )

            response.id shouldNotBe null
        }

        "APP_OWNER can list all organizations" {
            val (appOwnerToken, _) = TestDataBuilder.appOwnerToken(Emails.unique("owner"))
            val org1Name = Orgs.unique("List1")
            val org2Name = Orgs.unique("List2")

            client.toBlocking().retrieve(
                HttpRequest.POST("/api/organizations", CreateOrganizationRequest(org1Name)).withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/organizations", CreateOrganizationRequest(org2Name)).withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )

            val list = client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/organizations").withAuth(appOwnerToken),
                Array<OrganizationResponse>::class.java
            ).toList()

            list.size shouldBeGreaterThanOrEqualTo 2
            list.any { it.name == org1Name } shouldBe true
            list.any { it.name == org2Name } shouldBe true
        }

        "BOSS cannot access organization endpoints (forbidden)" {
            val (org, _) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.GET<Any>("/api/organizations").withAuth(bossToken),
                    Array<OrganizationResponse>::class.java
                )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }

        "BOSS cannot create organization (forbidden)" {
            val (org, _) = TestDataBuilder.orgAndSite()
            val (bossToken, _) = TestDataBuilder.bossToken(org.id!!, email = Emails.unique("boss"))

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST("/api/admin/organizations", CreateOrganizationRequest(Orgs.FORBIDDEN)).withAuth(bossToken),
                    OrganizationResponse::class.java
                )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }
    }
}
