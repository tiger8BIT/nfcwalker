package ge.tiger8bit.dto

import ge.tiger8bit.domain.Role
import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal
import java.util.*

@Serdeable
data class CreateCheckpointRequest(
    val organizationId: UUID,
    val siteId: UUID,
    val code: String,
    val geoLat: BigDecimal? = null,
    val geoLon: BigDecimal? = null,
    val radiusM: BigDecimal? = null,
    val label: String? = null,
    val detailsConfig: ge.tiger8bit.domain.CheckpointDetailsConfig? = null,
    val subChecks: List<SubCheckRequest>? = null
)

@Serdeable
data class UpdateCheckpointRequest(
    val label: String? = null,
    val geoLat: BigDecimal? = null,
    val geoLon: BigDecimal? = null,
    val radiusM: BigDecimal? = null,
    val detailsConfig: ge.tiger8bit.domain.CheckpointDetailsConfig? = null,
    val subChecks: List<SubCheckRequest>? = null
)

@Serdeable
data class CheckpointResponse(
    val id: UUID,
    val organizationId: UUID,
    val siteId: UUID,
    val code: String,
    val geoLat: BigDecimal? = null,
    val geoLon: BigDecimal? = null,
    val radiusM: BigDecimal? = null,
    val label: String? = null,
    val detailsConfig: ge.tiger8bit.domain.CheckpointDetailsConfig? = null,
    val subChecks: List<SubCheckResponse>? = null
)

@Serdeable
data class SubCheckRequest(
    val label: String,
    val description: String? = null,
    val requirePhoto: Boolean = false,
    val allowNotes: Boolean = true
)

@Serdeable
data class SubCheckResponse(
    val id: UUID,
    val label: String,
    val description: String? = null,
    val requirePhoto: Boolean = false,
    val allowNotes: Boolean = true
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
enum class CheckStatus {
    OK,
    PROBLEMS_FOUND,
    SKIPPED
}

@Serdeable
enum class ScanVerdict {
    OK,
    WARNING,
    FAIL
}

@Serdeable
data class FinishScanRequest(
    val challenge: String,
    val scannedAt: java.time.Instant,
    val lat: BigDecimal? = null,
    val lon: BigDecimal? = null,
    val checkStatus: CheckStatus? = null,
    val checkNotes: String? = null,
    val subCheckResults: List<SubCheckResultRequest>? = null,
    val incidents: List<IncidentCreateRequest>? = null
)

@Serdeable
data class SubCheckResultRequest(
    val subCheckId: UUID,
    val status: CheckStatus,
    val notes: String? = null
)

@Serdeable
data class FinishScanResponse(
    val eventId: UUID,
    val verdict: ScanVerdict
)

// ===== Authentication & User Management DTOs =====

@Serdeable
data class CreatePatrolRunRequest(
    val routeId: UUID,
    val organizationId: UUID,
    val plannedStart: java.time.Instant? = null,
    val plannedEnd: java.time.Instant? = null
)

@Serdeable
data class PatrolRunResponse(
    val id: UUID,
    val routeId: UUID,
    val organizationId: UUID,
    val status: PatrolRunStatus,
    val plannedStart: java.time.Instant,
    val plannedEnd: java.time.Instant
)

@Serdeable
enum class PatrolRunStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    MISSED
}

@Serdeable
data class UserResponse(
    val id: UUID,
    val email: String,
    val name: String,
    val createdAt: java.time.Instant
)

@Serdeable
data class RegisterDeviceRequest(
    val deviceId: String,
    val metadata: String? = null
)

@Serdeable
enum class DeviceStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED
}

@Serdeable
data class DeviceResponse(
    val id: UUID,
    val deviceId: String,
    val metadata: String? = null,
    val status: DeviceStatus,
    val registeredAt: java.time.Instant,
    val lastUsedAt: java.time.Instant? = null
)

@Serdeable
enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED
}

@Serdeable
data class InvitationResponse(
    val id: UUID,
    val email: String,
    val organizationId: UUID,
    val role: Role,
    val status: InvitationStatus,
    val expiresAt: java.time.Instant
)

@Serdeable
data class CreateInvitationRequest(
    val email: String,
    val organizationId: UUID,
    val role: Role
)

@Serdeable
data class AcceptInvitationRequest(
    val token: String
)

@Serdeable
data class AuthMeResponse(
    val user: UserResponse,
    val roles: Map<String, Role>
)

// ===== Incident DTOs =====

@Serdeable
enum class IncidentSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serdeable
enum class IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}

@Serdeable
data class IncidentCreateRequest(
    val organizationId: UUID? = null,
    val siteId: UUID? = null,
    val description: String,
    val severity: IncidentSeverity = IncidentSeverity.MEDIUM,
    val checkpointId: UUID? = null,
    val scanEventId: UUID? = null
)

@Serdeable
data class IncidentResponse(
    val id: UUID,
    val organizationId: UUID,
    val siteId: UUID,
    val checkpointId: UUID?,
    val scanEventId: UUID?,
    val reportedBy: UUID,
    val description: String,
    val severity: IncidentSeverity,
    val status: IncidentStatus,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant
)

@Serdeable
data class UpdateRouteRequest(val name: String? = null)

@Serdeable
data class IncidentPatchRequest(
    val description: String? = null,
    val severity: IncidentSeverity? = null,
    val status: IncidentStatus? = null
)
