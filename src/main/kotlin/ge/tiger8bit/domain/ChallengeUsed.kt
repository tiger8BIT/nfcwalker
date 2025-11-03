package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "challenge_used",
    indexes = [
        Index(name = "idx_challenge_used_expires_at", columnList = "expires_at")
    ]
)
@Serdeable
class ChallengeUsed(
    @Id
    @Column(nullable = false, length = 100)
    var jti: String = "",

    @Column(name = "issued_at", nullable = false)
    var issuedAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(name = "used_at", nullable = false)
    var usedAt: Instant = Instant.now(),

    @Column(name = "device_id", nullable = false, length = 200)
    var deviceId: String = "",

    @Column(name = "checkpoint_id", nullable = false)
    var checkpointId: Long = 0
)

