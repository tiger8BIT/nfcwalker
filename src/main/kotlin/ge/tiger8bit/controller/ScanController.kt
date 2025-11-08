package ge.tiger8bit.controller

import ge.tiger8bit.domain.PatrolScanEvent
import ge.tiger8bit.dto.*
import ge.tiger8bit.repository.*
import ge.tiger8bit.service.ChallengeService
import ge.tiger8bit.service.ValidationResult
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.transaction.Transactional
import io.micronaut.security.annotation.Secured
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

@Controller("/api/scan")
@Secured("ROLE_WORKER","ROLE_BOSS")
open class ScanController(
    private val checkpointRepository: CheckpointRepository,
    private val patrolRunRepository: PatrolRunRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
    private val patrolScanEventRepository: PatrolScanEventRepository,
    private val challengeService: ChallengeService
) {
    private val logger = LoggerFactory.getLogger(ScanController::class.java)

    @Post("/start")
    @Transactional
    open fun startScan(@Body request: StartScanRequest): StartScanResponse {
        val checkpoint = checkpointRepository.findByCode(request.checkpointCode)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Checkpoint not found") }

        if (checkpoint.organizationId != request.organizationId) {
            throw HttpStatusException(HttpStatus.FORBIDDEN, "Organization mismatch")
        }

        // Find an active patrol run for this checkpoint
        // For MVP, we'll find any pending/in-progress run that includes this checkpoint
        val runs = patrolRunRepository.findAll()
            .filter { it.organizationId == request.organizationId && it.status in listOf("pending", "in_progress") }

        val patrolRun = runs.firstOrNull()
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "No active patrol run found")

        // Find checkpoint order in route
        val routeCheckpoints = patrolRouteCheckpointRepository.findByRouteIdOrderBySeqAsc(patrolRun.routeId)
        val cpInRoute = routeCheckpoints.find { it.checkpointId == checkpoint.id!! }
            ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "Checkpoint not in route")

        // Issue challenge
        val challenge = challengeService.issue(
            orgId = request.organizationId,
            deviceId = request.deviceId,
            checkpointId = checkpoint.id!!
        )

        // Log issued challenge in debug for easier troubleshooting during tests
        logger.debug("Issued challenge for org=${request.organizationId}, device=${request.deviceId}, cp=${checkpoint.id}: $challenge")

        val policy = ScanPolicy(
            runId = patrolRun.id!!,
            checkpointId = checkpoint.id!!,
            order = cpInRoute.seq,
            timeWindow = TimeWindow(
                minOffsetSec = cpInRoute.minOffsetSec,
                maxOffsetSec = cpInRoute.maxOffsetSec
            ),
            geo = if (checkpoint.geoLat != null && checkpoint.geoLon != null && checkpoint.radiusM != null) {
                GeoConstraint(
                    lat = checkpoint.geoLat!!,
                    lon = checkpoint.geoLon!!,
                    radiusM = checkpoint.radiusM!!
                )
            } else null
        )

        return StartScanResponse(challenge, policy)
    }

    @Post("/finish")
    @Transactional
    open fun finishScan(@Body request: FinishScanRequest): HttpResponse<FinishScanResponse> {
        // Parse challenge to get expected values
        val tempResult = try {
            val signedJWT = com.nimbusds.jwt.SignedJWT.parse(request.challenge)
            val claims = signedJWT.jwtClaimsSet
            Triple(
                UUID.fromString(claims.getClaim("org") as String),
                claims.getClaim("dev") as String,
                UUID.fromString(claims.getClaim("cp") as String)
            )
        } catch (_: Exception) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid challenge format")
        }

        val (orgId, deviceId, checkpointId) = tempResult

        // Validate and consume challenge
        when (val result = challengeService.validateAndConsume(request.challenge, orgId, deviceId, checkpointId)) {
            is ValidationResult.Invalid -> {
                throw HttpStatusException(HttpStatus.BAD_REQUEST, result.reason)
            }
            is ValidationResult.Replay -> {
                throw HttpStatusException(HttpStatus.CONFLICT, result.reason)
            }
            is ValidationResult.Valid -> {
                // Find patrol run that's active
                val runs = patrolRunRepository.findAll()
                    .filter { it.organizationId == orgId && it.status in listOf("pending", "in_progress") }

                val patrolRun = runs.firstOrNull()
                    ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "No active patrol run found")

                // Create scan event with verdict "ok" (MVP)
                val scanEvent = patrolScanEventRepository.save(
                    PatrolScanEvent(
                        patrolRunId = patrolRun.id!!,
                        checkpointId = checkpointId,
                        userId = request.userId,
                        scannedAt = Instant.parse(request.scannedAt),
                        lat = request.lat,
                        lon = request.lon,
                        verdict = "ok"
                    )
                )

                return HttpResponse.ok(
                    FinishScanResponse(
                        eventId = scanEvent.id!!,
                        verdict = scanEvent.verdict
                    )
                )
            }
        }
    }
}
