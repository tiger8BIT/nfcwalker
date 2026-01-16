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
}
