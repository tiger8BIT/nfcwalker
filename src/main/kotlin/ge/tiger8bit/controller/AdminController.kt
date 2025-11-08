package ge.tiger8bit.controller

import ge.tiger8bit.domain.Checkpoint
import ge.tiger8bit.domain.PatrolRoute
import ge.tiger8bit.domain.PatrolRouteCheckpoint
import ge.tiger8bit.dto.*
import ge.tiger8bit.repository.CheckpointRepository
import ge.tiger8bit.repository.PatrolRouteCheckpointRepository
import ge.tiger8bit.repository.PatrolRouteRepository
import ge.tiger8bit.getLogger
import io.micronaut.http.annotation.*
import jakarta.transaction.Transactional
import io.micronaut.security.annotation.Secured
import java.util.UUID

@Controller("/api/admin")
@Secured("ROLE_BOSS")
open class AdminController(
    private val checkpointRepository: CheckpointRepository,
    private val patrolRouteRepository: PatrolRouteRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository
) {
    private val logger = getLogger()

    @Post("/checkpoints")
    @Transactional
    open fun createCheckpoint(@Body request: CreateCheckpointRequest): CheckpointResponse {
        logger.debug("Creating checkpoint: code={}, orgId={}, siteId={}", request.code, request.organizationId, request.siteId)

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
    fun listCheckpoints(@QueryValue siteId: UUID): List<CheckpointResponse> {
        logger.debug("Listing checkpoints for site: {}", siteId)

        val checkpoints = checkpointRepository.findBySiteId(siteId)
        logger.info("Found {} checkpoints in site: {}", checkpoints.size, siteId)

        return checkpoints.map { it.toResponse() }
    }

    @Post("/routes")
    @Transactional
    open fun createRoute(@Body request: CreateRouteRequest): RouteResponse {
        logger.debug("Creating route: name={}, orgId={}, siteId={}", request.name, request.organizationId, request.siteId)

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
        @Body request: BulkAddRouteCheckpointsRequest
    ): Map<String, Any> {
        logger.debug("Adding {} checkpoints to route: {}", request.checkpoints.size, id)

        val checkpoints = request.checkpoints.map { it.toEntity(id) }
        patrolRouteCheckpointRepository.saveAll(checkpoints)

        logger.info("Added {} route checkpoints to route: {}", checkpoints.size, id)
        return mapOf("added" to checkpoints.size)
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
