package ge.tiger8bit.dto

import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal

@Serdeable
data class CreateCheckpointRequest(
    val organizationId: Long,
    val siteId: Long,
    val code: String,
    val geoLat: BigDecimal? = null,
    val geoLon: BigDecimal? = null,
    val radiusM: BigDecimal? = null
)

@Serdeable
data class CheckpointResponse(
    val id: Long,
    val organizationId: Long,
    val siteId: Long,
    val code: String,
    val geoLat: BigDecimal? = null,
    val geoLon: BigDecimal? = null,
    val radiusM: BigDecimal? = null
)

@Serdeable
data class CreateRouteRequest(
    val organizationId: Long,
    val siteId: Long,
    val name: String
)

@Serdeable
data class RouteResponse(
    val id: Long,
    val organizationId: Long,
    val siteId: Long,
    val name: String
)

@Serdeable
data class AddRouteCheckpointRequest(
    val checkpointId: Long,
    val seq: Int,
    val minOffsetSec: Int = 0,
    val maxOffsetSec: Int = 3600
)

@Serdeable
data class BulkAddRouteCheckpointsRequest(
    val checkpoints: List<AddRouteCheckpointRequest>
)

@Serdeable
data class StartScanRequest(
    val organizationId: Long,
    val deviceId: String,
    val checkpointCode: String
)

@Serdeable
data class ScanPolicy(
    val runId: Long,
    val checkpointId: Long,
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
    val eventId: Long,
    val verdict: String
)

