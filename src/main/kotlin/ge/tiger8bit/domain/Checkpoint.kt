package ge.tiger8bit.domain

import io.micronaut.serde.annotation.Serdeable
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "checkpoints",
    indexes = [
        Index(name = "idx_checkpoints_organization_id", columnList = "organization_id"),
        Index(name = "idx_checkpoints_site_id", columnList = "site_id"),
        Index(name = "idx_checkpoints_code", columnList = "code")
    ]
)
@Serdeable
class Checkpoint(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "organization_id", nullable = false)
    var organizationId: Long = 0,

    @Column(name = "site_id", nullable = false)
    var siteId: Long = 0,

    @Column(nullable = false, unique = true, length = 100)
    var code: String = "",

    @Column(name = "geo_lat", precision = 10, scale = 7)
    var geoLat: BigDecimal? = null,

    @Column(name = "geo_lon", precision = 10, scale = 7)
    var geoLon: BigDecimal? = null,

    @Column(name = "radius_m", precision = 6, scale = 2)
    var radiusM: BigDecimal? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    constructor(
        organizationId: Long,
        siteId: Long,
        code: String,
        geoLat: BigDecimal? = null,
        geoLon: BigDecimal? = null,
        radiusM: BigDecimal? = null
    ) : this(null, organizationId, siteId, code, geoLat, geoLon, radiusM, Instant.now())
}

