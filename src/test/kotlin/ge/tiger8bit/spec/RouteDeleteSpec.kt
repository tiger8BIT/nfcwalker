package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.Checkpoint
import ge.tiger8bit.domain.PatrolRoute
import ge.tiger8bit.domain.PatrolRouteCheckpoint
import ge.tiger8bit.domain.Role
import ge.tiger8bit.repository.CheckpointRepository
import ge.tiger8bit.repository.PatrolRouteCheckpointRepository
import ge.tiger8bit.repository.PatrolRouteRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class RouteDeleteSpec(
    @Inject @Client("/") val client: HttpClient,
    @Inject val checkpointRepository: CheckpointRepository,
    @Inject val patrolRouteRepository: PatrolRouteRepository,
    @Inject val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
    @Inject val beanContext: io.micronaut.context.BeanContext
) : StringSpec({

    TestFixtures.init(beanContext)

    "DELETE /api/admin/routes/{id} should delete route and its checkpoints" {
        val (org, site) = TestFixtures.seedOrgAndSite()
        val (bossUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_BOSS,
            email = "boss@route-delete.com"
        )
        val bossToken = TestAuth.generateBossToken(bossUser.id.toString())

        val route = patrolRouteRepository.save(
            PatrolRoute(
                organizationId = org.id!!,
                siteId = site.id!!,
                name = "Test Route for Delete"
            )
        )

        val checkpoint1 = checkpointRepository.save(
            Checkpoint(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "DELETE_CP_1"
            )
        )
        val checkpoint2 = checkpointRepository.save(
            Checkpoint(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "DELETE_CP_2"
            )
        )

        patrolRouteCheckpointRepository.save(
            PatrolRouteCheckpoint(
                routeId = route.id!!,
                checkpointId = checkpoint1.id!!,
                seq = 1
            )
        )
        patrolRouteCheckpointRepository.save(
            PatrolRouteCheckpoint(
                routeId = route.id!!,
                checkpointId = checkpoint2.id!!,
                seq = 2
            )
        )

        patrolRouteRepository.existsById(route.id!!) shouldBe true
        patrolRouteCheckpointRepository.findByRouteIdOrderBySeqAsc(route.id!!).size shouldBe 2

        val request = HttpRequest.DELETE<Any>("/api/admin/routes/${route.id}")
            .bearerAuth(bossToken)

        val response = client.toBlocking().exchange(request, Map::class.java)

        response.status shouldBe HttpStatus.OK
        val body = response.body() as Map<*, *>
        body["status"] shouldBe "deleted"
        body["id"] shouldBe route.id.toString()

        patrolRouteRepository.existsById(route.id!!) shouldBe false
        patrolRouteCheckpointRepository.findByRouteIdOrderBySeqAsc(route.id!!).size shouldBe 0
    }

    "DELETE /api/admin/checkpoints/{id} should delete checkpoint" {
        val (org, site) = TestFixtures.seedOrgAndSite()
        val (bossUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_BOSS,
            email = "boss@checkpoint-delete.com"
        )
        val bossToken = TestAuth.generateBossToken(bossUser.id.toString())

        val checkpoint = checkpointRepository.save(
            Checkpoint(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "DELETE_CP_TEST"
            )
        )

        checkpointRepository.existsById(checkpoint.id!!) shouldBe true

        val request = HttpRequest.DELETE<Any>("/api/admin/checkpoints/${checkpoint.id}")
            .bearerAuth(bossToken)

        val response = client.toBlocking().exchange(request, Map::class.java)

        response.status shouldBe HttpStatus.OK
        val body = response.body() as Map<*, *>
        body["status"] shouldBe "deleted"

        checkpointRepository.existsById(checkpoint.id!!) shouldBe false
    }

    "WORKER cannot delete route (forbidden)" {
        val (org, site) = TestFixtures.seedOrgAndSite()
        val (workerUser, _) = TestFixtures.createUserWithRole(
            org.id!!,
            Role.ROLE_WORKER,
            email = "worker@route-delete-forbidden.com"
        )
        val workerToken = TestAuth.generateWorkerToken(workerUser.id.toString())

        val route = patrolRouteRepository.save(
            PatrolRoute(
                organizationId = org.id!!,
                siteId = site.id!!,
                name = "Protected Route"
            )
        )

        val workerRequest = HttpRequest.DELETE<Any>("/api/admin/routes/${route.id}")
            .bearerAuth(workerToken)

        try {
            client.toBlocking().exchange(workerRequest, Map::class.java)
            throw AssertionError("Should have thrown HttpClientResponseException")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.FORBIDDEN
        }
    }
})

