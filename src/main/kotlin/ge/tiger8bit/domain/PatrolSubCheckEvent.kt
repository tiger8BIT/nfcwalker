package ge.tiger8bit.domain

import ge.tiger8bit.dto.CheckStatus
import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import java.time.Instant
import java.util.*

@Entity
@Table(name = "patrol_sub_check_events")
@Serdeable
class PatrolSubCheckEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "scan_event_id", nullable = false)
    var scanEventId: UUID = UUID(0, 0),

    @Column(name = "sub_check_id", nullable = false)
    var subCheckId: UUID = UUID(0, 0),

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    @Column(nullable = false)
    var status: CheckStatus = CheckStatus.OK,

    @Column(columnDefinition = "text")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(
        scanEventId: UUID,
        subCheckId: UUID,
        status: CheckStatus = CheckStatus.OK,
        notes: String? = null
    ) : this(null, scanEventId, subCheckId, status, notes, Instant.now())
}
