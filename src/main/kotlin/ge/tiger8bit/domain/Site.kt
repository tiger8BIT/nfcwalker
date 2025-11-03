package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "sites")
@Serdeable
class Site(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "organization_id", nullable = false)
    var organizationId: Long = 0,

    @Column(nullable = false, length = 200)
    var name: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(organizationId: Long, name: String) : this(null, organizationId, name, Instant.now())
}

