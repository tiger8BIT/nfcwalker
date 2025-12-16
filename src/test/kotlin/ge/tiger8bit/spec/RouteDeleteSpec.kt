package ge.tiger8bit.spec

import ge.tiger8bit.domain.Checkpoint
import ge.tiger8bit.domain.PatrolRoute
import ge.tiger8bit.domain.PatrolRouteCheckpoint
import ge.tiger8bit.repository.CheckpointRepository
import ge.tiger8bit.repository.PatrolRouteCheckpointRepository
import ge.tiger8bit.repository.PatrolRouteRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class RouteDeleteSpec : BaseApiSpec() {

    @Inject
    private lateinit var checkpointRepository: CheckpointRepository

    @Inject
    private lateinit var patrolRouteRepository: PatrolRouteRepository

    @Inject
    private lateinit var patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository

    override fun StringSpec.registerTests() {
        "DELETE /api/admin/routes/{id} should delete route and its checkpoints" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = "boss@route-delete.com")

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

            val response = client.toBlocking().exchange(
                HttpRequest.DELETE<Any>("/api/admin/routes/${route.id}").bearerAuth(bossToken),
                Map::class.java
            )

            response.status shouldBe HttpStatus.OK
            val body = response.body() as Map<*, *>
            body["status"] shouldBe "deleted"
            body["id"] shouldBe route.id.toString()

            patrolRouteRepository.existsById(route.id!!) shouldBe false
            patrolRouteCheckpointRepository.findByRouteIdOrderBySeqAsc(route.id!!).size shouldBe 0
        }

        "DELETE /api/admin/checkpoints/{id} should delete checkpoint" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = "boss@checkpoint-delete.com")

            val checkpoint = checkpointRepository.save(
                Checkpoint(
                    organizationId = org.id!!,
                    siteId = site.id!!,
                    code = "DELETE_CP_TEST"
                )
            )

            checkpointRepository.existsById(checkpoint.id!!) shouldBe true

            val response = client.toBlocking().exchange(
                HttpRequest.DELETE<Any>("/api/admin/checkpoints/${checkpoint.id}").bearerAuth(bossToken),
                Map::class.java
            )

            response.status shouldBe HttpStatus.OK
            val body = response.body() as Map<*, *>
            body["status"] shouldBe "deleted"

            checkpointRepository.existsById(checkpoint.id!!) shouldBe false
        }

        "WORKER cannot delete route (forbidden)" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = "worker@route-delete-forbidden.com")

            val route = patrolRouteRepository.save(
                PatrolRoute(
                    organizationId = org.id!!,
                    siteId = site.id!!,
                    name = "Protected Route"
                )
            )

            try {
                client.toBlocking().exchange(
                    HttpRequest.DELETE<Any>("/api/admin/routes/${route.id}").bearerAuth(workerToken),
                    Map::class.java
                )
                throw AssertionError("Should have thrown HttpClientResponseException")
            } catch (e: HttpClientResponseException) {
                e.status shouldBe HttpStatus.FORBIDDEN
            }
        }
    }
}
