package ge.tiger8bit.repository

import ge.tiger8bit.domain.PatrolScanEvent
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface PatrolScanEventRepository : JpaRepository<PatrolScanEvent, Long> {
    fun findByPatrolRunId(patrolRunId: Long): List<PatrolScanEvent>
}

