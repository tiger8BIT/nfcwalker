package ge.tiger8bit.repository

import ge.tiger8bit.domain.CheckpointSubCheck
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface CheckpointSubCheckRepository : JpaRepository<CheckpointSubCheck, UUID> {
    fun findByCheckpointId(checkpointId: UUID): List<CheckpointSubCheck>
    fun deleteByCheckpointId(checkpointId: UUID)
}
