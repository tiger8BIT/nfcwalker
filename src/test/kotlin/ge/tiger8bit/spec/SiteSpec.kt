package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.Role
import ge.tiger8bit.dto.CreateSiteRequest
import ge.tiger8bit.dto.SiteResponse
import ge.tiger8bit.dto.UpdateSiteRequest
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
class SiteSpec(
    @Inject @Client("/") val client: HttpClient,
    @Inject val beanContext: io.micronaut.context.BeanContext
) : StringSpec({
    TestFixtures.init(beanContext)

    "BOSS can create site" {
        val (org, _) = TestFixtures.seedOrgAndSite()
        val (bossUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_BOSS,
            email = "boss@site-create.com"
        )
        val bossToken = TestAuth.generateBossToken(bossUser.id.toString())

        val request = CreateSiteRequest(
            organizationId = org.id!!,
            name = "New Site"
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
        val (org, _) = TestFixtures.seedOrgAndSite()
        val (bossUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_BOSS,
            email = "boss@site-list.com"
        )
        val bossToken = TestAuth.generateBossToken(bossUser.id.toString())

        val site2 = CreateSiteRequest(
            organizationId = org.id!!,
            name = "Site 2"
        )

        client.toBlocking().retrieve(
            HttpRequest.POST("/api/sites", site2).withAuth(bossToken),
            SiteResponse::class.java
        )

        val list = client.toBlocking().retrieve(
            HttpRequest.GET<Any>("/api/sites?organizationId=${org.id}").withAuth(bossToken),
            Array<SiteResponse>::class.java
        ).toList()

        list.size shouldBeGreaterThanOrEqualTo 1
        list.any { it.name == site2.name } shouldBe true
    }

    "BOSS can update site" {
        val (org, site) = TestFixtures.seedOrgAndSite()
        val (bossUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_BOSS,
            email = "boss@site-update.com"
        )
        val bossToken = TestAuth.generateBossToken(bossUser.id.toString())

        val updateRequest = UpdateSiteRequest(name = "Updated Site")

        val response = client.toBlocking()
            .retrieve(
                HttpRequest.PUT("/api/sites/${site.id}", updateRequest).withAuth(bossToken),
                SiteResponse::class.java
            )

        response.name shouldBe updateRequest.name
        response.id shouldBe site.id
    }

    "BOSS can delete site" {
        val (org, site) = TestFixtures.seedOrgAndSite()
        val (bossUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_BOSS,
            email = "boss@site-delete.com"
        )
        val bossToken = TestAuth.generateBossToken(bossUser.id.toString())

        val response = client.toBlocking()
            .retrieve(
                HttpRequest.DELETE<Any>("/api/sites/${site.id}").withAuth(bossToken),
                Map::class.java
            )

        response["deleted"] shouldBe true

        assertThrows<HttpClientResponseException> {
            client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/sites/${site.id}").withAuth(bossToken),
                SiteResponse::class.java
            )
        }
    }

    "WORKER cannot create site (forbidden)" {
        val (org, _) = TestFixtures.seedOrgAndSite()
        val (workerUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_WORKER,
            email = "worker@site-forbidden.com"
        )
        val workerToken = TestAuth.generateWorkerToken(workerUser.id.toString())

        val request = CreateSiteRequest(
            organizationId = org.id!!,
            name = "Unauthorized Site"
        )

        val exception = assertThrows<HttpClientResponseException> {
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/sites", request).withAuth(workerToken),
                SiteResponse::class.java
            )
        }

        exception.status shouldBe HttpStatus.FORBIDDEN
    }
})

