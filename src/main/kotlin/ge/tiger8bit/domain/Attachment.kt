package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import java.time.Instant
import java.util.*

@Serdeable
enum class AttachmentEntityType {
    checkpoint,
    scan_event,
    sub_check_event,
    incident
}

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

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    @Column(name = "entity_type", nullable = false)
    var entityType: AttachmentEntityType = AttachmentEntityType.checkpoint,

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
        entityType: AttachmentEntityType,
        entityId: UUID,
        filePath: String,
        originalName: String? = null,
        contentType: String? = null,
        fileSize: Long? = null
    ) : this(null, entityType, entityId, filePath, originalName, contentType, fileSize, Instant.now())
}
