package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.*

@Embeddable
@Serdeable
class UserRoleId(
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID = UUID.randomUUID()
) : Serializable

@Entity
@Table(name = "user_roles")
@Serdeable
class UserRole(
    @EmbeddedId
    var id: UserRoleId = UserRoleId(),

    @Column(nullable = false, length = 50)
    var role: Role = Role.ROLE_WORKER,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(userId: UUID, organizationId: UUID, role: Role) : this(
        UserRoleId(userId, organizationId),
        role,
        Instant.now()
    )
}
