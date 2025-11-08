package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sites")
@Serdeable
class Site(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID = UUID(0, 0),

    @Column(nullable = false, length = 200)
    var name: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(organizationId: UUID, name: String) : this(null, organizationId, name, Instant.now())
}

