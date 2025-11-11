package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.Role
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
import java.util.*

@MicronautTest(transactional = false)
class OrganizationSpec(
    @Inject @Client("/") val client: HttpClient,
    @Inject val beanContext: io.micronaut.context.BeanContext
) : StringSpec({
    TestFixtures.init(beanContext)

    val (testOrg, _) = TestFixtures.seedOrgAndSite()

    val (appOwnerUser, _) = TestFixtures.createUserWithRole(
        testOrg.id!!,
        Role.ROLE_APP_OWNER,
        email = "app-owner@org-test.com"
    )
    val appOwnerToken = TestAuth.generateAppOwnerToken(appOwnerUser.id.toString())

    val (bossUser, _) = TestFixtures.createUserWithRole(
        testOrg.id!!,
        Role.ROLE_BOSS,
        email = "boss@org-test.com"
    )
    val bossToken = TestAuth.generateBossToken(bossUser.id.toString())

    "APP_OWNER can create organization" {
        val request = CreateOrganizationRequest(
            name = "Test Security Company ${UUID.randomUUID()}"
        )
        val response = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/api/organizations", request).withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )
        response.name shouldBe request.name
        response.id shouldNotBe null
    }

    "APP_OWNER can list all organizations" {
        val org1 = CreateOrganizationRequest(name = "Org 1 ${UUID.randomUUID()}")
        val org2 = CreateOrganizationRequest(name = "Org 2 ${UUID.randomUUID()}")

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
        val exception = assertThrows<HttpClientResponseException> {
            client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/organizations").withAuth(bossToken),
                Array<OrganizationResponse>::class.java
            )
        }
        exception.status shouldBe HttpStatus.FORBIDDEN
    }
})

