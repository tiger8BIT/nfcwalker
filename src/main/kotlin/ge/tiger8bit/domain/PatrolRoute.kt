package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "patrol_routes")
@Serdeable
class PatrolRoute(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID = UUID(0, 0),

    @Column(name = "site_id", nullable = false)
    var siteId: UUID = UUID(0, 0),

    @Column(nullable = false, length = 200)
    var name: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(organizationId: UUID, siteId: UUID, name: String) : this(null, organizationId, siteId, name, Instant.now())
}

