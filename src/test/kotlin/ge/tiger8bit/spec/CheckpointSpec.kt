package ge.tiger8bit.spec

import ge.tiger8bit.dto.CheckpointResponse
import ge.tiger8bit.dto.CreateCheckpointRequest
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.TestData.Emails
import ge.tiger8bit.spec.common.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@MicronautTest(transactional = false)
class CheckpointSpec : BaseApiSpec() {

    override fun StringSpec.registerTests() {
        "BOSS can create checkpoint via admin API" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val request = CreateCheckpointRequest(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-TEST",
                geoLat = BigDecimal("41.7151377"),
                geoLon = BigDecimal("44.8270903"),
                radiusM = BigDecimal("50.00")
            )

            val response = client.toBlocking()
                .retrieve(
                    HttpRequest.POST("/api/admin/checkpoints", request).withAuth(bossToken),
                    CheckpointResponse::class.java
                )

            response.code shouldBe request.code
            response.id shouldNotBe null
        }

        "BOSS can list checkpoints for a site" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val request = CreateCheckpointRequest(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-LIST"
            )

            client.toBlocking()
                .retrieve(
                    HttpRequest.POST("/api/admin/checkpoints", request).withAuth(bossToken),
                    CheckpointResponse::class.java
                )

            val list = client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/admin/checkpoints?siteId=${site.id}").withAuth(bossToken),
                Array<CheckpointResponse>::class.java
            ).toList()

            list.isNotEmpty() shouldBe true
            list.any { it.code == request.code } shouldBe true
        }

        "WORKER cannot create checkpoint (forbidden)" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            val request = CreateCheckpointRequest(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-FORBIDDEN"
            )

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking()
                    .retrieve(
                        HttpRequest.POST("/api/admin/checkpoints", request).withAuth(workerToken),
                        CheckpointResponse::class.java
                    )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }

        "POST /api/admin/checkpoints without token returns 401" {
            val (org, site) = fixtures.seedOrgAndSite()

            val request = CreateCheckpointRequest(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-UNAUTHORIZED"
            )

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking()
                    .retrieve(
                        HttpRequest.POST("/api/admin/checkpoints", request),
                        CheckpointResponse::class.java
                    )
            }

            exception.status shouldBe HttpStatus.UNAUTHORIZED
        }
    }
}
