package ge.tiger8bit.domain

import ge.tiger8bit.dto.InvitationStatus
import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import java.time.Instant
import java.util.*

@Entity
@Table(name = "invitations")
@Serdeable
class Invitation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 255)
    var email: String = "",

    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var role: Role = Role.ROLE_WORKER,

    @Column(nullable = false, unique = true, length = 255)
    var token: String = "",

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var status: InvitationStatus = InvitationStatus.PENDING,

    @Column(name = "created_by")
    var createdBy: UUID? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(name = "accepted_at")
    var acceptedAt: Instant? = null
) {
    constructor(
        email: String,
        organizationId: UUID,
        role: Role,
        token: String,
        expiresAt: Instant,
        createdBy: UUID? = null
    ) : this(
        null,
        email,
        organizationId,
        role,
        token,
        InvitationStatus.PENDING,
        createdBy,
        Instant.now(),
        expiresAt,
        null
    )
}

