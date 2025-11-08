package ge.tiger8bit.repository

import ge.tiger8bit.domain.Site
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.UUID

@Repository
interface SiteRepository : JpaRepository<Site, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<Site>
}

