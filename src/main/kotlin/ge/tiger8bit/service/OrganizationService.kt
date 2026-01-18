package ge.tiger8bit.service

import ge.tiger8bit.domain.Organization
import ge.tiger8bit.dto.CreateOrganizationRequest
import ge.tiger8bit.dto.OrganizationResponse
import ge.tiger8bit.dto.UpdateOrganizationRequest
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.OrganizationRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.util.*

@Singleton
open class OrganizationService(
    private val organizationRepository: OrganizationRepository
) {
    private val logger = getLogger()

    @Transactional
    open fun create(request: CreateOrganizationRequest): OrganizationResponse {
        logger.info("Creating organization: name={}", request.name)

        val organization = organizationRepository.save(
            Organization(name = request.name)
        )

        logger.info("Organization created: id={}, name={}", organization.id, organization.name)
        return organization.toResponse()
    }

    fun findAll(pageable: Pageable): Page<OrganizationResponse> {
        logger.info("Listing organizations page={}, size={}", pageable.number, pageable.size)
        val pageOrgs = organizationRepository.findAll(pageable)
        logger.info("Found {} total organizations", pageOrgs.totalSize)
        return Page.of(pageOrgs.content.map { it.toResponse() }, pageOrgs.pageable, pageOrgs.totalSize)
    }

    fun findById(id: UUID): OrganizationResponse? {
        logger.info("Getting organization: {}", id)
        return organizationRepository.findById(id).orElse(null)?.toResponse()
    }

    @Transactional
    open fun update(id: UUID, request: UpdateOrganizationRequest): OrganizationResponse {
        logger.info("Updating organization: id={}, name={}", id, request.name)

        val organization = organizationRepository.findById(id).orElseThrow {
            IllegalArgumentException("Organization not found: $id")
        }

        organization.name = request.name
        val updated = organizationRepository.update(organization)

        logger.info("Organization updated: id={}, name={}", updated.id, updated.name)
        return updated.toResponse()
    }

    @Transactional
    open fun delete(id: UUID): Boolean {
        logger.info("Deleting organization: {}", id)
        organizationRepository.deleteById(id)
        logger.info("Organization deleted: {}", id)
        return true
    }

    private fun Organization.toResponse() = OrganizationResponse(
        id = id!!,
        name = name,
        createdAt = createdAt
    )
}

