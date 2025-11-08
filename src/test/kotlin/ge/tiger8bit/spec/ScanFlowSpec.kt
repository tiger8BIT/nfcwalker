package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.PatrolRun
import ge.tiger8bit.dto.*
import ge.tiger8bit.repository.*
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
import java.time.Instant

@MicronautTest(transactional = false)
class ScanFlowSpec : StringSpec() {
    @Inject
    lateinit var organizationRepository: OrganizationRepository
    @Inject
    lateinit var siteRepository: SiteRepository
    @Inject
    lateinit var patrolRouteRepository: PatrolRouteRepository
    @Inject
    lateinit var patrolRunRepository: PatrolRunRepository
    @Inject
    lateinit var patrolScanEventRepository: PatrolScanEventRepository
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    private val authToken = TestAuth.generateToken()

    init {
        "complete start/finish" {
            val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(
                        org.id!!,
                        site.id!!,
                        "CP-${java.util.UUID.randomUUID()}",
                        BigDecimal("41.7151377"),
                        BigDecimal("44.8270903"),
                        BigDecimal("100.00")
                    )
                ).withAuth(authToken),
                CheckpointResponse::class.java
            )
            val route = TestFixtures.createRoute(patrolRouteRepository, org.id!!, site.id!!)
            client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/routes/${route.id}/points",
                    BulkAddRouteCheckpointsRequest(listOf(AddRouteCheckpointRequest(cp.id as java.util.UUID, 1, 0, 3600)))
                ).withAuth(authToken), Map::class.java
            )

            val run = patrolRunRepository.save(
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
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, "device-123", cp.code)).withAuth(authToken),
                StartScanResponse::class.java
            )
            start.challenge shouldNotBe null
            start.policy.checkpointId shouldBe cp.id
            start.policy.runId shouldBe run.id

            val finish = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/scan/finish",
                    FinishScanRequest(
                        start.challenge,
                        "user-456",
                        Instant.now().toString(),
                        BigDecimal("41.7151377"),
                        BigDecimal("44.8270903")
                    )
                ).withAuth(authToken), FinishScanResponse::class.java
            )
            finish.verdict shouldBe "ok"
            finish.eventId shouldNotBe null

            val events = patrolScanEventRepository.findByPatrolRunId(run.id!!)
            events.size shouldBe 1
            events[0].verdict shouldBe "ok"
        }
    }
}
