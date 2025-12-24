package ge.tiger8bit.service

import ge.tiger8bit.constants.ChallengeConstants
import ge.tiger8bit.domain.Checkpoint
import ge.tiger8bit.domain.PatrolRouteCheckpoint
import ge.tiger8bit.domain.PatrolRun
import ge.tiger8bit.domain.PatrolScanEvent
import ge.tiger8bit.dto.*
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.CheckpointRepository
import ge.tiger8bit.repository.PatrolRouteCheckpointRepository
import ge.tiger8bit.repository.PatrolRunRepository
import ge.tiger8bit.repository.PatrolScanEventRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.*

@Singleton
open class ScanService(
    private val checkpointRepository: CheckpointRepository,
    private val patrolRunRepository: PatrolRunRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
    private val patrolScanEventRepository: PatrolScanEventRepository,
    private val challengeService: ChallengeService,
    private val accessService: AccessService
) {
    private val logger = getLogger()

    @Transactional
    open fun startScan(request: StartScanRequest, userId: UUID): StartScanResponse {
        accessService.ensureWorkerOrBoss(userId, request.organizationId)

        logger.info("Start scan: cpCode={}, device={}, org={}", request.checkpointCode, request.deviceId, request.organizationId)

        val checkpoint = checkpointRepository.findByCode(request.checkpointCode)
            .orElseThrow {
                logger.warn("Checkpoint not found: code={}", request.checkpointCode)
                HttpStatusException(HttpStatus.NOT_FOUND, "Checkpoint not found")
            }

        if (checkpoint.organizationId != request.organizationId) {
            logger.warn("Organization mismatch: expected={}, got={}", checkpoint.organizationId, request.organizationId)
            throw HttpStatusException(HttpStatus.FORBIDDEN, "Organization mismatch")
        }

        val patrolRun = findActivePatrolRun(request.organizationId)

        val routeCheckpoints = patrolRouteCheckpointRepository.findByRouteIdOrderBySeqAsc(patrolRun.routeId)
        val cpInRoute = routeCheckpoints.find { it.checkpointId == checkpoint.id!! }
            ?: run {
                logger.warn("Checkpoint not in route: cpId={}, routeId={}", checkpoint.id, patrolRun.routeId)
                throw HttpStatusException(HttpStatus.BAD_REQUEST, "Checkpoint not in route")
            }

        val challenge = challengeService.issue(
            organizationId = request.organizationId,
            deviceId = request.deviceId,
            checkpointId = checkpoint.id!!
        )

        logger.info("Challenge issued: org={}, device={}, cp={}", request.organizationId, request.deviceId, checkpoint.id)

        val policy = buildScanPolicy(patrolRun, checkpoint, cpInRoute)
        return StartScanResponse(challenge, policy)
    }

    @Transactional
    open fun finishScan(request: FinishScanRequest, userId: UUID): FinishScanResponse {
        val (orgId, deviceId, checkpointId) = parseChallenge(request.challenge)
        accessService.ensureWorkerOrBoss(userId, orgId)

        when (val result = challengeService.validateAndConsume(request.challenge, orgId, deviceId, checkpointId)) {
            is ValidationResult.Invalid -> {
                logger.warn("Challenge validation failed: reason={}", result.reason)
                throw HttpStatusException(HttpStatus.BAD_REQUEST, result.reason)
            }
            is ValidationResult.Replay -> {
                logger.warn("Replay attack detected: org={}, device={}, cp={}", orgId, deviceId, checkpointId)
                throw HttpStatusException(HttpStatus.CONFLICT, result.reason)
            }
            is ValidationResult.Valid -> {
                val patrolRun = findActivePatrolRun(orgId)

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

                logger.info("Scan event recorded: id={}, user={}, cp={}", scanEvent.id, request.userId, checkpointId)

                return FinishScanResponse(
                    eventId = scanEvent.id!!,
                    verdict = scanEvent.verdict
                )
            }
        }
    }

    private fun buildScanPolicy(
        patrolRun: PatrolRun,
        checkpoint: Checkpoint,
        cpInRoute: PatrolRouteCheckpoint
    ): ScanPolicy {
        val geoConstraint = if (checkpoint.geoLat != null && checkpoint.geoLon != null && checkpoint.radiusM != null) {
            GeoConstraint(
                lat = checkpoint.geoLat!!,
                lon = checkpoint.geoLon!!,
                radiusM = checkpoint.radiusM!!
            )
        } else null

        return ScanPolicy(
            runId = patrolRun.id!!,
            checkpointId = checkpoint.id!!,
            order = cpInRoute.seq,
            timeWindow = TimeWindow(
                minOffsetSec = cpInRoute.minOffsetSec,
                maxOffsetSec = cpInRoute.maxOffsetSec
            ),
            geo = geoConstraint
        )
    }

    private fun parseChallenge(jws: String): Triple<UUID, String, UUID> {
        return try {
            val signedJWT = com.nimbusds.jwt.SignedJWT.parse(jws)
            val claims = signedJWT.jwtClaimsSet
            Triple(
                UUID.fromString(claims.getClaim(ChallengeConstants.Claims.ORGANIZATION) as String),
                claims.getClaim(ChallengeConstants.Claims.DEVICE) as String,
                UUID.fromString(claims.getClaim(ChallengeConstants.Claims.CHECKPOINT) as String)
            )
        } catch (e: Exception) {
            logger.warn("Invalid challenge format: {}", e.message)
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid challenge format")
        }
    }

    private fun findActivePatrolRun(orgId: UUID): PatrolRun {
        val runs = patrolRunRepository.findAll()
            .filter { it.organizationId == orgId && it.status in listOf("pending", "in_progress") }

        return runs.firstOrNull()
            ?: run {
                logger.warn("No active patrol run for org: {}", orgId)
                throw HttpStatusException(HttpStatus.NOT_FOUND, "No active patrol run found")
            }
    }
}

