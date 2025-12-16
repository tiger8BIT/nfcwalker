package ge.tiger8bit.service

import ge.tiger8bit.domain.PatrolRoute
import ge.tiger8bit.domain.PatrolRouteCheckpoint
import ge.tiger8bit.dto.AddRouteCheckpointRequest
import ge.tiger8bit.dto.BulkAddRouteCheckpointsRequest
import ge.tiger8bit.dto.CreateRouteRequest
import ge.tiger8bit.dto.RouteResponse
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.PatrolRouteCheckpointRepository
import ge.tiger8bit.repository.PatrolRouteRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.util.*

@Singleton
open class PatrolRouteService(
    private val patrolRouteRepository: PatrolRouteRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
    private val accessService: AccessService
) {
    private val logger = getLogger()

    @Transactional
    open fun create(request: CreateRouteRequest, userId: UUID): RouteResponse {
        accessService.ensureBossOrAppOwner(userId, request.organizationId)

        logger.info("Creating route: name={}, orgId={}, siteId={}", request.name, request.organizationId, request.siteId)

        val route = patrolRouteRepository.save(
            PatrolRoute(
                organizationId = request.organizationId,
                siteId = request.siteId,
                name = request.name
            )
        )

        logger.info("Route created: id={}, name={}", route.id, route.name)
        return route.toResponse()
    }

    fun findById(id: UUID): PatrolRoute? {
        return patrolRouteRepository.findById(id).orElse(null)
    }

    @Transactional
    open fun addCheckpoints(routeId: UUID, request: BulkAddRouteCheckpointsRequest, userId: UUID): Int {
        val route = patrolRouteRepository.findById(routeId).orElseThrow {
            HttpStatusException(HttpStatus.NOT_FOUND, "Route not found")
        }
        accessService.ensureBossOrAppOwner(userId, route.organizationId)

        logger.info("Adding {} checkpoints to route: {}", request.checkpoints.size, routeId)

        val checkpoints = request.checkpoints.map { it.toEntity(routeId) }
        patrolRouteCheckpointRepository.saveAll(checkpoints)

        logger.info("Added {} route checkpoints to route: {}", checkpoints.size, routeId)
        return checkpoints.size
    }

    @Transactional
    open fun delete(id: UUID, userId: UUID): Boolean {
        val route = patrolRouteRepository.findById(id).orElseThrow {
            HttpStatusException(HttpStatus.NOT_FOUND, "Route not found")
        }
        accessService.ensureBossOrAppOwner(userId, route.organizationId)

        logger.info("Deleting route: {}", id)

        // First delete all route checkpoints (due to foreign key constraints)
        patrolRouteCheckpointRepository.deleteByRouteId(id)
        logger.info("Deleted route checkpoints for route: {}", id)

        // Then delete the route itself
        patrolRouteRepository.deleteById(id)
        logger.info("Route deleted: {}", id)

        return true
    }

    fun findRouteCheckpointsByRouteId(routeId: UUID): List<PatrolRouteCheckpoint> {
        return patrolRouteCheckpointRepository.findByRouteIdOrderBySeqAsc(routeId)
    }

    private fun PatrolRoute.toResponse() = RouteResponse(
        id = id!!,
        organizationId = organizationId,
        siteId = siteId,
        name = name
    )

    private fun AddRouteCheckpointRequest.toEntity(routeId: UUID) = PatrolRouteCheckpoint(
        routeId = routeId,
        checkpointId = checkpointId,
        seq = seq,
        minOffsetSec = minOffsetSec,
        maxOffsetSec = maxOffsetSec
    )
}

