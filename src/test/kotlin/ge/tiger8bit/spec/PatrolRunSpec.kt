package ge.tiger8bit.spec

import ge.tiger8bit.dto.*
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.TestData.Emails
import ge.tiger8bit.spec.common.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Duration
import java.time.Instant

@MicronautTest(transactional = false)
class PatrolRunSpec : BaseApiSpec() {

    override fun StringSpec.registerTests() {
        "BOSS can create patrol run" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val route = fixtures.createRoute(org.id!!, site.id!!)

            val request = CreatePatrolRunRequest(
                routeId = route.id!!,
                organizationId = org.id!!
            )

            val response = client.toBlocking().retrieve(
                HttpRequest.POST("/api/patrol-runs", request).withAuth(bossToken),
                PatrolRunResponse::class.java
            )

            response.id shouldNotBe null
            response.routeId shouldBe route.id
            response.organizationId shouldBe org.id
            response.status shouldBe PatrolRunStatus.IN_PROGRESS
            response.plannedStart shouldNotBe null
            response.plannedEnd shouldNotBe null
        }

        "APP_OWNER can create patrol run" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (ownerToken, _) = specHelpers.createAppOwnerTokenForOrg(org.id!!, Emails.unique("owner"))

            val route = fixtures.createRoute(org.id!!, site.id!!)

            val request = CreatePatrolRunRequest(
                routeId = route.id!!,
                organizationId = org.id!!
            )

            val response = client.toBlocking().retrieve(
                HttpRequest.POST("/api/patrol-runs", request).withAuth(ownerToken),
                PatrolRunResponse::class.java
            )

            response.id shouldNotBe null
            response.status shouldBe PatrolRunStatus.IN_PROGRESS
        }

        "can create patrol run with custom time window" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val route = fixtures.createRoute(org.id!!, site.id!!)

            val plannedStart = Instant.now().plus(Duration.ofHours(2))
            val plannedEnd = plannedStart.plus(Duration.ofHours(4))

            val request = CreatePatrolRunRequest(
                routeId = route.id!!,
                organizationId = org.id!!,
                plannedStart = plannedStart,
                plannedEnd = plannedEnd
            )

            val response = client.toBlocking().retrieve(
                HttpRequest.POST("/api/patrol-runs", request).withAuth(bossToken),
                PatrolRunResponse::class.java
            )

            response.id shouldNotBe null
            response.plannedStart shouldBe plannedStart
            response.plannedEnd shouldBe plannedEnd
            response.status shouldBe PatrolRunStatus.IN_PROGRESS
        }

        "WORKER cannot create patrol run" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            val route = fixtures.createRoute(org.id!!, site.id!!)

            val request = CreatePatrolRunRequest(
                routeId = route.id!!,
                organizationId = org.id!!
            )

            val exception = io.kotest.assertions.throwables.shouldThrow<io.micronaut.http.client.exceptions.HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST("/api/patrol-runs", request).withAuth(workerToken),
                    PatrolRunResponse::class.java
                )
            }

            exception.status shouldBe io.micronaut.http.HttpStatus.FORBIDDEN
        }

        "created patrol run is used by scan flow" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(
                        org.id!!,
                        site.id!!,
                        "CP-RUN-TEST",
                        java.math.BigDecimal("41.7151377"),
                        java.math.BigDecimal("44.8270903"),
                        java.math.BigDecimal("100.00")
                    )
                ).withAuth(bossToken),
                CheckpointResponse::class.java
            )

            val route = fixtures.createRoute(org.id!!, site.id!!)
            client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/routes/${route.id}/points",
                    BulkAddRouteCheckpointsRequest(listOf(AddRouteCheckpointRequest(cp.id, 1, 0, 3600)))
                ).withAuth(bossToken),
                Map::class.java
            )

            // Create patrol run via API
            val runRequest = CreatePatrolRunRequest(
                routeId = route.id!!,
                organizationId = org.id!!
            )

            val runResponse = client.toBlocking().retrieve(
                HttpRequest.POST("/api/patrol-runs", runRequest).withAuth(bossToken),
                PatrolRunResponse::class.java
            )

            // Now worker can scan
            val (workerToken, workerId) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))
            val deviceId = "device-run-test"
            fixtures.createDevice(workerId, org.id!!, deviceId = deviceId)

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, deviceId, cp.code)).withAuth(workerToken),
                StartScanResponse::class.java
            )

            start.challenge shouldNotBe null
            start.policy.runId shouldBe runResponse.id
            start.policy.checkpointId shouldBe cp.id
        }
    }
}
