package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
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
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserRoleId) return false
        return userId == other.userId && organizationId == other.organizationId
    }

    override fun hashCode(): Int {
        return 31 * userId.hashCode() + organizationId.hashCode()
    }
}

@Entity
@Table(name = "user_roles")
@Serdeable
class UserRole(
    @EmbeddedId
    var id: UserRoleId = UserRoleId(),

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    @Column(nullable = false)
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
