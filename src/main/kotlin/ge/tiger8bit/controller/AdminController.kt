package ge.tiger8bit.controller

import ge.tiger8bit.dto.*
import ge.tiger8bit.service.CheckpointService
import ge.tiger8bit.service.PatrolRouteService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import java.security.Principal
import java.util.*

@Controller("/api/admin")
@Secured("ROLE_BOSS", "ROLE_APP_OWNER")
class AdminController(
    private val checkpointService: CheckpointService,
    private val patrolRouteService: PatrolRouteService,
    private val accessService: ge.tiger8bit.service.AccessService
) {
    @Post("/checkpoints")
    fun createCheckpoint(@Body request: CreateCheckpointRequest, principal: Principal): CheckpointResponse {
        val userId = UUID.fromString(principal.name)
        return checkpointService.create(request, userId)
    }

    @Get("/checkpoints")
    fun listCheckpoints(
        @QueryValue siteId: UUID?,
        @QueryValue orgId: UUID?,
        @QueryValue(defaultValue = "0") page: Int,
        @QueryValue(defaultValue = "20") size: Int,
        principal: Principal
    ): Page<CheckpointResponse> {
        val userId = UUID.fromString(principal.name)
        val pageable = Pageable.from(page, size)
        return when {
            siteId != null -> checkpointService.findBySiteId(siteId, pageable, userId)
            orgId != null -> checkpointService.findByOrganizationId(orgId, pageable, userId)
            else -> Page.of(emptyList(), pageable, 0L)
        }
    }

    @Put("/checkpoints/{id}")
    fun updateCheckpoint(
        @PathVariable id: UUID,
        @Body request: UpdateCheckpointRequest,
        principal: Principal
    ): CheckpointResponse {
        val userId = UUID.fromString(principal.name)
        return checkpointService.update(id, request, userId)
    }

    @Delete("/checkpoints/{id}")
    fun deleteCheckpoint(@PathVariable id: UUID, principal: Principal): Map<String, String> {
        val userId = UUID.fromString(principal.name)
        checkpointService.delete(id, userId)
        return mapOf("status" to "deleted", "id" to id.toString())
    }

    @Post("/routes")
    fun createRoute(@Body request: CreateRouteRequest, principal: Principal): RouteResponse {
        val userId = UUID.fromString(principal.name)
        return patrolRouteService.create(request, userId)
    }

    @Post("/routes/{id}/points")
    fun addCheckpointsToRoute(
        @PathVariable id: UUID,
        @Body request: BulkAddRouteCheckpointsRequest,
        principal: Principal
    ): Map<String, Any> {
        val userId = UUID.fromString(principal.name)
        val addedCount = patrolRouteService.addCheckpoints(id, request, userId)
        return mapOf("status" to "updated", "added" to addedCount)
    }

    @Delete("/routes/{id}")
    fun deleteRoute(@PathVariable id: UUID, principal: Principal): Map<String, String> {
        val userId = UUID.fromString(principal.name)
        patrolRouteService.delete(id, userId)
        return mapOf("status" to "deleted", "id" to id.toString())
    }

    @Get("/routes/{id}")
    fun getRoute(@PathVariable id: UUID, principal: Principal): RouteResponse {
        val userId = UUID.fromString(principal.name)
        val route = patrolRouteService.findById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Route not found")
        accessService.ensureBossOrAppOwner(userId, route.organizationId)
        return route.toResponse()
    }

    @Put("/routes/{id}")
    fun updateRoute(@PathVariable id: UUID, @Body request: UpdateRouteRequest, principal: Principal): RouteResponse {
        val userId = UUID.fromString(principal.name)
        return patrolRouteService.update(id, request, userId)
    }

    @Get("/routes")
    fun listRoutes(
        @QueryValue orgId: UUID?,
        @QueryValue siteId: UUID?,
        @QueryValue(defaultValue = "0") page: Int,
        @QueryValue(defaultValue = "20") size: Int,
        principal: Principal
    ): Page<RouteResponse> {
        val userId = UUID.fromString(principal.name)
        val pageable = Pageable.from(page, size)
        return when {
            orgId != null -> patrolRouteService.findByOrganizationId(orgId, userId, pageable)
            siteId != null -> patrolRouteService.findBySiteId(siteId, userId, pageable)
            else -> Page.of(emptyList(), pageable, 0)
        }
    }

    @Get("/checkpoints/{id}")
    fun getCheckpoint(@PathVariable id: UUID, principal: Principal): CheckpointResponse {
        val userId = UUID.fromString(principal.name)
        val checkpoint = checkpointService.findById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Checkpoint not found")
        // Check access via site/org
        checkpointService.findBySiteId(checkpoint.siteId, Pageable.from(0, 1), userId)
        val subChecks = checkpointService.findSubChecks(id)
        return checkpoint.toResponse(subChecks)
    }
}

private fun ge.tiger8bit.domain.PatrolRoute.toResponse() = RouteResponse(
    id = id!!,
    organizationId = organizationId,
    siteId = siteId,
    name = name
)

private fun ge.tiger8bit.domain.Checkpoint.toResponse(subChecks: List<ge.tiger8bit.domain.CheckpointSubCheck> = emptyList()) =
    CheckpointResponse(
        id = id!!,
        organizationId = organizationId,
        siteId = siteId,
        code = code,
        geoLat = geoLat,
        geoLon = geoLon,
        radiusM = radiusM,
        label = label,
        detailsConfig = detailsConfig,
        subChecks = subChecks.map { it.toResponse() }
    )

private fun ge.tiger8bit.domain.CheckpointSubCheck.toResponse() = SubCheckResponse(
    id = id!!,
    label = label,
    description = description,
    requirePhoto = requirePhoto,
    allowNotes = allowNotes
)
