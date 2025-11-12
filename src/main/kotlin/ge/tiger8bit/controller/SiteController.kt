package ge.tiger8bit.controller

import ge.tiger8bit.domain.Site
import ge.tiger8bit.dto.CreateSiteRequest
import ge.tiger8bit.dto.SiteResponse
import ge.tiger8bit.dto.UpdateSiteRequest
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.SiteRepository
import ge.tiger8bit.service.AccessService
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import jakarta.transaction.Transactional
import java.security.Principal
import java.util.*

@Controller("/api/sites")
@Secured("ROLE_BOSS", "ROLE_APP_OWNER")
open class SiteController(
    private val siteRepository: SiteRepository,
    private val accessService: AccessService
) {
    private val logger = getLogger()

    @Post
    @Transactional
    open fun createSite(@Body request: CreateSiteRequest, principal: Principal): SiteResponse {
        val userId = UUID.fromString(principal.name)
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

    @Get
    fun listSites(@QueryValue organizationId: UUID, principal: Principal): List<SiteResponse> {
        val userId = UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, organizationId)

        logger.info("Listing sites for organization: {}", organizationId)

        val sites = siteRepository.findByOrganizationId(organizationId)
        logger.info("Found {} sites in organization: {}", sites.size, organizationId)

        return sites.map { it.toResponse() }
    }

    @Get("/{id}")
    fun getSite(@PathVariable id: UUID, principal: Principal): SiteResponse? {
        val site = siteRepository.findById(id).orElse(null) ?: return null
        val userId = UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, site.organizationId)

        logger.info("Getting site: {}", id)

        return site.toResponse()
    }

    @Put("/{id}")
    @Transactional
    open fun updateSite(
        @PathVariable id: UUID,
        @Body request: UpdateSiteRequest,
        principal: Principal
    ): SiteResponse {
        val site = siteRepository.findById(id).orElseThrow { IllegalArgumentException("Site not found: $id") }
        val userId = UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, site.organizationId)

        logger.info("Updating site: id={}, name={}", id, request.name)

        site.name = request.name

        val updated = siteRepository.update(site)
        logger.info("Site updated: id={}, name={}", updated.id, updated.name)

        return updated.toResponse()
    }

    @Delete("/{id}")
    @Transactional
    open fun deleteSite(@PathVariable id: UUID, principal: Principal): Map<String, Any> {
        val site = siteRepository.findById(id).orElseThrow { IllegalArgumentException("Site not found: $id") }
        val userId = UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, site.organizationId)

        logger.info("Deleting site: {}", id)

        siteRepository.deleteById(id)
        logger.info("Site deleted: {}", id)

        return mapOf("deleted" to true, "id" to id)
    }

    // ===== Private Helpers =====

    private fun Site.toResponse() = SiteResponse(
        id = id!!,
        organizationId = organizationId,
        name = name,
        createdAt = createdAt
    )
}
