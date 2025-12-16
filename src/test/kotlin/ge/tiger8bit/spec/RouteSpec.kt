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

@MicronautTest(transactional = false)
class RouteSpec : BaseApiSpec() {

    override fun StringSpec.registerTests() {
        "BOSS can create route and add checkpoints" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val cp1 = client.toBlocking().retrieve(
                HttpRequest.POST("/api/admin/checkpoints", CreateCheckpointRequest(org.id!!, site.id!!, "CP-1")).withAuth(bossToken),
                CheckpointResponse::class.java
            )
            val cp2 = client.toBlocking().retrieve(
                HttpRequest.POST("/api/admin/checkpoints", CreateCheckpointRequest(org.id!!, site.id!!, "CP-2")).withAuth(bossToken),
                CheckpointResponse::class.java
            )

            val route = fixtures.createRoute(org.id!!, site.id!!)

            val addResponse = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/routes/${route.id}/points",
                    BulkAddRouteCheckpointsRequest(
                        listOf(
                            AddRouteCheckpointRequest(cp1.id, 1, 0, 3600),
                            AddRouteCheckpointRequest(cp2.id, 2, 0, 3600)
                        )
                    )
                ).withAuth(bossToken),
                Map::class.java
            )

            addResponse["status"] shouldBe "updated"
        }

        "WORKER cannot create route (forbidden)" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            val exception = assertThrows<HttpClientResponseException> {
                client.toBlocking().retrieve(
                    HttpRequest.POST("/api/admin/routes", CreateRouteRequest(org.id!!, site.id!!, "Unauthorized Route"))
                        .withAuth(workerToken),
                    RouteResponse::class.java
                )
            }

            exception.status shouldBe HttpStatus.FORBIDDEN
        }
    }
}
