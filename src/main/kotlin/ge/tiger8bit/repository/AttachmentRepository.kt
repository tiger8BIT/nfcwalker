package ge.tiger8bit.repository

import ge.tiger8bit.domain.Attachment
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface AttachmentRepository : JpaRepository<Attachment, UUID> {
    fun findByEntityTypeAndEntityId(entityType: String, entityId: UUID): List<Attachment>
}
