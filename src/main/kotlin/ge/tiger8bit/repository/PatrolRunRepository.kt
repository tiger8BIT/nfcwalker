package ge.tiger8bit.repository

import ge.tiger8bit.domain.PatrolRun
import ge.tiger8bit.dto.PatrolRunStatus
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface PatrolRunRepository : JpaRepository<PatrolRun, UUID> {
    fun findByOrganizationIdAndStatusIn(orgId: UUID, statuses: List<PatrolRunStatus>): List<PatrolRun>
}
