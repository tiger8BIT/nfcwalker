package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "patrol_scan_events",
    indexes = [
        Index(name = "idx_pse_patrol_run_id", columnList = "patrol_run_id"),
        Index(name = "idx_pse_checkpoint_id", columnList = "checkpoint_id"),
        Index(name = "idx_pse_user_id", columnList = "user_id"),
        Index(name = "idx_pse_scanned_at", columnList = "scanned_at")
    ]
)
@Serdeable
class PatrolScanEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "patrol_run_id", nullable = false)
    var patrolRunId: UUID = UUID(0, 0),

    @Column(name = "checkpoint_id", nullable = false)
    var checkpointId: UUID = UUID(0, 0),

    @Column(name = "user_id", nullable = false)
    var userId: String = "",

    @Column(name = "scanned_at", nullable = false)
    var scannedAt: Instant = Instant.now(),

    @Column(precision = 10, scale = 7)
    var lat: BigDecimal? = null,

    @Column(precision = 10, scale = 7)
    var lon: BigDecimal? = null,

    @Column(nullable = false, length = 50)
    var verdict: String = "ok",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(
        patrolRunId: UUID,
        checkpointId: UUID,
        userId: String,
        scannedAt: Instant,
        lat: BigDecimal? = null,
        lon: BigDecimal? = null,
        verdict: String = "ok"
    ) : this(null, patrolRunId, checkpointId, userId, scannedAt, lat, lon, verdict, Instant.now())
}

