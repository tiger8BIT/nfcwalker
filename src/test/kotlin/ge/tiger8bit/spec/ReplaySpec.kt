package ge.tiger8bit.spec

import ge.tiger8bit.TestFixtures
import ge.tiger8bit.dto.*
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
class ReplaySpec @Inject constructor(
    @Client("/") client: HttpClient,
    beanContext: io.micronaut.context.BeanContext
) : BaseApiSpec(client, beanContext) {
    override fun StringSpec.registerTests() {
        "replay attack returns 409" {
            val (org, site) = TestFixtures.seedOrgAndSite()

            val (workerToken, workerId) = createWorkerToken(org.id!!, email = "worker@replay-test.com")
            val deviceId = "device-replay-test"
            TestFixtures.createDevice(workerId, org.id!!, deviceId = deviceId)

            val (bossToken, _) = createBossToken(org.id!!, email = "boss@replay-test.com")

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(org.id!!, site.id!!, "CP-REPLAY")
                ).withAuth(bossToken),
                ge.tiger8bit.dto.CheckpointResponse::class.java
            )

            val route = TestFixtures.createRoute(org.id!!, site.id!!)
            client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/routes/${route.id}/points",
                    BulkAddRouteCheckpointsRequest(listOf(AddRouteCheckpointRequest(cp.id, 1)))
                ).withAuth(bossToken),
                Map::class.java
            )

            TestFixtures.createPatrolRun(route.id!!, org.id!!)

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, deviceId, cp.code)).withAuth(workerToken),
                StartScanResponse::class.java
            )

            client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/scan/finish",
                    FinishScanRequest(start.challenge, workerId.toString(), Instant.now().toString())
                ).withAuth(workerToken),
                FinishScanResponse::class.java
            )

            val ex = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST(
                        "/api/scan/finish",
                        FinishScanRequest(start.challenge, workerId.toString(), Instant.now().toString())
                    ).withAuth(workerToken),
                    FinishScanResponse::class.java
                )
            }

            ex.status shouldBe HttpStatus.CONFLICT
        }
    }
}
