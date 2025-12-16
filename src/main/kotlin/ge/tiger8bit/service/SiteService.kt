package ge.tiger8bit.service

import ge.tiger8bit.domain.Site
import ge.tiger8bit.dto.CreateSiteRequest
import ge.tiger8bit.dto.SiteResponse
import ge.tiger8bit.dto.UpdateSiteRequest
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.SiteRepository
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.util.*

@Singleton
open class SiteService(
    private val siteRepository: SiteRepository,
    private val accessService: AccessService
) {
    private val logger = getLogger()

    @Transactional
    open fun create(request: CreateSiteRequest, userId: UUID): SiteResponse {
        accessService.ensureBossOrAppOwner(userId, request.organizationId)

        logger.info("Creating site: name={}, organizationId={}", request.name, request.organizationId)

        val site = siteRepository.save(
            Site(
                organizationId = request.organizationId,
                name = request.name
            )
        )

        logger.info("Site created: id={}, name={}", site.id, site.name)
        return site.toResponse()
    }

    fun findByOrganizationId(organizationId: UUID, userId: UUID): List<SiteResponse> {
        accessService.ensureBossOrAppOwner(userId, organizationId)

        logger.info("Listing sites for organization: {}", organizationId)

        val sites = siteRepository.findByOrganizationId(organizationId)
        logger.info("Found {} sites in organization: {}", sites.size, organizationId)

        return sites.map { it.toResponse() }
    }

    fun findById(id: UUID, userId: UUID): SiteResponse? {
        val site = siteRepository.findById(id).orElse(null) ?: return null
        accessService.ensureBossOrAppOwner(userId, site.organizationId)

        logger.info("Getting site: {}", id)
        return site.toResponse()
    }

    @Transactional
    open fun update(id: UUID, request: UpdateSiteRequest, userId: UUID): SiteResponse {
        val site = siteRepository.findById(id).orElseThrow {
            IllegalArgumentException("Site not found: $id")
        }
        accessService.ensureBossOrAppOwner(userId, site.organizationId)

        logger.info("Updating site: id={}, name={}", id, request.name)

        site.name = request.name
        val updated = siteRepository.update(site)

        logger.info("Site updated: id={}, name={}", updated.id, updated.name)
        return updated.toResponse()
    }

    @Transactional
    open fun delete(id: UUID, userId: UUID): Boolean {
        val site = siteRepository.findById(id).orElseThrow {
            IllegalArgumentException("Site not found: $id")
        }
        accessService.ensureBossOrAppOwner(userId, site.organizationId)

        logger.info("Deleting site: {}", id)
        siteRepository.deleteById(id)
        logger.info("Site deleted: {}", id)

        return true
    }

    private fun Site.toResponse() = SiteResponse(
        id = id!!,
        organizationId = organizationId,
        name = name,
        createdAt = createdAt
    )
}

