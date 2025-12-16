package ge.tiger8bit.controller

import ge.tiger8bit.dto.*
import ge.tiger8bit.service.OrganizationService
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import java.util.UUID

@Controller("/api/organizations")
@Secured("ROLE_APP_OWNER")
class OrganizationController(
    private val organizationService: OrganizationService
) {
    @Post
    fun createOrganization(@Body request: CreateOrganizationRequest): OrganizationResponse {
        return organizationService.create(request)
    }

    @Get
    fun listOrganizations(): List<OrganizationResponse> {
        return organizationService.findAll()
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
