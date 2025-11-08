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
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

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

    private val authToken = TestAuth.generateToken()

    init {
        "create route and add checkpoints" {
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
            val cp1 = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(org.id!!, site.id!!, "CP-${java.util.UUID.randomUUID()}")
                ).withAuth(authToken), CheckpointResponse::class.java
            )
            val cp2 = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(org.id!!, site.id!!, "CP-${java.util.UUID.randomUUID()}")
                ).withAuth(authToken), CheckpointResponse::class.java
            )

            val route = TestFixtures.createRoute(patrolRouteRepository, org.id!!, site.id!!)
            val routeResp = client.toBlocking().retrieve(
                HttpRequest.POST("/api/admin/routes", CreateRouteRequest(org.id!!, site.id!!, route.name)).withAuth(authToken),
                RouteResponse::class.java
            )

            val bulk = BulkAddRouteCheckpointsRequest(
                listOf(
                    AddRouteCheckpointRequest(checkpointId = cp1.id as java.util.UUID, seq = 1, minOffsetSec = 0, maxOffsetSec = 3600),
                    AddRouteCheckpointRequest(checkpointId = cp2.id as java.util.UUID, seq = 2, minOffsetSec = 3600, maxOffsetSec = 7200)
                )
            )
            val added = client.toBlocking()
                .retrieve(HttpRequest.POST("/api/admin/routes/${routeResp.id}/points", bulk).withAuth(authToken), Map::class.java)
            added["added"] shouldBe 2
        }
    }
}
