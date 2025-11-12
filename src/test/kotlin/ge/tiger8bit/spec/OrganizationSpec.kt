package ge.tiger8bit.spec

import ge.tiger8bit.TestFixtures
import ge.tiger8bit.dto.CreateOrganizationRequest
import ge.tiger8bit.dto.OrganizationResponse
import ge.tiger8bit.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.assertThrows

@MicronautTest(transactional = false)
class OrganizationSpec @Inject constructor(
    @Client("/") client: HttpClient,
    beanContext: io.micronaut.context.BeanContext
) : BaseApiSpec(client, beanContext) {
    override fun StringSpec.registerTests() {
        "APP_OWNER can create organization" {
            val (appOwnerToken, _) = createAppOwnerToken(email = "app-owner@org-create.com")
            val request = CreateOrganizationRequest(name = "Test Security Company")

            val response = client.toBlocking()
                .retrieve(
                    HttpRequest.POST("/api/organizations", request).withAuth(appOwnerToken),
                    OrganizationResponse::class.java
                )

            response.name shouldBe request.name
            response.id shouldNotBe null
        }

        "APP_OWNER can list all organizations" {
            val (appOwnerToken, _) = createAppOwnerToken(email = "app-owner@org-list.com")

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
            val (org, _) = TestFixtures.seedOrgAndSite()
            val (bossToken, _) = createBossToken(org.id!!, email = "boss@org-forbidden.com")

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.GET<Any>("/api/organizations").withAuth(bossToken),
                    Array<OrganizationResponse>::class.java
                )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }
    }
}
