package ge.tiger8bit.spec

import ge.tiger8bit.dto.*
import ge.tiger8bit.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.math.BigDecimal
import java.time.Instant

@MicronautTest(transactional = false)
class ScanFlowSpec : BaseApiSpec() {

    override fun StringSpec.registerTests() {
        "complete start/finish" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = "boss@scan-test.com")

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(
                        org.id!!,
                        site.id!!,
                        "CP-SCAN",
                        BigDecimal("41.7151377"),
                        BigDecimal("44.8270903"),
                        BigDecimal("100.00")
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

            val run = fixtures.createPatrolRun(route.id!!, org.id!!)

            val (workerToken, workerId) = specHelpers.createWorkerToken(org.id!!, email = "worker@scan-test.com")
            val deviceId = "device-scan-test"
            fixtures.createDevice(workerId, org.id!!, deviceId = deviceId)

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, deviceId, cp.code)).withAuth(workerToken),
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
                        workerId.toString(),
                        Instant.now().toString(),
                        BigDecimal("41.7151377"),
                        BigDecimal("44.8270903")
                    )
                ).withAuth(workerToken),
                FinishScanResponse::class.java
            )

            finish.verdict shouldBe "ok"
            finish.eventId shouldNotBe null
        }
    }
}
