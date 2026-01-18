package ge.tiger8bit.repository

import ge.tiger8bit.domain.Site
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

@Repository
interface SiteRepository : JpaRepository<Site, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<Site>

    @Query(
        value = "SELECT s FROM Site s WHERE s.organizationId = :organizationId ORDER BY s.createdAt DESC",
        countQuery = "SELECT COUNT(s) FROM Site s WHERE s.organizationId = :organizationId"
    )
    fun findByOrganizationIdPaginated(organizationId: UUID, pageable: Pageable): Page<Site>
}

