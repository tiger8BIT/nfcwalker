package ge.tiger8bit.spec

import ge.tiger8bit.TestAuth
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.Checkpoint
import ge.tiger8bit.domain.PatrolRoute
import ge.tiger8bit.domain.PatrolRouteCheckpoint
import ge.tiger8bit.repository.CheckpointRepository
import ge.tiger8bit.repository.OrganizationRepository
import ge.tiger8bit.repository.PatrolRouteCheckpointRepository
import ge.tiger8bit.repository.PatrolRouteRepository
import ge.tiger8bit.repository.SiteRepository
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
    @Inject @Client("/") private val client: HttpClient,
    @Inject private val organizationRepository: OrganizationRepository,
    @Inject private val siteRepository: SiteRepository,
    @Inject private val checkpointRepository: CheckpointRepository,
    @Inject private val patrolRouteRepository: PatrolRouteRepository,
    @Inject private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository
) : StringSpec({

    "DELETE /api/admin/routes/{id} should delete route and its checkpoints" {
        // Arrange
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

        // Create route
        val route = patrolRouteRepository.save(
            PatrolRoute(
                organizationId = org.id!!,
                siteId = site.id!!,
                name = "Test Route for Delete"
            )
        )

        // Create checkpoints
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

        // Add checkpoints to route
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

        // Verify route and checkpoints exist
        patrolRouteRepository.existsById(route.id!!) shouldBe true
        patrolRouteCheckpointRepository.findByRouteIdOrderBySeqAsc(route.id!!).size shouldBe 2

        // Act
        val token = TestAuth.generateBossToken()
        val request = HttpRequest.DELETE<Any>("/api/admin/routes/${route.id}")
            .bearerAuth(token)

        val response = client.toBlocking().exchange(request, Map::class.java)

        // Assert
        response.status shouldBe HttpStatus.OK
        val body = response.body() as Map<*, *>
        body["status"] shouldBe "deleted"
        body["id"] shouldBe route.id.toString()

        // Verify route is deleted
        patrolRouteRepository.existsById(route.id!!) shouldBe false

        // Verify route checkpoints are deleted
        patrolRouteCheckpointRepository.findByRouteIdOrderBySeqAsc(route.id!!).size shouldBe 0

        // Verify checkpoints still exist (not cascade deleted)
        checkpointRepository.existsById(checkpoint1.id!!) shouldBe true
        checkpointRepository.existsById(checkpoint2.id!!) shouldBe true
    }

    "DELETE /api/admin/routes/{id} should return 404 for non-existent route" {
        // Arrange
        val nonExistentId = java.util.UUID.randomUUID()
        val token = TestAuth.generateBossToken()

        // Act & Assert
        val request = HttpRequest.DELETE<Any>("/api/admin/routes/$nonExistentId")
            .bearerAuth(token)

        try {
            client.toBlocking().exchange(request, Map::class.java)
            throw AssertionError("Should have thrown HttpClientResponseException")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.NOT_FOUND
        }
    }

    "DELETE /api/admin/checkpoints/{id} should delete checkpoint and its route associations" {
        // Arrange
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)

        // Create checkpoint
        val checkpoint = checkpointRepository.save(
            Checkpoint(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "DELETE_CP_TEST"
            )
        )

        // Create routes
        val route1 = patrolRouteRepository.save(
            PatrolRoute(
                organizationId = org.id!!,
                siteId = site.id!!,
                name = "Route 1"
            )
        )
        val route2 = patrolRouteRepository.save(
            PatrolRoute(
                organizationId = org.id!!,
                siteId = site.id!!,
                name = "Route 2"
            )
        )

        // Add checkpoint to both routes
        patrolRouteCheckpointRepository.save(
            PatrolRouteCheckpoint(
                routeId = route1.id!!,
                checkpointId = checkpoint.id!!,
                seq = 1
            )
        )
        patrolRouteCheckpointRepository.save(
            PatrolRouteCheckpoint(
                routeId = route2.id!!,
                checkpointId = checkpoint.id!!,
                seq = 1
            )
        )

        // Verify checkpoint exists
        checkpointRepository.existsById(checkpoint.id!!) shouldBe true

        // Act
        val token = TestAuth.generateBossToken()
        val request = HttpRequest.DELETE<Any>("/api/admin/checkpoints/${checkpoint.id}")
            .bearerAuth(token)

        val response = client.toBlocking().exchange(request, Map::class.java)

        // Assert
        response.status shouldBe HttpStatus.OK
        val body = response.body() as Map<*, *>
        body["status"] shouldBe "deleted"
        body["id"] shouldBe checkpoint.id.toString()

        // Verify checkpoint is deleted
        checkpointRepository.existsById(checkpoint.id!!) shouldBe false

        // Verify routes still exist (not cascade deleted)
        patrolRouteRepository.existsById(route1.id!!) shouldBe true
        patrolRouteRepository.existsById(route2.id!!) shouldBe true
    }

    "DELETE /api/admin/checkpoints/{id} should return 404 for non-existent checkpoint" {
        // Arrange
        val nonExistentId = java.util.UUID.randomUUID()
        val token = TestAuth.generateBossToken()

        // Act & Assert
        val request = HttpRequest.DELETE<Any>("/api/admin/checkpoints/$nonExistentId")
            .bearerAuth(token)

        try {
            client.toBlocking().exchange(request, Map::class.java)
            throw AssertionError("Should have thrown HttpClientResponseException")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.NOT_FOUND
        }
    }

    "DELETE /api/admin/routes/{id} should require ROLE_BOSS" {
        // Arrange
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
        val route = patrolRouteRepository.save(
            PatrolRoute(
                organizationId = org.id!!,
                siteId = site.id!!,
                name = "Protected Route"
            )
        )

        // Act & Assert - WORKER should be forbidden
        val workerToken = TestAuth.generateWorkerToken()
        val workerRequest = HttpRequest.DELETE<Any>("/api/admin/routes/${route.id}")
            .bearerAuth(workerToken)

        try {
            client.toBlocking().exchange(workerRequest, Map::class.java)
            throw AssertionError("Should have thrown HttpClientResponseException")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.FORBIDDEN
        }
    }

    "DELETE /api/admin/checkpoints/{id} should require ROLE_BOSS" {
        // Arrange
        val (org, site) = TestFixtures.seedOrgAndSite(organizationRepository, siteRepository)
        val checkpoint = checkpointRepository.save(
            Checkpoint(
                organizationId = org.id!!,
                siteId = site.id!!,
                code = "PROTECTED_CP"
            )
        )

        // Act & Assert - WORKER should be forbidden
        val workerToken = TestAuth.generateWorkerToken()
        val workerRequest = HttpRequest.DELETE<Any>("/api/admin/checkpoints/${checkpoint.id}")
            .bearerAuth(workerToken)

        try {
            client.toBlocking().exchange(workerRequest, Map::class.java)
            throw AssertionError("Should have thrown HttpClientResponseException")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.FORBIDDEN
        }
    }
})

