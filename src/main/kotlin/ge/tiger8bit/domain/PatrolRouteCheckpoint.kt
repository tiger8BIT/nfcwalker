package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(
    name = "patrol_route_checkpoints",
    indexes = [
        Index(name = "idx_prc_route_id", columnList = "route_id")
    ]
)
@IdClass(PatrolRouteCheckpointId::class)
@Serdeable
class PatrolRouteCheckpoint(
    @Id
    @Column(name = "route_id", nullable = false)
    var routeId: Long = 0,

    @Id
    @Column(name = "checkpoint_id", nullable = false)
    var checkpointId: Long = 0,

    @Column(nullable = false)
    var seq: Int = 0,

    @Column(name = "min_offset_sec", nullable = false)
    var minOffsetSec: Int = 0,

    @Column(name = "max_offset_sec", nullable = false)
    var maxOffsetSec: Int = 3600
)

@Serdeable
data class PatrolRouteCheckpointId(
    var routeId: Long = 0,
    var checkpointId: Long = 0
) : Serializable

