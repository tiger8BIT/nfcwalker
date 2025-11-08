package ge.tiger8bit.repository

import ge.tiger8bit.domain.PatrolScanEvent
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.UUID

@Repository
interface PatrolScanEventRepository : JpaRepository<PatrolScanEvent, UUID> {
    fun findByPatrolRunId(patrolRunId: UUID): List<PatrolScanEvent>
}

