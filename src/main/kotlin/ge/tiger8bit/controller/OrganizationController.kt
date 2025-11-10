package ge.tiger8bit.controller

import ge.tiger8bit.domain.Organization
import ge.tiger8bit.dto.*
import ge.tiger8bit.repository.OrganizationRepository
import ge.tiger8bit.getLogger
import io.micronaut.http.annotation.*
import jakarta.transaction.Transactional
import io.micronaut.security.annotation.Secured
import java.util.UUID

@Controller("/api/organizations")
@Secured("ROLE_APP_OWNER")
open class OrganizationController(
    private val organizationRepository: OrganizationRepository
) {
    private val logger = getLogger()

    @Post
    @Transactional
    open fun createOrganization(@Body request: CreateOrganizationRequest): OrganizationResponse {
        logger.info("Creating organization: name={}", request.name)

        val organization = organizationRepository.save(
            Organization(
                name = request.name
            )
        )

        logger.info("Organization created: id={}, name={}", organization.id, organization.name)
        return organization.toResponse()
    }

    @Get
    fun listOrganizations(): List<OrganizationResponse> {
        logger.info("Listing all organizations")

        val organizations = organizationRepository.findAll()
        logger.info("Found {} organizations", organizations.size)

        return organizations.map { it.toResponse() }
    }

    @Get("/{id}")
    fun getOrganization(@PathVariable id: UUID): OrganizationResponse? {
        logger.info("Getting organization: {}", id)

        val organization = organizationRepository.findById(id).orElse(null)
        return organization?.toResponse()
    }

    @Put("/{id}")
    @Transactional
    open fun updateOrganization(
        @PathVariable id: UUID,
        @Body request: UpdateOrganizationRequest
    ): OrganizationResponse {
        logger.info("Updating organization: id={}, name={}", id, request.name)

        val organization = organizationRepository.findById(id).orElseThrow {
            IllegalArgumentException("Organization not found: $id")
        }

        organization.name = request.name

        val updated = organizationRepository.update(organization)
        logger.info("Organization updated: id={}, name={}", updated.id, updated.name)

        return updated.toResponse()
    }

    @Delete("/{id}")
    @Transactional
    open fun deleteOrganization(@PathVariable id: UUID): Map<String, Any> {
        logger.info("Deleting organization: {}", id)

        organizationRepository.deleteById(id)
        logger.info("Organization deleted: {}", id)

        return mapOf("deleted" to true, "id" to id)
    }

    // ===== Private Helpers =====

    private fun Organization.toResponse() = OrganizationResponse(
        id = id!!,
        name = name,
        createdAt = createdAt
    )
}

