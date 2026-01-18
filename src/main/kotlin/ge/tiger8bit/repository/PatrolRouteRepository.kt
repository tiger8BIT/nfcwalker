package ge.tiger8bit.repository

import ge.tiger8bit.domain.PatrolRoute
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

@Repository
interface PatrolRouteRepository : JpaRepository<PatrolRoute, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<PatrolRoute>
    fun findBySiteId(siteId: UUID): List<PatrolRoute>

    @Query(
        value = "SELECT r FROM PatrolRoute r WHERE r.siteId = :siteId ORDER BY r.createdAt DESC",
        countQuery = "SELECT COUNT(r) FROM PatrolRoute r WHERE r.siteId = :siteId"
    )
    fun findBySiteIdPaginated(siteId: UUID, pageable: Pageable): Page<PatrolRoute>

    @Query(
        value = "SELECT r FROM PatrolRoute r WHERE r.organizationId = :organizationId ORDER BY r.createdAt DESC",
        countQuery = "SELECT COUNT(r) FROM PatrolRoute r WHERE r.organizationId = :organizationId"
    )
    fun findByOrganizationIdPaginated(organizationId: UUID, pageable: Pageable): Page<PatrolRoute>
}

