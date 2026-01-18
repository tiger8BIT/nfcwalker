package ge.tiger8bit.repository

import ge.tiger8bit.domain.Checkpoint
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

@Repository
interface CheckpointRepository : JpaRepository<Checkpoint, UUID> {
    fun findBySiteId(siteId: UUID): List<Checkpoint>
    fun findByCode(code: String): java.util.Optional<Checkpoint>
    fun update(checkpoint: Checkpoint): Checkpoint
    fun findByOrganizationId(organizationId: UUID): List<Checkpoint>

    @Query(
        value = "SELECT c FROM Checkpoint c WHERE c.organizationId = :organizationId ORDER BY c.createdAt DESC",
        countQuery = "SELECT COUNT(c) FROM Checkpoint c WHERE c.organizationId = :organizationId"
    )
    fun findByOrganizationIdPaginated(organizationId: UUID, pageable: Pageable): Page<Checkpoint>

    @Query(
        value = "SELECT c FROM Checkpoint c WHERE c.siteId = :siteId ORDER BY c.createdAt DESC",
        countQuery = "SELECT COUNT(c) FROM Checkpoint c WHERE c.siteId = :siteId"
    )
    fun findBySiteIdPaginated(siteId: UUID, pageable: Pageable): Page<Checkpoint>
}

