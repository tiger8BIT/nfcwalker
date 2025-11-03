package ge.tiger8bit.repository

import ge.tiger8bit.domain.Checkpoint
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface CheckpointRepository : JpaRepository<Checkpoint, Long> {
    fun findBySiteId(siteId: Long): List<Checkpoint>
    fun findByCode(code: String): Optional<Checkpoint>
}

