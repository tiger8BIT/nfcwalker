package ge.tiger8bit.controller

import ge.tiger8bit.domain.Site
import ge.tiger8bit.dto.*
import ge.tiger8bit.repository.SiteRepository
import ge.tiger8bit.getLogger
import io.micronaut.http.annotation.*
import jakarta.transaction.Transactional
import io.micronaut.security.annotation.Secured
import java.util.UUID

@Controller("/api/sites")
@Secured("ROLE_BOSS")
open class SiteController(
    private val siteRepository: SiteRepository
) {
    private val logger = getLogger()

    @Post
    @Transactional
    open fun createSite(@Body request: CreateSiteRequest): SiteResponse {
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
    fun listSites(@QueryValue organizationId: UUID): List<SiteResponse> {
        logger.info("Listing sites for organization: {}", organizationId)

        val sites = siteRepository.findByOrganizationId(organizationId)
        logger.info("Found {} sites in organization: {}", sites.size, organizationId)

        return sites.map { it.toResponse() }
    }

    @Get("/{id}")
    fun getSite(@PathVariable id: UUID): SiteResponse? {
        logger.info("Getting site: {}", id)

        val site = siteRepository.findById(id).orElse(null)
        return site?.toResponse()
    }

    @Put("/{id}")
    @Transactional
    open fun updateSite(
        @PathVariable id: UUID,
        @Body request: UpdateSiteRequest
    ): SiteResponse {
        logger.info("Updating site: id={}, name={}", id, request.name)

        val site = siteRepository.findById(id).orElseThrow {
            IllegalArgumentException("Site not found: $id")
        }

        site.name = request.name

        val updated = siteRepository.update(site)
        logger.info("Site updated: id={}, name={}", updated.id, updated.name)

        return updated.toResponse()
    }

    @Delete("/{id}")
    @Transactional
    open fun deleteSite(@PathVariable id: UUID): Map<String, Any> {
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

