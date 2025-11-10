package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.dto.CreateSiteRequest
import ge.tiger8bit.dto.SiteResponse
import ge.tiger8bit.dto.UpdateSiteRequest
import ge.tiger8bit.repository.OrganizationRepository
import ge.tiger8bit.repository.SiteRepository
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
class SiteSpec : StringSpec() {
    @Inject
    lateinit var organizationRepository: OrganizationRepository

    @Inject
    lateinit var siteRepository: SiteRepository

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    private val bossToken = TestAuth.generateBossToken()
    private val workerToken = TestAuth.generateWorkerToken()

    init {
        "BOSS can create site" {
            val (org, _) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

            val request = CreateSiteRequest(
                organizationId = org.id!!,
                name = "New Site ${java.util.UUID.randomUUID()}"
            )

            val response = client.toBlocking()
                .retrieve(
                    HttpRequest.POST("/api/sites", request).withAuth(bossToken),
                    SiteResponse::class.java
                )

            response.name shouldBe request.name
            response.organizationId shouldBe org.id
            response.id shouldNotBe null
        }

        "BOSS can list sites by organization" {
            val (org, site1) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

            val site2Request = CreateSiteRequest(
                organizationId = org.id!!,
                name = "Site 2 ${java.util.UUID.randomUUID()}"
            )
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/sites", site2Request).withAuth(bossToken),
                SiteResponse::class.java
            )

            val list = client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/sites?organizationId=${org.id}").withAuth(bossToken),
                Array<SiteResponse>::class.java
            ).toList()

            list.size shouldBeGreaterThanOrEqualTo 2
            list.all { it.organizationId == org.id } shouldBe true
            list.any { it.id == site1.id } shouldBe true
        }

        "BOSS can get site by id" {
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

            val response = client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/sites/${site.id}").withAuth(bossToken),
                SiteResponse::class.java
            )

            response.id shouldBe site.id
            response.name shouldBe site.name
            response.organizationId shouldBe org.id
        }

        "BOSS can update site" {
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

            val updateRequest = UpdateSiteRequest(name = "Updated Site ${java.util.UUID.randomUUID()}")
            val updated = client.toBlocking().retrieve(
                HttpRequest.PUT("/api/sites/${site.id}", updateRequest).withAuth(bossToken),
                SiteResponse::class.java
            )

            updated.id shouldBe site.id
            updated.name shouldBe updateRequest.name
            updated.organizationId shouldBe org.id
        }

        "BOSS can delete site" {
            val (org, _) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

            val createRequest = CreateSiteRequest(
                organizationId = org.id!!,
                name = "Site to Delete ${java.util.UUID.randomUUID()}"
            )
            val created = client.toBlocking().retrieve(
                HttpRequest.POST("/api/sites", createRequest).withAuth(bossToken),
                SiteResponse::class.java
            )

            val response = client.toBlocking().retrieve(
                HttpRequest.DELETE<Any>("/api/sites/${created.id}").withAuth(bossToken),
                Map::class.java
            )

            response["deleted"] shouldBe true
            response["id"] shouldBe created.id.toString()
        }

        "WORKER cannot access site endpoints (forbidden)" {
            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.GET<Any>("/api/sites?organizationId=${java.util.UUID.randomUUID()}").withAuth(workerToken),
                    Array<SiteResponse>::class.java
                )
            }
            exception.status shouldBe HttpStatus.FORBIDDEN
        }

        "APP_OWNER cannot access site endpoints (forbidden)" {
            val appOwnerToken = TestAuth.generateAppOwnerToken()
            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.GET<Any>("/api/sites?organizationId=${java.util.UUID.randomUUID()}").withAuth(appOwnerToken),
                    Array<SiteResponse>::class.java
                )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }
    }
}

