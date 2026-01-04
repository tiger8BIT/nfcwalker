package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "attachments",
    indexes = [
        Index(name = "idx_attachments_entity", columnList = "entity_type, entity_id")
    ]
)
@Serdeable
class Attachment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = "",

    @Column(name = "entity_id", nullable = false)
    var entityId: UUID = UUID(0, 0),

    @Column(name = "file_path", nullable = false, length = 512)
    var filePath: String = "",

    @Column(name = "original_name", length = 255)
    var originalName: String? = null,

    @Column(name = "content_type", length = 100)
    var contentType: String? = null,

    @Column(name = "file_size")
    var fileSize: Long? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(
        entityType: String,
        entityId: UUID,
        filePath: String,
        originalName: String? = null,
        contentType: String? = null,
        fileSize: Long? = null
    ) : this(null, entityType, entityId, filePath, originalName, contentType, fileSize, Instant.now())
}
