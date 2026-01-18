package ge.tiger8bit.repository

import ge.tiger8bit.domain.Incident
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface IncidentRepository : JpaRepository<Incident, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<Incident>
    fun findBySiteId(siteId: UUID): List<Incident>
    fun findByScanEventId(scanEventId: UUID): List<Incident>

    @io.micronaut.data.annotation.Query(
        value = "SELECT i FROM Incident i WHERE i.organizationId = :organizationId " +
                "AND (cast(:siteId as uuid) IS NULL OR i.siteId = :siteId) " +
                "AND (cast(:status as string) IS NULL OR i.status = :status) " +
                "ORDER BY i.createdAt DESC",
        countQuery = "SELECT COUNT(i) FROM Incident i WHERE i.organizationId = :organizationId " +
                "AND (cast(:siteId as uuid) IS NULL OR i.siteId = :siteId) " +
                "AND (cast(:status as string) IS NULL OR i.status = :status)"
    )
    fun findPaginated(
        organizationId: UUID,
        siteId: UUID?,
        status: String?,
        pageable: io.micronaut.data.model.Pageable
    ): io.micronaut.data.model.Page<Incident>
}
