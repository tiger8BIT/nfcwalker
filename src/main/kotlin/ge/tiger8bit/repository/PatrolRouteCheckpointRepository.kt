package ge.tiger8bit.repository

import ge.tiger8bit.domain.PatrolRouteCheckpoint
import ge.tiger8bit.domain.PatrolRouteCheckpointId
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.UUID

@Repository
interface PatrolRouteCheckpointRepository : JpaRepository<PatrolRouteCheckpoint, PatrolRouteCheckpointId> {
    fun findByRouteIdOrderBySeqAsc(routeId: UUID): List<PatrolRouteCheckpoint>
    fun deleteByRouteId(routeId: UUID)
    fun deleteByCheckpointId(checkpointId: UUID)
}

