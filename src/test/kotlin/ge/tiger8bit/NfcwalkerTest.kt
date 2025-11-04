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
    private val patrolScanEventRepository: PatrolScanEventRepository
) : StringSpec({

    // single token used by all tests (makes debugging/printing easier)
    val authToken = TestAuth.generateToken()
    println("TEST AUTH TOKEN: $authToken")
    println("TOKEN VALID: ${TestAuth.validateToken(authToken)}")
    println("TOKEN CLAIMS: ${TestAuth.decodeClaims(authToken)}")

    "test the server is running" {
        assert(application.isRunning)
    }

    "should create checkpoint via admin API" {
        val org = organizationRepository.save(Organization(name = "Test Org"))
        // ensure the insert is flushed and visible to the HTTP call in another transaction
        organizationRepository.flush()
        val site = siteRepository.save(Site(organizationId = org.id!!, name = "Test Site"))
        siteRepository.flush()

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
        val org = organizationRepository.save(Organization(name = "Test Org 2"))
        organizationRepository.flush()
        val site = siteRepository.save(Site(organizationId = org.id!!, name = "Test Site 2"))
        siteRepository.flush()
        checkpointRepository.save(
            Checkpoint(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-LIST-001"
            )
        )

        checkpointRepository.flush()

        val checkpoints = client.toBlocking().retrieve(
            HttpRequest.GET<Any>("/api/admin/checkpoints?siteId=${site.id}").withAuth(authToken),
            Array<CheckpointResponse>::class.java
        ).toList()

        checkpoints.size shouldBe 1
        checkpoints[0].code shouldBe "CP-LIST-001"
    }

    "should create route and add checkpoints" {
        val org = organizationRepository.save(Organization(name = "Test Org 3"))
        organizationRepository.flush()
        val site = siteRepository.save(Site(organizationId = org.id!!, name = "Test Site 3"))
        siteRepository.flush()

        val routeRequest = CreateRouteRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            name = "Night Patrol Route"
        )

        val routeResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/routes", routeRequest).withAuth(authToken),
            RouteResponse::class.java
        )

        routeResponse.name shouldBe "Night Patrol Route"

        val cp1 = checkpointRepository.save(
            Checkpoint(organizationId = org.id!!, siteId = site.id!!, code = "CP-R1-001")
        )
        val cp2 = checkpointRepository.save(
            Checkpoint(organizationId = org.id!!, siteId = site.id!!, code = "CP-R1-002")
        )

        checkpointRepository.flush()

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
        // Seed data
        val org = organizationRepository.save(Organization(name = "Scan Test Org"))
        organizationRepository.flush()
        val site = siteRepository.save(Site(organizationId = org.id!!, name = "Scan Test Site"))
        siteRepository.flush()
        val checkpoint = checkpointRepository.save(
            Checkpoint(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-SCAN-001",
                geoLat = BigDecimal("41.7151377"),
                geoLon = BigDecimal("44.8270903"),
                radiusM = BigDecimal("100.00")
            )
        )

        checkpointRepository.flush()
        val route = patrolRouteRepository.save(
            PatrolRoute(organizationId = org.id!!, siteId = site.id!!, name = "Test Route")
        )
        patrolRouteCheckpointRepository.save(
            PatrolRouteCheckpoint(
                routeId = route.id!!,
                checkpointId = checkpoint.id!!,
                seq = 1,
                minOffsetSec = 0,
                maxOffsetSec = 3600
            )
        )
        val patrolRun = patrolRunRepository.save(
            PatrolRun(
                routeId = route.id!!,
                organizationId = org.id!!,
                plannedStart = Instant.now(),
                plannedEnd = Instant.now().plusSeconds(7200),
                status = "in_progress"
            )
        )

        patrolRunRepository.flush()

        // Start scan
        val startRequest = StartScanRequest(
            organizationId = org.id!!,
            deviceId = "device-123",
            checkpointCode = "CP-SCAN-001"
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
        // Seed data
        val org = organizationRepository.save(Organization(name = "Replay Test Org"))
        organizationRepository.flush()
        val site = siteRepository.save(Site(organizationId = org.id!!, name = "Replay Test Site"))
        siteRepository.flush()
        val checkpoint = checkpointRepository.save(
            Checkpoint(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "CP-REPLAY-001"
            )
        )
        checkpointRepository.flush()
        val route = patrolRouteRepository.save(
            PatrolRoute(organizationId = org.id!!, siteId = site.id!!, name = "Replay Test Route")
        )
        patrolRouteCheckpointRepository.save(
            PatrolRouteCheckpoint(
                routeId = route.id!!,
                checkpointId = checkpoint.id!!,
                seq = 1
            )
        )
        patrolRunRepository.save(
            PatrolRun(
                routeId = route.id!!,
                organizationId = org.id!!,
                plannedStart = Instant.now(),
                plannedEnd = Instant.now().plusSeconds(7200),
                status = "pending"
            )
        )

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
