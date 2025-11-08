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
    private val patrolRouteRepository: PatrolRouteRepository,
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
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

        val request = CreateCheckpointRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            code = "CP-${java.util.UUID.randomUUID()}",
            geoLat = BigDecimal("41.7151377"),
            geoLon = BigDecimal("44.8270903"),
            radiusM = BigDecimal("50.00")
        )

        val response = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/checkpoints", request).withAuth(authToken),
            CheckpointResponse::class.java
        )

        response.code shouldBe request.code
        response.id shouldNotBe null
    }

    "should list checkpoints by site" {
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

        val createRequest = CreateCheckpointRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            code = "CP-${java.util.UUID.randomUUID()}"
        )

        val createResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/checkpoints", createRequest).withAuth(authToken),
            CheckpointResponse::class.java
        )

        val checkpoints = client.toBlocking().retrieve(
            HttpRequest.GET<Any>("/api/admin/checkpoints?siteId=${site.id}").withAuth(authToken),
            Array<CheckpointResponse>::class.java
        ).toList()

        checkpoints.size shouldBe 1
        checkpoints[0].code shouldBe createRequest.code
    }

    "should create route and add checkpoints" {
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

        val cp1Request = CreateCheckpointRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            code = "CP-${java.util.UUID.randomUUID()}"
        )
        val cp1Response = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/checkpoints", cp1Request).withAuth(authToken),
            CheckpointResponse::class.java
        )

        val cp2Request = CreateCheckpointRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            code = "CP-${java.util.UUID.randomUUID()}"
        )
        val cp2Response = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/checkpoints", cp2Request).withAuth(authToken),
            CheckpointResponse::class.java
        )

        val route = TestFixtures.createRoute(patrolRouteRepository, org.id!!, site.id!!)

        val routeRequest = CreateRouteRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            name = route.name
        )

        val routeResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/routes", routeRequest).withAuth(authToken),
            RouteResponse::class.java
        )

        val addPointsRequest = BulkAddRouteCheckpointsRequest(
            checkpoints = listOf(
                AddRouteCheckpointRequest(checkpointId = cp1Response.id as java.util.UUID, seq = 1, minOffsetSec = 0, maxOffsetSec = 3600),
                AddRouteCheckpointRequest(checkpointId = cp2Response.id as java.util.UUID, seq = 2, minOffsetSec = 3600, maxOffsetSec = 7200)
            )
        )

        val addResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/routes/${routeResponse.id}/points", addPointsRequest).withAuth(authToken),
            Map::class.java
        )

        addResponse["added"] shouldBe 2
    }

    "should complete full scan flow: start then finish successfully" {
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

        val checkpointRequest = CreateCheckpointRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            code = "CP-${java.util.UUID.randomUUID()}",
            geoLat = BigDecimal("41.7151377"),
            geoLon = BigDecimal("44.8270903"),
            radiusM = BigDecimal("100.00")
        )
        val checkpointResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/checkpoints", checkpointRequest).withAuth(authToken),
            CheckpointResponse::class.java
        )

        val route = TestFixtures.createRoute(patrolRouteRepository, org.id!!, site.id!!)

        val addPointsRequest = BulkAddRouteCheckpointsRequest(
            checkpoints = listOf(
                AddRouteCheckpointRequest(checkpointId = checkpointResponse.id as java.util.UUID, seq = 1, minOffsetSec = 0, maxOffsetSec = 3600)
            )
        )
        client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/routes/${route.id}/points", addPointsRequest).withAuth(authToken),
            Map::class.java
        )

        val patrolRun = patrolRunRepository.save(
            PatrolRun(
                routeId = route.id!!,
                organizationId = org.id!!,
                plannedStart = Instant.now(),
                plannedEnd = Instant.now().plusSeconds(7200),
                status = "pending"
            )
        )
        patrolRunRepository.flush()

        val startRequest = StartScanRequest(
            organizationId = org.id!!,
            deviceId = "device-123",
            checkpointCode = checkpointResponse.code
        )

        val startResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/scan/start", startRequest).withAuth(authToken),
            StartScanResponse::class.java
        )

        startResponse.challenge shouldNotBe null
        startResponse.policy.checkpointId shouldBe checkpointResponse.id
        startResponse.policy.runId shouldBe patrolRun.id!!

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

        val events = patrolScanEventRepository.findByPatrolRunId(patrolRun.id!!)
        events.size shouldBe 1
        events[0].verdict shouldBe "ok"
    }

    "should reject replay attack - second finish with same challenge fails" {
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

        val checkpointRequest = CreateCheckpointRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            code = "CP-${java.util.UUID.randomUUID()}"
        )
        val checkpointResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/checkpoints", checkpointRequest).withAuth(authToken),
            CheckpointResponse::class.java
        )

        val route = TestFixtures.createRoute(patrolRouteRepository, org.id!!, site.id!!)

        val addPointsRequest = BulkAddRouteCheckpointsRequest(
            checkpoints = listOf(
                AddRouteCheckpointRequest(checkpointId = checkpointResponse.id as java.util.UUID, seq = 1)
            )
        )
        client.toBlocking().retrieve(
            HttpRequest.POST("/api/admin/routes/${route.id}/points", addPointsRequest).withAuth(authToken),
            Map::class.java
        )

        val patrolRun = patrolRunRepository.save(
            PatrolRun(
                routeId = route.id!!,
                organizationId = org.id!!,
                plannedStart = Instant.now(),
                plannedEnd = Instant.now().plusSeconds(7200),
                status = "pending"
            )
        )
        patrolRunRepository.flush()

        val startRequest = StartScanRequest(
            organizationId = org.id!!,
            deviceId = "device-replay-123",
            checkpointCode = checkpointResponse.code
        )

        val startResponse = client.toBlocking().retrieve(
            HttpRequest.POST("/api/scan/start", startRequest).withAuth(authToken),
            StartScanResponse::class.java
        )

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

        val exception = assertThrows<HttpClientResponseException> {
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/finish", finishRequest).withAuth(authToken),
                FinishScanResponse::class.java
            )
        }


        exception.status shouldBe HttpStatus.CONFLICT
    }
})
