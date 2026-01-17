package ge.tiger8bit.domain

import ge.tiger8bit.dto.DeviceStatus
import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import org.hibernate.annotations.JdbcType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "devices")
@Serdeable
class Device(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID = UUID.randomUUID(),

    @Column(name = "device_id", nullable = false, length = 255)
    var deviceId: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    var metadata: String? = null,

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    @Column(nullable = false)
    var status: DeviceStatus = DeviceStatus.ACTIVE,

    @Column(name = "registered_at", nullable = false)
    var registeredAt: Instant = Instant.now(),

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(
        userId: UUID,
        organizationId: UUID,
        deviceId: String,
        metadata: String? = null
    ) : this(
        null,
        userId,
        organizationId,
        deviceId,
        metadata,
        DeviceStatus.ACTIVE,
        Instant.now(),
        null,
        Instant.now()
    )
}

