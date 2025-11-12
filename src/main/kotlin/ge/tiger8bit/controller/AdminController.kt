package ge.tiger8bit.controller

import ge.tiger8bit.domain.Checkpoint
import ge.tiger8bit.domain.PatrolRoute
import ge.tiger8bit.domain.PatrolRouteCheckpoint
import ge.tiger8bit.dto.*
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.CheckpointRepository
import ge.tiger8bit.repository.PatrolRouteCheckpointRepository
import ge.tiger8bit.repository.PatrolRouteRepository
import ge.tiger8bit.service.AccessService
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import jakarta.transaction.Transactional
import java.security.Principal
import java.util.*

@Controller("/api/admin")
@Secured("ROLE_BOSS", "ROLE_APP_OWNER")
open class AdminController(
    private val checkpointRepository: CheckpointRepository,
    private val patrolRouteRepository: PatrolRouteRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
    private val accessService: AccessService
) {
    private val logger = getLogger()

    @Post("/checkpoints")
    @Transactional
    open fun createCheckpoint(@Body request: CreateCheckpointRequest, principal: Principal): CheckpointResponse {
        val userId = UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, request.organizationId)

        logger.info("Creating checkpoint: code={}, orgId={}, siteId={}", request.code, request.organizationId, request.siteId)

        val checkpoint = checkpointRepository.save(
            Checkpoint(
                organizationId = request.organizationId,
                siteId = request.siteId,
                code = request.code,
                geoLat = request.geoLat,
                geoLon = request.geoLon,
                radiusM = request.radiusM
            )
        )

        logger.info("Checkpoint created: id={}, code={}", checkpoint.id, checkpoint.code)
        return checkpoint.toResponse()
    }

    @Get("/checkpoints")
    fun listCheckpoints(@QueryValue siteId: UUID, principal: Principal): List<CheckpointResponse> {
        val checkpoints = checkpointRepository.findBySiteId(siteId)
        // If there are no checkpoints, just return empty (avoid 404). If present, check org of first.
        if (checkpoints.isNotEmpty()) {
            val userId = java.util.UUID.fromString(principal.name)
            accessService.ensureBossOrAppOwner(userId, checkpoints.first().organizationId)
        }
        return checkpoints.map { it.toResponse() }
    }

    @Post("/routes")
    @Transactional
    open fun createRoute(@Body request: CreateRouteRequest, principal: Principal): RouteResponse {
        val userId = java.util.UUID.fromString(principal.name)
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

    @Post("/routes/{id}/points")
    @Transactional
    open fun addCheckpointsToRoute(
        @PathVariable id: UUID,
        @Body request: BulkAddRouteCheckpointsRequest,
        principal: Principal
    ): Map<String, Any> {
        val route = patrolRouteRepository.findById(id).orElseThrow {
            io.micronaut.http.exceptions.HttpStatusException(
                io.micronaut.http.HttpStatus.NOT_FOUND,
                "Route not found"
            )
        }
        val userId = java.util.UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, route.organizationId)

        logger.info("Adding {} checkpoints to route: {}", request.checkpoints.size, id)

        val checkpoints = request.checkpoints.map { it.toEntity(id) }
        patrolRouteCheckpointRepository.saveAll(checkpoints)

        logger.info("Added {} route checkpoints to route: {}", checkpoints.size, id)
        return mapOf("status" to "updated", "added" to checkpoints.size)
    }

    @Delete("/routes/{id}")
    @Transactional
    open fun deleteRoute(@PathVariable id: UUID, principal: Principal): Map<String, String> {
        val route = patrolRouteRepository.findById(id).orElseThrow {
            io.micronaut.http.exceptions.HttpStatusException(
                io.micronaut.http.HttpStatus.NOT_FOUND,
                "Route not found"
            )
        }
        val userId = java.util.UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, route.organizationId)

        logger.info("Deleting route: {}", id)

        // First delete all route checkpoints (due to foreign key constraints)
        patrolRouteCheckpointRepository.deleteByRouteId(id)
        logger.info("Deleted route checkpoints for route: {}", id)

        // Then delete the route itself
        patrolRouteRepository.deleteById(id)
        logger.info("Route deleted: {}", id)

        return mapOf("status" to "deleted", "id" to id.toString())
    }

    @Delete("/checkpoints/{id}")
    @Transactional
    open fun deleteCheckpoint(@PathVariable id: UUID, principal: Principal): Map<String, String> {
        val checkpoint = checkpointRepository.findById(id).orElseThrow {
            io.micronaut.http.exceptions.HttpStatusException(
                io.micronaut.http.HttpStatus.NOT_FOUND,
                "Checkpoint not found"
            )
        }
        val userId = java.util.UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, checkpoint.organizationId)

        logger.info("Deleting checkpoint: {}", id)

        // First delete all route checkpoint associations
        patrolRouteCheckpointRepository.deleteByCheckpointId(id)
        logger.info("Deleted route associations for checkpoint: {}", id)

        // Then delete the checkpoint itself
        checkpointRepository.deleteById(id)
        logger.info("Checkpoint deleted: {}", id)

        return mapOf("status" to "deleted", "id" to id.toString())
    }

    // ===== Private Helpers =====

    private fun Checkpoint.toResponse() = CheckpointResponse(
        id = id!!,
        organizationId = organizationId,
        siteId = siteId,
        code = code,
        geoLat = geoLat,
        geoLon = geoLon,
        radiusM = radiusM
    )

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
