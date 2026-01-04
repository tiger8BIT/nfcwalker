package ge.tiger8bit.repository

import ge.tiger8bit.domain.PatrolSubCheckEvent
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface PatrolSubCheckEventRepository : JpaRepository<PatrolSubCheckEvent, UUID> {
    fun findByScanEventId(scanEventId: UUID): List<PatrolSubCheckEvent>
}
