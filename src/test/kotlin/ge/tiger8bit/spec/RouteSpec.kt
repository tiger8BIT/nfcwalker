package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.dto.*
import ge.tiger8bit.repository.OrganizationRepository
import ge.tiger8bit.repository.PatrolRouteRepository
import ge.tiger8bit.repository.SiteRepository
import ge.tiger8bit.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.assertThrows

@MicronautTest(transactional = false)
class RouteSpec : StringSpec() {
    @Inject
    lateinit var organizationRepository: OrganizationRepository
    @Inject
    lateinit var siteRepository: SiteRepository
    @Inject
    lateinit var patrolRouteRepository: PatrolRouteRepository
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    private val bossToken = TestAuth.generateBossToken()

    init {
        "BOSS can create route and add checkpoints" {
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
            val cp1 = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(org.id!!, site.id!!, "CP-${java.util.UUID.randomUUID()}")
                ).withAuth(bossToken), CheckpointResponse::class.java
            )
            val cp2 = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(org.id!!, site.id!!, "CP-${java.util.UUID.randomUUID()}")
                ).withAuth(bossToken), CheckpointResponse::class.java
            )

            val route = TestFixtures.createRoute(patrolRouteRepository, org.id!!, site.id!!)
            val routeResp = client.toBlocking().retrieve(
                HttpRequest.POST("/api/admin/routes", CreateRouteRequest(org.id!!, site.id!!, route.name)).withAuth(bossToken),
                RouteResponse::class.java
            )

            val bulk = BulkAddRouteCheckpointsRequest(
                listOf(
                    AddRouteCheckpointRequest(checkpointId = cp1.id, seq = 1, minOffsetSec = 0, maxOffsetSec = 3600),
                    AddRouteCheckpointRequest(checkpointId = cp2.id, seq = 2, minOffsetSec = 3600, maxOffsetSec = 7200)
                )
            )
            val added = client.toBlocking()
                .retrieve(HttpRequest.POST("/api/admin/routes/${routeResp.id}/points", bulk).withAuth(bossToken), Map::class.java)
            added["added"] shouldBe 2
        }

        "WORKER cannot create route (forbidden)" {
            val workerToken = TestAuth.generateWorkerToken()
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
            val request = CreateRouteRequest(org.id!!, site.id!!, "Test Route")

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST("/api/admin/routes", request).withAuth(workerToken),
                    RouteResponse::class.java
                )
            }
            exception.status shouldBe HttpStatus.FORBIDDEN
        }
    }
}

