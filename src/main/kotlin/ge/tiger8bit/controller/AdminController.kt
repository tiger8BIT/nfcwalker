package ge.tiger8bit.controller

import ge.tiger8bit.domain.Checkpoint
import ge.tiger8bit.domain.PatrolRoute
import ge.tiger8bit.domain.PatrolRouteCheckpoint
import ge.tiger8bit.dto.*
import ge.tiger8bit.repository.CheckpointRepository
import ge.tiger8bit.repository.PatrolRouteCheckpointRepository
import ge.tiger8bit.repository.PatrolRouteRepository
import io.micronaut.http.annotation.*
import jakarta.transaction.Transactional

@Controller("/api/admin")
open class AdminController(
    private val checkpointRepository: CheckpointRepository,
    private val patrolRouteRepository: PatrolRouteRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository
) {

    @Post("/checkpoints")
    @Transactional
    open fun createCheckpoint(@Body request: CreateCheckpointRequest): CheckpointResponse {
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

        return CheckpointResponse(
            id = checkpoint.id!!,
            organizationId = checkpoint.organizationId,
            siteId = checkpoint.siteId,
            code = checkpoint.code,
            geoLat = checkpoint.geoLat,
            geoLon = checkpoint.geoLon,
            radiusM = checkpoint.radiusM
        )
    }

    @Get("/checkpoints")
    fun listCheckpoints(@QueryValue siteId: Long): List<CheckpointResponse> {
        return checkpointRepository.findBySiteId(siteId).map { checkpoint ->
            CheckpointResponse(
                id = checkpoint.id!!,
                organizationId = checkpoint.organizationId,
                siteId = checkpoint.siteId,
                code = checkpoint.code,
                geoLat = checkpoint.geoLat,
                geoLon = checkpoint.geoLon,
                radiusM = checkpoint.radiusM
            )
        }
    }

    @Post("/routes")
    @Transactional
    open fun createRoute(@Body request: CreateRouteRequest): RouteResponse {
        val route = patrolRouteRepository.save(
            PatrolRoute(
                organizationId = request.organizationId,
                siteId = request.siteId,
                name = request.name
            )
        )

        return RouteResponse(
            id = route.id!!,
            organizationId = route.organizationId,
            siteId = route.siteId,
            name = route.name
        )
    }

    @Post("/routes/{id}/points")
    @Transactional
    open fun addCheckpointsToRoute(
        @PathVariable id: Long,
        @Body request: BulkAddRouteCheckpointsRequest
    ): Map<String, Any> {
        val checkpoints = request.checkpoints.map { cp ->
            PatrolRouteCheckpoint().apply {
                routeId = id
                checkpointId = cp.checkpointId
                seq = cp.seq
                minOffsetSec = cp.minOffsetSec
                maxOffsetSec = cp.maxOffsetSec
            }
        }

        patrolRouteCheckpointRepository.saveAll(checkpoints)

        return mapOf("added" to checkpoints.size)
    }
}

