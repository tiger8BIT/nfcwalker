package ge.tiger8bit.spec

import ge.tiger8bit.dto.*
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.TestData.Emails
import ge.tiger8bit.spec.common.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

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

        "BOSS can list routes" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            fixtures.createRoute(org.id!!, site.id!!, "Route 1")
            fixtures.createRoute(org.id!!, site.id!!, "Route 2")

            val page = getPage("/api/admin/routes?orgId=${org.id}&page=0&size=100", bossToken, RouteResponse::class.java)

            page.content.size shouldBe 2
        }

        "WORKER can list routes via user API" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            fixtures.createRoute(org.id!!, site.id!!, "Worker Route")

            val page = getPage("/api/routes?orgId=${org.id}", workerToken, RouteResponse::class.java)

            page.content.any { it.name == "Worker Route" } shouldBe true
        }

        "BOSS can update and delete route" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))
            val route = fixtures.createRoute(org.id!!, site.id!!)

            val updated = client.toBlocking().retrieve(
                HttpRequest.PUT("/api/admin/routes/${route.id}", UpdateRouteRequest(name = "Updated Name")).withAuth(bossToken),
                RouteResponse::class.java
            )
            updated.name shouldBe "Updated Name"

            val deleteResponse = client.toBlocking().exchange(
                HttpRequest.DELETE<Any>("/api/admin/routes/${route.id}").withAuth(bossToken),
                Map::class.java
            )
            deleteResponse.status shouldBe HttpStatus.OK
        }
    }
}
