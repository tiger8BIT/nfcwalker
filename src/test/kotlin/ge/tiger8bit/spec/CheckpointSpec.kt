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
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject
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

    private val authToken = TestAuth.generateToken()

    init {
        "create checkpoint via admin API" {
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
                .retrieve(HttpRequest.POST("/api/admin/checkpoints", request).withAuth(authToken), CheckpointResponse::class.java)
            response.code shouldBe request.code
            response.id shouldNotBe null
        }

        "list checkpoints by site" {
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
            val request = CreateCheckpointRequest(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-${java.util.UUID.randomUUID()}"
            )
            client.toBlocking()
                .retrieve(HttpRequest.POST("/api/admin/checkpoints", request).withAuth(authToken), CheckpointResponse::class.java)
            val list = client.toBlocking().retrieve(
                HttpRequest.GET<Any>("/api/admin/checkpoints?siteId=${site.id}").withAuth(authToken),
                Array<CheckpointResponse>::class.java
            ).toList()
            list.size shouldBe 1
            list[0].code shouldBe request.code
        }
    }
}
