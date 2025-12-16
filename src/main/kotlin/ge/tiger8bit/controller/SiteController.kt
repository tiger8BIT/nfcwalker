package ge.tiger8bit.controller

import ge.tiger8bit.dto.CreateSiteRequest
import ge.tiger8bit.dto.SiteResponse
import ge.tiger8bit.dto.UpdateSiteRequest
import ge.tiger8bit.service.SiteService
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import java.security.Principal
import java.util.*

@Controller("/api/sites")
@Secured("ROLE_BOSS", "ROLE_APP_OWNER")
class SiteController(
    private val siteService: SiteService
) {
    @Post
    fun createSite(@Body request: CreateSiteRequest, principal: Principal): SiteResponse {
        val userId = UUID.fromString(principal.name)
        return siteService.create(request, userId)
    }

    @Get
    fun listSites(@QueryValue organizationId: UUID, principal: Principal): List<SiteResponse> {
        val userId = UUID.fromString(principal.name)
        return siteService.findByOrganizationId(organizationId, userId)
    }

    @Get("/{id}")
    fun getSite(@PathVariable id: UUID, principal: Principal): SiteResponse? {
        val userId = UUID.fromString(principal.name)
        return siteService.findById(id, userId)
    }

    @Put("/{id}")
    fun updateSite(
        @PathVariable id: UUID,
        @Body request: UpdateSiteRequest,
        principal: Principal
    ): SiteResponse {
        val userId = UUID.fromString(principal.name)
        return siteService.update(id, request, userId)
    }

    @Delete("/{id}")
    fun deleteSite(@PathVariable id: UUID, principal: Principal): Map<String, Any> {
        val userId = UUID.fromString(principal.name)
        siteService.delete(id, userId)
        return mapOf("deleted" to true, "id" to id)
    }
}
