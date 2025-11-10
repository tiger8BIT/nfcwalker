package ge.tiger8bit.dto

import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal
import java.util.UUID

@Serdeable
data class CreateCheckpointRequest(
    val organizationId: UUID,
    val siteId: UUID,
    val code: String,
    val geoLat: BigDecimal? = null,
    val geoLon: BigDecimal? = null,
    val radiusM: BigDecimal? = null
)

@Serdeable
data class CheckpointResponse(
    val id: UUID,
    val organizationId: UUID,
    val siteId: UUID,
    val code: String,
    val geoLat: BigDecimal? = null,
    val geoLon: BigDecimal? = null,
    val radiusM: BigDecimal? = null
)

@Serdeable
data class CreateRouteRequest(
    val organizationId: UUID,
    val siteId: UUID,
    val name: String
)

@Serdeable
data class RouteResponse(
    val id: UUID,
    val organizationId: UUID,
    val siteId: UUID,
    val name: String
)

@Serdeable
data class AddRouteCheckpointRequest(
    val checkpointId: UUID,
    val seq: Int,
    val minOffsetSec: Int = 0,
    val maxOffsetSec: Int = 3600
)

@Serdeable
data class BulkAddRouteCheckpointsRequest(
    val checkpoints: List<AddRouteCheckpointRequest>
)

// ===== Organization DTOs =====

@Serdeable
data class CreateOrganizationRequest(
    val name: String
)

@Serdeable
data class UpdateOrganizationRequest(
    val name: String
)

@Serdeable
data class OrganizationResponse(
    val id: UUID,
    val name: String,
    val createdAt: java.time.Instant
)

// ===== Site DTOs =====

@Serdeable
data class CreateSiteRequest(
    val organizationId: UUID,
    val name: String
)

@Serdeable
data class UpdateSiteRequest(
    val name: String
)

@Serdeable
data class SiteResponse(
    val id: UUID,
    val organizationId: UUID,
    val name: String,
    val createdAt: java.time.Instant
)

@Serdeable
data class StartScanRequest(
    val organizationId: UUID,
    val deviceId: String,
    val checkpointCode: String
)

@Serdeable
data class ScanPolicy(
    val runId: UUID,
    val checkpointId: UUID,
    val order: Int,
    val timeWindow: TimeWindow,
    val geo: GeoConstraint?
)

@Serdeable
data class TimeWindow(
    val minOffsetSec: Int,
    val maxOffsetSec: Int
)

@Serdeable
data class GeoConstraint(
    val lat: BigDecimal,
    val lon: BigDecimal,
    val radiusM: BigDecimal
)

@Serdeable
data class StartScanResponse(
    val challenge: String,
    val policy: ScanPolicy
)

@Serdeable
data class FinishScanRequest(
    val challenge: String,
    val userId: String,
    val scannedAt: String,
    val lat: BigDecimal? = null,
    val lon: BigDecimal? = null
)

@Serdeable
data class FinishScanResponse(
    val eventId: UUID,
    val verdict: String
)

