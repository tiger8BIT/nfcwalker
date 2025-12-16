package ge.tiger8bit.controller

import ge.tiger8bit.dto.*
import ge.tiger8bit.service.CheckpointService
import ge.tiger8bit.service.OrganizationService
import ge.tiger8bit.service.PatrolRouteService
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import java.security.Principal
import java.util.*

@Controller("/api/admin")
@Secured("ROLE_BOSS", "ROLE_APP_OWNER")
class AdminController(
    private val checkpointService: CheckpointService,
    private val patrolRouteService: PatrolRouteService,
    private val organizationService: OrganizationService
) {
    @Post("/checkpoints")
    fun createCheckpoint(@Body request: CreateCheckpointRequest, principal: Principal): CheckpointResponse {
        val userId = UUID.fromString(principal.name)
        return checkpointService.create(request, userId)
    }

    @Get("/checkpoints")
    fun listCheckpoints(@QueryValue siteId: UUID, principal: Principal): List<CheckpointResponse> {
        val userId = UUID.fromString(principal.name)
        return checkpointService.findBySiteId(siteId, userId)
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

    @Post("/organizations")
    @Secured("ROLE_APP_OWNER")
    fun createOrganization(@Body request: CreateOrganizationRequest): OrganizationResponse {
        return organizationService.create(request)
    }
}
