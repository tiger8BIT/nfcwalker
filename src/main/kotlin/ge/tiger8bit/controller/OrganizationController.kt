package ge.tiger8bit.controller

import ge.tiger8bit.dto.CreateOrganizationRequest
import ge.tiger8bit.dto.OrganizationResponse
import ge.tiger8bit.dto.UpdateOrganizationRequest
import ge.tiger8bit.service.OrganizationService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import java.util.*

@Controller("/api/organizations")
@Secured("ROLE_BOSS", "ROLE_APP_OWNER")
class OrganizationController(
    private val organizationService: OrganizationService
) {
    @Post
    fun createOrganization(@Body request: CreateOrganizationRequest): OrganizationResponse {
        return organizationService.create(request)
    }

    @Get
    fun listOrganizations(
        @QueryValue(defaultValue = "0") page: Int,
        @QueryValue(defaultValue = "20") size: Int
    ): Page<OrganizationResponse> {
        val pageable = Pageable.from(page, size)
        return organizationService.findAll(pageable)
    }

    @Get("/{id}")
    fun getOrganization(@PathVariable id: UUID): OrganizationResponse? {
        return organizationService.findById(id)
    }

    @Put("/{id}")
    fun updateOrganization(
        @PathVariable id: UUID,
        @Body request: UpdateOrganizationRequest
    ): OrganizationResponse {
        return organizationService.update(id, request)
    }

    @Delete("/{id}")
    fun deleteOrganization(@PathVariable id: UUID): Map<String, Any> {
        organizationService.delete(id)
        return mapOf("deleted" to true, "id" to id)
    }
}
