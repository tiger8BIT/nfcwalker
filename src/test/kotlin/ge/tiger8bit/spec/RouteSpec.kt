package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.Role
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

@MicronautTest(transactional = false)
class RouteSpec(
    @Inject @Client("/") val client: HttpClient,
    @Inject val beanContext: io.micronaut.context.BeanContext
) : StringSpec({
    TestFixtures.init(beanContext)

    "BOSS can create route and add checkpoints" {
        val (org, site) = TestFixtures.seedOrgAndSite()
        val (bossUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_BOSS,
            email = "boss@route.com"
        )
        val bossToken = TestAuth.generateBossToken(bossUser.id.toString())

        val cp1 = client.toBlocking().retrieve(
            HttpRequest.POST(
                "/api/admin/checkpoints",
                CreateCheckpointRequest(org.id!!, site.id!!, "CP-1")
            ).withAuth(bossToken), CheckpointResponse::class.java
        )
        val cp2 = client.toBlocking().retrieve(
            HttpRequest.POST(
                "/api/admin/checkpoints",
                CreateCheckpointRequest(org.id!!, site.id!!, "CP-2")
            ).withAuth(bossToken), CheckpointResponse::class.java
        )

        val route = TestFixtures.createRoute(org.id!!, site.id!!)

        val addResponse = client.toBlocking().retrieve(
            HttpRequest.POST(
                "/api/admin/routes/${route.id}/points",
                BulkAddRouteCheckpointsRequest(
                    listOf(
                        AddRouteCheckpointRequest(cp1.id, 1, 0, 3600),
                        AddRouteCheckpointRequest(cp2.id, 2, 0, 3600)
                    )
                )
            ).withAuth(bossToken), Map::class.java
        )

        addResponse["status"] shouldBe "updated"
    }

    "WORKER cannot create route (forbidden)" {
        val (org, site) = TestFixtures.seedOrgAndSite()
        val (workerUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_WORKER,
            email = "worker@route-forbidden.com"
        )
        val workerToken = TestAuth.generateWorkerToken(workerUser.id.toString())

        val request = CreateRouteRequest(
            organizationId = org.id!!,
            siteId = site.id!!,
            name = "Unauthorized Route"
        )

        val exception = assertThrows<HttpClientResponseException> {
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/admin/routes", request).withAuth(workerToken),
                RouteResponse::class.java
            )
        }
        exception.status shouldBe HttpStatus.FORBIDDEN
    }
})

