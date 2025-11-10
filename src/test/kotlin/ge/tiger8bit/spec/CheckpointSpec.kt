package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.dto.CheckpointResponse
import ge.tiger8bit.dto.CreateCheckpointRequest
import ge.tiger8bit.repository.OrganizationRepository
import ge.tiger8bit.repository.SiteRepository
import ge.tiger8bit.withAuth
import io.kotest.core.spec.style.StringSpec
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
import java.math.BigDecimal

@MicronautTest(transactional = false)
class CheckpointSpec : StringSpec() {
    @Inject
    lateinit var organizationRepository: OrganizationRepository

    @Inject
    lateinit var siteRepository: SiteRepository

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    private val bossToken = TestAuth.generateBossToken()

    init {
        "BOSS can create checkpoint via admin API" {
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
            val request = CreateCheckpointRequest(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-${java.util.UUID.randomUUID()}",
                geoLat = BigDecimal("41.7151377"),
                geoLon = BigDecimal("44.8270903"),
                radiusM = BigDecimal("50.00")
            )
            val response = client.toBlocking()
                .retrieve(HttpRequest.POST("/api/admin/checkpoints", request).withAuth(bossToken), CheckpointResponse::class.java)
            response.code shouldBe request.code
            response.id shouldNotBe null
        }

        "BOSS can list checkpoints by site" {
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
            val request = CreateCheckpointRequest(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-${java.util.UUID.randomUUID()}"
            )
            client.toBlocking()
                .retrieve(HttpRequest.POST("/api/admin/checkpoints", request).withAuth(bossToken), CheckpointResponse::class.java)
            val list = client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/admin/checkpoints?siteId=${site.id}").withAuth(bossToken),
                Array<CheckpointResponse>::class.java
            ).toList()
            list.size shouldBe 1
            list[0].code shouldBe request.code
        }

        "WORKER cannot create checkpoint (forbidden)" {
            val workerToken = TestAuth.generateWorkerToken()
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
            val request = CreateCheckpointRequest(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-${java.util.UUID.randomUUID()}"
            )
            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST("/api/admin/checkpoints", request).withAuth(workerToken),
                    CheckpointResponse::class.java
                )
            }
            exception.status shouldBe HttpStatus.FORBIDDEN
        }
    }
}

