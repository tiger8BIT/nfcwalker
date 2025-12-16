package ge.tiger8bit.spec

import ge.tiger8bit.dto.CreateSiteRequest
import ge.tiger8bit.dto.SiteResponse
import ge.tiger8bit.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.junit.jupiter.api.assertThrows

@MicronautTest(transactional = false)
class SiteSpec : BaseApiSpec() {

    override fun StringSpec.registerTests() {
        "BOSS can create a site" {
            val (org, _) = TestDataBuilder.orgAndSite()
            val bossToken = TestDataBuilder.bossToken(org.id!!, email = "boss@site-create.com").first

            val request = CreateSiteRequest(
                organizationId = org.id!!,
                name = "New Site"
            )

            val response = client.toBlocking().retrieve(
                HttpRequest.POST("/api/sites", request).withAuth(bossToken),
                SiteResponse::class.java
            )

            response.id shouldNotBe null
            response.name shouldBe "New Site"
        }

        "BOSS can list sites" {
            val (org, _) = TestDataBuilder.orgAndSite()
            val bossToken = TestDataBuilder.bossToken(org.id!!, email = "boss@site-list.com").first

            val request = CreateSiteRequest(
                organizationId = org.id!!,
                name = "List Site"
            )

            client.toBlocking().retrieve(
                HttpRequest.POST("/api/sites", request).withAuth(bossToken),
                SiteResponse::class.java
            )

            val sites = client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/sites?organizationId=${org.id}").withAuth(bossToken),
                Argument.listOf(SiteResponse::class.java)
            )

            sites.size shouldBeGreaterThan 0
        }

        "BOSS can update a site" {
            val (org, site) = TestDataBuilder.orgAndSite()
            val bossToken = TestDataBuilder.bossToken(org.id!!, email = "boss@site-update.com").first

            val request = CreateSiteRequest(
                organizationId = org.id!!,
                name = "Updated Site"
            )

            val response = client.toBlocking().retrieve(
                HttpRequest.PUT("/api/sites/${site.id}", request).withAuth(bossToken),
                SiteResponse::class.java
            )

            response.id shouldBe site.id
            response.name shouldBe "Updated Site"
        }

        "BOSS can delete a site" {
            val (org, site) = TestDataBuilder.orgAndSite()
            val bossToken = TestDataBuilder.bossToken(org.id!!, email = "boss@site-delete.com").first

            val response = client.toBlocking().exchange(
                HttpRequest.DELETE<Any>("/api/sites/${site.id}").withAuth(bossToken),
                Map::class.java
            )

            response.status shouldBe HttpStatus.OK
        }

        "WORKER cannot manage sites (forbidden)" {
            val (org, _) = TestDataBuilder.orgAndSite()
            val workerToken = TestDataBuilder.workerToken(org.id!!, email = "worker@site-forbidden.com").first

            val request = CreateSiteRequest(
                organizationId = org.id!!,
                name = "Forbidden Site"
            )

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST("/api/sites", request).withAuth(workerToken),
                    SiteResponse::class.java
                )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }
    }
}
