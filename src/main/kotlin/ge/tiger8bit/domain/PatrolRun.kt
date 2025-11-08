package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "patrol_runs",
    indexes = [
        Index(name = "idx_patrol_runs_route_id", columnList = "route_id"),
        Index(name = "idx_patrol_runs_organization_id", columnList = "organization_id"),
        Index(name = "idx_patrol_runs_status", columnList = "status")
    ]
)
@Serdeable
class PatrolRun(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "route_id", nullable = false)
    var routeId: UUID = UUID(0, 0),

    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID = UUID(0, 0),

    @Column(name = "planned_start", nullable = false)
    var plannedStart: Instant = Instant.now(),

    @Column(name = "planned_end", nullable = false)
    var plannedEnd: Instant = Instant.now(),

    @Column(nullable = false, length = 50)
    var status: String = "pending",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(routeId: UUID, organizationId: UUID, plannedStart: Instant, plannedEnd: Instant, status: String)
        : this(null, routeId, organizationId, plannedStart, plannedEnd, status, Instant.now())
}

