package ge.tiger8bit.spec

import ge.tiger8bit.dto.*
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.TestData.Emails
import ge.tiger8bit.spec.common.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.junit.jupiter.api.assertThrows
import java.time.Instant

@MicronautTest(transactional = false)
class ReplaySpec : BaseApiSpec() {

    override fun StringSpec.registerTests() {
        "replay attack returns 409" {
            val (org, site) = fixtures.seedOrgAndSite()

            val (workerToken, workerId) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))
            val deviceId = "device-replay-test"
            fixtures.createDevice(workerId, org.id!!, deviceId = deviceId)

            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(org.id!!, site.id!!, "CP-REPLAY")
                ).withAuth(bossToken),
                CheckpointResponse::class.java
            )

            val route = fixtures.createRoute(org.id!!, site.id!!)
            client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/routes/${route.id}/points",
                    BulkAddRouteCheckpointsRequest(listOf(AddRouteCheckpointRequest(cp.id, 1)))
                ).withAuth(bossToken),
                Map::class.java
            )

            fixtures.createPatrolRun(route.id!!, org.id!!)

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, deviceId, cp.code)).withAuth(workerToken),
                StartScanResponse::class.java
            )

            val firstFinish = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/scan/finish",
                    FinishScanRequest(
                        challenge = start.challenge,
                        scannedAt = Instant.now().toString()
                    )
                ).withAuth(workerToken),
                FinishScanResponse::class.java
            )
            firstFinish.verdict shouldBe ScanVerdict.OK

            val ex = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST(
                        "/api/scan/finish",
                        FinishScanRequest(
                            challenge = start.challenge,
                            scannedAt = Instant.now().toString()
                        )
                    ).withAuth(workerToken),
                    FinishScanResponse::class.java
                )
            }

            ex.status shouldBe HttpStatus.CONFLICT
        }
    }
}
