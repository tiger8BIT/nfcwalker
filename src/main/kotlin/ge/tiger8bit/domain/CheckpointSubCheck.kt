package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "checkpoint_sub_checks")
@Serdeable
class CheckpointSubCheck(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "checkpoint_id", nullable = false)
    var checkpointId: UUID = UUID(0, 0),

    @Column(nullable = false, length = 200)
    var label: String = "",

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "require_photo")
    var requirePhoto: Boolean = false,

    @Column(name = "allow_notes")
    var allowNotes: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(
        checkpointId: UUID,
        label: String,
        description: String? = null,
        requirePhoto: Boolean = false,
        allowNotes: Boolean = true
    ) : this(null, checkpointId, label, description, requirePhoto, allowNotes, Instant.now())
}
