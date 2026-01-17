package ge.tiger8bit.domain

import ge.tiger8bit.dto.IncidentSeverity
import ge.tiger8bit.dto.IncidentStatus
import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "incidents", indexes = [
        Index(name = "idx_incidents_organization_id", columnList = "organization_id"),
        Index(name = "idx_incidents_site_id", columnList = "site_id"),
        Index(name = "idx_incidents_status", columnList = "status")
    ]
)
@Serdeable
class Incident(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID = UUID(0, 0),

    @Column(name = "site_id", nullable = false)
    var siteId: UUID = UUID(0, 0),

    @Column(name = "checkpoint_id")
    var checkpointId: UUID? = null,

    @Column(name = "scan_event_id")
    var scanEventId: UUID? = null,

    @Column(name = "reported_by", nullable = false)
    var reportedBy: UUID = UUID(0, 0),

    @Column(nullable = false, columnDefinition = "text")
    var description: String = "",

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    @Column(nullable = false)
    var severity: IncidentSeverity = IncidentSeverity.MEDIUM,

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    @Column(nullable = false)
    var status: IncidentStatus = IncidentStatus.OPEN,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    constructor(
        organizationId: UUID,
        siteId: UUID,
        reportedBy: UUID,
        description: String,
        severity: IncidentSeverity = IncidentSeverity.MEDIUM,
        status: IncidentStatus = IncidentStatus.OPEN,
        checkpointId: UUID? = null,
        scanEventId: UUID? = null
    ) : this(
        null, organizationId, siteId, checkpointId, scanEventId, reportedBy, description, severity, status, Instant.now(), Instant.now()
    )
}
