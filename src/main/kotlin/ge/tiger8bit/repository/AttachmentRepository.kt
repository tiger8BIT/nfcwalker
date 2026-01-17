package ge.tiger8bit.repository

import ge.tiger8bit.domain.Attachment
import ge.tiger8bit.domain.AttachmentEntityType
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface AttachmentRepository : JpaRepository<Attachment, UUID> {
    fun findByEntityTypeAndEntityId(entityType: AttachmentEntityType, entityId: UUID): List<Attachment>
}
