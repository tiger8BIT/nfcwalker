package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "users")
@Serdeable
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, unique = true, length = 255)
    var email: String = "",

    @Column(name = "google_id", unique = true, length = 255)
    var googleId: String? = null,

    @Column(nullable = false, length = 200)
    var name: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    constructor(email: String, name: String, googleId: String? = null) : this(
        null,
        email,
        googleId,
        name,
        Instant.now(),
        Instant.now()
    )
}

