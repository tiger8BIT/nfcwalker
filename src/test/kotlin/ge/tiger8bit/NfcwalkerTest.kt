package ge.tiger8bit

import ge.tiger8bit.domain.*
import ge.tiger8bit.dto.*
import ge.tiger8bit.repository.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant

@MicronautTest(transactional = false)
class NfcwalkerTest(
    private val application: EmbeddedApplication<*>,
    @Client("/") private val client: HttpClient,
    private val organizationRepository: OrganizationRepository,
    private val siteRepository: SiteRepository,
    private val checkpointRepository: CheckpointRepository,
    private val patrolRouteRepository: PatrolRouteRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
    private val patrolRunRepository: PatrolRunRepository,
    private val challengeUsedRepository: ChallengeUsedRepository,
    private val patrolScanEventRepository: PatrolScanEventRepository
) : StringSpec({

    beforeSpec {
        // Ensure no leftover challenge JTIs from previous runs
        TestFixtures.cleanupChallengeUsed(challengeUsedRepository)
    }

    // single token used by all tests (makes debugging/printing easier)
    val authToken = TestAuth.generateToken()
    println("TEST AUTH TOKEN: $authToken")
    println("TOKEN VALID: ${TestAuth.validateToken(authToken)}")
    println("TOKEN CLAIMS: ${TestAuth.decodeClaims(authToken)}")

    "test the server is running" {
        assert(application.isRunning)
    }

    "should create checkpoint via admin API" {
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository, "Test Org", "Test Site")

        val request = CreateCheckpointRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            code = "CP-001",
            geoLat = BigDecimal("41.7151377"),
            geoLon = BigDecimal("44.8270903"),
            radiusM = BigDecimal("50.00")
        )

        val response = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/checkpoints", request).withAuth(authToken),
            CheckpointResponse::class.java
        )

        response.code shouldBe "CP-001"
        response.id shouldNotBe null
    }

    "should list checkpoints by site" {
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository, "Test Org 2", "Test Site 2")
        TestFixtures.createCheckpoint(checkpointRepository, org.id!!, site.id!!, "CP-LIST-001")

        checkpointRepository.flush()

        val checkpoints = client.toBlocking().retrieve(
            HttpRequest.GET<Any>("/api/admin/checkpoints?siteId=${site.id}").withAuth(authToken),
            Array<CheckpointResponse>::class.java
        ).toList()

        checkpoints.size shouldBe 1
        checkpoints[0].code shouldBe "CP-LIST-001"
    }

    "should create route and add checkpoints" {
        val (route, cps) = TestFixtures.createRouteWithTwoCheckpoints(
            organizationRepository,
            siteRepository,
            checkpointRepository,
            patrolRouteRepository,
            patrolRouteCheckpointRepository,
            "Test Org 3",
            "Test Site 3",
            "CP-R1-001",
            "CP-R1-002"
        )

        val (cp1, cp2) = cps

        val routeRequest = CreateRouteRequest(
            organizationId = route.organizationId,
            siteId = route.siteId,
            name = "Night Patrol Route"
        )

        val routeResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/routes", routeRequest).withAuth(authToken),
            RouteResponse::class.java
        )

        routeResponse.name shouldBe "Night Patrol Route"

        val addPointsRequest = BulkAddRouteCheckpointsRequest(
            checkpoints = listOf(
                AddRouteCheckpointRequest(checkpointId = cp1.id!!, seq = 1, minOffsetSec = 0, maxOffsetSec = 3600),
                AddRouteCheckpointRequest(checkpointId = cp2.id!!, seq = 2, minOffsetSec = 3600, maxOffsetSec = 7200)
            )
        )

        val addResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/routes/${routeResponse.id}/points", addPointsRequest).withAuth(authToken),
            Map::class.java
        )

        addResponse["added"] shouldBe 2
    }

    "should complete full scan flow: start then finish successfully" {
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository, "Scan Test Org", "Scan Test Site")
        val checkpoint = TestFixtures.createCheckpoint(checkpointRepository, org.id!!, site.id!!, "CP-SCAN-001", BigDecimal("41.7151377"), BigDecimal("44.8270903"), BigDecimal("100.00"))
        val route = patrolRouteRepository.save(PatrolRoute(organizationId = org.id!!, siteId = site.id!!, name = "Test Route"))
        patrolRouteCheckpointRepository.save(PatrolRouteCheckpoint(routeId = route.id!!, checkpointId = checkpoint.id!!, seq = 1, minOffsetSec = 0, maxOffsetSec = 3600))
        val patrolRun = TestFixtures.createPatrolRun(patrolRunRepository, route.id!!, org.id!!)

        patrolRunRepository.flush()

        // Start scan
        val startRequest = StartScanRequest(
            organizationId = org.id!!,
            deviceId = "device-123",
            checkpointCode = checkpoint.code
        )

        val startResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/scan/start", startRequest).withAuth(authToken),
            StartScanResponse::class.java
        )

        startResponse.challenge shouldNotBe null
        startResponse.policy.checkpointId shouldBe checkpoint.id!!
        startResponse.policy.runId shouldBe patrolRun.id!!

        // Finish scan
        val finishRequest = FinishScanRequest(
            challenge = startResponse.challenge,
            userId = "user-456",
            scannedAt = Instant.now().toString(),
            lat = BigDecimal("41.7151377"),
            lon = BigDecimal("44.8270903")
        )

        val finishResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/scan/finish", finishRequest).withAuth(authToken),
            FinishScanResponse::class.java
        )

        finishResponse.verdict shouldBe "ok"
        finishResponse.eventId shouldNotBe null

        // Verify event was created
        val events = patrolScanEventRepository.findByPatrolRunId(patrolRun.id!!)
        events.size shouldBe 1
        events[0].verdict shouldBe "ok"
    }

    "should reject replay attack - second finish with same challenge fails" {
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository, "Replay Test Org", "Replay Test Site")
        val checkpoint = TestFixtures.createCheckpoint(checkpointRepository, org.id!!, site.id!!, "CP-REPLAY-001")
        val route = patrolRouteRepository.save(PatrolRoute(organizationId = org.id!!, siteId = site.id!!, name = "Replay Test Route"))
        patrolRouteCheckpointRepository.save(PatrolRouteCheckpoint(routeId = route.id!!, checkpointId = checkpoint.id!!, seq = 1))
        patrolRunRepository.save(PatrolRun(routeId = route.id!!, organizationId = org.id!!, plannedStart = Instant.now(), plannedEnd = Instant.now().plusSeconds(7200), status = "pending"))

        // Start scan
        val startRequest = StartScanRequest(
            organizationId = org.id!!,
            deviceId = "device-replay-123",
            checkpointCode = "CP-REPLAY-001"
        )

        val startResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/scan/start", startRequest).withAuth(authToken),
            StartScanResponse::class.java
        )

        // First finish - should succeed
        val finishRequest = FinishScanRequest(
            challenge = startResponse.challenge,
            userId = "user-replay",
            scannedAt = Instant.now().toString()
        )

        val finishResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/scan/finish", finishRequest).withAuth(authToken),
            FinishScanResponse::class.java
        )

        finishResponse.verdict shouldBe "ok"

        // Second finish with SAME challenge - should fail with 409 CONFLICT
        val exception = assertThrows<HttpClientResponseException> {
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/finish", finishRequest).withAuth(authToken),
                FinishScanResponse::class.java
            )
        }

        exception.status shouldBe HttpStatus.CONFLICT
    }
})
