package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.dto.CreateOrganizationRequest
import ge.tiger8bit.dto.OrganizationResponse
import ge.tiger8bit.dto.UpdateOrganizationRequest
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
class OrganizationSpec : StringSpec() {
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    private val appOwnerToken = TestAuth.generateAppOwnerToken()
    private val bossToken = TestAuth.generateBossToken()

    init {
        "APP_OWNER can create organization" {
            val request = CreateOrganizationRequest(
                name = "Test Security Company ${java.util.UUID.randomUUID()}"
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
            // Create a few organizations
            val org1 = CreateOrganizationRequest(name = "Org 1 ${java.util.UUID.randomUUID()}")
            val org2 = CreateOrganizationRequest(name = "Org 2 ${java.util.UUID.randomUUID()}")

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

        "APP_OWNER can get organization by id" {
            val createRequest = CreateOrganizationRequest(name = "Org for Get ${java.util.UUID.randomUUID()}")
            val created = client.toBlocking().retrieve(
                HttpRequest.POST("/api/organizations", createRequest).withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )

            val response = client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/organizations/${created.id}").withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )

            response.id shouldBe created.id
            response.name shouldBe createRequest.name
        }

        "APP_OWNER can update organization" {
            val createRequest = CreateOrganizationRequest(name = "Old Name ${java.util.UUID.randomUUID()}")
            val created = client.toBlocking().retrieve(
                HttpRequest.POST("/api/organizations", createRequest).withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )

            val updateRequest = UpdateOrganizationRequest(name = "New Name ${java.util.UUID.randomUUID()}")
            val updated = client.toBlocking().retrieve(
                HttpRequest.PUT("/api/organizations/${created.id}", updateRequest).withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )

            updated.id shouldBe created.id
            updated.name shouldBe updateRequest.name
        }

        "APP_OWNER can delete organization" {
            val createRequest = CreateOrganizationRequest(name = "To Delete ${java.util.UUID.randomUUID()}")
            val created = client.toBlocking().retrieve(
                HttpRequest.POST("/api/organizations", createRequest).withAuth(appOwnerToken),
                OrganizationResponse::class.java
            )

            val response = client.toBlocking().retrieve(
                HttpRequest.DELETE<Any>("/api/organizations/${created.id}").withAuth(appOwnerToken),
                Map::class.java
            )

            response["deleted"] shouldBe true
            response["id"] shouldBe created.id.toString()

            // Verify it's deleted - should return 404 or null
            assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.GET<Any>("/api/organizations/${created.id}").withAuth(appOwnerToken),
                    OrganizationResponse::class.java
                )
            }
        }

        "BOSS cannot access organization endpoints (forbidden)" {
            val request = CreateOrganizationRequest(name = "Should Fail ${java.util.UUID.randomUUID()}")

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST("/api/organizations", request).withAuth(bossToken),
                    OrganizationResponse::class.java
                )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }

        "WORKER cannot access organization endpoints (forbidden)" {
            val workerToken = TestAuth.generateWorkerToken()

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.GET<Any>("/api/organizations").withAuth(workerToken),
                    Array<OrganizationResponse>::class.java
                )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }
    }
}

