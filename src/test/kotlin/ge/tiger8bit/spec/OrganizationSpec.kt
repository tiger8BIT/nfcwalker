package ge.tiger8bit.spec

import ge.tiger8bit.dto.CreateOrganizationRequest
import ge.tiger8bit.dto.OrganizationResponse
import ge.tiger8bit.withAuth
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
            val (token, _) = TestDataBuilder.appOwnerToken("owner+create@org.com")

            val request = CreateOrganizationRequest(
                name = "Org Create"
            )

            val response = client.toBlocking().retrieve(
                HttpRequest.POST("/api/admin/organizations", request).withAuth(token),
                OrganizationResponse::class.java
            )

            response.id shouldNotBe null
            response.name shouldBe "Org Create"
        }

        "APP_OWNER can list all organizations" {
            val (appOwnerToken, _) = TestDataBuilder.appOwnerToken("app-owner@org-list.com")

            val org1 = CreateOrganizationRequest(name = "Org 1")
            val org2 = CreateOrganizationRequest(name = "Org 2")

            client.toBlocking().retrieve(
                HttpRequest.POST("/api/organizations", org1).withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/organizations", org2).withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )

            val list = client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/organizations").withAuth(appOwnerToken),
                Array<OrganizationResponse>::class.java
            ).toList()

            list.size shouldBeGreaterThanOrEqualTo 2
            list.any { it.name == org1.name } shouldBe true
            list.any { it.name == org2.name } shouldBe true
        }

        "BOSS cannot access organization endpoints (forbidden)" {
            val (org, _) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = "boss@org-forbidden.com")

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
            val (bossToken, _) = TestDataBuilder.bossToken(org.id!!, email = "boss+org-forbidden@org.com")

            val request = CreateOrganizationRequest(
                name = "Forbidden Org"
            )

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST("/api/admin/organizations", request).withAuth(bossToken),
                    OrganizationResponse::class.java
                )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }
    }
}
