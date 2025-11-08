package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.PatrolRun
import ge.tiger8bit.dto.*
import ge.tiger8bit.repository.OrganizationRepository
import ge.tiger8bit.repository.PatrolRouteRepository
import ge.tiger8bit.repository.PatrolRunRepository
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
import java.time.Instant

@MicronautTest(transactional = false)
class ReplaySpec : StringSpec() {
    @Inject
    lateinit var organizationRepository: OrganizationRepository
    @Inject
    lateinit var siteRepository: SiteRepository
    @Inject
    lateinit var patrolRouteRepository: PatrolRouteRepository
    @Inject
    lateinit var patrolRunRepository: PatrolRunRepository
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    private val authToken = TestAuth.generateToken()

    init {
        "replay attack returns 409" {
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(org.id!!, site.id!!, "CP-${java.util.UUID.randomUUID()}")
                ).withAuth(authToken), CheckpointResponse::class.java
            )
            val route = TestFixtures.createRoute(patrolRouteRepository, org.id!!, site.id!!)
            client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/routes/${route.id}/points",
                    BulkAddRouteCheckpointsRequest(listOf(AddRouteCheckpointRequest(cp.id, 1)))
                ).withAuth(authToken), Map::class.java
            )

            // Create an active patrol run so /api/scan/start can find it
            patrolRunRepository.save(
                PatrolRun(
                    routeId = route.id!!,
                    organizationId = org.id!!,
                    plannedStart = Instant.now(),
                    plannedEnd = Instant.now().plusSeconds(7200),
                    status = "pending"
                )
            )
            patrolRunRepository.flush()

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, "device-replay-123", cp.code)).withAuth(authToken),
                StartScanResponse::class.java
            )
            client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/scan/finish",
                    FinishScanRequest(start.challenge, "user-replay", java.time.Instant.now().toString())
                ).withAuth(authToken), FinishScanResponse::class.java
            )

            val ex = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST(
                        "/api/scan/finish",
                        FinishScanRequest(start.challenge, "user-replay", java.time.Instant.now().toString())
                    ).withAuth(authToken), FinishScanResponse::class.java
                )
            }
            ex.status shouldBe HttpStatus.CONFLICT
        }
    }
}
