package ge.tiger8bit.controller

import ge.tiger8bit.dto.RouteResponse
import ge.tiger8bit.service.PatrolRouteService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import java.security.Principal
import java.util.*

@Controller("/api/routes")
@Secured("ROLE_WORKER", "ROLE_BOSS", "ROLE_APP_OWNER")
class RouteController(private val patrolRouteService: PatrolRouteService) {

    @Get
    fun listRoutes(
        @QueryValue orgId: UUID?,
        @QueryValue siteId: UUID?,
        @QueryValue(defaultValue = "0") page: Int,
        @QueryValue(defaultValue = "20") size: Int,
        principal: Principal
    ): Page<RouteResponse> {
        val userId = UUID.fromString(principal.name)
        val pageable = Pageable.from(page, size)
        // For workers we might want to restrict this or use a different service method
        // but for now let's keep it consistent. PatrolRouteService already has access checks.
        return when {
            orgId != null -> patrolRouteService.findByOrganizationId(orgId, userId, pageable)
            siteId != null -> patrolRouteService.findBySiteId(siteId, userId, pageable)
            else -> Page.of(emptyList(), pageable, 0)
        }
    }

    @Get("/{id}")
    fun getRoute(@PathVariable id: UUID, principal: Principal): RouteResponse {
        val userId = UUID.fromString(principal.name)
        return patrolRouteService.getById(id, userId) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Route not found")
    }
}
