package ge.tiger8bit.repository

import ge.tiger8bit.domain.Site
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface SiteRepository : JpaRepository<Site, Long> {
    fun findByOrganizationId(organizationId: Long): List<Site>
}

